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
import pe.edu.kidcheck.colegio.Colegio;

@Entity
@Table(name = "apoderado")
@Getter
@Setter
@NoArgsConstructor
public class Apoderado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_apoderado")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colegio", nullable = false)
    private Colegio colegio;

    @Column(name = "nombres_apellidos", nullable = false, length = 150)
    private String nombresApellidos;

    @Column(name = "telefono_whatsapp", length = 20)
    private String telefonoWhatsapp;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 10)
    private String estado;
}
