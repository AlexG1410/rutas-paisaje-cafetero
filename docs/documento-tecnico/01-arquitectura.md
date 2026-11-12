# 1. Arquitectura de la solución

## 1.1 Arquitectura en tres capas (Java, Spring Boot)

Paquete base `co.edu.uniquindio.rutas`. Dependencias en una sola dirección (presentación →
lógica → datos), comprobadas automáticamente con ArchUnit (`architecture.ArquitecturaTest`).
Los paquetes Java usan nombres en inglés: `controller`, `cli`, `service`, `algorithm`,
`model` y `repository`; el arnés de medición reside en `service.measurement`.

```mermaid
flowchart TB
    subgraph P[Capa de presentación · controller / cli]
        WEB[controller: LugaresController · ItinerarioController<br/>ResultadosController · ManejoErrores]
        CLI[cli: ComandosCli<br/>bootRun --args=r1..r4]
        HTML[static: index.html + Leaflet]
    end
    subgraph L[Capa lógica · service / algorithm / model]
        SERV[service: RedService · RutaService · ItinerarioService<br/>ExperimentoR1..R4Service · measurement.Medidor]
        ALG[algorithm · PROPIOS<br/>Dijkstra · EnvolventeConvexa · TspFuerzaBruta<br/>HeldKarp · VecinoMasCercano · NodoMasCercano]
        MOD[model: Grafo · Punto · Lugar · resultados]
    end
    subgraph D[Capa de datos · repository]
        REPO[RedRepository · CatalogoRepository · LugaresNodosRepository<br/>MatrizRepository · ResultadosRepository · MaquinaRepository]
    end
    HTML -->|HTTP/JSON| WEB
    WEB --> SERV
    CLI --> SERV
    SERV --> ALG
    SERV --> MOD
    ALG --> MOD
    SERV --> REPO
    REPO --> ARCH[(data/ · results/)]
```

_(Describir cada capa: responsabilidad, qué usa de Spring y por qué los algoritmos propios no
dependen de ninguna librería ni de la capa de datos.)_

## 1.2 Flujo de datos entre Python, Java y los requerimientos

```mermaid
flowchart LR
    OSM[(OpenStreetMap)] -->|OSMnx · Python| DESC[descarga<br/>data/raw]
    DESC --> PESOS[pesos R1.2 · Python]
    PESOS --> SCC[componente fuertemente<br/>conexa R1.3 · Python]
    CAT[(catálogo<br/>lugares.csv)] --> EXP
    SCC --> EXP[exportación R1.5<br/>data/processed CSV]
    EXP --> R1J[R1.1 y R1.4 · Java]
    EXP --> R2[Dijkstra lineal y cola · Java R2]
    R2 --> MAT[(matriz de tiempos<br/>40 × 40)]
    EXP --> R3[envolvente convexa · Java R3]
    R3 --> SCI[SciPy · Python]
    MAT --> R4[fuerza bruta · Held-Karp<br/>heurística · itinerario · Java R4]
    R1J & R2 & SCI & R4 --> RES[(results/*.csv)]
    RES --> FIG[figuras · Python]
    EXP & MAT & RES & FIG --> APP[aplicación R5 · Spring Boot]
```

_(Describir cada componente: comando que lo ejecuta, entradas, salidas y qué requerimiento
consume su salida.)_
