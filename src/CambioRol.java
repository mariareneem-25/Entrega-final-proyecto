import java.sql.*;

/** Cambia el rol y cierra sus dependencias en una única transacción. */
public class CambioRol {
    public static void aTutor(int idUsuario) { cambiar(idUsuario, true); }
    public static void aEstudiante(int idUsuario) { cambiar(idUsuario, false); }
    private static void cambiar(int idUsuario, boolean tutor) {
        new CalendarioDAO();
        String columna = tutor ? "idEstudiante" : "idTutor";
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                ejecutar(c, "UPDATE solicitudes SET estado='RECHAZADO',motivoRechazo='La cuenta cambió de rol' WHERE " + columna + "=? AND estado IN ('PENDIENTE','APROBADO')", idUsuario);
                ejecutar(c, "UPDATE reservas SET estado='CANCELADA' WHERE " + columna + "=? AND estado='ACTIVA'", idUsuario);
                ejecutar(c, "UPDATE solicitudes_calendario SET estado='RECHAZADA' WHERE " + columna + "=? AND estado='PENDIENTE'", idUsuario);
                ejecutar(c, "DELETE FROM solicitudes_tutor WHERE idTutor=?", idUsuario);
                if (tutor) ejecutar(c, "UPDATE usuarios SET tipoUsuario='TUTOR' WHERE idUsuario=? AND tipoUsuario='ESTUDIANTE'", idUsuario);
                else ejecutar(c, "UPDATE usuarios SET tipoUsuario='ESTUDIANTE',descripcion='',banner='' WHERE idUsuario=? AND tipoUsuario='TUTOR'", idUsuario);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo cambiar el tipo de cuenta.", e); }
    }
    private static void ejecutar(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { p.setInt(1,id); p.executeUpdate(); }
    }
}
