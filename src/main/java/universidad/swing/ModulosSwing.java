package universidad.swing;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JFrame;
import universidad.CarreraForm;
import universidad.EstudianteForm;
import universidad.dao.AsistenciaDAO;
import universidad.dao.AsistenciaDAOImpl;
import universidad.dao.CRUD;
import universidad.dao.CalificacionDAO;
import universidad.dao.CalificacionDAOImpl;
import universidad.dao.CarreraDAO;
import universidad.dao.CarreraDAOImpl;
import universidad.dao.CursoDAO;
import universidad.dao.CursoDAOImpl;
import universidad.dao.EstudianteDAO;
import universidad.dao.EstudianteDAOImpl;
import universidad.dao.FacultadDAO;
import universidad.dao.FacultadDAOImpl;
import universidad.dao.MatriculaDAO;
import universidad.dao.MatriculaDAOImpl;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.PeriodoAcademicoDAOImpl;
import universidad.dao.ProfesorDAO;
import universidad.dao.ProfesorDAOImpl;
import universidad.dao.SeccionDAO;
import universidad.dao.SeccionDAOImpl;
import universidad.dao.UsuarioDAO;
import universidad.dao.UsuarioDAOImpl;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Carrera;
import universidad.modelo.Cuenta;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.Facultad;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;
import universidad.swing.FormularioCRUD.Campo;
import universidad.swing.FormularioCRUD.Definicion;
import universidad.swing.FormularioCRUD.OptionItem;
import universidad.web.Validaciones;

/**
 * Catálogo de módulos del cliente de escritorio. Cada ventana se construye con una
 * {@link Definicion} y repite la matriz de permisos de la API web: el rol decide qué se muestra,
 * qué se puede editar y qué listado consulta.
 */
public final class ModulosSwing {

    private static final String ADMINISTRATIVO = "ADMINISTRATIVO";
    private static final String PROFESOR = "PROFESOR";
    private static final String ESTUDIANTE = "ESTUDIANTE";

    /** Entrada del panel de control: título del botón y ventana que abre. */
    public static final class Modulo {
        private final String titulo;
        private final Supplier<JFrame> crear;

        private Modulo(String titulo, Supplier<JFrame> crear) {
            this.titulo = titulo;
            this.crear = crear;
        }

        public String titulo() {
            return titulo;
        }

        public JFrame crearVentana() {
            return crear.get();
        }
    }

    private final String rol;
    private final Integer idUsuario;
    private final Integer idEstudiante;
    private final Integer idProfesor;
    private final boolean esAdministrativo;
    private final boolean esProfesor;
    private final boolean esEstudiante;

    private final FacultadDAO facultades = new FacultadDAOImpl();
    private final ProfesorDAO profesores = new ProfesorDAOImpl();
    private final CarreraDAO carreras = new CarreraDAOImpl();
    private final EstudianteDAO estudiantes = new EstudianteDAOImpl();
    private final PeriodoAcademicoDAO periodos = new PeriodoAcademicoDAOImpl();
    private final CursoDAO cursos = new CursoDAOImpl();
    private final SeccionDAO secciones = new SeccionDAOImpl();
    private final MatriculaDAO matriculas = new MatriculaDAOImpl();
    private final AsistenciaDAO asistencias = new AsistenciaDAOImpl();
    private final CalificacionDAO calificaciones = new CalificacionDAOImpl();
    private final UsuarioDAO usuarios = new UsuarioDAOImpl();

    private ModulosSwing(String rol, Integer idUsuario, Integer idEstudiante, Integer idProfesor) {
        this.rol = rol == null ? "" : rol.toUpperCase();
        this.idUsuario = idUsuario;
        this.idEstudiante = idEstudiante;
        this.idProfesor = idProfesor;
        this.esAdministrativo = ADMINISTRATIVO.equals(this.rol);
        this.esProfesor = PROFESOR.equals(this.rol);
        this.esEstudiante = ESTUDIANTE.equals(this.rol);
    }

    /** Módulos visibles para el usuario autenticado, en el orden del panel de control. */
    public static List<Modulo> modulos(String rol, Integer idUsuario, Integer idEstudiante, Integer idProfesor) {
        ModulosSwing cliente = new ModulosSwing(rol, idUsuario, idEstudiante, idProfesor);
        if (cliente.esEstudiante) {
            return cliente.modulosDeEstudiante();
        }
        List<Modulo> modulos = new ArrayList<>();
        if (cliente.esAdministrativo) {
            modulos.add(new Modulo("Gestión de Estudiantes", EstudianteForm::new));
            modulos.add(new Modulo("Gestión de Carreras", CarreraForm::new));
        } else {
            modulos.add(new Modulo("Estudiantes", cliente.ventanaEstudiantes()));
            modulos.add(new Modulo("Carreras", cliente.ventanaCarreras()));
        }
        modulos.add(new Modulo("Facultades", cliente.ventanaFacultades()));
        modulos.add(new Modulo("Profesores", cliente.ventanaProfesores()));
        modulos.add(new Modulo("Periodos académicos", cliente.ventanaPeriodos()));
        modulos.add(new Modulo("Cursos", cliente.ventanaCursos()));
        modulos.add(new Modulo("Secciones", cliente.ventanaSecciones()));
        modulos.add(new Modulo("Matrículas", cliente.ventanaMatriculas()));
        modulos.add(new Modulo("Asistencias", cliente.ventanaAsistencias()));
        modulos.add(new Modulo("Calificaciones", cliente.ventanaCalificaciones()));
        if (cliente.esAdministrativo) {
            modulos.add(new Modulo("Cuentas de acceso", cliente.ventanaCuentas()));
        }
        return modulos;
    }

    private List<Modulo> modulosDeEstudiante() {
        return List.of(
                new Modulo("Mis secciones", ventanaSecciones()),
                new Modulo("Mis matrículas", ventanaMatriculas()),
                new Modulo("Mis asistencias", ventanaAsistencias()),
                new Modulo("Mis calificaciones", ventanaCalificaciones()));
    }

    // ------------------------------------------------------------------ ventanas

    /** Estudiantes y carreras como tabla de solo lectura: el alta la hacen las ventanas propias. */
    private Supplier<JFrame> ventanaEstudiantes() {
        return () -> new FormularioCRUD<>(Definicion.de("Estudiantes", estudiantes, Estudiante::new)
                .id(Estudiante::setIdEstudiante)
                .columna("ID", Estudiante::getIdEstudiante)
                .columna("Nombre", Estudiante::getNombre)
                .columna("Apellido", Estudiante::getApellido)
                .columna("Correo", Estudiante::getEmail)
                .columna("Fecha nacimiento", Estudiante::getFechaNacimiento)
                .columna("Carrera", Estudiante::getIdCarrera)
                .soloLectura(true)
                .construir());
    }

    private Supplier<JFrame> ventanaCarreras() {
        return () -> new FormularioCRUD<>(Definicion.de("Carreras", carreras, Carrera::new)
                .id(Carrera::setIdCarrera)
                .columna("ID", Carrera::getIdCarrera)
                .columna("Nombre", Carrera::getNombre)
                .columna("Facultad", Carrera::getIdFacultad)
                .soloLectura(true)
                .construir());
    }

    private Supplier<JFrame> ventanaFacultades() {
        return () -> new FormularioCRUD<>(Definicion.de("Facultades", facultades, Facultad::new)
                .id(Facultad::setIdFacultad)
                .columna("ID", Facultad::getIdFacultad)
                .columna("Nombre", Facultad::getNombre)
                .columna("Decano", Facultad::getDecano)
                .campo(Campo.texto("Nombre:", f -> texto(f.getNombre()), Facultad::setNombre))
                .campo(Campo.texto("Decano (opcional):", f -> texto(f.getDecano()), Facultad::setDecano))
                .valido(Validaciones::esFacultadValida, "El nombre es obligatorio y de hasta 100 caracteres.")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaProfesores() {
        return () -> new FormularioCRUD<>(Definicion.de("Profesores", profesores, Profesor::new)
                .id(Profesor::setIdProfesor)
                .columna("ID", Profesor::getIdProfesor)
                .columna("Nombre", Profesor::getNombre)
                .columna("Apellido", Profesor::getApellido)
                .columna("Correo", Profesor::getEmail)
                .columna("Facultad", Profesor::getIdFacultad)
                .campo(Campo.texto("Nombre:", p -> texto(p.getNombre()), Profesor::setNombre))
                .campo(Campo.texto("Apellido:", p -> texto(p.getApellido()), Profesor::setApellido))
                .campo(Campo.texto("Correo:", p -> texto(p.getEmail()), Profesor::setEmail))
                .campo(combo("Facultad:", Profesor::getIdFacultad, Profesor::setIdFacultad,
                        this::opcionesFacultades))
                .antesEscribir(p -> asegurar(facultades.leerPorId(p.getIdFacultad()) != null, "la facultad"))
                .valido(Validaciones::esProfesorValido, "Revise nombre, apellido, correo y la facultad.")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaPeriodos() {
        return () -> new FormularioCRUD<>(Definicion.de("Periodos académicos", periodos, PeriodoAcademico::new)
                .id(PeriodoAcademico::setIdPeriodo)
                .columna("ID", PeriodoAcademico::getIdPeriodo)
                .columna("Nombre", PeriodoAcademico::getNombre)
                .columna("Inicio", PeriodoAcademico::getFechaInicio)
                .columna("Fin", PeriodoAcademico::getFechaFin)
                .columna("Estado", PeriodoAcademico::getEstado)
                .campo(Campo.texto("Nombre:", p -> texto(p.getNombre()), PeriodoAcademico::setNombre))
                .campo(Campo.texto("Fecha inicio (AAAA-MM-DD):", p -> fecha(p.getFechaInicio()),
                        (p, v) -> p.setFechaInicio(fecha("la fecha de inicio", v))))
                .campo(Campo.texto("Fecha fin (AAAA-MM-DD):", p -> fecha(p.getFechaFin()),
                        (p, v) -> p.setFechaFin(fecha("la fecha de fin", v))))
                .campo(Campo.lista("Estado:", p -> texto(p.getEstado()), PeriodoAcademico::setEstado,
                        () -> List.of(OptionItem.texto(PeriodoAcademico.ACTIVO),
                                OptionItem.texto(PeriodoAcademico.INACTIVO),
                                OptionItem.texto(PeriodoAcademico.CERRADO))))
                .valido(Validaciones::esPeriodoValido,
                        "Nombre, estado y fechas coherentes (la inicio no puede ser posterior a la fin).")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaCursos() {
        return () -> new FormularioCRUD<>(Definicion.de("Cursos", cursos, Curso::new)
                .id(Curso::setIdCurso)
                .columna("ID", Curso::getIdCurso)
                .columna("Nombre", Curso::getNombre)
                .columna("Créditos", Curso::getCreditos)
                .columna("Carrera", Curso::getIdCarrera)
                .columna("Profesor", Curso::getIdProfesor)
                .campo(Campo.texto("Nombre:", c -> texto(c.getNombre()), Curso::setNombre))
                .campo(combo("Créditos:", Curso::getCreditos, Curso::setCreditos, this::opcionesCreditos))
                .campo(combo("Carrera:", Curso::getIdCarrera, Curso::setIdCarrera, this::opcionesCarreras))
                .campo(combo("Profesor:", Curso::getIdProfesor, Curso::setIdProfesor, this::opcionesProfesores))
                .antesEscribir(c -> {
                    asegurar(carreras.leerPorId(c.getIdCarrera()) != null, "la carrera");
                    asegurar(profesores.leerPorId(c.getIdProfesor()) != null, "el profesor");
                })
                .valido(Validaciones::esCursoValido, "Revise el nombre y los créditos (1 a 10).")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaSecciones() {
        Supplier<List<Seccion>> listado = switch (rol) {
            case ESTUDIANTE -> () -> secciones.listarPorEstudiante(idEstudiante);
            case PROFESOR -> () -> secciones.listarPorProfesor(idProfesor);
            default -> secciones::listarTodos;
        };
        return () -> new FormularioCRUD<>(Definicion.de("Secciones", secciones, Seccion::new)
                .id(Seccion::setIdSeccion)
                .listado(listado)
                .columna("ID", Seccion::getIdSeccion)
                .columna("Curso", Seccion::getIdCurso)
                .columna("Profesor", Seccion::getIdProfesor)
                .columna("Periodo", Seccion::getIdPeriodo)
                .columna("Aula", Seccion::getAula)
                .columna("Cupo", Seccion::getCupoMaximo)
                .campo(combo("Curso:", Seccion::getIdCurso, Seccion::setIdCurso, this::opcionesCursos))
                .campo(combo("Profesor:", Seccion::getIdProfesor, Seccion::setIdProfesor, this::opcionesProfesores))
                .campo(combo("Periodo:", Seccion::getIdPeriodo, Seccion::setIdPeriodo, this::opcionesPeriodos))
                .campo(Campo.texto("Aula:", s -> texto(s.getAula()), Seccion::setAula))
                .campo(Campo.texto("Cupo máximo:", s -> String.valueOf(s.getCupoMaximo()),
                        (s, v) -> s.setCupoMaximo(entero("el cupo máximo", v))))
                .antesEscribir(this::validarSeccion)
                .valido(Validaciones::esSeccionValida, "Revise el aula (hasta 50 caracteres) y el cupo (1 a 200).")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaMatriculas() {
        Supplier<List<Matricula>> listado = switch (rol) {
            case ESTUDIANTE -> () -> matriculas.listarPorEstudiante(idEstudiante);
            case PROFESOR -> this::listarMatriculasDeMisCursos;
            default -> matriculas::listarTodos;
        };
        return () -> new FormularioCRUD<>(Definicion.de("Matrículas", matriculas, Matricula::new)
                .id(Matricula::setIdMatricula)
                .listado(listado)
                .columna("ID", Matricula::getIdMatricula)
                .columna("Estudiante", Matricula::getIdEstudiante)
                .columna("Curso", Matricula::getIdCurso)
                .columna("Periodo", Matricula::getPeriodo)
                .columna("Nota", Matricula::getNota)
                .campo(combo("Estudiante:", Matricula::getIdEstudiante, Matricula::setIdEstudiante,
                        this::opcionesEstudiantes))
                .campo(combo("Curso:", Matricula::getIdCurso, Matricula::setIdCurso, this::opcionesCursos))
                .campo(Campo.texto("Periodo (ej. 2026-1):", m -> texto(m.getPeriodo()), Matricula::setPeriodo))
                .campo(Campo.texto("Nota (opcional, 0.0 a 5.0):", m -> numero(m.getNota()),
                        (m, v) -> m.setNota(nota(v))))
                .antesEscribir(m -> {
                    asegurar(estudiantes.leerPorId(m.getIdEstudiante()) != null, "el estudiante");
                    asegurar(cursos.leerPorId(m.getIdCurso()) != null, "el curso");
                })
                .valido(Validaciones::esMatriculaValida, "Revise el periodo (hasta 20 caracteres) y la nota (0.0 a 5.0).")
                .soloLectura(!esAdministrativo)
                .construir());
    }

    private Supplier<JFrame> ventanaAsistencias() {
        Supplier<List<Asistencia>> listado = switch (rol) {
            case ESTUDIANTE -> () -> asistencias.listarPorEstudiante(idEstudiante);
            case PROFESOR -> this::listarAsistenciasDeMisSecciones;
            default -> asistencias::listarTodos;
        };
        return () -> new FormularioCRUD<>(Definicion.de("Asistencias", asistencias, Asistencia::new)
                .id(Asistencia::setIdAsistencia)
                .listado(listado)
                .columna("ID", Asistencia::getIdAsistencia)
                .columna("Estudiante", Asistencia::getIdEstudiante)
                .columna("Sección", Asistencia::getIdSeccion)
                .columna("Fecha", Asistencia::getFecha)
                .columna("Estado", Asistencia::getEstado)
                .campo(combo("Estudiante:", Asistencia::getIdEstudiante, Asistencia::setIdEstudiante,
                        this::opcionesEstudiantes))
                .campo(combo("Sección:", Asistencia::getIdSeccion, Asistencia::setIdSeccion, this::opcionesSecciones))
                .campo(Campo.texto("Fecha (AAAA-MM-DD):", a -> fecha(a.getFecha()),
                        (a, v) -> a.setFecha(fecha("la fecha", v))))
                .campo(Campo.lista("Estado:", a -> texto(a.getEstado()), Asistencia::setEstado,
                        () -> List.of(OptionItem.texto(Asistencia.ASISTIO), OptionItem.texto(Asistencia.FALTO),
                                OptionItem.texto(Asistencia.TARDANZA), OptionItem.texto(Asistencia.JUSTIFICADO))))
                .antesEscribir(this::validarAsistencia)
                .valido(Validaciones::esAsistenciaValida,
                        "Estudiante, sección, fecha de hoy o anterior y estado son obligatorios.")
                .soloLectura(esEstudiante)
                .construir());
    }

    private Supplier<JFrame> ventanaCalificaciones() {
        Supplier<List<Calificacion>> listado = switch (rol) {
            case ESTUDIANTE -> () -> calificaciones.listarPorEstudiante(idEstudiante);
            case PROFESOR -> this::listarCalificacionesDeMisSecciones;
            default -> calificaciones::listarTodos;
        };
        return () -> new FormularioCRUD<>(Definicion.de("Calificaciones", calificaciones, Calificacion::new)
                .id(Calificacion::setIdCalificacion)
                .listado(listado)
                .columna("ID", Calificacion::getIdCalificacion)
                .columna("Estudiante", Calificacion::getIdEstudiante)
                .columna("Sección", Calificacion::getIdSeccion)
                .columna("Parcial 1", Calificacion::getNotaParcial1)
                .columna("Parcial 2", Calificacion::getNotaParcial2)
                .columna("Examen final", Calificacion::getNotaExamenFinal)
                .columna("Promedio", Calificacion::getPromedioFinal)
                .columna("Estado", Calificacion::getEstado)
                .campo(combo("Estudiante:", Calificacion::getIdEstudiante, Calificacion::setIdEstudiante,
                        this::opcionesEstudiantes))
                .campo(combo("Sección:", Calificacion::getIdSeccion, Calificacion::setIdSeccion,
                        this::opcionesSecciones))
                .campo(Campo.texto("Parcial 1 (opcional):", c -> numero(c.getNotaParcial1()),
                        (c, v) -> c.setNotaParcial1(nota(v))))
                .campo(Campo.texto("Parcial 2 (opcional):", c -> numero(c.getNotaParcial2()),
                        (c, v) -> c.setNotaParcial2(nota(v))))
                .campo(Campo.texto("Examen final (opcional):", c -> numero(c.getNotaExamenFinal()),
                        (c, v) -> c.setNotaExamenFinal(nota(v))))
                .antesEscribir(this::validarCalificacion)
                .valido(Validaciones::esCalificacionValida,
                        "Escala institucional 0.0 a 5.0 y al menos una nota registrada.")
                .soloLectura(esEstudiante)
                .construir());
    }

    private Supplier<JFrame> ventanaCuentas() {
        return () -> new FormularioCRUD<>(Definicion.de("Cuentas de acceso", cuentasDao(), Cuenta::new)
                .id(Cuenta::setIdUsuario)
                .columna("ID", Cuenta::getIdUsuario)
                .columna("Usuario", Cuenta::getUsername)
                .columna("Rol", Cuenta::getRol)
                .columna("Estudiante", Cuenta::getIdEstudiante)
                .columna("Profesor", Cuenta::getIdProfesor)
                .columna("Estado", c -> c.isEstado() ? "ACTIVA" : "INACTIVA")
                .campo(Campo.texto("Usuario:", c -> texto(c.getUsername()), Cuenta::setUsername))
                .campo(Campo.clave("Contraseña (10 caracteres; vacía = no cambiarla):",
                        (c, v) -> c.setPassword(v.isEmpty() ? null : v)))
                .campo(Campo.lista("Rol:", c -> texto(c.getRol()), Cuenta::setRol,
                        () -> List.of(OptionItem.texto(ADMINISTRATIVO), OptionItem.texto(PROFESOR),
                                OptionItem.texto(ESTUDIANTE))))
                .campo(Campo.lista("Ficha de estudiante:", c -> idOpcional(c.getIdEstudiante()),
                        (c, v) -> c.setIdEstudiante(fijaOpcional(v)), this::opcionesEstudiantesNinguna))
                .campo(Campo.lista("Ficha de profesor:", c -> idOpcional(c.getIdProfesor()),
                        (c, v) -> c.setIdProfesor(fijaOpcional(v)), this::opcionesProfesoresNinguno))
                .campo(Campo.lista("Estado:", c -> c.isEstado() ? "ACTIVA" : "INACTIVA",
                        (c, v) -> c.setEstado("ACTIVA".equals(v)),
                        () -> List.of(OptionItem.texto("ACTIVA"), OptionItem.texto("INACTIVA"))))
                .antesEscribir(c -> c.setRol(texto(c.getRol()).toUpperCase()))
                .valido(c -> Validaciones.esCuentaValida(c, false),
                        "Usuario de 3 a 50 caracteres, rol válido y ficha vinculada si el rol es PROFESOR o ESTUDIANTE.")
                .construir());
    }

    /** Adaptador de {@link UsuarioDAO} a CRUD: la contraseña se guarda hasheada y vacía significa "no cambiarla". */
    private CRUD<Cuenta> cuentasDao() {
        return new CRUD<Cuenta>() {
            @Override
            public List<Cuenta> listarTodos() {
                return usuarios.listar();
            }

            @Override
            public Cuenta leerPorId(int id) {
                return usuarios.leerPorId(id);
            }

            @Override
            public int registrar(Cuenta cuenta) throws SQLException {
                if (cuenta.getPassword() == null || cuenta.getPassword().length() < 10) {
                    throw new IllegalArgumentException(
                            "Al crear una cuenta la contraseña debe tener al menos 10 caracteres.");
                }
                if (usuarios.existeUsername(cuenta.getUsername())) {
                    throw new IllegalStateException("El nombre de usuario ya existe.");
                }
                return usuarios.crear(cuenta) == null ? 0 : 1;
            }

            @Override
            public int actualizar(Cuenta cuenta) throws SQLException {
                if (esMiCuenta(cuenta.getIdUsuario()) && !cuenta.isEstado()) {
                    throw new IllegalStateException("No puede desactivar su propia cuenta.");
                }
                Cuenta existente = usuarios.leerPorId(cuenta.getIdUsuario());
                if (existente == null) {
                    return 0;
                }
                if (!existente.getUsername().equalsIgnoreCase(texto(cuenta.getUsername()))
                        && usuarios.existeUsername(cuenta.getUsername())) {
                    throw new IllegalStateException("El nombre de usuario ya existe.");
                }
                int filas = usuarios.actualizar(cuenta);
                if (cuenta.getPassword() != null && !cuenta.getPassword().isBlank()) {
                    usuarios.cambiarPassword(cuenta.getIdUsuario(), cuenta.getPassword());
                }
                return filas;
            }

            @Override
            public int eliminar(int idCuenta) throws SQLException {
                if (esMiCuenta(idCuenta)) {
                    throw new IllegalStateException("No puede eliminar su propia cuenta.");
                }
                return usuarios.eliminar(idCuenta);
            }
        };
    }

    // ------------------------------------------------------------------ reglas de escritura

    private void validarSeccion(Seccion seccion) {
        asegurar(cursos.leerPorId(seccion.getIdCurso()) != null, "el curso");
        asegurar(profesores.leerPorId(seccion.getIdProfesor()) != null, "el profesor");
        asegurar(periodos.leerPorId(seccion.getIdPeriodo()) != null, "el periodo");
        boolean aulaOcupada = secciones.listarPorPeriodo(seccion.getIdPeriodo()).stream()
                .filter(otra -> otra.getIdCurso() == seccion.getIdCurso())
                .anyMatch(otra -> texto(otra.getAula()).equalsIgnoreCase(texto(seccion.getAula()))
                        && otra.getIdSeccion() != seccion.getIdSeccion());
        if (aulaOcupada) {
            throw new IllegalStateException("Esa aula ya está asignada al mismo curso en el mismo periodo.");
        }
    }

    private void validarAsistencia(Asistencia asistencia) {
        asistencia.setEstado(texto(asistencia.getEstado()).toUpperCase());
        asegurar(estudiantes.leerPorId(asistencia.getIdEstudiante()) != null, "el estudiante");
        asegurar(secciones.leerPorId(asistencia.getIdSeccion()) != null, "la sección");
        asegurarSeccionEscribible(asistencia.getIdSeccion());
        boolean repetida = asistencias.listarPorSeccionYFecha(asistencia.getIdSeccion(), asistencia.getFecha()).stream()
                .anyMatch(otra -> otra.getIdEstudiante() == asistencia.getIdEstudiante()
                        && otra.getIdAsistencia() != asistencia.getIdAsistencia());
        if (repetida) {
            throw new IllegalStateException("Ese estudiante ya tiene un registro de asistencia en esa fecha.");
        }
    }

    private void validarCalificacion(Calificacion calificacion) {
        asegurar(estudiantes.leerPorId(calificacion.getIdEstudiante()) != null, "el estudiante");
        asegurar(secciones.leerPorId(calificacion.getIdSeccion()) != null, "la sección");
        asegurarSeccionEscribible(calificacion.getIdSeccion());
    }

    /** El profesor solo registra asistencia y calificación en sus propias secciones. */
    private void asegurarSeccionEscribible(int idSeccion) {
        if (!esProfesor) {
            return;
        }
        Seccion seccion = secciones.leerPorId(idSeccion);
        if (seccion == null || seccion.getIdProfesor() != idProfesor) {
            throw new IllegalStateException("Solo puede escribir en las secciones asignadas a su profesor.");
        }
    }

    private boolean esMiCuenta(int id) {
        return idUsuario != null && idUsuario == id;
    }

    // ------------------------------------------------------------------ listados del rol

    private List<Matricula> listarMatriculasDeMisCursos() {
        List<Integer> cursosPropios = secciones.listarPorProfesor(idProfesor).stream()
                .map(Seccion::getIdCurso).distinct().toList();
        return matriculas.listarTodos().stream()
                .filter(m -> cursosPropios.contains(m.getIdCurso())).toList();
    }

    private List<Asistencia> listarAsistenciasDeMisSecciones() {
        return filtrarPorSeccionesPropias(asistencias.listarTodos(), Asistencia::getIdSeccion);
    }

    private List<Calificacion> listarCalificacionesDeMisSecciones() {
        return filtrarPorSeccionesPropias(calificaciones.listarTodos(), Calificacion::getIdSeccion);
    }

    private <E> List<E> filtrarPorSeccionesPropias(List<E> filas, Function<E, Integer> lectorSeccion) {
        List<Integer> propias = secciones.listarPorProfesor(idProfesor).stream()
                .map(Seccion::getIdSeccion).toList();
        return filas.stream().filter(f -> propias.contains(lectorSeccion.apply(f))).toList();
    }

    // ------------------------------------------------------------------ catálogos

    private List<OptionItem> opcionesFacultades() {
        return opciones(facultades.listarTodos(), Facultad::getIdFacultad, f -> f.getNombre());
    }

    private List<OptionItem> opcionesProfesores() {
        return opciones(profesores.listarTodos(), Profesor::getIdProfesor,
                p -> p.getApellido() + ", " + p.getNombre());
    }

    private List<OptionItem> opcionesProfesoresNinguno() {
        return conNinguna(opcionesProfesores());
    }

    private List<OptionItem> opcionesCarreras() {
        return opciones(carreras.listarTodos(), Carrera::getIdCarrera, c -> c.getNombre());
    }

    private List<OptionItem> opcionesEstudiantes() {
        return opciones(estudiantes.listarTodos(), Estudiante::getIdEstudiante,
                e -> e.getApellido() + ", " + e.getNombre());
    }

    private List<OptionItem> opcionesEstudiantesNinguna() {
        return conNinguna(opcionesEstudiantes());
    }

    private List<OptionItem> opcionesPeriodos() {
        return opciones(periodos.listarTodos(), PeriodoAcademico::getIdPeriodo,
                p -> p.getNombre() + " (" + p.getEstado() + ")");
    }

    private List<OptionItem> opcionesCursos() {
        return opciones(cursos.listarTodos(), Curso::getIdCurso, Curso::getNombre);
    }

    private List<OptionItem> opcionesSecciones() {
        return opciones(secciones.listarTodos(), Seccion::getIdSeccion,
                s -> "curso " + s.getIdCurso() + " · " + nombreDeCurso(s.getIdCurso()) + " · aula " + s.getAula());
    }

    private String nombreDeCurso(int idCurso) {
        Curso curso = cursos.leerPorId(idCurso);
        return curso == null ? "sin nombre" : curso.getNombre();
    }

    private List<OptionItem> opcionesCreditos() {
        List<OptionItem> opciones = new ArrayList<>();
        for (int credito = 1; credito <= 10; credito++) {
            opciones.add(new OptionItem(credito, String.valueOf(credito)));
        }
        return opciones;
    }

    private static <E> List<OptionItem> opciones(List<E> filas, Function<E, Integer> lectorId,
                                                 Function<E, String> lectorTexto) {
        List<OptionItem> opciones = new ArrayList<>();
        for (E fila : filas) {
            int id = lectorId.apply(fila);
            opciones.add(new OptionItem(id, id + " - " + lectorTexto.apply(fila)));
        }
        return opciones;
    }

    private static List<OptionItem> conNinguna(List<OptionItem> opciones) {
        List<OptionItem> conNinguna = new ArrayList<>();
        conNinguna.add(new OptionItem(0, "0 - ninguna"));
        conNinguna.addAll(opciones);
        return conNinguna;
    }

    // ------------------------------------------------------------------ conversiones de campos

    private static <T> Campo<T> combo(String etiqueta, Function<T, Integer> lector,
                                       BiConsumer<T, Integer> fijar, Supplier<List<OptionItem>> opciones) {
        return Campo.lista(etiqueta, t -> String.valueOf(lector.apply(t)),
                (t, valor) -> fijar.accept(t, entero(etiqueta, valor)), opciones);
    }

    private static Integer fijaOpcional(String valor) {
        return valor.isEmpty() || "0".equals(valor) ? null : Integer.valueOf(valor);
    }

    private static String idOpcional(Integer valor) {
        return valor == null ? "0" : String.valueOf(valor);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private static String fecha(Date valor) {
        return valor == null ? "" : valor.toString();
    }

    private static String numero(Double valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static int entero(String etiqueta, String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(etiqueta + " debe ser un número (registre primero los datos del catálogo).");
        }
    }

    private static Date fecha(String etiqueta, String valor) {
        try {
            return Date.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(etiqueta + " debe tener el formato AAAA-MM-DD.");
        }
    }

    private static Double nota(String valor) {
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(valor.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Las notas deben escribirse entre 0.0 y 5.0.");
        }
    }

    private static void asegurar(boolean condicion, String recurso) {
        if (!condicion) {
            throw new IllegalStateException("No se encontró " + recurso + " registrado en la base de datos.");
        }
    }
}
