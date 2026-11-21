import java.util.List;
import java.util.Scanner;

/**
 * Pantalla de consola para iniciar sesión y mostrar el menú de la cuenta.
 */
public class SesionConsola {

    private final Scanner sc;
    private final UsuarioRepositorio repositorioUsuarios = new UsuarioRepositorioPerfil();
    private final Foro foro = new Foro();
    private final ServicioChat servicioChat = new ServicioChat(repositorioUsuarios);
    private final Controlaracceso acceso = new Controlaracceso();
    private final PerfilConsola perfil;
    private final ForoConsola foroConsola;
    private final TutoriaConsola tutoria;
    private final ChatConsola chat;
    private final CalendarioConsola calendario;
    private final SolicitudTutorConsola solicitudTutor;

    public SesionConsola(Scanner sc) {
        this.sc = sc;
        this.perfil = new PerfilConsola(sc);
        this.foroConsola = new ForoConsola(sc, foro, repositorioUsuarios);
        this.tutoria = new TutoriaConsola(sc, servicioChat);
        this.chat = new ChatConsola(sc, servicioChat);
        this.calendario = new CalendarioConsola(sc);
        this.solicitudTutor = new SolicitudTutorConsola(sc);
    }

    public void iniciarSesion() {
        System.out.print("Correo: ");
        String correo = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        Perfil_usuario sesion = Perfil_usuario.iniciarSesion(correo, contrasena);

        if (sesion == null) {
            System.out.println("No se pudo iniciar sesión. Verifica tu correo, tu contraseña o el estado de tu cuenta.");
            return;
        }

        System.out.println("Sesión iniciada como: " + sesion.getNombre());

        if (sesion.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR
                && !solicitudTutor.gestionarAcceso(sesion)) {
            sesion.cerrarSesion();
            return;
        }

        Usuario usuarioActual = AdaptadorUsuario.convertir(sesion);

        if (usuarioActual instanceof Tutor) {
            System.out.println("Estado de tu solicitud de tutor: " + ((Tutor) usuarioActual).getEstadoSolicitud());
        }

        abrirMenu(sesion, usuarioActual);
        sesion.cerrarSesion();
    }

    private void abrirMenu(Perfil_usuario sesion, Usuario usuarioActual) {
        List<String> opciones = MenuPrincipal.opcionesPara(sesion, acceso);
        boolean salir = false;

        while (!salir) {
            mostrarOpciones(opciones);
            int opcion = leerOpcion();

            if (opcion >= 1 && opcion <= opciones.size()) {
                salir = ejecutarOpcion(opciones.get(opcion - 1), sesion, usuarioActual);
            }
        }
    }

    private void mostrarOpciones(List<String> opciones) {
        System.out.println();
        for (int i = 0; i < opciones.size(); i++) {
            System.out.println((i + 1) + ". " + opciones.get(i));
        }
        System.out.print("Selecciona una opción: ");
    }

    private int leerOpcion() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean ejecutarOpcion(String opcion, Perfil_usuario sesion, Usuario usuarioActual) {
        switch (opcion) {
            case MenuPrincipal.ACTUALIZAR_PERFIL:
                return perfil.actualizar(sesion);
            case MenuPrincipal.ELIMINAR_PERFIL:
                return perfil.eliminar(sesion);
            case MenuPrincipal.FORO:
                foroConsola.abrir(usuarioActual);
                return false;
            case MenuPrincipal.BUSCAR_TUTORES:
                tutoria.buscarTutores(sesion);
                return false;
            case MenuPrincipal.MIS_SOLICITUDES:
                tutoria.misSolicitudesYLogros(sesion);
                return false;
            case MenuPrincipal.SOLICITUDES_ESTUDIANTES:
                tutoria.solicitudesDeEstudiantes(sesion);
                return false;
            case MenuPrincipal.CHAT:
                chat.abrir(usuarioActual, sesion);
                return false;
            case MenuPrincipal.SOLICITUDES:
                solicitudTutor.revisarSolicitudes(sesion);
                return false;
            case MenuPrincipal.CALENDARIO:
                calendario.ver(sesion);
                return false;
            case MenuPrincipal.CERRAR_SESION:
                return true;
            default:
                return false;
        }
    }
}
 