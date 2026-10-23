package pe.edu.kidcheck.sync.dto;

import java.util.List;

public record SyncIngresosResponse(List<Resultado> resultados) {

    public record Resultado(
            String clientId,
            String status,
            Long idIngreso,
            String horaLocal,
            String codigo,
            String mensaje
    ) {
        public static Resultado creado(String clientId, Long idIngreso, String horaLocal) {
            return new Resultado(clientId, "CREADO", idIngreso, horaLocal, null, "Ingreso registrado");
        }

        public static Resultado duplicado(String clientId, Long idIngreso, String horaLocal) {
            return new Resultado(clientId, "DUPLICADO", idIngreso, horaLocal,
                    "INGRESO_YA_REGISTRADO", "El ingreso ya estaba registrado");
        }

        public static Resultado error(String clientId, String codigo, String mensaje) {
            return new Resultado(clientId, "ERROR", null, null, codigo, mensaje);
        }
    }
}
