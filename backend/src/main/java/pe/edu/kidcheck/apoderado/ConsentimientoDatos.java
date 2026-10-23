package pe.edu.kidcheck.apoderado;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimiento_datos")
@Getter
@Setter
@NoArgsConstructor
public class ConsentimientoDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_apoderado", nullable = false)
    private Apoderado apoderado;

    @Column(nullable = false, length = 30)
    private String finalidad;

    @Column(name = "version_texto", nullable = false, length = 20)
    private String versionTexto;

    @Column(nullable = false, length = 12)
    private String estado;

    @Column(name = "fecha_otorgamiento")
    private LocalDateTime fechaOtorgamiento;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(name = "documento_url", length = 255)
    private String documentoUrl;
}
