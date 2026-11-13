# Fuentes candidatas

**Ninguna se cita sin verificarla antes** (autor, año, título y, si aplica, capítulo o
sección). Al verificarla, marcar `[x]` y anotar exactamente qué se usó.

## Libros y artículos
- [x] Rényi, A. y Sulanke, R. (1963). *Über die konvexe Hülle von n zufällig
      gewählten Punkten*. Zeitschrift für Wahrscheinlichkeitstheorie und verwandte
      Gebiete, 2, 75–84. DOI: https://doi.org/10.1007/BF00535300.
      Ficha bibliográfica contrastada el 2026-11-12 con la lista del coautor:
      https://www.math.hu-berlin.de/~sulanke/sulanke-pub.html, entrada [5].
      Original consultado el 2026-11-12 en `docs/guias/über die páginas 75-84.pdf`
      (copia local excluida de Git), texto y facsímil contrastados. Introducción p. 75:
      puntos independientes con la misma distribución; §1: uniforme en el interior
      de un polígono convexo fijo K de r lados. Teorema 1, p. 78 (página 4 del PDF),
      ecuación (20): E_n=(2r/3)(ln n+γ)+(2/3)ln((∏f_i)/F^r)+o(1), donde F es el área
      de K, f_i el área del triángulo de tres vértices consecutivos y γ la constante
      de Euler. El artículo cuenta lados, equivalentes a vértices para el casco
      poligonal no degenerado (probabilidad uno en este modelo continuo con n≥3).
      Para un cuadrado, r=4 y f_i=F/2: E[h]=(8/3)(ln n+γ−ln 2)+o(1), de donde
      E[h]∼(8/3)ln n y E[h]=O(log n). Se usa este resultado para el modelo continuo;
      la deducción E[T]=O(n log n) desde nuestra cota O(nh) es propia, con operaciones
      de costo constante. La cuadrícula entera fija y la red vial tienen otras
      distribuciones. La referencia original reemplaza la consulta secundaria pendiente.
- [ ] Cormen, T. H., Leiserson, C. E., Rivest, R. L. y Stein, C. (2022). *Introduction to
      algorithms* (4.ª ed.). MIT Press. — Dijkstra, montículos binarios, caminos mínimos,
      envolvente convexa, Teorema Maestro. (Verificar números de capítulo en la edición usada.)
- [ ] Dijkstra, E. W. (1959). A note on two problems in connexion with graphs. *Numerische
      Mathematik, 1*, 269–271. — R2.
- [ ] Tarjan, R. E. (1972). Depth-first search and linear graph algorithms. *SIAM Journal on
      Computing, 1*(2), 146–160. — R1.3 (componentes fuertemente conexas).
- [ ] Nuutila, E. y Soisalon-Soininen, E. (1994). On finding the strongly connected
      components in a directed graph. *Information Processing Letters, 49*(1), 9–14. — R1.3
      (variante que cita NetworkX; confirmar en su documentación).
- [ ] Held, M. y Karp, R. M. (1962). A dynamic programming approach to sequencing problems.
      *Journal of the SIAM, 10*(1), 196–210. — R4.1.
- [x] Barber, C. B., Dobkin, D. P. y Huhdanpaa, H. (1996). The Quickhull algorithm for convex
       hulls. *ACM Transactions on Mathematical Software, 22*(4), 469–483. — R3 (Qhull/SciPy).
      Referencia bibliográfica contrastada el 2026-11-12 con README oficial de Qhull:
      https://raw.githubusercontent.com/qhull/qhull/master/README.txt.
      Se consultó el manual oficial para la idea QuickHull y su combinación con
      beneath-beyond en Qhull, no se afirma haber leído el PDF completo del artículo.
      Las recurrencias y la memoria de nuestra variante se derivan del código.
- [ ] Preparata, F. P. y Hong, S. J. (1977). Convex hulls of finite sets of points in two and
      three dimensions. *Communications of the ACM, 20*(2), 87–93. — R3 (alternativa por mezcla).
- [ ] Boeing, G. (2017). OSMnx: New methods for acquiring, constructing, analyzing, and
      visualizing complex street networks. *Computers, Environment and Urban Systems, 65*,
      126–139. — R1.
- [ ] Guías del curso 02–09 (docente de Análisis de Algoritmos, U. del Quindío, 2026-2).
      Consultas específicas verificadas para R4 (2026-11-12):
      - [x] Universidad del Quindío (2026). Guía 08, *Algoritmos voraces*, sección 1
        (elementos), sección 5 (Dijkstra, distancia acumulada) y sección 6 (elección
        voraz y subestructura óptima). Copia local en `docs/guias/`; se usa en §5.6 y
        §5.9. La prueba de seguridad de Dijkstra también se apoya en Sedgewick y Wayne.
      - [x] Universidad del Quindío (2026). Guía 09, *Programación dinámica*, sección 9,
        diapositivas 29–32: matriz de costo óptimo 80, estado por subconjuntos, cierre
        del ciclo y complejidad Held-Karp. Se adapta a k visitas con partida implícita;
        el conteo de permutaciones dirigidas, operaciones y memoria propios se deriva
        en §5.2–5.3. No se divide por dos al tratar una matriz asimétrica.

## Documentación técnica (citar con enlace y versión)
- [x] SciPy, documentación ConvexHull (página 1.18.0, instalación local 1.18.1):
      https://docs.scipy.org/doc/scipy/reference/generated/scipy.spatial.ConvexHull.html.
      Consultada el 2026-11-12: Qhull como motor, vertices CCW en 2D, opciones por defecto
      y Qt habilitado; QhullError ante degeneración. Fuente Cython 1.18.0 consultada:
      https://raw.githubusercontent.com/scipy/scipy/v1.18.0/scipy/spatial/_qhull.pyx,
      declaraciones de libqhull_r y llamada nativa qh_new_qhull_scipy sobre points.data.
      No se atribuye el costo medido a bucles Python ni a una compilación C++ no comprobada.
- [x] Qhull, manual oficial, distribución 2020.2, consultado el 2026-11-12 en el repositorio
      oficial porque el dominio qhull.org no respondió:
      https://raw.githubusercontent.com/qhull/qhull/master/html/index.htm (QuickHull 2D +
      beneath-beyond general, aritmética flotante y facetas gruesas),
      https://raw.githubusercontent.com/qhull/qhull/master/html/qh-code.htm (biblioteca
      reentrante C y rendimiento sujeto a hipótesis, no garantía universal O(n log h)),
      https://raw.githubusercontent.com/qhull/qhull/master/html/qh-quick.htm (opciones).
      No se traslada la complejidad interna de Qhull a nuestra implementación 2D.
- [x] CPython 3.14.3, fuente local `Lib/multiprocessing/popen_spawn_win32.py`, líneas
      73–99, inspeccionada el 2026-11-12: CreateProcess ocurre antes de reduction.dump
      del Process. Se usa para justificar serializar tarea/datos antes de crear el hijo;
      no es una afirmación de un fallo de CPython oficialmente reportado o diagnosticado.
- [x] Oracle. *Java SE 21 API*, `java.util.ArrayList`.
      https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html
      Consultada el 2026-11-12: get/size O(1), add al final O(1) amortizado. Usada para
      justificar O(r) amortizado al reconstruir una ruta de r nodos en Dijkstra propio.
- [x] NetworkX 3.7. `single_source_dijkstra_path_length`.
      https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.shortest_paths.weighted.single_source_dijkstra_path_length.html
      Consultada el 2026-11-12: distancias desde un origen a nodos alcanzables, suma de
      atributos de pesos numéricos. Usada solo como oráculo de R2, no para generar la
      matriz de producción ni atribuir complejidad a Java.
- [x] Oracle. *Java SE 21 API*, `java.util.PriorityQueue`.
      https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html
      Consultada el 2026-11-12: offer/poll O(log n), peek/size O(1). Se usa el montículo
      estándar como estructura de apoyo; Dijkstra y su borrado perezoso son propios.
- [x] Sedgewick, R. y Wayne, K. *Algorithms, 4th edition*, sitio del libro, §4.4 Shortest Paths.
      https://algs4.cs.princeton.edu/44sp/
      Consultada el 2026-11-12: relajación, distancias/predecesores, selección del mínimo
      y condición de pesos no negativos. La implementación del sitio usa IndexMinPQ;
      no se copia ni se atribuye a esa cola nuestra memoria O(V+E): el documento deriva
      aparte el costo del borrado perezoso con PriorityQueue.
- [x] Spring Boot. *Gradle Plugin: Running your Application with Gradle*.
      https://docs.spring.io/spring-boot/gradle-plugin/running.html
      Consultada el 2026-11-12: bootRun optimiza el arranque de desarrollo por defecto;
      `optimizedLaunch=false` desactiva esa política. Página consultada 4.1.2, plugin local
      4.1.1. El comando maquina local confirmó TieredStopAtLevel=1 antes del cambio;
      el segundo comando maquina confirmó su ausencia tras desactivarlo, sin actualizar dependencias.
- [x] Python Software Foundation. *Python 3.14 Library Reference*, `multiprocessing`.
      https://docs.python.org/3.14/library/multiprocessing.html
      Consultada el 2026-11-11: secciones Contexts and start methods, Process and exceptions,
      Pipes and Queues y Connection Objects. `spawn` abre un intérprete nuevo, requiere
      funciones importables y datos serializables; `Pipe` transmite objetos con pickle;
      `terminate`/`kill` detienen el hijo y `join` espera su cierre. La documentación
      advierte que terminar un proceso puede corromper su canal: aquí el canal se desecha
      después de cada serie. No se afirma que estas operaciones impongan un límite de
      tiempo real exacto. Desarrollo y pruebas locales con Python 3.14.3; fuente de la serie 3.14.
- [x] Oracle (2023). *The Java Language Specification, Java SE 21 Edition*, §4.2,
      §4.2.1 y §4.2.3. https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html
      Consultada el 2026-11-11: `int` de 32 bits y `double` de formato binary64 de 64 bits;
      §4.2.2 advierte que las operaciones enteras no notifican desbordamiento. Usada en
      R1.1 para motivar 4 y 8 bytes del modelo compacto y evitar productos `int` para V².
      No se atribuye a esta fuente el tamaño de referencias, objetos ni cabeceras JVM;
      8 bytes por cabecera es un supuesto explícito de comparación, no una medición.
- [x] Oracle. Documentación de Java SE 21, `java.lang.Math.hypot(double, double)`.
      https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html#hypot(double,double)
      Consultada el 2026-11-10: devuelve `sqrt(x² + y²)` sin desbordamiento o
      subdesbordamiento intermedio. Usada para la operación numérica de R1.4; la
      complejidad de la búsqueda lineal se deriva por conteo propio en el documento,
      no se atribuye al Javadoc.
- [ ] OSMnx 2.x: `truncate.largest_component`, `routing.add_edge_speeds`,
      `distance.nearest_nodes`, `graph_from_place`, `graph_from_point`.
- [x] OSMnx 2.1.1. *User Reference*, `graph.graph_from_place`,
      `io.save_graphml` e `io.load_graphml`.
      https://osmnx.readthedocs.io/en/stable/user-reference.html#osmnx.graph.graph_from_place
      Consultada el 2026-11-10, junto con las firmas y el código instalado de guardado y
      lectura: `retain_all=False` conserva solo la componente débil más grande;
      `simplify=True` simplifica la topología; GraphML conserva atributos serializados
      y `load_graphml` restaura tipos conocidos. Verificación para la descarga, no para
      atribuir complejidades de SCC ni validar la política de velocidades pendiente.
- [x] NetworkX 3.7. `strongly_connected_components`, documentación y código instalado.
      https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.components.strongly_connected_components.html
      Consultada el 2026-11-10: Tarjan iterativo con mejoras de Tarjan y Zwick (2024) y una
      técnica de WebGraph. La documentación NO declara una cota explícita; el documento
      deriva O(V+E) por inspección del recorrido y distingue memoria auxiliar de la copia.
      La referencia a Nuutila no describe la versión instalada 3.7.
- [x] OSMnx 2.1.1. `truncate.largest_component`, documentación y código instalado.
      https://osmnx.readthedocs.io/en/stable/user-reference.html#osmnx.truncate.largest_component
      Consultada el 2026-11-10: `strongly=True`, comprobación previa, selección con
      `max(..., key=len)` y copia del subgrafo inducido si no era ya conexo.
- [x] OSMnx 2.1.1. *User Reference*, `routing.add_edge_speeds`.
      https://osmnx.readthedocs.io/en/stable/user-reference.html#osmnx.routing.add_edge_speeds
      Consultada el 2026-11-10 junto con el código instalado de la función: imputación por
      tipo de vía, conversión mph y posibilidad de elegir agregación. El código confirma
      que el respaldo sin `fallback` es el promedio entre promedios de tipos con datos y
      que `highway` en lista se clasifica por su primer elemento. La selección por mínimo
      de límites múltiples es un supuesto propio documentado, no el valor predeterminado.
- [ ] Java 21 (Javadoc oficial): `java.util.PriorityQueue` (costo de `offer`/`poll`),
      `List.sort` / `Collections.sort` (TimSort), `Arrays.sort` para primitivos, `ArrayList`,
      `HashMap`.
- [ ] SciPy: `scipy.spatial.ConvexHull` (usa Qhull, escrito en C).
- [ ] Spring Boot 4.1 y ArchUnit (solo si se describen en la arquitectura).

## Datos del catálogo
- [x] OSMnx 2.1.1, código de `_get_network_filter` en `_overpass.py`.
      https://raw.githubusercontent.com/gboeing/osmnx/v2.1.1/osmnx/_overpass.py
      Consultado junto con el código instalado el 2026-11-10: el filtro `drive` excluye
      `highway=track`, `highway=service` y otras restricciones. Usado para describir el
      filtro general, no para atribuir etiquetas no observadas a un acceso particular.
- [ ] Listado FNC de compradores MeCIC 2024, localizado por búsqueda: entrada de Coocafeq
      Pijao, calle 13 #5-13, coincidente con la ficha aportada por el grupo.
      https://federaciondecafeteros.org/wp-content/uploads/2026/02/92-Publicacio%CC%81n-compradores-autorizados-MECIC-01082024.pdf
      El extracto indexado confirma nombre/dirección; lectura íntegra del PDF pendiente.
      No certifica coordenadas de portería ni operación actual.
- [x] Directorio Yoys, ficha «Cooperativa de Caficultores del Quindío - Compra de Café Pijao».
      https://www.yoys.net/phone-57-67544559-agricultural-association-Pijao-COEN135803.html
      Ficha localizada el 2026-11-10 mediante búsqueda, cuyo extracto muestra nombre de
      compra y dirección calle 13 #5-13. Evidencia secundaria de identidad/dirección;
      el grupo aporta la comprobación del punto en mapa, no esta ficha.
- [ ] Fuente de coordenadas (p. ej. OpenStreetMap/mapa consultado) y fecha.
- [ ] Fuente o criterio de la duración de cada atractivo.
- [ ] Fuente de la tabla de velocidades por tipo de vía, si se usa (R1.2).
