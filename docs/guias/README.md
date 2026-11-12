# Guías del docente (solo en local)

Aquí van los PDF de las clases (02 a 09) y su versión en texto (`.txt`), para que las IAs
sigan la notación del curso. Están excluidos de git (`.gitignore`) porque son material del
docente; cada integrante los copia en su máquina.

Para generar el `.txt` de una guía nueva: `pdftotext -layout guia.pdf guia.txt` (poppler).

| Guía | Se usa en |
|---|---|
| 02 Análisis de algoritmos, 03 Tiempo de ejecución, 04 Notación asintótica | Todo el análisis teórico |
| 05 Algoritmos recursivos (inducción, Teorema Maestro) | R3, R4 (recurrencias) |
| 06 Ordenamiento y búsqueda (medición empírica) | Costo de `List.sort` (TimSort), protocolo |
| 07 Divide y vencerás (Teorema Maestro a, b, d; pila de recursión) | R3 |
| 08 Algoritmos voraces (Dijkstra, elementos voraces, cuándo funciona) | R2, R4.2, R4.5 |
| 09 Programación dinámica (Held-Karp, `dp[S][i]`) | R4.1 |
