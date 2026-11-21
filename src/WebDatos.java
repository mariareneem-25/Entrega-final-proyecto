import java.sql.*;
import java.util.*;

/** Consultas de presentación y extensiones persistentes; conserva las dos BD originales. */
public final class WebDatos {
    private WebDatos() { }
    public static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) mapa.put(pares[i].toString(), pares[i + 1]);
        return mapa;
    }
    public static List<Map<String, Object>> consultar(String sql, Object... args) {
        try (Connection c = ConexionDB.conectar()) { return consultar(c, sql, args); }
        catch (SQLException e) { throw new IllegalStateException("No se pudo consultar la información.", e); }
    }
    public static List<Map<String, Object>> foro(String sql, Object... args) {
        return consultar(DatabaseConnection.getConnection(), sql, args);
    }
    private static List<Map<String, Object>> consultar(Connection c, String sql, Object... args) {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> salida = new ArrayList<>();
                ResultSetMetaData m = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) fila.put(m.getColumnLabel(i), r.getObject(i));
                    salida.add(fila);
                }
                return salida;
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo consultar la información.", e); }
    }
    public static int ejecutar(String sql, Object... args) {
        try (Connection c = ConexionDB.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron guardar los cambios.", e); }
    }
    public static void preparar() {
        ConexionDB.crearTablas();
        AutorizacionCorreos.preparar();
        new CalendarioDAO();
        new ChatDAO();
        ejecutar("CREATE TABLE IF NOT EXISTS web_archivos (id TEXT PRIMARY KEY, propietario INTEGER NOT NULL, "
                + "categoria TEXT NOT NULL, contexto TEXT, nombre TEXT NOT NULL, ruta TEXT NOT NULL, mime TEXT NOT NULL)");
        ejecutar("CREATE TABLE IF NOT EXISTS resenas_tutor (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idEstudiante INTEGER NOT NULL, idTutor INTEGER NOT NULL, puntuacion INTEGER NOT NULL CHECK(puntuacion BETWEEN 1 AND 5), "
                + "texto TEXT NOT NULL, fecha TEXT NOT NULL, UNIQUE(idEstudiante,idTutor))");
        // Reseñas anónimas y calificación interna (1 a 10) que docentes y directores dan a los tutores.
        columna("resenas_tutor", "anonimo", "INTEGER NOT NULL DEFAULT 0");
        columna("solicitudes_tutor", "calificacion", "INTEGER");
        columna("solicitudes_tutor", "calificadoPor", "INTEGER");
        columna("solicitudes_tutor", "fechaCalificacion", "TEXT");
        columna("logros", "tipo", "TEXT NOT NULL DEFAULT 'TEMA'");
        columna("logros", "fechaObjetivo", "TEXT");
        for (String t : List.of("reservas", "solicitudes_calendario")) {
            columna(t, "modalidad", "TEXT NOT NULL DEFAULT 'PRESENCIAL'");
            columna(t, "lugar", "TEXT NOT NULL DEFAULT ''");
        }
        // Las contraseñas antiguas se convierten sin cambiar la contraseña del usuario.
        for (Map<String, Object> fila : consultar("SELECT idUsuario, contrasena FROM usuarios")) {
            String clave = Objects.toString(fila.get("contrasena"), "");
            if (!ClaveSegura.esHash(clave)) ejecutar("UPDATE usuarios SET contrasena=? WHERE idUsuario=?", ClaveSegura.proteger(clave), fila.get("idUsuario"));
        }
    }
    private static void columna(String tabla, String nombre, String definicion) {
        boolean existe = consultar("PRAGMA table_info(" + tabla + ")").stream().anyMatch(f -> nombre.equals(f.get("name")));
        if (!existe) ejecutar("ALTER TABLE " + tabla + " ADD COLUMN " + nombre + " " + definicion);
    }
    public static Map<String, Object> perfil(Perfil_usuario p, boolean privado) {
        return mapa("id", p.getIdUsuario(), "nombre", p.getNombre(), "correo", privado ? p.getCorreoElectronico() : "",
                "rol", p.getTipoUsuario().name(), "descripcion", p.getDescripcion(), "foto", p.getFotoPerfil(), "banner", p.getBanner(),
                "estadoTutor", p.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR ? Perfil_usuario.obtenerEstadoSolicitudTutor(p.getIdUsuario()).name() : "");
    }
}
