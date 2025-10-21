package unq.desapp.futbol;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
	title = "Advanced Football Analytics API",
	version = "1.0",
	description = """
		An API for advanced football analytics, providing endpoints for:
		- Predicting match outcomes based on historical team performance.
		- Evaluating player performance and identifying patterns.
		- Calculating advanced metrics from various data sources.
		"""
))
@SecurityScheme(
	name = "BearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT",
	in = SecuritySchemeIn.HEADER,
	paramName = "Authorization")
public class FutbolProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FutbolProjectApplication.class, args);
	}

}
