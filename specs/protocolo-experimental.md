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
| 6.7 | Más de 5 min → detener, "no completada". | Java: `Medidor.medirConLimite` en JVM hija. Python: `medicion.medir_con_limite` en proceso `spawn`. Terminan al hijo si una ejecución supera 300 s; `estado = "no completada"`, `tiempo_ms` vacío. Integrar la versión con límite en cada experimento; los métodos directos no lo aplican. |
| 6.8 | Resultados en CSV/JSON; tablas y gráficas desde esos archivos, con unidad y párrafo de discusión. | `results/rN/*.csv` (vía `ResultadosRepository`) → `python -m preparacion graficas` → `results/figuras/*.png`. Cada tabla/figura del documento lleva un párrafo: ¿coincide con la teoría? si no, ¿por qué? |

## Condiciones de ejecución (Java)

Precisión del aislamiento Java para R2 (2026-11-12): la hija envía `INICIO` después
de preparar/deserializar entradas y recolectar basura, justo antes de cronometrar cada
llamada. El padre aplica el límite de 300 s a esa ejecución, no a arranque/preparación.
Entre ejecuciones y durante arranque/transferencia se permiten 60 s de infraestructura;
agotarlos es error, no timeout experimental. También se rechaza como no completada una
duración reportada >límite si el padre recibió tarde el mensaje. Se termina y recoge
el proceso propio en éxito, timeout o error. Prueba P6.J-a: constructor de tarea lento
y función rápida con límite reducido no agotan el límite experimental; P6.J-b: tarea
lenta sí se termina y no deja proceso activo. Estas son pruebas, no mediciones reales.

### Regla de ejecución superior a un minuto (precisión 2026-11-11)

La regla 6.3 se aplica a cualquier ejecución, no solo al calentamiento ni a la primera
repetición. Si una ejecución terminada supera estrictamente 60 s, no se hacen más
repeticiones y se informa únicamente su tiempo y resultado (`repeticiones=1`). Si ya
había repeticiones breves, no se mezclan con esta en una mediana: se conserva solo la
ejecución larga. A exactamente 60 s la excepción no se activa. Esta aclaración no cambia
firmas públicas ni columnas CSV; corrige la aplicación de la regla existente.

Casos de prueba del arnés (umbrales reducidos o reloj simulado, no experimentos):

| ID | Caso | Resultado |
|---|---|---|
| P6.3-a | calentamiento breve y primera repetición larga | una sola muestra larga y su resultado; ninguna llamada posterior |
| P6.3-b | calentamiento y repetición inicial breves; siguiente larga | descartar muestras breves; conservar solo la larga y detener |
| P6.3-c | tiempo exactamente igual al umbral (reloj simulado Python) | continuar las cinco repeticiones |
| P6.3-d | casos a/b en JVM aislada | misma política, sin cambiar límite ni canal de resultados |

Estado: implementada y verificada en Python y Java, incluida la JVM aislada.
El corte Python de cinco minutos también está implementado (sección siguiente).
La evidencia e historial de verificaciones están en `docs/registro-ia.md`.

- Medir siempre con `./gradlew bootRun --args='rN'`: fija `-Xms2g -Xmx2g -Xss16m` y el
  directorio de trabajo. Nunca medir desde JUnit, el IDE o el depurador.
- `bootRun.optimizedLaunch=false`: no limitar el JIT al nivel 1 para favorecer arranque
  de desarrollo. Las JVM hijas usan también Xms=Xmx del padre y Xss16m. Registrar de
  nuevo máquina/argumentos antes de medir al cambiar esta configuración; no comparar
  tiempos entre configuraciones diferentes sin explicarlo. R1.3 Python no se ve afectado.
- `System.gc()` antes de cada ejecución (lo hace `Medidor`) para que la basura de la anterior
  no se recoja dentro de la siguiente.
- El resultado de cada ejecución se conserva (`Medicion.resultado`) para verificarlo y para
  que la JVM no elimine el cálculo por no usarse.

## Medición Python con límite (protocolo 6.7)

### Contrato

Contrato: `preparacion.medicion.medir_con_limite(fn, *args,
repeticiones=REPETICIONES, calentamientos=CALENTAMIENTOS,
umbral_una_s=UMBRAL_UNA_SOLA_MEDICION_S, limite_s=LIMITE_TIEMPO_S, **kwargs) -> Medicion`.
Conserva `medir` para uso directo; los experimentos R1.3 y SciPy deberán usar la versión
con límite. No cambia columnas CSV ni añade dependencias.

- Un proceso hijo por serie con `multiprocessing` y contexto `spawn` explícito (Windows).
  Función, entradas y resultado deben ser serializables; usar funciones/clases importables,
  no lambdas ni funciones locales. El llamador CLI conserva su guardia `if __name__`.
- Lectura de archivos y construcción de entradas antes de llamar. Arranque, importaciones,
  serialización, recolección de basura y comunicación quedan fuera del tiempo registrado;
  solo `fn(*args, **kwargs)` se cronometra con `perf_counter_ns` en el hijo.
- El hijo avisa justo antes de cada ejecución y envía después su duración. El padre
  supervisa cada ejecución (también calentamiento), no la suma de toda la serie. Al
  superar `limite_s` termina y recoge el hijo; resultado `None`, `completada=False`,
  `tiempo_ms=None`, estado `no completada`. Puede conservar muestras breves anteriores
  como diagnóstico, pero nunca reportarlas como mediana de una serie completada.
- Si el hijo devuelve una duración >límite pese a retrasos de planificación del padre,
  también se marca no completada. Esto no es una garantía de temporización de tiempo real.
- Entre ejecuciones se admite hasta 60 s de espera por preparación/transferencia; si
  no hay respuesta es error de infraestructura, no tiempo de algoritmo ni timeout
  experimental. El resultado final se recibe fuera del cronómetro.
- Cinco muestras y un calentamiento por defecto. La regla P6.3 de única ejecución >60 s
  sigue vigente, sin mezclar muestras breves. Se devuelve el último resultado terminado.
- Una excepción de la tarea, fallo de serialización o salida inesperada del hijo produce
  un error claro; no se confunde con timeout. El hijo se recoge y se cierran conexiones
  tanto en éxito como en error/interrupción. No se terminan procesos ajenos al arnés.
- Rechazar repeticiones no enteras/positivas, calentamientos no enteros/no negativos,
  límites o umbrales no finitos/no positivos y tareas no invocables antes de crear un hijo.

| ID | Caso sintético del arnés Python | Resultado esperado |
|---|---|---|
| P6.7-a | suma con args/kwargs, un calentamiento y cinco repeticiones | resultado correcto en PID distinto, cinco muestras; hijo recogido |
| P6.7-b | tarea lenta en calentamiento o repetición | se termina, estado no completada, sin mediana ni resultado |
| P6.7-c | calentamiento y una repetición breves, siguiente excede límite | conserva solo muestra breve previa como diagnóstico; sin mediana final |
| P6.7-d | tarea lanza excepción o sale abruptamente | error claro y sin hijos del arnés activos |
| P6.7-e | entrada se prepara lentamente en el hijo; algoritmo rápido | preparación no consume el límite experimental |
| P6.7-f | parámetros inválidos o función no serializable | error sin dejar hijo activo |
| P6.7-g | tarea supera umbral de única muestra tras una repetición breve | solo muestra larga y resultado de esa llamada |
| P6.7-h | función real largest_component sobre grafo de prueba | devuelve la SCC esperada sin peticiones HTTP |
| P6.7-i | preparación no responde dentro de espera de infraestructura reducida en prueba | error claro, no estado experimental no completada; hijo recogido |

Estado (2026-11-11): implementado y verificado en Windows con Python 3.14.3. Nuevos
casos fallaron por función ausente antes de implementar; P6.7-a a i pasan después,
incluidos errores de serialización/deserialización. Suite Python completa: 105/105.
Pruebas Java filtradas de R1/medición/arquitectura satisfactorias (tarea sin cambios).
Estos casos usan tareas sintéticas y un grafo OSMnx pequeño, no son mediciones del
proyecto. R1.3 ya está integrado y ejecutado; la integración SciPy sigue pendiente.

Fuente de `spawn`, `Pipe`, `terminate`, `join` y serialización: documentación oficial
Python 3.14 de `multiprocessing` (enlace y alcance en `docs/fuentes.md`). El hijo es
daemon: no debe lanzar otros procesos; no se usa un pool para paralelizar experimentos.
La preparación y el transporte requieren copias/serialización de entradas y resultados;
su costo de tiempo y memoria existe, pero no se atribuye al algoritmo medido. No reutilizar
el canal después de un corte: se desecha junto al hijo en cada llamada.

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
