import { Observable } from 'rxjs';

/** Solo permite formatos pasivos validados por el servidor al registrar. */
export function abrirVistaPreviaEvidencia(
  evidencia: { nombreOriginal: string; tipoContenido: string },
  cargar: () => Observable<Blob>,
  error: (mensaje: string) => void,
): void {
  const tipo = evidencia.tipoContenido?.trim().toLowerCase();
  if (!['image/jpeg', 'image/png', 'image/webp', 'application/pdf'].includes(tipo)) {
    error('Este formato no admite vista previa.');
    return;
  }
  // Abrir dentro del clic evita el bloqueo de ventanas tras una petición asíncrona.
  const ventana = window.open('', '_blank');
  if (!ventana) {
    error('Permite las ventanas emergentes para visualizar la evidencia.');
    return;
  }
  ventana.opener = null;
  const doc = ventana.document;
  doc.title = evidencia.nombreOriginal;
  const estilo = doc.createElement('style');
  estilo.textContent =
    'html,body{margin:0;height:100%;background:#f5f5f7;color:#1d1d1f;font-family:Arial,sans-serif}body{display:flex;flex-direction:column}header{padding:16px;display:flex;gap:16px;align-items:center;flex-wrap:wrap;background:white}header span{flex:1;overflow-wrap:anywhere}a{color:#007aff}img{display:block;max-width:100%;min-height:0;flex:1;object-fit:contain}iframe{width:100%;flex:1;border:0;min-height:75vh}p{padding:24px}';
  doc.head.append(estilo);
  const espera = doc.createElement('p');
  espera.textContent = 'Cargando evidencia…';
  doc.body.replaceChildren(espera);
  cargar().subscribe({
    next: (archivo) => {
      if (ventana.closed) return;
      const url = URL.createObjectURL(new Blob([archivo], { type: tipo }));
      const encabezado = doc.createElement('header');
      const nombre = doc.createElement('span');
      nombre.textContent = evidencia.nombreOriginal;
      const descargar = doc.createElement('a');
      descargar.textContent = 'Descargar archivo';
      descargar.href = url;
      descargar.download = evidencia.nombreOriginal;
      encabezado.append(nombre, descargar);
      const visor = doc.createElement(tipo === 'application/pdf' ? 'iframe' : 'img');
      visor.setAttribute('src', url);
      visor.setAttribute('title', evidencia.nombreOriginal);
      if (tipo !== 'application/pdf') visor.setAttribute('alt', 'Evidencia del viaje');
      doc.body.replaceChildren(encabezado, visor);
      const limpieza = window.setInterval(() => {
        if (ventana.closed) {
          URL.revokeObjectURL(url);
          window.clearInterval(limpieza);
        }
      }, 2000);
    },
    error: () => {
      ventana.close();
      error('No fue posible abrir la evidencia. Comprueba tu conexión y vuelve a intentarlo.');
    },
  });
}
