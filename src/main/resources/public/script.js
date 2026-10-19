const API_BASE_URL = window.location.protocol === 'file:'
  ? 'http://localhost:8080'
  : window.location.origin;
const message = document.getElementById('message');

function showMessage(text, isError = false) {
  if (!message) return;
  message.textContent = text;
  message.style.color = isError ? '#dc2626' : '#15803d';
}

async function responseError(response) {
  const body = await response.text();
  if (!body) return `Error HTTP ${response.status}.`;

  try {
    const data = JSON.parse(body);
    return data.error || data.message || body;
  } catch {
    return body;
  }
}

function connectionErrorMessage(error) {
  if (error instanceof TypeError) {
    return 'No se pudo conectar con Java. Comprueba que el servidor esté activo en localhost:8080 y que CORS esté permitido.';
  }
  return error.message || 'Ocurrió un error inesperado.';
}

function formatDateForDisplay(value) {
  if (value === null || value === undefined || value === '') return '';

  let date;

  if (typeof value === 'number') {
    date = new Date(value);
  } else {
    const input = String(value).trim();
    if (!input) return '';

    const normalized = input.includes('T') || input.includes(' ') || input.includes('Z')
      ? input
      : `${input}T12:00:00`;

    date = new Date(normalized);
  }

  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(date);
}

async function loginUser(event) {
  if (!event) return;
  event.preventDefault();

  const username = document.getElementById('username')?.value?.trim();
  const password = document.getElementById('password')?.value || '';
  const loginButton = document.querySelector('#loginForm button[type="submit"]');

  if (!username || !password) {
    showMessage('Ingrese usuario y contraseña.', true);
    return;
  }

  if (loginButton) loginButton.disabled = true;

  try {
    const response = await fetch(`${API_BASE_URL}/api/login`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
      showMessage(await responseError(response), true);
      return;
    }

    showMessage('Inicio de sesión correcto.');
    window.location.href = 'menu.html';
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
    console.error(error);
  } finally {
    if (loginButton) loginButton.disabled = false;
  }
}

async function comprobarEstadoServidor() {
  if (!document.getElementById('loginForm')) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/health`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    const status = await response.json();

    if (!response.ok) {
      showMessage(status.error || 'MySQL no está disponible.', true);
      return;
    }

    showMessage('Servidor Java y MySQL conectados.');
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

let estudiantesActuales = [];
let carrerasEstudiantes = [];
let estudianteSeleccionado = null;

function fechaISO(value) {
  if (value === null || value === undefined || value === '') return '';
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toISOString().slice(0, 10);
}

async function cargarCarrerasParaEstudiantes() {
  const formSelect = document.getElementById('idCarrera');
  const filterSelect = document.getElementById('filtroCarrera');
  if (!formSelect || !filterSelect) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/carreras`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }

    carrerasEstudiantes = await response.json();
    formSelect.replaceChildren(new Option('Seleccione una carrera', ''));
    filterSelect.replaceChildren(new Option('Todas las carreras', ''));
    carrerasEstudiantes.forEach((carrera) => {
      formSelect.add(new Option(carrera.nombre, String(carrera.idCarrera)));
      filterSelect.add(new Option(carrera.nombre, String(carrera.idCarrera)));
    });
    document.getElementById('kpiCarreras').textContent = carrerasEstudiantes.length;
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

async function cargarEstudiantes() {
  const tbody = document.getElementById('tablaEstudiantes');
  if (!tbody) return false;
  showMessage('');

  try {
    const response = await fetch(`${API_BASE_URL}/api/estudiantes`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }
    estudiantesActuales = await response.json();
    aplicarFiltrosEstudiantes();
    return true;
  } catch (error) {
    console.error(error);
    showMessage(connectionErrorMessage(error), true);
    return false;
  }
}

function aplicarFiltrosEstudiantes() {
  const query = document.getElementById('buscarEstudiante').value.trim().toLocaleLowerCase('es');
  const field = document.getElementById('buscarEn').value;
  const careerId = document.getElementById('filtroCarrera').value;
  const fromDate = document.getElementById('fechaDesde').value;
  const toDate = document.getElementById('fechaHasta').value;
  const fields = {
    id: ['idEstudiante'],
    nombre: ['nombre'],
    apellido: ['apellido'],
    email: ['email']
  };

  const visibles = estudiantesActuales.filter((student) => {
    if (query) {
      const names = field === 'todos' ? ['idEstudiante', 'nombre', 'apellido', 'email'] : fields[field];
      const matches = names.some((name) => String(student[name] ?? '').toLocaleLowerCase('es').includes(query));
      if (!matches) return false;
    }
    if (careerId && String(student.idCarrera) !== careerId) return false;

    const birthDate = fechaISO(student.fechaNacimiento);
    if (fromDate && (!birthDate || birthDate < fromDate)) return false;
    if (toDate && (!birthDate || birthDate > toDate)) return false;
    return true;
  });

  renderEstudiantes(visibles);
  actualizarIndicadoresEstudiantes(visibles);
}

function renderEstudiantes(students) {
  const tbody = document.getElementById('tablaEstudiantes');
  const empty = document.getElementById('estudiantesVacio');
  tbody.replaceChildren();
  empty.hidden = students.length > 0;

  students.forEach((student) => {
    const row = document.createElement('tr');
    row.tabIndex = 0;
    row.setAttribute('aria-selected', String(student.idEstudiante === estudianteSeleccionado));
    if (student.idEstudiante === estudianteSeleccionado) row.classList.add('selected');
    const career = carrerasEstudiantes.find((item) => item.idCarrera === student.idCarrera);
    const values = [
      student.idEstudiante,
      student.nombre,
      student.apellido,
      student.email,
      formatDateForDisplay(student.fechaNacimiento),
      career?.nombre || `Carrera ${student.idCarrera}`
    ];

    values.forEach((value) => {
      const cell = document.createElement('td');
      cell.textContent = value === null || value === undefined ? '' : String(value);
      row.appendChild(cell);
    });
    row.addEventListener('click', () => seleccionarEstudiante(student));
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        seleccionarEstudiante(student);
      }
    });
    tbody.appendChild(row);
  });
}

function actualizarIndicadoresEstudiantes(visibles) {
  document.getElementById('kpiTotal').textContent = estudiantesActuales.length;
  document.getElementById('kpiCarreras').textContent = carrerasEstudiantes.length;
  document.getElementById('kpiVisibles').textContent = visibles.length;
  document.getElementById('contadorEstudiantes').textContent =
    `Mostrando ${visibles.length} de ${estudiantesActuales.length} estudiantes`;

  const today = new Date();
  const ages = estudiantesActuales
    .map((student) => fechaISO(student.fechaNacimiento))
    .filter(Boolean)
    .map((birthDate) => {
      const [year, month, day] = birthDate.split('-').map(Number);
      let age = today.getFullYear() - year;
      if (today.getMonth() + 1 < month || (today.getMonth() + 1 === month && today.getDate() < day)) age--;
      return age;
    })
    .filter((age) => age >= 0 && age < 130);
  const average = ages.length ? ages.reduce((sum, age) => sum + age, 0) / ages.length : null;
  document.getElementById('kpiEdad').textContent = average === null ? '-' : `${average.toFixed(1)} a\u00F1os`;
}

function seleccionarEstudiante(student) {
  estudianteSeleccionado = student.idEstudiante;
  document.getElementById('estudianteId').value = student.idEstudiante;
  document.getElementById('nombre').value = student.nombre ?? '';
  document.getElementById('apellido').value = student.apellido ?? '';
  document.getElementById('email').value = student.email ?? '';
  document.getElementById('fechaNacimiento').value = fechaISO(student.fechaNacimiento);
  document.getElementById('idCarrera').value = String(student.idCarrera);
  document.getElementById('actualizarEstudiante').disabled = false;
  document.getElementById('eliminarEstudiante').disabled = false;
  renderEstudiantes(estudiantesFiltradosActuales());
}

function estudiantesFiltradosActuales() {
  const query = document.getElementById('buscarEstudiante').value.trim().toLocaleLowerCase('es');
  const field = document.getElementById('buscarEn').value;
  const careerId = document.getElementById('filtroCarrera').value;
  const fromDate = document.getElementById('fechaDesde').value;
  const toDate = document.getElementById('fechaHasta').value;
  const fields = { id: ['idEstudiante'], nombre: ['nombre'], apellido: ['apellido'], email: ['email'] };

  return estudiantesActuales.filter((student) => {
    const names = field === 'todos' ? ['idEstudiante', 'nombre', 'apellido', 'email'] : fields[field];
    if (query && !names.some((name) => String(student[name] ?? '').toLocaleLowerCase('es').includes(query))) return false;
    if (careerId && String(student.idCarrera) !== careerId) return false;
    const birthDate = fechaISO(student.fechaNacimiento);
    if (fromDate && (!birthDate || birthDate < fromDate)) return false;
    if (toDate && (!birthDate || birthDate > toDate)) return false;
    return true;
  });
}

function limpiarFormularioEstudiante() {
  estudianteSeleccionado = null;
  document.getElementById('estudianteForm').reset();
  document.getElementById('estudianteId').value = '';
  document.getElementById('actualizarEstudiante').disabled = true;
  document.getElementById('eliminarEstudiante').disabled = true;
  renderEstudiantes(estudiantesFiltradosActuales());
}

function obtenerDatosFormularioEstudiante() {
  return {
    nombre: document.getElementById('nombre').value.trim(),
    apellido: document.getElementById('apellido').value.trim(),
    email: document.getElementById('email').value.trim(),
    fechaNacimiento: document.getElementById('fechaNacimiento').value,
    idCarrera: Number(document.getElementById('idCarrera').value)
  };
}

async function guardarEstudiante(event, actualizar = false) {
  event?.preventDefault();
  const form = document.getElementById('estudianteForm');
  if (!form.reportValidity()) return;
  if (actualizar && estudianteSeleccionado === null) {
    showMessage('Seleccione un estudiante de la tabla para actualizar.', true);
    return;
  }

  const estudiante = obtenerDatosFormularioEstudiante();
  if (!Number.isInteger(estudiante.idCarrera) || estudiante.idCarrera < 1) {
    showMessage('Seleccione una carrera válida.', true);
    return;
  }

  const button = document.getElementById(actualizar ? 'actualizarEstudiante' : 'guardarEstudiante');
  button.disabled = true;
  try {
    const response = await fetch(actualizar
      ? `${API_BASE_URL}/api/estudiantes/${estudianteSeleccionado}`
      : `${API_BASE_URL}/api/estudiantes`, {
      method: actualizar ? 'PUT' : 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(estudiante)
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }

    const saved = await response.json();
    limpiarFormularioEstudiante();
    await cargarEstudiantes();
    showMessage(actualizar
      ? `Estudiante ${saved.idEstudiante} actualizado correctamente.`
      : `Estudiante ${saved.idEstudiante} registrado correctamente.`);
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  } finally {
    button.disabled = actualizar ? estudianteSeleccionado === null : false;
  }
}

async function eliminarEstudiante() {
  if (estudianteSeleccionado === null) {
    showMessage('Seleccione un estudiante de la tabla para eliminar.', true);
    return;
  }
  if (!window.confirm('¿Está seguro de eliminar al estudiante seleccionado?')) return;

  const id = estudianteSeleccionado;
  const button = document.getElementById('eliminarEstudiante');
  button.disabled = true;
  try {
    const response = await fetch(`${API_BASE_URL}/api/estudiantes/${id}`, {
      method: 'DELETE',
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }
    limpiarFormularioEstudiante();
    await cargarEstudiantes();
    showMessage(`Estudiante ${id} eliminado.`);
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  } finally {
    button.disabled = estudianteSeleccionado === null;
  }
}

function iniciarModuloEstudiantes() {
  const currentDate = document.getElementById('fechaActual');
  if (currentDate) {
    currentDate.textContent = new Intl.DateTimeFormat('es-CO', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(new Date());
  }

  const applyFilters = () => aplicarFiltrosEstudiantes();
  document.getElementById('btnBuscar')?.addEventListener('click', applyFilters);
  document.getElementById('buscarEstudiante')?.addEventListener('input', applyFilters);
  document.getElementById('buscarEn')?.addEventListener('change', applyFilters);
  document.getElementById('filtroCarrera')?.addEventListener('change', applyFilters);
  document.getElementById('fechaDesde')?.addEventListener('change', applyFilters);
  document.getElementById('fechaHasta')?.addEventListener('change', applyFilters);
  document.getElementById('btnRestablecer')?.addEventListener('click', () => {
    document.getElementById('buscarEstudiante').value = '';
    document.getElementById('buscarEn').value = 'todos';
    document.getElementById('filtroCarrera').value = '';
    document.getElementById('fechaDesde').value = '';
    document.getElementById('fechaHasta').value = '';
    aplicarFiltrosEstudiantes();
  });
  document.getElementById('actualizarEstudiante')?.addEventListener('click', (event) => guardarEstudiante(event, true));
  document.getElementById('eliminarEstudiante')?.addEventListener('click', eliminarEstudiante);
  document.getElementById('limpiarEstudiante')?.addEventListener('click', limpiarFormularioEstudiante);
  document.getElementById('estudianteForm')?.addEventListener('submit', (event) => guardarEstudiante(event));
  cargarCarrerasParaEstudiantes().then(cargarEstudiantes);
}

let carreraSeleccionada = null;

async function cargarCarreras() {
  const tbody = document.getElementById('tablaCarreras');
  if (!tbody) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/carreras`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }
    const carreras = await response.json();
    tbody.replaceChildren();

    carreras.forEach((carrera) => {
      const row = document.createElement('tr');
      [carrera.idCarrera, carrera.nombre, carrera.idFacultad].forEach((value) => {
        const cell = document.createElement('td');
        cell.textContent = value === null || value === undefined ? '' : String(value);
        row.appendChild(cell);
      });

      const actions = document.createElement('td');
      actions.className = 'actions';
      const editButton = document.createElement('button');
      editButton.type = 'button';
      editButton.textContent = 'Editar';
      editButton.addEventListener('click', () => seleccionarCarrera(carrera));

      const deleteButton = document.createElement('button');
      deleteButton.type = 'button';
      deleteButton.className = 'delete';
      deleteButton.textContent = 'Eliminar';
      deleteButton.addEventListener('click', () => eliminarCarrera(carrera));

      actions.append(editButton, deleteButton);
      row.appendChild(actions);
      tbody.appendChild(row);
    });
    showMessage('');
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

function seleccionarCarrera(carrera) {
  carreraSeleccionada = carrera.idCarrera;
  document.getElementById('nombreCarrera').value = carrera.nombre ?? '';
  document.getElementById('idFacultad').value = carrera.idFacultad ?? '';
  document.getElementById('guardarCarrera').textContent = 'Guardar cambios';
  document.getElementById('cancelarEdicion').hidden = false;
  document.getElementById('nombreCarrera').focus();
}

function limpiarEdicion() {
  carreraSeleccionada = null;
  document.getElementById('carreraForm')?.reset();
  document.getElementById('guardarCarrera').textContent = 'Registrar';
  document.getElementById('cancelarEdicion').hidden = true;
}

async function guardarCarrera(event) {
  event.preventDefault();
  const carrera = {
    nombre: document.getElementById('nombreCarrera').value.trim(),
    idFacultad: Number(document.getElementById('idFacultad').value)
  };

  if (!carrera.nombre || !Number.isInteger(carrera.idFacultad) || carrera.idFacultad < 1) {
    showMessage('Ingrese un nombre y un ID de facultad válido.', true);
    return;
  }

  const editing = carreraSeleccionada !== null;
  const url = editing ? `${API_BASE_URL}/api/carreras/${carreraSeleccionada}` : `${API_BASE_URL}/api/carreras`;

  try {
    const response = await fetch(url, {
      method: editing ? 'PUT' : 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(carrera)
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }

    showMessage(editing ? 'Carrera actualizada.' : 'Carrera registrada.');
    limpiarEdicion();
    await cargarCarreras();
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

async function eliminarCarrera(carrera) {
  if (!window.confirm(`¿Eliminar la carrera "${carrera.nombre}"?`)) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/carreras/${carrera.idCarrera}`, {
      method: 'DELETE',
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(response.status === 401
        ? 'La sesión expiró. Inicie sesión nuevamente.'
        : await responseError(response));
    }

    if (carreraSeleccionada === carrera.idCarrera) limpiarEdicion();
    showMessage('Carrera eliminada.');
    await cargarCarreras();
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

async function cerrarSesion(event) {
  event.preventDefault();

  try {
    const response = await fetch(`${API_BASE_URL}/api/logout`, {
      method: 'POST',
      credentials: 'include'
    });
    if (!response.ok) throw new Error('No fue posible cerrar la sesión.');
    window.location.href = 'login.html';
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

const loginForm = document.getElementById('loginForm');
if (loginForm) {
  loginForm.addEventListener('submit', loginUser);
  comprobarEstadoServidor();
}

const studentForm = document.getElementById('estudianteForm');
if (studentForm) iniciarModuloEstudiantes();

const careerForm = document.getElementById('carreraForm');
if (careerForm) {
  careerForm.addEventListener('submit', guardarCarrera);
  document.getElementById('cancelarEdicion')?.addEventListener('click', limpiarEdicion);
  cargarCarreras();
}

const logoutForm = document.getElementById('logoutForm');
if (logoutForm) logoutForm.addEventListener('submit', cerrarSesion);

// Muestra en el menú solo los módulos permitidos para el rol de la sesión (el servidor valida igualmente cada petición).
async function aplicarPermisosDelMenu() {
  const enlaces = document.querySelectorAll('[data-roles]');
  if (enlaces.length === 0) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/me`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    if (!response.ok) {
      window.location.href = 'login.html';
      return;
    }
    const { rol, username } = await response.json();
    const usuario = document.getElementById('usuario');
    if (usuario) usuario.textContent = `${username} · ${rol}`;
    enlaces.forEach((enlace) => {
      enlace.hidden = !enlace.dataset.roles.split(',').includes(rol);
    });
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

aplicarPermisosDelMenu();
