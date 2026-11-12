package co.edu.uniquindio.rutas.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** T1.5-a y contratos de archivos de la capa de datos. */
class RepositoriosTest {

    @Test
    void csvConComillasComasYGzip() throws IOException {
        Path dir = Files.createTempDirectory("csv");
        Path ruta = dir.resolve("prueba.csv.gz");
        Csv.escribir(ruta, List.of("id", "nombre"),
                List.of(List.of("a", "Café, \"el mejor\""), List.of("b", "")));
        List<Map<String, String>> filas = Csv.leer(ruta);
        assertEquals(2, filas.size());
        assertEquals("Café, \"el mejor\"", filas.get(0).get("nombre"));
        assertEquals("", filas.get(1).get("nombre"));
    }

    @Test
    void t1_5a_redIdaYVuelta() throws IOException {
        Path dir = Files.createTempDirectory("red");
        Csv.escribir(dir.resolve("nodos.csv.gz"), RedRepository.COLUMNAS_NODOS, List.of(
                List.of(1, 22, 4.5, -75.7, 1.0, 0.0),
                List.of(0, 11, 4.5, -75.7, 0.0, 0.0),
                List.of(2, 33, 4.5, -75.7, 2.0, 0.0)));
        Csv.escribir(dir.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS, List.of(
                List.of(0, 1, 100.0, 36.0, "osm", 10.0, "primary"),
                List.of(1, 2, 200.0, 72.0, "imputada", 10.0, "residential")));
        RedLeida red = new RedRepository(dir).leer();
        assertEquals(3, red.n());
        assertArrayEquals(new long[] {11, 22, 33}, red.osmid());
        assertArrayEquals(new int[] {0, 1}, red.u());
        assertArrayEquals(new double[] {100.0, 200.0}, red.longitudM());
    }

    @Test
    void redRechazaIndicesConHuecos() throws IOException {
        Path dir = Files.createTempDirectory("red");
        Csv.escribir(dir.resolve("nodos.csv.gz"), RedRepository.COLUMNAS_NODOS,
                List.of(List.of(0, 11, 4.5, -75.7, 0.0, 0.0), List.of(5, 22, 4.5, -75.7, 1.0, 0.0)));
        Csv.escribir(dir.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS,
                List.of(List.of(0, 1, 1.0, 1.0, "osm", 1.0, "primary")));
        assertThrows(IllegalStateException.class, () -> new RedRepository(dir).leer());
    }

    @Test
    void matrizIdaYVuelta() throws IOException {
        Path ruta = Files.createTempDirectory("matriz").resolve("m.csv");
        MatrizRepository repo = new MatrizRepository(ruta);
        repo.guardar(List.of("a", "b"), new double[][] {{0, 12.5}, {30.25, 0}});
        MatrizRepository.MatrizLeida leida = repo.leer();
        assertEquals(List.of("a", "b"), leida.ids());
        assertArrayEquals(new double[] {30.25, 0}, leida.matriz()[1]);
    }

    @Test
    void resultadosConservanElOrdenDeColumnas() throws IOException {
        ResultadosRepository repo = new ResultadosRepository(Files.createTempDirectory("res"));
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("k", 5);
        fila.put("tiempo_ms", null);
        fila.put("estado", "no completada");
        repo.guardar("r4/exactas.csv", List.of(fila));
        Map<String, String> leida = repo.leer("r4/exactas.csv").getFirst();
        assertEquals(List.of("k", "tiempo_ms", "estado"), List.copyOf(leida.keySet()));
        assertEquals("", leida.get("tiempo_ms"));
    }

    @Test
    void figurasSoloConNombresSimples() throws IOException {
        ResultadosRepository repo = new ResultadosRepository(Files.createTempDirectory("res"));
        assertThrows(IllegalArgumentException.class, () -> repo.figura("../secreto.png"));
        assertEquals(List.of(), repo.figuras());
    }
}
