package universidad.servicio;

import universidad.dao.CursoDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.ProfesorDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;

/** Carga las referencias que declara un formulario, con el mensaje que la API debe devolver. */
final class Referencias {

    private Referencias() {
    }

    static Estudiante estudiante(EstudianteDAO dao, int id) {
        Estudiante e = dao.leerPorId(id);
        if (e == null) {
            throw ReglaVioladaException.deReferencia("No se encontró el estudiante #" + id + ".");
        }
        return e;
    }

    static Curso curso(CursoDAO dao, int id) {
        Curso c = dao.leerPorId(id);
        if (c == null) {
            throw ReglaVioladaException.deReferencia("No se encontró el curso #" + id + ".");
        }
        return c;
    }

    static Profesor profesor(ProfesorDAO dao, int id) {
        Profesor p = dao.leerPorId(id);
        if (p == null) {
            throw ReglaVioladaException.deReferencia("No se encontró el profesor #" + id + ".");
        }
        return p;
    }

    static PeriodoAcademico periodo(PeriodoAcademicoDAO dao, int id) {
        PeriodoAcademico p = dao.leerPorId(id);
        if (p == null) {
            throw ReglaVioladaException.deReferencia("No se encontró el periodo #" + id + ".");
        }
        return p;
    }

    static Seccion seccion(SeccionDAO dao, int id) {
        Seccion s = dao.leerPorId(id);
        if (s == null) {
            throw ReglaVioladaException.deReferencia("No se encontró la sección #" + id + ".");
        }
        return s;
    }
}
