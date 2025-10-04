package unq.desapp.futbol;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Futbol API", version = "1.0", description = "API para obtener información de jugadores de fútbol."))
public class FutbolProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FutbolProjectApplication.class, args);
	}

}
