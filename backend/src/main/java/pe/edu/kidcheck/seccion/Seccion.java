package pe.edu.kidcheck.seccion;

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

import java.time.LocalTime;

@Entity
@Table(name = "seccion")
@Getter
@Setter
@NoArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoEscolar periodo;

    @Column(nullable = false, length = 10)
    private String grado;

    @Column(nullable = false, length = 5)
    private String letra;

    @Column(name = "hora_limite_ingreso", nullable = false)
    private LocalTime horaLimiteIngreso;

    @Column(name = "tolerancia_minutos", nullable = false)
    private short toleranciaMinutos;

    public String nombre() {
        return grado + "°" + letra;
    }
}
