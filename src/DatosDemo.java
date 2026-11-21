import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Datos ficticios opcionales. --demo siempre usa sus propias bases de datos. */
public final class DatosDemo {
    private DatosDemo() { }
    public static void preparar() throws Exception {
        if (ConexionDB.existe("SELECT 1 FROM usuarios LIMIT 1")) return;
        int estudiante = cuenta("Sofía Morales", "estudiante.demo@uvg.edu.gt", "ESTUDIANTE", "Ingeniería en Sistemas · Segundo año. Aprendiendo paso a paso, con ganas de entender el porqué.");
        int tutor = cuenta("Andrés Rivera", "tutor.demo@uvg.edu.gt", "TUTOR", "Estudiante de cuarto año de Ingeniería. Te ayudo a entender los conceptos con ejemplos, práctica y mucha paciencia.");
        int tutor2 = cuenta("Valeria Méndez", "tutora.demo@uvg.edu.gt", "TUTOR", "Me apasionan las ciencias y enseñar. Trabajemos juntos para que llegues con confianza a tu próximo parcial.");
        int docente = cuenta("Dra. Elena García", "docente.demo@uvg.edu.gt", "DOCENTE", "Docente de la Facultad de Ingeniería. Acompañando el aprendizaje entre pares.");
        int director = cuenta("Luis Méndez", "director.demo@uvg.edu.gt", "DIRECTOR", "Dirección académica y acompañamiento del programa de tutorías.");
        int otro = cuenta("Daniel López", "daniel.demo@uvg.edu.gt", "ESTUDIANTE", "Me gustaría reforzar programación antes del parcial.");
        int aspirante = cuenta("Camila Reyes", "aspirante.demo@uvg.edu.gt", "TUTOR", "Estudiante de Ingeniería con experiencia como auxiliar de laboratorio.");
        aprobar(tutor, "Cálculo 1, Programación Orientada a Objetos, Matemática Discreta", docente);
        aprobar(tutor2, "Física 1, Química General, Cálculo 1", docente);
        WebDatos.ejecutar("INSERT INTO solicitudes_tutor(idTutor,estado,nombreCompleto,descripcion,materias,fotoPerfil,banner,idRevisor) SELECT idUsuario,'PENDIENTE',nombre,descripcion,'Física 1, Álgebra Lineal',fotoPerfil,banner,? FROM usuarios WHERE idUsuario=?", docente, aspirante);
        WebDatos.ejecutar("INSERT INTO solicitudes(idEstudiante,idTutor,materia,mensaje,estado) VALUES(?,?,?,?, 'APROBADO')", estudiante, tutor, "Cálculo 1", "Quiero repasar derivadas y prepararme para el próximo parcial.");
        WebDatos.ejecutar("INSERT INTO solicitudes(idEstudiante,idTutor,materia,mensaje,estado) VALUES(?,?,?,?, 'PENDIENTE')", otro, tutor, "Programación Orientada a Objetos", "Necesito practicar herencia e interfaces. ¿Podemos trabajar un ejemplo juntos?");
        LocalDate hoy = LocalDate.now();
        WebDatos.ejecutar("INSERT INTO reservas(idEstudiante,idTutor,fecha,hora,estado,modalidad,lugar) VALUES(?,?,?,'15:00','ACTIVA','VIRTUAL','Sala virtual del chat')", estudiante, tutor, hoy.plusDays(2).toString());
        WebDatos.ejecutar("INSERT INTO reservas(idEstudiante,idTutor,fecha,hora,estado,modalidad,lugar) VALUES(?,?,?,'10:00','ACTIVA','PRESENCIAL','Biblioteca UVG')", estudiante, tutor, hoy.minusDays(3).toString());
        new CalendarioDAO().guardarMarcas(tutor, java.util.List.of(hoy.plusDays(1), hoy.plusDays(4)), CalendarioDAO.TipoMarca.PREFERENCIA);
        new CalendarioDAO().guardarMarcas(tutor2, java.util.List.of(hoy.plusDays(3)), CalendarioDAO.TipoMarca.PREFERENCIA);
        WebDatos.ejecutar("INSERT INTO logros(idUsuario,tema,completado,tipo,fechaObjetivo) VALUES(?, 'Comprender la regla de la cadena',1,'TEMA',NULL)", estudiante);
        WebDatos.ejecutar("INSERT INTO logros(idUsuario,tema,completado,tipo,fechaObjetivo) VALUES(?, 'Resolver 10 ejercicios de derivadas',0,'META',?)", estudiante, hoy.plusDays(3).toString());
        WebDatos.ejecutar("INSERT INTO logros(idUsuario,tema,completado,tipo,fechaObjetivo) VALUES(?, 'Preparar dudas para la tutoría',0,'PENDIENTE',?)", estudiante, hoy.plusDays(1).toString());
        WebDatos.ejecutar("INSERT INTO resenas_tutor(idEstudiante,idTutor,puntuacion,texto,fecha) VALUES(?,?,5,?,?)", estudiante, tutor, "Explica con paciencia y adapta los ejemplos a lo que necesito. ¡Me ayudó mucho!", hoy.minusDays(2).toString());
        Perfil_usuario pe = Perfil_usuario.buscarPorId(estudiante), pt = Perfil_usuario.buscarPorId(tutor);
        ServicioChat servicio = new ServicioChat(new UsuarioRepositorioPerfil());
        Conversacion c = servicio.iniciarConversacion((Estudiante) AdaptadorUsuario.convertir(pe), (Tutor) AdaptadorUsuario.convertir(pt));
        servicio.enviarTexto(c, AdaptadorUsuario.convertir(pt), "¡Hola, Sofía! Ya acepté tu solicitud. Cuéntame qué temas te gustaría repasar primero.");
        servicio.enviarTexto(c, AdaptadorUsuario.convertir(pe), "¡Hola! Me cuesta identificar cuándo usar la regla de la cadena. ¿Podemos empezar por ahí?");
        servicio.enviarTexto(c, AdaptadorUsuario.convertir(pt), "¡Claro! Preparo algunos ejemplos y los resolvemos paso a paso. También puedes enviarme tus ejercicios por aquí.");
        Foro foro = new Foro();
        Publicacion pregunta = new Publicacion(AdaptadorUsuario.convertir(pe), "¿Cómo identifican cuándo aplicar la regla de la cadena?", "Entiendo los ejemplos sencillos, pero cuando hay funciones dentro de otras me confundo. ¿Tienen algún consejo o forma de reconocerlas?", false);
        foro.publicar(pregunta);
        foro.responder(pregunta, new Comentario(AdaptadorUsuario.convertir(pt), "Intenta separar la función exterior de la interior. Por ejemplo, en sen(x²), la exterior es sen(u) y la interior es x². ¡Dibuja las dos capas!", false));
        foro.publicar(new Publicacion(AdaptadorUsuario.convertir(Perfil_usuario.buscarPorId(otro)), "Ideas para practicar herencia e interfaces en Java", "Estoy buscando un ejercicio pequeño para entender mejor cuándo usar una clase abstracta y cuándo una interfaz. ¿Qué ejemplos recomiendan?", true));
        System.out.println("Modo demostración: estudiante.demo / tutor.demo / tutora.demo / docente.demo / director.demo / aspirante.demo @uvg.edu.gt. Contraseña: Nexo2026!");
    }
    private static int cuenta(String nombre, String correo, String rol, String descripcion) throws Exception {
        WebDatos.ejecutar("INSERT INTO usuarios(nombre,correoElectronico,contrasena,fotoPerfil,descripcion,banner,tipoUsuario,estadoCuenta) VALUES(?,?,?,'',?,'',?,'ACTIVA')", nombre, correo, ClaveSegura.proteger("Nexo2026!"), descripcion, rol);
        int id = ((Number) WebDatos.consultar("SELECT idUsuario FROM usuarios WHERE correoElectronico=?", correo).get(0).get("idUsuario")).intValue();
        String foto = imagen(id, nombre, false), banner = imagen(id, nombre, true);
        WebDatos.ejecutar("UPDATE usuarios SET fotoPerfil=?,banner=? WHERE idUsuario=?", foto, banner, id);
        return id;
    }
    private static void aprobar(int id, String materias, int revisor) {
        WebDatos.ejecutar("INSERT INTO solicitudes_tutor(idTutor,estado,nombreCompleto,descripcion,materias,fotoPerfil,banner,idRevisor,aprobadoPor) SELECT idUsuario,'APROBADO',nombre,descripcion,?,fotoPerfil,banner,?,? FROM usuarios WHERE idUsuario=?", materias, revisor, revisor, id);
    }
    private static String imagen(int propietario, String nombre, boolean banner) throws Exception {
        int w = banner ? 1000 : 200, h = banner ? 260 : 200;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = propietario % 2 == 0 ? new Color(108, 85, 151) : new Color(65, 121, 106);
        g.setPaint(new GradientPaint(0, 0, base, w, h, banner ? new Color(209, 193, 232) : base.brighter())); g.fillRect(0, 0, w, h);
        if (banner) { g.setColor(new Color(255,255,255,35)); for (int i=0;i<5;i++) g.fillOval(w/2+i*110, -100+i*25, 270, 270); }
        else { g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 64)); String[] n=nombre.split(" "); String iniciales=n[0].substring(0,1)+n[n.length-1].substring(0,1); FontMetrics fm=g.getFontMetrics(); g.drawString(iniciales,(w-fm.stringWidth(iniciales))/2,124); }
        g.dispose();
        String id = UUID.randomUUID().toString();
        Path dir = Path.of(System.getProperty("nexo.archivos", "archivos_web")).normalize(); Files.createDirectories(dir);
        Path ruta = dir.resolve(id + ".png"); ImageIO.write(img, "png", ruta.toFile());
        WebDatos.ejecutar("INSERT INTO web_archivos VALUES(?,?,'PERFIL','',?,?,?)", id, propietario, banner ? "banner.png" : "perfil.png", ruta.toString(), "image/png");
        return "/archivo/" + id;
    }
}
