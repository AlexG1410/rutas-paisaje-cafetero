// Pruebas R5 en Edge headless mediante CDP; no añade paquetes de npm.
// Requiere servidor en http://localhost:8080. No mide algoritmos.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { setTimeout: esperar } = require('node:timers/promises');

async function main() {
  const raiz = path.join(process.env.LOCALAPPDATA, 'Temp', 'opencode');
  const perfil = await fs.mkdtemp(path.join(raiz, 'r5-edge-'));
  const edge = spawn(process.env.R5_EDGE || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe', [
    '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--remote-debugging-port=0', '--remote-debugging-address=127.0.0.1',
    `--user-data-dir=${perfil}`, 'about:blank',
  ], { stdio: 'ignore' });
  let errorArranque;
  edge.on('error', (error) => { errorArranque = error; });
  let ws;
  let id = 0;
  const pendientes = new Map();
  const excepciones = [];
  async function hasta(condicion, descripcion, limite = 30000) {
    const inicio = Date.now();
    while (Date.now() - inicio < limite) {
      if (errorArranque) throw errorArranque;
      const valor = await condicion();
      if (valor) return valor;
      await esperar(100);
    }
    throw new Error(`Tiempo agotado: ${descripcion}`);
  }
  const enviar = (method, params = {}) => new Promise((resolve, reject) => {
    const numero = ++id;
    pendientes.set(numero, { resolve, reject });
    ws.send(JSON.stringify({ id: numero, method, params }));
  });
  async function evaluar(expression) {
    const resultado = await enviar('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (resultado.exceptionDetails) throw new Error(JSON.stringify(resultado.exceptionDetails));
    return resultado.result.value;
  }
  async function listo(selector) {
    await hasta(() => evaluar(`!!document.querySelector(${JSON.stringify(selector)})`), selector);
  }
  async function captura(nombre) {
    // Dejar terminar la animación de zoom y cargar las teselas del encuadre nuevo.
    await esperar(1200);
    await hasta(() => evaluar('Array.from(document.querySelectorAll(".leaflet-tile")).every(i => i.complete)'), 'teselas para captura');
    const imagen = await enviar('Page.captureScreenshot', { format: 'png' });
    await fs.writeFile(path.join('build', 'r5', nombre + '.png'), Buffer.from(imagen.data, 'base64'));
  }
  async function pestana(nombre) {
    await evaluar(`document.querySelector('[data-panel="${nombre}"]').click()`);
  }
  try {
    const puerto = await hasta(async () => {
      try { return (await fs.readFile(path.join(perfil, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
      catch { return null; }
    }, 'arranque Edge');
    const tabs = await (await fetch(`http://localhost:${puerto}/json/list`)).json();
    ws = new WebSocket(tabs.find((t) => t.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { ws.addEventListener('open', resolve); ws.addEventListener('error', reject); });
    ws.addEventListener('message', (evento) => {
      const mensaje = JSON.parse(evento.data);
      if (mensaje.method === 'Runtime.exceptionThrown') excepciones.push(mensaje.params);
      const espera = pendientes.get(mensaje.id);
      if (espera) {
        pendientes.delete(mensaje.id);
        if (mensaje.error) espera.reject(new Error(JSON.stringify(mensaje.error)));
        else espera.resolve(mensaje.result);
      }
    });
    await enviar('Runtime.enable');
    await enviar('Page.enable');
    await enviar('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
    await fs.mkdir(path.join('build', 'r5'), { recursive: true });
    await enviar('Page.navigate', { url: 'http://localhost:8080' });
    await listo('#origen option');
    await hasta(() => evaluar('!!window.L && !!document.querySelector(".leaflet-tile-loaded")'), 'Leaflet y teselas', 45000);
    assert.equal(await evaluar('document.querySelectorAll("#origen option").length'), 40);
    await evaluar('document.querySelector("#calcular-ruta").click()');
    await hasta(() => evaluar('document.querySelector("#info-ruta").textContent.includes("Tiempo estimado")'), 'ruta');
    assert.ok(await evaluar('document.querySelectorAll("#mapa path").length >= 1'));
    await captura('ruta');
    await pestana('envolvente');
    await evaluar('document.querySelector("#ver-envolvente").click()');
    await hasta(() => evaluar('document.querySelector("#info-envolvente").textContent.includes("14 vértices")'), 'envolvente');
    await captura('envolvente');
    await pestana('itinerario');
    await evaluar('document.querySelector("#calcular-itinerario").click()');
    await hasta(() => evaluar('document.querySelector(".estado-mapa")?.textContent.includes("calculados con Dijkstra")'), 'itinerarios', 60000);
    assert.equal(await evaluar('document.querySelectorAll("#info-itinerario table").length'), 2);
    assert.equal(await evaluar('document.querySelectorAll("#info-itinerario tbody tr").length'), 16);
    await captura('itinerario');
    await evaluar('document.querySelector("#lista-atractivos input[value=atr_valle_cocora]").click(); document.querySelector("#calcular-itinerario").click()');
    await hasta(() => evaluar('document.querySelectorAll("#info-itinerario .advertencia").length === 2'), 'jornada superior a 11 h');
    await hasta(() => evaluar('!document.querySelector("#calcular-itinerario").disabled'), 'fin de geometría');
    await evaluar('document.querySelectorAll("#lista-atractivos input").forEach(c => c.checked=false); document.querySelector("#calcular-itinerario").click()');
    await hasta(() => evaluar('document.querySelector("#info-itinerario .error")?.textContent.includes("6 y 8")'), 'selección inválida');
    await pestana('resultados');
    await evaluar('document.querySelector("#tabla").value="r4/exactas.csv"; document.querySelector("#ver-tabla").click()');
    await listo('#contenido-tabla tbody tr');
    assert.equal(await evaluar('document.querySelectorAll("#contenido-tabla tbody tr").length'), 8);
    await hasta(() => evaluar('Array.from(document.querySelectorAll("#figuras img")).every(i => i.complete && i.naturalWidth > 0)'), 'figuras');
    await captura('resultados');
    // Error de API y contenido no confiable: debe quedar como texto, nunca HTML ejecutable.
    await evaluar(`window.fetchOriginal = window.fetch; window.fetch = async () => new Response(JSON.stringify({error:'<img src=x onerror="window.inyectado=true">'}), {status:404,headers:{'Content-Type':'application/json'}}); document.querySelector('#ver-tabla').click()`);
    await listo('#contenido-tabla .error');
    assert.equal(await evaluar('document.querySelectorAll("#contenido-tabla img").length'), 0);
    assert.equal(await evaluar('window.inyectado === true'), false);
    await evaluar('window.fetch = window.fetchOriginal');
    // Una petición retrasada no debe volver a dibujar tras cambiar de pestaña.
    await pestana('ruta');
    await evaluar('window.fetch = () => new Promise(resolve => { window.resolverTardia = resolve; }); document.querySelector("#calcular-ruta").click()');
    await pestana('envolvente');
    await evaluar(`window.resolverTardia(new Response(JSON.stringify({tiempoS:1,nodosExplorados:1,coordenadas:[[4.5,-75.7]]}))); window.fetch=window.fetchOriginal`);
    await hasta(() => evaluar('!document.querySelector("#calcular-ruta").disabled'), 'respuesta tardía');
    assert.equal(await evaluar('capas.getLayers().length'), 0,
      JSON.stringify(await evaluar('({version, capas:capas.getLayers().length, info:document.querySelector("#info-ruta").textContent})')));
    // Vista móvil sin desbordamiento horizontal del documento.
    await enviar('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
    await captura('movil');
    assert.ok(await evaluar('document.documentElement.scrollWidth <= window.innerWidth'));
    // Recargar sin Leaflet, simulando CDN ausente; resultados aún deben funcionar.
    await enviar('Page.addScriptToEvaluateOnNewDocument', {
      source: `Object.defineProperty(window,'L',{get:()=>undefined,set:()=>{},configurable:true});`,
    });
    await enviar('Page.reload');
    await listo('#origen option');
    assert.ok(await evaluar('document.querySelector("#aviso-mapa").textContent.includes("Mapa no disponible")'));
    await pestana('resultados');
    await evaluar('document.querySelector("#ver-tabla").click()');
    await listo('#contenido-tabla table');
    assert.equal(excepciones.length, 0, JSON.stringify(excepciones));
    console.log('R5 navegador: cuatro funciones, datos reales, 11 h, errores/XSS, respuesta tardía, móvil y CDN ausente: OK. Capturas en build/r5/.');
    await enviar('Browser.close');
  } finally {
    ws?.close();
    if (edge.exitCode === null) edge.kill();
  }
}
main().catch((error) => { console.error(error); process.exitCode = 1; });
