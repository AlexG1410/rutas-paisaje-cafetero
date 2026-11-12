# Registro de decisiones

Formato breve: contexto → decisión → alternativas → consecuencias. Una decisión "propuesta"
se confirma (o cambia) cuando el equipo trabaja el requerimiento; al cambiarla, actualizar
también la tabla de la sección 10 de `AGENTS.md`.

## D1. Java 21 para los algoritmos; Python solo para datos y verificación — tomada (2026-11-07)
- **Contexto**: el equipo entiende mejor Java. El enunciado deja el lenguaje libre, exige que
  todos los algoritmos estén en el mismo lenguaje, que la descarga se haga en Python (OSMnx) y
  permite exportar el grafo a archivos para leerlo desde otro lenguaje.
- **Decisión**: Java 21 para todos los algoritmos, experimentos R1.1/R1.4/R2–R4 y la app.
  Python (`python/`) solo para descarga, pesos, componente con OSMnx (y su medición R1.3),
  exportación a CSV, verificación con NetworkX/OSMnx/SciPy y figuras.
- **Consecuencias**: dos entornos (JDK y Python). Las verificaciones contra librerías corren
  después del paso Java correspondiente. La comparación con SciPy en R3 mezcla algoritmo y
  lenguaje (C frente a Java con JIT) y así debe discutirse. (Reemplaza la decisión inicial
  del 2026-11-06 de hacer todo en Python.)

## D2. Spring Boot 4.1 + Gradle — tomada (2026-11-07)
- Preferencia del equipo. Spring Boot 4.1 es la línea vigente (requiere Java 17+, usamos 21).
  Gradle con wrapper para que todos usen la misma versión de la herramienta.

## D3. Arquitectura en 3 capas — tomada (2026-11-07), paquetes actualizados (2026-11-10)
- **Contexto**: el equipo trabaja siempre por capas.
- **Decisión**: paquetes tradicionales en inglés: `controller` y `cli` (presentación) →
  `service`, `algorithm` y `model` (lógica) → `repository` (datos). La capa de datos entrega records propios y la lógica
  arma el `Grafo`; la presentación pide todo a `service`. Algoritmos y modelo puros.
- **Alternativas**: 4 capas (presentación/aplicación/dominio/infraestructura).
- **Consecuencias**: `ArquitecturaTest` (ArchUnit) falla si alguien (persona o IA) salta una
  capa o usa una librería en los algoritmos.
- **Reorganización aprobada (2026-11-10)**: se eliminan los paquetes contenedores
  `presentacion` y `logica`; `datos` pasa a `repository`, `medicion` a `service.measurement`
  y las pruebas de arquitectura a `architecture`. Todas las carpetas Java usan inglés;
  clases, métodos y comentarios mantienen sus nombres e idioma. No cambia la lógica ni los
  contratos HTTP/CSV. ArchUnit conserva las mismas restricciones con los nuevos paquetes.

## D4. Listas de adyacencia con nodos 0..V-1 — tomada
- Red vial muy dispersa (E del orden de V). Matriz Θ(V²) inviable; Dijkstra solo recorre
  vecinos. Aristas paralelas: se conserva la de menor tiempo (única útil para caminos
  mínimos). Los números exactos de R1.1 confirman la elección.

## D5. Arnés de medición común — tomada
- `Medidor`: `System.nanoTime`, ms, `System.gc()` antes de cada ejecución, 1 calentamiento
  (JIT) + mediana de 5, una sola medición si pasa de 60 s, JVM hija con límite de 300 s para
   cada serie experimental que requiera corte. Python usa la misma política. Mediciones
   Java con `bootRun`, `-Xms2g -Xmx2g -Xss16m` y `optimizedLaunch=false` para no limitar
   el JIT por una optimización de arranque. El hijo avisa INICIO después de preparar/GC;
   límite por ejecución y espera de infraestructura separada, no por serie completa.

## D6. Envolvente: QuickHull — propuesta
- Puntos de la red mayoritariamente interiores (cascos urbanos y vías). Alternativa: por
  mezcla (Preparata-Hong), O(n log n) en el peor caso.

## D7. Heurística: vecino más cercano — propuesta
- Voraz clásica, Θ(k²), fácil de justificar con los elementos voraces de la guía 08.
  Contraejemplo conocido (T4-b) para R4.5.

## D8. Velocidades de tramos sin `maxspeed` — tomada (2026-11-10)
- Aprobada la imputación con `ox.routing.add_edge_speeds`: promedio de límites conocidos
  del mismo tipo de vía y, si no hay datos de ese tipo, promedio entre promedios de tipos
  con datos. No es un promedio global ponderado por número de aristas.
- Límites múltiples: convertir a km/h y tomar el mínimo, como supuesto conservador.
  Un valor ambiguo se considera desconocido. Si toda la red carece de datos, abortar.
- `highway` múltiple: primer elemento, siguiendo OSMnx 2.1.1; conservar el atributo original.
  Es una limitación de clasificación del modelo, no una afirmación de homogeneidad del tramo.
- Procedencia por arista: `osm`, `imputada` o `defecto`. Son tiempos estimados sin tráfico,
  semáforos ni pendientes, no velocidades observadas ni garantía de duración real.

## D9. `retain_all=True` al descargar — tomada (2026-11-10)
- Con el valor por defecto (`False`) OSMnx ya devuelve la componente débilmente conexa más
  grande, lo que altera "red descargada" y la medición de R1.3.
- Aprobado por el integrante: `network_type="drive"`, `retain_all=True`, `simplify=True`.
  La componente fuerte se seleccionará después. Se conserva la descarga en GraphML con
  metadatos incorporados; un archivo válido se reutiliza sin sobrescribir su fecha.
  La fecha corresponde a la adquisición local y no a una instantánea histórica de OSM;
  se registra si estaba habilitada la caché HTTP de OSMnx. Las opciones para las redes
   por radio se aprobaron posteriormente en D12; velocidades resueltas en D8.

## D10. Aplicación: REST + página estática con Leaflet — tomada (2026-11-07)
- Controladores REST que devuelven JSON y una página HTML/JavaScript con Leaflet servida por
  Spring Boot. Sin cálculos en el navegador.

## D11. Resultados en CSV — tomada
- Todos los resultados (incluida la máquina) en CSV para leerlos igual desde Java, Python y la
  app; el enunciado permite CSV o JSON.

## D12. Redes experimentales R1.3 con bbox — tomada (2026-11-11)
- Aprobado por el integrante: `graph_from_point` centrado en `cab_armenia` del catálogo,
  con `dist` de 2000/5000/10000/20000 m, `dist_type="bbox"`, `network_type="drive"`,
  `retain_all=True`, `simplify=True`. Son cuadrados, no círculos ni distancias de recorrido.
- Se guardan aparte en `data/raw/r1/`, con procedencia y versiones, sin modificar el
  GraphML departamental ni los CSV de rutas. Copias válidas se reutilizan; inválidas
  abortan sin sobrescritura. La descarga no forma parte del tiempo medido.
- Antes de medir se contrastan particiones SCC con Kosaraju. Se mide la función completa
  `ox.truncate.largest_component(strongly=True)`, no solo el recorrido interno de SCC.
  El medidor aislado aplica límite por ejecución, incluido calentamiento.
- Las zonas pueden diferir en densidad, proporción conexa y atributos; la gráfica no
  constituye por sí sola una prueba de complejidad ni una comparación de algoritmos propios.

## D13. Pares y tramo medido de R2 — tomada (2026-11-12)
- Aprobados diez pares del mismo municipio y diez intermunicipales, dirigidos y sin
  compartir nodo origen/destino en un mismo par. Se ordena catálogo por ID y se barajan
  ambos grupos con una instancia Random(SEMILLA); los pares elegidos se guardan en CSV.
- La lineal se mide solo en los diez intramunicipales; la cola en los veinte. Ambas
  incluyen reconstrucción de ruta, pero no preparación, lectura, impresión ni transporte.
- Mismo municipio es el criterio geográfico, no promesa de menor recorrido ni menor
  cantidad de nodos explorados. No reemplazar consultas por resultar lentas.
- Matriz conserva tres decimales en segundos (contrato existente), verificación externa
  con tolerancia de redondeo distinta de las comparaciones de resultados double.
