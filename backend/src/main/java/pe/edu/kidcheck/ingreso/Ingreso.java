package pe.edu.kidcheck.ingreso;

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
import pe.edu.kidcheck.dispositivo.Dispositivo;
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.estudiante.Matricula;
import pe.edu.kidcheck.usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "ingreso")
@Getter
@Setter
@NoArgsConstructor
public class Ingreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingreso")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matricula", nullable = false)
    private Matricula matricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dispositivo")
    private Dispositivo dispositivo;

    @Column(name = "fecha_hora_utc", nullable = false)
    private LocalDateTime fechaHoraUtc;

    @Column(name = "fecha_local", nullable = false)
    private LocalDate fechaLocal;

    @Column(name = "hora_local", nullable = false)
    private LocalTime horaLocal;

    @Column(nullable = false, length = 10)
    private String canal;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(length = 255)
    private String observacion;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
