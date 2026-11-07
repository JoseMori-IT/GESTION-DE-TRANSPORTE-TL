import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';
import { AccesoOfflineService } from './acceso-offline.service';

interface FotoPendiente {
  id: string;
  usuario: string;
  servicioId: number;
  iv: number[];
  datos: ArrayBuffer;
}
@Injectable({ providedIn: 'root' })
export class FotosOfflineService {
  private readonly acceso = inject(AccesoOfflineService);
  private readonly http = inject(HttpClient);
  private base(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const r = indexedDB.open('transporte-fotos-offline', 1);
      r.onupgradeneeded = () => r.result.createObjectStore('fotos', { keyPath: 'id' });
      r.onsuccess = () => resolve(r.result);
      r.onerror = () => reject(r.error);
    });
  }
  async pendientes(): Promise<FotoPendiente[]> {
    const usuario = sessionStorage.getItem('usuarioId');
    if (!usuario) return [];
    const db = await this.base();
    return new Promise((resolve, reject) => {
      const r = db.transaction('fotos').objectStore('fotos').getAll();
      r.onsuccess = () => {
        db.close();
        resolve(r.result.filter((f: FotoPendiente) => f.usuario === usuario));
      };
      r.onerror = () => {
        db.close();
        reject(r.error);
      };
    });
  }
  async guardar(servicioId: number, formulario: FormData): Promise<void> {
    const usuario = sessionStorage.getItem('usuarioId');
    const archivo = formulario.get('archivo');
    if (!usuario || !(archivo instanceof File))
      throw new Error('Selecciona el archivo y verifica tu sesión.');
    if (
      archivo.size > 10 * 1024 * 1024 ||
      !['image/jpeg', 'image/png', 'image/webp', 'application/pdf'].includes(archivo.type)
    )
      throw new Error('Archivo no permitido o mayor de 10 MB.');
    const pendientes = await this.pendientes();
    if (pendientes.length >= 20)
      throw new Error('Hay 20 archivos pendientes. Sincroniza antes de agregar más.');
    const campos: Record<string, string> = {};
    formulario.forEach((v, k) => {
      if (typeof v === 'string') campos[k] = v;
    });
    const cabecera = new TextEncoder().encode(
      JSON.stringify({ campos, nombre: archivo.name, tipo: archivo.type }),
    );
    const contenido = new Uint8Array(4 + cabecera.length + archivo.size);
    new DataView(contenido.buffer).setUint32(0, cabecera.length);
    contenido.set(cabecera, 4);
    contenido.set(new Uint8Array(await archivo.arrayBuffer()), 4 + cabecera.length);
    const sobre = await this.acceso.cifrar(contenido.buffer);
    if (sessionStorage.getItem('usuarioId') !== usuario)
      throw new Error('La sesión cambió. Vuelve a guardar el archivo.');
    const fila: FotoPendiente = { id: crypto.randomUUID(), usuario, servicioId, ...sobre };
    const db = await this.base();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction('fotos', 'readwrite');
      tx.objectStore('fotos').add(fila);
      tx.oncomplete = () => {
        db.close();
        resolve();
      };
      tx.onerror = () => {
        db.close();
        reject(tx.error);
      };
    });
  }
  async sincronizar(servicioId: number): Promise<void> {
    if (sessionStorage.getItem('accesoOffline') === 'true')
      throw new Error('Inicia sesión en la red corporativa para sincronizar.');
    for (const fila of (await this.pendientes()).filter((f) => f.servicioId === servicioId)) {
      const contenido = await this.acceso.descifrar(fila);
      const longitud = new DataView(contenido).getUint32(0);
      const meta = JSON.parse(new TextDecoder().decode(contenido.slice(4, 4 + longitud)));
      const formulario = new FormData();
      for (const [k, v] of Object.entries(meta.campos)) formulario.set(k, String(v));
      formulario.set(
        'archivo',
        new Blob([contenido.slice(4 + longitud)], { type: meta.tipo }),
        meta.nombre,
      );
      formulario.set('eventoUuid', fila.id);
      if (fila.usuario !== sessionStorage.getItem('usuarioId'))
        throw new Error('La sesión cambió.');
      await firstValueFrom(
        this.http.post(`/api/evidencias/servicio/${servicioId}`, formulario).pipe(timeout(20000)),
      );
      const db = await this.base();
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction('fotos', 'readwrite');
        tx.objectStore('fotos').delete(fila.id);
        tx.oncomplete = () => {
          db.close();
          resolve();
        };
        tx.onerror = () => {
          db.close();
          reject(tx.error);
        };
      });
    }
  }
}
