# R1 — Modelo de la red vial

Estado: borrador · Responsable: _(asignar)_ · Peso: 15 % · Paquetes reorganizados: 2026-11-10 (sin cambio funcional)

Avance parcial: búsqueda lineal de R1.4 implementada y verificada con T1.4-a a T1.4-f
(2026-11-10): 6/6 casos aprobados. Infraestructura y arquitectura más R1.4: 32/32 aprobadas.
La integración del catálogo real y el contraste OSMnx se completaron después (véase
actualización de R1.4 abajo); otros pasos de R1 siguen pendientes.
Descarga: configuración aprobada el 2026-11-10, implementada y con T1.D-a a T1.D-i
aprobados (15 comprobaciones parametrizadas; suite Python 20/20). Adquisición real
completada y GraphML releído: estructura y metadatos válidos. Segunda ejecución comprobada
sin peticiones HTTP y sin cambios de contenido (SHA-256) ni fecha de modificación.
R1.2: política aprobada e implementada; T1.2-a a T1.2-i verificados con 36 comprobaciones
(suite Python 56/56). Pesos verificados en memoria sobre todas las aristas de la red real;
GraphML original intacto. La exportación de pesos y porcentaje OSM sigue pendiente.
Actualización R1.3/R1.5: procesamiento y exportación implementados, ejecutados sobre la
red real y contrastados con Kosaraju. Java lee los CSV y construye `Grafo`; los pesos y
el porcentaje OSM ya se exportan. Suite Python entonces 65/65. En esa etapa inicial
`lugares_utm.csv` solo tenía cabecera; el catálogo se completó posteriormente.
Mediciones R1.3 pendientes.
Catálogo: el usuario incorporó los 40 lugares propuestos (12/15/8/5) con coordenadas,
duraciones y verificación en mapa declarada por el grupo. `bootRun --args=catalogo`
aprueba su validación estructural. Revisión preliminar de distancia a nodos detecta casos
>500 m revisados con observaciones del grupo. La sede principal de Quimbaya se sustituye
por compra de café Pijao por indicación del grupo. El 2026-11-11 el integrante eligió
un nuevo punto para Peñas Blancas y decidió conservar la finca El Agrado, sin adoptar
las alternativas anteriores. Entrada exacta de recepción en El Agrado no confirmada:
se conserva como limitación explícita, no como tarea bloqueante de selección de pin.
No equivale a una certificación independiente de las entradas por la IA.
`lugares_utm.csv` regenerado con los 40 IDs actuales, incluido Pijao. Propuesta y protocolo
humano en `docs/propuesta-catalogo.md`.

Actualización R1.4 (2026-11-11): asociación formal Java y reporte de lejanos generados por
`bootRun --args=r1`. `verificar-lugares` contrasta los 40 índices y distancias con OSMnx:
40/40 coinciden (`results/r1/verificacion_lugar_nodo.csv`). Cuatro alertas con notas de
tratamiento en `results/r1/lugares_lejanos.csv`; no certifican accesos ni operación actual.
Pruebas Python 74/74; Java completa 64: 41 aprobadas (incluida arquitectura) y 23 fallidas
por implementaciones pendientes de R2–R4. R1.1 y mediciones R1.3 aún pendientes.

Revisión posterior de accesos (2026-11-11): Cocora actualizado a `4.637694,-75.487470`
y Combia al parqueadero indicado (`4.491763,-75.686133`). RECUCA y El Agrado se conservan
con acceso vehicular confirmado por el integrante, sin certificación independiente.
UTM, asociaciones y reportes regenerados: OSMnx sigue coincidiendo en 40/40 lugares;
persisten las cuatro alertas geométricas >500 m. La llegada en vehículo declarada no
elimina el umbral ni añade el último acceso al grafo.

Diagnóstico posterior de esos puntos sobre la geometría del GraphML (2026-11-11):
en Cocora la vía incluida pasa cerca del punto, mientras sus extremos están separados;
en Combia la geometría vial incluida más cercana sigue apartada del parqueadero.
No se ha identificado ni comprobado el recorrido del acceso de la hacienda en OSM.
En ambos lugares la distancia mínima al nodo coincide entre la red cruda simplificada
y la componente exportada: no se atribuye la alerta al filtrado de componente fuerte.
Diagnóstico en memoria, no medición experimental ni cambio de asociación. Detalle de
comprobación en `docs/registro-ia.md`.

Actualización R1.1 (2026-11-11): comparación compacta implementada, probada y generada
por `bootRun --args=r1` en `results/r1/memoria_representacion.csv`. Incluye descargada,
componente exportada y grafo Java; se añadió `red` con aprobación del integrante.
R1.1 ya no está pendiente. Suite Java completa: 68 pruebas, 45 aprobadas y 23 fallos
conocidos de R2–R4 sin implementar; arquitectura aprobada. Python 74/74 y asociaciones
OSMnx 40/40. Mediciones R1.3 aún pendientes, por lo que R1 no se declara completo.

Revisión superficial (2026-11-11): alineación con enunciado y guías registrada en
`docs/revision-superficial.md`. Estado de R1 sigue parcial; antes de medir SCC hay que
aplicar el límite experimental también en Python. Pendientes de robustez de entrada
Java y consolidación de textos históricos señalados, sin cambios de implementación.

## Objetivo

Descargar la red vial del Quindío (OSMnx, `network_type="drive"`), elegir su representación,
asignar pesos (distancia y tiempo), quedarse con la componente fuertemente conexa más grande,
ubicar los lugares del catálogo en ella y guardar todo para no volver a descargar.

## Contrato

R1 se reparte entre Python (lo que exige OSMnx) y Java (lo propio y R1.1):

| Paso | Dónde · código | Salida |
|---|---|---|
| Descarga | Python · `preparacion.red.descargar` (`python -m preparacion descargar`) | `data/raw/quindio.graphml` (fuera de git) + fecha en metadatos |
| Pesos (R1.2) | Python · `preparacion.red.asignar_pesos` | `velocidad_kmh`, `fuente_velocidad`, `tiempo_s` por arista |
| Componente (R1.3) | Python · `preparacion.red.procesar` (`ox.truncate.largest_component`) | subgrafo SCC más grande |
| Exportación (R1.5) | Python · `preparacion.red.exportar_red`, `exportar_lugares_utm` | `data/processed/{nodos,aristas}.csv.gz`, `lugares_utm.csv`, `metadatos.json` |
| Medición R1.3 | Python · `preparacion.r1_componente` (`python -m preparacion r1`) | `results/r1/tamano_red.csv`, `tiempo_componente.csv` |
| Lectura | Java · `repository` · `RedRepository.leer()` → `RedLeida` | — |
| Grafo | Java · lógica · `RedService.grafo()` → `Grafo` (listas de adyacencia) | — |
| Memoria (R1.1) | Java · lógica · `ExperimentoR1Service` (`bootRun --args='r1'`) | `results/r1/memoria_representacion.csv` |
| Lugar → nodo (R1.4) | Java · lógica · `NodoMasCercano.buscar` (PROPIO) | `data/processed/lugares_nodos.csv`, `results/r1/lugares_lejanos.csv` |
| Verificación R1.4 | Python · `preparacion.verificaciones.verificar_lugares` | `results/r1/verificacion_lugar_nodo.csv` |

La capa de datos de Java entrega arreglos planos (`RedLeida`) y la capa lógica construye el
`Grafo` (`RedService.construir`). `repository` nunca usa clases de lógica.

**Formato de los archivos procesados** (contrato entre Python y Java; columnas en
`RedRepository.COLUMNAS_*`, `LugaresNodosRepository.COLUMNAS` y `python/preparacion/red.py`):

- `nodos.csv.gz`: `idx` (0..V-1, contiguo), `osmid`, `lat`, `lon`, `x_utm`, `y_utm`.
- `aristas.csv.gz`: `u`, `v` (en `idx`), `longitud_m`, `velocidad_kmh`, `fuente_velocidad`
  (`osm` | `imputada` | `defecto`), `tiempo_s`, `highway`.
- `lugares_utm.csv`: `id_lugar`, `x_utm`, `y_utm` (pyproj, porque Java no proyecta).
- `lugares_nodos.csv`: `id_lugar`, `idx_nodo`, `distancia_m`, `mas_de_500m`, `tratamiento`.
- `metadatos.json`: `fecha_descarga`, `consulta`, `network_type`, `retain_all`, versiones de
  osmnx/networkx, `nodos_descargada`, `aristas_descargada`, `nodos_componente`,
  `aristas_componente`, `porcentaje_velocidad_osm`.

**Red cruda:** `data/raw/quindio.graphml`, guardada con `ox.save_graphml` y leída con
`ox.load_graphml`. Incluye un atributo de grafo `metadatos_descarga` con texto JSON:
`fecha_descarga` (ISO 8601 en UTC), `consulta`, `network_type`, `retain_all`, `simplify`,
`osmnx`, `networkx`, `nodos_descargada`, `aristas_descargada`, `uso_cache_http`.
La fecha registra la adquisición por este comando, no una instantánea histórica de OSM;
OSMnx puede reutilizar respuestas HTTP en caché. Al procesar se conservarán estos datos en
`data/processed/metadatos.json`, sin crear una red procesada parcial durante la descarga.

Aristas paralelas (el grafo de OSMnx es un `MultiDiGraph`): `Grafo.desdeArreglos` conserva
para cada par (u, v) la de menor `tiempoS`. Documentarlo en el R1.1.

## Comportamiento esperado

### Descarga de la red (preparación de R1)

1. Ejecutar `ox.graph_from_place("Quindío, Colombia", network_type="drive",
   retain_all=True, simplify=True)`. Se conservan todas las componentes de la red obtenida
   para vehículos; todavía NO se selecciona la componente fuertemente conexa.
2. Registrar los metadatos anteriores y guardar el grafo no vacío, dirigido y multigrafo.
   La simplificación topológica de OSMnx conserva atributos de vías; no equivale a filtrar
   componentes. No se asignan velocidades ni pesos de viaje en este paso.
3. Si el archivo ya existe, leerlo y comprobar que sus metadatos corresponden a la consulta,
   las opciones aprobadas y sus conteos. Si es válido, reutilizarlo sin peticiones HTTP,
   sin reescribirlo y sin cambiar la fecha original. No exigir que la versión instalada
   coincida con la de adquisición: se conserva la procedencia original.
4. Un archivo ilegible, incompatible o sin metadatos produce un error claro; no se borra,
   sobrescribe ni vuelve a descargar silenciosamente. Para una descarga nueva, el usuario
   debe conservar o mover explícitamente la copia anterior.
5. Guardar primero en un archivo temporal en `data/raw/` y publicarlo al terminar. Si falla
   la descarga o escritura, no dejar un GraphML final incompleto; retirar solo el temporal
   creado por esta ejecución. Los errores se muestran desde la CLI y terminan con código 1.
6. Las pruebas usan un grafo pequeño y sustituyen únicamente la petición de descarga; el
   guardado y la lectura GraphML se comprueban con OSMnx real, sin acceso a internet.

### R1.1 Representación
1. Con los V y E reales de la red **descargada** (y de la componente), calcular los MB de:
   - **Matriz de adyacencia**: V² celdas. Supuesto explícito de bytes por celda (por ejemplo
     2 pesos `float64` = 16 B, o 1 peso `float32` = 4 B).
   - **Listas de adyacencia**: V cabeceras + E entradas; supuesto explícito por entrada
     (por ejemplo destino `int32` 4 B + 2 pesos `float64` 16 B = 20 B; cabecera 8 B).
   - Mencionar que en Java cada objeto tiene un encabezado (la implementación usa un
     `Arista[]` por nodo con objetos `Arista`); la estimación usa el modelo compacto de
     arreglos primitivos y puede complementarse con la medición real de la JVM.
2. Elegir y justificar (densidad E / V², operaciones que necesita Dijkstra: recorrer
   vecinos). La implementación ya elegida es listas (`model.Grafo`).
3. Guardar supuestos y resultados en `results/r1/memoria_representacion.csv`
   (`red,representacion,supuesto_bytes,V,E,megabytes`). Ampliación aprobada por el
   integrante el 2026-11-11; conserva las columnas previas y añade la identificación.
4. Modelo compacto acordado: matriz `16·V²` bytes (dos pesos de 8 bytes por celda),
   listas `8·V + 20·E` bytes (cabecera de 8 bytes por nodo, destino de 4 bytes y dos
   pesos de 8 bytes por entrada). MB decimales: dividir por 1 000 000, no por 2²⁰.
   No incluye objetos Java, coordenadas ni geometrías; NO es memoria real de la JVM.
   Una matriz representa un peso de tiempo y uno de distancia por par, no todas las
   aristas paralelas: la comparación de multigrafos es de almacenamiento compacto
   estimado, no equivalencia de conservación de todas las alternativas de cada par.
5. Generar seis filas: matriz y listas para `descargada`, `componente_exportada`
   (con paralelas) y `grafo_java` (deduplicado por menor tiempo).
   Conteos originales desde `data/processed/metadatos.json`; no descargar ni leer
   GraphML desde Java. `RedRepository.leerConteosRed()` entrega `ConteosRed`
   (cuatro enteros: nodos/aristas de descargada y componente); `RedService.conteosRed()`
   valida positivos, componente no mayor que descargada y coincidencia con filas CSV.
   Rechazar campos faltantes, fraccionarios, no enteros de 32 bits, negativos o cero.
6. `ExperimentoR1Service.ejecutar()` valida y calcula R1.1 y R1.4 antes de escribir
   las salidas; errores de validación no sobrescriben archivos anteriores. Los productos
   cuadráticos se calculan en `double` antes de multiplicar para evitar desbordamiento
   de `int`. `ResultadosRepository.COLUMNAS_MEMORIA_REPRESENTACION` fija la cabecera.
   No hay cronómetro: es una estimación analítica sobre tamaños reales.

### R1.2 Pesos
1. `longitud_m`: atributo `length` de OSMnx.
2. Política aprobada: `asignar_pesos(G: nx.MultiDiGraph) -> nx.MultiDiGraph` devuelve una
   copia con pesos; no modifica la entrada ni escribe archivos. Normaliza números positivos
   finitos, textos numéricos (km/h por defecto), unidades `km/h`, `kph` o `mph`
   (1 mph = 1.609344 km/h). En listas o valores separados por `;`, convierte primero y toma
   el mínimo. Si algún elemento no se interpreta o no es positivo finito, el atributo
   completo se considera desconocido. Conserva los atributos originales, incluido `maxspeed`.
3. Imputa mediante `ox.routing.add_edge_speeds`, usando su promedio por tipo de `highway`.
   Si un tipo no tiene velocidades conocidas, usa el promedio de los promedios conocidos
   por tipo (respaldo predeterminado de OSMnx, NO promedio global ponderado por aristas).
   Para `highway` en lista se usa el primer elemento, como OSMnx; se conserva la lista
   original. Exige tipo de vía no vacío y al menos una velocidad OSM válida en la red;
   si no hay ninguna, aborta sin inventar una velocidad constante.
   `fuente_velocidad`: `osm` si el atributo se interpreta, `imputada` si se usa el promedio
   de su tipo y `defecto` si se usa el respaldo entre tipos. El porcentaje OSM se calculará
   al exportar metadatos (no se confunde atributo presente con velocidad válida).
4. `tiempo_s = longitud_m / (velocidad_kmh / 3.6)`. Es una estimación del modelo, no el
   tiempo real (el enunciado pide decirlo).
5. Todos los pesos deben ser > 0 (Dijkstra exige no negativos; `Grafo.desdeArreglos`
   rechaza negativos).
   Rechaza longitudes ausentes, no numéricas, no finitas o <= 0, y comprueba que velocidades
   y tiempos calculados sean finitos y positivos. Añade `longitud_m`, `velocidad_kmh`,
   `fuente_velocidad`, `tiempo_s`; elimina de la copia el atributo auxiliar `speed_kph`
   generado por OSMnx. No redondea tiempos durante este paso.

### R1.3 Componente fuertemente conexa
1. Obtenerla con `ox.truncate.largest_component(G, strongly=True)` (aquí sí se usa la
   librería, lo pide el enunciado).
2. **Investigar el algoritmo interno en la versión instalada**. Revisar el código fuente:
   `python -c "import inspect, osmnx; print(inspect.getsource(osmnx.truncate.largest_component))"`.
   En OSMnx 2.1.1 la función llama a `nx.is_strongly_connected`, luego a
   `nx.strongly_connected_components`, toma la de mayor tamaño con `max(..., key=len)` y
    copia el subgrafo inducido. Inspeccionados código y documentación de NetworkX 3.7:
    `strongly_connected_components` usa Tarjan iterativo con mejoras de Tarjan y Zwick
    (2024) y una técnica de WebGraph, no la variante de Nuutila de versiones anteriores.
    Explicar su funcionamiento y derivar el costo por inspección del recorrido, incluyendo
    la verificación previa y la copia del subgrafo. No atribuir a la documentación una
    complejidad que no declara explícitamente.
3. Medir el tiempo de la función en redes de tamaño creciente: `ox.graph_from_point` desde la
   plaza de Bolívar de Armenia con radios 2, 5, 10 y 20 km, y la red del departamento.
   1 calentamiento + 5 repeticiones, mediana (`preparacion.medicion.medir`). Tabla: red, V,
   E, V+E, tiempo (ms). Se mide en Python porque la función es de OSMnx.
4. Comprobar si el tiempo crece como lo predice la complejidad (gráfica V+E vs tiempo y
   cociente tiempo/(V+E) aproximadamente constante si es lineal).
5. Explicar por qué conviene trabajar solo con la componente: desde cualquier lugar se llega
   a cualquier otro respetando el sentido de las vías, así que la matriz de tiempos de R2 no
    tiene infinitos y todo recorrido de R4 es factible.

### Procesamiento y exportación (R1.3 / R1.5)

- `procesar()` no descarga: exige `data/raw/quindio.graphml` válido, lee su procedencia,
  asigna pesos y obtiene la componente con `strongly=True`.
- Ordena los identificadores OSM numéricos para asignar índices contiguos `0..V-1`.
  Ordena aristas por `(u_osm, v_osm, clave)`; conserva paralelas, sentidos y bucles.
  `highway` múltiple se serializa como texto con `|`, conservando el orden de la lista.
  Proyecta nodos a UTM 18N y valida coordenadas geográficas y proyectadas finitas y válidas.
- Exporta las columnas ya acordadas. Los metadatos conservan la adquisición original;
  añade los conteos de la componente y `porcentaje_velocidad_osm` sobre sus aristas,
  calculado por `fuente_velocidad == 'osm'`. No confundir E exportado con E tras deduplicar
  aristas paralelas en Java. No modifica la red cruda.
- Exporta coordenadas UTM de las filas del catálogo que tengan latitud Y longitud;
  si ninguna las tiene, genera solo la cabecera y la CLI avisa: la asociación de lugares
  sigue pendiente. Esto NO da por válido ni completo el catálogo.
- La CLI informa la carpeta exportada y la limitación del catálogo. No mide tiempos:
  los experimentos siguen en `preparacion.r1_componente`, con `medicion.medir`.

**Cuidado (detectado en OSMnx 2.1.1):** `graph_from_place` y `graph_from_point` tienen
`retain_all=False` por defecto, es decir, **ya devuelven la componente débilmente conexa más
grande**. Para que "Red descargada" sea la red completa y para que la medición de R1.3 incluya
redes no conexas, usar `retain_all=True` (decisión recomendada; documentarla).
`graph_from_point` usa `dist_type="bbox"` por defecto (un cuadrado de lado 2·radio);
documentar qué se usó.

### R1.4 Ubicación de los lugares
1. Python convierte lugares y nodos a UTM 18N (`preparacion.proyeccion`, pyproj).
2. En Java, para cada lugar, `NodoMasCercano.buscar(xs, ys, x, y)`: recorrido lineal propio,
   distancia euclidiana, empate → menor índice. Devuelve `NodoCercano(indice, distanciaM)`.
3. Verificar contra `ox.distance.nearest_nodes` sobre el grafo proyectado para todos los
   lugares (`python -m preparacion verificar-lugares`).
4. Reportar los lugares con distancia > 500 m (`results/r1/lugares_lejanos.csv`) y explicar
   el tratamiento de cada uno (`tratamiento`): corregir coordenada si estaba mal, aceptar
   indicando que el acceso es por una vía no incluida en `drive`, o reemplazar el lugar.
    El catálogo final debe seguir cumpliendo los mínimos por categoría.
5. La distancia al nodo y la distancia a la geometría de una vía son distintas. Para
   diagnosticar accesos se permite contrastarlas con librerías sobre el GraphML crudo,
   proyectado a UTM y restringido a los nodos de la componente exportada. Esto no cambia
   la asociación por nodo, no inserta nodos y no añade tiempos de acceso al modelo.
   Una alerta >500 m NO prueba por sí sola que el filtro `drive` haya excluido una vía.
   Solo documentar etiquetas de exclusión si se han comprobado en el tramo correspondiente;
   de lo contrario distinguir observación del grupo, hipótesis y limitación del modelo.

### Integración de R1.4 (etapa sin mediciones)

- `RedService.lugaresUtm(): Map<String, double[]>` entrega las coordenadas exportadas
  mediante `RedRepository`; los algoritmos y la CLI no leen archivos directamente.
- `ExperimentoR1Service.ejecutar()` integra R1.1 y R1.4; genera estimaciones de memoria,
  nunca tiempos experimentales. Conserva su constructor actual.
- Antes de escribir, valida el catálogo, la igualdad de IDs con `lugares_utm.csv`,
  coordenadas finitas y la forma no vacía del grafo. Calcula todas las asociaciones con
  `NodoMasCercano.buscar`, en orden del catálogo. `mas_de_500m` es `distancia_m > 500`,
  no `>= 500`. Conserva en `tratamiento` las notas del catálogo; si un caso lejano carece
  de notas, escribe `Pendiente: revisar acceso y justificar distancia al nodo`.
  Ese texto pendiente NO certifica que el caso esté aceptado ni resuelto.
- Guarda `lugares_nodos.csv` para todos los lugares y `results/r1/lugares_lejanos.csv`
  solo para los lejanos, ambos con `id_lugar,idx_nodo,distancia_m,mas_de_500m,tratamiento`.
  Con cero lejanos, el segundo archivo contiene solo cabecera, sin filas ficticias.
  `ResultadosRepository.guardarLugaresLejanos(List<FilaLugarNodo>)` define este contrato
  con `COLUMNAS_LUGARES_LEJANOS`. Fallos de validación no sobrescriben salidas anteriores.
- `python -m preparacion verificar-lugares` lee CSV, construye un grafo de nodos UTM
  (`crs=EPSG:32618`) y consulta `ox.distance.nearest_nodes(..., return_dist=True)`.
  No descarga ni recalcula asociaciones productivas. Comprueba IDs, índices, valores
  finitos, coordenadas UTM coherentes con lat/lon actuales del catálogo (tolerancia
  absoluta 1e-6 m), distancias Java (tolerancia absoluta 1e-6 m) y banderas del umbral.
- Salida del verificador: `id_lugar,idx_java,idx_osmnx,coinciden` en
  `results/r1/verificacion_lugar_nodo.csv`. `coinciden` exige mismo índice, distancia
  correcta y bandera de umbral correcta. La comparación de índices es estricta: una diferencia, incluso debida a
  empates que OSMnx resuelva distinto, queda para revisión; nunca se oculta ni se cambia
  la salida Java. Guarda discrepancias y termina con error si hay alguna. Archivos
  incompletos o mal formados abortan antes de generar el reporte. No es una medición.

### R1.5 Grafo procesado
1. Exportar con `preparacion.red.exportar_red` (formato arriba). El resto del proyecto
   **solo** lee estos archivos (`RedRepository` → `RedService`), nunca vuelve a descargar.
2. Reportar nodos y aristas antes y después de filtrar la componente
   (`results/r1/tamano_red.csv`).

## Restricciones

- `NodoMasCercano` es implementación propia (`algorithm`, solo `java.lang`/`java.util`).
- En `python/` se permiten OSMnx, GeoPandas, pyproj, pandas, NumPy (datos y verificación).

## Casos de prueba

| ID | Entrada | Salida esperada | Origen |
|---|---|---|---|
| T1.M-a | descargada V=3/E=4; componente V=2/E=2; Java V=2/E=1 por paralelas | seis filas; matriz 144/64/64 bytes; listas 104/56/36 bytes | cálculo manual |
| T1.M-b | metadatos faltantes, ilegibles, incompletos, fraccionarios, negativos o fuera de rango | error antes de sobrescribir cualquier salida | validación |
| T1.M-c | conteos de componente diferentes de CSV o mayores que descargada | error antes de exportar | validación |
| T1.M-d | V descargada=100000 | matriz 160000 MB sin desbordamiento de int | cálculo manual |
| T1.D-a | descarga simulada con varias componentes y aristas paralelas | llamada con `drive`, `retain_all=True`, `simplify=True`; GraphML conserva nodos/aristas y metadatos | simulación + ida y vuelta |
| T1.D-b | GraphML válido ya guardado | no llama a la red ni reescribe archivo o fecha | simulación |
| T1.D-c | archivo GraphML ilegible | error claro, archivo intacto, sin peticiones | archivo inválido |
| T1.D-d | archivo sin metadatos de descarga | error claro, sin peticiones ni sobrescritura | archivo de prueba |
| T1.D-e | metadatos incompatibles o conteos distintos | error claro, conserva archivo | archivo de prueba |
| T1.D-f | descarga devuelve un grafo vacío | error y ningún GraphML final | simulación |
| T1.D-g | fallo de conexión durante descarga | CLI termina con código 1 y ningún GraphML final | simulación |
| T1.D-h | fallo al escribir GraphML temporal | ningún GraphML final ni temporal huérfano | simulación |
| T1.D-i | CLI con descarga correcta | anuncia preparación y ruta del archivo disponible | simulación |
| T1.4-a | nodos (0,0), (10,0), (0,10); punto (6,1) | `(1, √17 ≈ 4.1231)` | a mano |
| T1.4-b | mismos nodos; punto (5,0) (equidistante de 0 y 1) | `(0, 5.0)` (empate → menor índice) | a mano |
| T1.4-c | un solo nodo (3,4); punto (0,0) | `(0, 5.0)` | a mano |
| T1.4-d | 500 nodos al azar (semilla), 50 puntos | igual a una búsqueda directa en la prueba | verificación |
| T1.4-e | nodos (0,0), (10,0), (0,10); punto (0,10) | `(2, 0.0)` | coincidencia exacta |
| T1.4-f | nodos (100,100), (0,0), (10,0); punto (5,0) | `(1, 5.0)` | empate entre nodos posteriores al primero |
| T1.5-a | guardar y volver a cargar una red pequeña | mismo V, E y pesos | ida y vuelta |
| T1.2-a | arista de 1000 m a 60 km/h | `tiempo_s = 60.0` (prueba en `python/tests`) | a mano |
| T1.2-b | números, unidades, listas y `;` | conversión mph y mínimo tras convertir | a mano |
| T1.2-c | tipo conocido con velocidades 30 y 50 | faltante recibe 40, fuente imputada | a mano |
| T1.2-d | tipos con promedios 40 y 80; tipo sin datos | respaldo 60, fuente defecto | a mano |
| T1.2-e | atributos inválidos con otros tramos válidos | imputación, no fuente osm | a mano |
| T1.2-f | ninguna velocidad válida | error sin velocidad inventada | validación |
| T1.2-g | longitud ausente, inválida, cero, negativa o infinita | error sin modificar entrada | validación |
| T1.2-h | highway múltiple o ausente | primer tipo o error; original intacto | validación |
| T1.2-i | vías paralelas y entrada con atributos | copia conserva estructura y originales; pesos positivos | ida y vuelta en memoria |
| T1.P-a | dos SCC de tamaños distintos, con unión de un solo sentido | exporta solo la SCC mayor; índices ordenados y contiguos | grafo manual |
| T1.P-b | SCC con paralelas y sentidos opuestos | conserva filas y pesos; conteos y porcentaje OSM correctos | grafo manual |
| T1.P-c | coordenadas geográficas conocidas | UTM igual a proyección independiente pyproj | pyproj |
| T1.P-d | archivo crudo ausente o inválido | falla sin descargar ni exportar | simulación |
| T1.P-e | catálogo sin coordenadas | cabecera UTM y aviso CLI; no inventa lugares | catálogo sintético |
| T1.P-f | red ya fuertemente conexa, procesada dos veces | mismas filas y metadatos; GraphML intacto | ida y vuelta |
| T1.P-g | coordenada de nodo ausente, no finita o fuera de rango | error antes de exportar | validación |
| T1.I-a | catálogo válido y coordenadas UTM con lugares a 0, 500 y >500 m | asociación propia; bandera estricta; notas conservadas | grafo manual |
| T1.I-b | ningún lugar lejano | CSV de lejanos solo con cabecera | archivo temporal |
| T1.I-c | UTM faltante, sobrante o no finita; catálogo inválido | aborta sin sobrescribir asociaciones | validación |
| T1.I-d | nodos con coordenadas inválidas | aborta sin exportar | validación |
| T1.V-a | CSV sintético de Java correcto | índices y distancias coinciden con OSMnx, sin red | OSMnx real |
| T1.V-b | índice o distancia Java incorrectos | reporte con discrepancia y error | archivo de prueba |
| T1.V-c | IDs repetidos/faltantes, índice inválido o UTM desactualizado | aborta antes de reporte | validación |
| T1.V-d | bandera del umbral incorrecta | discrepancia y error | archivo de prueba |

## Criterios de aceptación

- [x] Descarga cruda: GraphML real guardado, leído y reutilizado sin peticiones ni sobrescritura.
- [x] Componente y CSV de red generados por `procesar`, validados y leídos desde Java.
- [ ] `data/processed/` generado y versionado; `metadatos.json` con fecha de descarga.
- [ ] `results/r1/tamano_red.csv` y `tiempo_componente.csv` (`python -m preparacion r1`);
      `memoria_representacion.csv` y `lugares_lejanos.csv` (`./gradlew bootRun --args='r1'`);
      `verificacion_lugar_nodo.csv` sin diferencias (`python -m preparacion verificar-lugares`).
- [ ] Pruebas T1.* en verde (`NodoMasCercanoTest`, `RepositoriosTest`, `GrafoTest`).
- [x] Búsqueda de R1.4: T1.4-a a T1.4-f aprobadas; falta verificar con OSMnx en la red real.
- [ ] Documento: supuestos de memoria, elección justificada, origen de velocidades,
      algoritmo de SCC con fuente y complejidad, tabla y discusión de tiempos.

## Análisis teórico (va al documento técnico)

- Memoria: matriz Θ(V²) vs listas Θ(V + E) con los números reales.
- SCC: algoritmo, complejidad en tiempo y memoria con fuente (documentación/código de
  NetworkX y el artículo original del algoritmo).
- `NodoMasCercano.buscar`: Θ(V) por lugar, Θ(L·V) en total para L lugares; memoria O(1) extra.

## Experimento

`results/r1/tiempo_componente.csv`: `red, V, E, V_mas_E, tiempo_ms, repeticiones, estado`.
Figura `results/figuras/r1_tiempo_componente.png` (V+E en x, tiempo en ms en y).

## Decisiones abiertas

1. **Velocidades**: decisión tomada; imputación OSMnx y mínimo de límites múltiples,
   según R1.2. No se usa una tabla normativa como si representara velocidades observadas.
2. `retain_all=True` y `simplify=True` aprobados para la red departamental. Falta definir
   explícitamente las opciones y `dist_type` de `graph_from_point` para los experimentos.
3. Tratamiento de los lugares a más de 500 m.
