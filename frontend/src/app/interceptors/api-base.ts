// Mantener /api/ en desarrollo y anteponer el base href en despliegues por subruta.
export function apiDesdeBase(url: string, baseUri: string): string {
  if (!url.startsWith('/api/')) return url;
  const destino = new URL(url.slice(1), baseUri);
  return destino.pathname + destino.search + destino.hash;
}
