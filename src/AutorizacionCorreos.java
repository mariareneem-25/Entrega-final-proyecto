import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Correos institucionales con un tipo de cuenta previamente autorizado.
 *
 * <p>Cualquier correo @uvg.edu.gt puede registrarse como estudiante o tutor, pero las cuentas de
 * docente y director de carrera solo se crean para correos que un director autorizó antes. Así nadie
 * puede elegir un rol con más permisos desde el formulario de registro: el rol autorizado siempre
 * prevalece sobre el que envía el navegador.</p>
 */
public final class AutorizacionCorreos {

    /** Correos autorizados desde el primer arranque para poder crear las primeras cuentas. */
    private static final List<String[]> INICIALES = List.of(
            new String[]{"estudiante1@uvg.edu.gt", "estudiante"},
            new String[]{"director1@uvg.edu.gt", "director"},
            new String[]{"docente1@uvg.edu.gt", "docente"});

    private AutorizacionCorreos() { }

    /** Crea la tabla si no existe y agrega los correos iniciales sin duplicarlos. */
    public static void preparar() {
        WebDatos.ejecutar("CREATE TABLE IF NOT EXISTS correos_autorizados (correo TEXT PRIMARY KEY, tipo TEXT NOT NULL)");
        for (String[] inicial : INICIALES) {
            WebDatos.ejecutar("INSERT OR IGNORE INTO correos_autorizados(correo, tipo) VALUES(?, ?)", inicial[0], inicial[1]);
        }
        // Al desplegar, la propiedad nexo.directores (correos separados por coma) autoriza a los directores
        // reales, para no depender del correo de ejemplo director1@uvg.edu.gt.
        for (String correo : System.getProperty("nexo.directores", "").split(",")) {
            String normalizado = correo.trim().toLowerCase(Locale.ROOT);
            if (!normalizado.isEmpty() && Seguridad.esCorreoInstitucional(normalizado)) {
                WebDatos.ejecutar("INSERT INTO correos_autorizados(correo, tipo) VALUES(?, 'director') "
                        + "ON CONFLICT(correo) DO UPDATE SET tipo = 'director'", normalizado);
            }
        }
    }

    /** Tipo autorizado para el correo ("estudiante", "docente" o "director"), si existe. */
    public static Optional<String> tipoDe(String correo) {
        if (correo == null) return Optional.empty();
        return WebDatos.consultar("SELECT tipo FROM correos_autorizados WHERE correo = ?", normalizar(correo))
                .stream().findFirst().map(fila -> fila.get("tipo").toString().toLowerCase(Locale.ROOT));
    }

    /**
     * Rol con el que se crea la cuenta. Un correo autorizado como docente o director
     * siempre obtiene ese rol; en cualquier otro caso se respeta lo elegido (estudiante o tutor).
     */
    public static Perfil_usuario.TipoUsuario rolPara(String correo, Perfil_usuario.TipoUsuario elegido) {
        String tipo = tipoDe(correo).orElse("");
        if (tipo.equals("director")) return Perfil_usuario.TipoUsuario.DIRECTOR;
        if (tipo.equals("docente")) return Perfil_usuario.TipoUsuario.DOCENTE;
        return elegido;
    }

    /**
     * Autoriza un correo para un tipo de cuenta. No se puede autorizar un correo que ya tiene cuenta,
     * porque el cambio no afectaría a esa cuenta y confundiría a quien administra.
     *
     * @throws IllegalArgumentException si el correo o el tipo no son válidos
     * @throws IllegalStateException si el correo ya tiene una cuenta creada
     */
    public static void autorizar(String correo, String tipo) {
        String normalizado = normalizar(correo);
        if (!Seguridad.esCorreoInstitucional(normalizado)) {
            throw new IllegalArgumentException("Usa un correo institucional @uvg.edu.gt.");
        }
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if (!List.of("estudiante", "docente", "director").contains(tipoNormalizado)) {
            throw new IllegalArgumentException("El tipo debe ser estudiante, docente o director.");
        }
        if (ConexionDB.existe("SELECT 1 FROM usuarios WHERE lower(correoElectronico) = ?", normalizado)) {
            throw new IllegalStateException("Ese correo ya tiene una cuenta creada.");
        }
        WebDatos.ejecutar("INSERT INTO correos_autorizados(correo, tipo) VALUES(?, ?) "
                + "ON CONFLICT(correo) DO UPDATE SET tipo = excluded.tipo", normalizado, tipoNormalizado);
    }

    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
