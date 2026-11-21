/**
 * Contrato para obtener usuarios del dominio por su identificador, sin depender de cómo se guardan.
 */
public interface UsuarioRepositorio {
    Usuario obtenerPorId(String idUsuario);
}

