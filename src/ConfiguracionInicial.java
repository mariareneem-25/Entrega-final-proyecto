/**
 * Prepara la base de datos al iniciar la aplicación creando las tablas necesarias.
 */
public class ConfiguracionInicial {
    private ConfiguracionInicial() {}
    public static void preparar() { ConexionDB.crearTablas(); }
}
