package universidad.dao;

import java.util.List;
import universidad.modelo.PeriodoAcademico;

/**
 * DAO de la tabla periodos_academicos: los semestres con su estado (ACTIVO, INACTIVO o CERRADO).
 * La matrícula solo funciona contra el periodo que está ACTIVO.
 */
public interface PeriodoAcademicoDAO extends CRUD<PeriodoAcademico> {

    /** Periodos con un estado dado; la pantalla de periodos filtra por aquí. */
    List<PeriodoAcademico> listarPorEstado(String estado);
}
