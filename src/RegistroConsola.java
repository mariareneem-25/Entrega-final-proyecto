import java.util.Scanner;

/**
 * Pantalla de consola para registrar una cuenta nueva.
 */
public class RegistroConsola {

    private final Scanner sc;

    public RegistroConsola(Scanner sc) {
        this.sc = sc;
    }

    public void registrar() {
        System.out.print("Nombre: ");
        String nombre = sc.nextLine();

        System.out.print("Correo institucional: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        System.out.print("Foto de perfil (ruta o nombre de archivo): ");
        String fotoPerfil = sc.nextLine();

        if (!Perfil_usuario.verificarCorreo(correo)) {
            System.out.println("Registro rechazado: el correo no pertenece al dominio institucional, "
                    + "o ya fue utilizado para crear una cuenta.");
            return;
        }

        Perfil_usuario usuario = Perfil_usuario.crearUsuario(nombre, correo, contrasena, fotoPerfil);

        if (usuario != null) {
            System.out.println("Cuenta creada correctamente:");
            System.out.println(usuario);
        } else {
            System.out.println("No se pudo guardar la cuenta en la base de datos.");
        }
    }

}
