/*
 * Este archivo describe las pantallas de registro (facultades, profesores, matrículas…) y tiene las
 * funciones que arman la tabla y el formulario siguiendo esa descripción. Cada página HTML solo
 * escribe en su <body> data-modulo="facultades" (o el módulo que sea) y carga este archivo.
 * Los datos se piden a la API REST del proyecto: cada módulo tiene su dirección, por ejemplo
 * /api/facultades, y el método HTTP dice la acción (leer, crear, cambiar, borrar). Lo que viaja es
 * JSON, un texto con llaves y comas que Java y el navegador entienden los dos.
 */
// En cada módulo: recurso = dirección, idField = nombre del identificador, columnas = lo que se ve
// en la tabla, campos = lo que pide el formulario y filtros = cajas de búsqueda.
const MODULOS = {
  facultades: {
    titulo: 'Facultades',
    recurso: 'facultades',
    idField: 'idFacultad',
    columnas: [
      { campo: 'idFacultad', etiqueta: 'ID' },
      { campo: 'nombre', etiqueta: 'Nombre' },
      { campo: 'decano', etiqueta: 'Decano' },
    ],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
      { nombre: 'decano', etiqueta: 'Decano', tipo: 'texto' },
    ],
  },
  profesores: {
    titulo: 'Profesores',
    recurso: 'profesores',
    idField: 'idProfesor',
    columnas: [
      { campo: 'idProfesor', etiqueta: 'ID' },
      { campo: 'nombre', etiqueta: 'Nombre' },
      { campo: 'apellido', etiqueta: 'Apellido' },
      { campo: 'email', etiqueta: 'Correo' },
      { campo: 'idFacultad', etiqueta: 'Facultad', ref: 'facultades' },
    ],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
      { nombre: 'apellido', etiqueta: 'Apellido', tipo: 'texto', requerido: true },
      { nombre: 'email', etiqueta: 'Correo', tipo: 'texto', requerido: true },
      { nombre: 'idFacultad', etiqueta: 'Facultad', tipo: 'referencia', ref: 'facultades', requerido: true },
    ],
  },
  periodos: {
    titulo: 'Periodos académicos',
    recurso: 'periodos',
    idField: 'idPeriodo',
    columnas: [
      { campo: 'idPeriodo', etiqueta: 'ID' },
      { campo: 'nombre', etiqueta: 'Nombre' },
      { campo: 'fechaInicio', etiqueta: 'Inicio', formato: 'fecha' },
      { campo: 'fechaFin', etiqueta: 'Fin', formato: 'fecha' },
      { campo: 'estado', etiqueta: 'Estado' },
    ],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
      { nombre: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'fecha', requerido: true },
      { nombre: 'fechaFin', etiqueta: 'Fecha de fin', tipo: 'fecha', requerido: true },
      { nombre: 'estado', etiqueta: 'Estado', tipo: 'lista', opciones: ['ACTIVO', 'INACTIVO', 'CERRADO'], requerido: true },
    ],
  },
  cursos: {
    titulo: 'Cursos',
    recurso: 'cursos',
    idField: 'idCurso',
    columnas: [
      { campo: 'idCurso', etiqueta: 'ID' },
      { campo: 'nombre', etiqueta: 'Curso' },
      { campo: 'creditos', etiqueta: 'Créditos' },
      { campo: 'idCarrera', etiqueta: 'Carrera', ref: 'carreras' },
      { campo: 'idProfesor', etiqueta: 'Profesor', ref: 'profesores' },
    ],
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre del curso', tipo: 'texto', requerido: true },
      { nombre: 'creditos', etiqueta: 'Créditos', tipo: 'numero', min: 1, max: 10, requerido: true },
      { nombre: 'idCarrera', etiqueta: 'Carrera', tipo: 'referencia', ref: 'carreras', requerido: true },
      { nombre: 'idProfesor', etiqueta: 'Profesor', tipo: 'referencia', ref: 'profesores', requerido: true },
    ],
  },
  secciones: {
    titulo: 'Secciones',
    recurso: 'secciones',
    idField: 'idSeccion',
    // Estas dos reglas se comprueban en Java al guardar; el texto solo avisa al usuario antes de que pierda el tiempo.
    ayuda: 'Un aula no puede repetir el mismo curso y periodo, y el cupo no puede bajar de los estudiantes ya matriculados.',
    columnas: [
      { campo: 'idSeccion', etiqueta: 'ID' },
      { campo: 'idCurso', etiqueta: 'Curso', ref: 'cursos' },
      { campo: 'idProfesor', etiqueta: 'Profesor', ref: 'profesores' },
      { campo: 'idPeriodo', etiqueta: 'Periodo', ref: 'periodos' },
      { campo: 'aula', etiqueta: 'Aula' },
      { campo: 'cuposOcupados', etiqueta: 'Ocupación', formato: 'cupos' },
    ],
    filtros: [
      { nombre: 'periodo', etiqueta: 'Filtrar por periodo', tipo: 'referencia', ref: 'periodos' },
    ],
    campos: [
      { nombre: 'idCurso', etiqueta: 'Curso', tipo: 'referencia', ref: 'cursos', requerido: true },
      { nombre: 'idProfesor', etiqueta: 'Profesor', tipo: 'referencia', ref: 'profesores', requerido: true },
      { nombre: 'idPeriodo', etiqueta: 'Periodo', tipo: 'referencia', ref: 'periodos', requerido: true },
      { nombre: 'aula', etiqueta: 'Aula', tipo: 'texto', requerido: true },
      { nombre: 'cupoMaximo', etiqueta: 'Cupo máximo', tipo: 'numero', min: 1, max: 200, requerido: true },
    ],
  },
  matriculas: {
    titulo: 'Matrículas',
    recurso: 'matriculas',
    idField: 'idMatricula',
    ayuda: 'La sección decide el curso y el periodo. Al elegir el estudiante, la lista solo trae los grupos que él puede inscribir: de su carrera, con el periodo abierto, cupo disponible y sin pasarse de 20 créditos ni repetir un curso del periodo.',
    columnas: [
      { campo: 'idMatricula', etiqueta: 'ID' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idSeccion', etiqueta: 'Sección', ref: 'secciones' },
      { campo: 'periodo', etiqueta: 'Periodo' },
      { campo: 'nota', etiqueta: 'Nota' },
      { campo: 'estado', etiqueta: 'Estado', formato: 'academico' },
    ],
    filtros: [
      { nombre: 'curso', etiqueta: 'Filtrar por curso', tipo: 'referencia', ref: 'cursos' },
      { nombre: 'estudiante', etiqueta: 'Filtrar por estudiante', tipo: 'referencia', ref: 'estudiantes' },
      { nombre: 'seccion', etiqueta: 'Filtrar por sección', tipo: 'referencia', ref: 'secciones' },
    ],
    campos: [
      { nombre: 'idEstudiante', etiqueta: 'Estudiante', tipo: 'referencia', ref: 'estudiantes', requerido: true },
      { nombre: 'idSeccion', etiqueta: 'Sección', tipo: 'referencia', ref: 'secciones', requerido: true,
        // en vez de ofrecer todos los grupos, se piden solo los que este estudiante puede inscribir
        inscribiblesDe: 'idEstudiante' },
      { nombre: 'nota', etiqueta: 'Nota (0.0 a 5.0, opcional)', tipo: 'numero', min: 0, max: 5, step: 0.01 },
    ],
  },
  asistencias: {
    titulo: 'Asistencias',
    recurso: 'asistencias',
    idField: 'idAsistencia',
    // sin esta línea solo el administrativo guarda; pasar lista también le toca al profesor
    escritura: ['ADMINISTRATIVO', 'PROFESOR'],
    ayuda: 'Solo se pasa lista a estudiantes matriculados en la sección y con fechas dentro del periodo. Cuatro faltas sin justificar hacen perder el curso.',
    columnas: [
      { campo: 'idAsistencia', etiqueta: 'ID' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idSeccion', etiqueta: 'Sección', ref: 'secciones' },
      { campo: 'fecha', etiqueta: 'Fecha', formato: 'fecha' },
      { campo: 'estado', etiqueta: 'Estado' },
    ],
    filtros: [
      { nombre: 'seccion', etiqueta: 'Filtrar por sección', tipo: 'referencia', ref: 'secciones' },
      { nombre: 'fecha', etiqueta: 'Filtrar por fecha', tipo: 'fecha' },
    ],
    campos: [
      { nombre: 'idEstudiante', etiqueta: 'Estudiante', tipo: 'referencia', ref: 'estudiantes', requerido: true },
      { nombre: 'idSeccion', etiqueta: 'Sección', tipo: 'referencia', ref: 'secciones', requerido: true },
      { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'fecha', requerido: true },
      { nombre: 'estado', etiqueta: 'Estado', tipo: 'lista', opciones: ['ASISTIO', 'FALTO', 'TARDANZA', 'JUSTIFICADO'], requerido: true },
    ],
  },
  calificaciones: {
    titulo: 'Calificaciones',
    recurso: 'calificaciones',
    idField: 'idCalificacion',
    // el profesor califica sus propios grupos; la lista que ve cada rol la decide Java
    escritura: ['ADMINISTRATIVO', 'PROFESOR'],
    ayuda: 'El servidor calcula el promedio (30 % + 30 % + 40 %) y el estado: se aprueba con 3.0, salvo que las faltas no justificadas hagan perder el curso por asistencia.',
    columnas: [
      { campo: 'idCalificacion', etiqueta: 'ID' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idSeccion', etiqueta: 'Sección', ref: 'secciones' },
      { campo: 'notaParcial1', etiqueta: 'Parcial 1' },
      { campo: 'notaParcial2', etiqueta: 'Parcial 2' },
      { campo: 'notaExamenFinal', etiqueta: 'Examen final' },
      { campo: 'promedioFinal', etiqueta: 'Promedio' },
      { campo: 'estado', etiqueta: 'Estado', formato: 'academico' },
    ],
    filtros: [
      { nombre: 'seccion', etiqueta: 'Filtrar por sección', tipo: 'referencia', ref: 'secciones' },
    ],
    campos: [
      { nombre: 'idEstudiante', etiqueta: 'Estudiante', tipo: 'referencia', ref: 'estudiantes', requerido: true },
      { nombre: 'idSeccion', etiqueta: 'Sección', tipo: 'referencia', ref: 'secciones', requerido: true },
      { nombre: 'notaParcial1', etiqueta: 'Parcial 1 (opcional)', tipo: 'numero', min: 0, max: 5, step: 0.01 },
      { nombre: 'notaParcial2', etiqueta: 'Parcial 2 (opcional)', tipo: 'numero', min: 0, max: 5, step: 0.01 },
      { nombre: 'notaExamenFinal', etiqueta: 'Examen final (opcional)', tipo: 'numero', min: 0, max: 5, step: 0.01 },
    ],
  },
  usuarios: {
    titulo: 'Cuentas de acceso',
    recurso: 'usuarios',
    idField: 'idUsuario',
    ayuda: 'La contraseña se guarda con PBKDF2 y nunca se devuelve. En una edición, déjela vacía para no cambiarla.',
    columnas: [
      { campo: 'idUsuario', etiqueta: 'ID' },
      { campo: 'username', etiqueta: 'Usuario' },
      { campo: 'rol', etiqueta: 'Rol' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idProfesor', etiqueta: 'Profesor', ref: 'profesores' },
      { campo: 'estado', etiqueta: 'Activa', formato: 'estado' },
    ],
    campos: [
      { nombre: 'username', etiqueta: 'Usuario', tipo: 'texto', requerido: true },
      { nombre: 'password', etiqueta: 'Contraseña (10 caracteres o más)', tipo: 'clave' },
      { nombre: 'rol', etiqueta: 'Rol', tipo: 'lista', opciones: ['ADMINISTRATIVO', 'PROFESOR', 'ESTUDIANTE'], requerido: true },
      { nombre: 'idEstudiante', etiqueta: 'Ficha del estudiante (solo rol ESTUDIANTE)', tipo: 'referencia', ref: 'estudiantes' },
      { nombre: 'idProfesor', etiqueta: 'Ficha del profesor (solo rol PROFESOR)', tipo: 'referencia', ref: 'profesores' },
      { nombre: 'estado', etiqueta: 'Cuenta activa', tipo: 'logico' },
    ],
  },
};

// Últimas listas leídas (facultades, carreras, cursos…). Se guardan para no volver a pedirlas cada
// vez que la pantalla abre una lista desplegable.
const CATALOGOS = {};
// En qué lista buscar el número que trae una columna: idFacultad se resuelve leyendo facultades.
const REFERENCIA_POR_COLUMNA = {
  idFacultad: 'facultades', idCarrera: 'carreras', idProfesor: 'profesores',
  idEstudiante: 'estudiantes', idCurso: 'cursos', idPeriodo: 'periodos', idSeccion: 'secciones',
};

let rolActual = '';      // rol de la sesión conectada; lo devuelve /api/me al abrir la página
let edicion = null;      // registro que se está corrigiendo; null significa que el formulario crea uno nuevo

// Cada lista tiene su propio nombre de identificador; esto lo recuerda por si hace falta buscarlo.
function idDe(recurso) {
  return { facultades: 'idFacultad', carreras: 'idCarrera', profesores: 'idProfesor', estudiantes: 'idEstudiante',
    cursos: 'idCurso', periodos: 'idPeriodo', secciones: 'idSeccion' }[recurso];
}

// Texto que se ve en las opciones de las listas desplegables y en algunas columnas de la tabla.
function etiqueta(item, recurso) {
  if (recurso === 'profesores' || recurso === 'estudiantes') return `${item.nombre} ${item.apellido}`.trim();
  if (recurso === 'secciones') {
    const materia = item.nombreCurso
      ?? (CATALOGOS.cursos || []).find((c) => c.idCurso === item.idCurso)?.nombre;
    const partes = [materia, item.aula, item.nombrePeriodo].filter(Boolean);
    return `${partes.join(' · ')} (${item.cuposOcupados ?? 0}/${item.cupoMaximo})`;
  }
  return String(item.nombre ?? item.id ?? '');
}

// El navegador pide la lista al servidor con las cookies de la sesión y la guarda en CATALOGOS.
// Si ya estaba leída no se vuelve a pedir. Cuando Java contesta mal, por ejemplo 401 porque la
// sesión se cerró, se deja la lista vacía y se avisa arriba de la página en vez de romper la pantalla.
async function cargarCatalogo(recurso) {
  if (CATALOGOS[recurso]) return CATALOGOS[recurso];
  try {
    const response = await fetch(`${API_BASE_URL}/api/${recurso}`, { credentials: 'include' });
    CATALOGOS[recurso] = response.ok ? await response.json() : [];
    if (!response.ok) showMessage(`No se pudo cargar "${recurso}" (${response.status}).`, true);
  } catch (error) {
    CATALOGOS[recurso] = [];
    showMessage(connectionErrorMessage(error), true);
  }
  return CATALOGOS[recurso];
}

// Reúne las listas que necesitan los desplegables y los filtros de esta pantalla y las pide todas
// a la vez, para no abrir la página con casillas vacías mientras llegan.
async function cargarCatalogosNecesarios(config) {
  const necesarios = new Set();
  for (const lista of [config.campos, config.filtros, config.columnas]) {
    for (const item of lista || []) {
      const recurso = item.ref || (item.campo ? REFERENCIA_POR_COLUMNA[item.campo] : null);
      if (recurso && CATALOGOS[recurso] === undefined) necesarios.add(recurso);
    }
  }
  await Promise.all([...necesarios].map(cargarCatalogo));
}

/**
 * Escribe las opciones en una lista desplegable. Si el usuario ya había elegido una opción que
 * sigue existiendo, se mantiene elegida; si no, se vuelve al "Seleccione…" de la primera línea.
 */
function llenarReferencia(control, items, campo, textoSiVacio) {
  const anterior = control.value;
  control.textContent = '';
  const vacio = document.createElement('option');
  vacio.value = '';
  if (items.length === 0 && textoSiVacio) {
    vacio.disabled = true;
    vacio.textContent = textoSiVacio;
  } else {
    vacio.textContent = campo.requerido ? 'Seleccione…' : '— sin valor —';
  }
  control.appendChild(vacio);
  for (const item of items) {
    const opcion = document.createElement('option');
    opcion.value = item[idDe(campo.ref)];
    opcion.textContent = etiqueta(item, campo.ref);
    control.appendChild(opcion);
  }
  if ([...control.options].some((o) => o.value === anterior && !o.disabled)) control.value = anterior;
}

// Arma en la pantalla el tipo de caja que toca: lista desplegable, casilla de sí/no o campo de
// texto, número, fecha o contraseña. El prefijo distingue los campos del formulario de los filtros.
function crearControl(campo, prefijo) {
  const label = document.createElement('label');
  const texto = document.createElement('span');
  texto.textContent = campo.etiqueta;
  label.appendChild(texto);

  let control;
  if (campo.tipo === 'referencia' || campo.tipo === 'lista') {
    control = document.createElement('select');
    if (campo.tipo === 'referencia') {
      llenarReferencia(control, CATALOGOS[campo.ref] || [], campo);
    } else {
      const vacio = document.createElement('option');
      vacio.value = '';
      vacio.textContent = campo.requerido ? 'Seleccione…' : '— sin valor —';
      control.appendChild(vacio);
      for (const valor of campo.opciones) {
        const opcion = document.createElement('option');
        opcion.value = valor;
        opcion.textContent = valor;
        control.appendChild(opcion);
      }
    }
  } else if (campo.tipo === 'logico') {
    control = document.createElement('input');
    control.type = 'checkbox';
  } else {
    control = document.createElement('input');
    control.type = campo.tipo === 'numero' ? 'number'
      : campo.tipo === 'fecha' ? 'date'
      : campo.tipo === 'clave' ? 'password' : 'text';
    if (campo.min !== undefined) control.min = campo.min;
    if (campo.max !== undefined) control.max = campo.max;
    if (campo.step !== undefined) control.step = campo.step;
    if (campo.requerido) control.required = true;
    if (campo.tipo === 'texto') control.maxLength = 100;
  }
  control.name = campo.nombre;
  control.id = `${prefijo}-${campo.nombre}`;
  label.appendChild(control);
  return label;
}

// Saca lo que el usuario escribió para armar el JSON que se envía. La casilla manda 1 o 0, los
// números y las referencias se pasan a número, y un campo vacío manda null para que Java lo deje vacío.
function leerValor(campo) {
  const control = document.getElementById(`campo-${campo.nombre}`);
  if (!control) return undefined;
  if (campo.tipo === 'logico') return control.checked ? 1 : 0;
  const texto = control.value.trim();
  if (texto === '') return null;
  return campo.tipo === 'numero' || campo.tipo === 'referencia' ? Number(texto) : texto;
}

// Deja el formulario vacío, o le escribe los valores del registro que se va a corregir. La contraseña
// nunca se rellena: si se deja vacía, Java conserva la que ya tenía. Al crear, la casilla arranca en sí.
function escribirValor(campo, valor) {
  const control = document.getElementById(`campo-${campo.nombre}`);
  if (!control) return;
  if (campo.tipo === 'logico') {
    control.checked = edicion ? Boolean(valor) : true;
    return;
  }
  if (campo.tipo === 'clave') {
    control.value = '';
    return;
  }
  if (campo.tipo === 'fecha' && valor) {
    control.value = String(valor).slice(0, 10);
    return;
  }
  control.value = valor === null || valor === undefined ? '' : valor;
}

/*
 * En matrículas, la lista de grupos no se muestra a ciegas: el navegador pregunta a Java qué grupos
 * puede inscribir el estudiante elegido (los de su carrera, con periodo abierto, con cupo libre y
 * con los créditos que todavía le caben en el periodo) y ofrece solo eso. Así el formulario no
 * admite una matrícula que el servidor tendría que rechazar al guardar.
 */
async function actualizarInscribibles(campo) {
  const destino = document.getElementById(`campo-${campo.nombre}`);
  const driver = document.getElementById(`campo-${campo.inscribiblesDe}`);
  if (!destino || !driver) return;
  const id = Number(driver.value);
  // Sin estudiante elegido no hay nada que preguntar: la lista queda vacía con el aviso.
  if (!Number.isInteger(id) || id <= 0) {
    llenarReferencia(destino, [], campo, 'Seleccione primero el estudiante.');
    return;
  }
  // La consulta se envía con las cookies de la sesión; solo el administrativo tiene permiso.
  const respuesta = await fetch(`${API_BASE_URL}/api/estudiantes/${id}/secciones-inscribibles`,
    { credentials: 'include' });
  if (!respuesta.ok) {
    showMessage(await responseError(respuesta), true);
    return;
  }
  llenarReferencia(destino, await respuesta.json(), campo, 'No hay grupos disponibles para este estudiante.');
}

// Enlaza el campo que manda (el estudiante) con el que obedece (el grupo): al cambiar el primero se
// vuelve a preguntar la lista. La marca en el propio campo evita enlazar el mismo cambio dos veces.
function enlazarInscribibles(config) {
  for (const campo of config.campos) {
    if (!campo.inscribiblesDe) continue;
    const driver = document.getElementById(`campo-${campo.inscribiblesDe}`);
    if (driver && !driver.dataset.inscribiblesEnlazado) {
      driver.dataset.inscribiblesEnlazado = 'si';
      driver.addEventListener('change', () => actualizarInscribibles(campo));
    }
    actualizarInscribibles(campo);
  }
}

/*
 * Al editar una matrícula, Java no trae el grupo actual entre los grupos inscribibles: el estudiante
 * ya está matriculado allí. Para poder guardar sin cambiar de grupo, aquí se carga la lista completa.
 */
function mostrarCatalogoCompleto(config) {
  for (const campo of config.campos) {
    if (!campo.inscribiblesDe) continue;
    const control = document.getElementById(`campo-${campo.nombre}`);
    if (control) llenarReferencia(control, CATALOGOS[campo.ref] || [], campo);
  }
}

// Junta los valores del formulario en el objeto que se convierte a JSON. La contraseña vacía no se
// manda, para no borrar la que ya está guardada en la base.
function cuerpoDelFormulario(config) {  const cuerpo = {};
  for (const campo of config.campos) {
    const valor = leerValor(campo);
    if (valor === null && campo.tipo === 'clave') continue;
    cuerpo[campo.nombre] = valor;
  }
  return cuerpo;
}

// Convierte un dato en el texto que se pinta en la celda: fechas legibles, sí/no, cupos usados
// sobre el total, y los números de otra tabla sustituidos por su nombre buscándolo en la lista leída.
function textoCelula(columna, item) {
  const valor = item[columna.campo];
  if (columna.formato === 'fecha') return formatDateForDisplay(valor);
  if (columna.formato === 'estado') return valor ? 'Sí' : 'No';
  if (columna.formato === 'cupos') return `${valor ?? 0} / ${item.cupoMaximo}`;
  if (columna.ref) {
    const catalogo = CATALOGOS[columna.ref] || [];
    const encontrado = catalogo.find((x) => x[idDe(columna.ref)] === valor);
    if (encontrado) return etiqueta(encontrado, columna.ref);
    return valor === null || valor === undefined ? '' : `#${valor}`;
  }
  return valor === null || valor === undefined ? '' : String(valor);
}

/*
 * El estado académico (en curso, aprobado, reprobado o perdido por asistencia) lo calcula Java, nunca
 * el formulario. Aquí solo se cambia ese nombre por la clase de estilo con la que la hoja lo pinta.
 */
const ESTADOS_ACADEMICOS = {
  EN_CURSO: 'curso',
  APROBADO: 'aprobado',
  REPROBADO: 'reprobado',
  PERDIDO_POR_FALTA_DE_ASISTENCIA: 'perdido',
};

// Roles que pueden guardar en este módulo. Ocultar el formulario es comodidad, no seguridad: Java
// comprueba la sesión en cada petición y rechaza igual al rol sin permiso.
function puedeEscribir(config) {
  return (config.escritura || ['ADMINISTRATIVO']).includes(rolActual);
}

/*
 * Vuelve a armar la tabla: borra encabezado y filas, pide la lista al servidor con las cookies de la
 * sesión y escribe una fila por registro. Los filtros de la pantalla viajan en la misma dirección
 * (por ejemplo /api/secciones?periodo=3), así filtra Java y no se traen datos de más.
 * Se llama otra vez después de guardar o de borrar, para que en pantalla quede lo que de verdad
 * quedó en la base de datos, con el ID y el estado que calculó el servidor.
 */
async function refrescarTabla(config) {
  const tbody = document.getElementById('tabla');
  const cabecera = document.getElementById('encabezados');
  cabecera.textContent = '';
  tbody.textContent = '';

  for (const columna of config.columnas) {
    const th = document.createElement('th');
    th.textContent = columna.etiqueta;
    cabecera.appendChild(th);
  }
  const thAccion = document.createElement('th');
  thAccion.textContent = 'Acciones';
  cabecera.appendChild(thAccion);

  // Los filtros escritos en la pantalla se pasan a Java como texto detrás del signo de pregunta.
  const consulta = new URLSearchParams();
  for (const filtro of config.filtros || []) {
    const control = document.getElementById(`filtro-${filtro.nombre}`);
    if (control && control.value.trim() !== '') consulta.set(filtro.nombre, control.value.trim());
  }
  const sufijo = consulta.toString() ? `?${consulta}` : '';
  const response = await fetch(`${API_BASE_URL}/api/${config.recurso}${sufijo}`, { credentials: 'include' });
  if (!response.ok) {
    showMessage(await responseError(response), true);
    return;
  }
  const datos = await response.json();
  // El aviso de "no hay registros" aparece solo cuando la lista viene vacía.
  document.getElementById('vacio').hidden = datos.length > 0;
  document.getElementById('contador').textContent = `${datos.length} registro(s)`;

  for (const item of datos) {
    const tr = document.createElement('tr');
    for (const columna of config.columnas) {
      const td = document.createElement('td');
      td.textContent = textoCelula(columna, item);
      if (columna.formato === 'academico') {
        td.className = `estado ${ESTADOS_ACADEMICOS[item[columna.campo]] || 'desconocido'}`;
      }
      tr.appendChild(td);
    }
    const td = document.createElement('td');
    td.className = 'actions';
    const editar = document.createElement('button');
    editar.type = 'button';
    editar.textContent = 'Editar';
    editar.addEventListener('click', () => {
      // Editar no guarda nada: copia el registro al formulario, que pasa a modo edición, y baja la vista hasta él.
      edicion = item;
      mostrarCatalogoCompleto(config);
      for (const campo of config.campos) escribirValor(campo, item[campo.nombre]);
      document.getElementById('guardar').textContent = 'Guardar cambios';
      document.getElementById('cancelar').hidden = false;
      document.getElementById('formulario').scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
    td.appendChild(editar);
    if (puedeEscribir(config)) {
      const borrar = document.createElement('button');
      borrar.type = 'button';
      borrar.className = 'delete';
      borrar.textContent = 'Eliminar';
      borrar.addEventListener('click', async () => {
        // Se pide confirmación antes de borrar. Si el registro tiene datos dependientes, Java lo impide y cuenta por qué.
        if (!window.confirm(`¿Eliminar el registro #${item[config.idField]} de ${config.titulo}?`)) return;
        const respuesta = await fetch(`${API_BASE_URL}/api/${config.recurso}/${item[config.idField]}`, {
          method: 'DELETE', credentials: 'include',
        });
        await notificar(respuesta, 'Eliminar');
        await refrescarTabla(config);
      });
      td.appendChild(borrar);
    }
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
}

// Deja el resultado arriba de la página: en verde si Java contestó bien, en rojo con el mensaje que
// escribió el servidor para explicar el problema.
async function notificar(response, accion) {
  if (response.ok) {
    showMessage(`${accion} correcto.`);
    return true;
  }
  showMessage(`${accion}: ${await responseError(response)}`, true);
  return false;
}

// Un solo botón crea y cambia: sin registro elegido se envía con POST a la dirección del módulo; con
// uno elegido se envía con PUT a la dirección de ese registro. El formulario se convierte a JSON y se
// manda con las cookies de la sesión, para que Java sepa quién escribe.
async function guardar(evento, config) {
  evento.preventDefault();
  const esEdicion = Boolean(edicion);
  const url = esEdicion
    ? `${API_BASE_URL}/api/${config.recurso}/${edicion[config.idField]}`
    : `${API_BASE_URL}/api/${config.recurso}`;
  const response = await fetch(url, {
    method: esEdicion ? 'PUT' : 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cuerpoDelFormulario(config)),
  });
  if (!await notificar(response, esEdicion ? 'Actualizar' : 'Registrar')) return;
  salirDeEdicion(config);
  await refrescarTabla(config);
}

// El formulario vuelve a modo "crear": se olvida del registro y se queda vacío.
function salirDeEdicion(config) {
  edicion = null;
  document.getElementById('guardar').textContent = 'Registrar';
  document.getElementById('cancelar').hidden = true;
  for (const campo of config.campos) escribirValor(campo, null);
  enlazarInscribibles(config);
}

/*
 * Prepara la pantalla. Lo primero es saber quién está conectado: se pide /api/me con las cookies de
 * la sesión y de ahí sale el rol. Si la sesión ya se cerró, Java contesta 401 (no autorizado), así
 * que se regresa al login en lugar de mostrar una página vacía o a medias.
 */
async function montarModulo(clave) {
  const config = MODULOS[clave];
  if (!config) return;

  const me = await fetch(`${API_BASE_URL}/api/me`, { credentials: 'include' });
  if (!me.ok) {
    window.location.href = 'login.html';
    return;
  }
  rolActual = (await me.json()).rol;

  document.getElementById('titulo').textContent = config.titulo;
  document.getElementById('ayuda').textContent = config.ayuda || '';

  await cargarCatalogosNecesarios(config);

  const filtros = document.getElementById('filtros');
  // La fila de filtros solo aparece en los módulos que la definen.
  if (config.filtros && config.filtros.length > 0) {
    filtros.hidden = false;
    for (const filtro of config.filtros) filtros.appendChild(crearControl(filtro, 'filtro'));
  }
  if (config.filtros && config.filtros.length > 0) {
    const aplicar = document.createElement('button');
    aplicar.type = 'button';
    aplicar.className = 'secondary';
    aplicar.textContent = 'Aplicar filtros';
    aplicar.addEventListener('click', () => refrescarTabla(config));
    filtros.appendChild(aplicar);
    if (puedeEscribir(config)) {
      const limpiar = document.createElement('button');
      limpiar.type = 'button';
      limpiar.className = 'secondary';
      limpiar.textContent = 'Ver todo';
      limpiar.addEventListener('click', async () => {
        for (const filtro of config.filtros) document.getElementById(`filtro-${filtro.nombre}`).value = '';
        await refrescarTabla(config);
      });
      filtros.appendChild(limpiar);
    }
  }

  const campos = document.getElementById('campos');
  for (const campo of config.campos) campos.appendChild(crearControl(campo, 'campo'));
  document.getElementById('formulario').addEventListener('submit', (evento) => guardar(evento, config));
  document.getElementById('cancelar').addEventListener('click', () => salirDeEdicion(config));
  if (!puedeEscribir(config)) {
    // Rol de solo lectura: se oculta el formulario para no ofrecer algo que Java rechazaría igual.
    document.getElementById('formulario').hidden = true;
    showMessage(`Su rol (${rolActual}) es de solo lectura en este módulo.`, false);
  } else {
    for (const campo of config.campos) escribirValor(campo, null);
    enlazarInscribibles(config);
  }

  await refrescarTabla(config);
}

// La clave data-modulo del <body> dice qué pantalla montar. Las páginas que no la llevan (el panel
// o el login) ni siquiera cargan este archivo, y aquí tampoco se arrancaría nada.
const MODULO_PAGINA = document.body.dataset.modulo;
if (MODULO_PAGINA) montarModulo(MODULO_PAGINA);
