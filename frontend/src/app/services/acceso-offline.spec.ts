import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { webcrypto } from 'node:crypto';
import { AccesoOfflineService } from './acceso-offline.service';

describe('Protección del acceso local', () => {
  beforeEach(() => {
    vi.stubGlobal('crypto', webcrypto);
    sessionStorage.setItem('rol', 'CHOFER');
    sessionStorage.setItem('usuarioId', '17');
    sessionStorage.setItem('conductorId', '22');
    sessionStorage.setItem('nombres', 'Conductor de prueba');
    sessionStorage.setItem('token', 'TOKEN-NO-DEBE-PERSISTIR');
  });
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    vi.unstubAllGlobals();
  });
  it('recupera solo el perfil local tras reiniciar sin persistir token ni contraseña', async () => {
    const api = new AccesoOfflineService();
    await api.preparar('Frase de prueba local 2026');
    const persistido = localStorage.getItem(api.nombreAlmacen)!;
    expect(persistido).not.toContain('TOKEN-NO-DEBE-PERSISTIR');
    expect(persistido).not.toContain('Conductor de prueba');
    sessionStorage.clear();
    const nueva = new AccesoOfflineService();
    await nueva.desbloquear('Frase de prueba local 2026');
    expect(sessionStorage.getItem('usuarioId')).toBe('17');
    expect(sessionStorage.getItem('accesoOffline')).toBe('true');
    expect(sessionStorage.getItem('token')).toBeNull();
  });
  it('cifra archivos y permite recuperarlos con la misma frase tras reiniciar', async () => {
    const api = new AccesoOfflineService();
    await api.preparar('Frase de prueba local 2026');
    const sobre = await api.cifrar(new TextEncoder().encode('Foto de prueba').buffer);
    api.bloquear();
    await expect(api.descifrar(sobre)).rejects.toThrow('Desbloquea');
    await api.desbloquear('Frase de prueba local 2026', false);
    expect(new TextDecoder().decode(await api.descifrar(sobre))).toBe('Foto de prueba');
  });
  it('rechaza frase incorrecta y no restaura la sesión', async () => {
    const api = new AccesoOfflineService();
    await api.preparar('Frase de prueba local 2026');
    sessionStorage.clear();
    await expect(new AccesoOfflineService().desbloquear('Frase incorrecta')).rejects.toThrow(
      'No se pudo desbloquear',
    );
    expect(sessionStorage.getItem('usuarioId')).toBeNull();
  });
  it('no cifra fotografías para otra cuenta que reutilice el dispositivo', async () => {
    const api = new AccesoOfflineService();
    await api.preparar('Frase de prueba local 2026');
    sessionStorage.setItem('usuarioId', '99');
    await expect(api.cifrar(new ArrayBuffer(1))).rejects.toThrow('Prepara');
  });
  it('informa el desbloqueo solo para el propietario y lo revoca al bloquear', async () => {
    const api = new AccesoOfflineService();
    expect(api.desbloqueado()).toBe(false);
    await api.preparar('Frase de prueba local 2026');
    expect(api.desbloqueado()).toBe(true);
    sessionStorage.setItem('usuarioId', '99');
    expect(api.desbloqueado()).toBe(false);
    sessionStorage.setItem('usuarioId', '17');
    api.bloquear();
    expect(api.desbloqueado()).toBe(false);
  });
});
