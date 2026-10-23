package pe.edu.kidcheck.apoderado;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.kidcheck.estudiante.Estudiante;

@Entity
@Table(name = "estudiante_apoderado")
@Getter
@Setter
@NoArgsConstructor
public class EstudianteApoderado {

    @EmbeddedId
    private EstudianteApoderadoId id;

    @MapsId("idEstudiante")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @MapsId("idApoderado")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_apoderado", nullable = false)
    private Apoderado apoderado;

    @Column(nullable = false, length = 15)
    private String parentesco;

    @Column(name = "es_principal", nullable = false)
    private byte esPrincipal;

    @Column(name = "autorizado_recoger", nullable = false)
    private byte autorizadoRecoger;

    public boolean principal() {
        return esPrincipal != 0;
    }

    public boolean autorizadoParaRecoger() {
        return autorizadoRecoger != 0;
    }
}
