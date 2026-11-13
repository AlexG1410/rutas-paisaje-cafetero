# R4 — Ruta óptima de recorrido (viajante asimétrico)

Estado: implementada, verificada, medida y documentada (2026-11-12).
Ocho tamaños exactos con costos coincidentes, veinte instancias de calidad e itinerario
final de siete atractivos dentro de 660 min. Evidencia en `results/r4/` y capítulo 5.
Peso: 25 %.

## Decisiones y precisiones del bloque aprobado

- Vecino más cercano confirmado. Itinerario inicial desde `cab_armenia` con ocho
  atractivos: `atr_museo_oro_quimbaya`, `atr_jardin_botanico`, `atr_valle_cocora`,
  `atr_colina_iluminada`, `atr_bejuco_canasto`, `atr_parque_vida`, `atr_calle_real`,
  `atr_alto_cruz`, en ese orden de entrada. Retirar el atractivo cuya eliminación
  más reduce la jornada óptima recalculada; empate conserva el primero de la lista
  vigente. Registrar cada retiro y la duración antes/después. Es una política local
  de reducción, no optimización de la satisfacción ni del número de visitas.
- Los algoritmos reciben matriz cuadrada no vacía y lugares distintos, no nulos,
  en rango y con partida. Validar forma O(N) de la matriz general y valores de la
  submatriz seleccionada O(k²): segundos finitos no negativos; diagonal seleccionada
  cero. Rechazar inválidos con IllegalArgumentException; no modificar entrada.
  La validación por pares también detecta índices repetidos sin memoria O(N).
  Sumatorias no representables como double finito se rechazan explícitamente.
- Fuerza bruta recursiva con intercambios propios, sin poda; costo completo O(k)
  en cada hoja. Empate conserva primera hoja; incluye viaje de regreso.
- Held-Karp: arreglos planos dp double y predecesor int de k·2^k celdas; posiciones
  locales del vector lugares, no ids globales. Recorrer máscaras, finales y anteriores
  crecientemente; actualizar solo ante mejora estricta. Empate de cierre: menor
  posición local final. Puede devolver otro recorrido óptimo distinto del de fuerza bruta.
  Resolver admite k<=20 (límite explícito de recursos para uso local, experimento<=12);
  entradasTabla admite 0<=k<=57 y rechaza fuera de rango antes de desplazar/multiplicar.
- La validación añade O(N+k²) al tiempo; las cotas de resolución se indican separadas.
  Memoria estimada HK: 8·celdas de dp + 4·celdas de predecesores; excluye cabeceras,
  alineación, entrada y salida. Memoria auxiliar asintótica incluye pila cuando aplica.
- El servicio valida identidad entre catálogo y ids de matriz, índices únicos y datos
  de matriz antes de planear/experimentar; datos faltantes/incoherentes -> IllegalStateException.
  Solicitud inválida (ids repetidos/desconocidos, partida seleccionada como visita,
  categoría no atractivo, visita sin duración positiva) -> IllegalArgumentException.
  `planear` y `comparar` admiten 0..8 atractivos internamente; el controlador R5
  conserva su selección pública de 6..8. `comparar` no retira; `planear` retira hasta
  <=660 min, igualdad incluida. Partida sin visita; paradas numeradas desde 1 y regreso.
  Si óptimo cero: diferencia porcentual cero cuando ambas jornadas cero; caso positivo
  sobre cero es error, no porcentaje ficticio. Rechazar porcentajes no representables
  como double finito; dividir antes de multiplicar por 100 para evitar desbordes intermedios.
- Experimento: ids en orden de matriz; partida Armenia. Por cada k=5..12, barajar
  todos los demás lugares con `new Random(SEMILLA+k)` y tomar k; una instancia por k.
  Calidad: k=5,8,10,12 y cinco instancias, semilla SEMILLA+1000*k+numero (numero=1..5).
  Guardar ids en orden y semilla para reproducir; no modificar catálogo ni matriz R2.
- Verificar primero manuales T4-a/b/c y todas las instancias con HK y validación
  independiente de pertenencia/costo del recorrido. Para cada instancia exacta,
  verificar también fuerza bruta en JVM aislada (una ejecución, sin calentamiento,
  límite de 300 s, sus tiempos se descartan). Si termina, exigir igualdad con HK
  antes de medir la serie oficial. Un timeout en verificación queda explícito y corta
  k mayores para ese método; no se presenta como serie temporal completada.
  Tras cada medición completada verificar ruta/costo y coincidencia con la referencia.
  Verificación y medición comparten arnés Medidor, nunca cronómetros ad hoc.
- Publicar salidas solo al completar verificaciones (un error no sobrescribe evidencia
  anterior); timeout es un resultado experimental, excepción/OOM no lo es.

### CSV del bloque (columnas fijadas en ResultadosRepository)

Infraestructura: sobrecarga `ResultadosRepository.guardar(nombre,columnas,filas)`
admite filas vacías con cabecera explícita (retiros sin eliminaciones). La sobrecarga
existente conserva su comportamiento. `EntornoService.registrarEn(nombre)` de alcance
de paquete registra entorno en una ruta elegida por el caso de uso; registrar() se conserva.

- `instancias.csv`: tipo,k,numero,semilla,partida,lugares (ids separados por `;`, incluye partida).
- `exactas.csv`: columnas base del final de esta spec más repeticiones_fuerza_bruta,
  repeticiones_held_karp,semilla,fase_fuerza_bruta,fase_held_karp. Fases medicion,
  verificacion o no_ejecutada. Métodos cortados: tiempo/costo vacíos, no completada,
  fase no_ejecutada y repeticiones 0. `coinciden` vacío si falta un resultado oficial.
- `memoria_held_karp.csv`: k,entradas,bytes_dp,bytes_predecesores,bytes_estimados.
- `calidad_detalle.csv`: k,numero,semilla,costo_held_karp_s,costo_heuristica_s,brecha_pct,
  recorrido_held_karp,recorrido_heuristica (ids separados por `;`). Óptimo cero y ambos
  cero -> brecha 0; una diferencia positiva sobre cero aborta con explicación.
- `calidad_heuristica.csv`: k,instancias,brecha_promedio_pct,brecha_maxima_pct.
- `itinerario.csv`: orden,id_lugar,lugar,desplazamiento_min,visita_min,tiempo_acumulado_h.
- `itinerario_resumen.csv`: partida,atractivos_iniciales,atractivos_finales,retirados,
  total_visitas_min,total_desplazamientos_min,duracion_jornada_min,limite_min,
  duracion_inicial_min (listas de ids separadas por `;`).
- `retiros.csv`: paso,id_retirado,duracion_antes_min,duracion_despues_min,reduccion_min.
  Derivar secuencia con la misma política de planear, fuera de la medición.
- `voraz_vs_optimo.csv`: caso,k,costo_optimo_s,costo_voraz_s,brecha_pct,
  recorrido_optimo,recorrido_voraz. Siempre ejemplo T4-b; añadir primera instancia
  real de calidad con brecha estrictamente positiva si existe.
- `maquina_java.csv`: capturar dentro de results/r4/ con EntornoService al ejecutar r4,
  sin pisar la evidencia de máquina de bloques anteriores.
- `graficas` produce `r4_tiempo_vs_k.png` con eje y logarítmico, timeout marcado sin
  tiempo cero; `results/r4/cocientes_tiempos.csv`: metodo,k_anterior,k,razon_observada,
  razon_modelo; solo pares consecutivos con ambas mediciones oficiales completadas
  y positivas. Modelos costo completo BF: (k/(k−1))·k; HK: 2·(k/(k−1))².
  Son cocientes del modelo, no promesas para las mediciones.

## Objetivo

Salir de un lugar, visitar k lugares y regresar con el menor tiempo total según la matriz de
R2. Comparar fuerza bruta, Held-Karp y una heurística voraz; armar el itinerario de un día.

## Contrato

```java
// algorithm.Tsp (ya implementado)
static double costoRecorrido(double[][] matriz, int[] recorrido)
static boolean esRecorridoValido(int[] recorrido, int[] lugares)
// algorithm (PROPIOS)
TspFuerzaBruta.resolver(double[][] matriz, int[] lugares) -> ResultadoTsp
HeldKarp.resolver(double[][] matriz, int[] lugares)       -> ResultadoTsp
HeldKarp.entradasTabla(int k) -> long                       // k · 2^k
VecinoMasCercano.resolver(double[][] matriz, int[] lugares) -> ResultadoTsp
// model.ResultadoTsp(double costoS, int[] recorrido)
// service.TareasTsp.{FuerzaBruta, HeldKarpTarea, Heuristica}: para Medidor.medirConLimite
```

Experimento: `service.ExperimentoR4Service` (`./gradlew bootRun --args='r4'`);
itinerario: `service.ItinerarioService` (también lo usa la app).

`matriz[i][j]` = segundos de i a j (no simétrica). `lugares[0]` = partida; `lugares[1..k]`
= los k lugares. `recorrido` = `{partida, ..., partida}`, longitud k + 2, cada lugar una vez.

## Comportamiento esperado

### R4.1 Soluciones exactas
1. **Fuerza bruta**: generador **propio** de permutaciones de `lugares[1..k]` (p. ej.
   algoritmo de Heap o recursión con intercambios). Evalúa todas; en empate conserva la
   primera encontrada.
2. **Held-Karp** (notación de la guía 09, con S sobre los k lugares y la partida 0 implícita):
   - `dp[{j}][j] = M[0][j]`
   - `dp[S][j] = min_{i ∈ S − {j}} ( dp[S − {j}][i] + M[i][j] )` para |S| ≥ 2, j ∈ S
   - `OPT = min_{j} ( dp[U][j] + M[j][0] )`, U = todos los k lugares
   - S como máscara de bits; tabla de **predecesores** para reconstruir el orden de visita.
3. Antes de implementar, justificar en el documento la **subestructura óptima**: si el
   recorrido óptimo que visita S y termina en j pasa por i justo antes de j, entonces su
   prefijo es óptimo para (S − {j}, i); si no, se reemplaza por uno mejor y se contradice
   la optimalidad.
4. k = 0 → `{p, p}`, costo 0. k = 1 → `{p, a, p}`.

### R4.2 Heurística voraz
Heurística confirmada: **vecino más cercano** (desde el lugar actual, ir al no visitado con
menor `M[actual][j]`; empate → el que aparece primero en `lugares`; al final regresar).
Justificar la elección en el documento con los elementos voraces de la guía 08.

### R4.3 Calidad
`brecha_% = 100 · (costo_heurística − costo_HK) / costo_HK`, para k = 5, 8, 10, 12, con ≥ 5
instancias por k. Reportar promedio y máximo.

### R4.4 Itinerario de un día
1. Partida (por defecto `cab_armenia`) y entre 6 y 8 atractivos elegidos por el equipo.
2. Ordenar con Held-Karp. Duración = Σ desplazamientos (matriz, en min) + Σ visitas
   (`duracion_visita_min` del catálogo). La partida no tiene visita.
3. Si supera 11 h (660 min): retirar atractivos con un **criterio justificado**, recalcular
   con Held-Karp y reportar la duración final. Criterio sugerido: en cada paso retirar el
   atractivo cuya eliminación más reduce la duración total (probar cada uno es barato con
   k ≤ 8); alternativa: prioridad por interés definida en el catálogo.
4. Tabla del anexo: orden, lugar, desplazamiento desde el anterior (min), visita (min),
   tiempo acumulado (h), fila de regreso; al final totales de visita, desplazamiento y jornada.

### R4.5 Voraz frente a óptimo
Ejemplo donde el vecino más cercano no es óptimo (T4-b y, si aparece, uno con datos
reales). Explicar por qué en Dijkstra la elección voraz sí garantiza el óptimo (propiedad de
elección voraz + subestructura óptima, guía 08 sección 6) y aquí no.

## Restricciones

- Las tres son implementación propia (`java.lang`/`java.util`, verificado por ArchUnit).
- Instancias de experimento: partida Armenia; k lugares al azar entre **todas** las
  categorías del catálogo (sin Armenia), con semilla derivada de `Constantes.SEMILLA` y guardadas
  en `results/r4/instancias.csv`.

## Casos de prueba

| ID | Entrada | Salida esperada | Origen |
|---|---|---|---|
| T4-a | matriz A,B,C,D de la guía 09: `[[0,10,15,20],[10,0,35,25],[15,35,0,30],[20,25,30,0]]`, lugares [0,1,2,3] | costo 80 (fuerza bruta y Held-Karp); recorrido válido de costo 80 | tabla de la guía 09 |
| T4-b | `[[0,7,8,6],[7,0,9,2],[8,9,0,1],[6,2,1,0]]`, lugares [0,1,2,3] | vecino más cercano: 23, `[0,3,2,1,0]`; óptimo: 18 | a mano (A→D→C→B→A vs A→B→D→C→A) |
| T4-c | asimétrica `[[0,1,9],[9,0,1],[1,9,0]]`, lugares [0,1,2] | 3, `[0,1,2,0]` (el sentido inverso cuesta 27) | a mano |
| T4-d | lugares [0] y [0,1] | `[0,0]` costo 0; `[0,1,0]` costo M01+M10 | borde |
| T4-e | lugares no consecutivos de una matriz 8×8: [5,2,7,0] | fuerza bruta = Held-Karp | indexación |
| T4-f | 40 matrices asimétricas al azar, k = 1..7 (semilla) | fuerza bruta = Held-Karp; recorridos válidos; `Tsp.costoRecorrido == costoS` | verificación cruzada |
| T4-g | `HeldKarp.entradasTabla(k)` | `k · 2^k` | definición |
| T4-h | heurística en T4-f | recorrido válido y costo ≥ óptimo | propiedad |
| T4-i | matriz/índices inválidos, null, repetidos, negativos, infinito, suma desbordada | error explícito | validación |
| T4-j | todos los costos iguales, cero, orden no consecutivo | empates deterministas, entrada intacta | manual |
| T4-k | matriz asimétrica pequeña | óptimos contra enumeración independiente sin intercambios | oráculo |
| T4.I-a | itinerario sintético con 0/1/8 atractivos | totales, retorno, retiro y empate correctos | integración |
| T4.I-b | ids inválidos y datos incoherentes | error sin publicación | integración |
| T4.E-a | medidor sustituido y datos sintéticos | CSV reproducibles, verificaciones previas/posteriores | integración |
| T4.E-b | timeout verificación/medición, resultado incorrecto | cortar solo método afectado, sin cifras ficticias | integración |
| T4.F-a | tabla de tiempos con timeout | eje log, marcas y cocientes correctos | Matplotlib |

## Criterios de aceptación

- [x] T4-* en verde (`TspTest`).
- [x] Fuerza bruta y Held-Karp dan el mismo costo óptimo en todos los k que la fuerza bruta
      resuelve (columna `coinciden`).
- [x] Tabla y figura tiempo vs k (5..12) para ambos; mayor k resuelto por cada uno;
      "no completada" si pasa de 5 min (`Medidor.medirConLimite`).
- [x] Memoria de Held-Karp: entradas de la tabla y bytes estimados por k (y opcionalmente
      la memoria medida de la JVM, en una ejecución aparte de la de tiempo).
- [x] Tabla de calidad de la heurística; itinerario con totales; contraejemplo voraz.

## Análisis teórico (va al documento técnico)

- Fuerza bruta: Θ(k!) recorridos, Θ(k·k!) operaciones si cada costo se calcula de nuevo;
  memoria O(k).
- Held-Karp: Θ(k² · 2^k) tiempo, Θ(k · 2^k) memoria (dp + predecesores); recurrencia de
  arriba; fuente consultada: guía 09, sección 9, Universidad del Quindío (2026).
  Conteo ajustado a esta implementación en el capítulo 5. El artículo de 1962 sigue
  como candidato no verificado en `docs/fuentes.md`; no se cita como original leído.
- Vecino más cercano: Θ(k²) tiempo, O(k) memoria; sin garantía de optimalidad.
- Comparar con lo medido: modelos de cociente t(k+1)/t(k): (k+1)²/k en fuerza bruta
  con costo completo por hoja y 2·((k+1)/k)² en Held-Karp. No se exige coincidencia
  numérica ni monotonía en cada par de la serie finita; discutir las desviaciones.

## Experimento

- `exactas.csv`: `k, tiempo_fuerza_bruta_ms, estado_fuerza_bruta, tiempo_held_karp_ms,
  estado_held_karp, costo_fuerza_bruta_s, costo_held_karp_s, coinciden`. Al primer k en que
  un método no completa, no se prueban k mayores para ese método (se marcan "no completada").
- `calidad_heuristica.csv`: `k, instancias, brecha_promedio_pct, brecha_maxima_pct`.
- Figura `r4_tiempo_vs_k.png`, eje y en escala logarítmica.

## Decisiones resueltas

Vecino más cercano, ocho atractivos y política de retiro aprobados por el usuario
al iniciar este bloque. Empates y contratos precisados arriba antes de implementar.
