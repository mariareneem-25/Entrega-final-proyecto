'use strict';

const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const roleName = {ESTUDIANTE:'Estudiante',TUTOR:'Tutor',DOCENTE:'Docente',DIRECTOR:'Director'};
const statusName = {APROBADO:'Aceptada',PENDIENTE:'Pendiente',RECHAZADO:'Rechazada',BORRADOR:'Borrador',ACEPTADA:'Aceptada',RECHAZADA:'Rechazada',ACTIVA:'Confirmada',CANCELADA:'Cancelada'};
const kindName = {TEMA:'Tema estudiado',META:'Meta',PENDIENTE:'Actividad pendiente',FECHA:'Fecha importante'};
const paths = {
 home:'M3 10l9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z',
 users:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
 calendar:'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2M8 14h2M14 14h2M8 18h2',
 chat:'M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8z',
 book:'M12 7v14M2 3c4-1 7 0 10 3 3-3 6-4 10-3v15c-4-1-7 0-10 3-3-3-6-4-10-3z',
 check:'M5 12l4 4L19 6', plus:'M12 5v14M5 12h14', arrow:'M5 12h14M13 6l6 6-6 6',
 search:'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
 logout:'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
 settings:'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z',
 star:'M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9z',
 award:'M8 14l-1 8 5-3 5 3-1-8M19 9a7 7 0 1 1-14 0 7 7 0 0 1 14 0',
 shield:'M12 2L3 6v6c0 5 9 10 9 10s9-5 9-10V6zM8 12l3 3 5-5',
 file:'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 13h8M8 17h5',
 clip:'M21 11.5l-9 9a6 6 0 0 1-8.5-8.5l10-10a4 4 0 0 1 5.7 5.7l-10 10a2 2 0 0 1-2.9-2.9l9-9',
 video:'M23 7l-7 5 7 5zM3 5h11a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2',
 clock:'M12 8v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
 send:'M22 2L9 15M22 2l-7 20-6-7-7-6z', close:'M6 6l12 12M18 6L6 18',
 menu:'M3 6h18M3 12h18M3 18h18', left:'M15 18l-6-6 6-6', right:'M9 18l6-6-6-6',
 trash:'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
 bell:'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
 user:'M20 21v-2a7 7 0 0 0-14 0v2M17 7a5 5 0 1 1-10 0 5 5 0 0 1 10 0',
 leaf:'M20 3c-15-2-20 15-10 17 11 2 11-13 10-17M5 22L17 8',
 mail:'M3 5h18v14H3zM3 5l9 8 9-8', refresh:'M21 3v6h-6M3 21v-6h6M5.5 8a7 7 0 0 1 11.6-3L21 9M3 15l3.9 4A7 7 0 0 0 18.5 16'
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[name] || paths.book}"></path></svg>`;
const act = (action, label, data = {}, cls = 'btn primary', ico = '') => `<button type="button" class="${cls}" data-action="${action}" ${!label?`aria-label="${esc({'profile':'Ver perfil','goal-delete':'Eliminar registro','post':'Ver conversación','navigate':'Abrir sección','month':data.step<0?'Mes anterior':'Mes siguiente'}[action]||'Abrir opción')}"`:''} ${Object.entries(data).map(([k,v]) => `data-${k}="${esc(v)}"`).join(' ')}>${ico ? icon(ico) : ''}${esc(label)}</button>`;
const badge = value => `<span class="badge ${esc(value)}">${esc(statusName[value] || value)}</span>`;
const initials = name => String(name || 'N').trim().split(/\s+/).filter(Boolean).slice(0,2).map(s => s[0]).join('').toUpperCase();
const safeImage = url => /^\/archivo\/[a-f0-9-]{36}$/i.test(url || '') ? url : '';
const avatar = (name, url = '', size = '') => `<span class="avatar ${size}">${safeImage(url) ? `<img src="${esc(url)}" alt="" loading="lazy">` : esc(initials(name))}</span>`;
const cover = url => safeImage(url) ? `<img src="${esc(url)}" alt="" loading="lazy">` : '';
const empty = (title, text, ico = 'book', extra = '') => `<div class="empty">${icon(ico)}<strong>${esc(title)}</strong><p>${esc(text)}</p>${extra}</div>`;
const art = () => `<div class="hero-art" aria-hidden="true"><div class="book"></div><span class="spark">✦</span><span class="bubble">${icon('book')}</span><span class="dot">${icon('check')}</span></div>`;
const fmt = (d, opts = {}) => d ? new Date(String(d).length === 10 ? d+'T12:00:00' : d).toLocaleDateString('es-GT', {day:'numeric',month:'short',...opts}) : '';
const clock = d => d ? new Date(d).toLocaleTimeString('es-GT',{hour:'2-digit',minute:'2-digit'}) : '';
const shortHour = h => String(h || '').slice(0,5);
let state = null, view = 'inicio', selectedChat = '', authTab = 'login', tutorSearch = '', forumSearch = '', requestFilter = 'TODAS', goalFilter = 'TODOS';
let month = new Date(new Date().getFullYear(), new Date().getMonth(), 1), chatSignature = '', chatBusy = false, routeVersion = 0;

async function api(path, data, auth = true) {
  const opts = data === undefined ? {} : {method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8', ...(auth && state?.csrf ? {'X-CSRF-Token':state.csrf} : {})},body:new URLSearchParams(data)};
  let res;
  try { res = await fetch('/api/'+path, opts); } catch (_) { throw new Error('No se pudo conectar con Java. Comprueba que la ventana del servidor siga abierta.'); }
  const result = await res.json();
  if (!res.ok) {
    if (res.status === 401 && state?.usuario) { state = null; renderAuth(); }
    throw new Error(result.error || 'No se pudo completar la operación.');
  }
  return result;
}
function toast(message, error = false) {
  const el = document.createElement('div'); el.className = 'toast'+(error?' error':''); el.textContent = message;
  $('#toasts').append(el); setTimeout(() => el.remove(), error ? 8500 : 4300);
}
async function reload(render = true) { state = await api('sesion'); if (render) state.usuario ? renderApp() : renderAuth(); }
function navigate(page, id = '') { location.hash = page + (id ? '/'+encodeURIComponent(id) : ''); if (view === page && (!id || selectedChat === id)) route(); }
function route() {
  const [page, id] = location.hash.slice(1).split('/');
  view = page || 'inicio'; selectedChat = page === 'mensajes' ? decodeURIComponent(id || '') : ''; chatSignature = '';
  if (state?.usuario) renderApp();
}
function allowed() {
  const r = state.usuario.rol, pending = r === 'TUTOR' && state.usuario.estadoTutor !== 'APROBADO';
  if (pending) return ['inicio','validacion','calendario','perfil'];
  if (r === 'DIRECTOR') return ['inicio','validaciones','perfil'];
  if (r === 'DOCENTE') return ['inicio','validaciones','foro','perfil'];
  return ['inicio',...(r === 'ESTUDIANTE'?['tutores']:[]),'solicitudes','mensajes','calendario',...(r==='ESTUDIANTE'?['progreso']:[]),'foro','perfil'];
}
const labels = {inicio:'Inicio',tutores:'Explorar tutores',solicitudes:'Solicitudes',mensajes:'Mensajes',calendario:'Mi calendario',progreso:'Mi progreso',foro:'Foro académico',perfil:'Mi perfil',validacion:'Mi solicitud de tutor',validaciones:'Validar tutores'};
const navIcons = {inicio:'home',tutores:'users',solicitudes:'file',mensajes:'chat',calendario:'calendar',progreso:'award',foro:'book',perfil:'user',validacion:'shield',validaciones:'shield'};

function renderApp() {
  routeVersion++;
  if (!allowed().includes(view)) view = 'inicio';
  const u = state.usuario;
  const pending = u.rol === 'TUTOR' ? state.solicitudes.filter(s => s.estado === 'PENDIENTE').length : state.validaciones.length;
  $('#app').innerHTML = `<div class="shell ${view==='inicio'?'home-shell':''}"><aside class="sidebar" id="sidebar"><div class="brand"><span class="brand-symbol">T</span>TutorMatch<span class="muted small">®</span></div><div class="brand-caption">Tutorías · UVG</div><p class="eyebrow">Tu espacio de aprendizaje</p><nav class="nav" aria-label="Navegación principal">${allowed().filter(v=>v!=='perfil').map(v=>`<button data-action="navigate" data-page="${v}" class="${view===v?'active':''}" ${view===v?'aria-current="page"':''}>${icon(navIcons[v])}<span>${labels[v]}</span>${pending && ((v==='solicitudes' && u.rol==='TUTOR') || v==='validaciones') ? `<span class="nav-count">${pending}</span>`:''}</button>`).join('')}</nav><div class="sidebar-bottom"><div class="sidebar-note">${icon('leaf')}<strong>Aprender es mejor en compañía </strong>Un paso a la vez, una duda menos</div><nav class="nav" aria-label="Cuenta"><button data-action="navigate" data-page="perfil" class="${view==='perfil'?'active':''}">${icon('settings')}Mi perfil y ajustes</button></nav><div class="sidebar-user">${avatar(u.nombre,u.foto)}<div class="grow"><strong class="truncate">${esc(u.nombre)}</strong><small>${roleName[u.rol]}</small></div>${act('logout','','','icon-btn','logout')}</div></div></aside><div class="workspace"><header class="topbar">${act('menu','',{},'icon-btn mobile-toggle','menu')}<div class="topbar-title">Mi campus <span class="muted">&nbsp; / &nbsp;</span><strong>${labels[view]}</strong></div><div class="top-actions"><span class="date-label">${fmt(state.hoy,{weekday:'long',year:'numeric'})}</span>${act('refresh','',{},'icon-btn','refresh')}${act('navigate','',{page:u.rol==='DOCENTE'||u.rol==='DIRECTOR'?'validaciones':'solicitudes'},'icon-btn','bell')}${avatar(u.nombre,u.foto)}</div></header><main class="page" id="page" tabindex="-1"></main>${mobileNav()}</div></div>`;
  $$('[data-action="logout"]').forEach(b => b.setAttribute('aria-label','Cerrar sesión'));
  $('[data-action="refresh"]').setAttribute('aria-label','Actualizar información');
  $('[data-action="menu"]').setAttribute('aria-label','Abrir menú');
  const pages = {inicio:renderHome,tutores:renderTutors,solicitudes:renderRequests,mensajes:renderChats,calendario:renderCalendar,progreso:renderProgress,foro:renderForum,perfil:renderSettings,validacion:renderApplication,validaciones:renderApprovals};
  pages[view]();
}
// Los accesos reutilizan las rutas y permisos existentes.
function mobileNav() {
  const pages = ['inicio','tutores','mensajes','calendario'].filter(p=>allowed().includes(p));
  return `<nav class="bottom-nav" aria-label="Navegación móvil">${pages.map(p=>`<button data-action="navigate" data-page="${p}" class="${view===p?'active':''}" ${view===p?'aria-current="page"':''}>${icon(navIcons[p])}<span>${({inicio:'Inicio',tutores:'Tutores',mensajes:'Chat',calendario:'Agenda'})[p]}</span></button>`).join('')}<button data-action="menu" aria-label="Abrir todas las opciones">${icon('menu')}<span>Más</span></button></nav>`;
}
function homeDirectory(u) {
  return `<section class="home-profile" aria-label="Tu perfil">${avatar(u.nombre,u.foto,'xlarge')}<div><h1>${esc(u.nombre)}</h1>${act('navigate','Ver mi perfil',{page:'perfil'},'profile-link','arrow')}</div></section><nav class="home-directory" aria-label="Accesos de inicio">${allowed().filter(p=>!['inicio','perfil'].includes(p)).map(p=>`<button data-action="navigate" data-page="${p}">${icon(navIcons[p])}<span>${labels[p]}</span>${icon('arrow')}</button>`).join('')}</nav>`;
}
function heading(title, subtitle, button = '') { return `<div class="page-heading between"><div><h1>${esc(title)}</h1><p class="muted">${esc(subtitle)}</p></div>${button}</div>`; }
function tutorCard(t) {
  const materias = String(t.materias || '').split(',').filter(Boolean);
  return `<article class="tutor-card"><div class="tutor-cover">${cover(t.banner)}</div><div class="tutor-body"><div class="tutor-top">${avatar(t.nombre,t.foto,'large')}<span class="rating">★ ${t.promedio?esc(t.promedio):'Nuevo'}</span></div><h3>${esc(t.nombre)} <span class="verified" title="Tutor aprobado">${icon('shield')}</span></h3><p class="tutor-subtitle">Tutor verificado · UVG</p><div class="chips">${materias.slice(0,2).map(m=>`<span class="chip">${esc(m.trim())}</span>`).join('')}${materias.length>2?`<span class="chip">+${materias.length-2}</span>`:''}</div><p class="tutor-desc">${esc(t.descripcion || 'Aprendamos juntos, paso a paso.')}</p><div class="tutor-footer"><span>Presencial o virtual</span>${act('profile','Ver perfil',{id:t.id},'btn soft small','arrow')}</div></div></article>`;
}
function renderHome() {
  const u = state.usuario, name = u.nombre.split(' ')[0], student = u.rol === 'ESTUDIANTE', tutor = u.rol === 'TUTOR', reviewer = ['DOCENTE','DIRECTOR'].includes(u.rol);
  const future = state.reservas.filter(r => r.fecha >= state.hoy), done = state.logros.filter(l=>l.completado).length;
  const pending = state.solicitudes.filter(s=>s.estado==='PENDIENTE');
  const heroTitle = student ? 'Tu próximo logro empieza\ncon una buena pregunta.' : reviewer ? 'El talento crece con\nun buen acompañamiento.' : 'Lo que sabes puede\nhacer la diferencia.';
  const heroDescription = student ? 'Encuentra a alguien que ya recorrió ese camino. Conecta, resuelve tus dudas y sigue avanzando.' : reviewer ? 'Acompaña a los próximos tutores y ayuda a construir una comunidad donde todos puedan aprender.' : 'Acompaña a otros estudiantes, comparte lo que has aprendido y construye nuevas oportunidades.';
  const heroAction = student ? act('navigate','Encontrar mi tutor',{page:'tutores'},'btn primary','arrow') : reviewer ? act('navigate','Revisar solicitudes',{page:'validaciones'},'btn primary','arrow') : u.estadoTutor==='APROBADO' ? act('navigate','Ver mis solicitudes',{page:'solicitudes'},'btn primary','arrow') : act('navigate','Completar mi solicitud',{page:'validacion'},'btn primary','arrow');
  const stats = student ? [[future.length,'Tutorías por venir','calendar'],[state.tutores.length,'Tutores para acompañarte','users'],[done,'Logros completados','award']] : reviewer ? [[state.validaciones.length,'Tutores por revisar','shield'],[state.tutores.length,'Tutores aprobados','users'],[state.revisores.length,'Docentes y directores','users']] : [[future.length,'Próximas tutorías','calendar'],[pending.length,'Solicitudes por revisar','file'],[state.chats.length,'Conversaciones activas','chat']];
  $('#page').innerHTML = `${homeDirectory(u)}<section class="hero"><div class="hero-content"><div class="eyebrow">${student?'Todo gran avance empieza aquí':'Aprendizaje entre pares'}</div><h2>${esc(heroTitle).replace('\n','<br>')}</h2><p>${heroDescription}</p>${heroAction}</div>${art()}</section><div class="stats">${stats.map(([n,t,i])=>`<div class="stat"><div class="stat-icon">${icon(i)}</div><div><strong>${n}</strong><span>${t}</span></div></div>`).join('')}</div><div class="dashboard-grid"><div><div class="section-title"><h2>${student?'Encuentra a tu próximo tutor':reviewer?'Solicitudes de validación':'Tu comunidad de aprendizaje'}</h2>${act('navigate','Ver todos',{page:student?'tutores':reviewer?'validaciones':'solicitudes'},'link-btn','arrow')}</div>${student ? `<div class="cards">${state.tutores.slice(0,2).map(tutorCard).join('')}</div>${!state.tutores.length?empty('Los próximos tutores están en camino','Cuando un docente apruebe un tutor, aparecerá aquí.','users'):''}` : reviewer ? state.validaciones.slice(0,2).map(approvalCard).join('') || empty('Todo al día','No tienes solicitudes de tutores pendientes de revisión.','shield') : u.estadoTutor!=='APROBADO'?`<div class="notice warning">Para aparecer en el catálogo y recibir solicitudes, completa tu perfil y solicita la aprobación de un docente o director.</div>` : pending.slice(0,2).map(requestCard).join('') || empty('Tu espacio está listo','Las solicitudes de los estudiantes aparecerán aquí.','users')}<section class="forum-preview"><div class="section-title"><h2>${u.rol==='DIRECTOR'?'Gestión académica':'La comunidad está preguntando'}</h2>${act('navigate',u.rol==='DIRECTOR'?'Validar tutores':'Ir al foro',{page:u.rol==='DIRECTOR'?'validaciones':'foro'},'link-btn','arrow')}</div>${u.rol==='DIRECTOR'?`<div class="panel"><p class="small muted">Revisa las solicitudes de los tutores que te elijan como revisor.</p></div>` : state.foro.slice(0,2).map(p=>`<article class="post-compact">${avatar(p.autor,p.anonimo?'':p.foto)}<div class="grow"><strong>${esc(p.titulo)}</strong><p>${esc(p.autor)} · ${fmt(p.fecha)} · ${p.comentarios.length} respuestas</p></div>${act('post','',{id:p.id},'icon-btn','arrow')}</article>`).join('') || empty('Hagamos la primera pregunta','Comparte una duda y aprendamos juntos.','chat')}</section></div><aside class="stack dashboard-aside"><section class="panel"><div class="section-title"><h2>Tu agenda</h2>${act('navigate','',{page:reviewer?'validaciones':'calendario'},'icon-btn','calendar')}</div>${calendarMarkup(new Date(),false)}${future.slice(0,2).map(reservationMini).join('') || `<p class="hint spaced">${reviewer?'Acompaña a la comunidad desde la revisión de tutores.':'Las tutorías confirmadas aparecerán aquí.'}</p>`}</section><section class="panel"><div class="section-title"><h2>${student?'Pequeños pasos, grandes logros':'Aprender y acompañar'}</h2>${icon('leaf')}</div>${student ? state.logros.slice(0,3).map(l=>goalRow(l)).join('') || `<p class="hint">Anota tu primera meta y empieza a construir tu progreso.</p>` : `<p class="hint">Una explicación con paciencia puede cambiar la forma en que alguien ve una materia.</p>`}${student?`<div class="spaced">${act('navigate','Ver mi progreso',{page:'progreso'},'link-btn','arrow')}</div>`:''}</section></aside></div>`;
}
function reservationMini(r) {
  const other = state.usuario.rol==='TUTOR'?r.nombreEstudiante:r.nombreTutor;
  return `<div class="upcoming"><strong>${esc(r.modalidad==='VIRTUAL'?'Tutoría virtual':'Tutoría presencial')}</strong><p>${fmt(r.fecha,{weekday:'short'})} · ${shortHour(r.hora)} · 1 hora<br>${esc(other)}</p>${act('open-pair','Abrir conversación',{other:state.usuario.rol==='TUTOR'?r.idEstudiante:r.idTutor},'btn secondary small','chat')}</div>`;
}
function renderTutors() {
  $('#page').innerHTML = `${heading('Alguien que te entiende.','Encuentra un tutor para esa materia que necesita un poco más de práctica.')}<div class="toolbar"><label class="search-field">${icon('search')}<input id="tutor-search" type="search" placeholder="Busca por nombre, materia o experiencia…" value="${esc(tutorSearch)}" aria-label="Buscar tutores"></label><span class="hint">Solo tutores aprobados por docentes y directores</span></div><div id="tutor-results" class="cards catalog"></div>`;
  filterTutors();
}
function filterTutors() {
  const q=tutorSearch.toLocaleLowerCase('es');
  const matches=state.tutores.filter(t=>[t.nombre,t.materias,t.descripcion].join(' ').toLocaleLowerCase('es').includes(q));
  $('#tutor-results').innerHTML=matches.map(tutorCard).join('') || empty('Aún no hay coincidencias','Prueba otra materia o espera a que se aprueben nuevos tutores.','search');
}
function requestCard(s) {
  const tutor = state.usuario.rol==='TUTOR', name=tutor?s.nombreEstudiante:s.nombreTutor;
  return `<article class="panel request spaced"><div class="between"><div class="flex gap">${avatar(name)}<div><h3>${esc(name)}</h3><p class="request-meta">${esc(s.materia)} · Solicitud #${s.idSolicitud}</p></div></div>${badge(s.estado)}</div><p class="request-message text">${esc(s.mensaje)}</p>${s.motivoRechazo?`<p class="notice danger">Motivo: ${esc(s.motivoRechazo)}</p>`:''}<div class="request-actions">${s.estado!=='RECHAZADO'?act('profile',tutor?'Ver perfil y progreso':'Ver perfil del tutor',{id:tutor?s.idEstudiante:s.idTutor},'btn secondary small','user'):''}${tutor && s.estado==='PENDIENTE'?act('request-accept','Aceptar solicitud',{id:s.idSolicitud},'btn primary small','check')+act('reject','Rechazar',{id:s.idSolicitud,kind:'request'},'btn danger small'):''}${s.estado==='APROBADO'?act('open-pair','Ir al chat',{other:tutor?s.idEstudiante:s.idTutor},'btn soft small','chat'):''}</div></article>`;
}
function renderRequests() {
  const tutor=state.usuario.rol==='TUTOR';
  const matches=state.solicitudes.filter(s=>requestFilter==='TODAS'||s.estado===requestFilter);
  $('#page').innerHTML=`${heading(tutor?'Comparte lo que sabes.':'El primer paso es conectar.',tutor?'Revisa las necesidades de cada estudiante antes de aceptar.':'Una vez aceptada tu solicitud, podrán conversar y acordar la fecha.',!tutor?act('navigate','Buscar un tutor',{page:'tutores'},'btn primary','plus'):'')}<div class="toolbar">${[['TODAS','Todas'],['PENDIENTE','Pendientes'],['APROBADO','Aceptadas'],['RECHAZADO','Rechazadas']].map(([k,v])=>act('filter-request',v,{filter:k},'filter-btn'+(requestFilter===k?' selected':''))).join('')}</div>${matches.map(requestCard).join('')||empty('Todo comienza con una conversación',tutor?'Las solicitudes de los estudiantes aparecerán aquí.':'Busca un tutor, abre su perfil y envíale una solicitud.','file')}`;
}
function calendarMarkup(date, full = false) {
  const y=date.getFullYear(),m=date.getMonth(), first=(new Date(y,m,1).getDay()+6)%7,days=new Date(y,m+1,0).getDate(), today=state.hoy;
  let html=`${!full?`<div class="mini-cal-header">${new Date(y,m,1).toLocaleDateString('es-GT',{month:'long',year:'numeric'})}</div>`:''}<div class="calendar-grid">${['L','M','M','J','V','S','D'].map(s=>`<div class="weekday">${s}</div>`).join('')}`;
  for(let i=0;i<first;i++)html+='<span class="cal-day faded"></span>';
  for(let d=1;d<=days;d++){
    const key=`${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`,mark=state.marcas.find(x=>x.fecha===key),reserved=state.reservas.some(r=>r.fecha===key),goal=state.logros.some(l=>l.fecha===key&&!l.completado);
    const type=key===today?'today':reserved?'reserved':mark?.tipo==='OCUPADO'?'busy':mark?.tipo==='PREFERENCIA'?'preferred':'';
    html+=full?`<button class="cal-day ${type} ${goal?'goal':''}" data-action="day" data-date="${key}" aria-label="${fmt(key)}${reserved?', tutoría confirmada':''}${mark?', '+mark.tipo.toLowerCase():''}">${d}</button>`:`<span class="cal-day ${type} ${goal?'goal':''}">${d}</span>`;
  }
  return html+'</div><div class="legend"><span><i></i>Tutoría</span><span><i class="preferred"></i>Preferencia</span><span><i class="busy"></i>Ocupado</span><span><i class="goal"></i>Meta</span></div>';
}
function renderCalendar() {
  const future=state.reservas.filter(r=>r.fecha>=state.hoy);
  $('#page').innerHTML=`${heading('Haz espacio para aprender.','Tus tutorías se confirman cuando ambas personas aceptan la fecha.',act('day','Marcar una fecha',{date:state.hoy},'btn primary','plus'))}<div class="calendar-layout"><section class="panel calendar-full"><div class="between"><h2 class="calendar-month">${month.toLocaleDateString('es-GT',{month:'long',year:'numeric'})}</h2><div class="flex">${act('month','',{step:-1},'icon-btn','left')}${act('month','Hoy',{step:0},'btn secondary small')}${act('month','',{step:1},'icon-btn','right')}</div></div><div class="spaced">${calendarMarkup(month,true)}</div><p class="hint spaced">Selecciona un día para indicar que estás ocupado o que prefieres esa fecha. Las tutorías duran una hora y se acuerdan desde el chat.</p></section><aside class="stack"><section class="panel"><div class="section-title"><h2>Próximas tutorías</h2>${icon('calendar')}</div>${future.map(r=>`<div class="reservation-row"><strong>${fmt(r.fecha,{weekday:'long'})} · ${shortHour(r.hora)}</strong><p>${esc(state.usuario.rol==='TUTOR'?r.nombreEstudiante:r.nombreTutor)}<br>${esc(r.modalidad==='VIRTUAL'?'Virtual':'Presencial')} · ${esc(r.lugar||'Por acordar')}</p>${act('open-pair','Abrir chat',{other:state.usuario.rol==='TUTOR'?r.idEstudiante:r.idTutor},'link-btn','chat')} ${act('cancel-reservation','Solicitar cancelación',{id:r.idReserva},'link-btn')}</div>`).join('')||`<p class="hint">Todavía no tienes tutorías confirmadas. Envía una propuesta de fecha desde una conversación.</p>`}</section><div class="notice">La otra persona debe aceptar una propuesta para reservar o cancelar una tutoría. Los horarios que se traslapan se bloquean automáticamente.</div></aside></div>`;
}
function goalRow(l, big = false) {
  return `<div class="goal-row ${l.completado?'done':''} ${big?'goal-big panel':''}"><button class="check ${l.completado?'done':''}" data-action="goal-toggle" data-id="${l.id}" data-completed="${l.completado?0:1}" aria-label="${l.completado?'Marcar pendiente':'Completar'}: ${esc(l.tema)}">${l.completado?icon('check'):''}</button><div class="grow"><strong>${esc(l.tema)}</strong><p>${esc(kindName[l.tipo]||'Tema')}${l.fecha?' · '+fmt(l.fecha):''}</p></div>${big?act('goal-delete','',{id:l.id},'icon-btn','trash'):''}</div>`;
}
function renderProgress() {
  const total=state.logros.length,done=state.logros.filter(l=>l.completado).length,pct=total?Math.round(done/total*100):0;
  const rows=state.logros.filter(l=>goalFilter==='TODOS'?true:goalFilter==='COMPLETADOS'?!!l.completado:goalFilter==='SIN_COMPLETAR'?!l.completado:l.tipo===goalFilter);
  $('#page').innerHTML=`${heading('Cada pequeño paso cuenta.','Guarda tus temas, metas y pendientes. Reconoce todo lo que ya avanzaste.',act('new-goal','Agregar progreso',{},'btn primary','plus'))}<section class="panel progress-overview"><svg class="progress-circle" viewBox="0 0 100 100" role="img" aria-label="${pct}% completado"><circle cx="50" cy="50" r="42"/><circle class="value" cx="50" cy="50" r="42" stroke-dasharray="${pct*2.639} 263.9"/><text x="50" y="50">${pct}%</text></svg><div><h2>${done} de ${total} pasos completados</h2><p>No tienes que entenderlo todo hoy. Solo seguir avanzando.</p></div></section><div class="toolbar">${[['TODOS','Todos'],['SIN_COMPLETAR','Por completar'],['COMPLETADOS','Completados'],['TEMA','Temas'],['META','Metas'],['PENDIENTE','Actividades'],['FECHA','Fechas']].map(([k,v])=>act('filter-goal',v,{filter:k},'filter-btn'+(goalFilter===k?' selected':''))).join('')}</div><div class="stack">${rows.map(l=>goalRow(l,true)).join('')||empty('Un buen lugar para empezar','Agrega una meta, un tema que quieras dominar o una fecha importante.','award')}</div>`;
}
function renderForum() {
  $('#page').innerHTML=`${heading('Las buenas preguntas nos conectan.','Comparte tus dudas, aporta ideas y aprende junto a la comunidad.',state.usuario.rol==='ESTUDIANTE'?act('new-post','Hacer una pregunta',{},'btn primary','plus'):'')}<div class="toolbar"><label class="search-field">${icon('search')}<input id="forum-search" type="search" value="${esc(forumSearch)}" placeholder="Busca preguntas o temas…" aria-label="Buscar preguntas"></label><span class="hint">Los estudiantes pueden publicar de forma anónima.</span></div><div id="forum-results"></div>`;
  filterForum();
}
function filterForum() {
  const posts=state.foro.filter(p=>[p.titulo,p.texto].join(' ').toLocaleLowerCase('es').includes(forumSearch.toLocaleLowerCase('es')));
  $('#forum-results').innerHTML=posts.map(p=>postMarkup(p,false)).join('')||empty('El espacio para tus preguntas','Publica la primera duda o prueba otra búsqueda.','chat');
}
function postMarkup(p, expanded) {
  return `<article class="panel post"><div class="post-author">${avatar(p.autor,p.anonimo?'':p.foto)}<div class="grow"><strong>${esc(p.autor)}</strong><small>${p.anonimo?'Anónimo':roleName[p.rol]} · ${fmt(p.fecha,{hour:'2-digit',minute:'2-digit'})}</small></div><span class="chip">Comunidad UVG</span></div><h3>${esc(p.titulo)}</h3><p class="post-text text">${esc(p.texto)}</p>${p.adjuntos.length?`<div class="chips spaced">${p.adjuntos.map(a=>`<a class="btn secondary small" href="/archivo/${esc(a.id)}" target="_blank" rel="noopener">${icon('clip')}${esc(a.nombre)}</a>`).join('')}</div>`:''}<div class="post-footer between"><span>${icon('chat')} ${p.comentarios.length} ${p.comentarios.length===1?'respuesta':'respuestas'}</span><div class="flex gap">${p.mia?`<label class="link-btn">${icon('clip')}Adjuntar<input type="file" class="hidden" data-upload-forum="${esc(p.id)}"></label>`:''}${act(expanded?'reply':'post',expanded?'Responder':'Ver conversación',{id:p.id},'link-btn','arrow')}</div></div>${expanded?commentsMarkup(p):''}</article>`;
}
function commentsMarkup(p) {
  const all=p.comentarios;
  function comment(c,depth=0) {
    return `<div class="comment ${depth?'nested':''}"><div class="between"><div class="flex gap">${avatar(c.autor,c.anonimo?'':c.foto)}<strong>${esc(c.autor)}</strong></div><span class="comment-meta">${fmt(c.fecha)}</span></div>${c.padre?`<span class="comment-meta">En respuesta a ${esc(all.find(a=>a.id===c.padre)?.autor||'un comentario')}</span>`:''}<p class="text">${esc(c.texto)}</p>${act('reply','Responder',{id:p.id,parent:c.id},'link-btn small')}</div>`;
  }
  // El orden cronológico mantiene accesibles incluso hilos antiguos con padres ausentes.
  return `<div class="comments">${all.map(c=>comment(c,c.padre?1:0)).join('')||'<p class="hint">Sé la primera persona en aportar una respuesta.</p>'}</div>`;
}

function renderChats() {
  if(!selectedChat || !state.chats.some(c=>c.id===selectedChat)) selectedChat=state.chats[0]?.id || '';
  const c=state.chats.find(c=>c.id===selectedChat);
  $('#page').innerHTML=`${heading('Una conversación que te acerca.','Mensajes privados, archivos y fechas, en un mismo lugar.')}<div class="chat-layout"><aside class="chat-list"><div class="chat-list-header">TUS CONVERSACIONES · ${state.chats.length}</div>${state.chats.map(c=>`<button class="conversation ${c.id===selectedChat?'selected':''}" data-action="select-chat" data-id="${esc(c.id)}">${avatar(c.nombre,c.foto)}<div class="grow"><strong class="truncate">${esc(c.nombre)}</strong><p class="truncate">${esc(c.ultimo)}</p></div></button>`).join('')||'<p class="hint">Aquí aparecerán tus tutorías aceptadas.</p>'}</aside><section class="chat-main">${c?`<header class="chat-header">${avatar(c.nombre,c.foto)}<div class="grow"><h3>${esc(c.nombre)}</h3><p>Conversación privada · Tutoría aceptada</p></div>${act('profile','',{id:c.otro},'icon-btn','user')}<button class="btn secondary small" data-action="meeting" data-id="${esc(c.id)}" title="Compartir enlace de videollamada externa">${icon('video')}<span class="btn-label">Sala virtual</span></button>${act('propose','Proponer fecha',{id:c.id},'btn soft small','calendar')}</header><div class="chat-body" id="chat-body"><div class="loader">Cargando conversación…</div></div><form class="composer" data-form="message"><input type="hidden" name="chat" value="${esc(c.id)}"><label class="icon-btn" title="Enviar imagen o archivo">${icon('clip')}<span class="sr-only">Adjuntar archivo</span><input type="file" class="hidden" id="chat-file"></label><textarea name="texto" placeholder="Escribe un mensaje…" rows="1" maxlength="6000" required aria-label="Mensaje"></textarea><button class="btn primary" type="submit" aria-label="Enviar mensaje">${icon('send')}</button></form>`:empty('Conecta para empezar','El chat se habilita cuando un tutor acepta la solicitud del estudiante.','chat',state.usuario.rol==='ESTUDIANTE'?act('navigate','Explorar tutores',{page:'tutores'},'btn primary small'):'')}</section></div>`;
  if(c)refreshChat(true).catch(e=>toast(e.message,true));
}
function messageText(text) {
  // Solo reconocemos enlaces de reunión generados por Java; el resto siempre es texto.
  return esc(text).replace(/https:\/\/meet\.jit\.si\/TutorMatch-[a-f0-9-]{36}/g,url=>`<a href="${url}" target="_blank" rel="noopener noreferrer">Abrir sala virtual ↗</a>`);
}
async function refreshChat(force = false) {
  if(chatBusy || !selectedChat || view!=='mensajes' || !state?.usuario) return;
  chatBusy=true; const requested=selectedChat, version=routeVersion;
  try {
    const data=await api('chat?id='+encodeURIComponent(requested));
    if(version!==routeVersion || requested!==selectedChat || !$('#chat-body'))return;
    const sig=JSON.stringify(data);
    if(!force && sig===chatSignature)return;
    chatSignature=sig;
    const body=$('#chat-body'),atBottom=body.scrollHeight-body.scrollTop-body.clientHeight<90;
    body.innerHTML=`<div class="notice small">Este chat está disponible porque la solicitud de tutoría fue aceptada. Comparte solo la información necesaria para aprender.</div>${data.propuestas.filter(p=>p.estado==='PENDIENTE').map(p=>`<div class="proposal"><strong>${icon('calendar')} ${p.tipo==='ELIMINAR'?'Solicitud de cancelación':'Nueva propuesta de tutoría'}</strong><p>${fmt(p.fecha,{weekday:'long'})} · ${shortHour(p.hora)}${p.tipo==='RESERVAR'?' · '+(p.modalidad==='VIRTUAL'?'Virtual':'Presencial')+' · '+esc(p.lugar):''}</p>${p.idEmisor!==state.usuario.id?`<div class="request-actions">${act('proposal-answer','Aceptar',{id:p.idSolicitudCal,accept:1},'btn primary small','check')}${act('proposal-answer','Rechazar',{id:p.idSolicitudCal,accept:0},'btn secondary small')}</div>`:'<p>Esperando la respuesta de la otra persona.</p>'}</div>`).join('')}${data.propuestas.filter(p=>p.estado!=='PENDIENTE').slice(0,5).reverse().map(p=>`<div class="hint">${p.tipo==='ELIMINAR'?'Cancelación':'Propuesta'} · ${fmt(p.fecha)} ${shortHour(p.hora)} · ${statusName[p.estado]||p.estado}</div>`).join('')}${data.mensajes.map(m=>`<div class="message ${m.emisor===state.usuario.id?'mine':''}"><div class="message-author">${avatar(m.nombre,m.foto)}<span>${esc(m.emisor===state.usuario.id?'Tú':m.nombre)}</span></div><div class="message-bubble">${messageText(m.texto||'')}${m.archivo?m.tipo==='IMAGEN'?`<a href="${esc(m.archivo)}" target="_blank" rel="noopener"><img src="${esc(m.archivo)}" alt="${esc(m.nombreArchivo)}" loading="lazy"></a>`:m.tipo==='VIDEO'?`<video src="${esc(m.archivo)}" controls preload="metadata"></video>`:`<a href="${esc(m.archivo)}" target="_blank" rel="noopener">${icon('file')} ${esc(m.nombreArchivo||'Descargar archivo')}</a>`:m.tipo!=='TEXTO'?'<span class="hint">El archivo original no está disponible en esta computadora.</span>':''}</div><small>${fmt(m.fecha)} · ${clock(m.fecha)}</small></div>`).join('')}`;
    if(force||atBottom)body.scrollTop=body.scrollHeight;
    $$('img',body).forEach(i=>i.addEventListener('load',()=>{if(atBottom||force)body.scrollTop=body.scrollHeight;},{once:true}));
  } finally {chatBusy=false;}
}

function imageFields(foto='',banner='') {
  return `<div class="form-row"><label>Foto de perfil<input type="hidden" name="foto" value="${esc(foto)}"><input type="file" accept="image/png,image/jpeg,image/gif" data-image-field="foto"><span class="hint">PNG, JPG o GIF. Máximo 25 MB.</span><span class="image-slot">${safeImage(foto)?`<img class="image-preview" src="${esc(foto)}" alt="Foto actual">`:''}</span></label><label>Banner<input type="hidden" name="banner" value="${esc(banner)}"><input type="file" accept="image/png,image/jpeg,image/gif" data-image-field="banner"><span class="hint">Recomendado: imagen horizontal.</span><span class="image-slot">${safeImage(banner)?`<img class="image-preview" src="${esc(banner)}" alt="Banner actual">`:''}</span></label></div>`;
}
function renderSettings() {
  const u=state.usuario;
  $('#page').innerHTML=`${heading('Tu espacio, a tu manera.','Cuenta un poco sobre ti y mantén tu información al día.')}<div class="settings"><section class="panel"><h2>Información de perfil</h2><form class="form-grid" data-form="profile"><label>Nombre completo<input name="nombre" value="${esc(u.nombre)}" maxlength="100" required autocomplete="name"></label><label>Correo institucional<input value="${esc(u.correo)}" disabled><span class="hint">Tu correo y rol se conservan con tu cuenta.</span></label><label>Sobre ti<textarea name="descripcion" maxlength="3000" placeholder="Tu carrera, intereses, experiencia o necesidades de aprendizaje…">${esc(u.descripcion)}</textarea></label>${imageFields(u.foto,u.banner)}<div><button class="btn primary" type="submit">Guardar perfil</button></div></form></section><aside class="stack"><section class="panel"><h2>Cambiar contraseña</h2><form class="form-grid" data-form="password"><label>Contraseña actual<input type="password" name="actual" required autocomplete="current-password"></label><label>Nueva contraseña<input type="password" name="nueva" minlength="8" maxlength="200" required autocomplete="new-password"></label><label>Confirmar nueva contraseña<input type="password" name="confirmacion" required autocomplete="new-password"></label><p class="hint">Al menos 8 caracteres, letras y números, sin espacios.</p><div><button type="submit" class="btn secondary">Actualizar contraseña</button></div></form></section>${['ESTUDIANTE','TUTOR'].includes(u.rol)?`<section class="panel"><h2>Tu forma de participar</h2><p class="hint">Actualmente participas como ${roleName[u.rol].toLowerCase()}. ${u.rol==='ESTUDIANTE'?'Para brindar tutorías necesitas la aprobación de un docente o director.':'Puedes volver a solicitar tutorías como estudiante.'}</p><p class="notice warning spaced">El cambio cierra tus solicitudes y reservas del rol anterior.${u.rol==='TUTOR'?' También elimina la información específica de tutor.':''}</p><div class="spaced">${act('change-role','Cambiar a '+(u.rol==='ESTUDIANTE'?'tutor':'estudiante'),{},'btn secondary')}</div></section>`:''}<section class="panel danger-zone"><h2>Desactivar cuenta</h2><p class="hint">Tu perfil dejará de estar disponible. Podrás reactivarlo con tu correo y contraseña desde la pantalla de acceso.</p><div class="spaced">${act('delete-account','Desactivar mi cuenta',{},'btn danger')}</div></section></aside></div>`;
}
function renderApplication() {
  const u=state.usuario,s=state.miSolicitud[0]||{},editable=!['PENDIENTE','APROBADO'].includes(s.estado);
  $('#page').innerHTML=`${heading('Comparte lo que has aprendido.','Completa tu presentación y elige a la persona que revisará tu solicitud.')}<div class="two-cols"><section class="panel"><div class="section-title"><h2>Solicitud de tutor</h2>${badge(s.estado||'BORRADOR')}</div>${s.motivoRechazo?`<div class="notice danger">Motivo del rechazo: ${esc(s.motivoRechazo)}<br>Puedes corregir tus datos y enviar una nueva solicitud.</div>`:''}${!editable?`<div class="notice">${s.estado==='APROBADO'?'Tu perfil está aprobado. Ya puedes recibir solicitudes de estudiantes.':'Tu solicitud está en revisión. Podrás continuar cuando la persona elegida la apruebe o la devuelva con un motivo.'}</div>`:`<form class="form-grid spaced" data-form="application"><label>Nombre completo<input name="nombre" value="${esc(s.nombreCompleto??u.nombre)}" maxlength="100"></label><label>Experiencia y presentación<textarea name="descripcion" maxlength="3000" placeholder="Cuéntanos qué sabes y cómo te gustaría ayudar…">${esc(s.descripcion??u.descripcion)}</textarea></label><label>Materias que impartes<input name="materias" maxlength="1000" value="${esc(s.materias||'')}" placeholder="Cálculo 1, Física 1, Programación…"><span class="hint">Separa las materias por comas.</span></label>${imageFields(s.fotoPerfil||u.foto,s.banner||u.banner)}<label>Docente o director revisor<select name="revisor"><option value="">Selecciona a tu revisor</option>${state.revisores.map(r=>`<option value="${r.id}">${esc(r.nombre)} · ${roleName[r.rol]}</option>`).join('')}</select></label><div class="flex gap wrap"><button type="submit" class="btn primary" name="decision" value="enviar">Enviar solicitud ${icon('arrow')}</button><button type="submit" class="btn secondary" name="decision" value="borrador" formnovalidate>Guardar borrador</button></div></form>`}</section><aside><div class="panel"><h2>Un perfil que inspire confianza.</h2><p class="hint spaced">Comparte tus materias, experiencia y tu forma de enseñar. Agrega una foto y un banner que te representen.</p><div class="subtle-divider"></div><div class="stack"><p class="hint">${icon('file')} Guarda tu borrador y continúa después.</p><p class="hint">${icon('shield')} Solo el revisor que elijas puede atender tu solicitud.</p><p class="hint">${icon('users')} Cuando te aprueben, aparecerás en el catálogo.</p></div></div>${!state.revisores.length?'<div class="notice warning spaced">Todavía no hay docentes o directores registrados. Necesitas un docente o director con cuenta activa para revisar tu solicitud.</div>':''}</aside></div>`;
}
function approvalCard(s) {
  return `<article class="panel request spaced"><div class="between"><div class="flex gap">${avatar(s.nombreCompleto,s.fotoPerfil)}<div><h3>${esc(s.nombreCompleto)}</h3><p class="request-meta">${esc(s.correo||'Solicitud de tutor')}</p></div></div>${badge(s.estado)}</div><div class="chips">${String(s.materias||'').split(',').map(m=>`<span class="chip">${esc(m.trim())}</span>`).join('')}</div><p class="request-message text">${esc(s.descripcion)}</p>${s.banner?`<div class="tutor-cover">${cover(s.banner)}</div>`:''}<div class="request-actions">${act('approve-form','Aprobar tutor',{id:s.idTutor},'btn primary small','check')}${act('reject','Rechazar con motivo',{id:s.idTutor,kind:'tutor'},'btn danger small')}</div></article>`;
}
function ratingOptions(selected='') { return [10,9,8,7,6,5,4,3,2,1].map(n=>`<option value="${n}" ${String(selected)===String(n)?'selected':''}>${n}</option>`).join(''); }
function ratedTutorsPanel() {
  const list=state.tutoresAprobados||[];
  if(!list.length)return '';
  return `<section class="panel spaced"><div class="section-title"><h2>Tutores aprobados</h2></div><p class="hint">La calificación es interna, va de 1 a 10 y reemplaza a la anterior.</p><div class="stack spaced">${list.map(t=>`<div class="between"><div><strong>${esc(t.nombre)}</strong><div class="hint">${esc(t.materias||'')} · ${t.calificacion?'Calificación: '+esc(t.calificacion)+'/10':'Sin calificar'}</div></div>${act('rate-form','Calificar',{id:t.id,name:t.nombre},'btn secondary small')}</div>`).join('')}</div></section>`;
}
function authorizationPanel() {
  if(state.usuario.rol!=='DIRECTOR')return '';
  const list=state.autorizados||[];
  return `<section class="panel spaced"><div class="section-title"><h2>Autorizar cuentas</h2></div><p class="hint">Solo los correos autorizados pueden crear una cuenta de docente o de director de carrera.</p><form class="form-grid spaced" data-form="authorize"><label>Correo institucional<input name="correo" type="email" required maxlength="120" placeholder="nombre@uvg.edu.gt"></label><label>Tipo de cuenta<select name="tipo"><option value="docente">Docente</option><option value="director">Director de carrera</option></select></label><button type="submit" class="btn primary">Autorizar correo</button></form><div class="stack spaced">${list.map(a=>`<div class="between"><span>${esc(a.correo)} · ${a.tipo==='director'?'Director de carrera':'Docente'}</span><span class="hint">${Number(a.registrado)?'Cuenta creada':'Pendiente de registro'}</span></div>`).join('')||'<p class="hint">Todavía no hay correos autorizados.</p>'}</div></section>`;
}
function renderApprovals() { $('#page').innerHTML=`${heading('Acompaña a los próximos tutores.','Solo aparecen las solicitudes que te eligieron como revisor.')}<div class="two-cols">${state.validaciones.map(approvalCard).join('')||empty('Todo está al día','No tienes solicitudes pendientes de revisión.','shield')}</div>${ratedTutorsPanel()}${authorizationPanel()}`; }
function renderAuth() {
  const register=authTab==='registro',reactivate=authTab==='reactivar';
  $('#app').innerHTML=`<main class="auth ${register?'auth-register':''}"><section class="auth-visual"><div class="brand"><span class="brand-symbol">T</span>TutorMatch<span class="small muted">®</span></div><div class="auth-visual-content"><div class="eyebrow">Tutorías · Universidad del Valle de Guatemala</div><h1>${register?'Tu comunidad empieza contigo.':'Aprender juntos nos lleva más lejos.'}</h1><p>${register?'Comparte lo que sabes, encuentra apoyo y da el siguiente paso con la comunidad UVG.':'Encuentra tu tutor, resuelve tus dudas y sigue creciendo a tu ritmo.'}</p><figure class="jack-welcome"><div class="jack-frame"><img src="/jack.png" width="1205" height="1305" alt="Jack, la mascota de UVG, saludando" fetchpriority="high"></div><figcaption>Un nuevo comienzo, en buena compañía.</figcaption></figure></div><div class="auth-footer-note">Comunidad UVG · Tutorías entre pares</div></section><section class="auth-form-container"><div class="auth-form"><span class="eyebrow">Tu comunidad de aprendizaje</span><h2>${register?'Tu siguiente paso empieza aquí':reactivate?'Volvamos a aprender juntos':'Qué bueno tenerte aquí'}</h2><p>${register?'Crea tu cuenta con un correo institucional':reactivate?'Reactiva la cuenta que habías desactivado':'Entra a tu espacio. Hay mucho por descubrir'}</p><div class="auth-tabs"><button data-action="auth-tab" data-tab="login" class="${authTab==='login'?'active':''}">Iniciar sesión</button><button data-action="auth-tab" data-tab="registro" class="${register?'active':''}">Crear cuenta</button><button data-action="auth-tab" data-tab="reactivar" class="${reactivate?'active':''}">Reactivar</button></div><form data-form="auth" class="form-grid"><div id="auth-error" class="auth-error hidden" role="alert"></div>${register?'<label>Nombre completo<input name="nombre" placeholder="¿Cómo te llamas?" maxlength="100" required autocomplete="name"></label>':''}<label>Correo institucional<input type="email" name="correo" placeholder="name.nombre@uvg.edu.gt" maxlength="120" autocomplete="username" title="Usa un correo con terminación @uvg.edu.gt" required></label><label>Contraseña<input type="password" name="clave" placeholder="${register?'Crea una contraseña segura':'Tu contraseña'}" maxlength="200" ${register?'minlength="8"':''} required autocomplete="${register?'new-password':'current-password'}"></label>${register?'<label>Confirmar contraseña<input type="password" name="confirmacion" placeholder="Repite tu contraseña" maxlength="200" required autocomplete="new-password"></label><label>¿Cómo participarás?<select name="rol"><option value="ESTUDIANTE">Como estudiante: quiero recibir apoyo</option><option value="TUTOR">Como tutor: quiero ayudar a otros</option></select></label><p class="hint">La contraseña necesita 8 caracteres, letras y números, sin espacios</p>':''}<button type="submit" class="btn primary full">${register?'Crear mi cuenta':reactivate?'Reactivar cuenta':'Entrar a mi espacio'} ${icon('arrow')}</button></form><p class="auth-foot">Exclusivo para la comunidad UVG · Acceso según tu tipo de cuenta<br>${register?'Regístrate directamente con tu correo @uvg.edu.gt, sin autorización previa':'¿Es tu primera vez? Usa la pestaña Crear cuenta'}</p></div></section></main>`;
}

function modal(title, content) {
  const d=$('#modal');
  $('#modal-content').innerHTML=`<header class="modal-header"><h2 id="modal-title">${esc(title)}</h2>${act('close-modal','',{},'icon-btn','close')}</header><div class="modal-body">${content}</div>`;
  $('[data-action="close-modal"]').setAttribute('aria-label','Cerrar ventana');
  if(!d.open)d.showModal();
}
function closeModal() { $('#modal').close(); }
async function showProfile(id) {
  modal('Cargando perfil…','<div class="loader">Un momento…</div>');
  const data=await api('perfil?id='+encodeURIComponent(id));
  if(!$('#modal').open)return;
  const p=data.perfil;
  const reserves=(data.reservas||[]).filter(r=>r.fecha>=state.hoy);
  modal('Conoce a '+p.nombre.split(' ')[0],`<div class="profile-banner">${safeImage(p.banner)?`<img class="profile-banner" src="${esc(p.banner)}" alt="Banner de ${esc(p.nombre)}">`:''}</div><div class="profile-heading">${avatar(p.nombre,p.foto,'xlarge')}<div><h2>${esc(p.nombre)}</h2><span class="muted">${roleName[p.rol]} · UVG ${p.estadoTutor==='APROBADO'?'· Verificado':''}</span></div></div><div class="modal-profile-info"><p class="text">${esc(p.descripcion||'Este perfil todavía no tiene una descripción.')}</p>${data.materias?`<div class="chips">${data.materias.split(',').map(m=>`<span class="chip">${esc(m.trim())}</span>`).join('')}</div>`:''}${data.calificacion!==undefined?`<p class="hint">Calificación interna: ${data.calificacion?esc(data.calificacion)+'/10':'sin calificar'}</p>`:''}<div class="subtle-divider"></div><h3>Disponibilidad y calendario</h3><p class="hint spaced">${[...(data.marcas||[]).map(m=>`${fmt(m.fecha)} · ${m.tipo==='PREFERENCIA'?'Fecha de preferencia':'Día ocupado'}`),...reserves.map(r=>`${fmt(r.fecha)} · ${shortHour(r.hora)} · Horario reservado`)].map(esc).join('<br>')||'Sin fechas marcadas. Puedes acordar una fecha desde el chat después de que se acepte tu solicitud.'}</p>${data.logros?`<div class="subtle-divider"></div><h3>Progreso del estudiante</h3><div class="stack spaced">${data.logros.map(l=>`<div class="hint">${l.completado?'✓':'○'} ${esc(l.tema)} · ${esc(kindName[l.tipo]||'Tema')}${l.fecha?' · '+fmt(l.fecha):''}</div>`).join('')||'<p class="hint">Todavía no ha registrado su progreso.</p>'}</div>`:''}${p.rol==='TUTOR'?`<div class="subtle-divider"></div><h3>Experiencias de otros estudiantes</h3>${(data.resenas||[]).map(r=>`<div class="review"><div class="between"><strong>${esc(r.nombre)}</strong><span class="stars">${'★'.repeat(r.puntuacion)}${'☆'.repeat(5-r.puntuacion)}</span></div><p class="text">${esc(r.texto)}</p><span class="hint">${fmt(r.fecha)}</span></div>`).join('')||'<p class="hint spaced">Este tutor todavía no tiene reseñas.</p>'}${data.puedeResenar?`<div class="spaced">${act('review','Compartir mi experiencia',{id:p.id},'btn secondary small','star')}</div>`:''}`:''}${state.usuario.rol==='ESTUDIANTE'&&p.rol==='TUTOR'?`<div class="modal-footer spaced">${state.solicitudes.some(s=>s.idTutor===p.id&&s.estado==='APROBADO')?act('open-pair','Abrir conversación',{other:p.id},'btn primary','chat'):state.solicitudes.some(s=>s.idTutor===p.id&&s.estado==='PENDIENTE')?'<span class="badge PENDIENTE">Tu solicitud está pendiente</span>':act('request','Solicitar tutoría',{id:p.id},'btn primary','arrow')}</div>`:''}</div>`);
}
function showRequest(id) {
  const t=state.tutores.find(t=>t.id===Number(id)); if(!t)throw new Error('Este tutor no está disponible. Actualiza la página.');
  modal('Empecemos a aprender juntos.',`<p class="hint">Envía tu solicitud a ${esc(t.nombre)}. Podrá consultar tu perfil, progreso y disponibilidad para saber cómo ayudarte.</p><form class="form-grid spaced" data-form="request"><input type="hidden" name="tutor" value="${t.id}"><label>¿En qué materia necesitas apoyo?<select name="materia" required>${String(t.materias).split(',').filter(Boolean).map(m=>`<option value="${esc(m.trim())}">${esc(m.trim())}</option>`).join('')}</select></label><label>Cuéntale qué te gustaría trabajar<textarea name="mensaje" maxlength="3000" placeholder="Quisiera repasar… Estoy preparando…" required></textarea></label><div class="notice">Si acepta tu solicitud, se habilitará un chat privado. Allí podrán acordar fecha, hora y modalidad.</div><div class="modal-footer"><button class="btn primary" type="submit">Enviar solicitud ${icon('send')}</button></div></form>`);
}
function showDay(date) {
  const reservations=state.reservas.filter(r=>r.fecha===date),mark=state.marcas.find(m=>m.fecha===date),past=date<state.hoy;
  modal(fmt(date,{weekday:'long',year:'numeric'}),`<div class="stack">${reservations.map(reservationMini).join('')}${state.logros.filter(l=>l.fecha===date).map(l=>`<p class="notice">${esc(kindName[l.tipo])}: ${esc(l.tema)}</p>`).join('')}${past?'<p class="hint">Esta fecha ya pasó. Puedes consultar los eventos registrados.</p>':`<form data-form="mark" class="form-grid"><input type="hidden" name="fecha" value="${date}"><p class="hint">Marca tu disponibilidad para el día completo. Las reservas de tutoría se acuerdan desde el chat.</p><label>Disponibilidad<select name="tipo"><option value="PREFERENCIA" ${mark?.tipo==='PREFERENCIA'?'selected':''}>Prefiero esta fecha</option><option value="OCUPADO" ${mark?.tipo==='OCUPADO'?'selected':''}>Estoy ocupado este día</option><option value="QUITAR" ${!mark?'selected':''}>Sin marca de disponibilidad</option></select></label><div class="modal-footer"><button type="submit" class="btn primary">Guardar disponibilidad</button></div></form>`}</div>`);
}
function showProposal(id) {
  modal('Un buen momento para aprender.',`<form class="form-grid" data-form="proposal"><input type="hidden" name="chat" value="${esc(id)}"><div class="form-row"><label>Fecha<input type="date" name="fecha" min="${state.hoy}" value="${state.hoy}" required></label><label>Hora de inicio<input type="time" name="hora" required></label></div><label>Modalidad<select name="modalidad"><option value="VIRTUAL">Virtual</option><option value="PRESENCIAL">Presencial</option></select></label><label>Ubicación o indicaciones<input name="lugar" maxlength="300" placeholder="Biblioteca UVG, sala virtual del chat…" required></label><div class="notice">La tutoría dura una hora. Se guardará en ambos calendarios cuando la otra persona acepte la propuesta.</div><div class="modal-footer"><button class="btn primary" type="submit">Proponer fecha ${icon('calendar')}</button></div></form>`);
}
function showNewGoal() {
  modal('Un paso más en tu camino.',`<form class="form-grid" data-form="goal"><label>¿Qué quieres registrar?<input name="tema" maxlength="300" placeholder="Comprender integrales, terminar un proyecto…" required></label><div class="form-row"><label>Tipo<select name="tipo">${Object.entries(kindName).map(([k,v])=>`<option value="${k}">${v}</option>`).join('')}</select></label><label>Fecha objetivo (opcional)<input name="fecha" type="date"></label></div><div class="modal-footer"><button type="submit" class="btn primary">Guardar mi progreso ${icon('plus')}</button></div></form>`);
}
function showPost(id) {
  const p=state.foro.find(p=>p.id===id);if(!p)throw new Error('Esta publicación ya no está disponible.');
  modal('Una pregunta, muchas perspectivas.',postMarkup(p,true));
}
function showReply(id,parent='') {
  const p=state.foro.find(p=>p.id===id);
  modal('Aporta tu perspectiva.',`<p class="hint">${esc(p?.titulo||'Responder en el foro')}${parent?'<br>En respuesta a un comentario.':''}</p><form data-form="comment" class="form-grid spaced"><input type="hidden" name="id" value="${esc(id)}"><input type="hidden" name="padre" value="${esc(parent)}"><label>Tu respuesta<textarea name="texto" maxlength="6000" placeholder="Comparte una idea, un ejemplo o una explicación…" required></textarea></label>${state.usuario.rol==='ESTUDIANTE'?'<label class="checkbox-label"><input type="checkbox" name="anonimo">Responder de forma anónima</label>':''}<div class="modal-footer"><button class="btn primary" type="submit">Publicar respuesta</button></div></form>`);
}
function confirmDanger(kind) {
  const role=kind==='role',word=role?'CAMBIAR':'ELIMINAR';
  modal(role?'Cambiar tu forma de participar':'Desactivar tu cuenta',`<form data-form="${role?'role':'delete-account'}" class="form-grid"><div class="notice danger">${role?'Se cerrarán tus solicitudes y reservas del rol anterior. Si eras tutor, también se borrará tu presentación y aprobación de tutor.':'Tu perfil dejará de estar disponible y se cerrarán tus sesiones. Tus datos se conservan para poder reactivar la cuenta con tus credenciales.'}</div><label>Contraseña actual<input type="password" name="clave" required autocomplete="current-password"></label><label>Escribe ${word} para confirmar<input name="confirmacion" placeholder="${word}" pattern="${word}" required autocomplete="off"></label><div class="modal-footer">${act('close-modal','Volver',{},'btn secondary')}<button type="submit" class="btn danger">${role?'Confirmar cambio':'Desactivar cuenta'}</button></div></form>`);
}

document.addEventListener('click',async e=>{
  const b=e.target.closest('[data-action]');if(!b||b.disabled)return;
  const d=b.dataset;
  try {
    switch(d.action) {
      case 'navigate':closeModal();navigate(d.page);break;
      case 'menu':$('#sidebar').classList.toggle('open');break;
      case 'close-modal':closeModal();break;
      case 'auth-tab':authTab=d.tab;renderAuth();break;
      case 'refresh':await reload();toast('Tu información está actualizada.');break;
      case 'logout':await api('logout',{});closeModal();state=null;location.hash='';renderAuth();break;
      case 'profile':await showProfile(d.id);break;
      case 'request':showRequest(d.id);break;
      case 'request-accept':await mutate('solicitud-responder',{id:d.id,aceptar:1});break;
      case 'approve':await mutate('tutor-revisar',{id:d.id,aceptar:1});break;
      case 'approve-form':modal('Aprobar tutor',`<form class="form-grid" data-form="approve"><input type="hidden" name="id" value="${esc(d.id)}"><p class="hint">Al aprobar, esta persona podrá recibir solicitudes de estudiantes. Puedes calificar su experiencia de 1 a 10; la calificación es interna.</p><label>Calificación de su experiencia (opcional)<select name="calificacion"><option value="">Sin calificar</option>${ratingOptions()}</select></label><div class="modal-footer"><button class="btn primary" type="submit">Aprobar tutor</button></div></form>`);break;
      case 'rate-form':modal('Calificar a '+(d.name||'este tutor'),`<form class="form-grid" data-form="rate"><input type="hidden" name="id" value="${esc(d.id)}"><label>Calificación de 1 a 10<select name="calificacion">${ratingOptions(8)}</select></label><div class="modal-footer"><button class="btn primary" type="submit">Guardar calificación</button></div></form>`);break;
      case 'reject':modal('Un motivo que ayude a seguir.',`<form class="form-grid" data-form="reject"><input type="hidden" name="id" value="${esc(d.id)}"><input type="hidden" name="kind" value="${esc(d.kind)}"><p class="hint">Explica por qué no puedes aceptar esta solicitud. La persona podrá consultar este motivo.</p><label>Motivo del rechazo<textarea name="motivo" maxlength="1500" required></textarea></label><div class="modal-footer"><button class="btn danger" type="submit">Rechazar solicitud</button></div></form>`);break;
      case 'filter-request':requestFilter=d.filter;renderRequests();break;
      case 'filter-goal':goalFilter=d.filter;renderProgress();break;
      case 'open-pair':{
        const c=state.chats.find(c=>c.otro===Number(d.other));
        if(!c)throw new Error('La conversación todavía no está disponible. Actualiza la información o comprueba que la solicitud esté aceptada.');
        closeModal();navigate('mensajes',c.id);break;
      }
      case 'select-chat':navigate('mensajes',d.id);break;
      case 'propose':showProposal(d.id);break;
      case 'proposal-answer':await api('fecha-responder',{id:d.id,aceptar:d.accept}).then(r=>toast(r.mensaje));await reload(false);await refreshChat(true);break;
      case 'cancel-reservation':modal('Solicitar la cancelación',`<p class="hint">Se enviará una propuesta al chat. La reserva seguirá en ambos calendarios hasta que la otra persona acepte cancelarla.</p><div class="modal-footer spaced">${act('close-modal','Volver',{},'btn secondary')}${act('confirm-cancel','Enviar solicitud',{id:d.id},'btn danger')}</div>`);break;
      case 'confirm-cancel':await mutate('cancelar-reserva',{id:d.id});closeModal();break;
      case 'meeting':modal('Aprendamos cara a cara.',`<p class="hint">Se compartirá una sala externa de Jitsi Meet en el chat. Ambos deben abrir el enlace y permitir el micrófono o la cámara en su navegador. Para una llamada de voz, apaguen la cámara.</p><div class="notice spaced">Necesita Internet. El servicio externo puede pedir autenticación al anfitrión y está sujeto a su disponibilidad.</div><div class="modal-footer spaced">${act('share-meeting','Compartir enlace en el chat',{id:d.id},'btn primary','video')}</div>`);break;
      case 'share-meeting':{
        const r=await api('reunion',{chat:d.id});await refreshChat(true);
        modal('Tu sala virtual está compartida.',`<p class="hint">La otra persona puede abrir el mismo enlace desde el chat.</p><div class="modal-footer spaced"><a class="btn primary" href="${esc(r.url)}" target="_blank" rel="noopener noreferrer">Abrir sala virtual ↗</a></div>`);break;
      }
      case 'day':showDay(d.date);break;
      case 'month':month=Number(d.step)?new Date(month.getFullYear(),month.getMonth()+Number(d.step),1):new Date(new Date().getFullYear(),new Date().getMonth(),1);renderCalendar();break;
      case 'new-goal':showNewGoal();break;
      case 'goal-toggle':await mutate('logro-estado',{id:d.id,completado:d.completed},false);break;
      case 'goal-delete':modal('Quitar este paso',`<p class="hint">Se eliminará el registro de tu progreso.</p><div class="modal-footer spaced">${act('close-modal','Conservar',{},'btn secondary')}${act('confirm-goal-delete','Eliminar',{id:d.id},'btn danger')}</div>`);break;
      case 'confirm-goal-delete':await mutate('logro-eliminar',{id:d.id});closeModal();break;
      case 'new-post':modal('No hay preguntas pequeñas.',`<form class="form-grid" data-form="post"><label>Título de tu pregunta<input name="titulo" maxlength="180" placeholder="¿Qué te gustaría entender mejor?" required></label><label>Cuéntanos un poco más<textarea name="texto" maxlength="6000" placeholder="Agrega contexto, lo que ya intentaste o un ejemplo…" required></textarea></label><label class="checkbox-label"><input type="checkbox" name="anonimo">Publicar de forma anónima</label><p class="hint">Podrás adjuntar archivos después de publicar, desde la conversación.</p><div class="modal-footer"><button class="btn primary" type="submit">Publicar pregunta</button></div></form>`);break;
      case 'post':showPost(d.id);break;
      case 'reply':showReply(d.id,d.parent||'');break;
      case 'review':modal('Tu experiencia puede ayudar.',`<form class="form-grid" data-form="review"><input type="hidden" name="tutor" value="${esc(d.id)}"><p class="hint">Disponible después de una tutoría confirmada y finalizada. Puedes publicarla con tu nombre o de forma anónima.</p><label>¿Cómo fue tu experiencia?<select name="puntuacion"><option value="5">★★★★★ · Excelente</option><option value="4">★★★★☆ · Muy buena</option><option value="3">★★★☆☆ · Buena</option><option value="2">★★☆☆☆ · Mejorable</option><option value="1">★☆☆☆☆ · Mala</option></select></label><label>Tu comentario<textarea name="texto" maxlength="2000" required placeholder="¿Qué te ayudó? ¿Qué podría mejorar?"></textarea></label><label class="checkbox-label"><input type="checkbox" name="anonimo" value="1">Publicar de forma anónima</label><div class="modal-footer"><button type="submit" class="btn primary">Guardar reseña</button></div></form>`);break;
      case 'change-role':confirmDanger('role');break;
      case 'delete-account':confirmDanger('account');break;
    }
  }catch(err){toast(err.message,true);}
});

async function mutate(path,data,notify=true) {
  const r=await api(path,data);await reload();if(notify)toast(r.mensaje||'Cambios guardados.');return r;
}
document.addEventListener('submit',async e=>{
  const form=e.target.closest('form[data-form]');if(!form)return;e.preventDefault();
  if(uploading){toast('Espera a que termine la carga del archivo.',true);return;}
  const submitter=e.submitter;
  if(form.dataset.busy)return;
  form.dataset.busy='1';const buttons=$$('button[type="submit"]',form);buttons.forEach(b=>b.disabled=true);
  const data=Object.fromEntries(new FormData(form)),kind=form.dataset.form;
  try {
    switch(kind) {
      case 'auth':{
        const r=await api(authTab,data,false);
        if(authTab==='reactivar'){authTab='login';renderAuth();toast(r.mensaje);}else {location.hash='inicio';view='inicio';await reload();}break;
      }
      case 'message':await api('mensaje',data);form.reset();await refreshChat(true);break;
      case 'profile':await mutate('perfil',data);break;
      case 'password':await mutate('clave',data);break;
      case 'request':await mutate('solicitud',data);closeModal();navigate('solicitudes');break;
      case 'reject':await mutate(data.kind==='tutor'?'tutor-revisar':'solicitud-responder',{...data,aceptar:0});closeModal();break;
      case 'proposal':await api('fecha',data).then(r=>toast(r.mensaje));closeModal();await refreshChat(true);break;
      case 'mark':await mutate('marca',data);closeModal();break;
      case 'goal':await mutate('logro',data);closeModal();break;
      case 'post':await mutate('publicacion',data);closeModal();break;
      case 'comment':await reload(false);await api('comentario',data);await reload();showPost(data.id);toast('Respuesta publicada.');break;
      case 'application':await mutate(submitter?.value==='borrador'?'tutor-borrador':'tutor-enviar',data);break;
      case 'review':await mutate('resena',data);await showProfile(data.tutor);break;
      case 'approve':await mutate('tutor-revisar',{...data,aceptar:1});closeModal();break;
      case 'rate':await mutate('tutor-calificar',data);closeModal();break;
      case 'authorize':await mutate('autorizacion',data);break;
      case 'role':await mutate('rol',data);closeModal();break;
      case 'delete-account':await api('eliminar-cuenta',data);state=null;closeModal();renderAuth();toast('Tu cuenta está desactivada. Puedes reactivarla desde el acceso.');break;
    }
  } catch(err) {
    if(kind==='auth'&&$('#auth-error')){$('#auth-error').textContent=err.message;$('#auth-error').classList.remove('hidden');}
    else toast(err.message,true);
  } finally {delete form.dataset.busy;buttons.forEach(b=>b.disabled=false);}
});
document.addEventListener('input',e=>{
  if(e.target.id==='tutor-search'){tutorSearch=e.target.value;filterTutors();}
  if(e.target.id==='forum-search'){forumSearch=e.target.value;filterForum();}
});
async function upload(file,categoria,contexto='') {
  if(!file||file.size===0)throw new Error('Selecciona un archivo que no esté vacío.');
  if(file.size>25*1024*1024)throw new Error('El archivo supera el máximo de 25 MB.');
  const datos=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result).split(',')[1]);reader.onerror=()=>reject(new Error('No se pudo leer el archivo.'));reader.readAsDataURL(file);});
  return api('archivo',{categoria,contexto,nombre:file.name,datos});
}
let uploading=0;
document.addEventListener('change',async e=>{
  const input=e.target;if(input.type!=='file'||!input.files?.[0])return;
  const form=input.closest('form'),buttons=form?$$('button[type="submit"]',form):[];
  uploading++;buttons.forEach(b=>b.disabled=true);input.disabled=true;
  let status;
  try {
    if(input.dataset.imageField){
      status=document.createElement('span');status.className='upload-status';status.textContent='Guardando imagen…';input.parentElement.append(status);
      const r=await upload(input.files[0],'PERFIL');
      form.elements[input.dataset.imageField].value=r.url;
      $('.image-slot',input.parentElement).innerHTML=`<img class="image-preview" src="${esc(r.url)}" alt="Imagen seleccionada">`;
      status.textContent='Imagen cargada. Guarda el formulario para aplicar el cambio.';
    }else if(input.id==='chat-file'){
      const id=selectedChat;toast('Enviando archivo…');await upload(input.files[0],'CHAT',id);await refreshChat(true);toast('Archivo enviado.');
    }else if(input.dataset.uploadForum){
      const id=input.dataset.uploadForum;toast('Guardando adjunto…');await upload(input.files[0],'FORO',id);await reload();showPost(id);toast('Adjunto publicado.');
    }
  }catch(err){toast(err.message,true);if(status)status.textContent='No se pudo subir esta imagen.';}
  finally{uploading--;input.disabled=false;input.value='';if(!uploading)buttons.forEach(b=>b.disabled=false);}
});
$('#modal').addEventListener('click',e=>{if(e.target===$('#modal')){const r=$('#modal').getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeModal();}});
document.addEventListener('error',e=>{if(e.target instanceof HTMLImageElement){const parent=e.target.parentElement;if(parent?.classList.contains('avatar'))parent.textContent='N';else e.target.classList.add('hidden');}},true);
window.addEventListener('hashchange',route);
document.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey&&e.target.matches('.composer textarea')){e.preventDefault();e.target.closest('form').requestSubmit();}});
setInterval(()=>{if(document.hidden||!state?.usuario)return;if(view==='mensajes')refreshChat().catch(e=>toast(e.message,true));},3500);
setInterval(async()=>{
  if(document.hidden||!state?.usuario||$('#modal').open||uploading)return;
  try{await reload(false);$('#connection-warning')?.remove();}catch(_){if(!$('#connection-warning')){const e=document.createElement('div');e.className='connection';e.id='connection-warning';e.textContent='No se pudo actualizar. Revisa que Java siga ejecutándose.';document.body.append(e);}}
},20000);
reload().then(route).catch(e=>{$('#app').innerHTML=`<div class="boot"><span class="brand-symbol">n</span><p>${esc(e.message)}</p>${act('refresh','Volver a intentar',{},'btn primary')}</div>`;});
