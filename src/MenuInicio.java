import java.util.Scanner;

/**
 * Menú de inicio de la versión de consola (se activa con {@code --consola}): registro, inicio de sesión, reactivar cuenta y salir.
 */
public class MenuInicio {

    private final Scanner sc = new Scanner(System.in);
    private final RegistroConsola registro = new RegistroConsola(sc);
    private final SesionConsola sesion = new SesionConsola(sc);
    private final PerfilConsola perfil = new PerfilConsola(sc);

    public void iniciar() {
        int opcion = -1;

        while (opcion != 4) {
            mostrarOpciones();
            opcion = leerOpcion();
            ejecutarOpcion(opcion);
        }

        System.out.println("Programa finalizado.");
    }

    private void mostrarOpciones() {
        System.out.println("\n=== Menú ===");
        System.out.println("1. Registrarse");
        System.out.println("2. Iniciar sesión");
        System.out.println("3. Reactivar cuenta");
        System.out.println("4. Salir");
        System.out.print("Selecciona una opción: ");
    }

    private int leerOpcion() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void ejecutarOpcion(int opcion) {
        if (opcion == 1) {
            registro.registrar();
        } else if (opcion == 2) {
            sesion.iniciarSesion();
        } else if (opcion == 3) {
            perfil.reactivar();
        }
    }
}
 