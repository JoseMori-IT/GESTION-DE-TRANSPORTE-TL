import { evaluarDistancia } from '../../services/distancia-verificable';
import { minutosAveria } from '../../services/tiempos-averia';
import { ServicioTransporte as Servicio } from '../../services/servicio-transporte.service';
export interface FiltrosIndicadores {
  desde: string;
  hasta: string;
  ruta: string;
  chofer: string;
  estado?: string;
}
export function minutos(
  inicio: string | null | undefined,
  fin: string | null | undefined,
  ahora: number,
): number | null {
  if (!inicio || !fin) return null;
  const a = Date.parse(inicio),
    b = Date.parse(fin);
  return Number.isFinite(a) && Number.isFinite(b) && b >= a && b <= ahora ? (b - a) / 60000 : null;
}
export const promedio = (valores: number[]) =>
  valores.length ? valores.reduce((a, b) => a + b, 0) / valores.length : null;
export function calcularIndicadores(datos: Servicio[], f: FiltrosIndicadores, ahora: number) {
  const auxilios = datos.filter((s) => s.servicioAuxiliadoId);
  const base = datos
    .filter((s) => !s.servicioAuxiliadoId)
    .filter(
      (s) =>
        (f.ruta === 'TODAS' ||
          (s.ruta?.nombre || `${s.origenDescripcion} → ${s.destinoDescripcion}`) === f.ruta) &&
        (f.chofer === 'TODOS' || s.conductor.id === Number(f.chofer)),
    );
  const periodo = base.filter(
    (s) =>
      (!f.desde && !f.hasta) ||
      (!!s.fechaHoraSalida &&
        (!f.desde || s.fechaHoraSalida.slice(0, 10) >= f.desde) &&
        (!f.hasta || s.fechaHoraSalida.slice(0, 10) <= f.hasta)),
  );
  const servicios = periodo.filter(
    (s) => !f.estado || f.estado === 'TODOS' || s.estado === f.estado,
  );
  const completados = servicios.filter((s) => s.estado === 'COMPLETADO');
  const iniciados = servicios.filter(
    (s) =>
      s.estado !== 'CANCELADO' &&
      minutos(s.fechaHoraSalidaReal, s.fechaHoraSalidaReal, ahora) !== null &&
      !!s.fechaHoraSalida &&
      Number.isFinite(Date.parse(s.fechaHoraSalida)),
  );
  const puntuales = iniciados.filter(
    (s) => Date.parse(s.fechaHoraSalidaReal!) <= Date.parse(s.fechaHoraSalida!),
  );
  const pendientes = servicios.filter(
    (s) =>
      s.estado === 'PROGRAMADO' && !!s.fechaHoraSalida && Date.parse(s.fechaHoraSalida) < ahora,
  );
  const salidas = iniciados.map((s) => ({
    s,
    atraso: Math.max(
      0,
      (Date.parse(s.fechaHoraSalidaReal!) - Date.parse(s.fechaHoraSalida!)) / 60000,
    ),
  }));
  const atrasos = salidas.filter((s) => s.atraso > 0);
  const detalles = completados.map((s) => {
    const tramos = [...(s.tramos || [])].sort((a, b) => a.orden - b.orden);
    const tiempos = tramos.map((t) => {
      const n = minutos(t.salidaReal, t.llegadaReal, ahora);
      return n === null ? null : Math.max(0, n - minutosAveria(s, t.salidaReal, t.llegadaReal));
    });
    const estancias = tramos.slice(0, -1).map((t, i) => {
      const n = minutos(t.llegadaReal, tramos[i + 1].salidaReal, ahora);
      return n === null
        ? null
        : Math.max(0, n - minutosAveria(s, t.llegadaReal, tramos[i + 1].salidaReal));
    });
    const total = minutos(s.fechaHoraSalidaReal, s.fechaHoraLlegadaReal, ahora);
    const secuencia = tramos.every(
      (t, i) => i === 0 || minutos(tramos[i - 1].llegadaReal, t.salidaReal, ahora) !== null,
    );
    const conduccion =
      tramos.length && secuencia && tiempos.every((t) => t !== null)
        ? (tiempos as number[]).reduce((a, b) => a + b, 0)
        : null;
    const permanencia =
      tramos.length > 1 && estancias.every((t) => t !== null)
        ? (estancias as number[]).reduce((a, b) => a + b, 0)
        : null;
    const distancia = evaluarDistancia(s);
    const km = distancia.fuente === 'ODOMETRO' ? distancia.km : null,
      plan = distancia.plan;
    return {
      s,
      distancia,
      total,
      conduccion,
      permanencia,
      averia: minutosAveria(s, s.fechaHoraSalidaReal, s.fechaHoraLlegadaReal),
      km,
      plan,
      recorrido: tramos.length
        ? [tramos[0].origen, ...tramos.map((t) => t.destino)].join(' → ')
        : `${s.origenDescripcion} → ${s.destinoDescripcion}`,
    };
  });
  const validos = (campo: 'total' | 'conduccion' | 'permanencia') =>
    detalles.flatMap((d) => (d[campo] === null ? [] : [d[campo]!]));
  const pares = detalles.filter((d) => d.km !== null && d.plan !== null);
  const real = pares.reduce((a, d) => a + d.km!, 0),
    plan = pares.reduce((a, d) => a + d.plan!, 0);
  return {
    auxilios,
    interrumpidos: servicios.filter((s) => s.estado === 'INTERRUMPIDO'),
    servicios,
    completados,
    detalles,
    iniciados,
    puntuales,
    pendientes,
    salidas,
    atrasos,
    atrasoPromedio: promedio(atrasos.map((s) => s.atraso)),
    omitidos: base.filter((s) => !s.fechaHoraSalida).length,
    totalPromedio: promedio(validos('total')),
    conduccionPromedio: promedio(validos('conduccion')),
    permanenciaPromedio: promedio(validos('permanencia')),
    muestras: {
      total: validos('total').length,
      conduccion: validos('conduccion').length,
      permanencia: validos('permanencia').length,
    },
    pares: pares.length,
    kmReal: real,
    kmPlan: plan,
    desviacion: pares.length ? ((real - plan) / plan) * 100 : null,
    sinOdometro: detalles.filter((d) => d.km === null),
    sinTiempos: detalles.filter((d) => d.total === null),
    cancelados: servicios.filter((s) => s.estado === 'CANCELADO').length,
  };
}
