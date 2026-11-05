// Dirección a la que el navegador manda sus pedidos. Aquí la página y la API REST (las direcciones
// /api/... que leen, crean, cambian y borran datos mediante HTTP) las entrega el mismo Java, así que
// basta con reutilizar la dirección con la que se abrió la página.
// Lo único aparte es abrir un HTML con file:// desde el disco: ahí no hay servidor de por medio y se
// escribe a mano localhost:8080, que es el puerto donde Java escucha.
const API_BASE_URL = window.location.protocol === 'file:'
  ? 'http://localhost:8080'
  : window.location.origin;
// Caja que todas las páginas dejan arriba para escribir avisos; se localiza por su id.
const message = document.getElementById('message');

// Escribe el aviso: en rojo cuando algo salió mal y en verde cuando todo va bien.
function showMessage(text, isError = false) {
  if (!message) return;
  message.textContent = text;
  message.style.color = isError ? '#dc2626' : '#15803d';
}

// Cuando algo falla, Java manda en la respuesta un texto con llaves (JSON) explicando el motivo, por
// ejemplo {"error":"El correo ya está registrado"}. Aquí se saca ese texto para mostrarlo; si no trae
// ese formato, se enseña lo que haya llegado junto con el número de error de HTTP.
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

// Este error es de conexión: el navegador ni consiguió hablar con Java. Suele ser el servidor apagado
// o que Java no autoriza el otro origen desde el que se le llama (eso es el CORS del mensaje).
function connectionErrorMessage(error) {
  if (error instanceof TypeError) {
    return `No se pudo conectar con Java. Comprueba que el servidor esté activo en ${API_BASE_URL.replace(/^https?:\/\//, '')} y que CORS esté permitido.`;
  }
  return error.message || 'Ocurrió un error inesperado.';
}

// Java manda las fechas como texto ("2026-03-01") y a veces como un número de milisegundos. Aquí todo
// se convierte al formato día/mes/año que la gente lee. Si el texto no se entiende, se deja como vino
// en vez de mostrar una fecha inventada.
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

// Envío del login. Usuario y contraseña viajan escritos como JSON dentro de un pedido HTTP a
// /api/login. Java comprueba la cuenta, cierra la sesión que pudiera haber de antes y abre una nueva
// donde guarda el rol; esa sesión se identifica con una cookie que el navegador manda solo en cada
// pedido siguiente, para no estar escribiendo la contraseña otra vez.
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

  // Se apaga el botón mientras Java responde, para que un doble clic no mande dos ingresos seguidos.
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

    // Con 401 Java dice que las credenciales no valen o la cuenta está inactiva: se muestra su
    // mensaje y la persona se queda escribiendo en la misma página.
    if (!response.ok) {
      showMessage(await responseError(response), true);
      return;
    }

    // La sesión ya existe: se pasa al panel, que es donde se decide qué se ve según el rol.
    showMessage('Inicio de sesión correcto.');
    window.location.href = 'menu.html';
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
    console.error(error);
  } finally {
    if (loginButton) loginButton.disabled = false;
  }
}

// Nada más abrir el login se pregunta a /api/health si Java está levantado y si puede abrir conexión
// con MySQL. Así el aviso de arriba avisa antes de que nadie pierda el tiempo escribiendo credenciales.
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

// Datos que esta pantalla tiene en la memoria del navegador mientras se usa.
let estudiantesActuales = [];      // lista completa leída de /api/estudiantes
let carrerasEstudiantes = [];      // carreras, para los desplegables y para escribir su nombre
let estudianteSeleccionado = null; // fila elegida; null significa que el formulario está creando uno nuevo

// Deja cualquier fecha como texto aaaa-mm-dd, que es lo que entienden las cajas de fecha del navegador.
function fechaISO(value) {
  if (value === null || value === undefined || value === '') return '';
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toISOString().slice(0, 10);
}

// Llena los dos desplegables de carrera (el del formulario y el del filtro) con las carreras que
// manda Java. Estos datos casi no cambian, así que se leen una vez al abrir la pantalla.
async function cargarCarrerasParaEstudiantes() {
  const formSelect = document.getElementById('idCarrera');
  const filterSelect = document.getElementById('filtroCarrera');
  if (!formSelect || !filterSelect) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/carreras`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    // El 401 significa que la sesión se cerró mientras la página estaba abierta; en todos los pedidos
    // de este archivo se avisa igual y no se intenta seguir.
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

// Pide la lista de estudiantes al servidor con las cookies de la sesión. El navegador la guarda y con
// ella trabaja la pantalla: por eso buscar o filtrar no vuelve a tocar la base de datos.
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

// Aplica la búsqueda de texto, la carrera y el rango de fecha sobre la lista que ya está en el
// navegador. Se compara sin mayúsculas y las fechas se comparan como texto aaaa-mm-dd, que ordena bien.
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

// Escribe las filas de la tabla con los datos de cada estudiante. La carrera se busca por su número
// para mostrar el nombre. Cada fila se puede elegir con el clic o con la tecla Enter o el espacio.
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

// Renueva los cuatro números de arriba. La edad promedio se calcula con la fecha de hoy y se descartan
// las edades que no pueden ser (negativas o mayores de 130), que salen de fechas mal escritas.
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

// Al elegir una fila, sus datos pasan al formulario y la tabla se redibuja con esa fila resaltada.
// A partir de aquí "Actualizar" y "Eliminar" trabajan sobre este estudiante.
function seleccionarEstudiante(student) {
  estudianteSeleccionado = student.idEstudiante;
  document.getElementById('estudianteId').value = student.idEstudiante;
  document.getElementById('nombre').value = student.nombre ?? '';
  document.getElementById('apellido').value = student.apellido ?? '';
  document.getElementById('email').value = student.email ?? '';
  document.getElementById('fechaNacimiento').value = fechaISO(student.fechaNacimiento);
  document.getElementById('idCarrera').value = String(student.idCarrera);
  renderEstudiantes(estudiantesFiltradosActuales());
}

// Repite el mismo filtro de aplicarFiltrosEstudiantes, pero devolviendo la lista: sirve para volver a
// escribir la tabla después de elegir o limpiar, sin perder lo que se estaba viendo.
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

// Deja el formulario en blanco y quita la fila elegida, o sea: vuelve al modo "registrar uno nuevo".
function limpiarFormularioEstudiante() {
  estudianteSeleccionado = null;
  document.getElementById('estudianteForm').reset();
  document.getElementById('estudianteId').value = '';
  renderEstudiantes(estudiantesFiltradosActuales());
}

// Lee las cajas del formulario y arma el objeto que después se convierte al JSON que acepta Java.
function obtenerDatosFormularioEstudiante() {
  return {
    nombre: document.getElementById('nombre').value.trim(),
    apellido: document.getElementById('apellido').value.trim(),
    email: document.getElementById('email').value.trim(),
    fechaNacimiento: document.getElementById('fechaNacimiento').value,
    idCarrera: Number(document.getElementById('idCarrera').value)
  };
}

// Crea o cambia un estudiante. El mismo botón de enviar crea con POST; "Actualizar" manda PUT a la
// dirección del elegido. Después de guardar se vuelve a pedir la lista: el número nuevo y los datos
// confirmados los pone Java, así que la tabla debe mostrar lo que quedó en la base, no lo que se escribió.
async function guardarEstudiante(event, actualizar = false) {
  event?.preventDefault();
  if (actualizar && estudianteSeleccionado === null) {
    showMessage('Seleccione un estudiante de la tabla para actualizar.', true);
    return;
  }
  const form = document.getElementById('estudianteForm');
  if (!form.reportValidity()) return;

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
    // El navegador todavía no sabe si el correo se repite o la carrera existe; eso lo decide Java y,
    // si no le gusta, contesta con el motivo en su mensaje.
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
    button.disabled = false;
  }
}

// Borra al estudiante elegido, pero solo tras confirmar. Si tiene matrículas o una cuenta de acceso
// encima, Java lo impide y escribe el motivo; la tabla se recarga cuando el borrado sí ocurrió.
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
    button.disabled = false;
  }
}

// Arranca solo en la pantalla de estudiantes: pone la fecha de arriba, enlaza botones y filtros, y
// primero lee las carreras para luego leer los estudiantes (las tablas necesitan los nombres).
function iniciarModuloEstudiantes() {
  const currentDate = document.getElementById('fechaActual');
  if (currentDate) {
    currentDate.textContent = new Intl.DateTimeFormat('es-CO', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(new Date());
  }

  // En esta pantalla la búsqueda se aplica mientras se escribe, sin botón de por medio; "Restablecer"
  // deja las cinco cajas como al principio.
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

let carreraSeleccionada = null;  // carrera que se está corrigiendo; null = el formulario está creando

// Lee las carreras y escribe las filas con sus botones. El encabezado de la tabla ya viene escrito en
// el HTML, así que aquí solo se llenan las filas.
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

// Pasa la carrera elegida al mismo formulario que crea, para que sirva para los dos casos.
function seleccionarCarrera(carrera) {
  carreraSeleccionada = carrera.idCarrera;
  document.getElementById('nombreCarrera').value = carrera.nombre ?? '';
  document.getElementById('idFacultad').value = carrera.idFacultad ?? '';
  document.getElementById('guardarCarrera').textContent = 'Guardar cambios';
  document.getElementById('cancelarEdicion').hidden = false;
  document.getElementById('nombreCarrera').focus();
}

// Vuelve el formulario de carreras al modo "crear" y esconde el botón de cancelar.
function limpiarEdicion() {
  carreraSeleccionada = null;
  document.getElementById('carreraForm')?.reset();
  document.getElementById('guardarCarrera').textContent = 'Registrar';
  document.getElementById('cancelarEdicion').hidden = true;
}

// Crea con POST o cambia con PUT según haya carrera elegida. El número de facultad se escribe a mano
// en esta pantalla: si esa facultad no existe en la base, Java contesta explicándolo.
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

// Borra la carrera después de confirmar. Si justo se estaba editando esa misma, se limpia el
// formulario para que no quede apuntando a un registro que ya no existe.
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

// Cerrar sesión: se avisa a Java para que olvide al usuario y la cookie deja de servir. Después se
// regresa al login; si el aviso no llegó, la persona se queda donde está con el error escrito.
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

// Cada página arranca lo suyo buscando un elemento que solo ella tiene: si aparece el formulario de
// login se enlaza el ingreso y se comprueba el servidor; si aparece el de estudiantes o el de carreras
// se monta ese módulo. Así un solo archivo sirve para todas las páginas.
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

// En el panel y en la barra lateral, los enlaces a módulos nacen ocultos y llevan un data-roles que
// dice para qué roles son. Aquí se pregunta a /api/me el rol de la sesión y se dejan visibles solo los
// que le corresponden; los grupos que quedan sin enlaces visibles también se ocultan. Ojo: esto es
// comodidad, no seguridad. El servidor comprueba la sesión en cada petición, así que escribir en la
// barra la dirección de una página que no le toca tampoco sirve de nada.
async function aplicarPermisosDelMenu() {
  const enlaces = document.querySelectorAll('[data-roles]');
  if (enlaces.length === 0) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/me`, {
      headers: { Accept: 'application/json' },
      credentials: 'include'
    });
    // Sin sesión que responda (401) no hay nada que filtrar: se regresa al login.
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
    document.querySelectorAll('.grupo').forEach((grupo) => {
      const visibles = [...grupo.querySelectorAll('[data-roles]')].filter((enlace) => !enlace.hidden);
      grupo.hidden = visibles.length === 0;
    });
  } catch (error) {
    showMessage(connectionErrorMessage(error), true);
  }
}

aplicarPermisosDelMenu();
