import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

/** Servidor local: recursos web, sesiones HTTP y puente hacia las clases Java. */
public final class ServidorWeb {
    private static final Map<String, Sesion> SESIONES = new HashMap<>();
    private static final Map<String, long[]> INTENTOS = new HashMap<>();
    private static final int MAX_BODY = 40 * 1024 * 1024;
    private static int puerto;
    private ServidorWeb() { }
    static final class Sesion {
        final int usuario;
        final String csrf = token();
        final long creada = System.currentTimeMillis();
        long ultimo = creada;
        Sesion(int usuario) { this.usuario = usuario; }
    }
    public static void iniciar(int p, boolean abrir) throws IOException {
        puerto = p;
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
        // Los DAO originales usan una conexión compartida: todas las operaciones web se serializan.
        servidor.setExecutor(Executors.newSingleThreadExecutor());
        WebApi api = new WebApi();
        servidor.createContext("/", x -> {
            try {
                cabeceras(x);
                validarOrigen(x);
                String ruta = x.getRequestURI().getPath();
                if (ruta.startsWith("/api/")) api.atender(x);
                else if (ruta.startsWith("/archivo/")) api.archivo(x, ruta.substring(9));
                else estatico(x, ruta);
            } catch (ErrorHttp e) { json(x, e.codigo, WebDatos.mapa("error", e.getMessage())); }
            catch (IllegalArgumentException | IllegalStateException e) {
                if (e.getCause() != null) e.printStackTrace();
                json(x, 400, WebDatos.mapa("error", e.getMessage()));
            } catch (java.time.DateTimeException e) {
                json(x, 400, WebDatos.mapa("error", "La fecha u hora indicada no es válida."));
            } catch (Exception e) {
                e.printStackTrace();
                json(x, 500, WebDatos.mapa("error", "Ocurrió un problema al procesar la solicitud. Revisa la ventana de Java."));
            } finally { x.close(); }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(0)));
        servidor.start();
        String url = "http://127.0.0.1:" + p;
        System.out.println("\nNEXO - Tutorías académicas\n" + url + "\nDeja esta ventana abierta. Ctrl+C para detener.\n");
        if (abrir && Desktop.isDesktopSupported()) {
            try { Desktop.getDesktop().browse(URI.create(url)); } catch (Exception ignored) { }
        }
    }
    private static void cabeceras(HttpExchange x) {
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("Referrer-Policy", "same-origin");
        x.getResponseHeaders().set("X-Frame-Options", "DENY");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; media-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
    }
    private static void validarOrigen(HttpExchange x) {
        String host = x.getRequestHeaders().getFirst("Host");
        if (!("127.0.0.1:" + puerto).equals(host) && !("localhost:" + puerto).equals(host))
            throw new ErrorHttp(403, "Dirección del servidor no permitida.");
        String origen = x.getRequestHeaders().getFirst("Origin");
        if (origen != null && !origen.equals("http://" + host)) throw new ErrorHttp(403, "Origen no permitido.");
        if ("cross-site".equals(x.getRequestHeaders().getFirst("Sec-Fetch-Site"))) throw new ErrorHttp(403, "Origen no permitido.");
    }
    static Map<String, String> parametros(HttpExchange x) throws IOException {
        if ("GET".equals(x.getRequestMethod())) return decodificar(x.getRequestURI().getRawQuery());
        if (!"POST".equals(x.getRequestMethod())) throw new ErrorHttp(405, "Método no permitido.");
        String tipo = x.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.startsWith("application/x-www-form-urlencoded")) throw new ErrorHttp(415, "Usa un formulario válido.");
        byte[] bytes = x.getRequestBody().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) throw new ErrorHttp(413, "La solicitud excede el límite permitido.");
        return decodificar(new String(bytes, StandardCharsets.UTF_8));
    }
    private static Map<String, String> decodificar(String datos) {
        Map<String, String> mapa = new HashMap<>();
        if (datos != null && !datos.isBlank()) for (String par : datos.split("&")) {
            String[] p = par.split("=", 2);
            mapa.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length == 2 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return mapa;
    }
    static Sesion sesion(HttpExchange x, boolean obligatoria) {
        SESIONES.entrySet().removeIf(e -> expirada(e.getValue()));
        Sesion s = SESIONES.get(cookie(x));
        if (s != null) s.ultimo = System.currentTimeMillis();
        if (s == null && obligatoria) throw new ErrorHttp(401, "Inicia sesión para continuar.");
        return s;
    }
    private static boolean expirada(Sesion s) {
        long ahora = System.currentTimeMillis();
        return ahora - s.ultimo > 2 * 60 * 60_000L || ahora - s.creada > 8 * 60 * 60_000L;
    }
    static void iniciarSesion(HttpExchange x, int id) {
        SESIONES.remove(cookie(x));
        String token = token();
        SESIONES.put(token, new Sesion(id));
        x.getResponseHeaders().add("Set-Cookie", "NEXO=" + token + "; Path=/; HttpOnly; SameSite=Strict");
    }
    static void cerrarSesion(HttpExchange x) {
        SESIONES.remove(cookie(x));
        x.getResponseHeaders().add("Set-Cookie", "NEXO=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
    }
    static void invalidar(int id) { SESIONES.entrySet().removeIf(e -> e.getValue().usuario == id); }
    static void limitarIntentos(String correo) {
        long ahora = System.currentTimeMillis();
        INTENTOS.entrySet().removeIf(e -> ahora - e.getValue()[0] > 60_000);
        long[] datos = INTENTOS.computeIfAbsent(correo, k -> new long[]{ahora, 0});
        if (++datos[1] > 12) throw new ErrorHttp(429, "Demasiados intentos. Espera un minuto y vuelve a intentar.");
    }
    private static String cookie(HttpExchange x) {
        String c = x.getRequestHeaders().getFirst("Cookie");
        if (c != null) for (String parte : c.split(";")) if (parte.trim().startsWith("NEXO=")) return parte.trim().substring(5);
        return "";
    }
    private static String token() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static void json(HttpExchange x, int codigo, Object contenido) throws IOException {
        byte[] bytes = JsonWeb.escribir(contenido).getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        x.sendResponseHeaders(codigo, bytes.length);
        x.getResponseBody().write(bytes);
    }
    private static void estatico(HttpExchange x, String ruta) throws IOException {
        if (!"GET".equals(x.getRequestMethod())) throw new ErrorHttp(405, "Método no permitido.");
        String archivo;
        if (ruta.equals("/") || ruta.equals("/index.html")) archivo = "index.html";
        else if (ruta.equals("/app.js")) archivo = "app.js";
        else if (ruta.equals("/estilos.css")) archivo = "estilos.css";
        else if (ruta.equals("/tema-uvg.css")) archivo = "tema-uvg.css";
        else if (ruta.equals("/jack.png")) archivo = "jack.png";
        else if (ruta.equals("/favicon.svg")) archivo = "favicon.svg";
        else throw new ErrorHttp(404, "Recurso no encontrado.");
        byte[] bytes;
        Path path = Path.of("src", "web", archivo);
        if (Files.isRegularFile(path)) bytes = Files.readAllBytes(path);
        else {
            try (InputStream in = ServidorWeb.class.getResourceAsStream("/web/" + archivo)) {
                if (in == null) throw new ErrorHttp(404, "No se encuentra src/web. Ejecuta desde la carpeta del proyecto.");
                bytes = in.readAllBytes();
            }
        }
        String mime = archivo.endsWith(".png") ? "image/png" : archivo.endsWith(".js") ? "text/javascript" : archivo.endsWith(".css") ? "text/css" : archivo.endsWith(".svg") ? "image/svg+xml" : "text/html";
        x.getResponseHeaders().set("Content-Type", mime.startsWith("image/") ? mime : mime + "; charset=utf-8");
        x.sendResponseHeaders(200, bytes.length); x.getResponseBody().write(bytes);
    }
    static final class ErrorHttp extends RuntimeException {
        final int codigo;
        ErrorHttp(int codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
    }
}
