/** Entrada principal: web por defecto; --consola conserva el programa original. */
public class Main {
    public static void main(String[] args) throws Exception {
        boolean consola = java.util.Arrays.asList(args).contains("--consola");
        boolean demo = java.util.Arrays.asList(args).contains("--demo");
        if (demo) {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("datos-demo"));
            System.setProperty("nexo.db", "datos-demo/proyecto.db");
            System.setProperty("nexo.foro", "datos-demo/tutorias.db");
            System.setProperty("nexo.archivos", "datos-demo/archivos_web");
        }
        ConfiguracionInicial.preparar();
        WebDatos.preparar();
        if (demo) DatosDemo.preparar();
        if (consola) {
            FiltroConsola.activar();
            new MenuInicio().iniciar();
        } else {
            int puerto = Integer.getInteger("nexo.puerto", 8080);
            ServidorWeb.iniciar(puerto, !java.util.Arrays.asList(args).contains("--sin-navegador"));
        }
    }
}
