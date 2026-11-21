public class ConfiguracionInicial {
    private ConfiguracionInicial() {}
    public static void preparar() { ConexionDB.crearTablas(); }
}
