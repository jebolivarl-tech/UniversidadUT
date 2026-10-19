/* Motor genérico de las pantallas CRUD. Cada página declara data-modulo="<clave>". */
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
    columnas: [
      { campo: 'idSeccion', etiqueta: 'ID' },
      { campo: 'idCurso', etiqueta: 'Curso', ref: 'cursos' },
      { campo: 'idProfesor', etiqueta: 'Profesor', ref: 'profesores' },
      { campo: 'idPeriodo', etiqueta: 'Periodo', ref: 'periodos' },
      { campo: 'aula', etiqueta: 'Aula' },
      { campo: 'cupoMaximo', etiqueta: 'Cupo' },
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
    columnas: [
      { campo: 'idMatricula', etiqueta: 'ID' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idCurso', etiqueta: 'Curso', ref: 'cursos' },
      { campo: 'periodo', etiqueta: 'Periodo' },
      { campo: 'nota', etiqueta: 'Nota' },
    ],
    filtros: [
      { nombre: 'curso', etiqueta: 'Filtrar por curso', tipo: 'referencia', ref: 'cursos' },
      { nombre: 'estudiante', etiqueta: 'Filtrar por estudiante', tipo: 'referencia', ref: 'estudiantes' },
    ],
    campos: [
      { nombre: 'idEstudiante', etiqueta: 'Estudiante', tipo: 'referencia', ref: 'estudiantes', requerido: true },
      { nombre: 'idCurso', etiqueta: 'Curso', tipo: 'referencia', ref: 'cursos', requerido: true },
      { nombre: 'periodo', etiqueta: 'Periodo (texto)', tipo: 'texto', requerido: true },
      { nombre: 'nota', etiqueta: 'Nota (0.0 a 5.0, opcional)', tipo: 'numero', min: 0, max: 5, step: 0.01 },
    ],
  },
  asistencias: {
    titulo: 'Asistencias',
    recurso: 'asistencias',
    idField: 'idAsistencia',
    escritura: ['ADMINISTRATIVO', 'PROFESOR'],
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
    escritura: ['ADMINISTRATIVO', 'PROFESOR'],
    ayuda: 'El promedio y el estado los calcula el servidor: 30% + 30% + 40%, se aprueba con 3.0.',
    columnas: [
      { campo: 'idCalificacion', etiqueta: 'ID' },
      { campo: 'idEstudiante', etiqueta: 'Estudiante', ref: 'estudiantes' },
      { campo: 'idSeccion', etiqueta: 'Sección', ref: 'secciones' },
      { campo: 'notaParcial1', etiqueta: 'Parcial 1' },
      { campo: 'notaParcial2', etiqueta: 'Parcial 2' },
      { campo: 'notaExamenFinal', etiqueta: 'Examen final' },
      { campo: 'promedioFinal', etiqueta: 'Promedio' },
      { campo: 'estado', etiqueta: 'Estado' },
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

const CATALOGOS = {};
const REFERENCIA_POR_COLUMNA = {
  idFacultad: 'facultades', idCarrera: 'carreras', idProfesor: 'profesores',
  idEstudiante: 'estudiantes', idCurso: 'cursos', idPeriodo: 'periodos', idSeccion: 'secciones',
};

let rolActual = '';
let edicion = null;

function idDe(recurso) {
  return { facultades: 'idFacultad', carreras: 'idCarrera', profesores: 'idProfesor', estudiantes: 'idEstudiante',
    cursos: 'idCurso', periodos: 'idPeriodo', secciones: 'idSeccion' }[recurso];
}

function etiqueta(item, recurso) {
  if (recurso === 'profesores' || recurso === 'estudiantes') return `${item.nombre} ${item.apellido}`.trim();
  if (recurso === 'secciones') {
    const curso = (CATALOGOS.cursos || []).find((c) => c.idCurso === item.idCurso);
    return `${item.aula}${curso ? ` · ${curso.nombre}` : ''}`;
  }
  return String(item.nombre ?? item.id ?? '');
}

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

function crearControl(campo, prefijo) {
  const label = document.createElement('label');
  const texto = document.createElement('span');
  texto.textContent = campo.etiqueta;
  label.appendChild(texto);

  let control;
  if (campo.tipo === 'referencia' || campo.tipo === 'lista') {
    control = document.createElement('select');
    const vacio = document.createElement('option');
    vacio.value = '';
    vacio.textContent = campo.requerido ? 'Seleccione…' : '— sin valor —';
    control.appendChild(vacio);
    if (campo.tipo === 'referencia') {
      for (const item of CATALOGOS[campo.ref] || []) {
        const opcion = document.createElement('option');
        opcion.value = item[idDe(campo.ref)];
        opcion.textContent = etiqueta(item, campo.ref);
        control.appendChild(opcion);
      }
    } else {
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

function leerValor(campo) {
  const control = document.getElementById(`campo-${campo.nombre}`);
  if (!control) return undefined;
  if (campo.tipo === 'logico') return control.checked ? 1 : 0;
  const texto = control.value.trim();
  if (texto === '') return null;
  return campo.tipo === 'numero' || campo.tipo === 'referencia' ? Number(texto) : texto;
}

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

function cuerpoDelFormulario(config) {
  const cuerpo = {};
  for (const campo of config.campos) {
    const valor = leerValor(campo);
    if (valor === null && campo.tipo === 'clave') continue;
    cuerpo[campo.nombre] = valor;
  }
  return cuerpo;
}

function textoCelula(columna, item) {
  const valor = item[columna.campo];
  if (columna.formato === 'fecha') return formatDateForDisplay(valor);
  if (columna.formato === 'estado') return valor ? 'Sí' : 'No';
  if (columna.ref) {
    const catalogo = CATALOGOS[columna.ref] || [];
    const encontrado = catalogo.find((x) => x[idDe(columna.ref)] === valor);
    if (encontrado) return etiqueta(encontrado, columna.ref);
    return valor === null || valor === undefined ? '' : `#${valor}`;
  }
  return valor === null || valor === undefined ? '' : String(valor);
}

function puedeEscribir(config) {
  return (config.escritura || ['ADMINISTRATIVO']).includes(rolActual);
}

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
  document.getElementById('vacio').hidden = datos.length > 0;
  document.getElementById('contador').textContent = `${datos.length} registro(s)`;

  for (const item of datos) {
    const tr = document.createElement('tr');
    for (const columna of config.columnas) {
      const td = document.createElement('td');
      td.textContent = textoCelula(columna, item);
      tr.appendChild(td);
    }
    const td = document.createElement('td');
    td.className = 'actions';
    const editar = document.createElement('button');
    editar.type = 'button';
    editar.textContent = 'Editar';
    editar.addEventListener('click', () => {
      edicion = item;
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

async function notificar(response, accion) {
  if (response.ok) {
    showMessage(`${accion} correcto.`);
    return true;
  }
  showMessage(`${accion}: ${await responseError(response)}`, true);
  return false;
}

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

function salirDeEdicion(config) {
  edicion = null;
  document.getElementById('guardar').textContent = 'Registrar';
  document.getElementById('cancelar').hidden = true;
  for (const campo of config.campos) escribirValor(campo, null);
}

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
    document.getElementById('formulario').hidden = true;
    showMessage(`Su rol (${rolActual}) es de solo lectura en este módulo.`, false);
  } else {
    for (const campo of config.campos) escribirValor(campo, null);
  }

  await refrescarTabla(config);
}

const MODULO_PAGINA = document.body.dataset.modulo;
if (MODULO_PAGINA) montarModulo(MODULO_PAGINA);
