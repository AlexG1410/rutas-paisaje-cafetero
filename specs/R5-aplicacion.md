# R5 — Aplicación demostrativa

Estado: borrador · Responsable: _(asignar)_ · Peso: 5 % · Paquetes reorganizados: 2026-11-10 (sin cambio funcional)

## Objetivo

Aplicación local que muestre los resultados del proyecto. Se evalúa que funcione siguiendo
el README y que muestre los resultados, no el diseño visual.

## Contrato

- Tecnología: **Spring Boot** (REST) + página estática con **Leaflet**
  (`src/main/resources/static/index.html`, `app.js`, `estilos.css`).
- Entrada: `./gradlew bootRun` desde la raíz → http://localhost:8080.
- Endpoints (capa de presentación, `controller`):

| Método y ruta | Controlador | Servicio (capa lógica) | Respuesta |
|---|---|---|---|
| `GET /api/lugares` | `LugaresController` | `CatalogoService.lugares()` | `List<Lugar>` |
| `GET /api/ruta?origen=&destino=` | `LugaresController` | `RutaService.calcular` | `RutaCalculada` |
| `GET /api/envolvente` | `ResultadosController` | `ConsultaResultadosService.tabla("r3/envolvente_red.csv")` | vértices con `lat`, `lon` |
| `POST /api/itinerario` `{partida, atractivos}` | `ItinerarioController` | `ItinerarioService.comparar` | `ComparacionItinerario` |
| `GET /api/resultados?nombre=r2/cociente.csv` | `ResultadosController` | `ConsultaResultadosService.tabla` | filas del CSV |
| `GET /api/figuras`, `GET /api/figuras/{nombre}` | `ResultadosController` | `ConsultaResultadosService` | nombres / PNG |

- Errores (`ManejoErrores`): pendiente → 501, faltan datos → 404, solicitud inválida → 400,
  siempre como `{"error": "..."}` para que la página muestre qué comando falta ejecutar.

## Comportamiento esperado

1. **Ruta** (R5.1): elegir origen y destino entre los lugares del catálogo; `RutaService`
   ubica sus nodos (`lugares_nodos.csv`), ejecuta `Dijkstra.colaPrioridad` EN EL MOMENTO y
   convierte la ruta a `[lat, lon]`; la página la dibuja y muestra el tiempo (min).
2. **Envolvente** (R5.2): polígono de `envolvente_red.csv` sobre el mapa del departamento.
3. **Itinerario** (R5.3): partida (Armenia por defecto) y entre 6 y 8 atractivos (validado en
   el controlador); `ItinerarioService.comparar` devuelve Held-Karp y la heurística con su
   orden, tiempo total de cada uno y la diferencia absoluta y porcentual. La página dibuja los
   dos recorridos.
4. **Resultados** (R5.4): tablas de results/ y las figuras de `results/figuras`.
5. El grafo se carga una sola vez (`RedService` lo guarda).

## Restricciones

- Los controladores no reimplementan algoritmos ni leen archivos: llaman a
  `service` (regla de capas, verificada por `ArquitecturaTest`).
- La página solo consume la API; ningún cálculo en JavaScript.

## Criterios de aceptación

- [ ] Arranca en una máquina limpia siguiendo solo el README.
- [ ] Las cuatro funciones anteriores operan con los datos reales.
- [ ] Docker o URL pública: opcional.
