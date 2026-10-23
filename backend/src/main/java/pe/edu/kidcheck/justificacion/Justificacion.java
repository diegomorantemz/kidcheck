package pe.edu.kidcheck.justificacion;

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
import pe.edu.kidcheck.alerta.Alerta;
import pe.edu.kidcheck.apoderado.Apoderado;

import java.time.LocalDateTime;

@Entity
@Table(name = "justificacion")
@Getter
@Setter
@NoArgsConstructor
public class Justificacion {

    public static final String PENDIENTE = "pendiente";
    public static final String APROBADA = "aprobada";
    public static final String RECHAZADA = "rechazada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_justificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_apoderado", nullable = false)
    private Apoderado apoderado;

    @Column(nullable = false, length = 20)
    private String motivo;

    @Column(length = 500)
    private String detalle;

    @Column(name = "evidencia_url", length = 255)
    private String evidenciaUrl;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "id_usuario_revision")
    private Long usuarioRevisionId;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;
}
