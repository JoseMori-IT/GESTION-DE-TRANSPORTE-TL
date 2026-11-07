import { ServicioTransporte } from './servicio-transporte.service';

export function fechaViaje(servicio: ServicioTransporte): string {
  const fecha =
    servicio.fechaHoraSalidaReal || servicio.fechaHoraLlegadaReal || servicio.fechaHoraSalida;
  if (!fecha) return 'Fecha no registrada';
  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return fecha.slice(0, 10);
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(valor);
}

export function fechaHoraViaje(fecha: string | null): string {
  if (!fecha) return 'No registrada';
  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return fecha.replace('T', ' ').slice(0, 16);
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(valor);
}

export function nombreProducto(producto: string): string {
  const productos: Record<string, string> = {
    RFF: 'Racimo de Fruto Fresco',
    FS: 'Fruto Suelto',
    ESCOBAJO: 'Escobajo',
    FIBRA: 'Fibra',
    TORTA_CLARIFICACION: 'Torta de clarificación',
    LODO_BIOGAS: 'Lodo de biogás',
    CENIZA: 'Ceniza',
    RESIDUOS_VARIOS: 'Residuos varios',
  };

  return productos[producto] || producto;
}
