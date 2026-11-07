import { ServicioTransporte } from './servicio-transporte.service';
/** Recorta cada detención al intervalo medido: no la atribuye a conducir ni cargar. */
export function minutosAveria(
  s: ServicioTransporte,
  inicio: string | null | undefined,
  fin: string | null | undefined,
): number {
  if (!inicio || !fin) return 0;
  const a = Date.parse(inicio),
    b = Date.parse(fin);
  if (!Number.isFinite(a) || !Number.isFinite(b) || b < a) return 0;
  return (s.incidencias || []).reduce((total, i) => {
    const x = Date.parse(i.fechaReporte),
      y = i.fechaResolucion ? Date.parse(i.fechaResolucion) : b;
    if (!Number.isFinite(x) || !Number.isFinite(y)) return total;
    return total + Math.max(0, Math.min(b, y) - Math.max(a, x)) / 60000;
  }, 0);
}
