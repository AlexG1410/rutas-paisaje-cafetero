# Rutas del Paisaje Cultural Cafetero

Proyecto final de **Análisis de Algoritmos** — Ingeniería de Sistemas, Universidad del
Quindío, 2026-2.

Planificación de rutas sobre la red vial real del Quindío (OpenStreetMap) con análisis teórico
y experimental de: Dijkstra (lineal y con cola de prioridad), envolvente convexa por divide y
vencerás, y el viajante asimétrico (fuerza bruta, Held-Karp y una heurística voraz).

**Integrantes:** _(completar)_ · **Docente:** _(completar)_ · **Fecha de descarga de la red:**
_(se registra en `data/processed/metadatos.json`)_

## Tecnología

- **Java 21 + Spring Boot 4.1 + Gradle**: todos los algoritmos (implementación propia), los
  experimentos y la aplicación. Arquitectura en 3 capas con paquetes tradicionales en inglés:
  `controller`/`cli` → `service` → `repository`, con `model` y `algorithm` como lógica pura.
- **Python 3.11+** (`python/`): solo descarga de la red con OSMnx, componente fuertemente
  conexa, exportación a CSV, verificación contra NetworkX/OSMnx/SciPy y figuras. El enunciado
  exige la descarga en Python y permite leer el grafo exportado desde otro lenguaje.

## Requisitos

- JDK 21 (el wrapper de Gradle se descarga solo la primera vez).
- Python 3.11 o superior.
- Internet para instalar dependencias, descargar la red (paso 4) y cargar Leaflet y las
  teselas del mapa en la aplicación. Los algoritmos reutilizan los datos locales procesados.

## Instalación

```bash
./gradlew test                       # Windows: gradlew.bat test

cd python
python -m venv .venv
source .venv/bin/activate            # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python -m pytest tests
cd ..
```

### Windows (PowerShell)

`JAVA_HOME` debe apuntar al directorio del **JDK 21**, no a su carpeta `bin`.
Comprobar el Java que realmente usa Gradle con `--version` (puede ser distinto del que
aparece al ejecutar `java -version` si el `PATH` contiene otra instalación):

```powershell
.\gradlew.bat --version
.\gradlew.bat test --console=plain

cd python
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pip check
.\.venv\Scripts\python.exe -m pytest tests
cd ..
```

Usar directamente el intérprete de `.venv` evita instalar dependencias en el Python global
y no requiere cambiar la política de ejecución de PowerShell para activar el entorno.
La instalación de dependencias también requiere internet.

### Pruebas de infraestructura y pruebas de algoritmos

Los algoritmos pendientes lanzan `UnsupportedOperationException`: sus pruebas deben seguir
visibles y fallarán hasta implementarlos. Eso no equivale a un fallo de instalación.
Para comprobar por separado la infraestructura Java, desde la raíz:

```powershell
.\gradlew.bat test --console=plain --tests '*ArquitecturaTest' --tests '*RepositoriosTest' --tests '*GrafoTest' --tests '*CatalogoServiceTest' --tests '*MedidorTest'
```

La suite completa sigue siendo `gradlew.bat test`; no se deshabilitan pruebas para hacerla
pasar. El informe HTML queda en `build/reports/tests/test/index.html` y corresponde a la
última ejecución de la tarea `test`.

Estado del entorno comprobado el 2026-11-10: Java 21.0.10 con Gradle 9.7.1 compila y aprueba
26/26 pruebas de infraestructura. Python 3.14.3 en `.venv` aprueba 5/5 pruebas y `pip check`
no reporta incompatibilidades. La descarga incorpora otras 15 comprobaciones automatizadas:
la suite Python tras integrar el procesamiento aprueba 65/65, sin peticiones HTTP desde las pruebas.
Tras implementar la búsqueda del nodo más cercano (R1.4),
sus 6 pruebas también pasan: infraestructura más R1.4, 32/32 aprobadas. La suite Java completa
tiene 23 fallos por los demás algoritmos todavía pendientes (56 pruebas en total); no es un
proyecto completamente implementado.

## Reproducir los resultados

Los comandos Java se ejecutan desde la raíz; los de Python, desde `python/`.

| Paso | Comando | Produce |
|---|---|---|
| 1 | `./gradlew bootRun --args='maquina'` | `results/maquina_java.csv` |
| 2 | `python -m preparacion maquina` | `results/maquina_python.csv` |
| 3 | `./gradlew bootRun --args='catalogo'` | valida `data/catalogo/lugares.csv` |
| 4 | `python -m preparacion descargar` | `data/raw/` (red cruda, fuera de git) |
| 5 | `python -m preparacion procesar` | `data/processed/` (componente, pesos, `lugares_utm.csv`) |
| 6 | `python -m preparacion r1` | `results/r1/tamano_red.csv`, `tiempo_componente.csv` |
| 7 | `./gradlew bootRun --args='r1'` | `data/processed/lugares_nodos.csv`, `results/r1/` |
| 8 | `python -m preparacion verificar-lugares` | `results/r1/verificacion_lugar_nodo.csv` |
| 9 | `./gradlew bootRun --args='r2'` | `results/r2/` (incluye `matriz_tiempos.csv`) |
| 10 | `python -m preparacion verificar-dijkstra` | `results/r2/verificacion_networkx.csv` |
| 11 | `./gradlew bootRun --args='r3'` | `results/r3/` (puntos, vértices, tiempos propios) |
| 12 | `python -m preparacion envolvente-scipy` | `results/r3/envolvente.csv` |
| 13 | `./gradlew bootRun --args='r4'` | `results/r4/` |
| 14 | `python -m preparacion graficas` | `results/figuras/` |

Del paso 6 en adelante nada vuelve a descargar la red: todo lee `data/processed/`. Semilla de
los datos aleatorios: `2026` (`Constantes.java`).

### Descarga de la red del departamento

Desde `python/`, en PowerShell:

```powershell
.\.venv\Scripts\python.exe -u -m preparacion descargar
```

Usa OSMnx con `network_type="drive"`, `retain_all=True` y `simplify=True`: vías para
vehículos, sin seleccionar todavía la componente conexa más grande y con simplificación
topológica de OSMnx. Guarda `data/raw/quindio.graphml`, que incluye el atributo JSON
`metadatos_descarga` (fecha UTC de adquisición, consulta, opciones, versiones y conteos).
La fecha no representa una instantánea histórica de OSM: pueden usarse respuestas HTTP
de la caché local de OSMnx.

Una copia existente válida se reutiliza sin peticiones y sin cambiar su fecha. Si está
dañada o no corresponde a la configuración, el comando informa el problema y termina con
código 1; no la sobrescribe. Para obtener una copia nueva, conservar o mover manualmente
la anterior antes de ejecutar otra vez. El archivo final solo aparece después de guardar
y volver a leer correctamente un temporal.

La descarga real necesita acceso a Nominatim y Overpass; puede tardar varios minutos y
depende de su disponibilidad. No se desactiva la verificación de certificados TLS.
La descarga no calcula velocidades, tiempos de viaje ni la componente fuerte.
`asignar_pesos` ya está implementado y verificado: devuelve una copia en memoria con
velocidades OSM o imputadas por tipo y tiempos estimados positivos; conserva el GraphML
original. La política completa está en `specs/R1-modelo-red.md`, R1.2.
`procesar` ya integra pesos, componente fuerte y exportación; los experimentos Python siguen
pendientes. Las pruebas de descarga usan una
respuesta simulada y lectura/escritura GraphML real, no los servidores públicos.

La adquisición real y la reutilización local se comprobaron el 2026-11-10 (fecha local).
La fecha exacta en UTC y los conteos están dentro del propio GraphML; la red cruda se
mantiene fuera de Git. Los CSV de red ya se generaron con `procesar` y se comprobó su lectura
desde Java; esto no implica que existan resultados de experimentos.

### Procesamiento local (sin internet)

Desde `python/`: `.\.venv\Scripts\python.exe -m preparacion procesar`.
Lee el GraphML, asigna pesos, selecciona la componente fuertemente conexa mayor, reindexa
y proyecta nodos a UTM 18N. Escribe `data/processed/nodos.csv.gz`, `aristas.csv.gz`,
`metadatos.json` y `lugares_utm.csv`. Conserva paralelas en CSV; Java las deduplica al
construir `Grafo`. La selección fue contrastada con Kosaraju y la red exportada es fuerte.

El grupo completó el catálogo con 40 lugares y coordenadas; `lugares_utm.csv` ya contiene
esos 40 IDs, incluido el punto de compra de Pijao que reemplaza la sede de Quimbaya.
El integrante corrigió el punto de Peñas Blancas y decidió conservar la finca El Agrado,
con entrada de recepción no confirmada como limitación. Las distancias a nodos y a geometrías viales no
son equivalentes: estar lejos de un nodo no demuestra por sí solo ausencia de carretera.

### Asociación de lugares (R1.4, sin mediciones)

1. Desde la raíz: `.\gradlew.bat bootRun --args=r1 --console=plain`.
2. Desde `python/`: `.\.venv\Scripts\python.exe -m preparacion verificar-lugares`.

El primer comando valida el catálogo y asocia todos los lugares con la búsqueda lineal
propia Java; escribe `data/processed/lugares_nodos.csv` y `results/r1/lugares_lejanos.csv`.
El segundo contrasta índices, distancias y banderas con OSMnx sobre nodos UTM, sin red ni
descargas, y escribe `results/r1/verificacion_lugar_nodo.csv`; aborta ante discrepancias.
Tras cambiar coordenadas del catálogo, regenerar UTM antes de repetir ambos comandos.

Ejecución real del 2026-11-11: 40/40 coincidencias; cuatro alertas (>500 m): Cocora,
RECUCA, Combia y El Agrado. Las notas conservan las limitaciones de acceso del catálogo.
Después de revisar accesos, Cocora y Combia se actualizaron con puntos indicados por el
integrante (Combia en el parqueadero); RECUCA y El Agrado se conservan con acceso
vehicular declarado. Los reportes se regeneraron y siguen las cuatro alertas de distancia
al nodo, sin que esto implique imposibilidad de llegar en vehículo.
`r1` Java también genera `results/r1/memoria_representacion.csv`: modelo compacto
de matriz (16·V² bytes) y listas (8·V + 20·E bytes) para descargada, componente exportada
y grafo Java deduplicado. Cabecera: `red,representacion,supuesto_bytes,V,E,megabytes`.
MB decimales, sin medir memoria real de objetos JVM. Los conteos de `metadatos.json`
deben ser compatibles con los CSV procesados; no se escriben salidas ante errores
de validación. No se descarga nada ni se modifican las coordenadas del catálogo.
Las mediciones de R1.3 siguen pendientes. Validación actual: Python 74/74; Java completa
45/68 aprobadas y 23 fallidas por algoritmos R2–R4 sin implementar; arquitectura aprobada.

## Aplicación (R5)

```bash
./gradlew bootRun
```

Abrir http://localhost:8080: ruta origen–destino, envolvente convexa, itinerario de un día
(Held-Karp frente a la heurística) y resultados R1–R4.

## Estructura

```
src/main/java/co/edu/uniquindio/rutas/
  controller/    controladores REST y manejo de errores HTTP
  service/       servicios y experimentos (casos de uso)
    measurement/ arnés de medición
  repository/    repositorios de archivos (CSV)
  model/         modelo de dominio y resultados
  algorithm/     algoritmos propios
  cli/           comandos de terminal
src/main/resources/static/   página con Leaflet
src/test/java/               mismos paquetes; architecture/ para las reglas de ArchUnit
python/                      preparación de datos y verificación
specs/                       especificaciones (Spec-Driven Development) y protocolo experimental
data/                        catálogo de lugares y red procesada
results/                     resultados de los experimentos (CSV) y figuras
docs/                        documento técnico, registro de uso de IA, fuentes, decisiones
```

## Desarrollo con IA

El equipo usa Claude Code y OpenCode. Las reglas comunes están en [`AGENTS.md`](AGENTS.md)
(OpenCode lo lee directamente; [`CLAUDE.md`](CLAUDE.md) lo importa). Ver
[`docs/configuracion-ias.md`](docs/configuracion-ias.md). Todo uso de IA se registra en
[`docs/registro-ia.md`](docs/registro-ia.md), que alimenta el anexo del documento técnico.
