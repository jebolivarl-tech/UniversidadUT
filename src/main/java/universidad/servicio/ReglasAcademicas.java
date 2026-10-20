package universidad.servicio;

import java.util.List;
import universidad.modelo.Asistencia;

/**
 * Los números que definen cómo se evalúa en la universidad. Cambian una vez por año académico, no
 * una vez por despliegue, así que viven en un solo lugar y no repartidos por los formularios.
 */
public final class ReglasAcademicas {

    /** Por debajo de este porcentaje de asistencia el estudiante pierde el derecho a evaluación. */
    public static final double PORCENTAJE_MINIMO_ASISTENCIA = 0.75;

    /** Con pocos controles aún no se puede sentenciar a nadie por inasistencia. */
    public static final int CONTROLES_MINIMOS_PARA_PERDER_POR_ASISTENCIA = 4;

    /** Tope de créditos que un estudiante puede acumular en un mismo periodo. */
    public static final int CREDITOS_MAXIMOS_POR_PERIODO = 20;

    private ReglasAcademicas() {
    }

    /** La tardanza y la falta justificada no descuentan: solo la falta no justificada. */
    public static boolean esFaltaNoJustificada(String estado) {
        return estado != null && Asistencia.FALTO.equalsIgnoreCase(estado.trim());
    }

    /**
     * Porcentaje de asistencia sobre los controles registrados. Con lista vacía devuelve 1.0 para
     * que "sin clases dictadas todavía" no se lea como "perdió el curso".
     */
    public static double porcentajeDeAsistencia(List<Asistencia> controles) {
        if (controles == null || controles.isEmpty()) {
            return 1.0;
        }
        long faltas = controles.stream().filter(a -> esFaltaNoJustificada(a.getEstado())).count();
        return (controles.size() - faltas) / (double) controles.size();
    }

    public static boolean pierdePorAsistencia(List<Asistencia> controles) {
        if (controles == null || controles.size() < CONTROLES_MINIMOS_PARA_PERDER_POR_ASISTENCIA) {
            return false;
        }
        return porcentajeDeAsistencia(controles) < PORCENTAJE_MINIMO_ASISTENCIA;
    }

    /** Dos decimales, la precisión con la que se publican las notas. */
    public static double redondearNota(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
