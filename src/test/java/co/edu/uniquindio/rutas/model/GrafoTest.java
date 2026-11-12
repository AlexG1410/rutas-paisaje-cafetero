package co.edu.uniquindio.rutas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GrafoTest {

    @Test
    void aristasParalelasConservanLaDeMenorTiempo() {
        Grafo g = Grafo.dirigido(2, new double[][] {
            {0, 1, 10.0, 100.0}, {0, 1, 7.0, 120.0}, {1, 0, 3.0, 30.0}});
        assertEquals(2, g.numAristas());
        assertEquals(1, g.vecinos(0).length);
        assertEquals(new Arista(1, 7.0, 120.0), g.vecinos(0)[0]);
    }

    @Test
    void noDirigidoAgregaAmbosSentidos() {
        Grafo g = Grafo.noDirigido(3, new double[][] {{0, 1, 2.0}, {1, 2, 5.0}});
        assertEquals(4, g.numAristas());
        int[] vecinosDe1 = Arrays.stream(g.vecinos(1)).mapToInt(Arista::destino).sorted().toArray();
        assertEquals("[0, 2]", Arrays.toString(vecinosDe1));
    }

    @Test
    void rechazaPesosNegativosYNodosFueraDeRango() {
        assertThrows(IllegalArgumentException.class,
                () -> Grafo.dirigido(2, new double[][] {{0, 1, -1.0}}));
        assertThrows(IllegalArgumentException.class,
                () -> Grafo.dirigido(2, new double[][] {{0, 2, 1.0}}));
    }

    @Test
    void conservaIdentificadoresYCoordenadas() {
        Grafo g = Grafo.desdeArreglos(2, new int[] {0}, new int[] {1}, new double[] {10},
                new double[] {100}, new long[] {111L, 222L}, new double[] {1, 2},
                new double[] {3, 4}, new double[] {4.5, 4.6}, new double[] {-75.7, -75.6});
        assertEquals(222L, g.osmid(1));
        assertEquals(2.0, g.x(1));
        assertEquals(4.6, g.lat(1));
    }
}
