package pe.edu.kidcheck.colegio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "colegio")
@Getter
@Setter
@NoArgsConstructor
public class Colegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colegio")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(length = 100)
    private String distrito;

    @Column(nullable = false, length = 15)
    private String zona;

    @Column(nullable = false, length = 15)
    private String conectividad;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
