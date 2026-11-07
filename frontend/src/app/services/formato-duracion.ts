/** Redondea solo la presentacion; los calculos conservan segundos y decimales. */
export function formatearDuracionMinutos(valor: number | null | undefined): string {
  if (typeof valor !== 'number' || !Number.isFinite(valor) || valor < 0) return 'Sin datos';
  const total = Math.round(valor);
  if (total < 60) return `${total} min`;
  const dias = Math.floor(total / 1440),
    horas = Math.floor((total % 1440) / 60),
    minutos = total % 60;
  return [dias ? `${dias} d` : '', horas ? `${horas} h` : '', minutos ? `${minutos} min` : '']
    .filter(Boolean)
    .join(' ');
}
