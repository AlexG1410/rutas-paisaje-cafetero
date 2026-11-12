package co.edu.uniquindio.rutas.controller;

import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.model.RutaCalculada;
import co.edu.uniquindio.rutas.service.CatalogoService;
import co.edu.uniquindio.rutas.service.RutaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R5.1 — Lugares del catálogo y ruta origen–destino. */
@RestController
@RequestMapping("/api")
public class LugaresController {

    private final CatalogoService catalogoService;
    private final RutaService rutaService;

    public LugaresController(CatalogoService catalogoService, RutaService rutaService) {
        this.catalogoService = catalogoService;
        this.rutaService = rutaService;
    }

    @GetMapping("/lugares")
    public List<Lugar> lugares() {
        return catalogoService.lugares();
    }

    /** GET /api/ruta?origen=cab_armenia&amp;destino=atr_valle_cocora */
    @GetMapping("/ruta")
    public RutaCalculada ruta(@RequestParam("origen") String origen,
                              @RequestParam("destino") String destino) {
        return rutaService.calcular(origen, destino);
    }
}
