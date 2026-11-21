import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Controlador HTTP. La identidad siempre procede de la sesión, nunca del formulario. */
public final class WebApi {
    private final SolicitudTutorDAO validaciones = new SolicitudTutorDAO();
    private final Gestionarsolicitudes solicitudes = new Gestionarsolicitudes();
    private final CalendarioDAO calendario = new CalendarioDAO();
    private final ServicioChat chat = new ServicioChat(new UsuarioRepositorioPerfil());
    private final Controlaracceso acceso = new Controlaracceso();

    public void atender(HttpExchange x) throws IOException {
        String ruta = x.getRequestURI().getPath().substring(5);
        Map<String, String> f = ServidorWeb.parametros(x);
        boolean get = "GET".equals(x.getRequestMethod());
        if (get && ruta.equals("sesion")) {
            ServidorWeb.Sesion s = ServidorWeb.sesion(x, false);
            ServidorWeb.json(x, 200, s == null ? WebDatos.mapa("usuario", null) : estado(s)); return;
        }
        if (!get && Set.of("login", "registro", "reactivar").contains(ruta)) {
            autenticar(x, ruta, f); return;
        }
        ServidorWeb.Sesion s = ServidorWeb.sesion(x, true);
        Perfil_usuario u = usuario(s);
        if (get) {
            Object respuesta;
            switch (ruta) {
                case "chat": respuesta = detalleChat(u, texto(f, "id", 100)); break;
                case "perfil": respuesta = detallePerfil(u, numero(f, "id")); break;
                default: throw new ServidorWeb.ErrorHttp(404, "No existe esa consulta.");
            }
            ServidorWeb.json(x, 200, respuesta); return;
        }
        if (!s.csrf.equals(x.getRequestHeaders().getFirst("X-CSRF-Token"))) throw new ServidorWeb.ErrorHttp(403, "La sesión cambió. Actualiza la página.");
        if (ruta.equals("logout")) {
            ServidorWeb.cerrarSesion(x); ServidorWeb.json(x, 200, WebDatos.mapa("ok", true)); return;
        }
        String mensaje = "Cambios guardados.";
        switch (ruta) {
            case "perfil":
                String foto = opcional(f, "foto", 200), banner = opcional(f, "banner", 200);
                validarImagen(u, foto); validarImagen(u, banner);
                exigir(u.actualizarSoloNombre(texto(f, "nombre", 100)), "No se pudo guardar el nombre.");
                exigir(u.actualizarSoloFoto(foto), "No se pudo guardar la foto.");
                exigir(u.actualizarInformacionPerfil(opcional(f, "descripcion", 3000), banner), "No se pudo guardar el perfil.");
                break;
            case "clave":
                Seguridad.cambiarContrasena(u, texto(f, "actual", 200), texto(f, "nueva", 200), texto(f, "confirmacion", 200));
                ServidorWeb.invalidar(u.getIdUsuario()); ServidorWeb.iniciarSesion(x, u.getIdUsuario());
                mensaje = "Contraseña actualizada. Se cerraron las otras sesiones."; break;
            case "eliminar-cuenta":
                verificarClave(u, texto(f, "clave", 200));
                exigir(texto(f, "confirmacion", 20).equals("ELIMINAR"), "Escribe ELIMINAR para confirmar.");
                exigir(u.eliminarPerfil(), "No se pudo desactivar la cuenta.");
                ServidorWeb.invalidar(u.getIdUsuario()); ServidorWeb.cerrarSesion(x); break;
            case "rol":
                rol(u, "ESTUDIANTE", "TUTOR"); verificarClave(u, texto(f, "clave", 200));
                exigir(texto(f, "confirmacion", 20).equals("CAMBIAR"), "Escribe CAMBIAR para confirmar.");
                if (u.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE) CambioRol.aTutor(u.getIdUsuario());
                else CambioRol.aEstudiante(u.getIdUsuario());
                ServidorWeb.invalidar(u.getIdUsuario()); ServidorWeb.iniciarSesion(x, u.getIdUsuario());
                mensaje = "Tipo de cuenta actualizado."; break;
            case "autorizacion":
                rol(u, "DIRECTOR");
                AutorizacionCorreos.autorizar(texto(f, "correo", 120), texto(f, "tipo", 20));
                mensaje = "Correo autorizado. Ya puede crear su cuenta."; break;
            case "tutor-borrador": case "tutor-enviar":
                guardarSolicitudTutor(u, f, ruta.equals("tutor-enviar"));
                mensaje = ruta.equals("tutor-enviar") ? "Solicitud enviada al revisor seleccionado." : "Borrador guardado."; break;
            case "tutor-revisar":
                rol(u, "DOCENTE", "DIRECTOR");
                SolicitudTutor st = validaciones.obtener(numero(f, "id"));
                exigir(st != null && st.getEstado() == EstadoSolicitud.PENDIENTE && Objects.equals(st.getIdRevisor(), u.getIdUsuario()), "No puedes revisar esta solicitud.");
                exigir(Perfil_usuario.buscarPorId(st.getIdTutor()).getEstadoCuenta() == Perfil_usuario.EstadoCuenta.ACTIVA, "La cuenta del solicitante no está activa.");
                boolean aceptar = booleano(f, "aceptar");
                int calificacionRevision = aceptar ? calificacionOpcional(f) : 0;
                exigir(aceptar ? validaciones.aprobar(st, u.getIdUsuario()) : validaciones.rechazar(st.getIdTutor(), u.getIdUsuario(), texto(f, "motivo", 1500)), "La solicitud ya fue respondida.");
                if (calificacionRevision > 0) registrarCalificacion(st.getIdTutor(), calificacionRevision, u.getIdUsuario());
                mensaje = aceptar ? "Tutor aprobado." : "Solicitud rechazada con su motivo."; break;
            case "tutor-calificar":
                rol(u, "DOCENTE", "DIRECTOR");
                int tutorCalificado = numero(f, "id"), nota = calificacionOpcional(f);
                exigir(nota > 0, "Indica una calificación del 1 al 10.");
                exigir(ConexionDB.existe("SELECT 1 FROM solicitudes_tutor WHERE idTutor=? AND estado='APROBADO'", tutorCalificado), "Solo se puede calificar a un tutor aprobado.");
                registrarCalificacion(tutorCalificado, nota, u.getIdUsuario());
                mensaje = "Calificación guardada. Reemplaza a la anterior."; break;
            case "solicitud":
                rol(u, "ESTUDIANTE");
                int tutor = numero(f, "tutor"); String materia = texto(f, "materia", 120);
                SolicitudTutor ficha = validaciones.obtener(tutor);
                exigir(ficha != null && ficha.getEstado() == EstadoSolicitud.APROBADO, "El tutor no está aprobado.");
                exigir(Arrays.stream(Objects.toString(ficha.getMaterias(), "").split(",")).anyMatch(m -> m.trim().equalsIgnoreCase(materia)), "Selecciona una materia que imparte este tutor.");
                solicitudes.crearSolicitud(u, tutor, materia, texto(f, "mensaje", 3000)); mensaje = "Solicitud de tutoría enviada."; break;
            case "solicitud-responder":
                tutorAprobado(u);
                int solicitud = numero(f, "id");
                if (booleano(f, "aceptar")) {
                    solicitudes.aceptarSolicitud(u, solicitud);
                    Map<String, Object> par = primera(WebDatos.consultar("SELECT idEstudiante FROM solicitudes WHERE idSolicitud=? AND idTutor=?", solicitud, u.getIdUsuario()));
                    chat.iniciarConversacion((Estudiante) AdaptadorUsuario.convertir(Perfil_usuario.buscarPorId(((Number) par.get("idEstudiante")).intValue())), (Tutor) AdaptadorUsuario.convertir(u));
                    mensaje = "Tutoría aceptada. El chat ya está habilitado.";
                } else { solicitudes.rechazarSolicitud(u, solicitud, texto(f, "motivo", 1500)); mensaje = "Solicitud rechazada."; }
                break;
            case "mensaje":
                Conversacion c = conversacion(u, texto(f, "chat", 100));
                chat.enviarTexto(c, AdaptadorUsuario.convertir(u), texto(f, "texto", 6000)); mensaje = "Mensaje enviado."; break;
            case "reunion":
                Conversacion reunion = conversacion(u, texto(f, "chat", 100));
                String url = "https://meet.jit.si/NexoUVG-" + reunion.getId();
                chat.enviarTexto(reunion, AdaptadorUsuario.convertir(u), "Sala virtual de tutoría (servicio externo Jitsi Meet): " + url);
                ServidorWeb.json(x, 200, WebDatos.mapa("ok", true, "url", url, "mensaje", "Enlace compartido en el chat.")); return;
            case "archivo":
                ServidorWeb.json(x, 200, subir(u, f)); return;
            case "marca":
                rol(u, "ESTUDIANTE", "TUTOR");
                LocalDate dia = fecha(f, "fecha");
                String marca = texto(f, "tipo", 20);
                exigir(Set.of("OCUPADO", "PREFERENCIA", "QUITAR").contains(marca), "Tipo de marca inválido.");
                if (marca.equals("QUITAR")) calendario.eliminarMarcas(u.getIdUsuario(), List.of(dia));
                else calendario.guardarMarcas(u.getIdUsuario(), List.of(dia), CalendarioDAO.TipoMarca.valueOf(marca));
                break;
            case "fecha":
                Conversacion cf = conversacion(u, texto(f, "chat", 100));
                String modalidad = texto(f, "modalidad", 20);
                exigir(Set.of("VIRTUAL", "PRESENCIAL").contains(modalidad), "Modalidad inválida.");
                String lugar = texto(f, "lugar", 300);
                int prop = calendario.solicitarReserva(u.getIdUsuario(), Integer.parseInt(cf.getEstudiante().getIdUsuario()), Integer.parseInt(cf.getTutor().getIdUsuario()), fecha(f, "fecha"), hora(f, "hora"));
                WebDatos.ejecutar("UPDATE solicitudes_calendario SET modalidad=?,lugar=? WHERE idSolicitudCal=?", modalidad, lugar, prop);
                mensaje = "Fecha propuesta. Aparecerá en ambos calendarios cuando la otra persona la acepte."; break;
            case "fecha-responder":
                int idProp = numero(f, "id");
                Map<String, Object> propuesta = primera(WebDatos.consultar("SELECT * FROM solicitudes_calendario WHERE idSolicitudCal=? AND estado='PENDIENTE'", idProp));
                validarPar(u, ((Number) propuesta.get("idEstudiante")).intValue(), ((Number) propuesta.get("idTutor")).intValue());
                mensaje = calendario.responder(idProp, u.getIdUsuario(), booleano(f, "aceptar"));
                if (booleano(f, "aceptar") && propuesta.get("tipo").equals("RESERVAR")) {
                    WebDatos.ejecutar("UPDATE reservas SET modalidad=?,lugar=? WHERE idEstudiante=? AND idTutor=? AND fecha=? AND hora=? AND estado='ACTIVA'", propuesta.get("modalidad"), propuesta.get("lugar"), propuesta.get("idEstudiante"), propuesta.get("idTutor"), propuesta.get("fecha"), propuesta.get("hora"));
                }
                break;
            case "cancelar-reserva":
                Map<String, Object> reserva = primera(WebDatos.consultar("SELECT * FROM reservas WHERE idReserva=? AND estado='ACTIVA'", numero(f, "id")));
                validarPar(u, ((Number) reserva.get("idEstudiante")).intValue(), ((Number) reserva.get("idTutor")).intValue());
                calendario.solicitarEliminacion(u.getIdUsuario(), numero(f, "id")); mensaje = "Cancelación solicitada. La otra persona debe aceptarla en el chat."; break;
            case "logro":
                rol(u, "ESTUDIANTE");
                String tema = texto(f, "tema", 300), categoria = texto(f, "tipo", 20), objetivo = opcional(f, "fecha", 20);
                exigir(Set.of("TEMA", "META", "PENDIENTE", "FECHA").contains(categoria), "Tipo de progreso inválido.");
                if (!objetivo.isEmpty()) fecha(f, "fecha");
                agregarLogro(u, tema, categoria, objetivo); break;
            case "logro-estado":
                rol(u, "ESTUDIANTE");
                exigir(WebDatos.ejecutar("UPDATE logros SET completado=? WHERE idLogro=? AND idUsuario=?", booleano(f, "completado") ? 1 : 0, numero(f, "id"), u.getIdUsuario()) == 1, "No existe ese logro."); break;
            case "logro-eliminar":
                rol(u, "ESTUDIANTE");
                exigir(WebDatos.ejecutar("DELETE FROM logros WHERE idLogro=? AND idUsuario=?", numero(f, "id"), u.getIdUsuario()) == 1, "No existe ese logro."); break;
            case "publicacion":
                rol(u, "ESTUDIANTE");
                new Foro().publicar(new Publicacion(AdaptadorUsuario.convertir(u), texto(f, "titulo", 180), texto(f, "texto", 6000), booleano(f, "anonimo")));
                mensaje = "Pregunta publicada."; break;
            case "comentario":
                rolForo(u);
                String post = texto(f, "id", 100), padre = opcional(f, "padre", 100);
                exigir(!WebDatos.foro("SELECT id FROM publicaciones WHERE id=?", post).isEmpty(), "La publicación no existe.");
                if (!padre.isEmpty()) exigir(!WebDatos.foro("SELECT id FROM comentarios WHERE id=? AND publicacion_id=?", padre, post).isEmpty(), "La respuesta no corresponde a esta publicación.");
                new ForoDAO().guardarComentario(new Comentario(AdaptadorUsuario.convertir(u), texto(f, "texto", 6000), booleano(f, "anonimo")), post, padre.isEmpty() ? null : padre);
                mensaje = "Respuesta publicada."; break;
            case "resena":
                rol(u, "ESTUDIANTE");
                int idTutor = numero(f, "tutor"), puntuacion = numero(f, "puntuacion");
                exigir(puntuacion >= 1 && puntuacion <= 5, "La puntuación debe ser de 1 a 5.");
                exigir(recibioTutoria(u.getIdUsuario(), idTutor), "Podrás dejar una reseña después de una tutoría confirmada y finalizada.");
                WebDatos.ejecutar("INSERT INTO resenas_tutor(idEstudiante,idTutor,puntuacion,texto,fecha,anonimo) VALUES(?,?,?,?,?,?) ON CONFLICT(idEstudiante,idTutor) DO UPDATE SET puntuacion=excluded.puntuacion,texto=excluded.texto,fecha=excluded.fecha,anonimo=excluded.anonimo", u.getIdUsuario(), idTutor, puntuacion, texto(f, "texto", 2000), LocalDateTime.now().toString(), booleano(f, "anonimo") ? 1 : 0);
                mensaje = "Reseña guardada."; break;
            default: throw new ServidorWeb.ErrorHttp(404, "No existe esa acción.");
        }
        ServidorWeb.json(x, 200, WebDatos.mapa("ok", true, "mensaje", mensaje));
    }

    private void autenticar(HttpExchange x, String ruta, Map<String, String> f) throws IOException {
        String correo = texto(f, "correo", 120).toLowerCase(Locale.ROOT), clave = texto(f, "clave", 200);
        ServidorWeb.limitarIntentos(correo);
        exigir(Seguridad.esCorreoInstitucional(correo), "Usa tu correo institucional @uvg.edu.gt.");
        if (ruta.equals("reactivar")) {
            exigir(Perfil_usuario.reactivarCuenta(correo, clave), "No se pudo reactivar. Verifica las credenciales y que la cuenta esté eliminada.");
            ServidorWeb.json(x, 200, WebDatos.mapa("ok", true, "mensaje", "Cuenta reactivada. Ya puedes iniciar sesión.")); return;
        }
        if (ruta.equals("registro")) {
            exigir(Seguridad.contrasenaValida(clave), Seguridad.requisitosContrasena());
            exigir(clave.equals(texto(f, "confirmacion", 200)), "Las contraseñas no coinciden.");
            exigir(Perfil_usuario.verificarCorreo(correo), "Este correo ya tiene una cuenta. Inicia sesión o reactívala.");
            String elegido = texto(f, "rol", 20);
            exigir(Set.of("ESTUDIANTE", "TUTOR").contains(elegido), "Selecciona estudiante o tutor.");
            // El rol autorizado para el correo (docente o director) prevalece sobre el enviado por el navegador.
            Perfil_usuario.TipoUsuario rol = AutorizacionCorreos.rolPara(correo, Perfil_usuario.TipoUsuario.valueOf(elegido));
            WebDatos.ejecutar("INSERT INTO usuarios(nombre,correoElectronico,contrasena,fotoPerfil,descripcion,banner,tipoUsuario,estadoCuenta) VALUES(?,?,?,'','','',?,'ACTIVA')", texto(f, "nombre", 100), correo, ClaveSegura.proteger(clave), rol.name());
        }
        Perfil_usuario u = Perfil_usuario.iniciarSesion(correo, clave);
        exigir(u != null, "Correo o contraseña incorrectos, o cuenta inactiva.");
        ServidorWeb.iniciarSesion(x, u.getIdUsuario());
        ServidorWeb.json(x, 200, WebDatos.mapa("ok", true));
    }

    private Perfil_usuario usuario(ServidorWeb.Sesion s) {
        Perfil_usuario u = Perfil_usuario.buscarPorId(s.usuario);
        if (u == null || u.getEstadoCuenta() != Perfil_usuario.EstadoCuenta.ACTIVA) throw new ServidorWeb.ErrorHttp(401, "La cuenta no está activa.");
        u.activarSesionWeb(); return u;
    }

    private Object estado(ServidorWeb.Sesion s) {
        Perfil_usuario u = usuario(s);
        int id = u.getIdUsuario();
        Map<String, Object> r = WebDatos.mapa("usuario", WebDatos.perfil(u, true), "csrf", s.csrf, "hoy", LocalDate.now().toString(), "ahora", LocalDateTime.now().toString());
        r.put("tutores", WebDatos.consultar("SELECT u.idUsuario AS id,u.nombre,u.descripcion,u.fotoPerfil AS foto,u.banner,s.materias, (SELECT ROUND(AVG(puntuacion),1) FROM resenas_tutor WHERE idTutor=u.idUsuario) AS promedio FROM usuarios u JOIN solicitudes_tutor s ON s.idTutor=u.idUsuario WHERE u.tipoUsuario='TUTOR' AND u.estadoCuenta='ACTIVA' AND s.estado='APROBADO' ORDER BY u.nombre"));
        r.put("revisores", WebDatos.consultar("SELECT idUsuario AS id,nombre,tipoUsuario AS rol FROM usuarios WHERE estadoCuenta='ACTIVA' AND tipoUsuario IN ('DOCENTE','DIRECTOR') ORDER BY nombre"));
        r.put("solicitudes", WebDatos.consultar(Solicitud.CONSULTA + "WHERE s.idEstudiante=? OR s.idTutor=? ORDER BY s.idSolicitud DESC", id, id));
        r.put("marcas", WebDatos.consultar("SELECT fecha,tipo FROM calendario_marcas WHERE idUsuario=? ORDER BY fecha", id));
        r.put("reservas", reservas(id));
        r.put("logros", WebDatos.consultar("SELECT idLogro AS id,tema,completado,tipo,fechaObjetivo AS fecha FROM logros WHERE idUsuario=? ORDER BY completado,idLogro DESC", id));
        boolean puedeForo = u.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE || u.getTipoUsuario() == Perfil_usuario.TipoUsuario.DOCENTE
                || (u.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR && Perfil_usuario.obtenerEstadoSolicitudTutor(id) == EstadoSolicitud.APROBADO);
        r.put("foro", puedeForo ? feed(u) : List.of());
        r.put("chats", listaChats(u));
        r.put("miSolicitud", u.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR ? WebDatos.consultar("SELECT * FROM solicitudes_tutor WHERE idTutor=?", id) : List.of());
        boolean revisor = Set.of("DOCENTE", "DIRECTOR").contains(u.getTipoUsuario().name());
        r.put("validaciones", revisor ? WebDatos.consultar("SELECT s.*,u.correoElectronico AS correo FROM solicitudes_tutor s JOIN usuarios u ON u.idUsuario=s.idTutor WHERE s.estado='PENDIENTE' AND s.idRevisor=? AND u.estadoCuenta='ACTIVA' ORDER BY s.idTutor", id) : List.of());
        r.put("tutoresAprobados", revisor ? WebDatos.consultar("SELECT s.idTutor AS id,u.nombre,s.materias,s.calificacion FROM solicitudes_tutor s JOIN usuarios u ON u.idUsuario=s.idTutor WHERE s.estado='APROBADO' AND u.estadoCuenta='ACTIVA' ORDER BY u.nombre") : List.of());
        r.put("autorizados", u.getTipoUsuario() == Perfil_usuario.TipoUsuario.DIRECTOR ? WebDatos.consultar("SELECT a.correo,a.tipo,EXISTS(SELECT 1 FROM usuarios x WHERE lower(x.correoElectronico)=a.correo) AS registrado FROM correos_autorizados a WHERE a.tipo IN ('docente','director') ORDER BY a.correo") : List.of());
        return r;
    }

    private List<Map<String, Object>> reservas(int id) {
        return WebDatos.consultar("SELECT r.*,e.nombre AS nombreEstudiante,t.nombre AS nombreTutor FROM reservas r JOIN usuarios e ON e.idUsuario=r.idEstudiante JOIN usuarios t ON t.idUsuario=r.idTutor WHERE r.estado='ACTIVA' AND (r.idEstudiante=? OR r.idTutor=?) ORDER BY r.fecha,r.hora", id, id);
    }
    private Object detallePerfil(Perfil_usuario u, int id) {
        Perfil_usuario otro = Perfil_usuario.buscarPorId(id);
        exigir(otro != null && otro.getEstadoCuenta() == Perfil_usuario.EstadoCuenta.ACTIVA, "El perfil no está disponible.");
        boolean publico = otro.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR && Perfil_usuario.obtenerEstadoSolicitudTutor(id) == EstadoSolicitud.APROBADO;
        exigir(publico || acceso.puedeVerInformacionDe(u, id), "No tienes acceso a este perfil.");
        boolean propio = u.getIdUsuario() == id;
        Map<String, Object> r = WebDatos.mapa("perfil", WebDatos.perfil(otro, propio), "materias", "", "marcas", WebDatos.consultar("SELECT fecha,tipo FROM calendario_marcas WHERE idUsuario=? AND fecha>=? ORDER BY fecha", id, LocalDate.now().toString()));
        List<Map<String, Object>> ocupadas = new ArrayList<>();
        for (Map<String, Object> reserva : reservas(id)) ocupadas.add(WebDatos.mapa("fecha", reserva.get("fecha"), "hora", reserva.get("hora")));
        r.put("reservas", ocupadas);
        if (publico) {
            SolicitudTutor ficha = validaciones.obtener(id); r.put("materias", Objects.toString(ficha.getMaterias(), ""));
            r.put("resenas", WebDatos.consultar("SELECT r.puntuacion,r.texto,r.fecha,CASE WHEN r.anonimo=1 THEN 'Estudiante anónimo' ELSE u.nombre END AS nombre FROM resenas_tutor r JOIN usuarios u ON u.idUsuario=r.idEstudiante WHERE r.idTutor=? ORDER BY r.fecha DESC", id));
            r.put("puedeResenar", u.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE && recibioTutoria(u.getIdUsuario(), id));
        }
        if (publico && Set.of("DOCENTE", "DIRECTOR").contains(u.getTipoUsuario().name())) {
            r.put("calificacion", primera(WebDatos.consultar("SELECT calificacion FROM solicitudes_tutor WHERE idTutor=?", id)).get("calificacion"));
        }
        if (acceso.puedeVerInformacionDe(u, id)) r.put("logros", WebDatos.consultar("SELECT tema,completado,tipo,fechaObjetivo AS fecha FROM logros WHERE idUsuario=?", id));
        return r;
    }

    private List<Map<String, Object>> feed(Perfil_usuario u) {
        if (u.getTipoUsuario() == Perfil_usuario.TipoUsuario.DIRECTOR) return List.of();
        List<Map<String, Object>> posts = WebDatos.foro("SELECT id,autor_id,anonimo,titulo,cuerpo AS texto,fecha FROM publicaciones ORDER BY fecha DESC");
        for (Map<String, Object> p : posts) {
            p.put("mia", p.get("autor_id").toString().equals(Integer.toString(u.getIdUsuario())));
            autorPublico(p);
            List<Map<String, Object>> comentarios = WebDatos.foro("SELECT id,comentario_padre_id AS padre,autor_id,anonimo,cuerpo AS texto,fecha FROM comentarios WHERE publicacion_id=? ORDER BY fecha,rowid", p.get("id"));
            for (Map<String, Object> c : comentarios) autorPublico(c);
            p.put("comentarios", comentarios);
            p.put("adjuntos", WebDatos.consultar("SELECT id,nombre,mime FROM web_archivos WHERE categoria='FORO' AND contexto=?", p.get("id")));
        }
        return posts;
    }
    private void autorPublico(Map<String, Object> p) {
        boolean anonimo = ((Number) p.get("anonimo")).intValue() == 1;
        Perfil_usuario autor = anonimo ? null : Perfil_usuario.buscarPorId(Integer.parseInt(p.get("autor_id").toString()));
        p.put("autor", anonimo ? "Estudiante anónimo" : autor == null ? "Usuario" : autor.getNombre());
        p.put("rol", anonimo || autor == null ? "ESTUDIANTE" : autor.getTipoUsuario().name());
        p.put("foto", autor == null ? "" : Objects.toString(autor.getFotoPerfil(), ""));
        p.remove("autor_id"); // El anonimato también se respeta en la respuesta de la API.
    }
    private List<Map<String, Object>> listaChats(Perfil_usuario u) {
        if (!(u.getTipoUsuario() == Perfil_usuario.TipoUsuario.ESTUDIANTE || u.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR)) return List.of();
        if (u.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR && Perfil_usuario.obtenerEstadoSolicitudTutor(u.getIdUsuario()) != EstadoSolicitud.APROBADO) return List.of();
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Conversacion c : chat.conversacionesDe(AdaptadorUsuario.convertir(u))) {
            int e = Integer.parseInt(c.getEstudiante().getIdUsuario()), t = Integer.parseInt(c.getTutor().getIdUsuario());
            if (!parActivo(e, t)) continue;
            Usuario otro = c.getOtroParticipante(AdaptadorUsuario.convertir(u));
            Perfil_usuario perfilOtro = Perfil_usuario.buscarPorId(Integer.parseInt(otro.getIdUsuario()));
            List<Mensaje> ms = c.getMensajes();
            Mensaje ultimo = ms.isEmpty() ? null : ms.get(ms.size() - 1);
            salida.add(WebDatos.mapa("id", c.getId(), "nombre", otro.getNombreCompleto(), "foto", perfilOtro == null ? "" : Objects.toString(perfilOtro.getFotoPerfil(), ""), "otro", Integer.parseInt(otro.getIdUsuario()), "ultimo", ultimo == null ? "Ya pueden conversar y acordar una fecha." : Objects.toString(ultimo.getContenido(), "Archivo adjunto"), "fecha", ultimo == null ? c.getFechaCreacion().toString() : ultimo.getFecha().toString()));
        }
        salida.sort((a,b) -> b.get("fecha").toString().compareTo(a.get("fecha").toString()));
        return salida;
    }
    private Object detalleChat(Perfil_usuario u, String id) {
        Conversacion c = conversacion(u, id);
        int e = Integer.parseInt(c.getEstudiante().getIdUsuario()), t = Integer.parseInt(c.getTutor().getIdUsuario());
        List<Map<String, Object>> mensajes = new ArrayList<>();
        for (Mensaje m : c.getMensajes()) {
            String archivo = registrarLegado(m.getRutaArchivo(), "CHAT", id, Integer.parseInt(m.getRemitente().getIdUsuario()));
            String nombreArchivo = "";
            if (!archivo.isEmpty()) nombreArchivo = Objects.toString(primera(WebDatos.consultar("SELECT nombre FROM web_archivos WHERE id=?", archivo.substring(9))).get("nombre"), "Archivo");
            Perfil_usuario emisor = Perfil_usuario.buscarPorId(Integer.parseInt(m.getRemitente().getIdUsuario()));
            mensajes.add(WebDatos.mapa("id", m.getId(), "emisor", Integer.parseInt(m.getRemitente().getIdUsuario()), "foto", emisor == null ? "" : Objects.toString(emisor.getFotoPerfil(), ""), "nombre", emisor == null ? "Usuario" : emisor.getNombre(), "texto", m.getContenido(), "tipo", m.getTipo().name(), "archivo", archivo, "nombreArchivo", nombreArchivo, "fecha", m.getFecha().toString()));
        }
        return WebDatos.mapa("mensajes", mensajes, "propuestas", WebDatos.consultar("SELECT * FROM solicitudes_calendario WHERE idEstudiante=? AND idTutor=? ORDER BY idSolicitudCal DESC", e, t), "reservas", WebDatos.consultar("SELECT * FROM reservas WHERE idEstudiante=? AND idTutor=? AND estado='ACTIVA' ORDER BY fecha,hora", e, t));
    }
    private Conversacion conversacion(Perfil_usuario u, String id) {
        Conversacion c = new ChatDAO().buscarPorId(id, new UsuarioRepositorioPerfil()).orElseThrow(() -> new ServidorWeb.ErrorHttp(404, "La conversación no existe."));
        validarPar(u, Integer.parseInt(c.getEstudiante().getIdUsuario()), Integer.parseInt(c.getTutor().getIdUsuario()));
        return c;
    }
    private boolean parActivo(int e, int t) {
        return ConexionDB.existe("SELECT 1 FROM solicitudes s JOIN usuarios e ON e.idUsuario=s.idEstudiante JOIN usuarios t ON t.idUsuario=s.idTutor JOIN solicitudes_tutor v ON v.idTutor=t.idUsuario WHERE s.idEstudiante=? AND s.idTutor=? AND s.estado='APROBADO' AND e.estadoCuenta='ACTIVA' AND t.estadoCuenta='ACTIVA' AND e.tipoUsuario='ESTUDIANTE' AND t.tipoUsuario='TUTOR' AND v.estado='APROBADO'", e, t);
    }
    private void validarPar(Perfil_usuario u, int e, int t) {
        exigir((u.getIdUsuario() == e || u.getIdUsuario() == t) && parActivo(e, t), "No tienes acceso a este chat o la tutoría ya no está activa.");
    }
    private void guardarSolicitudTutor(Perfil_usuario u, Map<String, String> f, boolean enviar) {
        rol(u, "TUTOR");
        SolicitudTutor anterior = validaciones.obtener(u.getIdUsuario());
        exigir(anterior == null || Set.of(EstadoSolicitud.BORRADOR, EstadoSolicitud.RECHAZADO).contains(anterior.getEstado()), "La solicitud ya fue enviada o aprobada.");
        String foto = opcional(f, "foto", 200), banner = opcional(f, "banner", 200);
        validarImagen(u, foto); validarImagen(u, banner);
        SolicitudTutor s = new SolicitudTutor(u.getIdUsuario());
        s.setNombreCompleto(opcional(f, "nombre", 100)); s.setDescripcion(opcional(f, "descripcion", 3000));
        s.setMateriasDesdeTexto(opcional(f, "materias", 1000)); s.setFotoPerfil(foto); s.setBanner(banner);
        int revisor = 0;
        if (enviar) {
            exigir(s.estaCompleta(), "Completa: " + String.join(", ", s.camposFaltantes()) + ".");
            revisor = numero(f, "revisor");
            exigir(ConexionDB.existe("SELECT 1 FROM usuarios WHERE idUsuario=? AND tipoUsuario IN ('DOCENTE','DIRECTOR') AND estadoCuenta='ACTIVA'", revisor), "Selecciona un revisor activo.");
        }
        validaciones.guardarBorrador(s);
        if (enviar) exigir(validaciones.enviar(u.getIdUsuario(), revisor), "No se pudo enviar la solicitud.");
    }
    private void agregarLogro(Perfil_usuario u, String tema, String tipo, String fecha) {
        // Inserción y campos adicionales en una sola sentencia; conserva el formato del repositorio original.
        WebDatos.ejecutar("INSERT INTO logros(idUsuario,tema,completado,tipo,fechaObjetivo) VALUES(?,?,0,?,?)", u.getIdUsuario(), tema, tipo, fecha.isEmpty() ? null : fecha);
    }
    private boolean recibioTutoria(int e, int t) {
        for (Map<String, Object> r : WebDatos.consultar("SELECT fecha,hora FROM reservas WHERE idEstudiante=? AND idTutor=? AND estado IN ('ACTIVA','COMPLETADA')", e, t)) {
            if (LocalDateTime.of(LocalDate.parse(r.get("fecha").toString()), LocalTime.parse(r.get("hora").toString())).plusHours(1).isBefore(LocalDateTime.now())) return true;
        }
        return false;
    }

    private Object subir(Perfil_usuario u, Map<String, String> f) throws IOException {
        String categoria = texto(f, "categoria", 20), contexto = opcional(f, "contexto", 100);
        Conversacion c = null;
        if (categoria.equals("CHAT")) c = conversacion(u, contexto);
        else if (categoria.equals("FORO")) {
            rol(u, "ESTUDIANTE");
            exigir(!WebDatos.foro("SELECT id FROM publicaciones WHERE id=? AND autor_id=?", contexto, Integer.toString(u.getIdUsuario())).isEmpty(), "Solo puedes adjuntar a tus publicaciones.");
            exigir(WebDatos.consultar("SELECT id FROM web_archivos WHERE categoria='FORO' AND contexto=?", contexto).size() < 5, "Máximo 5 adjuntos por publicación.");
        } else exigir(categoria.equals("PERFIL"), "Categoría de archivo inválida.");
        String nombre = texto(f, "nombre", 150).replace('\\', '_').replace('/', '_').replaceAll("[\\p{Cntrl}]", "_");
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(texto(f, "datos", 36 * 1024 * 1024)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("No se pudo leer el archivo seleccionado."); }
        exigir(bytes.length > 0 && bytes.length <= 25 * 1024 * 1024, "El archivo debe pesar entre 1 byte y 25 MB.");
        String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String mime = mime(extension);
        if (categoria.equals("PERFIL")) {
            exigir(mime.startsWith("image/"), "Selecciona una imagen PNG, JPG, GIF o WEBP.");
            validarBytesImagen(bytes);
        }
        String id = UUID.randomUUID().toString();
        Path dir = Path.of(System.getProperty("nexo.archivos", "archivos_web")).normalize();
        Files.createDirectories(dir);
        Path destino = dir.resolve(id + (extension.matches("[a-z0-9]{1,8}") ? "." + extension : ".bin"));
        Files.write(destino, bytes);
        try {
            if (c != null) {
                TipoMensaje tipo = mime.startsWith("image/") ? TipoMensaje.IMAGEN : mime.startsWith("video/") ? TipoMensaje.VIDEO : TipoMensaje.ARCHIVO;
                Mensaje m = new Mensaje(AdaptadorUsuario.convertir(u), tipo, opcional(f, "descripcion", 2000), destino.toString());
                new ChatDAO().guardarMensaje(m, c.getId());
            }
            WebDatos.ejecutar("INSERT INTO web_archivos(id,propietario,categoria,contexto,nombre,ruta,mime) VALUES(?,?,?,?,?,?,?)", id, u.getIdUsuario(), categoria, contexto, nombre, destino.toString(), mime);
        } catch (RuntimeException e) { Files.deleteIfExists(destino); throw e; }
        return WebDatos.mapa("ok", true, "url", "/archivo/" + id, "mensaje", "Archivo guardado.");
    }
    private static void validarBytesImagen(byte[] bytes) throws IOException {
        try (javax.imageio.stream.ImageInputStream in = javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<javax.imageio.ImageReader> lectores = javax.imageio.ImageIO.getImageReaders(in);
            exigir(lectores.hasNext(), "La imagen no es válida. Usa PNG, JPG o GIF.");
            javax.imageio.ImageReader lector = lectores.next();
            try { lector.setInput(in); exigir((long) lector.getWidth(0) * lector.getHeight(0) <= 25_000_000, "La imagen es demasiado grande; reduce sus dimensiones."); }
            finally { lector.dispose(); }
        }
    }
    private void validarImagen(Perfil_usuario u, String ruta) {
        if (ruta.isEmpty() || ruta.equals(Objects.toString(u.getFotoPerfil(), "")) || ruta.equals(Objects.toString(u.getBanner(), ""))) return;
        exigir(ruta.startsWith("/archivo/"), "Sube la imagen desde el formulario.");
        exigir(ConexionDB.existe("SELECT 1 FROM web_archivos WHERE id=? AND propietario=? AND categoria='PERFIL' AND mime LIKE 'image/%'", ruta.substring(9), u.getIdUsuario()), "La imagen no pertenece a tu cuenta.");
    }
    public void archivo(HttpExchange x, String id) throws IOException {
        if (!x.getRequestMethod().equals("GET")) throw new ServidorWeb.ErrorHttp(405, "Método no permitido.");
        Perfil_usuario u = usuario(ServidorWeb.sesion(x, true));
        Map<String, Object> a = primera(WebDatos.consultar("SELECT * FROM web_archivos WHERE id=?", id));
        String categoria = a.get("categoria").toString(), contexto = Objects.toString(a.get("contexto"), "");
        if (categoria.equals("CHAT")) conversacion(u, contexto);
        else if (categoria.equals("FORO")) rolForo(u);
        else {
            int propietario = ((Number) a.get("propietario")).intValue();
            Perfil_usuario dueno = Perfil_usuario.buscarPorId(propietario);
            boolean publico = dueno != null && dueno.getEstadoCuenta() == Perfil_usuario.EstadoCuenta.ACTIVA && dueno.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR && Perfil_usuario.obtenerEstadoSolicitudTutor(propietario) == EstadoSolicitud.APROBADO;
            boolean revision = ConexionDB.existe("SELECT 1 FROM solicitudes_tutor WHERE idTutor=? AND idRevisor=? AND estado='PENDIENTE'", propietario, u.getIdUsuario());
            // Solo la foto actual de perfil es visible a la comunidad autenticada.
            // Los banners, adjuntos y fotos anteriores mantienen sus permisos.
            boolean fotoActual = categoria.equals("PERFIL") && dueno != null
                    && dueno.getEstadoCuenta() == Perfil_usuario.EstadoCuenta.ACTIVA
                    && ("/archivo/" + id).equals(dueno.getFotoPerfil());
            exigir(fotoActual || publico || revision || acceso.puedeVerInformacionDe(u, propietario), "No tienes acceso a esta imagen.");
        }
        Path path = Path.of(a.get("ruta").toString()).toAbsolutePath().normalize();
        Path raiz = Path.of(System.getProperty("nexo.archivos", "archivos_web")).toAbsolutePath().normalize();
        Path legado = Path.of("archivos_subidos").toAbsolutePath().normalize();
        exigir((path.startsWith(raiz) || path.startsWith(legado)) && Files.isRegularFile(path) && !Files.isSymbolicLink(path)
                && ((Files.exists(raiz) && path.toRealPath().startsWith(raiz.toRealPath())) || (Files.exists(legado) && path.toRealPath().startsWith(legado.toRealPath()))), "El archivo no está disponible en esta computadora.");
        String mime = a.get("mime").toString();
        boolean inline = mime.startsWith("image/") || mime.startsWith("video/");
        x.getResponseHeaders().set("Content-Type", mime);
        x.getResponseHeaders().set("Content-Disposition", (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + java.net.URLEncoder.encode(a.get("nombre").toString(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        x.sendResponseHeaders(200, Files.size(path)); Files.copy(path, x.getResponseBody());
    }
    private String registrarLegado(String ruta, String categoria, String contexto, int propietario) {
        if (ruta == null || ruta.isBlank()) return "";
        List<Map<String, Object>> existentes = WebDatos.consultar("SELECT id FROM web_archivos WHERE ruta=? AND categoria=? AND contexto=?", ruta, categoria, contexto);
        if (!existentes.isEmpty()) return "/archivo/" + existentes.get(0).get("id");
        Path path = Path.of(ruta).toAbsolutePath().normalize();
        if (!path.startsWith(Path.of("archivos_subidos").toAbsolutePath().normalize()) || !Files.isRegularFile(path)) return "";
        String id = UUID.randomUUID().toString(), nombre = GestorArchivos.nombreOriginal(ruta);
        String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        WebDatos.ejecutar("INSERT INTO web_archivos VALUES(?,?,?,?,?,?,?)", id, propietario, categoria, contexto, nombre, ruta, mime(extension));
        return "/archivo/" + id;
    }
    private static String mime(String ext) {
        switch (ext) {
            case "png": return "image/png"; case "jpg": case "jpeg": return "image/jpeg"; case "gif": return "image/gif";
            case "webp": return "image/webp"; case "mp4": return "video/mp4"; case "webm": return "video/webm";
            case "pdf": return "application/pdf"; case "txt": return "text/plain; charset=utf-8";
            default: return "application/octet-stream";
        }
    }
    private static void verificarClave(Perfil_usuario u, String clave) {
        Map<String, Object> fila = primera(WebDatos.consultar("SELECT contrasena FROM usuarios WHERE idUsuario=?", u.getIdUsuario()));
        exigir(ClaveSegura.verificar(clave, fila.get("contrasena").toString()), "La contraseña actual es incorrecta.");
    }
    private static void tutorAprobado(Perfil_usuario u) {
        rol(u, "TUTOR"); exigir(Perfil_usuario.obtenerEstadoSolicitudTutor(u.getIdUsuario()) == EstadoSolicitud.APROBADO, "Primero debes ser aprobado como tutor.");
    }
    private static void rolForo(Perfil_usuario u) {
        rol(u, "ESTUDIANTE", "TUTOR", "DOCENTE");
        if (u.getTipoUsuario() == Perfil_usuario.TipoUsuario.TUTOR) tutorAprobado(u);
    }
    private static void rol(Perfil_usuario u, String... roles) {
        if (!Arrays.asList(roles).contains(u.getTipoUsuario().name())) throw new ServidorWeb.ErrorHttp(403, "Tu tipo de cuenta no permite esta acción.");
    }
    private static Map<String, Object> primera(List<Map<String, Object>> lista) {
        if (lista.isEmpty()) throw new ServidorWeb.ErrorHttp(404, "No se encontró la información solicitada.");
        return lista.get(0);
    }
    private static void exigir(boolean condicion, String mensaje) { if (!condicion) throw new IllegalArgumentException(mensaje); }
    private static String texto(Map<String, String> f, String campo, int max) {
        String valor = opcional(f, campo, max); exigir(!valor.isBlank(), "Completa el campo " + campo + "."); return valor;
    }
    private static String opcional(Map<String, String> f, String campo, int max) {
        String valor = f.getOrDefault(campo, "");
        if (!Set.of("clave", "actual", "nueva", "confirmacion", "datos").contains(campo)) valor = valor.trim();
        exigir(valor.length() <= max, "El campo " + campo + " supera el tamaño permitido."); return valor;
    }
    private static int numero(Map<String, String> f, String campo) {
        try { int n = Integer.parseInt(texto(f, campo, 12)); exigir(n > 0, "Identificador inválido."); return n; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("El campo " + campo + " debe ser un número válido."); }
    }
    /** Fecha en formato aaaa-MM-dd; si no es válida responde con un mensaje claro en lugar de un error interno. */
    private static LocalDate fecha(Map<String, String> f, String campo) {
        try { return LocalDate.parse(texto(f, campo, 20)); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("La fecha no es válida. Usa el formato aaaa-MM-dd."); }
    }
    /** Hora en formato HH:mm; si no es válida responde con un mensaje claro. */
    private static LocalTime hora(Map<String, String> f, String campo) {
        try { return LocalTime.parse(texto(f, campo, 20)); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("La hora no es válida. Usa el formato HH:mm."); }
    }
    /** Calificación opcional de 1 a 10; devuelve 0 cuando no se envió. */
    private static int calificacionOpcional(Map<String, String> f) {
        String valor = opcional(f, "calificacion", 3);
        if (valor.isEmpty()) return 0;
        try {
            int nota = Integer.parseInt(valor);
            exigir(nota >= 1 && nota <= 10, "La calificación debe ser un número del 1 al 10.");
            return nota;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("La calificación debe ser un número del 1 al 10."); }
    }
    private static void registrarCalificacion(int idTutor, int calificacion, int idRevisor) {
        WebDatos.ejecutar("UPDATE solicitudes_tutor SET calificacion=?,calificadoPor=?,fechaCalificacion=? WHERE idTutor=?", calificacion, idRevisor, LocalDateTime.now().toString(), idTutor);
    }
    private static boolean booleano(Map<String, String> f, String campo) { return Set.of("true", "1", "on").contains(f.getOrDefault(campo, "false")); }
}
