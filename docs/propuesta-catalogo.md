# Propuesta de catálogo y revisión en mapa

Fecha de consulta: 2026-11-10. **Propuesta para aprobación del grupo, no catálogo validado.**
Se mantienen los 17 lugares ya propuestos en `data/catalogo/lugares.csv` y se añaden
23 candidatos con referencias públicas. No se modifica todavía el catálogo operativo.

**Actualización posterior:** el usuario ya sustituyó el catálogo operativo por las 40 filas
con coordenadas y comprobación en mapa declarada por el grupo. La validación estructural
aprueba. Por indicación posterior, `ac_coocafeq_quimbaya` se reemplazó por
`ac_coocafeq_pijao`, compra de café en calle 13 #5-13, Pijao. Se añadieron enlaces y
tratamientos a los casos revisados. El 2026-11-11 el integrante eligió un nuevo punto de
acceso para Peñas Blancas y decidió conservar la finca El Agrado, cuya entrada exacta de
recepción no confirmó. Las alternativas anteriores no se adoptan. Las tablas siguientes
conservan la propuesta original; no son una certificación independiente de los puntos.

En una revisión posterior del 2026-11-11 el integrante actualizó Cocora a
`4.637694,-75.487470` y Combia al parqueadero `4.491763,-75.686133`. Conservó RECUCA
confirmando llegada en vehículo pese a no poder inspeccionar el camino en Google Maps,
y confirmó acceso vehicular sin inconvenientes a El Agrado. El catálogo registra estas
declaraciones humanas y las distingue de una comprobación independiente.

El enunciado exige 12 cabeceras, 15 atractivos, 8 fincas cafeteras u hoteles rurales y
5 centros de acopio o trilladoras: 40 lugares distintos. Esta selección cubre esos cupos
como propuesta; la aceptación final depende de comprobar identidad, categoría y acceso.

## Cómo leer y revisar esta propuesta

- **Fuente** documenta existencia o actividad; no certifica una coordenada de entrada ni
  garantiza que el establecimiento siga abierto o permita visitas.
- **Mapa** es una búsqueda por nombre y municipio, NO un punto ya seleccionado/verificado.
  Si aparecen varias coincidencias, no aceptar automáticamente la primera.
- No se copian coordenadas aproximadas ni se marcan puntos como verificados. Para todos
  los candidatos, el estado inicial es **pendiente / `verificado_mapa = no`**.
- Seleccionar la plaza principal de cada cabecera. Para establecimientos, localizar
  portería/entrada o acceso de recepción de café; no el centro de un predio o una oficina
  que no realiza acopio. En lugares peatonales, identificar el acceso vial de referencia.
- No mover el punto hacia la carretera solamente para reducir su distancia al grafo.
  Si hay acceso peatonal, documentarlo; el modelo de rutas solo calcula conducción.
- No se requiere visitar físicamente los 40 lugares: la comprobación exigida es en mapa.
  Si la categoría de una sede es ambigua, consultar su fuente o contactar al establecimiento.

## A. Cabeceras municipales — 12

Los municipios y el criterio de plaza principal provienen de la sección 4 del
[enunciado](enunciado/Proyecto_final_2026-2.txt). «Plaza principal» es una descripción del
punto requerido, no una afirmación sobre su nombre oficial. No son 12 atractivos extra.

| ID | Punto propuesto | Municipio | Mapa para revisar |
|---|---|---|---|
| `cab_armenia` | Plaza de Bolívar; salida y regreso por defecto | Armenia | [Buscar](https://www.google.com/maps/search/?api=1&query=Plaza+de+Bolivar+Armenia+Quindio+Colombia) |
| `cab_buenavista` | Plaza principal | Buenavista | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Buenavista+Quindio+Colombia) |
| `cab_calarca` | Plaza principal | Calarcá | [Buscar](https://www.google.com/maps/search/?api=1&query=Plaza+principal+Calarca+Quindio+Colombia) |
| `cab_circasia` | Plaza principal | Circasia | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Circasia+Quindio+Colombia) |
| `cab_cordoba` | Plaza principal | Córdoba | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Cordoba+Quindio+Colombia) |
| `cab_filandia` | Plaza principal | Filandia | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Filandia+Quindio+Colombia) |
| `cab_genova` | Plaza principal | Génova | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Genova+Quindio+Colombia) |
| `cab_la_tebaida` | Plaza principal | La Tebaida | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+La+Tebaida+Quindio+Colombia) |
| `cab_montenegro` | Plaza principal | Montenegro | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Montenegro+Quindio+Colombia) |
| `cab_pijao` | Plaza principal | Pijao | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Pijao+Quindio+Colombia) |
| `cab_quimbaya` | Plaza principal | Quimbaya | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+principal+Quimbaya+Quindio+Colombia) |
| `cab_salento` | Plaza principal | Salento | [Buscar](https://www.google.com/maps/search/?api=1&query=Plaza+principal+Salento+Quindio+Colombia) |

## B. Atractivos turísticos — 15

Las duraciones son **propuestas**, no mediciones ni horarios de apertura. Salvo RECUCA,
son presupuestos de visita sugeridos para que el grupo los acepte o cambie. Si se aprueban,
la fuente/criterio será «Criterio del grupo: [alcance de la visita]», no el sitio web
como si este hubiera publicado esas duraciones. No se aplicará un tiempo artificialmente
corto para conseguir que todos los lugares quepan en el itinerario de 11 horas.

| ID | Lugar / municipio | Fuente de existencia | Mapa | Min propuestos y alcance |
|---|---|---|---|---|
| `atr_parque_cafe` | Parque del Café — Montenegro | [Sitio del parque](https://parquedelcafe.co/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+del+Cafe+Montenegro+Quindio) | 360; visita amplia, no todas las atracciones garantizadas |
| `atr_panaca` | PANACA — Quimbaya | [Cómo llegar, PANACA](https://panaca.co/en/getting-here) | [Buscar](https://www.google.com/maps/search/?api=1&query=PANACA+Quimbaya+Quindio) | 360; recorrido por estaciones y actividades |
| `atr_valle_cocora` | Valle de Cocora — Salento | [Referencia turística](https://www.tripadvisor.com/Attraction_Review-g1580963-d10393528-Reviews-Valle_del_Cocora-Salento_Quindio_Department.html) | [Buscar](https://www.google.com/maps/search/?api=1&query=Valle+de+Cocora+Salento+Quindio) | 180; paseo corto y paisaje, NO circuito completo de montaña |
| `atr_jardin_botanico` | Jardín Botánico del Quindío — Calarcá | [Sitio del jardín](https://jardinbotanicoquindio.org/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Jardin+Botanico+del+Quindio+Calarca) | 120; visita botánica y mariposario |
| `atr_museo_oro_quimbaya` | Museo del Oro Quimbaya — Armenia | [Banco de la República](https://www.banrepcultural.org/armenia/museo-del-oro-quimbaya) | [Buscar](https://www.google.com/maps/search/?api=1&query=Museo+del+Oro+Quimbaya+Armenia) | 90; salas principales. El museo NO está en el municipio de Quimbaya |
| `atr_recuca` | RECUCA — Calarcá, Barcelona / vereda Callelarga | [Operador](https://recuca.com/) | [Buscar](https://www.google.com/maps/search/?api=1&query=RECUCA+Calarca+Quindio) | 180; recorrido del café, duración aproximada publicada por el operador |
| `atr_arrieros` | Parque Los Arrieros — Quimbaya | [Sitio del parque](https://parquelosarrieros.com/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+Los+Arrieros+Quimbaya+Quindio) | 240; experiencia cultural. Confirmar ubicación de entrada en vía Montenegro–Quimbaya |
| `atr_colina_iluminada` | Mirador Colina Iluminada — Filandia | [Colombia Travel](https://colombia.travel/es/armenia/disfruta-del-mirador-colina-iluminada) | [Buscar](https://www.google.com/maps/search/?api=1&query=Mirador+Colina+Iluminada+Filandia) | 45; ascenso y observación |
| `atr_bejuco_canasto` | Centro de Interpretación Del Bejuco al Canasto — Filandia | [Referencia del centro](https://www.tripadvisor.com/Attraction_Review-g3147813-d15829359-Reviews-Centro_De_Interpretacion_Del_Bejuco_al_Canasto-Filandia_Quindio_Department.html) | [Buscar](https://www.google.com/maps/search/?api=1&query=Centro+Interpretacion+Bejuco+Canasto+Filandia) | 45; exposición de cestería, no taller extenso |
| `atr_parque_vida` | Parque de la Vida — Armenia | [Colombia Travel](https://colombia.travel/en/armenia/stroll-in-park-life) | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+de+la+Vida+Armenia+Quindio) | 60; senderos y observación |
| `atr_parque_fundadores` | Parque de los Fundadores — Armenia | [Colombia Travel](https://colombia.travel/en/armenia/wander-through-founders-park) | [Buscar](https://www.google.com/maps/search/?api=1&query=Parque+de+los+Fundadores+Armenia+Quindio) | 30; recorrido breve. No confundir con parques homónimos de otras ciudades |
| `atr_alto_cruz` | Mirador Alto de la Cruz — Salento | [Referencia turística](https://www.tripadvisor.com/Attraction_Review-g1580963-d8415774-Reviews-Ecoparque_the_Mirador_Alto_De_La_Cruz-Salento_Quindio_Department.html) | [Buscar](https://www.google.com/maps/search/?api=1&query=Mirador+Alto+de+la+Cruz+Salento) | 60; incluye aproximación peatonal. Definir punto vial al pie del acceso |
| `atr_calle_real` | Calle Real — Salento | [Descripción de Calle Real y mirador](https://discoverdiscomfort.com/the-ultimate-salento-colombia-travel-guide/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Calle+Real+Salento+Quindio) | 45; paseo urbano. Elegir punto distinto de plaza y mirador; no duplicar visitas solapadas |
| `atr_centro_guadua` | Centro Nacional para el Estudio del Bambú Guadua — Córdoba | [CRQ](https://crq.gov.co/centro-nacional-para-el-estudio-del-bambu-guadua-cnebg/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Centro+Nacional+Estudio+Bambu+Guadua+Cordoba+Quindio) | 90; recorrido educativo. Confirmar condiciones actuales de ingreso |
| `atr_penas_blancas` | Ecoparque Peñas Blancas — Calarcá | [Descripción turística](https://elturismoencolombia.com/a-donde-ir/calarca-quindio-travel-colombia/ecoparque-penas-blancas-calarca-quindio-colombia/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Ecoparque+Penas+Blancas+Calarca+Quindio) | 240; presupuesto propuesto sujeto al recorrido elegido. Usar entrada del ecoparque, NO cima de la peña |

## C. Fincas cafeteras u hoteles rurales — 8

Se eligen fincas productoras o alojamientos campestres; no se cuentan además como
atractivos para completar artificialmente dos categorías. La duración solo es obligatoria
para `atractivo` según el enunciado. No se presupone derecho de entrada a predios privados.

| ID propuesto | Lugar / municipio | Evidencia | Mapa y comprobación importante |
|---|---|---|---|
| `fh_combia` | Hotel Hacienda Combia — Calarcá | [Contacto del hotel](https://www.combia.com.co/es/contact-us.html) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hotel+Hacienda+Combia+Calarca); acceso en vereda La Bella |
| `fh_el_ocaso` | Finca Cafetera El Ocaso — Salento | [Sitio de la finca](https://www.ocasocoffee.com/web/ocaso-farm/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Finca+Cafetera+El+Ocaso+Salento); evitar homónimos y puntos de venta urbanos |
| `fh_las_acacias` | Finca Las Acacias — Salento | [Operador turístico](https://cocoratours.com.co/en/tour/tour-de-cafe-finca-cafetera-las-acacias/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Finca+Las+Acacias+Coffee+Farm+Salento); entrada de finca, no agencia |
| `fh_san_alberto` | Hacienda San Alberto — Buenavista | [Colombia Travel](https://colombia.travel/es/armenia/visita-hacienda-san-alberto) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hacienda+San+Alberto+Buenavista+Quindio); acceso de la hacienda, no tienda de otra ciudad |
| `fh_las_camelias` | Hotel Campestre Las Camelias — Montenegro | [Referencia de alojamiento](https://www.tripadvisor.in/Hotel_Review-g1915404-d3318512-Reviews-Las_Camelias_Hotel_Campestre-Montenegro_Quindio_Department.html) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hotel+Campestre+Las+Camelias+Montenegro+Quindio); portería |
| `fh_mocawa_resort` | Hotel Mocawa Resort — La Tebaida | [Sitio del resort](https://hotelmocawaresort.com/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hotel+Mocawa+Resort+La+Tebaida); NO Mocawa Plaza de Armenia |
| `fh_mirador_palmas` | Hotel Mirador Las Palmas — La Tebaida | [Sitio del hotel](https://www.miradorlaspalmashotel.com/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hotel+Mirador+Las+Palmas+La+Tebaida); acceso vial, no un mirador homónimo |
| `fh_decameron_panaca` | Hotel Decameron Panaca — Quimbaya | [Cadena hotelera](https://www.decameron.com/en/hotels/quindio/decameron-panaca/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Hotel+Decameron+Panaca+Quimbaya); recepción/portería del hotel, distinta de taquilla PANACA |

## D. Acopio o trilladoras de café — 5 candidatos

Son la categoría que requiere mayor cuidado. Una entidad puede tener oficinas y bodegas
en ubicaciones distintas. No basta un nombre de cooperativa para certificar acopio en
la coordenada elegida. Si no se confirma esa función en la sede, se reemplaza el candidato
y se mantiene el mínimo de cinco; no se marca como validado para completar el cupo.

| ID propuesto | Sede candidata / municipio | Evidencia localizada | Mapa y qué confirmar |
|---|---|---|---|
| `ac_coocafeq_armenia` | Cooperativa de Caficultores del Quindío, compra de café Armenia — Armenia | [Ficha de compra en Waze](https://www.waze.com/es-419/live-map/directions/co/quindio/armenia/cooperativa-de-caficultores-del-quindio-compra-de-cafe-armenia?to=place.ChIJgZk-fPj1OI4RD_B-G20KTNE) | [Buscar](https://www.google.com/maps/search/?api=1&query=Cooperativa+Caficultores+Quindio+Compra+Cafe+Armenia); ficha sitúa Cra. 18 #51-26, confirmar acceso y actividad |
| `ac_coocafeq_calarca` | Cooperativa de Caficultores del Quindío, compra de café Calarcá — Calarcá | [Directorio de ubicación](https://me.maptons.com/p/2539358813) | [Buscar](https://www.google.com/maps/search/?api=1&query=Cooperativa+Caficultores+Quindio+Compra+Cafe+Calarca); referencia Cra. 28 con calle 36; fuente secundaria, confirmar sede de compra |
| `ac_coocafeq_filandia` | Cooperativa de Caficultores del Quindío, compra de café Filandia — Filandia | [Ficha de compra en Waze](https://www.waze.com/es/live-map/directions/co/quindio/filandia/cooperativa-de-caficultores-del-quindio-compra-de-cafe-filandia?to=place.ChIJm4fib3GJOI4RHtjECLV5llw) | [Buscar](https://www.google.com/maps/search/?api=1&query=Cooperativa+Caficultores+Quindio+Compra+Cafe+Filandia); ficha indica carrera 6 #7-31, Edificio del Café; comprobar ingreso de compra |
| `ac_coocafeq_quimbaya` | Cooperativa de Caficultores del Quindío, sede operativa de Quimbaya — Quimbaya | [Contacto de la entidad](https://coocafeq.wixsite.com/index/blank-1) y [directorio BASC de actividad](https://sibasc.wbasco.org/mod_consulta_pag_wbo/consulta_empresa.php?empresa_id=19&capitulo_id=17&session_nomidioma=) | [Buscar](https://www.google.com/maps/search/?api=1&query=Cooperativa+Caficultores+Quindio+Quimbaya+Carrera+2+Calles+20+21); contacto ubica sede en carrera 2 entre calles 20 y 21. Confirmar que bodega/trilla funciona allí, no solo administración |
| `ac_central_el_agrado` | Central de Beneficio de Café, finca El Agrado — Montenegro, Pueblo Tapao | [Comité de Cafeteros: acopio y compra de café cereza](https://quindio.federaciondecafeteros.org/listado-noticias/comite-de-cafeteros-del-quindio-inaugurara-la-primera-central-de-beneficio-de-la-fnc-para-compra-de-cafe-cereza-en-el-departamento/) | [Buscar](https://www.google.com/maps/search/?api=1&query=Central+Beneficio+Cafe+Finca+El+Agrado+Pueblo+Tapao+Montenegro); localizar central, no cualquier finca El Agrado. Se propone por función documentada de acopio, no por ser una trilladora |

## Alcance de la investigación de fuentes

Se consultaron resultados de búsqueda y, cuando fue posible, las páginas enlazadas.
Se pudo leer directamente el contacto de Coocafeq, la ficha de Filandia, la publicación
del Comité de Cafeteros sobre El Agrado y las páginas de RECUCA, Mocawa Resort y Mirador
Las Palmas. El sitio de RECUCA publica aproximadamente tres horas para el recorrido del café.

La lectura directa falló por transporte o tiempo de espera en Combia, El Ocaso, Parque
Los Arrieros, CRQ y el directorio BASC: en esos casos la evidencia disponible fue el
resultado indexado, no una revisión completa de la página. Las otras referencias también
se localizaron mediante búsqueda; no se afirma haber comprobado todos sus contenidos.
Los enlaces son referencias de investigación, no coordenadas certificadas. Una publicación
antigua sobre una sede no garantiza funcionamiento actual; revisar antes de aceptarla.

## Cómo enviarme los puntos comprobados

### Paso 1: elegir el punto en Google Maps

1. Abrir «Buscar», comprobar nombre y municipio y comparar con la fuente.
2. Acercar el mapa o la vista satelital. Elegir plaza o entrada según corresponda.
3. En computador, hacer clic derecho sobre el punto; Maps muestra latitud y longitud.
   En móvil, mantener pulsado para colocar un pin y copiar las coordenadas mostradas.
4. Copiar coordenadas y enlace de «Compartir». Usar decimales con punto, no coma decimal.
   En Quindío la latitud normalmente empieza por `4...` y la longitud por `-75...`;
   es una comprobación orientativa, no sustituto del mapa.

### Paso 2: enviarlos por bloques pequeños

Empezar por `cab_armenia`, `cab_buenavista` y `cab_calarca`. Se pueden enviar de 3 a 5
por mensaje, sin necesidad de editar archivos ni saber programar:

```text
ID: cab_armenia
Latitud: [copiar el valor del pin]
Longitud: [copiar el valor del pin]
Enlace de mapa: [pegar enlace compartido]
Qué comprobé: corresponde a la Plaza de Bolívar de Armenia
Revisado por: [integrante]
Fecha: [AAAA-MM-DD]
Estado: comprobado
Observaciones: [si hay dudas o un acceso especial]
```

No escribir «comprobado» si solo se abrió la búsqueda. En ese caso usar «dudoso» y
explicar la coincidencia pendiente. Si no se pueden copiar coordenadas, enviar el enlace
del pin y aclararlo; se intentará extraer el punto sin adivinarlo. Un enlace corto o una
captura sin coordenadas puede requerir que vuelvas a copiar el pin.

Para un atractivo añadir:

```text
Duración propuesta: acepto / cambiar a [minutos]
Criterio: [qué recorrido o actividad incluye]
```

Para una sede cafetera añadir:

```text
Actividad de la sede: compra/acopio/trilla / no pude confirmar
Evidencia: [ficha, web o confirmación de la entidad]
```

### Paso 3: incorporación y validación técnica

Con tu confirmación se incorporará el punto a `data/catalogo/lugares.csv`, con fuente,
nota de revisión y `verificado_mapa = si` solo para los revisados por una persona.
Los candidatos aceptados sin comprobación pueden incorporarse con `no`, sin presentarlos
como catálogo final. Se conservará el ID al corregir coordenadas de un mismo lugar.

Después se proyectarán las coordenadas y se asociarán a nodos. La distancia al grafo
permitirá detectar posibles errores o accesos fuera de `drive`; los casos >500 m
necesitan explicación o corrección, no un ajuste automático para ocultar el problema.
La comprobación humana y la validación técnica son pasos distintos y necesarios.
