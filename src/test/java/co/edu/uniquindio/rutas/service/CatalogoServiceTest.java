package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.rutas.repository.CatalogoRepository;
import co.edu.uniquindio.rutas.repository.FilaCatalogo;
import co.edu.uniquindio.rutas.model.Punto;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogoServiceTest {

    private static List<FilaCatalogo> catalogoValido() {
        List<FilaCatalogo> filas = new ArrayList<>();
        String[] categorias = {"cabecera", "atractivo", "finca_hotel", "acopio_trilladora"};
        int[] cantidades = {12, 15, 8, 5};
        for (int c = 0; c < categorias.length; c++) {
            for (int i = 0; i < cantidades[c]; i++) {
                boolean atractivo = categorias[c].equals("atractivo");
                filas.add(new FilaCatalogo(categorias[c] + "_" + i, "x", "Armenia", categorias[c],
                        4.5, -75.7, atractivo ? 60 : null, atractivo ? "criterio" : null, "mapa",
                        "si", null));
            }
        }
        return filas;
    }

    @Test
    void aceptaUnCatalogoCompleto() {
        assertEquals(List.of(), CatalogoService.validar(catalogoValido()));
    }

    @Test
    void detectaProblemas() {
        List<FilaCatalogo> filas = new ArrayList<>(catalogoValido());
        FilaCatalogo f0 = filas.get(0);
        filas.set(0, new FilaCatalogo(f0.id(), f0.nombre(), f0.municipio(), f0.categoria(), -75.7,
                4.5, null, null, "mapa", "si", null)); // lat/lon invertidas
        FilaCatalogo f12 = filas.get(12);
        filas.set(12, new FilaCatalogo(f12.id(), f12.nombre(), f12.municipio(), f12.categoria(),
                4.5, -75.7, null, "criterio", "mapa", "si", null)); // atractivo sin duración
        filas.removeIf(f -> f.categoria().equals("acopio_trilladora"));
        String errores = String.join(" | ", CatalogoService.validar(filas));
        assertTrue(errores.contains("fuera del Quindío"), errores);
        assertTrue(errores.contains("sin duración"), errores);
        assertTrue(errores.contains("acopio_trilladora"), errores);
    }

    @Test
    void laPlantillaDelRepositorioTieneLasColumnasYArmenia() {
        CatalogoRepository repo = new CatalogoRepository(Path.of("data/catalogo/lugares.csv"));
        assertEquals(CatalogoRepository.COLUMNAS, repo.columnas());
        assertTrue(repo.leer().stream().anyMatch(f -> f.id().equals("cab_armenia")));
    }

    @Test
    void t3h_redondeoHaciaArribaYSinRepetidos() {
        List<Punto> puntos = ExperimentoR3Service.redondearYDeduplicar(
                new double[] {0.5, 1.49, 1.5}, new double[] {2.5, 0, 0});
        assertEquals(List.of(new Punto(1, 3), new Punto(1, 0), new Punto(2, 0)), puntos);
        List<Punto> repetidos = ExperimentoR3Service.redondearYDeduplicar(
                new double[] {1.2, 0.8, 5.0}, new double[] {3.0, 3.1, 1.0});
        assertEquals(List.of(new Punto(1, 3), new Punto(5, 1)), repetidos);
    }
}
