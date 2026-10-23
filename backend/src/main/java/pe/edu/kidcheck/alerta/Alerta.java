package pe.edu.kidcheck.alerta;

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
import pe.edu.kidcheck.asistencia.AsistenciaDia;
import pe.edu.kidcheck.estudiante.Estudiante;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    public static final String INASISTENCIA = "inasistencia";
    public static final String REINCIDENCIA = "reincidencia";
    public static final String TARDANZA = "tardanza";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_asistencia")
    private AsistenciaDia asistencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(nullable = false, length = 15)
    private String tipo;

    private Short umbral;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "id_usuario_atencion")
    private Long usuarioAtencionId;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;
}
