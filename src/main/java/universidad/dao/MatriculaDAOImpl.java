package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Matricula;

/**
 * DAO de las matrículas, la fila que ata un estudiante con un grupo. Además del grupo repite el curso y el
 * nombre del periodo, y no es un descuido: son los datos con los que la base prohíbe inscribir el mismo
 * curso dos veces en el mismo periodo, aunque cambie el grupo.
 * Todo el SQL sale por JDBC hacia MySQL y las conexiones se toman y se devuelven al pool de conexiones en
 * cada bloque. Los valores del formulario se entregan aparte de la consulta, nunca dentro del texto.
 */
public class MatriculaDAOImpl implements MatriculaDAO {

    // La consulta que repiten los listar. La tabla se llama m solo para escribir menos; cada método le
    // pega al final su WHERE o su ORDER BY. Se nombran las columnas una por una, en vez de un * que un
    // día cambie de orden y rompa algo sin avisar.
    private static final String CONSULTA = """
            SELECT m.id_matricula, m.id_estudiante, m.id_seccion, m.id_curso, m.periodo, m.nota, m.estado
            FROM matriculas m
            """;

    @Override
    public List<Matricula> listarTodos() {
        // El periodo es un texto tipo 2026-2, así que al revés queda lo más nuevo arriba.
        return consultar(CONSULTA + " ORDER BY m.periodo DESC, m.id_matricula", new Object[0]);
    }

    // El historial de un estudiante, en todos sus periodos.
    @Override
    public List<Matricula> listarPorEstudiante(int idEstudiante) {
        return consultar(CONSULTA + " WHERE m.id_estudiante = ? ORDER BY m.periodo DESC, m.id_matricula", idEstudiante);
    }

    @Override
    public List<Matricula> listarPorCurso(int idCurso) {
        return consultar(CONSULTA + " WHERE m.id_curso = ? ORDER BY m.periodo DESC, m.id_matricula", idCurso);
    }

    // La nómina del grupo, en el orden en que se fueron inscribiendo.
    @Override
    public List<Matricula> listarPorSeccion(int idSeccion) {
        return consultar(CONSULTA + " WHERE m.id_seccion = ? ORDER BY m.id_matricula", idSeccion);
    }

    // Las matrículas de un estudiante en un periodo, buscándolo por el nombre del periodo (2026-1), no por
    // su número: la columna guarda un texto. Con esta lista el servicio suma los créditos del periodo y
    // descubre si el estudiante ya cursa ese mismo curso en otro grupo.
    @Override
    public List<Matricula> listarPorEstudianteYPeriodo(int idEstudiante, String periodo) {
        return consultar(CONSULTA + " WHERE m.id_estudiante = ? AND m.periodo = ? ORDER BY m.id_matricula",
                idEstudiante, periodo);
    }

    // Cuántos estudiantes llenan hoy el grupo. El servicio le resta el número al cupo máximo, y si no sobra
    // espacio no deja inscribir a otro.
    @Override
    public int contarPorSeccion(int idSeccion) {
        String sql = "SELECT COUNT(*) FROM matriculas WHERE id_seccion = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                // El recuento llega en una única fila con el número; el 0 cubre que no venga nada.
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible contar los matriculados de la sección.", ex);
        }
    }

    // Un estudiante no puede tener dos matrículas en el mismo grupo: la base lo impide con una clave única,
    // así que aquí lo normal es una fila o ninguna. Se saca la primera del listado.
    @Override
    public Matricula leerPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        List<Matricula> encontradas = consultar(
                CONSULTA + " WHERE m.id_estudiante = ? AND m.id_seccion = ?", idEstudiante, idSeccion);
        return encontradas.isEmpty() ? null : encontradas.get(0);
    }

    // Sin esa matrícula, null; el que llama contesta el aviso.
    @Override
    public Matricula leerPorId(int id) {
        List<Matricula> encontradas = consultar(CONSULTA + " WHERE m.id_matricula = ?", id);
        return encontradas.isEmpty() ? null : encontradas.get(0);
    }

    // MySQL numera la fila nueva; el número se pide después de guardar y se copia al objeto, que es como el
    // llamador sabe qué matrícula acaba de crear.
    @Override
    public int registrar(Matricula m) throws SQLException {
        String sql = "INSERT INTO matriculas (id_estudiante, id_seccion, id_curso, periodo, nota, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdSeccion());
            ps.setInt(3, m.getIdCurso());
            ps.setString(4, m.getPeriodo());
            ponerNota(ps, 5, m.getNota());
            ps.setString(6, m.getEstado());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setIdMatricula(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    // Reescribe los datos de la inscripción; el id del final solo dice qué fila hay que cambiar.
    @Override
    public int actualizar(Matricula m) throws SQLException {
        String sql = "UPDATE matriculas SET id_estudiante = ?, id_seccion = ?, id_curso = ?, periodo = ?, "
                + "nota = ?, estado = ? WHERE id_matricula = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdSeccion());
            ps.setInt(3, m.getIdCurso());
            ps.setString(4, m.getPeriodo());
            ponerNota(ps, 5, m.getNota());
            ps.setString(6, m.getEstado());
            ps.setInt(7, m.getIdMatricula());
            return ps.executeUpdate();
        }
    }

    // Retirar una inscripción. Devuelve 1 si la encontraba y 0 si ya no estaba; borrar una matrícula no
    // borra al estudiante ni al grupo.
    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM matriculas WHERE id_matricula = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Sin nota aún se guarda un hueco, no un cero: un cero es una nota presentada y eso cambia si el
    // estudiante aprobó. La propia base rechaza cualquier nota fuera de 0 a 5.
    private static void ponerNota(PreparedStatement ps, int posicion, Double nota) throws SQLException {
        if (nota == null) {
            ps.setNull(posicion, Types.DECIMAL);
        } else {
            ps.setDouble(posicion, nota);
        }
    }

    // El común de los listar: los valores entregan en el orden de los signos de interrogación. Los que
    // son texto van como texto (el periodo) y los demás como número (las ids).
    private List<Matricula> consultar(String sql, Object... parametros) {
        List<Matricula> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                Object p = parametros[i];
                if (p instanceof String texto) {
                    ps.setString(i + 1, texto);
                } else {
                    ps.setInt(i + 1, (Integer) p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException ex) {
            // El fallo sube como excepción: una lista vacía por error se vería como "no hay matrículas".
            throw new IllegalStateException("No fue posible listar las matrículas.", ex);
        }
        return lista;
    }

    // Arma el objeto fila por fila, leyendo cada columna de MySQL por su nombre.
    private Matricula mapear(ResultSet rs) throws SQLException {
        Matricula m = new Matricula();
        m.setIdMatricula(rs.getInt("id_matricula"));
        m.setIdEstudiante(rs.getInt("id_estudiante"));
        m.setIdSeccion(rs.getInt("id_seccion"));
        m.setIdCurso(rs.getInt("id_curso"));
        m.setPeriodo(rs.getString("periodo"));
        double nota = rs.getDouble("nota");
        // Pedir un número de una columna vacía da 0.0; hay que preguntar después si de verdad era hueco.
        m.setNota(rs.wasNull() ? null : nota);
        m.setEstado(rs.getString("estado"));
        return m;
    }
}
