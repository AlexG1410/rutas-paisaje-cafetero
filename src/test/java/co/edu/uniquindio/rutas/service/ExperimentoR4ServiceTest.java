package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.algorithm.HeldKarp;
import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.model.ResultadoTsp;
import co.edu.uniquindio.rutas.repository.*;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperimentoR4ServiceTest {
    @TempDir Path carpeta;
    private static final List<String> ATRACTIVOS = List.of("atr_museo_oro_quimbaya", "atr_jardin_botanico",
            "atr_valle_cocora", "atr_colina_iluminada", "atr_bejuco_canasto", "atr_parque_vida",
            "atr_calle_real", "atr_alto_cruz");

    private class ExperimentoPrueba extends ExperimentoR4Service {
        final List<String> llamadas = new ArrayList<>();
        int corteBf;
        int corteHk;
        boolean corteVerificacion;
        boolean corrupto;
        ExperimentoPrueba(MatrizRepository matriz, CatalogoService catalogo) {
            super(matriz, catalogo, new ItinerarioService(matriz, catalogo),
                    new ResultadosRepository(carpeta),
                    new EntornoService(new MaquinaRepository(), new ResultadosRepository(carpeta)));
        }
        @Override Medicion medir(InstanciaTsp instancia, boolean fuerzaBruta, boolean verificacion) {
            int k = instancia.lugares().length - 1;
            llamadas.add(k + ":" + fuerzaBruta + ":" + verificacion);
            if ((fuerzaBruta && k == corteBf && verificacion == corteVerificacion)
                    || (!fuerzaBruta && k == corteHk)) return Medicion.noCompletada(List.of());
            var resultado = HeldKarp.resolver(instancia.matriz(), instancia.lugares());
            if (corrupto && !verificacion) resultado = new ResultadoTsp(-1, new int[] {0});
            return new Medicion(List.of(1.0), true, resultado);
        }
    }

    private ExperimentoPrueba experimento() {
        List<String> ids = new ArrayList<>(List.of("cab_armenia"));
        ids.addAll(ATRACTIVOS);
        for (int i = 0; i < 6; i++) ids.add("otro" + i);
        List<Lugar> lugares = ids.stream().map(id -> new Lugar(id, id, "m",
                ATRACTIVOS.contains(id) ? "atractivo" : "cabecera", 4.0, -75.0,
                ATRACTIVOS.contains(id) ? 120 : null)).toList();
        double[][] matriz = new double[ids.size()][ids.size()];
        for (int i = 0; i < ids.size(); i++) for (int j = 0; j < ids.size(); j++) {
            matriz[i][j] = i == j ? 0 : 60 + (i * 17 + j * 7) % 53;
        }
        var repositorio = new MatrizRepository() {
            @Override public MatrizLeida leer() { return new MatrizLeida(ids, matriz); }
        };
        var catalogo = new CatalogoService(null) {
            @Override public List<Lugar> lugares() { return lugares; }
        };
        return new ExperimentoPrueba(repositorio, catalogo);
    }

    @Test void t4ea_exportacionReproducibleYTotales() throws Exception {
        var e = experimento();
        e.ejecutar();
        assertEquals(24, e.llamadas.size());
        var exactas = Csv.leer(carpeta.resolve("r4/exactas.csv"));
        assertEquals(8, exactas.size());
        assertTrue(exactas.stream().allMatch(f -> f.get("coinciden").equals("true")));
        assertEquals(28, Csv.leer(carpeta.resolve("r4/instancias.csv")).size());
        assertEquals(20, Csv.leer(carpeta.resolve("r4/calidad_detalle.csv")).size());
        assertEquals(4, Csv.leer(carpeta.resolve("r4/calidad_heuristica.csv")).size());
        var resumen = Csv.leer(carpeta.resolve("r4/itinerario_resumen.csv")).getFirst();
        assertTrue(Double.parseDouble(resumen.get("duracion_jornada_min")) <= 660);
        assertFalse(resumen.get("retirados").isEmpty());
        assertEquals(8, resumen.get("atractivos_iniciales").split(";").length);
        assertTrue(Files.exists(carpeta.resolve("r4/maquina_java.csv")));
        byte[] antes = Files.readAllBytes(carpeta.resolve("r4/instancias.csv"));
        e.ejecutar();
        assertArrayEquals(antes, Files.readAllBytes(carpeta.resolve("r4/instancias.csv")));
        var memoria = Csv.leer(carpeta.resolve("r4/memoria_held_karp.csv")).getLast();
        assertEquals("49152", memoria.get("entradas"));
        assertEquals("589824", memoria.get("bytes_estimados"));
        assertEquals("18.0", Csv.leer(carpeta.resolve("r4/voraz_vs_optimo.csv")).getFirst().get("costo_optimo_s"));
    }

    @Test void t4eb_timeoutSoloCortaMetodoAfectado() {
        for (boolean previa : List.of(true, false)) {
            var e = experimento();
            e.corteBf = 6;
            e.corteVerificacion = previa;
            e.corteHk = 7;
            e.ejecutar();
            assertFalse(e.llamadas.stream().anyMatch(s -> s.matches("(7|8|9|10|11|12):true:.*")));
            assertFalse(e.llamadas.stream().anyMatch(s -> s.matches("(8|9|10|11|12):false:.*")));
            var filas = Csv.leer(carpeta.resolve("r4/exactas.csv"));
            assertEquals(previa ? "verificacion" : "medicion", filas.get(1).get("fase_fuerza_bruta"));
            for (var fila : filas.subList(1, filas.size())) {
                assertEquals("no completada", fila.get("estado_fuerza_bruta"));
                assertTrue(fila.get("tiempo_fuerza_bruta_ms").isEmpty());
                assertTrue(fila.get("coinciden").isEmpty());
            }
        }
    }

    @Test void t4eb_resultadoInvalidoNoSobrescribe() throws Exception {
        var e = experimento();
        Path salida = carpeta.resolve("r4/exactas.csv");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, "anterior");
        e.corrupto = true;
        assertThrows(IllegalStateException.class, e::ejecutar);
        assertEquals("anterior", Files.readString(salida));
    }

    @Test void t4ea_adaptadoresConJVMReal() {
        var instancia = new InstanciaTsp(new double[][] {{0, 1, 9}, {9, 0, 1}, {1, 9, 0}}, new int[] {0, 1, 2});
        for (var tarea : List.of(TareasTsp.FuerzaBruta.class, TareasTsp.HeldKarpTarea.class, TareasTsp.Heuristica.class)) {
            var m = Medidor.medirConLimite(tarea, instancia, 1, 0, 60, 30);
            assertTrue(m.completada());
            assertEquals(3, ((ResultadoTsp) m.resultado()).costoS());
        }
    }
}
