package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.model.ComparacionItinerario;
import co.edu.uniquindio.rutas.model.Itinerario;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.algorithm.HeldKarp;
import co.edu.uniquindio.rutas.algorithm.VecinoMasCercano;
import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.model.ResultadoTsp;
import org.springframework.stereotype.Service;

/**
 * R4.4 y R5.3 — Itinerario de un día (spec: specs/R4-ruta-optima.md, sección R4.4).
 *
 * <p>Ordena los atractivos con Held-Karp sobre la matriz de tiempos de R2, suma
 * desplazamientos y visitas, y si la jornada pasa de {@code Constantes.JORNADA_MIN} retira
 * atractivos con el criterio documentado en la spec y recalcula.
 */
@Service
public class ItinerarioService {

    private final MatrizRepository matrizRepository;
    private final CatalogoService catalogoService;

    public ItinerarioService(MatrizRepository matrizRepository, CatalogoService catalogoService) {
        this.matrizRepository = matrizRepository;
        this.catalogoService = catalogoService;
    }

    /** Itinerario óptimo (Held-Karp) con retiro de atractivos si no cabe en 11 h. */
    public Itinerario planear(String idPartida, List<String> idsAtractivos) {
        Datos datos = preparar(idPartida, idsAtractivos);
        List<String> pendientes = new ArrayList<>(idsAtractivos);
        List<String> retirados = new ArrayList<>();
        Itinerario actual = ordenar(datos, idPartida, pendientes, retirados, false);
        while (actual.duracionJornadaMin() > Constantes.JORNADA_MIN) {
            Itinerario mejor = null;
            int elegido = -1;
            for (int i = 0; i < pendientes.size(); i++) {
                var candidato = new ArrayList<>(pendientes);
                candidato.remove(i);
                Itinerario intento = ordenar(datos, idPartida, candidato, retirados, false);
                if (mejor == null || intento.duracionJornadaMin() < mejor.duracionJornadaMin()) {
                    mejor = intento;
                    elegido = i;
                }
            }
            if (elegido < 0) throw new IllegalStateException("No se pudo reducir la jornada");
            retirados.add(pendientes.remove(elegido));
            actual = ordenar(datos, idPartida, pendientes, retirados, false);
        }
        return actual;
    }

    /** Held-Karp frente a la heurística, SIN retirar atractivos (R5.3). */
    public ComparacionItinerario comparar(String idPartida, List<String> idsAtractivos) {
        Datos datos = preparar(idPartida, idsAtractivos);
        Itinerario optimo = ordenar(datos, idPartida, idsAtractivos, List.of(), false);
        Itinerario voraz = ordenar(datos, idPartida, idsAtractivos, List.of(), true);
        double diferencia = voraz.duracionJornadaMin() - optimo.duracionJornadaMin();
        return new ComparacionItinerario(optimo, voraz, diferencia,
                brecha(optimo.duracionJornadaMin(), voraz.duracionJornadaMin()));
    }

    private record Datos(MatrizRepository.MatrizLeida leida, Map<String, Lugar> catalogo,
                         Map<String, Integer> indices) { }

    private Datos preparar(String partida, List<String> atractivos) {
        if (partida == null || atractivos == null || atractivos.size() > Constantes.ATRACTIVOS_MAX) {
            throw new IllegalArgumentException("Seleccione una partida y hasta ocho atractivos");
        }
        var leida = matrizRepository.leer();
        var lugares = catalogoService.lugares();
        validarDatos(leida, lugares);
        Map<String, Lugar> catalogo = new HashMap<>();
        Map<String, Integer> indices = new HashMap<>();
        lugares.forEach(l -> catalogo.put(l.id(), l));
        for (int i = 0; i < leida.ids().size(); i++) indices.put(leida.ids().get(i), i);
        if (!catalogo.containsKey(partida)) throw new IllegalArgumentException("Partida desconocida: " + partida);
        var usados = new HashSet<String>();
        usados.add(partida);
        for (String id : atractivos) {
            Lugar lugar = catalogo.get(id);
            if (lugar == null || !usados.add(id) || !"atractivo".equals(lugar.categoria())
                    || lugar.duracionVisitaMin() == null || lugar.duracionVisitaMin() <= 0) {
                throw new IllegalArgumentException("Atractivo inválido, repetido o sin duración: " + id);
            }
        }
        return new Datos(leida, catalogo, indices);
    }

    /** Validación de datos en lógica; el repositorio solo entrega los valores leídos. */
    static void validarDatos(MatrizRepository.MatrizLeida leida, List<Lugar> lugares) {
        if (leida == null || leida.ids() == null || leida.ids().isEmpty() || lugares == null
                || leida.matriz() == null || leida.matriz().length != leida.ids().size()) {
            throw new IllegalStateException("Matriz de R2 ausente o incompleta");
        }
        var ids = new HashSet<String>();
        for (Lugar lugar : lugares) {
            if (lugar == null || lugar.id() == null || !ids.add(lugar.id())) {
                throw new IllegalStateException("Catálogo con ids inválidos o repetidos");
            }
        }
        if (leida.ids().stream().anyMatch(java.util.Objects::isNull) || new HashSet<>(leida.ids()).size() != leida.ids().size()
                || !ids.equals(new HashSet<>(leida.ids()))) {
            throw new IllegalStateException("La matriz y el catálogo no tienen los mismos ids únicos");
        }
        for (int i = 0; i < leida.matriz().length; i++) {
            double[] fila = leida.matriz()[i];
            if (fila == null || fila.length != leida.ids().size()) {
                throw new IllegalStateException("Matriz no cuadrada");
            }
            for (int j = 0; j < fila.length; j++) {
                if (!Double.isFinite(fila[j]) || fila[j] < 0 || (i == j && fila[j] != 0)) {
                    throw new IllegalStateException("Matriz con tiempos inválidos");
                }
            }
        }
    }

    static double brecha(double optimo, double heuristico) {
        if (!Double.isFinite(optimo) || !Double.isFinite(heuristico) || optimo < 0
                || heuristico < optimo - 1e-6) throw new IllegalStateException("Costos comparados inválidos");
        if (optimo == 0) {
            if (heuristico == 0) return 0;
            throw new IllegalStateException("Brecha indefinida: óptimo cero y costo voraz positivo");
        }
        double porcentaje = (Math.max(0, heuristico - optimo) / optimo) * 100;
        if (!Double.isFinite(porcentaje)) {
            throw new IllegalStateException("La brecha porcentual no es representable como double finito");
        }
        return porcentaje;
    }

    private static Itinerario ordenar(Datos datos, String partida, List<String> atractivos,
                                      List<String> retirados, boolean voraz) {
        int[] seleccion = new int[atractivos.size() + 1];
        seleccion[0] = datos.indices().get(partida);
        for (int i = 0; i < atractivos.size(); i++) seleccion[i + 1] = datos.indices().get(atractivos.get(i));
        ResultadoTsp resultado = voraz ? VecinoMasCercano.resolver(datos.leida().matriz(), seleccion)
                : HeldKarp.resolver(datos.leida().matriz(), seleccion);
        List<Itinerario.Parada> paradas = new ArrayList<>();
        double visitasMin = 0;
        double desplazamientosMin = 0;
        for (int i = 1; i < resultado.recorrido().length; i++) {
            int anterior = resultado.recorrido()[i - 1];
            int actual = resultado.recorrido()[i];
            Lugar lugar = datos.catalogo().get(datos.leida().ids().get(actual));
            double viajeMin = datos.leida().matriz()[anterior][actual] / 60;
            double visitaMin = i == resultado.recorrido().length - 1 ? 0 : lugar.duracionVisitaMin();
            visitasMin += visitaMin;
            desplazamientosMin += viajeMin;
            paradas.add(new Itinerario.Parada(i, lugar.id(), lugar.nombre(), viajeMin,
                    visitaMin, (visitasMin + desplazamientosMin) / 60));
        }
        return new Itinerario(voraz ? "vecino_mas_cercano" : "held_karp", partida,
                List.copyOf(paradas), visitasMin, desplazamientosMin,
                visitasMin + desplazamientosMin, List.copyOf(retirados));
    }
}
