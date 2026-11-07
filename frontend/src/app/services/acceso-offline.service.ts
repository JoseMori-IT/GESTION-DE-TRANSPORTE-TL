import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AccesoOfflineService {
  private clave: CryptoKey | null = null;
  private propietario: string | null = null;
  readonly nombreAlmacen = 'transporte-acceso-local-v1';
  private readonly reanudacion = 'transporte-reanudar-v2';
  private readonly duracionLocal = 24 * 60 * 60 * 1000;
  disponible(): boolean {
    return this.anteriorDisponible() || this.automaticoDisponible();
  }
  anteriorDisponible(): boolean {
    return !!localStorage.getItem(this.nombreAlmacen);
  }
  automaticoDisponible(): boolean {
    try {
      const permiso = JSON.parse(localStorage.getItem(this.reanudacion) || 'null');
      return !!permiso?.usuario && Date.now() >= permiso.desde && Date.now() < permiso.hasta;
    } catch {
      return false;
    }
  }
  private baseAutomatica(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const solicitud = indexedDB.open('transporte-claves-dispositivo', 1);
      solicitud.onupgradeneeded = () =>
        solicitud.result.createObjectStore('claves', { keyPath: 'usuario' });
      solicitud.onsuccess = () => resolve(solicitud.result);
      solicitud.onerror = () =>
        reject(new Error('No se pudo acceder al almacenamiento protegido de este dispositivo.'));
    });
  }
  private async leerClave(usuario: string): Promise<ClaveDispositivo | undefined> {
    const db = await this.baseAutomatica();
    return new Promise((resolve, reject) => {
      const solicitud = db.transaction('claves').objectStore('claves').get(usuario);
      solicitud.onsuccess = () => {
        db.close();
        resolve(solicitud.result);
      };
      solicitud.onerror = () => {
        db.close();
        reject(solicitud.error);
      };
    });
  }
  private async guardarClave(fila: ClaveDispositivo): Promise<void> {
    const db = await this.baseAutomatica();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction('claves', 'readwrite');
      tx.objectStore('claves').put(fila);
      tx.oncomplete = () => {
        db.close();
        resolve();
      };
      tx.onerror = tx.onabort = () => {
        db.close();
        reject(
          new Error('No se pudo preparar el dispositivo. Los archivos existentes se conservan.'),
        );
      };
    });
  }
  async prepararAutomatico(fraseAnterior?: string): Promise<void> {
    const usuario = sessionStorage.getItem('usuarioId');
    if (
      !usuario ||
      sessionStorage.getItem('rol') !== 'CHOFER' ||
      !sessionStorage.getItem('token') ||
      sessionStorage.getItem('accesoOffline') === 'true' ||
      sessionStorage.getItem('cambioContrasenaRequerido') === 'true'
    )
      throw new Error('Inicia sesión en la red corporativa para preparar este dispositivo.');
    if (!crypto.subtle) throw new Error('Se necesita HTTPS confiable para proteger los archivos.');
    const existente = await this.leerClave(usuario);
    let clave = existente?.clave;
    let migrado = false;
    if (!clave && this.anteriorDisponible()) {
      if (!fraseAnterior)
        throw new Error(
          'Hay protección de una versión anterior. Introduce la frase anterior una sola vez para conservar los archivos.',
        );
      await this.desbloquear(fraseAnterior, false);
      clave = this.clave!;
      migrado = true;
    }
    clave ??= await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, [
      'encrypt',
      'decrypt',
    ]);
    const perfil = Object.fromEntries(
      ['usuarioId', 'conductorId', 'nombres'].map((k) => [k, sessionStorage.getItem(k)]),
    );
    if (!perfil['conductorId']) throw new Error('Cuenta de chofer incompleta.');
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const datos = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      clave,
      new TextEncoder().encode(JSON.stringify(perfil)),
    );
    const desde = Date.now();
    const fila: ClaveDispositivo = {
      usuario,
      clave,
      iv: Array.from(iv),
      datos,
      desde,
      hasta: desde + this.duracionLocal,
    };
    if (sessionStorage.getItem('usuarioId') !== usuario || !sessionStorage.getItem('token'))
      throw new Error('La sesión cambió. Vuelve a ingresar.');
    await this.guardarClave(fila);
    if (sessionStorage.getItem('usuarioId') !== usuario || !sessionStorage.getItem('token'))
      throw new Error('La sesión cambió. Vuelve a ingresar.');
    localStorage.setItem(
      this.reanudacion,
      JSON.stringify({ usuario, desde: fila.desde, hasta: fila.hasta }),
    );
    this.clave = clave;
    this.propietario = usuario;
    // Se conserva la misma clave AES: no se reescribe ni elimina la cola de fotos.
    if (migrado) {
      localStorage.removeItem(this.nombreAlmacen);
      localStorage.removeItem(this.nombreAlmacen + '-intentos');
    }
  }
  async reanudarAutomatico(): Promise<void> {
    if (!this.automaticoDisponible())
      throw new Error('Inicia sesión en la red corporativa para renovar el acceso al viaje.');
    const permisoOriginal = localStorage.getItem(this.reanudacion);
    const permiso = JSON.parse(permisoOriginal!);
    if (sessionStorage.getItem('token') && sessionStorage.getItem('usuarioId') !== permiso.usuario)
      throw new Error('Ya hay otra cuenta activa. Cierra su sesión antes de abrir el viaje local.');
    const fila = await this.leerClave(permiso.usuario);
    if (
      !fila ||
      fila.desde !== permiso.desde ||
      fila.hasta !== permiso.hasta ||
      Date.now() < fila.desde ||
      Date.now() >= fila.hasta ||
      localStorage.getItem(this.reanudacion) !== permisoOriginal
    )
      throw new Error(
        'El acceso local ya no está disponible. Inicia sesión en la red corporativa.',
      );
    const datos = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: new Uint8Array(fila.iv) },
      fila.clave,
      fila.datos,
    );
    const perfil = JSON.parse(new TextDecoder().decode(datos));
    if (
      perfil.usuarioId !== fila.usuario ||
      localStorage.getItem(this.reanudacion) !== permisoOriginal
    )
      throw new Error('No se pudo verificar el propietario del viaje.');
    sessionStorage.clear();
    for (const k of ['usuarioId', 'conductorId', 'nombres'])
      if (perfil[k]) sessionStorage.setItem(k, perfil[k]);
    sessionStorage.setItem('rol', 'CHOFER');
    sessionStorage.setItem('accesoOffline', 'true');
    sessionStorage.setItem('modoAccesoLocal', 'automatico');
    this.clave = fila.clave;
    this.propietario = fila.usuario;
  }
  desbloqueado(): boolean {
    return !!this.clave && this.propietario === sessionStorage.getItem('usuarioId');
  }
  validarSesionLocal(): void {
    if (
      sessionStorage.getItem('accesoOffline') === 'true' &&
      sessionStorage.getItem('modoAccesoLocal') === 'automatico' &&
      !this.automaticoDisponible()
    )
      throw new Error(
        'El acceso sin conexión venció. Conéctate a la red corporativa e inicia sesión. Tus pendientes se conservan.',
      );
  }
  private async derivar(frase: string, salt: Uint8Array<ArrayBuffer>): Promise<CryptoKey> {
    const material = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(frase),
      'PBKDF2',
      false,
      ['deriveKey'],
    );
    return crypto.subtle.deriveKey(
      { name: 'PBKDF2', salt, iterations: 310000, hash: 'SHA-256' },
      material,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt'],
    );
  }
  async preparar(frase: string): Promise<void> {
    if (
      sessionStorage.getItem('rol') !== 'CHOFER' ||
      !sessionStorage.getItem('token') ||
      sessionStorage.getItem('accesoOffline') === 'true'
    )
      throw new Error('Primero inicia sesión en la red corporativa.');
    if (frase.length < 12)
      throw new Error(
        'Usa una frase local de al menos 12 caracteres, diferente de tu contraseña corporativa.',
      );
    if (!crypto.subtle) throw new Error('El acceso local requiere HTTPS confiable.');
    // No reemplazar una clave que protege archivos pendientes.
    if (this.anteriorDisponible()) {
      await this.desbloquear(frase, false);
      return;
    }
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const clave = await this.derivar(frase, salt);
    const perfil = Object.fromEntries(
      ['usuarioId', 'conductorId', 'nombres'].map((k) => [k, sessionStorage.getItem(k)]),
    );
    if (!perfil['usuarioId'] || !perfil['conductorId'])
      throw new Error('Cuenta de chofer incompleta.');
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const datos = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      clave,
      new TextEncoder().encode(JSON.stringify(perfil)),
    );
    localStorage.setItem(
      this.nombreAlmacen,
      JSON.stringify({
        salt: Array.from(salt),
        iv: Array.from(iv),
        datos: Array.from(new Uint8Array(datos)),
      }),
    );
    this.clave = clave;
    this.propietario = perfil['usuarioId'];
  }
  async desbloquear(frase: string, restaurar = true): Promise<void> {
    const bloqueo = JSON.parse(localStorage.getItem(this.nombreAlmacen + '-intentos') || '{}');
    if (bloqueo.hasta > Date.now())
      throw new Error('Espera 15 minutos antes de reintentar el desbloqueo local.');
    let perfil: Record<string, string>;
    let clave: CryptoKey;
    try {
      const sobre = JSON.parse(localStorage.getItem(this.nombreAlmacen) || 'null');
      if (!sobre) throw new Error();
      clave = await this.derivar(frase, new Uint8Array(sobre.salt));
      const datos = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv: new Uint8Array(sobre.iv) },
        clave,
        new Uint8Array(sobre.datos),
      );
      perfil = JSON.parse(new TextDecoder().decode(datos));
    } catch {
      const intentos = (bloqueo.intentos || 0) + 1;
      localStorage.setItem(
        this.nombreAlmacen + '-intentos',
        JSON.stringify({
          intentos: intentos >= 5 ? 0 : intentos,
          hasta: intentos >= 5 ? Date.now() + 900000 : 0,
        }),
      );
      throw new Error('No se pudo desbloquear el acceso local. Revisa la frase y el dispositivo.');
    }
    if (!restaurar && perfil['usuarioId'] !== sessionStorage.getItem('usuarioId'))
      throw new Error(
        'El acceso local pertenece a otro chofer. No se reemplazará mientras pueda haber datos pendientes.',
      );
    this.clave = clave;
    this.propietario = perfil['usuarioId'];
    localStorage.removeItem(this.nombreAlmacen + '-intentos');
    if (restaurar) {
      sessionStorage.clear();
      for (const [k, v] of Object.entries(perfil)) if (v) sessionStorage.setItem(k, v);
      sessionStorage.setItem('rol', 'CHOFER');
      sessionStorage.setItem('accesoOffline', 'true');
      sessionStorage.setItem('modoAccesoLocal', 'anterior');
    }
  }
  async cifrar(datos: ArrayBuffer): Promise<{ iv: number[]; datos: ArrayBuffer }> {
    this.validarSesionLocal();
    if (!this.clave || this.propietario !== sessionStorage.getItem('usuarioId'))
      throw new Error('Prepara el dispositivo desde Configuración antes de guardar fotografías.');
    const iv = crypto.getRandomValues(new Uint8Array(12));
    return {
      iv: Array.from(iv),
      datos: await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, this.clave, datos),
    };
  }
  async descifrar(sobre: { iv: number[]; datos: ArrayBuffer }): Promise<ArrayBuffer> {
    this.validarSesionLocal();
    if (!this.clave || this.propietario !== sessionStorage.getItem('usuarioId'))
      throw new Error('Desbloquea el dispositivo desde Configuración para enviar los archivos.');
    return crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: new Uint8Array(sobre.iv) },
      this.clave,
      sobre.datos,
    );
  }
  bloquear(): void {
    this.clave = null;
    this.propietario = null;
    try {
      localStorage.removeItem(this.reanudacion);
    } catch {
      /* No conservar la clave en memoria si falla el almacenamiento. */
    }
  }
}

interface ClaveDispositivo {
  usuario: string;
  clave: CryptoKey;
  iv: number[];
  datos: ArrayBuffer;
  desde: number;
  hasta: number;
}
