package universidad.dao;

import java.util.List;
import universidad.modelo.PeriodoAcademico;

public interface PeriodoAcademicoDAO extends CRUD<PeriodoAcademico> {

    List<PeriodoAcademico> listarPorEstado(String estado);
}
