import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, Observable, timeout } from 'rxjs';
import { FotosOfflineService } from './fotos-offline.service';
import { AccesoOfflineService } from './acceso-offline.service';

export type TipoMarcacionServicio =
  'SALIDA_IDA' | 'LLEGADA_DESTINO' | 'SALIDA_RETORNO' | 'LLEGADA_BASE';

export interface MarcacionPendiente {
  propietarioUsuarioId?: string;
  odometroKm?: number | null;
  odometroNoDisponible?: boolean;
  motivoOdometro?: string | null;
  tramoOrden?: number | null;
  eventoUuid: string;
  servicioId: number;
  tipo: TipoMarcacionServicio;
  latitud: number;
  longitud: number;
  precisionMetros: number;
  fechaHoraDispositivo: string;
  registradoOffline: boolean;
  observacion: string | null;
  intentosSincronizacion: number;
  ultimoError: string | null;
}

export interface MarcacionServicioResponse extends Omit<
  MarcacionPendiente,
  'intentosSincronizacion' | 'ultimoError'
> {
  id: number;
  fechaHoraServidor: string;
}

@Injectable({ providedIn: 'root' })
export class MarcacionServicioService {
  private readonly accesoLocal = inject(AccesoOfflineService);
  private readonly fotos = inject(FotosOfflineService);
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/chofer/servicios';
  private readonly baseDatos = 'transporte-chofer-offline';
  private readonly almacen = 'marcaciones-pendientes';

  listar(servicioId: number): Observable<MarcacionServicioResponse[]> {
    return this.http.get<MarcacionServicioResponse[]>(`${this.baseUrl}/${servicioId}/marcaciones`);
  }

  async capturar(
    servicioId: number,
    tipo: TipoMarcacionServicio,
    observacion: string | null = null,
    tramoOrden: number | null = null,
    odometroKm: number | null = null,
    odometroNoDisponible = false,
    motivoOdometro: string | null = null,
  ): Promise<MarcacionPendiente> {
    this.accesoLocal.validarSesionLocal();
    const usuarioId = this.usuarioActual();
    const posicion = await this.obtenerPosicion();
    this.accesoLocal.validarSesionLocal();
    if (usuarioId !== sessionStorage.getItem('usuarioId')) {
      throw new Error('La sesión cambió durante la captura. Vuelve a registrar la marcación.');
    }

    if (posicion.coords.accuracy > 100) {
      throw new Error(
        `La precisión GPS es de ${Math.round(posicion.coords.accuracy)} m. ` +
          'Ubícate en un lugar despejado e inténtalo nuevamente.',
      );
    }

    const marcacion: MarcacionPendiente = {
      propietarioUsuarioId: usuarioId,
      tramoOrden,
      odometroKm,
      odometroNoDisponible,
      motivoOdometro,
      eventoUuid: crypto.randomUUID(),
      servicioId,
      tipo,
      latitud: Number(posicion.coords.latitude.toFixed(7)),
      longitud: Number(posicion.coords.longitude.toFixed(7)),
      precisionMetros: Number(posicion.coords.accuracy.toFixed(2)),
      fechaHoraDispositivo: this.fechaLocalIso(new Date(posicion.timestamp)),
      registradoOffline: !navigator.onLine,
      observacion,
      intentosSincronizacion: 0,
      ultimoError: null,
    };

    await this.guardar(marcacion);
    return marcacion;
  }

  async pendientes(): Promise<MarcacionPendiente[]> {
    const usuarioId = this.usuarioActual();
    const db = await this.abrirBaseDatos();
    return new Promise((resolve, reject) => {
      const transaccion = db.transaction(this.almacen, 'readonly');
      const solicitud = transaccion.objectStore(this.almacen).getAll();
      solicitud.onsuccess = () =>
        resolve(
          (solicitud.result as MarcacionPendiente[])
            .filter((m) => m.propietarioUsuarioId === usuarioId)
            .sort((a, b) => a.fechaHoraDispositivo.localeCompare(b.fechaHoraDispositivo)),
        );
      solicitud.onerror = () => reject(solicitud.error);
    });
  }

  async sincronizarPendientes(
    servicioIds?: number[],
  ): Promise<{ enviados: number; pendientes: number }> {
    const propietario = this.usuarioActual();
    if (!navigator.onLine) {
      return { enviados: 0, pendientes: (await this.pendientes()).length };
    }

    let enviados = 0;
    const cola = (await this.pendientes()).filter(
      (m) => !servicioIds || servicioIds.includes(m.servicioId),
    );

    for (const marcacion of cola) {
      if (marcacion.propietarioUsuarioId !== sessionStorage.getItem('usuarioId')) break;
      try {
        if (marcacion.tipo !== 'SALIDA_IDA') await this.fotos.sincronizar(marcacion.servicioId);
        const confirmada = await firstValueFrom(
          this.http
            .post<MarcacionServicioResponse>(
              `${this.baseUrl}/${marcacion.servicioId}/marcaciones`,
              {
                tramoOrden: marcacion.tramoOrden,
                odometroKm: marcacion.odometroKm ?? null,
                odometroNoDisponible: marcacion.odometroNoDisponible ?? false,
                motivoOdometro: marcacion.motivoOdometro ?? null,
                eventoUuid: marcacion.eventoUuid,
                tipo: marcacion.tipo,
                latitud: marcacion.latitud,
                longitud: marcacion.longitud,
                precisionMetros: marcacion.precisionMetros,
                fechaHoraDispositivo: marcacion.fechaHoraDispositivo,
                registradoOffline: marcacion.registradoOffline,
                observacion: marcacion.observacion,
              },
            )
            .pipe(timeout(20000)),
        );
        this.actualizarCopia({
          ...confirmada,
          odometroKm: marcacion.odometroKm,
          odometroNoDisponible: marcacion.odometroNoDisponible,
          motivoOdometro: marcacion.motivoOdometro,
        });
        await this.eliminar(marcacion.eventoUuid);
        enviados++;
        if (marcacion.tipo === 'SALIDA_IDA') await this.fotos.sincronizar(marcacion.servicioId);
      } catch (error: any) {
        const estado = Number(error?.status || 0);
        marcacion.intentosSincronizacion++;
        marcacion.ultimoError =
          error?.error?.mensaje || error?.message || 'No fue posible sincronizar.';
        await this.guardar(marcacion);

        // 400/409 requieren revisión; los siguientes eventos dependen del orden.
        if (estado === 400 || estado === 409 || estado === 401 || estado === 403) {
          break;
        }
        // Sin conexión o servidor no disponible: conservar toda la cola.
        if (estado === 0 || estado >= 500) {
          break;
        }
      }
    }

    return {
      enviados,
      pendientes:
        sessionStorage.getItem('usuarioId') === propietario
          ? (await this.pendientes()).length
          : cola.length - enviados,
    };
  }

  private obtenerPosicion(): Promise<GeolocationPosition> {
    if (!navigator.geolocation) {
      return Promise.reject(new Error('Este dispositivo no permite obtener la ubicación GPS.'));
    }

    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        resolve,
        (error) =>
          reject(
            new Error(
              error.code === error.PERMISSION_DENIED
                ? 'Debes permitir el acceso a la ubicación para registrar la marcación.'
                : 'No fue posible obtener una ubicación precisa. Activa el GPS e inténtalo nuevamente.',
            ),
          ),
        {
          enableHighAccuracy: true,
          timeout: 25_000,
          maximumAge: 0,
        },
      );
    });
  }

  private actualizarCopia(evento: MarcacionServicioResponse): void {
    if (evento.tramoOrden == null) return;
    const clave = 'viajes-chofer-' + this.usuarioActual();
    try {
      const viajes = JSON.parse(
        localStorage.getItem(clave) || '[]',
      ) as import('./servicio-transporte.service').ServicioTransporte[];
      const viaje = viajes.find((s) => s.id === evento.servicioId);
      const tramo = viaje?.tramos?.find((t) => t.orden === evento.tramoOrden);
      if (!viaje || !tramo) return;
      if (evento.odometroKm != null && evento.tramoOrden === 1 && evento.tipo === 'SALIDA_IDA')
        tramo.odometroSalidaKm = evento.odometroKm;
      if (
        evento.odometroKm != null &&
        evento.tramoOrden === viaje.tramos?.length &&
        evento.tipo === 'LLEGADA_DESTINO'
      )
        tramo.odometroLlegadaKm = evento.odometroKm;
      const extremo =
        (evento.tramoOrden === 1 && evento.tipo === 'SALIDA_IDA') ||
        (evento.tramoOrden === viaje.tramos?.length && evento.tipo === 'LLEGADA_DESTINO');
      if (extremo) {
        if (evento.tipo === 'SALIDA_IDA') {
          tramo.odometroNoDisponibleSalida = evento.odometroNoDisponible;
          tramo.motivoOdometroSalida = evento.motivoOdometro;
        } else {
          tramo.odometroNoDisponibleLlegada = evento.odometroNoDisponible;
          tramo.motivoOdometroLlegada = evento.motivoOdometro;
        }
        for (const otro of viajes.filter((s) => s.vehiculo.id === viaje.vehiculo.id)) {
          otro.vehiculo.estadoOdometro = evento.odometroNoDisponible ? 'AVERIADO' : 'OPERATIVO';
          if (
            evento.odometroKm != null &&
            (!otro.vehiculo.fechaOdometro ||
              evento.fechaHoraDispositivo >= otro.vehiculo.fechaOdometro)
          ) {
            otro.vehiculo.odometroKm = evento.odometroKm;
            otro.vehiculo.fechaOdometro = evento.fechaHoraDispositivo;
          }
        }
      }
      if (evento.tipo === 'SALIDA_IDA') {
        tramo.salidaReal = evento.fechaHoraDispositivo;
        viaje.estado = 'EN_CURSO';
      }
      if (evento.tipo === 'LLEGADA_DESTINO') {
        tramo.llegadaReal = evento.fechaHoraDispositivo;
        if (viaje.servicioAuxiliadoId && tramo.orden === 1 && viaje.tramos?.length === 2)
          viaje.tramos[1].salidaReal ||= evento.fechaHoraDispositivo;
      }
      if (viaje.tramos?.every((t) => t.llegadaReal)) viaje.estado = 'COMPLETADO';
      localStorage.setItem(clave, JSON.stringify(viajes));
    } catch {
      /* La recepción ya fue confirmada por el servidor. */
    }
  }

  private async guardar(marcacion: MarcacionPendiente): Promise<void> {
    const db = await this.abrirBaseDatos();
    return new Promise((resolve, reject) => {
      const transaccion = db.transaction(this.almacen, 'readwrite');
      transaccion.objectStore(this.almacen).put(marcacion);
      transaccion.oncomplete = () => resolve();
      transaccion.onerror = () => reject(transaccion.error);
    });
  }

  private async eliminar(eventoUuid: string): Promise<void> {
    const db = await this.abrirBaseDatos();
    return new Promise((resolve, reject) => {
      const transaccion = db.transaction(this.almacen, 'readwrite');
      transaccion.objectStore(this.almacen).delete(eventoUuid);
      transaccion.oncomplete = () => resolve();
      transaccion.onerror = () => reject(transaccion.error);
    });
  }

  private abrirBaseDatos(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const solicitud = indexedDB.open(this.baseDatos, 1);
      solicitud.onupgradeneeded = () => {
        if (!solicitud.result.objectStoreNames.contains(this.almacen)) {
          solicitud.result.createObjectStore(this.almacen, {
            keyPath: 'eventoUuid',
          });
        }
      };
      solicitud.onsuccess = () => resolve(solicitud.result);
      solicitud.onerror = () => reject(solicitud.error);
    });
  }

  private usuarioActual(): string {
    const id = sessionStorage.getItem('usuarioId');
    if (!id) throw new Error('Inicia sesión antes de consultar o guardar marcaciones.');
    return id;
  }

  private fechaLocalIso(fecha: Date): string {
    const dos = (valor: number) => String(valor).padStart(2, '0');
    const tres = (valor: number) => String(valor).padStart(3, '0');
    return (
      `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}` +
      `T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}:${dos(fecha.getSeconds())}` +
      `.${tres(fecha.getMilliseconds())}`
    );
  }
}
