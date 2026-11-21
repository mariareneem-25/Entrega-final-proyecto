/**
 * Estados por los que pasa una solicitud: PENDIENTE, APROBADO, RECHAZADO y BORRADOR.
 * BORRADOR solo se usa en la solicitud de tutor que se guardó sin enviar.
 */
public enum EstadoSolicitud {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    BORRADOR        // solo para la solicitud de tutor: se guardan los datos de la socitud pero no se envian todavia
}
