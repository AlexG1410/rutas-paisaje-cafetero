# R1 — Modelo de la red vial

Estado (2026-11-12): R1.1–R1.5 implementados y experimento R1.3 ejecutado;
documentación y figura disponibles, en revisión de cierre. Peso: 15 %.

- Red descargada: 8877 nodos/20245 aristas; componente exportada: 8791/20132;
  grafo Java sin paralelas redundantes: 8791/19997.
- Catálogo: 40 lugares (12/15/8/5), con asociación Java contrastada contra OSMnx:
  40/40 coincidencias y cuatro alertas >500 m que se conservan y explican.
- Experimento SCC: cinco redes y cinco repeticiones por red, sin timeout. Tablas en
  `results/r1/`, entorno en `results/maquina_python.csv`, figura en
  `results/figuras/r1_tiempo_componente.png`.
- Contratos y pruebas se definen abajo. La evidencia de ejecución y la evolución de
  decisiones se conservan en `docs/registro-ia.md` y `docs/decisiones.md`, no como estados
  históricos superpuestos en esta spec.
- Robustez de entradas Java cerrada con el bloque R2: pesos no finitos/negativos,
  tamaños y coordenadas inválidas se rechazan; se admiten cero y opcionales null en pruebas.
  Revisión final del documento a cargo del grupo. R3/R4 pendientes no son fallos de R1.

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
| Exportación (R1.5) | Python · `preparacion.red.procesar` | `data/processed/{nodos,aristas}.csv.gz`, `lugares_utm.csv`, `metadatos.json` |
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

Contrato experimental aprobado (2026-11-11): `python -m preparacion r1` reutiliza
`quindio.graphml` y adquiere/reutiliza `data/raw/r1/armenia_{2,5,10,20}km.graphml`.
Centro: única fila `cab_armenia` del catálogo, coordenadas finitas y geográficas válidas.
Descarga explícita `dist_type="bbox"`, `dist=radio_km*1000`, `network_type="drive"`,
`retain_all=True`, `simplify=True`: cuadrados, no discos ni distancias por carretera.
Cada GraphML conserva `metadatos_descarga` con centro, radio, opciones, fecha UTC,
versiones y conteos. Copias inválidas abortan sin redescarga ni sobrescritura; escritura
temporal validada antes de publicar. No altera red departamental ni `data/processed`.

Antes de medir cada red, contrastar la partición SCC de NetworkX con Kosaraju y la
componente OSMnx con el máximo tamaño (aceptar empates); comprobar subgrafo inducido
con todas las aristas paralelas. Después, medir solo `largest_component(strongly=True)`
con `medir_con_limite` y validar también el resultado medido. Si una verificación falla,
abortar antes de publicar resultados. Preparar las cinco redes antes de iniciar mediciones.
Guardar `results/maquina_python.csv` antes de medir.

`tamano_red.csv`: `red,nodos,aristas`, dos filas por zona, sufijos `_descargada` y
`_componente`; `tiempo_componente.csv`: `red,V,E,V_mas_E,tiempo_ms,repeticiones,estado`.
Zonas: `armenia_2km`, `armenia_5km`, `armenia_10km`, `armenia_20km`, `quindio`.
V/E de tiempos son de la red de entrada; no se deduplican aristas. Timeout conserva
conteos verificados previamente, pero deja tiempo vacío. Publicar tablas solo tras
completar la serie sin errores; un timeout experimental no es error de infraestructura.
La figura se genera con `python -m preparacion graficas`, exclusivamente desde el CSV;
el documento presenta tiempos redondeados a un decimal y cocientes tiempo/(V+E).
Los CSV conservan la precisión original, sin edición manual.
La figura distingue ejecuciones `completada` de `no completada`: las últimas se
marcan sin atribuirles un tiempo de cero ni inventar su duración. Rechaza CSV vacíos,
columnas faltantes y tiempos no finitos para ejecuciones completadas.

| ID | Prueba experimental sin HTTP | Esperado |
|---|---|---|
| T1.E-a | adquisición con OSMnx simulado y persistencia GraphML real | opciones exactas; reutilización sin HTTP ni cambios |
| T1.E-b | copia corrupta/metadatos distintos o centro inválido | aborta sin sobrescribir ni descargar |
| T1.E-c | SCC con paralelas y empate de máximos | acepta máximo inducido; rechaza resultado incorrecto |
| T1.E-d | serie con medidor simulado y timeout | tablas y máquina; solo función OSMnx al medidor; no cambia entradas |
| T1.E-e | verificación fallida | no mide ni publica tablas |
| T1.F-a | CSV completo con filas fuera de orden | genera PNG desde CSV sin modificarlo |
| T1.F-b | ejecución no completada con tiempo vacío | marca explícita sin tiempo ficticio |
| T1.F-c | columnas faltantes, datos vacíos/no finitos | error sin generar figura |
| T1.C-a | catálogo actual | conserva coordenadas acordadas y fuente uniforme; sin bitácora individual en notas |

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
   1 calentamiento + 5 repeticiones, mediana (`preparacion.medicion.medir_con_limite`). Tabla: red, V,
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
  los experimentos siguen en `preparacion.r1_componente`, con `medicion.medir_con_limite`.

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
   de notas, aborta con `IllegalStateException` identificando el lugar, antes de escribir
   cualquier salida. No genera tratamientos ficticios ni textos «Pendiente».
- El catálogo usa en `fuente_coordenadas` el formato `Google Maps; revisado en mapa por
  el grupo (AAAA-MM-DD)`, conservando las fechas de las revisiones declaradas. Las notas
  describen el punto y, si corresponde, su tratamiento geométrico, sin atribuciones
  individuales ni bitácora. Las limitaciones generales del modelo van al documento.
- Puntos acordados: Peñas Blancas `4.481609,-75.634164`, Cocora `4.637694,-75.487470`,
  Combia `4.491763,-75.686133`, RECUCA `4.4326046,-75.7332347`,
  El Agrado `4.5180079,-75.7926973`. No se mueven para reducir alertas.
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
1. Exportar con `preparacion.red.procesar` (formato arriba). El resto del proyecto
   **solo** lee estos archivos (`RedRepository` → `RedService`), nunca vuelve a descargar.
2. Reportar nodos y aristas antes y después de filtrar la componente
   (`results/r1/tamano_red.csv`).

## Restricciones

Validación de `Grafo.desdeArreglos` (cierre previo a R2): n >= 0, arreglos de aristas
no nulos y de igual longitud; pesos finitos y no negativos. Cada arreglo opcional de
OSM IDs/coordenadas, si está presente, tiene longitud n; coordenadas finitas. Se permiten
arreglos opcionales null en pruebas y pesos cero. Error: `IllegalArgumentException`,
antes de construir listas. Casos T1.G-a: NaN/infinito/negativos; T1.G-b: tamaños,
coordenadas o entradas nulas inválidas; T1.G-c: grafo vacío y pesos cero válidos.

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
| T1.I-e | lugar a >500 m sin notas | error con ID; no sobrescribe asociaciones, reporte ni memoria | validación |
| T1.V-a | CSV sintético de Java correcto | índices y distancias coinciden con OSMnx, sin red | OSMnx real |
| T1.V-b | índice o distancia Java incorrectos | reporte con discrepancia y error | archivo de prueba |
| T1.V-c | IDs repetidos/faltantes, índice inválido o UTM desactualizado | aborta antes de reporte | validación |
| T1.V-d | bandera del umbral incorrecta | discrepancia y error | archivo de prueba |

## Criterios de aceptación

- [x] Descarga cruda: GraphML real guardado, leído y reutilizado sin peticiones ni sobrescritura.
- [x] Componente y CSV de red generados por `procesar`, validados y leídos desde Java.
- [x] `data/processed/` generado y versionado; `metadatos.json` con fecha de descarga.
- [x] `results/r1/tamano_red.csv` y `tiempo_componente.csv` (`python -m preparacion r1`);
      `memoria_representacion.csv` y `lugares_lejanos.csv` (`./gradlew bootRun --args='r1'`);
      `verificacion_lugar_nodo.csv` sin diferencias (`python -m preparacion verificar-lugares`).
- [x] Pruebas T1.* en verde (`NodoMasCercanoTest`, `RepositoriosTest`, `GrafoTest`).
- [x] Búsqueda de R1.4: T1.4-a a T1.4-f aprobadas; contraste OSMnx real: 40/40 coinciden.
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
2. Resuelta (2026-11-11): redes experimentales con `drive`, `retain_all=True`,
   `simplify=True`, `dist_type="bbox"`; cuadrados centrados en `cab_armenia`.
3. Lugares a más de 500 m: se mantienen y explican las cuatro alertas de R1.4;
   acceso vehicular declarado no equivale a comprobar la entrada exacta ni la operación actual.
