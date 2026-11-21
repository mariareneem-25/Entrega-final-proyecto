"""Pruebas de recorridos HTTP reales; solo modifica una carpeta temporal propia.
Ejecutar desde la raíz: python pruebas/integracion.py (requiere compilar antes).
"""
import base64
import http.cookiejar
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, timedelta

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:18081'
CHECKS = 0


def check(condition, label):
    global CHECKS
    assert condition, label
    CHECKS += 1
    print('OK', label)


class Cliente:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = ''

    def call(self, path, data=None, expected=200, csrf=True, headers=None):
        req = urllib.request.Request(BASE + '/api/' + path,
            None if data is None else urllib.parse.urlencode(data).encode(), headers=headers or {})
        if data is not None:
            req.add_header('Content-Type', 'application/x-www-form-urlencoded')
            if csrf:
                req.add_header('X-CSRF-Token', self.csrf)
        try:
            r = self.opener.open(req)
        except urllib.error.HTTPError as e:
            r = e
        body = json.load(r)
        assert r.code == expected, (path, r.code, body, expected)
        return body

    def state(self):
        s = self.call('sesion')
        if s.get('usuario'):
            self.csrf = s['csrf']
        return s

    def register(self, email, role='ESTUDIANTE', name='Usuario de prueba'):
        self.call('registro', dict(nombre=name, correo=email, clave='Prueba2026!', confirmacion='Prueba2026!', rol=role), csrf=False)
        return self.state()

    def file(self, url, expected=200):
        try:
            r = self.opener.open(BASE + url)
        except urllib.error.HTTPError as e:
            r = e
        assert r.code == expected, (url, r.code, r.read())
        return r.read()


def run(directory):
    anon = Cliente()
    check(anon.state()['usuario'] is None, 'Acceso inicial sin sesión')
    anon.call('chat?id=inexistente', expected=401)
    check(True, 'Chat privado bloqueado sin sesión')
    anon.call('login', dict(correo='persona@gmail.com', clave='Prueba2026!'), expected=400, csrf=False)
    director, docente, student, tutor, outsider = [Cliente() for _ in range(5)]
    ds = director.register('director1@uvg.edu.gt', 'TUTOR', 'Director de prueba')
    check(ds['usuario']['rol'] == 'DIRECTOR', 'El rol autorizado prevalece sobre el enviado por el navegador')
    es = student.register('estudiante1@uvg.edu.gt', name='Estudiante principal')
    cs = docente.register('docente1@uvg.edu.gt', name='Docente revisor')
    for email in ['tutor.prueba@uvg.edu.gt','otro.prueba@uvg.edu.gt']:
        director.call('autorizacion', dict(correo=email, tipo='estudiante'))
    ts = tutor.register('tutor.prueba@uvg.edu.gt', 'TUTOR', 'Tutor de prueba')
    os_ = outsider.register('otro.prueba@uvg.edu.gt', name='Otro estudiante')
    tid, eid, oid = ts['usuario']['id'], es['usuario']['id'], os_['usuario']['id']
    student.call('autorizacion', dict(correo='intruso@uvg.edu.gt', tipo='director'), expected=403)
    check(True, 'Un estudiante no puede autorizar cuentas ni elevar privilegios')
    student.call('perfil',dict(nombre='Sin CSRF',foto='',banner='',descripcion=''),expected=403,csrf=False)
    student.call('perfil',dict(nombre='Otro origen'),expected=403,headers={'Origin':'https://otro-sitio.example'})
    check(True, 'Protección de cambios frente a formularios sin CSRF y orígenes externos')
    student.call('solicitud',dict(tutor=tid,materia='Cálculo 1',mensaje='Hola'),expected=400)
    check(not student.state()['tutores'], 'Un tutor pendiente no aparece disponible')
    png = base64.b64encode(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==')).decode()
    pic = tutor.call('archivo',dict(categoria='PERFIL',nombre='foto.png',datos=png))['url']
    app = dict(nombre='Tutor de prueba',descripcion='Experiencia explicando límites y derivadas',materias='Cálculo 1, Física 1',foto=pic,banner=pic,revisor=cs['usuario']['id'])
    tutor.call('tutor-borrador',app)
    check(tutor.state()['miSolicitud'][0]['estado']=='BORRADOR','Guardar y recuperar borrador de tutor')
    tutor.call('tutor-enviar',app)
    director.call('tutor-revisar',dict(id=tid,aceptar=1),expected=400)
    docente.call('tutor-revisar',dict(id=tid,aceptar=0,motivo=''),expected=400)
    docente.call('tutor-revisar',dict(id=tid,aceptar=0,motivo='Amplía tu experiencia.'))
    check(tutor.state()['miSolicitud'][0]['motivoRechazo']=='Amplía tu experiencia.','Rechazo visible y motivo obligatorio; solo responde el revisor elegido')
    tutor.call('tutor-enviar',app)
    docente.call('tutor-revisar',dict(id=tid,aceptar=1))
    check(len(student.state()['tutores'])==1,'Tutor aprobado aparece en el catálogo')
    student.call('solicitud',dict(tutor=tid,materia='Otra materia',mensaje='Hola'),expected=400)
    student.call('solicitud',dict(tutor=tid,materia='Cálculo 1',mensaje='Quiero practicar derivadas.'))
    check(not student.state()['chats'],'El chat sigue cerrado hasta que el tutor acepte')
    req=tutor.state()['solicitudes'][0]['idSolicitud']
    tutor.call('perfil?id='+str(eid))
    outsider.call('perfil?id='+str(eid),expected=400)
    tutor.call('solicitud-responder',dict(id=req,aceptar=1))
    chatid=student.state()['chats'][0]['id']
    check(chatid==tutor.state()['chats'][0]['id'],'Aceptar tutoría habilita un mismo chat para ambos')
    student.call('solicitud',dict(tutor=tid,materia='Cálculo 1',mensaje='Duplicada'),expected=400)
    outsider.call('chat?id='+chatid,expected=400)
    student.call('mensaje',dict(chat=chatid,texto='¡Hola, tutor! <script>alert(1)</script>'))
    messages=tutor.call('chat?id='+chatid)['mensajes']
    check(messages[-1]['texto'].startswith('¡Hola'),'Mensajes persistentes con identidad tomada de la sesión')
    payload=base64.b64encode('Ejercicios de cálculo'.encode()).decode()
    fileurl=student.call('archivo',dict(categoria='CHAT',contexto=chatid,nombre='ejercicios.txt',datos=payload))['url']
    check(tutor.file(fileurl).decode()=='Ejercicios de cálculo','Subir y descargar archivos en el chat')
    outsider.file(fileurl,400)
    check(True,'Los archivos privados no son accesibles a un tercero')
    student.call('archivo',dict(categoria='PERFIL',nombre='falso.png',datos=payload),expected=400)
    check(True,'Se valida el contenido de las imágenes de perfil')
    tomorrow=(date.today()+timedelta(days=2)).isoformat()
    student.call('fecha',dict(chat=chatid,fecha=tomorrow,hora='14:00',modalidad='VIRTUAL',lugar='Sala virtual del chat'))
    prop=tutor.call('chat?id='+chatid)['propuestas'][0]['idSolicitudCal']
    check(not student.state()['reservas'],'Una fecha propuesta todavía no es una reserva')
    student.call('fecha-responder',dict(id=prop,aceptar=1),expected=400)
    tutor.call('fecha-responder',dict(id=prop,aceptar=1))
    reserve=student.state()['reservas'][0]
    check(reserve['idReserva']==tutor.state()['reservas'][0]['idReserva'] and reserve['modalidad']=='VIRTUAL','Confirmación reflejada en ambos calendarios y modalidad guardada')
    tutor.call('fecha-responder',dict(id=prop,aceptar=1),expected=404)
    student.call('fecha',dict(chat=chatid,fecha=tomorrow,hora='14:30',modalidad='PRESENCIAL',lugar='Biblioteca'),expected=400)
    check(True,'Se impiden respuestas duplicadas y horarios que se traslapan')
    student.call('resena',dict(tutor=tid,puntuacion=5,texto='Excelente'),expected=400)
    student.call('cancelar-reserva',dict(id=reserve['idReserva']))
    check(len(student.state()['reservas'])==1,'Solicitar cancelación conserva la reserva hasta su aceptación')
    cancel=tutor.call('chat?id='+chatid)['propuestas'][0]['idSolicitudCal']
    tutor.call('fecha-responder',dict(id=cancel,aceptar=1))
    check(not student.state()['reservas'] and not tutor.state()['reservas'],'Cancelación aceptada quita la reserva de ambos calendarios')
    student.call('marca',dict(fecha=tomorrow,tipo='OCUPADO'))
    tutor.call('fecha',dict(chat=chatid,fecha=tomorrow,hora='17:00',modalidad='VIRTUAL',lugar='Sala virtual'),expected=400)
    student.call('marca',dict(fecha=tomorrow,tipo='QUITAR'))
    student.call('logro',dict(tema='Practicar límites',tipo='META',fecha=tomorrow))
    goal=student.state()['logros'][0]['id']
    outsider.call('logro-estado',dict(id=goal,completado=1),expected=400)
    student.call('logro-estado',dict(id=goal,completado=1))
    check(student.state()['logros'][0]['completado']==1,'Progreso persistente y protegido por propietario')
    student.call('publicacion',dict(titulo='Pregunta anónima',texto='Una duda',anonimo=1))
    post=student.state()['foro'][0]
    other=outsider.state()['foro'][0]
    check(other['autor']=='Estudiante anónimo' and 'autor_id' not in other and not other['mia'],'Anonimato también en JSON; no se expone el autor real')
    docente.call('publicacion',dict(titulo='No permitido',texto='No',anonimo=0),expected=403)
    docente.call('comentario',dict(id=post['id'],texto='Primero veamos el concepto.',anonimo=0))
    check(len(student.state()['foro'][0]['comentarios'])==1,'Docentes pueden responder y no crear preguntas')
    comment=student.state()['foro'][0]['comentarios'][0]['id']
    student.call('comentario',dict(id=post['id'],padre=comment,texto='¡Gracias!',anonimo=1))
    forumurl=student.call('archivo',dict(categoria='FORO',contexto=post['id'],nombre='apuntes.txt',datos=payload))['url']
    check(len(student.state()['foro'][0]['adjuntos'])==1 and outsider.file(forumurl),'Adjuntos y respuestas anidadas en el foro')
    reunion=student.call('reunion',dict(chat=chatid))
    check(reunion['url'].startswith('https://meet.jit.si/NexoUVG-'),'Enlace de sala externa guardado en el chat')
    # Una reserva histórica finalizada habilita la reseña (sin manipular datos del usuario).
    with sqlite3.connect(Path(directory)/'proyecto.db') as db:
        db.execute("INSERT INTO reservas(idEstudiante,idTutor,fecha,hora,estado) VALUES(?,?,?,'10:00','ACTIVA')",(eid,tid,(date.today()-timedelta(days=1)).isoformat()))
    student.call('resena',dict(tutor=tid,puntuacion=5,texto='Explicación clara y paciente.'))
    check(student.call('perfil?id='+str(tid))['resenas'][0]['puntuacion']==5,'Reseña permitida después de una tutoría finalizada')
    student.call('clave',dict(actual='Incorrecta1',nueva='OtraClave2026!',confirmacion='OtraClave2026!'),expected=400)
    student.call('clave',dict(actual='Prueba2026!',nueva='OtraClave2026!',confirmacion='OtraClave2026!'))
    student.state()
    student.call('logout',{})
    student.call('login',dict(correo='estudiante1@uvg.edu.gt',clave='OtraClave2026!'),csrf=False)
    student.state()
    check(True,'Cambio de contraseña e inicio de sesión con la nueva clave')
    student.call('eliminar-cuenta',dict(clave='OtraClave2026!',confirmacion='ELIMINAR'))
    check(student.state()['usuario'] is None,'Desactivar cuenta invalida la sesión')
    student.call('reactivar',dict(correo='estudiante1@uvg.edu.gt',clave='OtraClave2026!'),csrf=False)
    student.call('login',dict(correo='estudiante1@uvg.edu.gt',clave='OtraClave2026!'),csrf=False)
    check(student.state()['usuario']['rol']=='ESTUDIANTE','Reactivación y recuperación del acceso')
    student.call('rol',dict(clave='OtraClave2026!',confirmacion='CAMBIAR'))
    check(student.state()['usuario']['rol']=='TUTOR','Cambio de estudiante a tutor con confirmación')
    tutor.call('chat?id='+chatid,expected=404)
    with sqlite3.connect(Path(directory)/'proyecto.db') as db:
        check(all(r[0].startswith('pbkdf2$') for r in db.execute('SELECT contrasena FROM usuarios')),'Contraseñas guardadas como hashes con sal')
    print('\nRESULTADO:',CHECKS,'comprobaciones y recorridos completos aprobados.')


if __name__=='__main__':
    parent=ROOT/'runtime-tmp'
    parent.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pruebas-',dir=parent) as directory:
        cmd=['java','-Djava.io.tmpdir='+str(parent),'-Dnexo.puerto=18081','-Dnexo.db='+str(Path(directory)/'proyecto.db'),'-Dnexo.foro='+str(Path(directory)/'tutorias.db'),'-Dnexo.archivos='+str(Path(directory)/'archivos'),'--add-modules','jdk.httpserver','-cp',str(ROOT/'bin')+os.pathsep+str(ROOT/'lib'/'*'),'Main','--sin-navegador']
        with open(Path(directory)/'servidor.log','w+',encoding='utf-8') as log:
            proc=subprocess.Popen(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            try:
                import time
                for _ in range(150):
                    if proc.poll() is not None: raise RuntimeError('Java no pudo iniciarse')
                    try:
                        urllib.request.urlopen(BASE+'/api/sesion',timeout=.3).close();break
                    except (OSError,urllib.error.URLError): time.sleep(.1)
                run(directory)
            except BaseException:
                log.seek(0);print(log.read());raise
            finally:
                proc.terminate();proc.wait(timeout=10)
