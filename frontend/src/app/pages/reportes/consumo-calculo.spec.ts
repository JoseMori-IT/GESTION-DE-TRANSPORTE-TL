import { describe, it, expect } from 'vitest';
import { calcularConsumo as calcular, ReferenciaConsumo } from './consumo-calculo';
const REFERENCIA_CONSUMO: ReferenciaConsumo = {
  version: 'PRUEBA',
  fecha: '2026-11-06',
  combustible: 'Diésel',
  galonesPorKm: 1,
  litrosPorGalon: 3.785411784,
  litrosPor100Km: 378.5411784,
  kgCo2PorKm: 1,
  fuente: 'Fixture de pruebas',
};
const calcularConsumo = (datos: ServicioTransporte[], f: Parameters<typeof calcular>[1]) =>
  calcular(datos, f, REFERENCIA_CONSUMO);
import { calcularIndicadores } from './indicadores-calculo';
import { ServicioTransporte } from '../../services/servicio-transporte.service';
const f = { desde: '', hasta: '', ruta: 'TODAS', chofer: 'TODOS', estado: 'TODOS' };
function viaje(id = 1, km: number | null = 100, plan: number | null = 100): ServicioTransporte {
  return {
    id,
    codigo: 'SER-' + id,
    estado: 'COMPLETADO',
    vehiculo: { id: 1, placa: 'ABC', tipo: 'CAMION' },
    conductor: { id: 1 },
    origenDescripcion: 'Planta',
    destinoDescripcion: 'Planta',
    fechaHoraSalida: '2026-10-29T08:00:00',
    distanciaEstimadaKm: plan,
    tramos: [
      { orden: 1, origen: 'Planta', destino: 'Acopio', odometroSalidaKm: 1000 },
      {
        orden: 2,
        origen: 'Acopio',
        destino: 'Planta',
        odometroLlegadaKm: km === null ? null : 1000 + km,
      },
    ],
  } as ServicioTransporte;
}
describe('Consumo estimado con referencia interna y cobertura verificable', () => {
  it('aplica 1 galón/km y 1 kg CO2/km una sola vez', () => {
    const c = calcularConsumo([viaje()], f);
    expect(c.galones).toBe(100);
    expect(c.litros).toBeCloseTo(378.5411784);
    expect(c.co2).toBe(100);
    expect(c.kmOdometro).toBe(100);
    expect(c.conRuta).toBe(0);
    expect(c.referencia.version).toBe(REFERENCIA_CONSUMO.version);
  });
  it('usa la ruta si falta una lectura e identifica la fuente estimada', () => {
    const c = calcularConsumo([viaje(1, null, 30.74)], f);
    expect(c.galones).toBeCloseTo(30.74);
    expect(c.kmRuta).toBe(30.74);
    expect(c.conRuta).toBe(1);
    expect(c.filas[0].fuente).toBe('RUTA');
  });
  it('excluye el caso 50000 a 58000 frente a 30.74 km sin alterar lecturas ni sustituir por el plan', () => {
    const s = viaje(1, 8000, 30.74);
    s.tramos![0].odometroSalidaKm = 50000;
    s.tramos![1].odometroLlegadaKm = 58000;
    const c = calcularConsumo([s], f);
    expect(c.revision.length).toBe(1);
    expect(c.litros).toBeNull();
    expect(c.filas[0].registrado).toBe(8000);
    expect(c.filas[0].km).toBeNull();
    expect(s.tramos![1].odometroLlegadaKm).toBe(58000);
    expect(calcularIndicadores([s], f, Date.now()).kmReal).toBe(0);
  });
  it('no reemplaza una lectura final igual o menor por la ruta', () => {
    for (const km of [0, -10]) {
      const c = calcularConsumo([viaje(1, km)], f);
      expect(c.revision.length).toBe(1);
      expect(c.incluidos.length).toBe(0);
    }
  });
  it('no elimina desviaciones moderadas por un desvio real', () => {
    expect(calcularConsumo([viaje(1, 150, 92.28)], f).incluidos.length).toBe(1);
  });
  it('separa ausencia de datos de un consumo cero', () => {
    const c = calcularConsumo([viaje(1, null, null)], f);
    expect(c.sinDatos.length).toBe(1);
    expect(c.litros).toBeNull();
    expect(c.co2).toBeNull();
  });
  it('incluye auxilios completados sin duplicar el viaje original', () => {
    const aux = { ...viaje(2, 20, 20), servicioAuxiliadoId: 1 };
    const interrumpido = { ...viaje(3), estado: 'INTERRUMPIDO' } as ServicioTransporte;
    const c = calcularConsumo([viaje(), aux, interrumpido], f);
    expect(c.galones).toBe(120);
    expect(c.auxilios).toBe(1);
    expect(c.interrumpidos).toBe(1);
    expect(c.cerrados.length).toBe(2);
  });
  it('no extrapola viajes activos cancelados o interrumpidos', () => {
    const estados = ['PROGRAMADO', 'EN_CURSO', 'CANCELADO', 'INTERRUMPIDO'];
    const c = calcularConsumo(
      estados.map((estado, n) => ({ ...viaje(n + 1), estado }) as ServicioTransporte),
      f,
    );
    expect(c.incluidos.length).toBe(0);
    expect(c.litros).toBeNull();
  });
  it('combina filtros y agrupa meses de años distintos sin mezclarlos', () => {
    const a = viaje(),
      b = { ...viaje(2), fechaHoraSalida: '2025-10-29T08:00:00' },
      c = { ...viaje(3), fechaHoraSalida: '2026-11-01T08:00:00' };
    expect(calcularConsumo([a, b, c], f).porMes.map((m) => m.clave)).toEqual([
      '2025-10',
      '2026-10',
      '2026-11',
    ]);
    const filtrado = calcularConsumo([a, b, c], {
      ...f,
      desde: '2026-10-01',
      hasta: '2026-10-31',
      chofer: '1',
      ruta: 'Planta → Planta',
      estado: 'COMPLETADO',
    });
    expect(filtrado.galones).toBe(100);
    expect(calcularConsumo([a], { ...f, estado: 'CANCELADO' }).litros).toBeNull();
  });
  it('los grupos y las fuentes suman el total, sin redondear cada viaje', () => {
    const a = viaje(1, 10.01, 10),
      b = viaje(2, null, 20.02);
    b.vehiculo = { ...b.vehiculo, id: 2, placa: 'OTR' };
    const c = calcularConsumo([a, b], f);
    expect(c.km).toBeCloseTo(30.03);
    expect(c.porUnidad.reduce((n, u) => n + u.litros, 0)).toBeCloseTo(c.litros!);
    expect(c.porMes.reduce((n, u) => n + u.co2, 0)).toBeCloseTo(c.co2!);
    expect(c.kmOdometro + c.kmRuta).toBeCloseTo(c.km!);
  });
});

describe('Referencia segun unidad', () => {
  it('aplica la referencia operativa a todas las unidades', () => {
    const s = viaje();
    s.vehiculo.tipo = 'CAMIONETA';
    const d = calcularConsumo([s], f);
    expect(d.galones).toBe(100);
    expect(d.sinReferencia.length).toBe(0);
  });
});

describe('Mes con informacion no utilizable', () => {
  it('mantiene el mes visible con una exclusion, sin presentarlo como consumo cero medido', () => {
    const d = calcularConsumo([viaje(1, 8000, 30.74)], f);
    expect(d.porMes.length).toBe(1);
    expect(d.porMes[0].viajes).toBe(0);
    expect(d.porMes[0].excluidos).toBe(1);
    expect(d.litros).toBeNull();
  });
});

describe('Parámetros del servidor', () => {
  it('aplica factores configurados sin asumir un galón por km', () => {
    const c = calcular([viaje()], f, {
      ...REFERENCIA_CONSUMO,
      galonesPorKm: 0.35,
      litrosPor100Km: 0.35 * 3.785411784 * 100,
      kgCo2PorKm: 0.9,
    });
    expect(c.galones).toBe(35);
    expect(c.co2).toBe(90);
  });
  it('no inventa estimaciones cuando falla la configuración', () => {
    const c = calcular([viaje()], f, null);
    expect(c.configurada).toBe(false);
    expect(c.galones).toBeNull();
    expect(c.co2).toBeNull();
    expect(c.filas[0].galones).toBeNull();
    expect(c.porMes).toEqual([]);
    expect(c.km).toBe(100);
  });
});
