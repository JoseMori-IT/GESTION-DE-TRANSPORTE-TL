import { ServicioTransporte } from '../../services/servicio-transporte.service';
import { evaluarDistancia } from '../../services/distancia-verificable';
import { FiltrosIndicadores } from './indicadores-calculo';
export interface ReferenciaConsumo {
  version: string;
  fecha: string;
  combustible: string;
  galonesPorKm: number;
  litrosPorGalon: number;
  litrosPor100Km: number;
  kgCo2PorKm: number;
  fuente: string;
}
export function calcularConsumo(
  datos: ServicioTransporte[],
  f: FiltrosIndicadores,
  referencia: ReferenciaConsumo | null,
) {
  const configurada = referencia !== null;
  // Zero placeholders are never used for estimates without a confirmed configuration.
  const ref = referencia ?? {
    version: 'No disponible',
    fecha: '',
    combustible: 'Diésel',
    galonesPorKm: 0,
    litrosPorGalon: 0,
    litrosPor100Km: 0,
    kgCo2PorKm: 0,
    fuente: 'No se pudo consultar la configuración.',
  };
  const seleccion = datos.filter(
    (s) =>
      (!f.estado || f.estado === 'TODOS' || s.estado === f.estado) &&
      (f.chofer === 'TODOS' || s.conductor.id === Number(f.chofer)) &&
      (f.ruta === 'TODAS' ||
        (s.ruta?.nombre || `${s.origenDescripcion} → ${s.destinoDescripcion}`) === f.ruta) &&
      ((!f.desde && !f.hasta) ||
        (!!s.fechaHoraSalida &&
          (!f.desde || s.fechaHoraSalida.slice(0, 10) >= f.desde) &&
          (!f.hasta || s.fechaHoraSalida.slice(0, 10) <= f.hasta))),
  );
  // Un viaje incompleto no acredita que haya recorrido todo el plan. Auxilios completados si consumen.
  const cerrados = seleccion.filter((s) => s.estado === 'COMPLETADO');
  const filas = cerrados.map((s) => {
    const distancia = evaluarDistancia(s);
    const d = {
      ...distancia,
      fuente: distancia.fuente as typeof distancia.fuente | 'SIN_REFERENCIA',
    };
    return {
      s,
      ...d,
      galones: !configurada || d.km === null ? null : d.km * ref.galonesPorKm,
      litros: !configurada || d.km === null ? null : (d.km * ref.litrosPor100Km) / 100,
      co2: !configurada || d.km === null ? null : d.km * ref.kgCo2PorKm,
    };
  });
  const incluidos = filas.filter((d) => d.km !== null),
    revision = filas.filter((d) => d.fuente === 'REVISION'),
    sinDatos = filas.filter((d) => d.fuente === 'SIN_DATOS'),
    sinReferencia = filas.filter((d) => d.fuente === 'SIN_REFERENCIA');
  const suma = (campo: 'km' | 'galones' | 'litros' | 'co2') =>
    incluidos.length && (campo === 'km' || configurada)
      ? incluidos.reduce((n, d) => n + d[campo]!, 0)
      : null;
  const agrupar = (por: 'mes' | 'unidad') => {
    const grupos = new Map<
      string,
      {
        clave: string;
        nombre: string;
        galones: number;
        litros: number;
        co2: number;
        km: number;
        kmOdometro: number;
        kmRuta: number;
        viajes: number;
        excluidos: number;
      }
    >();
    for (const d of filas) {
      const clave =
        por === 'mes' ? d.s.fechaHoraSalida?.slice(0, 7) || 'SIN_FECHA' : String(d.s.vehiculo.id);
      const g = grupos.get(clave) || {
        clave,
        nombre: por === 'mes' ? clave : d.s.vehiculo.placa,
        galones: 0,
        litros: 0,
        co2: 0,
        km: 0,
        kmOdometro: 0,
        kmRuta: 0,
        viajes: 0,
        excluidos: 0,
      };
      if (d.km === null) {
        g.excluidos++;
        grupos.set(clave, g);
        continue;
      }
      g.galones += d.galones ?? 0;
      g.litros += d.litros ?? 0;
      g.co2 += d.co2 ?? 0;
      g.km += d.km!;
      g.viajes++;
      if (d.fuente === 'ODOMETRO') g.kmOdometro += d.km!;
      else g.kmRuta += d.km!;
      grupos.set(clave, g);
    }
    return [...grupos.values()]
      .filter((g) => por === 'mes' || g.viajes > 0)
      .sort((a, b) => (por === 'mes' ? a.clave.localeCompare(b.clave) : b.litros - a.litros));
  };
  const conOdo = incluidos.filter((d) => d.fuente === 'ODOMETRO'),
    conRuta = incluidos.filter((d) => d.fuente === 'RUTA');
  return {
    configurada,
    referencia: ref,
    filas,
    incluidos,
    revision,
    sinDatos,
    sinReferencia,
    cerrados,
    seleccion,
    km: suma('km'),
    galones: suma('galones'),
    litros: suma('litros'),
    co2: suma('co2'),
    kmOdometro: conOdo.reduce((n, d) => n + d.km!, 0),
    kmRuta: conRuta.reduce((n, d) => n + d.km!, 0),
    conOdometro: conOdo.length,
    conRuta: conRuta.length,
    auxilios: incluidos.filter((d) => d.s.servicioAuxiliadoId).length,
    interrumpidos: seleccion.filter((s) => s.estado === 'INTERRUMPIDO').length,
    porMes: configurada ? agrupar('mes') : [],
    porUnidad: configurada ? agrupar('unidad') : [],
  };
}
