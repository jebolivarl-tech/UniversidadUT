package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Seccion;

/**
 * DAO de las secciones. Una sección es un grupo: un curso, dictado por un profesor en un periodo, en un
 * aula y con cupo limitado. La tabla secciones no guarda ninguno de esos nombres, solo sus números, y por
 * eso casi todas las consultas de esta clase llevan JOIN: MySQL arma la fila junto con las filas hermanas
 * de cursos y de periodos, y la pantalla recibe el nombre del curso y del periodo sin otra consulta suelta.
 * "Sección" aquí es el grupo de clase, no la sesión con la que el navegador recuerda quién entró.
 */
public class SeccionDAOImpl implements SeccionDAO {

    // La consulta que comparten casi todos los listar. Trae tres cosas de más: el nombre del curso, el del
    // periodo (ambos por JOIN, porque la tabla solo guarda sus números) y cuántas matrículas tiene el grupo,
    // que es el cupo ya ocupado.
    private static final String BASE = """
            SELECT s.id_seccion, s.id_curso, s.id_profesor, s.id_periodo, s.aula, s.cupo_maximo,
                   c.nombre AS nombre_curso, p.nombre AS nombre_periodo,
                   (SELECT COUNT(*) FROM matriculas m WHERE m.id_seccion = s.id_seccion) AS cupos_ocupados
            FROM secciones s
            JOIN cursos c ON c.id_curso = s.id_curso
            JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
            """;

    // Grupos en los que el estudiante está inscrito. Al cruzarse con matrículas, cada inscripción aporta su
    // renglón, y DISTINCT quita el grupo repetido por si llegara a duplicarse.
    private static final String POR_ESTUDIANTE = """
            SELECT DISTINCT s.id_seccion, s.id_curso, s.id_profesor, s.id_periodo, s.aula, s.cupo_maximo,
                   c.nombre AS nombre_curso, p.nombre AS nombre_periodo,
                   (SELECT COUNT(*) FROM matriculas m2 WHERE m2.id_seccion = s.id_seccion) AS cupos_ocupados
            FROM secciones s
            JOIN cursos c ON c.id_curso = s.id_curso
            JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
            JOIN matriculas m ON m.id_seccion = s.id_seccion
            WHERE m.id_estudiante = ?
            ORDER BY s.id_periodo DESC, s.id_seccion
            """;

    @Override
    public List<Seccion> listarTodos() {
        return consultar(BASE + " ORDER BY s.id_periodo DESC, s.id_seccion", null);
    }

    // Los grupos que tiene a cargo: la pantalla del profesor.
    @Override
    public List<Seccion> listarPorProfesor(int idProfesor) {
        return consultar(BASE + " WHERE s.id_profesor = ? ORDER BY s.id_periodo DESC, s.id_seccion", idProfesor);
    }

    // Los grupos de un periodo. El servicio los usa para comparar: dos grupos del mismo curso no pueden
    // partirse el aula en el mismo periodo.
    @Override
    public List<Seccion> listarPorPeriodo(int idPeriodo) {
        return consultar(BASE + " WHERE s.id_periodo = ? ORDER BY s.id_seccion", idPeriodo);
    }

    @Override
    public List<Seccion> listarPorEstudiante(int idEstudiante) {
        return consultar(POR_ESTUDIANTE, idEstudiante);
    }

    /**
     * Los grupos que este estudiante todavía puede inscribir. Cuatro filtros, cada uno con su motivo: se
     * cruza con estudiantes para dejar solo los cursos de su carrera; se exige que el periodo esté ACTIVO,
     * porque un periodo cerrado no admite inscripciones; se descarta todo grupo cuyo curso ya tenga matrícula
     * del estudiante en ese mismo periodo, que es la regla uq_matricula de la base (se compara por el nombre
     * del periodo, porque así lo guarda la matrícula); y se pide cupo por debajo del máximo. El tope de
     * créditos no está aquí a propósito: esa regla vive en el servicio, que es donde se suman los créditos
     * de todos los periodos.
     */
    @Override
    public List<Seccion> listarInscribiblesPara(int idEstudiante) {
        String sql = BASE + """
                 JOIN estudiantes e ON e.id_carrera = c.id_carrera
                WHERE e.id_estudiante = ? AND p.estado = 'ACTIVO'
                  AND NOT EXISTS (SELECT 1 FROM matriculas ya
                                  WHERE ya.id_estudiante = e.id_estudiante
                                    AND ya.id_curso = s.id_curso
                                    AND ya.periodo = p.nombre)
                  AND (SELECT COUNT(*) FROM matriculas ocupada
                       WHERE ocupada.id_seccion = s.id_seccion) < s.cupo_maximo
                ORDER BY c.nombre, s.id_seccion
                """;
        return consultar(sql, idEstudiante);
    }

    @Override
    public Seccion leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(BASE + " WHERE s.id_seccion = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Trae null si ese número de grupo no existe; con el JOIN, la respuesta es una fila ya
                // servida con los nombres que faltan.
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la sección.", ex);
        }
    }

    // La base tiene una clave única sobre curso, periodo y aula, así que repetir el mismo grupo lo rechaza
    // MySQL. El servicio comprueba lo mismo antes, para avisar con nombres en vez de dejar un error de la
    // base sin explicar.
    @Override
    public int registrar(Seccion s) throws SQLException {
        String sql = "INSERT INTO secciones (id_curso, id_profesor, id_periodo, aula, cupo_maximo) VALUES (?, ?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getIdCurso());
            ps.setInt(2, s.getIdProfesor());
            ps.setInt(3, s.getIdPeriodo());
            ps.setString(4, s.getAula());
            ps.setInt(5, s.getCupoMaximo());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdSeccion(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Seccion s) throws SQLException {
        String sql = "UPDATE secciones SET id_curso = ?, id_profesor = ?, id_periodo = ?, aula = ?, cupo_maximo = ? WHERE id_seccion = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, s.getIdCurso());
            ps.setInt(2, s.getIdProfesor());
            ps.setInt(3, s.getIdPeriodo());
            ps.setString(4, s.getAula());
            // Bajar el cupo por debajo de los que ya están inscritos lo revisa el servicio antes de llegar
            // aquí; MySQL no sabe comparar dos tablas en esa regla.
            ps.setInt(5, s.getCupoMaximo());
            ps.setInt(6, s.getIdSeccion());
            return ps.executeUpdate();
        }
    }

    // Con matriculados encima, MySQL no la deja borrar (la relación con matrículas está en modo restrictivo).
    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM secciones WHERE id_seccion = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // El común de los listar: null es que la consulta no tiene hueco donde meter.
    private List<Seccion> consultar(String sql, Integer parametro) {
        List<Seccion> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las secciones.", ex);
        }
        return lista;
    }

    // La fila del JOIN llega mezclada: columnas del grupo, los dos nombres prestados y el recuento de
    // matrículas. Aquí cada una entra en su campo.
    private Seccion mapear(ResultSet rs) throws SQLException {
        Seccion s = new Seccion();
        s.setIdSeccion(rs.getInt("id_seccion"));
        s.setIdCurso(rs.getInt("id_curso"));
        s.setIdProfesor(rs.getInt("id_profesor"));
        s.setIdPeriodo(rs.getInt("id_periodo"));
        s.setAula(rs.getString("aula"));
        s.setCupoMaximo(rs.getInt("cupo_maximo"));
        s.setNombreCurso(rs.getString("nombre_curso"));
        s.setNombrePeriodo(rs.getString("nombre_periodo"));
        s.setCuposOcupados(rs.getInt("cupos_ocupados"));
        return s;
    }
}
