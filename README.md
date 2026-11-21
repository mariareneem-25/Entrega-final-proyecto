# Plataforma de tutorías entre pares (Proyecto POO)

Aplicación web local para que estudiantes encuentren tutores, acuerden tutorías por chat y lleven su progreso.
Está escrita en **Java** (servidor y lógica), **SQLite** (persistencia) y **HTML, CSS y JavaScript** (interfaz).
Se conserva también la versión de consola original (`--consola`).

## Cómo ejecutarla

Requisitos: **JDK 17 o superior** (el proyecto trae el controlador de SQLite en `lib/`).

| Sistema | Comando |
| --- | --- |
| Windows | doble clic en `iniciar.bat` |
| Linux / macOS | `./iniciar.sh` |

El script compila las clases de `src/`, inicia el servidor en `http://127.0.0.1:8080` y abre el navegador.

Opciones al final del comando: `--demo` (datos ficticios en `datos-demo/`), `--consola` (versión de consola) y `--sin-navegador`.

### Configuración (propiedades de Java, `-Dnombre=valor`)

| Propiedad | Para qué sirve | Valor por defecto |
| --- | --- | --- |
| `nexo.puerto` | Puerto del servidor | `8080` |
| `nexo.db` | Base de datos principal | `proyecto.db` |
| `nexo.foro` | Base de datos del foro y del chat | `tutorias.db` |
| `nexo.archivos` | Carpeta de archivos subidos | `archivos_web` |
| `nexo.directores` | Correos que se autorizan como director de carrera, separados por coma | (ninguno) |

## Cuentas y roles

| Rol | Cómo se crea | Qué puede hacer |
| --- | --- | --- |
| Estudiante | Registro con cualquier correo `@uvg.edu.gt` | Buscar tutores, solicitar tutorías, chatear, calendario, progreso, foro (publica y comenta) |
| Tutor | Registro como tutor o cambio desde el perfil; debe ser **aprobado** | Recibir solicitudes, chat, calendario, foro (solo comenta) |
| Docente | Correo autorizado por un director | Revisar solicitudes de tutor, calificarlos, foro (solo comenta) |
| Director de carrera | Correo autorizado (inicial o por otro director) | Lo mismo que el docente y además autorizar cuentas de docente y director |

El rol autorizado para un correo **siempre prevalece** sobre el que se elija en el registro.
Correos autorizados desde el primer arranque (solo para pruebas): `estudiante1@`, `docente1@` y `director1@uvg.edu.gt`.
**Antes de usarlo con personas reales** define `nexo.directores` y no dejes disponible `director1@uvg.edu.gt`.

## Flujos principales

1. **Solicitud de tutor**: el tutor llena nombre, descripción, materias, foto y banner; puede guardar el borrador o enviarlo a un docente o director elegido. Quien revisa aprueba (con calificación opcional de 1 a 10) o rechaza con un motivo obligatorio.
2. **Tutoría**: el estudiante busca un tutor aprobado y envía su solicitud; al aceptarla se habilita un chat entre ambos.
3. **Calendario**: cada persona marca días como ocupado o de preferencia. Las reservas solo se crean o se eliminan cuando la otra persona acepta una propuesta hecha en el chat.
4. **Reseñas**: después de una tutoría finalizada, el estudiante puede dejar una reseña con su nombre o de forma anónima.
5. **Foro académico**: preguntas con adjuntos, respuestas anidadas y publicación anónima para estudiantes.

## Arquitectura

```
Navegador (src/web: HTML, CSS, JS)
        │  JSON sobre HTTP
ServidorWeb ── sesiones, cabeceras de seguridad, límites
        │
WebApi ─────── controlador: valida permisos y entradas
        │
Clases de dominio y DAO ── Usuario/Estudiante/Tutor/Docente, Solicitud, CalendarioDAO, ChatDAO, ForoDAO, ...
        │
SQLite (proyecto.db y tutorias.db)
```

Patrones usados: **MVC** (vista web, controlador `WebApi`, modelo de dominio), **DAO** para el acceso a datos, **herencia y polimorfismo** en los tipos de usuario y **interfaces** (`Publicable`, `UsuarioRepositorio`, `SelectorImagen`).

## Seguridad

- Contraseñas guardadas como hash con sal (PBKDF2) y requisitos mínimos de contraseña.
- La identidad siempre sale de la sesión, nunca del formulario; protección CSRF y validación de origen.
- Límite de intentos de inicio de sesión, cierre de sesiones al cambiar la contraseña y caducidad de sesiones.
- Consultas preparadas, validación de fechas, horas, tipos y tamaños, y mensajes de error claros.
- Los archivos privados solo los ven las personas del chat o quienes tienen permiso.

## Pruebas y documentación

```bash
python pruebas/integracion.py        # recorridos completos por HTTP (compilar antes)
javadoc -d docs -encoding UTF-8 -docencoding UTF-8 --add-modules jdk.httpserver -cp "lib/*" -package src/*.java
```

La prueba usa una carpeta temporal propia y no modifica tus bases de datos.

## Control de versiones

El archivo `.gitignore` excluye lo generado (`bin/`, `runtime-tmp/`), las bases de datos y los archivos subidos.
