package co.edu.uniquindio.rutas.cli;

import co.edu.uniquindio.rutas.service.CatalogoService;
import co.edu.uniquindio.rutas.service.EntornoService;
import co.edu.uniquindio.rutas.service.ExperimentoR1Service;
import co.edu.uniquindio.rutas.service.ExperimentoR2Service;
import co.edu.uniquindio.rutas.service.ExperimentoR3Service;
import co.edu.uniquindio.rutas.service.ExperimentoR4Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Comandos de línea (capa de presentación): {@code ./gradlew bootRun --args='<comando>'}.
 * Es la única parte del programa, junto con la web, que imprime. Sin argumentos no hace nada
 * y se levanta la aplicación web.
 */
@Component
public class ComandosCli implements CommandLineRunner {

    private record Comando(String ayuda, Runnable accion) {
    }

    private final Map<String, Comando> comandos = new LinkedHashMap<>();

    public ComandosCli(EntornoService entorno, CatalogoService catalogo, ExperimentoR1Service r1,
                       ExperimentoR2Service r2, ExperimentoR3Service r3, ExperimentoR4Service r4) {
        comandos.put("maquina", new Comando("Guarda results/maquina_java.csv (protocolo 6.1)",
                () -> entorno.registrar().forEach((k, v) -> System.out.println(k + ": " + v))));
        comandos.put("catalogo", new Comando("Valida data/catalogo/lugares.csv",
                () -> mostrarValidacion(catalogo.validarArchivo())));
        comandos.put("r1", new Comando("R1.1 memoria estimada y R1.4 lugares -> nodos", () -> {
            r1.ejecutar();
            System.out.println("R1.4: asociaciones y lugares lejanos exportados. "
                    + "Ejecutar python -m preparacion verificar-lugares. "
                    + "R1.1: memoria estimada exportada (no consumo real JVM). Mediciones R1.3 pendientes.");
        }));
        comandos.put("r2", new Comando("Dijkstra: mediciones, cociente y matriz de tiempos",
                r2::ejecutar));
        comandos.put("r3", new Comando("Envolvente convexa propia (SciPy se mide en python/)",
                r3::ejecutar));
        comandos.put("r4", new Comando("Fuerza bruta, Held-Karp, heurística e itinerario",
                r4::ejecutar));
    }

    @Override
    public void run(String... args) {
        if (args.length == 0 || args[0].startsWith("-")) {
            return; // modo web
        }
        Comando comando = comandos.get(args[0]);
        if (comando == null) {
            System.out.println("Comando desconocido: " + args[0]);
            mostrarAyuda();
            System.exit(1);
        }
        try {
            comando.accion().run();
        } catch (UnsupportedOperationException pendiente) {
            System.out.println("Pendiente de implementar: " + pendiente.getMessage());
            System.exit(2);
        } catch (SalidaConError e) {
            System.exit(1);
        } catch (IllegalStateException faltanDatos) {
            System.out.println("No se puede ejecutar '" + args[0] + "': " + faltanDatos.getMessage());
            System.exit(1);
        }
    }

    private void mostrarValidacion(List<String> errores) {
        if (errores.isEmpty()) {
            System.out.println("Catálogo válido.");
            return;
        }
        System.out.println("Catálogo con problemas:");
        errores.forEach(e -> System.out.println("  - " + e));
        throw new SalidaConError();
    }

    private void mostrarAyuda() {
        System.out.println("Uso: ./gradlew bootRun --args='<comando>'");
        comandos.forEach((nombre, c) -> System.out.printf("  %-10s %s%n", nombre, c.ayuda()));
    }

    /** Señal interna para terminar con código 1 después de mostrar un error. */
    private static final class SalidaConError extends RuntimeException {
    }
}
