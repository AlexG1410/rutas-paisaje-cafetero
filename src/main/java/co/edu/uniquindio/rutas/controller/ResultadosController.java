package co.edu.uniquindio.rutas.controller;

import co.edu.uniquindio.rutas.service.ConsultaResultadosService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R5.2 y R5.4 — Envolvente, tablas y figuras de results/. */
@RestController
@RequestMapping("/api")
public class ResultadosController {

    private final ConsultaResultadosService consultas;

    public ResultadosController(ConsultaResultadosService consultas) {
        this.consultas = consultas;
    }

    /** Vértices de la envolvente de la red (results/r3/envolvente_red.csv). */
    @GetMapping("/envolvente")
    public List<Map<String, String>> envolvente() {
        return consultas.tabla("r3/envolvente_red.csv");
    }

    /** GET /api/resultados?nombre=r2/cociente.csv */
    @GetMapping("/resultados")
    public List<Map<String, String>> tabla(@RequestParam("nombre") String nombre) {
        return consultas.tabla(nombre);
    }

    @GetMapping("/figuras")
    public List<String> figuras() {
        return consultas.figuras();
    }

    @GetMapping(value = "/figuras/{nombre}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] figura(@PathVariable("nombre") String nombre) {
        return consultas.figura(nombre);
    }
}
