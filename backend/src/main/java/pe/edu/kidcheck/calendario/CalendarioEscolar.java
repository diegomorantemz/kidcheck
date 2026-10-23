package pe.edu.kidcheck.calendario;

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
import pe.edu.kidcheck.colegio.Colegio;

import java.time.LocalDate;

@Entity
@Table(name = "calendario_escolar")
@Getter
@Setter
@NoArgsConstructor
public class CalendarioEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calendario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colegio", nullable = false)
    private Colegio colegio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "es_laborable", nullable = false)
    private byte esLaborable;

    @Column(length = 100)
    private String descripcion;

    public boolean laborable() {
        return esLaborable != 0;
    }
}
