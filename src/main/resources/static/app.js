// R5 — Página de la aplicación. Solo consume la API REST (capa de presentación).
// Los cálculos los hace el servidor con los algoritmos propios.

const mapa = L.map('mapa').setView([4.53, -75.68], 10); // Quindío
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 18,
  attribution: '&copy; colaboradores de OpenStreetMap',
}).addTo(mapa);

const capas = L.layerGroup().addTo(mapa);
let lugares = [];

async function api(url, opciones) {
  const respuesta = await fetch(url, opciones);
  const cuerpo = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    throw new Error(cuerpo.error || `Error ${respuesta.status}`);
  }
  return cuerpo;
}

function mostrarError(elemento, error) {
  elemento.innerHTML = `<span class="error">${error.message}</span>`;
}

function minutos(segundos) {
  return (segundos / 60).toFixed(1);
}

function nombre(id) {
  const lugar = lugares.find((l) => l.id === id);
  return lugar ? lugar.nombre : id;
}

// --- Pestañas ---
document.querySelectorAll('.pestana').forEach((boton) => {
  boton.addEventListener('click', () => {
    document.querySelectorAll('.pestana').forEach((b) => b.classList.remove('activa'));
    document.querySelectorAll('.panel').forEach((p) => p.classList.add('oculto'));
    boton.classList.add('activa');
    document.getElementById(`panel-${boton.dataset.panel}`).classList.remove('oculto');
    capas.clearLayers();
  });
});

// --- Lugares ---
async function cargarLugares() {
  try {
    lugares = await api('/api/lugares');
  } catch (e) {
    mostrarError(document.getElementById('info-ruta'), e);
    return;
  }
  const opciones = lugares.map((l) => `<option value="${l.id}">${l.nombre}</option>`).join('');
  ['origen', 'destino', 'partida'].forEach((id) => {
    document.getElementById(id).innerHTML = opciones;
  });
  document.getElementById('partida').value = 'cab_armenia';
  document.getElementById('lista-atractivos').innerHTML = lugares
    .filter((l) => l.categoria === 'atractivo')
    .map((l) => `<label><input type="checkbox" value="${l.id}"> ${l.nombre}</label>`)
    .join('');
}

// --- R5.1 Ruta ---
document.getElementById('calcular-ruta').addEventListener('click', async () => {
  const info = document.getElementById('info-ruta');
  const origen = document.getElementById('origen').value;
  const destino = document.getElementById('destino').value;
  try {
    const ruta = await api(`/api/ruta?origen=${origen}&destino=${destino}`);
    capas.clearLayers();
    const linea = L.polyline(ruta.coordenadas, { weight: 5 }).addTo(capas);
    mapa.fitBounds(linea.getBounds());
    info.textContent = `Tiempo de viaje: ${minutos(ruta.tiempoS)} min · ` +
      `nodos explorados: ${ruta.nodosExplorados}`;
  } catch (e) {
    mostrarError(info, e);
  }
});

// --- R5.2 Envolvente ---
document.getElementById('ver-envolvente').addEventListener('click', async () => {
  const info = document.getElementById('info-envolvente');
  try {
    const vertices = await api('/api/envolvente');
    capas.clearLayers();
    const poligono = L.polygon(vertices.map((v) => [Number(v.lat), Number(v.lon)]))
      .addTo(capas);
    mapa.fitBounds(poligono.getBounds());
    info.textContent = `${vertices.length} vértices`;
  } catch (e) {
    mostrarError(info, e);
  }
});

// --- R5.3 Itinerario ---
function tablaItinerario(itinerario) {
  const filas = itinerario.paradas.map((p) => `<tr><td>${p.orden}</td><td>${p.nombre}</td>` +
    `<td>${p.desplazamientoMin.toFixed(1)}</td><td>${p.visitaMin.toFixed(0)}</td>` +
    `<td>${p.tiempoAcumuladoH.toFixed(2)}</td></tr>`).join('');
  return `<table><tr><th>Orden</th><th>Lugar</th><th>Desplaz. (min)</th>` +
    `<th>Visita (min)</th><th>Acum. (h)</th></tr>${filas}</table>` +
    `<p>Jornada: ${(itinerario.duracionJornadaMin / 60).toFixed(2)} h</p>`;
}

document.getElementById('calcular-itinerario').addEventListener('click', async () => {
  const info = document.getElementById('info-itinerario');
  const atractivos = [...document.querySelectorAll('#lista-atractivos input:checked')]
    .map((c) => c.value);
  try {
    const comparacion = await api('/api/itinerario', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partida: document.getElementById('partida').value, atractivos }),
    });
    info.innerHTML = `<h3>Held-Karp (óptimo)</h3>${tablaItinerario(comparacion.optimo)}` +
      `<h3>Heurística</h3>${tablaItinerario(comparacion.heuristico)}` +
      `<p>Diferencia: ${comparacion.diferenciaMin.toFixed(1)} min ` +
      `(${comparacion.diferenciaPct.toFixed(2)} %)</p>`;
    capas.clearLayers();
    const coordenadas = (it) => it.paradas.map((p) => lugares.find((l) => l.id === p.idLugar))
      .filter((l) => l && l.lat != null).map((l) => [l.lat, l.lon]);
    const inicio = lugares.find((l) => l.id === comparacion.optimo.idPartida);
    const conInicio = (c) => (inicio && inicio.lat != null ? [[inicio.lat, inicio.lon], ...c] : c);
    L.polyline(conInicio(coordenadas(comparacion.optimo)), { color: '#2d6a4f' }).addTo(capas);
    L.polyline(conInicio(coordenadas(comparacion.heuristico)),
      { color: '#d62828', dashArray: '6 6' }).addTo(capas);
  } catch (e) {
    mostrarError(info, e);
  }
});

// --- R5.4 Resultados ---
document.getElementById('ver-tabla').addEventListener('click', async () => {
  const destino = document.getElementById('contenido-tabla');
  try {
    const filas = await api(`/api/resultados?nombre=${document.getElementById('tabla').value}`);
    const columnas = filas.length ? Object.keys(filas[0]) : [];
    destino.innerHTML = `<table><tr>${columnas.map((c) => `<th>${c}</th>`).join('')}</tr>` +
      filas.map((f) => `<tr>${columnas.map((c) => `<td>${f[c]}</td>`).join('')}</tr>`).join('') +
      '</table>';
  } catch (e) {
    mostrarError(destino, e);
  }
});

async function cargarFiguras() {
  try {
    const figuras = await api('/api/figuras');
    document.getElementById('figuras').innerHTML = figuras.length
      ? figuras.map((f) => `<img src="/api/figuras/${f}" alt="${f}">`).join('')
      : '<p class="info">Aún no hay figuras (python -m preparacion graficas).</p>';
  } catch (e) {
    mostrarError(document.getElementById('figuras'), e);
  }
}

cargarLugares();
cargarFiguras();
