# R5 — Aplicación demostrativa

Estado: implementada y verificada localmente, con documentación y capturas (2026-11-12).
118 pruebas Java y 176 Python; prueba de Edge con datos reales aprobada.
Pendiente de entrega: reproducir en una segunda máquina limpia. Peso: 5 %.

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

## Precisiones del bloque aprobado

- No cambian endpoints, DTO ni CSV de R1–R4. `RutaService` añade `CatalogoService`
  a su constructor para distinguir ids desconocidos (400) de asociaciones faltantes (404).
  Exige ids no vacíos y conocidos, asociaciones únicas con índices en rango y coordenadas
  geográficas válidas. Una asociación faltante indica ejecutar r1; red ausente, procesar.
  El grafo se obtiene de `RedService.grafo()`, con caché sincronizada existente. Asociaciones
  se leen en cada solicitud. Camino inalcanzable -> 404 explicativo, nunca Infinity en JSON.
  Origen igual destino o dos lugares asociados al mismo nodo -> costo cero y una coordenada.
- Las coordenadas siguen los nodos del camino de Dijkstra, no segmentos directos entre
  lugares turísticos. Los CSV no conservan la geometría curva completa de cada arista:
  el mapa interpola entre nodos y lo advierte. No se agregan accesos artificiales desde
  los lugares al nodo vial ni se mueven coordenadas del catálogo.
- Para dibujar itinerarios, la página solicita `/api/ruta` por cada par consecutivo
  devuelto por el servidor, incluido salida/regreso; reutiliza pares dentro de esa
  comparación. Solo une coordenadas y presenta unidades, no optimiza rutas ni recalcula
  costos. Tablas usan tiempos de la matriz R2 redondeada a 3 decimales; geometría viene
  de Dijkstra en vivo. No se reemplazan los totales R4 por nuevas sumas del navegador.
- `/api/itinerario` conserva 6..8 atractivos. Partida omitida/vacía -> Armenia; ids nulos,
  repetidos o desconocidos, partida entre visitas y categorías no válidas -> 400.
  Compara la MISMA selección sin retiros. La página muestra duración, visitas,
  desplazamientos, diferencia, leyenda y aviso si alguna jornada supera 660 min.
  Los retiros del ejemplo R4 se consultan como resultados ya generados.
- Errores de JSON (incluido cuerpo ausente/null) y parámetros requeridos ausentes ->
  400 con `error`; tabla/figura ausente -> 404 con comando, nombre inválido/null o
  traversal -> 400. No exponer rutas arbitrarias: conservar lista de patrones permitidos.
  Errores de lectura de archivos -> 404 explicativo, no traza técnica en la página.
- Frontend: textos externos se insertan como texto o HTML escapado; no ejecutar contenido
  del catálogo/CSV/errores. Controles con carga y error, ajuste de mapa al recorrido,
  marcadores/leyenda, diseño adaptable, tablas desplazables. Solicitudes anteriores no
  pueden sobrescribir el panel tras cambiar de pestaña o iniciar otra operación.
  Si falla Leaflet/CDN, se informa y las tablas/API siguen utilizables; mapa base requiere
  internet. Sin dependencias nuevas ni herramientas de rutas externas.
- Verificación de navegador opcional para desarrollo: `node src/test/javascript/r5-browser.cjs`
  con aplicación ya activa, Node 22+ y Edge local en Windows. Usa CDP y módulos estándar,
  sin instalar paquetes; perfil temporal propio bajo `%LOCALAPPDATA%/Temp/opencode`.
  Capturas en `build/r5/` (no resultados experimentales); las revisadas pueden acompañar
  el capítulo 6. Node/Edge no son requisitos para ejecutar la aplicación.

## Casos de prueba

| ID | Entrada/acción | Resultado esperado |
|---|---|---|
| T5.R-a | grafo dirigido sintético y asociaciones no consecutivas | camino mínimo, costo y lat/lon en orden; regreso distinto |
| T5.R-b | origen=destino / dos ids mismo nodo | cero, una coordenada |
| T5.R-c | ids nulos/vacíos/desconocidos | IllegalArgumentException |
| T5.R-d | archivo/asociación ausente, duplicada, índice o coordenadas inválidos | error de datos explícito |
| T5.R-e | destino inalcanzable | error sin costo infinito |
| T5.R-f | dos llamadas concurrentes a caché RedService | una lectura, mismo grafo |
| T5.C-a | GET lugares/ruta/envolvente/tablas/PNG y POST itinerario | JSON/PNG y orden/unidades correctos vía HTTP |
| T5.C-b | selección fuera de 6..8, null, JSON malformado, parámetros ausentes | HTTP 400 y campo error |
| T5.C-c | tabla/figura ausente o nombre traversal | HTTP 404/400 con error |
| T5.Q-a | consultas de tablas/figuras y nombres nulos/inválidos | archivos permitidos o error explicativo sin acceso arbitrario |
| T5.UI-a | cuatro funciones con datos reales en navegador | rutas de nodos, casco, ambos itinerarios y resultados visibles |
| T5.UI-b | error API, selección inválida, cambio rápido de pestaña | error legible, sin resultado obsoleto ni excepción JS |
| T5.UI-c | ancho móvil y CDN ausente | controles/tablas usables; aviso si falta mapa |

## Criterios de aceptación

- [ ] Arranca en una máquina limpia siguiendo solo el README (validación externa pendiente).
- [x] Las cuatro funciones anteriores operan con los datos reales.
- [x] Casos T5.R, T5.C y T5.Q aprobados; T5.UI comprobados con Edge headless y capturas.
- [ ] Docker o URL pública: opcional.
