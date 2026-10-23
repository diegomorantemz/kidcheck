package pe.edu.kidcheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración real: MySQL 8.4 en Testcontainers, Flyway desde cero,
 * seguridad JWT y endpoints críticos. Requiere Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("kidcheck")
            .withUsername("kidcheck")
            .withPassword("kidcheck");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        String url = MYSQL.getJdbcUrl();
        registry.add("spring.datasource.url", () -> url
                + (url.contains("?") ? "&" : "?")
                + "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("kidcheck.jobs.habilitado", () -> "false");
        registry.add("kidcheck.notificaciones.intervalo-ms", () -> "3600000");
        registry.add("logging.level.root", () -> "WARN");
        registry.add("logging.level.pe.edu.kidcheck", () -> "INFO");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Order(1)
    void levantaConFlywayYSalud() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    void loginYPerfil() throws Exception {
        String token = token("admin1");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("admin1"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @Order(3)
    void reportesDiarioYMensualConSemilla() throws Exception {
        String token = token("admin1");

        mvc.perform(get("/api/v1/reportes/diario")
                        .param("fecha", "2026-10-23")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-10-23"))
                .andExpect(jsonPath("$.resumen.total").value(3))
                .andExpect(jsonPath("$.resumen.presentes").value(1))
                .andExpect(jsonPath("$.resumen.ausentes").value(1))
                .andExpect(jsonPath("$.secciones.length()").value(2));

        mvc.perform(get("/api/v1/reportes/mensual")
                        .param("periodo", "2026-10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-10"))
                .andExpect(jsonPath("$.resumen.estudiantes").value(3))
                .andExpect(jsonPath("$.estudiantes.length()").value(3));
    }

    @Test
    @Order(4)
    void roleGatingDeReportesCsvYAuditoria() throws Exception {
        String admin = token("admin1");
        String auxiliar = token("auxiliar1");
        String vigilante = token("vigilante1");

        mvc.perform(get("/api/v1/reportes/diario").header("Authorization", "Bearer " + auxiliar))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/reportes/asistencia.csv")
                        .header("Authorization", "Bearer " + auxiliar))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + auxiliar))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/diario").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/sync/roster").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/reportes/asistencia.csv")
                        .param("periodo", "2026-10")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Codigo,Estudiante,Seccion")));

        mvc.perform(get("/api/v1/auditoria")
                        .param("pagina", "0")
                        .param("tamano", "5")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    @Order(5)
    void justificacionPublicaDeLaSemilla() throws Exception {
        mvc.perform(get("/api/v1/justificaciones/mia").param("token", "just-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante").value("Mateo Silva Cordova"))
                .andExpect(jsonPath("$.seccion").value("5°A"))
                .andExpect(jsonPath("$.yaEnviada").value(true));
    }

    @Test
    @Order(6)
    void syncDeIngresoEsIdempotenteYGuardaObservacion() throws Exception {
        String token = token("vigilante1");
        String clientId = UUID.randomUUID().toString();
        String cuerpo = """
                {
                  "deviceUuid": "11111111-1111-1111-1111-111111111111",
                  "ingresos": [{
                    "clientId": "%s",
                    "idEstudiante": 3,
                    "fechaHoraUtc": "2026-12-01T13:00:00Z",
                    "canal": "manual",
                    "observacion": "cita_medica"
                  }]
                }
                """.formatted(clientId);

        mvc.perform(post("/api/v1/sync/ingresos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[0].status").value("CREADO"))
                .andExpect(jsonPath("$.resultados[0].idIngreso").isNumber());

        mvc.perform(post("/api/v1/sync/ingresos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[0].status").value("DUPLICADO"));

        assertThat(jdbc.queryForObject(
                "select observacion from ingreso where idempotency_key = ?",
                String.class, clientId)).isEqualTo("cita_medica");
    }

    @Test
    @Order(7)
    void configuracionYSeccionesSoloAdmin() throws Exception {
        String admin = token("admin1");
        String vigilante = token("vigilante1");

        mvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.clave == 'umbral_reincidencia')]").exists());

        mvc.perform(get("/api/v1/secciones").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/secciones").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("5°A"));

        mvc.perform(put("/api/v1/secciones/1")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"horaLimiteIngreso":"07:45:00","toleranciaMinutos":30}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toleranciaMinutos").value(30));

        mvc.perform(put("/api/v1/configuracion/umbral_reincidencia")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("3"));

        mvc.perform(get("/api/v1/auditoria")
                        .param("entidad", "seccion")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].entidad").value("seccion"));
    }

    private String token(String usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"%s","password":"KidCheck2026!"}
                                """.formatted(usuario)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode datos = json.readTree(respuesta);
        return datos.get("accessToken").asText();
    }
}
