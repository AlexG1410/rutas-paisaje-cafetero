# Documento técnico (15 % de la nota)

Estructura exigida por el enunciado (sección 7):

1. [Arquitectura de la solución](01-arquitectura.md): diagrama de componentes y flujo de datos.
2. Por cada requerimiento: problema; complejidad en tiempo y memoria **con fuente** (y
   recurrencia cuando corresponda); argumentos de correctitud; resultados en tablas y
   gráficas; discusión.
   [R1](02-r1-modelo-red.md) · [R2](03-r2-caminos-minimos.md) ·
   [R3](04-r3-envolvente.md) · [R4](05-r4-ruta-optima.md) · [R5](06-r5-aplicacion.md)
3. [Conclusiones](07-conclusiones.md).
4. [Anexo de uso de inteligencia artificial](08-anexo-ia.md).

Reglas: todo número sale de `results/`; cada tabla/figura indica unidad y lleva un párrafo
(¿coincide con la teoría? si no, ¿por qué?); cada complejidad lleva su fuente.

Exportar a PDF (opcional, con pandoc):
`pandoc docs/documento-tecnico/0*.md -o documento-tecnico.pdf --toc`
