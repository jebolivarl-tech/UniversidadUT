package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Curso;

/**
 * DAO de los cursos. Un curso guarda nombre, créditos y dos referencias numéricas: la carrera a la que
 * pertenece y el profesor que lo administra. Se anotan los números y no los nombres para que, al cambiar
 * algo, ningún curso se quede con el nombre viejo escrito encima.
 * Las conexiones se piden al pool de conexiones y se devuelven al cerrar cada bloque. Las consultas SQL
 * van con signos de interrogación y los valores se entregan aparte, así MySQL no los confunde con una orden.
 */
public class CursoDAOImpl implements CursoDAO {

    @Override
    public List<Curso> listarTodos() {
        return consultar("SELECT * FROM cursos ORDER BY nombre", null);
    }

    // Los cursos que ofrece una carrera.
    @Override
    public List<Curso> listarPorCarrera(int idCarrera) {
        return consultar("SELECT * FROM cursos WHERE id_carrera = ? ORDER BY nombre", idCarrera);
    }

    // Los cursos a cargo de un profesor.
    @Override
    public List<Curso> listarPorProfesor(int idProfesor) {
        return consultar("SELECT * FROM cursos WHERE id_profesor = ? ORDER BY nombre", idProfesor);
    }

    @Override
    public Curso leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM cursos WHERE id_curso = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Sin filas devueltas no hay curso: se entrega null, no un objeto a medias.
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el curso.", ex);
        }
    }

    // MySQL pone el número del curso; se pide justo después de guardar y se copia al objeto para que la
    // pantalla pueda seguir editando esa misma fila.
    @Override
    public int registrar(Curso c) throws SQLException {
        String sql = "INSERT INTO cursos (nombre, creditos, id_carrera, id_profesor) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getCreditos());
            ps.setInt(3, c.getIdCarrera());
            ps.setInt(4, c.getIdProfesor());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCurso(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    // Un curso con créditos fuera de 1 a 10 lo rechaza la propia base, que lleva esa comprobación escrita
    // en la tabla; el reparto de carga por periodo es lo que mira el servicio.
    @Override
    public int actualizar(Curso c) throws SQLException {
        String sql = "UPDATE cursos SET nombre = ?, creditos = ?, id_carrera = ?, id_profesor = ? WHERE id_curso = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getCreditos());
            ps.setInt(3, c.getIdCarrera());
            ps.setInt(4, c.getIdProfesor());
            ps.setInt(5, c.getIdCurso());
            return ps.executeUpdate();
        }
    }

    // La base encadenó los grupos al curso, así que los grupos vacíos se van con él. Si alguno ya tiene
    // estudiantes inscritos, MySQL no deja borrar nada: primero hay que retirar esas matrículas.
    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM cursos WHERE id_curso = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Compartido por los tres listar: con null no hay nada que entregar; con número se coloca en el
    // primer hueco de la consulta.
    private List<Curso> consultar(String sql, Integer parametro) {
        List<Curso> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar los cursos.", ex);
        }
        return lista;
    }

    // Copia la fila que llegó de MySQL a un objeto Curso, con los nombres exactos de las columnas.
    private Curso mapear(ResultSet rs) throws SQLException {
        Curso c = new Curso();
        c.setIdCurso(rs.getInt("id_curso"));
        c.setNombre(rs.getString("nombre"));
        c.setCreditos(rs.getInt("creditos"));
        c.setIdCarrera(rs.getInt("id_carrera"));
        c.setIdProfesor(rs.getInt("id_profesor"));
        return c;
    }
}
