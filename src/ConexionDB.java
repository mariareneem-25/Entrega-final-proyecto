import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Acceso a la base de datos principal ({@code proyecto.db}): abre conexiones, crea las tablas del sistema y ofrece una consulta de existencia.
 * La ruta del archivo se puede cambiar con la propiedad del sistema {@code nexo.db}.
 */
public class ConexionDB {
    private static final String URL = "jdbc:sqlite:" + System.getProperty("nexo.db", "proyecto.db");
 
    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL);
            try (Statement st = conexion.createStatement()) { st.execute("PRAGMA busy_timeout=5000"); st.execute("PRAGMA foreign_keys=ON"); }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo abrir proyecto.db. Revisa los permisos de la carpeta y la biblioteca SQLite.", e);
        }
        return conexion;
    }
 
    public static void crearTablas() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "idUsuario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "correoElectronico TEXT NOT NULL UNIQUE, " +
                "contrasena TEXT NOT NULL, " +
                "fotoPerfil TEXT, " +
                "descripcion TEXT, " +
                "banner TEXT, " +
                "tipoUsuario TEXT NOT NULL, " +
                "estadoCuenta TEXT NOT NULL)";
     
        String sqlSolicitudesTutor = "CREATE TABLE IF NOT EXISTS solicitudes_tutor (" +
                "idTutor INTEGER PRIMARY KEY, " +
                "estado TEXT NOT NULL, " +
                "aprobadoPor INTEGER)";
 
        String sqlSolicitudes = "CREATE TABLE IF NOT EXISTS solicitudes (" +
                "idSolicitud INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idEstudiante INTEGER NOT NULL, " +
                "idTutor INTEGER NOT NULL, " +
                "materia TEXT NOT NULL, " +
                "mensaje TEXT NOT NULL, " +
                "estado TEXT NOT NULL, " +
                "motivoRechazo TEXT)";

        String sqlMensajes = "CREATE TABLE IF NOT EXISTS mensajesTutoria (" +
                "idMensaje INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idSolicitud INTEGER NOT NULL, " +
                "idEmisor INTEGER NOT NULL, " +
                "texto TEXT NOT NULL, " +
                "fecha TEXT NOT NULL)";

        String sqlPropuestas = "CREATE TABLE IF NOT EXISTS propuestasFecha (" +
                "idPropuesta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idSolicitud INTEGER NOT NULL, " +
                "idProponente INTEGER NOT NULL, " +
                "fechaHora TEXT NOT NULL, " +
                "estado TEXT NOT NULL)";

        String sqlLogros = "CREATE TABLE IF NOT EXISTS logros (" +
                "idLogro INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idUsuario INTEGER NOT NULL, " +
                "tema TEXT NOT NULL, " +
                "completado INTEGER NOT NULL)";
     
      String sqlCalendarioPersonal = "CREATE TABLE IF NOT EXISTS calendario_personal (" +
         "idCalendario INTEGER PRIMARY KEY AUTOINCREMENT, " +
         "idUsuario INTEGER NOT NULL, " +
         "fechaHora TEXT NOT NULL, " +
         "tipo TEXT NOT NULL, " +
         "FOREIGN KEY (idUsuario) REFERENCES usuarios(idUsuario))";

        try (Connection conexion = conectar();
             Statement st = conexion.createStatement()) {
 
            st.execute(sqlUsuarios);
            st.execute(sqlSolicitudesTutor);
            st.execute(sqlSolicitudes);
            st.execute(sqlMensajes);
            st.execute(sqlPropuestas);
            st.execute(sqlLogros);
            st.execute(sqlCalendarioPersonal);
        
        try { 
          st.execute("ALTER TABLE usuarios ADD COLUMN descripcion TEXT");
        } catch (SQLException ignored) {
          
        }
        try {
          st.execute("ALTER TABLE usuarios ADD COLUMN banner TEXT");
        } catch (SQLException ignored) {
          
        }

        // columnas de la solicitud de tutor (informacion que llena el tutor y revision)
        String[] columnasSolicitudTutor = {
            "nombreCompleto TEXT", "descripcion TEXT", "materias TEXT",
            "fotoPerfil TEXT", "banner TEXT", "idRevisor INTEGER", "motivoRechazo TEXT"
        };

        for (String columna : columnasSolicitudTutor) {
            try {
                st.execute("ALTER TABLE solicitudes_tutor ADD COLUMN " + columna);
            } catch (SQLException ignored) {
                // la columna ya existe, no hacemos nada
            }
        }
 
        } catch (SQLException e) {
            System.out.println("Error al crear las tablas: " + e.getMessage());
        }
    }

    public static boolean existe(String sql, Object... parametros) {
        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error en la base de datos: " + e.getMessage());
        }
    }
}
