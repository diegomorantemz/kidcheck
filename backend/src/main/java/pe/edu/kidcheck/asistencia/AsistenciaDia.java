package pe.edu.kidcheck.asistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.ingreso.Ingreso;
import pe.edu.kidcheck.seccion.Seccion;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencia_dia")
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaDia {

    public static final String PRESENTE = "presente";
    public static final String TARDE = "tarde";
    public static final String AUSENTE = "ausente";
    public static final String JUSTIFICADO = "justificado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seccion", nullable = false)
    private Seccion seccion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 12)
    private String estado;

    @Column(name = "minutos_tarde", nullable = false)
    private short minutosTarde;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ingreso")
    private Ingreso ingreso;

    @Column(nullable = false, length = 10)
    private String origen;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
