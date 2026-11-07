import { ServicioTransporte } from './servicio-transporte.service';
// Regla de revision operativa, no limite tecnico o legal. No modifica lecturas.
export const REGLA_REVISION_KM = Object.freeze({ multiplicadorPlan: 3, excesoMinimoKm: 100 });
export function evaluarDistancia(s: ServicioTransporte) {
  const ts = [...(s.tramos ?? [])].sort((a, b) => a.orden - b.orden);
  const inicial = ts[0]?.odometroSalidaKm ?? null,
    final = ts.at(-1)?.odometroLlegadaKm ?? null;
  const positivo = (n: unknown): n is number =>
    typeof n === 'number' && Number.isFinite(n) && n > 0;
  const tramosPlan =
    ts.length && ts.every((t) => positivo(t.distanciaEstimadaKm))
      ? ts.reduce((n, t) => n + t.distanciaEstimadaKm!, 0)
      : null;
  const plan = positivo(s.distanciaEstimadaKm) ? s.distanciaEstimadaKm : tramosPlan;
  const resultado = (
    fuente: 'ODOMETRO' | 'RUTA' | 'REVISION' | 'SIN_DATOS',
    km: number | null,
    motivo: string,
    registrado: number | null = null,
  ) => ({ fuente, km, motivo, registrado, plan, inicial, final });
  if (ts[0]?.odometroNoDisponibleSalida || ts.at(-1)?.odometroNoDisponibleLlegada)
    return plan !== null
      ? resultado('RUTA', plan, 'Odómetro averiado reportado; distancia estimada por ruta.')
      : resultado('SIN_DATOS', null, 'Odómetro averiado y ruta sin distancia válida.');
  if (inicial === null || final === null)
    return plan !== null
      ? resultado('RUTA', plan, 'Lecturas incompletas; se utiliza la ruta planificada.')
      : resultado('SIN_DATOS', null, 'Faltan lecturas y una distancia planificada valida.');
  if (!Number.isFinite(inicial) || !Number.isFinite(final) || inicial < 0 || final <= inicial)
    return resultado(
      'REVISION',
      null,
      'Revisar odometro: la lectura final debe ser mayor que la inicial.',
    );
  const km = final - inicial;
  if (
    plan !== null &&
    km > plan * REGLA_REVISION_KM.multiplicadorPlan &&
    km - plan > REGLA_REVISION_KM.excesoMinimoKm
  )
    return resultado(
      'REVISION',
      null,
      'Lectura supera 3 veces el plan y lo excede en mas de 100 km.',
      km,
    );
  return resultado('ODOMETRO', km, 'Distancia obtenida de las lecturas registradas.', km);
}
