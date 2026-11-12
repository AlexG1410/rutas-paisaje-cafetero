package co.edu.uniquindio.rutas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada.
 *
 * <ul>
 *   <li>Sin argumentos: levanta la aplicación web (R5) en http://localhost:8080.</li>
 *   <li>Con un comando ({@code ./gradlew bootRun --args='r2'}): ejecuta ese comando sin
 *       servidor web y termina. Ver {@code cli.ComandosCli}.</li>
 * </ul>
 */
@SpringBootApplication
public class RutasApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RutasApplication.class);
        if (args.length > 0 && !args[0].startsWith("-")) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        app.run(args);
    }
}
