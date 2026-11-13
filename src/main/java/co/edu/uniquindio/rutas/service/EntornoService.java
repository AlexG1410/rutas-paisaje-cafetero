package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.MaquinaRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Protocolo 6.1: registra la máquina y la JVM donde se mide en results/maquina_java.csv
 * (columnas clave, valor). python/ guarda results/maquina_python.csv para R1.3 y SciPy.
 */
@Service
public class EntornoService {

    public static final String ARCHIVO = "maquina_java.csv";

    private final MaquinaRepository maquina;
    private final ResultadosRepository resultados;

    public EntornoService(MaquinaRepository maquina, ResultadosRepository resultados) {
        this.maquina = maquina;
        this.resultados = resultados;
    }

    public Map<String, String> registrar() {
        return registrarEn(ARCHIVO);
    }

    Map<String, String> registrarEn(String nombre) {
        Map<String, String> info = maquina.informacion();
        List<Map<String, String>> filas = new ArrayList<>();
        info.forEach((clave, valor) -> {
            Map<String, String> fila = new LinkedHashMap<>();
            fila.put("clave", clave);
            fila.put("valor", valor);
            filas.add(fila);
        });
        resultados.guardar(nombre, filas);
        return info;
    }
}
