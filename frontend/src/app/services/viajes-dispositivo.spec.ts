import { afterEach, describe, expect, it } from 'vitest';
import { ViajesDispositivoService } from './viajes-dispositivo.service';
import { ServicioTransporte } from './servicio-transporte.service';

describe('Itinerario descargado', () => {
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });
  it('guarda solo viajes activos y omite documentos y copiloto', () => {
    sessionStorage.setItem('usuarioId', '1');
    sessionStorage.setItem('rol', 'CHOFER');
    const api = new ViajesDispositivoService();
    const viaje = {
      id: 1,
      estado: 'PROGRAMADO',
      conductor: { numeroDocumento: '12345678', numeroLicencia: 'LICENCIA' },
      copiloto: { nombres: 'Privado' },
    } as ServicioTransporte;
    api.guardar([viaje, { ...viaje, id: 2, estado: 'COMPLETADO' }]);
    const copia = api.leer()!;
    expect(copia.servicios.length).toBe(1);
    expect(copia.servicios[0].conductor.numeroDocumento).toBeNull();
    expect(copia.servicios[0].conductor.numeroLicencia).toBeNull();
    expect(copia.servicios[0].copiloto).toBeNull();
    expect(copia.fecha).toBeTruthy();
  });
  it('no recupera los viajes de otra cuenta', () => {
    sessionStorage.setItem('usuarioId', '1');
    sessionStorage.setItem('rol', 'CHOFER');
    const api = new ViajesDispositivoService();
    api.guardar([]);
    sessionStorage.setItem('usuarioId', '2');
    expect(api.leer()).toBeNull();
  });
  it('no entrega copias sin sesión', () => {
    expect(() => new ViajesDispositivoService().leer()).toThrow('Inicia sesión');
  });
});
