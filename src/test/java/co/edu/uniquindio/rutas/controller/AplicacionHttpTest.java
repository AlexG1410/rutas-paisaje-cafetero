package co.edu.uniquindio.rutas.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.repository.FilaLugarNodo;
import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import co.edu.uniquindio.rutas.service.CatalogoService;
import co.edu.uniquindio.rutas.service.RedService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

/** HTTP real en puerto aleatorio, con archivos sustituidos por datos sintéticos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacionHttpTest {
    @Value("${local.server.port}") int puerto;
    @MockitoBean CatalogoService catalogo;
    @MockitoBean RedService red;
    @MockitoBean LugaresNodosRepository asociaciones;
    @MockitoBean MatrizRepository matriz;
    @MockitoBean ResultadosRepository resultados;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void preparar() {
        List<Lugar> lugares = new ArrayList<>();
        List<FilaLugarNodo> filas = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        double[][] tiempos = new double[9][9];
        List<double[]> aristas = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String id = i == 0 ? "cab_armenia" : "a" + i;
            ids.add(id);
            lugares.add(new Lugar(id, id, "m", i == 0 ? "cabecera" : "atractivo", 4.5, -75.7,
                    i == 0 ? null : 120));
            filas.add(new FilaLugarNodo(id, i, 0, false, ""));
            for (int j = 0; j < 9; j++) if (i != j) {
                tiempos[i][j] = 60;
                aristas.add(new double[] {i, j, 60});
            }
        }
        when(catalogo.lugares()).thenReturn(lugares);
        int[] u = new int[aristas.size()];
        int[] v = new int[aristas.size()];
        double[] pesos = new double[aristas.size()];
        for (int i = 0; i < aristas.size(); i++) {
            u[i] = (int) aristas.get(i)[0];
            v[i] = (int) aristas.get(i)[1];
            pesos[i] = aristas.get(i)[2];
        }
        double[] lat = new double[9];
        double[] lon = new double[9];
        java.util.Arrays.fill(lat, 4.5);
        java.util.Arrays.fill(lon, -75.7);
        when(red.grafo()).thenReturn(Grafo.desdeArreglos(9, u, v, pesos, pesos, null, null, null, lat, lon));
        when(asociaciones.existe()).thenReturn(true);
        when(asociaciones.leer()).thenReturn(filas);
        when(matriz.leer()).thenReturn(new MatrizRepository.MatrizLeida(ids, tiempos));
        when(resultados.existe("r3/envolvente_red.csv")).thenReturn(true);
        when(resultados.leer("r3/envolvente_red.csv")).thenReturn(List.of(Map.of("lat", "4.5", "lon", "-75.7")));
        when(resultados.existe("r4/retiros.csv")).thenReturn(true);
        when(resultados.leer("r4/retiros.csv")).thenReturn(List.of());
        when(resultados.figuras()).thenReturn(List.of("prueba.png"));
        when(resultados.existe("figuras/prueba.png")).thenReturn(true);
        when(resultados.figura("prueba.png")).thenReturn(new byte[] {(byte) 137, 80, 78, 71});
    }

    private HttpResponse<String> solicitar(String ruta, String cuerpo) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        if (cuerpo != null) peticion.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void error(String ruta, String cuerpo, int codigo) throws Exception {
        var respuesta = solicitar(ruta, cuerpo);
        assertEquals(codigo, respuesta.statusCode(), respuesta.body());
        assertTrue(json.readTree(respuesta.body()).get("error").asString().length() > 0);
    }

    @Test void t5ca_funcionesYRecursos() throws Exception {
        var lugares = solicitar("/api/lugares", null);
        assertEquals(200, lugares.statusCode());
        assertEquals(9, json.readTree(lugares.body()).size());
        var ruta = solicitar("/api/ruta?origen=cab_armenia&destino=a1", null);
        assertEquals(200, ruta.statusCode());
        assertEquals(60, json.readTree(ruta.body()).get("tiempoS").asDouble());
        assertEquals(2, json.readTree(ruta.body()).get("coordenadas").size());
        assertEquals(200, solicitar("/api/envolvente", null).statusCode());
        assertEquals("[]", solicitar("/api/resultados?nombre=r4/retiros.csv", null).body());
        assertEquals(200, solicitar("/api/figuras", null).statusCode());
        var png = solicitar("/api/figuras/prueba.png", null);
        assertEquals(200, png.statusCode());
        assertTrue(png.headers().firstValue("content-type").orElse("").startsWith("image/png"));
        for (String recurso : List.of("/", "/app.js", "/estilos.css")) {
            assertEquals(200, solicitar(recurso, null).statusCode());
        }
    }

    @Test void t5ca_itinerarioSinRetirosYPartidaPorDefecto() throws Exception {
        var respuesta = solicitar("/api/itinerario", "{\"atractivos\":[\"a1\",\"a2\",\"a3\",\"a4\",\"a5\",\"a6\"]}");
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        var dato = json.readTree(respuesta.body());
        assertEquals("cab_armenia", dato.get("optimo").get("idPartida").asString());
        assertEquals(727, dato.get("optimo").get("duracionJornadaMin").asDouble());
        assertEquals(0, dato.get("optimo").get("retirados").size());
        assertEquals(7, dato.get("heuristico").get("paradas").size());
        assertEquals(0, dato.get("diferenciaPct").asDouble());
    }

    @Test void t5cb_entradasInvalidas() throws Exception {
        error("/api/ruta?origen=cab_armenia", null, 400);
        error("/api/ruta?origen=desconocido&destino=a1", null, 400);
        for (String cuerpo : List.of("", "null", "{", "[]", "{}", "{\"atractivos\":null}",
                "{\"atractivos\":[\"a1\",\"a1\",\"a2\",\"a3\",\"a4\",\"a5\"]}",
                "{\"atractivos\":[null,\"a1\",\"a2\",\"a3\",\"a4\",\"a5\"]}",
                "{\"atractivos\":[\"cab_armenia\",\"a1\",\"a2\",\"a3\",\"a4\",\"a5\"]}",
                "{\"atractivos\":[\"a1\",\"a2\",\"a3\",\"a4\",\"a5\",\"a6\",\"a7\",\"a8\",\"a9\"]}")) {
            error("/api/itinerario", cuerpo, 400);
        }
    }

    @Test void t5cc_archivosAusentesYNombresInvalidos() throws Exception {
        error("/api/resultados?nombre=r4/ausente.csv", null, 404);
        error("/api/figuras/ausente.png", null, 404);
        error("/api/resultados?nombre=../secreto.csv", null, 400);
        error("/api/figuras/secreto.csv", null, 400);
        when(asociaciones.existe()).thenReturn(false);
        error("/api/ruta?origen=cab_armenia&destino=a1", null, 404);
    }
}
