package universidad.dao;

import java.util.List;
import universidad.modelo.Seccion;

/**
 * DAO de la tabla secciones: un grupo concreto de un curso, con profesor, periodo y cupo
 * máximo. Aquí se decide si un estudiante puede o no inscribirse.
 */
public interface SeccionDAO extends CRUD<Seccion> {

    List<Seccion> listarPorProfesor(int idProfesor);

    List<Seccion> listarPorPeriodo(int idPeriodo);

    /** Secciones de los cursos donde el estudiante tiene matrícula. */
    List<Seccion> listarPorEstudiante(int idEstudiante);

    /**
     * Secciones que el estudiante todavía puede inscribir hoy: curso de su carrera, periodo
     * ACTIVO, sin matrícula previa en ese curso y con cupo libre. El formulario de matrícula
     * muestra justo esta lista; el tope de créditos lo filtra el servicio, no este SQL.
     */
    List<Seccion> listarInscribiblesPara(int idEstudiante);
}
