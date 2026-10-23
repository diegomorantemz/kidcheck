package pe.edu.kidcheck.dispositivo;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivo")
@Getter
@Setter
@NoArgsConstructor
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_colegio", nullable = false)
    private Colegio colegio;

    @Column(name = "device_uuid", nullable = false, unique = true, length = 36)
    private String deviceUuid;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(length = 100)
    private String ubicacion;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "ultima_sincronizacion")
    private LocalDateTime ultimaSincronizacion;
}
