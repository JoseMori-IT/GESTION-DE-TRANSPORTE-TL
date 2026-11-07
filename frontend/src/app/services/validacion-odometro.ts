import { ServicioTransporte } from './servicio-transporte.service';
import { evaluarDistancia, REGLA_REVISION_KM } from './distancia-verificable';
export function validarLecturaViaje(
  s: ServicioTransporte,
  inicio: boolean,
  lectura: number | null,
  sinLectura: boolean,
  motivo: string,
  ultima: number | null,
  averiado: boolean,
): string | null {
  if (sinLectura)
    return motivo.trim().length >= 5 && motivo.length <= 300
      ? null
      : 'Describe por qué no puedes leer el odómetro (entre 5 y 300 caracteres).';
  if (averiado)
    return 'El odómetro figura averiado. Marca la excepción o solicita al coordinador confirmar su reparación.';
  if (
    lectura == null ||
    !Number.isFinite(lectura) ||
    lectura < 0 ||
    lectura > 99999999.99 ||
    Math.abs(lectura * 100 - Math.round(lectura * 100)) > 0.0001
  )
    return 'Ingresa el odómetro entre 0 y 99999999.99 km, con hasta dos decimales.';
  if (ultima != null && lectura < ultima)
    return `La lectura no puede ser menor que la última válida: ${ultima} km.`;
  const d = evaluarDistancia(s),
    base = inicio ? ultima : d.inicial;
  if (!inicio && base != null && lectura <= base)
    return 'El odómetro final debe ser mayor que el de salida.';
  if (
    base != null &&
    d.plan != null &&
    lectura - base > d.plan * REGLA_REVISION_KM.multiplicadorPlan &&
    lectura - base - d.plan > REGLA_REVISION_KM.excesoMinimoKm
  )
    return 'La lectura supera 3 veces el plan y lo excede en más de 100 km. Corrige la lectura o solicita revisión al coordinador.';
  return null;
}
