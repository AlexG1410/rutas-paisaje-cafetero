---
description: Revisa el código de un requerimiento contra su spec y las reglas, sin modificar nada
argument-hint: <Rx>
---

Revisa **$ARGUMENTS** sin editar archivos. Entrega un informe con hallazgos ordenados de más a
menos grave, cada uno con archivo:línea y la regla o ID de la spec que incumple.

Lista de chequeo:
1. **Spec**: ¿cada comportamiento (Rx.y) y cada caso de prueba (Tx-*) está implementado y
   probado? ¿Hay código que la spec no pide?
2. **Capas**: ¿cada clase está en la capa correcta (`controller`/`cli` → `service` → `repository`,
   con `algorithm` y `model` puros)? ¿algún controlador lee archivos, algún repositorio usa
   clases de `service`/`algorithm`/`model`, algún servicio imprime?
3. **Implementación propia**: dependencias de `algorithm` fuera de `java.lang` /
   `java.util`; librerías que resuelvan el problema; permutaciones de librería.
4. **Correctitud**: casos borde (vacío, inalcanzable, empates, k = 0/1, colineales), parada
   temprana de Dijkstra, conteo de nodos explorados, reconstrucción de rutas/recorridos,
   desbordamiento (usar `long` en productos cruz).
5. **Complejidad**: ¿la implementación realmente tiene la complejidad del Javadoc? (p. ej.
   `List.remove(Object)` o `contains` dentro de ciclos, copias de arreglos innecesarias,
   autoboxing en ciclos internos).
6. **Protocolo experimental**: solo se mide el algoritmo; calentamiento + 5 repeticiones y
   mediana; ms; semilla documentada; verificación antes de medir; límite de 5 min con
   `Medidor.medirConLimite`.
7. **Resultados y documento**: números que no salen de `results/`; fuentes sin verificar;
   figuras sin unidad.
8. **Sustentación**: partes difíciles de explicar que convenga simplificar o comentar.

Ejecuta `./gradlew test` e incluye el resultado en el informe.
