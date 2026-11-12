# Revisión superficial de alineación — 2026-11-11

## Alcance y dictamen

Lectura del enunciado y su anexo, extractos pertinentes de las guías locales, specs,
código de R1, arneses de medición y resultados disponibles. No es una auditoría exhaustiva
ni certificación de entrega. No se modificaron implementación, catálogo ni resultados.

La dirección es consistente con el proyecto. No se encontró una decisión de diseño que
obligue a rehacer lo implementado. R1 está avanzado pero incompleto; R2–R5 conservan
partes fundamentales pendientes. Pruebas, teoría y experimentos son entregas distintas.

## Alineación comprobada

- Enunciado §§3–4: Java para algoritmos propios; Python para descarga, preparación,
  verificación y SCC permitida expresamente. Intercambio por archivos, como se permite.
- R1.1: estimaciones con V/E reales y bytes explícitos; se distingue el modelo compacto
  de memoria real JVM y se identifican descargada, componente exportada y grafo Java.
- R1.2: distancia/tiempo, velocidades desconocidas y limitaciones documentadas; los
  tiempos del modelo no se presentan como observaciones de tráfico real.
- R1.3/R1.5: componente fuerte y exportación implementadas; GraphML original preservado.
  Investigación de la implementación interna documentada con la versión de NetworkX.
- R1.4: asociación euclidiana UTM y contraste OSMnx. El enunciado pide reportar y explicar
  >500 m, no eliminar las alertas. Mantener explícita la limitación de último acceso.
- Catálogo: cupos 12/15/8/5, duración turística con fuente o criterio del grupo,
  plazas municipales y revisión humana declaradas. No se certificaron independientemente
  todas las entradas ni la operación actual de los predios.
- Guías 03–04: T(n), conteo de operaciones y O/Θ en el análisis de búsqueda lineal,
  con invariante y derivación; se separan tiempo y memoria auxiliar.
- Guías 05/07, 08 y 09: specs prevén recurrencias y pila, elección voraz y subestructura
  óptima, dp[S][i] y reconstrucción. El plan está alineado, pero R2–R4 siguen pendientes.
- Arquitectura por capas, algoritmos sin librerías resolutoras y registro de IA presentes.

## Pendientes y riesgos

1. **Git/GitHub:** `git rev-parse --show-toplevel` falla en esta copia. El enunciado §3
   exige repositorio compartido con docente e historial por integrante. Resolver con el
   equipo; no se inicializó Git ni se hicieron commits en esta revisión.
2. **Protocolo antes de medir:** `python/preparacion/medicion.py` no implementa corte de
   cinco minutos. Java `Medidor.medir` tampoco limita; `medirConLimite` sí ofrece
   aislamiento y terminación. Aplicar §6.7 a todos los experimentos que se ejecuten.
   Revisar también la regla de una sola medición si una repetición supera un minuto
   después del calentamiento: los bucles actuales no la tratan en ese escenario.
3. **Robustez de entrada:** por inspección, `Grafo.desdeArreglos` rechaza pesos negativos,
   pero no NaN/infinito. Su Javadoc exige coordenadas de longitud n, pero `copiaOVacio`
   no comprueba esa longitud. Python valida los datos preparados; reforzar la frontera
   Java con spec y pruebas. No se constató ese defecto en los datos reales actuales.
4. **Documentación:** `specs/README.md` conserva conteos de pruebas antiguos; R1 contiene
   textos históricos de exportaciones pendientes y el documento técnico aún dice que
   faltan coordenadas del catálogo en su sección SCC. Consolidar estados, sin borrar
   evidencia histórica ni llenar resultados a mano.
5. **Entrega incompleta:** faltan `tamano_red.csv`, tiempos SCC para radios 2/5/10/20 km
   y departamento, y datos de máquina. Solo se encontraron tres CSV de resultados R1
   (memoria, lejanos, verificación). R2–R4, gráficas y funciones centrales de R5 pendientes.
   Completar fuentes teóricas y costos de estructuras auxiliares antes de entregar.

## Verificación y siguiente paso

Pruebas Java filtradas de R1, infraestructura y arquitectura satisfactorias; Python 74/74,
ejecutadas nuevamente. La última suite Java completa anterior tuvo 68 pruebas: 45
aprobadas y 23 fallos por R2–R4 pendientes; no se repitió completa en esta revisión.
No se ejecutaron experimentos ni se alteraron resultados.

Resolver control de versión con el equipo y ajustar el protocolo antes de R1.3
experimental; consolidar documentación y robustez de R1; continuar R2 → R3 → R4 → R5,
verificando antes de medir y completando análisis y discusión en cada etapa.

## Seguimiento posterior

2026-11-11: corregida la regla de una sola muestra si una repetición posterior al
calentamiento supera un minuto, en Python y Java (también JVM aislada). Pruebas de
regresión añadidas antes de implementar; Python 77/77 y Java filtrado con arquitectura
satisfactorio. El corte de 5 min en Python y los demás pendientes siguen abiertos.
No se generaron resultados experimentales ni se modificaron los datos del proyecto.

2026-11-11, siguiente etapa: añadido `medicion.medir_con_limite` para Python con proceso
`spawn`, límite por ejecución, error distinto de timeout y recogida del hijo. Python
105/105; Java filtrado de R1/medición/arquitectura satisfactorio (sin cambios de tareas).
R1.3/SciPy aún pendientes de integrar con el arnés; no se ha ejecutado un experimento.
La publicación inicial Git/GitHub ya se realizó en `main` (commit `d6430f3`); los cambios
posteriores no se han confirmado ni subido sin una nueva solicitud del integrante.
