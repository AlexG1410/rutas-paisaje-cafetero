package co.edu.uniquindio.rutas.repository;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Información de la máquina y de la JVM donde se mide (protocolo 6.1). */
@Repository
public class MaquinaRepository {

    public Map<String, String> informacion() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("fecha", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        info.put("procesador", nombreProcesador());
        info.put("nucleos_logicos", String.valueOf(Runtime.getRuntime().availableProcessors()));
        info.put("ram_gb", ramGb());
        info.put("sistema_operativo", System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        info.put("java", System.getProperty("java.version"));
        info.put("jvm", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        info.put("proveedor_java", System.getProperty("java.vendor"));
        info.put("heap_max_mb", String.valueOf(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        info.put("argumentos_jvm",
                String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        return info;
    }

    private static String ramGb() {
        if (ManagementFactory.getOperatingSystemMXBean()
                instanceof com.sun.management.OperatingSystemMXBean os) {
            return String.format(Locale.ROOT, "%.2f", os.getTotalMemorySize() / Math.pow(1024, 3));
        }
        return "desconocida";
    }

    private static String nombreProcesador() {
        String sistema = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        try {
            if (sistema.contains("linux")) {
                for (String linea : Files.readAllLines(Path.of("/proc/cpuinfo"),
                        StandardCharsets.UTF_8)) {
                    if (linea.startsWith("model name")) {
                        return linea.split(":", 2)[1].strip();
                    }
                }
            } else if (sistema.contains("mac")) {
                Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
                return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            } else if (sistema.contains("windows")) {
                String id = System.getenv("PROCESSOR_IDENTIFIER");
                if (id != null) {
                    return id;
                }
            }
        } catch (IOException | RuntimeException e) {
            // se informa como desconocido
        }
        return "desconocido";
    }
}
