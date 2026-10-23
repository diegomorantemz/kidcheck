package pe.edu.kidcheck.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI kidcheckOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KidCheck API")
                        .version("0.1.0")
                        .description("""
                                API del sistema de control de ingreso escolar por credencial QR.
                                Autenticación con JWT: usa /api/v1/auth/login y luego el botón Authorize.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .name(ESQUEMA)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
