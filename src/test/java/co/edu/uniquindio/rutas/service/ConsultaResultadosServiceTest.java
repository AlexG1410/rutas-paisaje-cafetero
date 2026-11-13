package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConsultaResultadosServiceTest {
    @TempDir Path carpeta;

    @Test void t5qa_nombresValidosAusentesYNulos() throws Exception {
        var repositorio = new ResultadosRepository(carpeta);
        var servicio = new ConsultaResultadosService(repositorio);
        repositorio.guardar("r4/retiros.csv", List.of("paso"), List.of());
        assertTrue(servicio.tabla("r4/retiros.csv").isEmpty());
        repositorio.guardar("r2/cociente.csv", List.of(Map.of("cociente", 2)));
        assertEquals("2", servicio.tabla("r2/cociente.csv").getFirst().get("cociente"));
        for (String nombre : new String[] {null, "", "../secret.csv", "r2/../../secret.csv", "r2/a.txt"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.tabla(nombre));
        }
        assertTrue(assertThrows(NoSuchElementException.class,
                () -> servicio.tabla("r3/ausente.csv")).getMessage().contains("r3"));
        assertTrue(assertThrows(NoSuchElementException.class,
                () -> servicio.figura("ausente.png")).getMessage().contains("graficas"));
        for (String nombre : new String[] {null, "../secreto.png", "x.csv"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.figura(nombre));
        }
        Files.createDirectories(carpeta.resolve("figuras"));
        Files.write(carpeta.resolve("figuras/prueba.png"), new byte[] {1, 2, 3});
        assertEquals(List.of("prueba.png"), servicio.figuras());
        assertArrayEquals(new byte[] {1, 2, 3}, servicio.figura("prueba.png"));
    }
}
