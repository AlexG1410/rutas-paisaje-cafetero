// R5: presentación. Los algoritmos y totales se calculan exclusivamente en Java.
const elemento = (id) => document.getElementById(id);
const escapar = (texto) => String(texto ?? '').replace(/[&<>"']/g, (c) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
}[c]));
const numero = (valor, decimales = 1) => Number(valor).toLocaleString('es-CO', {
  minimumFractionDigits: decimales, maximumFractionDigits: decimales,
});
let lugares = [];
let version = 0;
let mapa;
let capas;
let solicitudes = new AbortController();

try {
  if (!window.L) throw new Error('Leaflet no está disponible');
  mapa = L.map('mapa').setView([4.53, -75.68], 10);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18, attribution: '&copy; colaboradores de OpenStreetMap',
  }).addTo(mapa).on('tileerror', () => {
    elemento('aviso-mapa').textContent = 'No se pudieron cargar algunas teselas. Revisa la conexión a internet.';
  });
  capas = L.featureGroup().addTo(mapa);
} catch (error) {
  elemento('aviso-mapa').textContent = 'Mapa no disponible: revisa internet y recarga. Las consultas y tablas siguen funcionando.';
}

async function api(url, opciones = {}) {
  const respuesta = await fetch(url, opciones);
  const cuerpo = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) throw new Error(cuerpo.error || `Error HTTP ${respuesta.status}`);
  return cuerpo;
}

function mostrarError(destino, error) {
  destino.replaceChildren();
  const texto = document.createElement('p');
  texto.className = 'error';
  texto.textContent = error.message;
  destino.append(texto);
}

function limpiarMapa() {
  capas?.clearLayers();
  elemento('leyenda').replaceChildren();
  reiniciarResumen();
}

function ajustarMapa() {
  if (capas?.getBounds().isValid()) {
    mapa.fitBounds(capas.getBounds(), {
      paddingTopLeft: [50, 55], paddingBottomRight: [110, 60], maxZoom: 15, animate: false,
    });
  }
}

const sinMovimiento = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// Vuelve a disparar la animación de entrada sobre un contenedor de resultados.
function revelar(destino) {
  destino.classList.remove('revelado');
  void destino.offsetWidth;
  destino.classList.add('revelado');
}

const ICONOS = {
  reloj: '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></svg>',
  explorados: '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.3-4.3"/></svg>',
  nodos: '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true"><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="M8.3 10.9l7.4-3.7M8.3 13.1l7.4 3.7"/></svg>',
  vertices: '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3l8.5 6.2-3.2 10.3H6.7L3.5 9.2z"/></svg>',
  diferencia: '<svg class="ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 18l5-6 4 3 7-9"/><path d="M15 6h5v5"/></svg>',
};

const TITULOS = {
  ruta: 'Ruta origen–destino',
  envolvente: 'Envolvente convexa de la red',
  itinerario: 'Itinerario de un día',
  resultados: 'Resultados',
};

const formatoEntero = (valor) => Number(valor).toLocaleString('es-CO');

// Encabezado del mapa: título y chips con los valores ya calculados en el servidor.
function mostrarResumen(titulo, chips = []) {
  elemento('titulo-mapa').textContent = titulo;
  elemento('chips').replaceChildren(...chips.map(([icono, texto]) => {
    const chip = document.createElement('span');
    chip.className = 'chip';
    chip.innerHTML = ICONOS[icono];
    chip.append(texto);
    return chip;
  }));
}

function mostrarDatos(datos) {
  for (const [id, valor] of Object.entries(datos)) {
    elemento(id).textContent = valor;
    revelar(elemento(id));
  }
}

function reiniciarResumen() {
  mostrarResumen(TITULOS[document.querySelector('main').dataset.modo] || TITULOS.ruta);
  for (const id of ['dato-tiempo', 'dato-explorados', 'dato-camino']) elemento(id).textContent = '—';
}

// Dibuja progresivamente un camino ya ajustado al encuadre final del mapa.
function trazar(capa) {
  const camino = capa?.getElement?.();
  if (!camino || sinMovimiento()) return;
  const largo = camino.getTotalLength();
  camino.style.strokeDasharray = `${largo}`;
  camino.style.strokeDashoffset = `${largo}`;
  camino.getBoundingClientRect();
  camino.style.transition = 'stroke-dashoffset 1s cubic-bezier(0.4, 0, 0.2, 1)';
  camino.style.strokeDashoffset = '0';
  camino.addEventListener('transitionend', () => {
    camino.style.removeProperty('stroke-dasharray');
    camino.style.removeProperty('stroke-dashoffset');
    camino.style.removeProperty('transition');
  }, { once: true });
}

// tipo: 'origen' y 'destino' se dibujan como pines; 'parada' como círculo numerado.
function marcador(coordenada, texto, { tipo = 'parada', etiqueta, numero } = {}) {
  if (!mapa || !coordenada) return;
  const esPin = tipo === 'origen' || tipo === 'destino';
  const icono = L.divIcon({
    className: 'icono-pin',
    html: esPin ? `<span class="pin pin-${tipo}"></span>` : `<span class="parada">${escapar(numero ?? '')}</span>`,
    iconSize: esPin ? [30, 40] : [24, 24],
    iconAnchor: esPin ? [15, 37] : [12, 12],
  });
  const marca = L.marker(coordenada, { icon: icono, title: texto, alt: texto, keyboard: false }).addTo(capas);
  const contenido = document.createElement('span');
  contenido.textContent = etiqueta || texto;
  marca.bindTooltip(contenido, etiqueta
    ? { permanent: true, direction: 'right', offset: [10, -22], className: 'etiqueta-pin' }
    : { direction: 'top', offset: [0, esPin ? -34 : -12] });
}

const nombre = (id) => lugares.find((lugar) => lugar.id === id)?.nombre || id;
const corto = (id) => nombre(id).replace(/\s*\(.*\)\s*$/, '');

// Una nueva operación invalida las respuestas anteriores, incluso si llegan tarde.
async function ejecutar(boton, destino, accion) {
  const actual = ++version;
  solicitudes.abort();
  solicitudes = new AbortController();
  const signal = solicitudes.signal;
  boton.disabled = true;
  destino.setAttribute('aria-busy', 'true');
  destino.textContent = 'Consultando…';
  limpiarMapa();
  try {
    await accion(signal, () => actual === version);
  } catch (error) {
    if (actual === version && error.name !== 'AbortError') {
      mostrarError(destino, error);
      revelar(destino);
    }
  } finally {
    boton.disabled = false;
    destino.removeAttribute('aria-busy');
  }
}

document.querySelectorAll('.pestana').forEach((boton) => {
  boton.addEventListener('click', () => {
    version++;
    solicitudes.abort();
    document.querySelectorAll('.pestana').forEach((b) => {
      b.classList.toggle('activa', b === boton);
      b.setAttribute('aria-selected', String(b === boton));
    });
    document.querySelectorAll('.panel').forEach((p) => p.classList.add('oculto'));
    elemento(`panel-${boton.dataset.panel}`).classList.remove('oculto');
    document.querySelector('main').classList.toggle('solo-resultados', boton.dataset.panel === 'resultados');
    document.querySelector('main').dataset.modo = boton.dataset.panel;
    document.querySelectorAll('[aria-busy="true"]').forEach((p) => {
      p.removeAttribute('aria-busy');
      p.textContent = 'Consulta cancelada. Puedes intentarlo de nuevo.';
    });
    limpiarMapa();
    mapa?.invalidateSize();
  });
});

async function cargarLugares() {
  try {
    lugares = await api('/api/lugares');
    for (const id of ['origen', 'destino', 'partida']) {
      elemento(id).replaceChildren(...lugares.map((lugar) => new Option(lugar.nombre, lugar.id)));
    }
    elemento('origen').value = 'cab_armenia';
    elemento('destino').value = 'cab_calarca';
    elemento('partida').value = 'cab_armenia';
    const seleccionInicial = new Set(['atr_museo_oro_quimbaya', 'atr_jardin_botanico',
      'atr_colina_iluminada', 'atr_bejuco_canasto', 'atr_parque_vida', 'atr_calle_real', 'atr_alto_cruz']);
    elemento('lista-atractivos').replaceChildren(...lugares.filter((l) => l.categoria === 'atractivo').map((lugar) => {
      const etiqueta = document.createElement('label');
      const casilla = document.createElement('input');
      casilla.type = 'checkbox';
      casilla.value = lugar.id;
      casilla.checked = seleccionInicial.has(lugar.id);
      casilla.addEventListener('change', actualizarSeleccion);
      etiqueta.append(casilla, ` ${lugar.nombre} (${lugar.duracionVisitaMin} min)`);
      return etiqueta;
    }));
    actualizarSeleccion();
    elemento('calcular-ruta').disabled = false;
    elemento('calcular-itinerario').disabled = false;
  } catch (error) {
    mostrarError(elemento('info-ruta'), error);
    mostrarError(elemento('info-itinerario'), error);
  }
}

function seleccion() {
  return [...document.querySelectorAll('#lista-atractivos input:checked')].map((c) => c.value);
}

function actualizarSeleccion() {
  elemento('contador-atractivos').textContent = `${seleccion().length} seleccionados (elige entre 6 y 8).`;
}

elemento('calcular-ruta').addEventListener('click', (evento) => {
  const info = elemento('info-ruta');
  ejecutar(evento.currentTarget, info, async (signal, vigente) => {
    const origen = elemento('origen').value;
    const destino = elemento('destino').value;
    const ruta = await api(`/api/ruta?${new URLSearchParams({ origen, destino })}`, { signal });
    if (!vigente()) return;
    const borde = mapa ? L.polyline(ruta.coordenadas, { color: '#ffffff', weight: 10, opacity: 0.95 }).addTo(capas) : null;
    const linea = mapa ? L.polyline(ruta.coordenadas, { color: '#e1a526', weight: 5 }).addTo(capas) : null;
    marcador(ruta.coordenadas[0], `Origen vial: ${nombre(origen)}`, { tipo: 'origen', etiqueta: corto(origen) });
    marcador(ruta.coordenadas.at(-1), `Destino vial: ${nombre(destino)}`, { tipo: 'destino', etiqueta: corto(destino) });
    ajustarMapa();
    trazar(borde);
    trazar(linea);
    info.textContent = `${nombre(origen)} → ${nombre(destino)}. Tiempo estimado: ${numero(ruta.tiempoS / 60)} min · ` +
      `nodos explorados: ${ruta.nodosExplorados}.`;
    revelar(info);
    mostrarResumen(`Ruta: ${corto(origen)} → ${corto(destino)}`, [
      ['reloj', `≈ ${numero(ruta.tiempoS / 60)} min`],
      ['nodos', `${formatoEntero(ruta.coordenadas.length)} nodos`],
    ]);
    mostrarDatos({
      'dato-tiempo': `≈ ${numero(ruta.tiempoS / 60)} min`,
      'dato-explorados': formatoEntero(ruta.nodosExplorados),
      'dato-camino': formatoEntero(ruta.coordenadas.length),
    });
  });
});

elemento('ver-envolvente').addEventListener('click', (evento) => {
  const info = elemento('info-envolvente');
  ejecutar(evento.currentTarget, info, async (signal, vigente) => {
    const vertices = await api('/api/envolvente', { signal });
    if (!vigente()) return;
    if (vertices.length < 3 || vertices.some((v) => !Number.isFinite(Number(v.lat)) || !Number.isFinite(Number(v.lon)))) {
      throw new Error('La envolvente no contiene un polígono válido. Ejecuta el experimento r3.');
    }
    if (mapa) L.polygon(vertices.map((v) => [Number(v.lat), Number(v.lon)]), {
      color: '#7c3c15', fillOpacity: 0.15, className: 'envolvente',
    }).addTo(capas);
    ajustarMapa();
    info.textContent = `${vertices.length} vértices de la envolvente de nodos. No representa la frontera administrativa.`;
    revelar(info);
    mostrarResumen(TITULOS.envolvente, [['vertices', `${vertices.length} vértices`]]);
  });
});

function tablaItinerario(itinerario) {
  const filas = itinerario.paradas.map((p) => `<tr><td>${p.orden}</td><td>${escapar(p.nombre)}</td>` +
    `<td>${numero(p.desplazamientoMin)}</td><td>${numero(p.visitaMin, 0)}</td>` +
    `<td>${numero(p.tiempoAcumuladoH, 2)}</td></tr>`).join('');
  return `<div class="tabla-scroll"><table><thead><tr><th>Orden</th><th>Lugar</th><th>Viaje (min)</th>` +
    `<th>Visita (min)</th><th>Acum. (h)</th></tr></thead><tbody>${filas}</tbody></table></div>` +
    `<p>Visitas: ${numero(itinerario.totalVisitasMin)} min · Desplazamientos: ${numero(itinerario.totalDesplazamientosMin)} min.<br>` +
    `<strong>Jornada: ${numero(itinerario.duracionJornadaMin)} min (${numero(itinerario.duracionJornadaMin / 60, 2)} h).</strong></p>` +
    (itinerario.duracionJornadaMin > 660 ? '<p class="advertencia">Supera las 11 horas. Reduce o cambia la selección; esta comparación no retira atractivos.</p>' :
      '<p>Dentro del límite de 11 horas bajo el modelo de tiempos y visitas.</p>');
}

elemento('calcular-itinerario').addEventListener('click', (evento) => {
  const info = elemento('info-itinerario');
  ejecutar(evento.currentTarget, info, async (signal, vigente) => {
    const atractivos = seleccion();
    if (atractivos.length < 6 || atractivos.length > 8) throw new Error('Elige entre 6 y 8 atractivos.');
    const comparacion = await api('/api/itinerario', {
      signal, method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partida: elemento('partida').value, atractivos }),
    });
    if (!vigente()) return;
    info.innerHTML = `<h3>Held-Karp (óptimo)</h3>${tablaItinerario(comparacion.optimo)}` +
      `<h3>Vecino más cercano</h3>${tablaItinerario(comparacion.heuristico)}` +
      `<p>Diferencia de jornada: ${numero(comparacion.diferenciaMin)} min (${numero(comparacion.diferenciaPct, 2)} %).</p>` +
      '<p class="estado-mapa">Cargando caminos de ambos itinerarios…</p>';
    revelar(info);
    mostrarResumen(`Itinerario desde ${corto(comparacion.optimo.idPartida)}`, [
      ['reloj', `Óptimo: ${numero(comparacion.optimo.duracionJornadaMin / 60, 2)} h`],
      ['diferencia', `Diferencia: ${numero(comparacion.diferenciaPct, 2)} %`],
    ]);
    const cache = new Map();
    async function coordenadas(itinerario) {
      const ids = [itinerario.idPartida, ...itinerario.paradas.map((p) => p.idLugar)];
      const tramos = [];
      for (let i = 1; i < ids.length; i++) {
        const clave = `${ids[i - 1]}→${ids[i]}`;
        if (!cache.has(clave)) cache.set(clave, api(`/api/ruta?${new URLSearchParams({
          origen: ids[i - 1], destino: ids[i],
        })}`, { signal }));
        tramos.push((await cache.get(clave)).coordenadas);
      }
      return tramos;
    }
    try {
      const optimo = await coordenadas(comparacion.optimo);
      const voraz = await coordenadas(comparacion.heuristico);
      if (!vigente()) return;
      if (mapa) {
        const lineaOptima = L.polyline(optimo, { color: '#2d6a4f', weight: 6, opacity: 0.8 }).addTo(capas);
        L.polyline(voraz, { color: '#b42333', weight: 3, dashArray: '7 6' }).addTo(capas);
        marcador(optimo[0][0], `Partida y regreso: ${nombre(comparacion.optimo.idPartida)}`,
          { tipo: 'origen', etiqueta: corto(comparacion.optimo.idPartida) });
        comparacion.optimo.paradas.slice(0, -1).forEach((p, i) => marcador(optimo[i].at(-1), `${p.orden}. ${p.nombre}`,
          { numero: p.orden }));
        ajustarMapa();
        trazar(lineaOptima);
      }
      elemento('leyenda').textContent = 'Verde continuo: Held-Karp · Rojo discontinuo: vecino más cercano. Pueden superponerse.';
      info.querySelector('.estado-mapa').textContent = mapa ? 'Caminos viales calculados con Dijkstra.' : 'Mapa no disponible; las tablas contienen la comparación.';
    } catch (error) {
      if (vigente() && error.name !== 'AbortError') {
        info.querySelector('.estado-mapa').textContent = `Las tablas están disponibles, pero no se pudieron dibujar los caminos: ${error.message}`;
      }
    }
  });
});

elemento('ver-tabla').addEventListener('click', (evento) => {
  const destino = elemento('contenido-tabla');
  ejecutar(evento.currentTarget, destino, async (signal, vigente) => {
    const filas = await api(`/api/resultados?${new URLSearchParams({ nombre: elemento('tabla').value })}`, { signal });
    if (!vigente()) return;
    if (!filas.length) { destino.textContent = 'Tabla sin filas.'; return; }
    const columnas = Object.keys(filas[0]);
    destino.innerHTML = `<div class="tabla-scroll"><table><thead><tr>${columnas.map((c) => `<th>${escapar(c)}</th>`).join('')}</tr></thead><tbody>` +
      filas.map((f) => `<tr>${columnas.map((c) => `<td>${escapar(f[c])}</td>`).join('')}</tr>`).join('') + '</tbody></table></div>';
    revelar(destino);
  });
});

async function cargarFiguras() {
  try {
    const figuras = await api('/api/figuras');
    elemento('figuras').replaceChildren(...figuras.map((nombreFigura) => {
      const marco = document.createElement('figure');
      const imagen = document.createElement('img');
      imagen.src = `/api/figuras/${encodeURIComponent(nombreFigura)}`;
      imagen.alt = nombreFigura;
      imagen.loading = 'lazy';
      imagen.addEventListener('load', () => imagen.classList.add('cargada'));
      imagen.addEventListener('error', () => {
        imagen.classList.add('cargada');
        pie.textContent = `No se pudo cargar ${nombreFigura}`;
      });
      const pie = document.createElement('figcaption');
      pie.textContent = nombreFigura;
      marco.append(imagen, pie);
      return marco;
    }));
    if (!figuras.length) elemento('figuras').textContent = 'No hay figuras. Ejecuta python -m preparacion graficas.';
  } catch (error) { mostrarError(elemento('figuras'), error); }
}

// La fotografía del panel se revela según la fracción que ya es visible al desplazarse.
function revelarPostal() {
  const postal = document.querySelector('.postal');
  const paneles = document.querySelector('.paneles');
  if (!postal || !paneles) return;
  let pendiente = false;
  const actualizar = () => {
    pendiente = false;
    const caja = postal.getBoundingClientRect();
    if (!caja.height) return;
    const limite = Math.min(paneles.getBoundingClientRect().bottom, window.innerHeight);
    const visible = Math.min(Math.max((limite - caja.top) / caja.height, 0), 1);
    postal.style.setProperty('--revelado', visible.toFixed(3));
  };
  const programar = () => {
    if (pendiente) return;
    pendiente = true;
    requestAnimationFrame(actualizar);
  };
  paneles.addEventListener('scroll', programar, { passive: true });
  window.addEventListener('scroll', programar, { passive: true });
  window.addEventListener('resize', programar);
  if (window.ResizeObserver) {
    const observador = new ResizeObserver(programar);
    observador.observe(paneles);
    document.querySelectorAll('.panel').forEach((panel) => observador.observe(panel));
  }
  programar();
}

cargarLugares();
cargarFiguras();
revelarPostal();
