# Protocolo experimental (sección 6 del enunciado)

Estado: aprobada · Aplica a todas las mediciones de R1–R4. Paquetes reorganizados: 2026-11-10 (sin cambio funcional).

## Reglas y cómo se cumplen en el código

| # | Regla del enunciado | Cómo se cumple |
|---|---|---|
| 6.1 | Mismo requerimiento, misma máquina. Reportar CPU, RAM, SO y versión del lenguaje. | `./gradlew bootRun --args='maquina'` → `results/maquina_java.csv` (incluye versión de Java, JVM y argumentos como `-Xmx2g`); `python -m preparacion maquina` → `results/maquina_python.csv` (R1.3 y SciPy). Si un requerimiento se mide en otra máquina, guardar una copia en `results/rN/`. |
| 6.2 | Solo se mide el algoritmo. | `service.measurement.Medidor.medir(() -> algoritmo(...))` cronometra solo el `Supplier`. Cargar el grafo, leer CSV, generar puntos, convertir a `numpy` para SciPy e imprimir quedan **fuera**. Nada de `System.out` en `service`, `algorithm` o `model` (ArchUnit), salvo el canal de medición de `ProcesoAislado`. |
| 6.3 | 5 repeticiones, mediana. Más de 1 min → una sola vez. | `Constantes.REPETICIONES = 5`, `UMBRAL_UNA_SOLA_MEDICION_S = 60`. **Calentamiento** (`CALENTAMIENTOS = 1`): antes de medir se ejecuta una vez sin contarla, porque la JVM compila con JIT el código que más se usa y la primera ejecución es más lenta; si esa ejecución ya pasa de 1 min, se toma como la única medición. Python usa la misma política. Columna `repeticiones` = repeticiones medidas. |
| 6.4 | Una sola unidad de tiempo. | **Milisegundos** (`tiempo_ms`) con `System.nanoTime()` (Java) y `perf_counter_ns` (Python). |
| 6.5 | Semilla fija documentada. | `Constantes.SEMILLA = 2026` (Python: `config.SEMILLA`). Toda aleatoriedad usa `new Random(semillaDerivada)`; nunca `Math.random()`. La semilla usada se guarda en el CSV. |
| 6.6 | Verificar antes de medir. | Cada `ExperimentoRxService` ejecuta su verificación y **aborta** si falla. Las verificaciones contra librerías (NetworkX, OSMnx, SciPy) están en `python/`. |
| 6.7 | Más de 5 min → detener, "no completada". | `Medidor.medirConLimite(TareasTsp.FuerzaBruta.class, instancia)` corre la tarea en una **JVM hija** con el mismo classpath y memoria, y la termina si una ejecución pasa de 300 s. Columna `estado = "no completada"`, `tiempo_ms` vacío. |
| 6.8 | Resultados en CSV/JSON; tablas y gráficas desde esos archivos, con unidad y párrafo de discusión. | `results/rN/*.csv` (vía `ResultadosRepository`) → `python -m preparacion graficas` → `results/figuras/*.png`. Cada tabla/figura del documento lleva un párrafo: ¿coincide con la teoría? si no, ¿por qué? |

## Condiciones de ejecución (Java)

- Medir siempre con `./gradlew bootRun --args='rN'`: fija `-Xms2g -Xmx2g -Xss16m` y el
  directorio de trabajo. Nunca medir desde JUnit, el IDE o el depurador.
- `System.gc()` antes de cada ejecución (lo hace `Medidor`) para que la basura de la anterior
  no se recoja dentro de la siguiente.
- El resultado de cada ejecución se conserva (`Medicion.resultado`) para verificarlo y para
  que la JVM no elimine el cálculo por no usarse.

## Convenciones de archivos

- Columnas del anexo del enunciado: se pueden **ampliar, nunca reducir**. Columnas extra
  estándar: `repeticiones`, `estado`, `semilla`.
- Nombres de columnas en `snake_case`, sin tildes. Separador `,`, decimales con `.`.
- Los CSV de `results/` se versionan en git (son evidencia). Nunca se editan a mano: si un
  número está mal, se corrige el código y se vuelve a ejecutar el comando.

## Convenciones de figuras

- Título, ejes con nombre **y unidad** (`Tiempo (ms)`), leyenda cuando hay más de una serie.
- Escala logarítmica cuando los valores abarcan varios órdenes de magnitud (R4), indicándolo.
- Las ejecuciones "no completadas" se marcan explícitamente (no se omiten en silencio).
- Formato PNG, 150 dpi, en `results/figuras/`, nombre `rN_<contenido>.png`.

## Recomendaciones para mediciones estables

- Cerrar programas pesados, conectar el portátil a la corriente y no usarlo mientras mide.
- Ejecutar desde la terminal.
- Comparar Java contra SciPy (C) es comparar lenguajes además de algoritmos: decirlo en la
  discusión de R3, igual que el efecto del calentamiento JIT.

## Checklist por tabla o figura

- [ ] Se generó desde un CSV de `results/` por un comando.
- [ ] Indica la unidad de tiempo.
- [ ] Indica máquina (referencia a `maquina_java.csv` / `maquina_python.csv`) y semilla.
- [ ] Tiene un párrafo: lo esperado según la complejidad, lo medido y la explicación.
