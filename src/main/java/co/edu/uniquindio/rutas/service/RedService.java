package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.RedLeida;
import co.edu.uniquindio.rutas.repository.RedRepository;
import co.edu.uniquindio.rutas.repository.ConteosRed;
import co.edu.uniquindio.rutas.model.Grafo;
import org.springframework.stereotype.Service;

/**
 * Carga el {@link Grafo} propio a partir de data/processed (frontera lógica/datos: la capa de
 * datos entrega arreglos y aquí se arma el grafo). Se carga una sola vez y se reutiliza.
 */
@Service
public class RedService {

    private final RedRepository redRepository;
    private Grafo grafo;

    public RedService(RedRepository redRepository) {
        this.redRepository = redRepository;
    }

    public synchronized Grafo grafo() {
        if (grafo == null) {
            if (!redRepository.existe()) {
                throw new IllegalStateException(
                        "Falta data/processed: ejecutar primero python -m preparacion procesar");
            }
            try {
                grafo = construir(redRepository.leer());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Red procesada inválida: " + e.getMessage(), e);
            }
        }
        return grafo;
    }

    public static Grafo construir(RedLeida red) {
        return Grafo.desdeArreglos(red.n(), red.u(), red.v(), red.tiempoS(), red.longitudM(),
                red.osmid(), red.x(), red.y(), red.lat(), red.lon());
    }

    /** Coordenadas UTM de los lugares, exportadas por Python. Se leen de nuevo en cada uso. */
    public java.util.Map<String, double[]> lugaresUtm() {
        return redRepository.leerLugaresUtm();
    }

    /** Valida procedencia de los tamaños utilizados en la estimación de R1.1. */
    public ConteosRed conteosRed() {
        ConteosRed conteos = redRepository.leerConteosRed();
        RedLeida red = redRepository.leer();
        if (conteos.nodosComponente() > conteos.nodosDescargada()
                || conteos.aristasComponente() > conteos.aristasDescargada()
                || conteos.nodosComponente() != red.n()
                || conteos.aristasComponente() != red.u().length) {
            throw new IllegalStateException("Conteos de metadatos incompatibles con la red exportada");
        }
        return conteos;
    }
}
