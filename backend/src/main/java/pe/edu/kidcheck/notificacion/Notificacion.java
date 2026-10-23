package pe.edu.kidcheck.notificacion;

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
import pe.edu.kidcheck.estudiante.Estudiante;
import pe.edu.kidcheck.ingreso.Ingreso;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_apoderado")
    private Apoderado apoderado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ingreso")
    private Ingreso ingreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta")
    private Alerta alerta;

    @Column(nullable = false, length = 10)
    private String canal;

    @Column(nullable = false, length = 40)
    private String plantilla;

    @Column(nullable = false, length = 150)
    private String destino;

    @Column(length = 1000)
    private String cuerpo;

    @Column(name = "estado_entrega", nullable = false, length = 12)
    private String estadoEntrega;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(length = 20)
    private String proveedor;

    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Column(name = "error_codigo", length = 30)
    private String errorCodigo;

    @Column(name = "error_mensaje", length = 255)
    private String errorMensaje;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;
}
