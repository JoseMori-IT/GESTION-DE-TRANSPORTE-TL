package com.palmas.transporte.modulos.operacion.application;


import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.operacion.api.EvidenciaServicioResponse;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EvidenciaServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoEvidencia;
import com.palmas.transporte.modulos.operacion.persistence.EvidenciaServicioRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

@Service
public class EvidenciaServicioService {

    private static final long TAMANO_MAXIMO =
        10L * 1024L * 1024L;

    private static final Set<String>
        ROLES_LECTURA_GENERAL =
            Set.of(
                "MASTER", "COORDINADOR", "ADMINISTRADOR",
                "OPERADOR",
                "CONSULTA"
            );

    private static final Set<String>
        ROLES_REGISTRO_GENERAL =
            Set.of(
                "MASTER", "COORDINADOR", "ADMINISTRADOR",
                "OPERADOR"
            );

    private final EvidenciaServicioRepository
        evidenciaRepository;

    private final ServicioTransporteRepository
        servicioRepository;

    private final Path directorioEvidencias;

    public EvidenciaServicioService(
            EvidenciaServicioRepository
                evidenciaRepository,
            ServicioTransporteRepository
                servicioRepository,
            @Value("${app.evidencias.directorio}")
            String directorio) {

        this.evidenciaRepository =
            evidenciaRepository;

        this.servicioRepository =
            servicioRepository;

        this.directorioEvidencias =
            Path.of(directorio)
                .toAbsolutePath()
                .normalize();

        crearDirectorio();
    }

    @Transactional(readOnly = true)
    public List<EvidenciaServicioResponse>
        listarPorServicio(
            Long servicioId,
            String rol,
            Long conductorId) {

        ServicioTransporte servicio =
            verificarServicio(servicioId);

        validarLecturaPermitida(
            servicio,
            rol,
            conductorId
        );

        List<EvidenciaServicio> evidencias;

        if ("CHOFER".equals(rol)) {
            evidencias = evidenciaRepository
                .findByServicio_IdAndServicio_Conductor_IdOrderByFechaRegistroDesc(
                    servicioId,
                    conductorId
                );
        } else {
            evidencias = evidenciaRepository
                .findByServicio_IdOrderByFechaRegistroDesc(
                    servicioId
                );
        }

        return evidencias
            .stream()
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional
    public EvidenciaServicioResponse registrar(
            Long servicioId,
            TipoEvidencia tipo,
            MultipartFile archivo,
            String descripcion,
            BigDecimal latitud,
            BigDecimal longitud,
            String nombreResponsable,
            String documentoResponsable,
            String registradoPor,
            String rol,
            Long conductorId) {

        return registrar(servicioId, tipo, archivo, descripcion, latitud, longitud,
            nombreResponsable, documentoResponsable, registradoPor, rol, conductorId, null);
    }

    @Transactional
    public EvidenciaServicioResponse registrar(
            Long servicioId, TipoEvidencia tipo, MultipartFile archivo, String descripcion,
            BigDecimal latitud, BigDecimal longitud, String nombreResponsable,
            String documentoResponsable, String registradoPor, String rol, Long conductorId,
            String eventoUuid) {

        if (eventoUuid != null) {
            try { eventoUuid = UUID.fromString(eventoUuid).toString(); }
            catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de evidencia inválido"); }
            ServicioTransporte bloqueado = servicioRepository.bloquearEvidencias(servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
            validarLecturaPermitida(bloqueado, rol, conductorId);
            var existente = evidenciaRepository.findByNombreArchivoStartingWith("offline-" + eventoUuid + ".");
            if (existente.isPresent()) {
                var previa = existente.get();
                if (!previa.getServicio().getId().equals(servicioId) || previa.getTipo() != tipo)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificador usado en otra evidencia");
                return convertirRespuesta(previa);
            }
        }

        if (tipo == TipoEvidencia.CONFORMIDAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La conformidad se registra en Validación operativa desde la cuenta del responsable, no como evidencia del chofer.");
        }

        ServicioTransporte servicio =
            verificarServicio(servicioId);

        validarRegistroPermitido(
            servicio,
            rol,
            conductorId
        );

        TipoArchivoDetectado tipoDetectado =
            validarYDetectarArchivo(archivo);

        validarCoordenadas(
            latitud,
            longitud
        );

        validarConformidad(
            tipo,
            nombreResponsable,
            documentoResponsable
        );

        validarLongitud(
            descripcion,
            500,
            "La descripción"
        );

        validarLongitud(
            nombreResponsable,
            150,
            "El nombre del responsable"
        );

        validarLongitud(
            documentoResponsable,
            30,
            "El documento del responsable"
        );

        validarLongitud(
            registradoPor,
            150,
            "El usuario que registra"
        );

        String nombreOriginal =
            limpiarNombreOriginal(
                archivo.getOriginalFilename()
            );

        String nombreArchivo =
            (eventoUuid == null ? UUID.randomUUID().toString() : "offline-" + eventoUuid)
                + tipoDetectado.extension();

        Path destino =
            resolverRutaSegura(nombreArchivo);

        try (
            InputStream entrada =
                archivo.getInputStream()
        ) {
            /*
             * Sin REPLACE_EXISTING: nunca debe
             * sobrescribirse otra evidencia.
             */
            Files.copy(
                entrada,
                destino
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible almacenar la evidencia"
            );
        }

        EvidenciaServicio evidencia =
            new EvidenciaServicio();

        evidencia.setServicio(servicio);
        evidencia.setTipo(tipo);

        evidencia.setNombreOriginal(
            nombreOriginal
        );

        evidencia.setNombreArchivo(
            nombreArchivo
        );

        /*
         * Se guarda el MIME detectado por el servidor,
         * no el declarado por el navegador.
         */
        evidencia.setTipoContenido(
            tipoDetectado.tipoContenido()
        );

        evidencia.setTamanoBytes(
            archivo.getSize()
        );

        evidencia.setDescripcion(
            limpiarOpcional(descripcion)
        );

        evidencia.setLatitud(latitud);
        evidencia.setLongitud(longitud);

        evidencia.setNombreResponsable(
            limpiarOpcional(
                nombreResponsable
            )
        );

        evidencia.setDocumentoResponsable(
            limpiarOpcional(
                documentoResponsable
            )
        );

        evidencia.setRegistradoPor(
            limpiarObligatorio(
                registradoPor,
                "No fue posible identificar "
                    + "al usuario"
            )
        );

        try {
            EvidenciaServicio guardada =
                evidenciaRepository
                    .saveAndFlush(evidencia);

            return convertirRespuesta(
                guardada
            );
        } catch (RuntimeException exception) {
            eliminarArchivoSilenciosamente(
                destino
            );

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public EvidenciaServicio buscarPorId(
            Long id,
            String rol,
            Long conductorId) {

        if ("CHOFER".equals(rol)) {
            if (conductorId == null) {
                throw accesoDenegado();
            }

            /*
             * La consulta exige simultáneamente
             * evidencia y conductor propietario.
             */
            return evidenciaRepository
                .findByIdAndServicio_Conductor_Id(
                    id,
                    conductorId
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evidencia no encontrada"
                    )
                );
        }

        validarRolLectura(rol);

        return evidenciaRepository
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Evidencia no encontrada"
                )
            );
    }

    public Resource cargarArchivo(
            EvidenciaServicio evidencia) {

        if (
            evidencia == null
            || evidencia.getNombreArchivo() == null
            || evidencia.getNombreArchivo().isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Evidencia no encontrada"
            );
        }

        Path archivo = resolverRutaSegura(
            evidencia.getNombreArchivo()
        );

        try {
            if (
                !Files.isRegularFile(
                    archivo,
                    LinkOption.NOFOLLOW_LINKS
                )
                || !Files.isReadable(archivo)
            ) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El archivo de evidencia no existe"
                );
            }

            Path directorioReal =
                directorioEvidencias
                    .toRealPath();

            Path archivoReal =
                archivo.toRealPath(
                    LinkOption.NOFOLLOW_LINKS
                );

            if (
                !archivoReal.startsWith(
                    directorioReal
                )
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ubicación de la evidencia "
                        + "no es válida"
                );
            }

            return new UrlResource(
                archivoReal.toUri()
            );
        } catch (
            ResponseStatusException exception
        ) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No fue posible leer la evidencia"
            );
        }
    }

    private ServicioTransporte verificarServicio(
            Long servicioId) {

        if (servicioId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El servicio es obligatorio"
            );
        }

        return servicioRepository
            .findById(servicioId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Servicio de transporte "
                        + "no encontrado"
                )
            );
    }

    private void validarLecturaPermitida(
            ServicioTransporte servicio,
            String rol,
            Long conductorId) {

        if (
            rol != null
            && ROLES_LECTURA_GENERAL
                .contains(rol)
        ) {
            return;
        }

        if (
            "CHOFER".equals(rol)
            && conductorId != null
            && servicio.getConductor() != null
            && conductorId.equals(
                servicio
                    .getConductor()
                    .getId()
            )
        ) {
            return;
        }

        throw accesoDenegado();
    }

    private void validarRolLectura(
            String rol) {

        if (
            rol != null
            && ROLES_LECTURA_GENERAL
                .contains(rol)
        ) {
            return;
        }

        throw accesoDenegado();
    }

    private void validarRegistroPermitido(
            ServicioTransporte servicio,
            String rol,
            Long conductorId) {

        boolean autorizado =
            rol != null
            && ROLES_REGISTRO_GENERAL
                .contains(rol);

        if (
            "CHOFER".equals(rol)
            && conductorId != null
            && servicio.getConductor() != null
            && conductorId.equals(
                servicio
                    .getConductor()
                    .getId()
            )
        ) {
            autorizado = true;
        }

        if (!autorizado) {
            throw accesoDenegado();
        }

        if (
            servicio.getEstado()
                != EstadoServicio.EN_CURSO
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Las evidencias solo pueden registrarse "
                    + "mientras el servicio está en curso"
            );
        }
    }

    private TipoArchivoDetectado
        validarYDetectarArchivo(
            MultipartFile archivo) {

        if (
            archivo == null
            || archivo.isEmpty()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes seleccionar un archivo"
            );
        }

        if (
            archivo.getSize() <= 0
            || archivo.getSize()
                > TAMANO_MAXIMO
        ) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo no debe superar "
                    + "los 10 MB"
            );
        }

        byte[] cabecera;

        try (
            InputStream entrada =
                archivo.getInputStream()
        ) {
            cabecera =
                entrada.readNBytes(12);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No fue posible leer el archivo"
            );
        }

        if (esJpeg(cabecera)) {
            return new TipoArchivoDetectado(
                "image/jpeg",
                ".jpg"
            );
        }

        if (esPng(cabecera)) {
            return new TipoArchivoDetectado(
                "image/png",
                ".png"
            );
        }

        if (esWebp(cabecera)) {
            return new TipoArchivoDetectado(
                "image/webp",
                ".webp"
            );
        }

        if (esPdf(cabecera)) {
            return new TipoArchivoDetectado(
                "application/pdf",
                ".pdf"
            );
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El contenido del archivo no corresponde "
                + "a JPG, PNG, WEBP o PDF"
        );
    }

    private boolean esJpeg(
            byte[] datos) {

        return datos.length >= 3
            && byteSinSigno(datos[0]) == 0xFF
            && byteSinSigno(datos[1]) == 0xD8
            && byteSinSigno(datos[2]) == 0xFF;
    }

    private boolean esPng(
            byte[] datos) {

        int[] firma = {
            0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
        };

        return comienzaCon(
            datos,
            firma
        );
    }

    private boolean esPdf(
            byte[] datos) {

        int[] firma = {
            0x25,
            0x50,
            0x44,
            0x46,
            0x2D
        };

        return comienzaCon(
            datos,
            firma
        );
    }

    private boolean esWebp(
            byte[] datos) {

        return datos.length >= 12
            && datos[0] == 'R'
            && datos[1] == 'I'
            && datos[2] == 'F'
            && datos[3] == 'F'
            && datos[8] == 'W'
            && datos[9] == 'E'
            && datos[10] == 'B'
            && datos[11] == 'P';
    }

    private boolean comienzaCon(
            byte[] datos,
            int[] firma) {

        if (datos.length < firma.length) {
            return false;
        }

        for (
            int indice = 0;
            indice < firma.length;
            indice++
        ) {
            if (
                byteSinSigno(datos[indice])
                    != firma[indice]
            ) {
                return false;
            }
        }

        return true;
    }

    private int byteSinSigno(
            byte valor) {

        return valor & 0xFF;
    }

    private void validarCoordenadas(
            BigDecimal latitud,
            BigDecimal longitud) {

        if (
            (latitud == null)
                != (longitud == null)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes enviar latitud y "
                    + "longitud juntas"
            );
        }

        if (latitud == null) {
            return;
        }

        if (
            latitud.compareTo(
                BigDecimal.valueOf(-90)
            ) < 0
            || latitud.compareTo(
                BigDecimal.valueOf(90)
            ) > 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La latitud no es válida"
            );
        }

        if (
            longitud.compareTo(
                BigDecimal.valueOf(-180)
            ) < 0
            || longitud.compareTo(
                BigDecimal.valueOf(180)
            ) > 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La longitud no es válida"
            );
        }
    }

    private void validarConformidad(
            TipoEvidencia tipo,
            String nombreResponsable,
            String documentoResponsable) {

        if (tipo == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tipo de evidencia es obligatorio"
            );
        }

        if (
            tipo == TipoEvidencia.CONFORMIDAD
            && (
                nombreResponsable == null
                || nombreResponsable.isBlank()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La conformidad requiere el nombre "
                    + "del responsable"
            );
        }

        if (
            tipo == TipoEvidencia.CONFORMIDAD
            && (
                documentoResponsable == null
                || documentoResponsable.isBlank()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La conformidad requiere el documento "
                    + "del responsable"
            );
        }
    }

    private void validarLongitud(
            String valor,
            int longitudMaxima,
            String nombreCampo) {

        if (
            valor != null
            && valor.trim().length()
                > longitudMaxima
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                nombreCampo
                    + " supera la longitud permitida"
            );
        }
    }

    private Path resolverRutaSegura(
            String nombreArchivo) {

        Path destino =
            directorioEvidencias
                .resolve(nombreArchivo)
                .normalize();

        if (
            !destino.startsWith(
                directorioEvidencias
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La ubicación del archivo "
                    + "no es válida"
            );
        }

        return destino;
    }

    private String limpiarNombreOriginal(
            String nombre) {

        if (
            nombre == null
            || nombre.isBlank()
        ) {
            return "evidencia";
        }

        String limpio =
            nombre.replace('\\', '/');

        int ultimaBarra =
            limpio.lastIndexOf('/');

        if (ultimaBarra >= 0) {
            limpio = limpio.substring(
                ultimaBarra + 1
            );
        }

        limpio = limpio
            .replaceAll(
                "[\\p{Cntrl}]",
                "_"
            )
            .trim();

        if (limpio.isBlank()) {
            limpio = "evidencia";
        }

        return limpio.length() > 255
            ? limpio.substring(0, 255)
            : limpio;
    }

    private String limpiarOpcional(
            String valor) {

        if (
            valor == null
            || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }

    private String limpiarObligatorio(
            String valor,
            String mensaje) {

        String limpio =
            limpiarOpcional(valor);

        if (limpio == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
            );
        }

        return limpio;
    }

    private void crearDirectorio() {
        try {
            Files.createDirectories(
                directorioEvidencias
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "No fue posible crear el "
                    + "directorio de evidencias",
                exception
            );
        }
    }

    private void eliminarArchivoSilenciosamente(
            Path archivo) {

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            /*
             * Se conserva la excepción original.
             */
        }
    }

    private EvidenciaServicioResponse
        convertirRespuesta(
            EvidenciaServicio evidencia) {

        return new EvidenciaServicioResponse(
            evidencia.getId(),
            evidencia.getServicio().getId(),
            evidencia
                .getServicio()
                .getCodigo(),
            evidencia.getTipo(),
            evidencia.getNombreOriginal(),
            evidencia.getTipoContenido(),
            evidencia.getTamanoBytes(),
            evidencia.getDescripcion(),
            evidencia.getLatitud(),
            evidencia.getLongitud(),
            evidencia.getNombreResponsable(),
            evidencia.getDocumentoResponsable(),
            evidencia.getRegistradoPor(),
            evidencia.getFechaRegistro(),
            "/api/evidencias/"
                + evidencia.getId()
                + "/archivo"
        );
    }

    private ResponseStatusException
        accesoDenegado() {

        return new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No tienes permiso para acceder "
                + "a esta evidencia"
        );
    }

    private record TipoArchivoDetectado(
        String tipoContenido,
        String extension
    ) {
    }
}
