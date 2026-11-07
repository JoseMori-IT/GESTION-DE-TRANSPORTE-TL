package com.palmas.transporte.modulos.mantenimiento.application;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.mantenimiento.api.EvidenciaMantenimientoResponse;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.EvidenciaMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;
import com.palmas.transporte.modulos.mantenimiento.domain.TipoEvidenciaMantenimiento;
import com.palmas.transporte.modulos.mantenimiento.persistence.EvidenciaMantenimientoRepository;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;

@Service
public class EvidenciaMantenimientoService {

    private static final long TAMANIO_MAXIMO =
        10L * 1024L * 1024L;

    private static final int LONGITUD_DESCRIPCION =
        1000;

    private static final int LONGITUD_REGISTRADO_POR =
        150;

    private static final int LONGITUD_NOMBRE_ORIGINAL =
        255;

    private final EvidenciaMantenimientoRepository
        evidenciaRepository;

    private final MantenimientoRepository
        mantenimientoRepository;

    private final Path directorio;

    public EvidenciaMantenimientoService(
        EvidenciaMantenimientoRepository evidenciaRepository,
        MantenimientoRepository mantenimientoRepository,
        @Value("${app.evidencias.directorio}")
        String directorioBase
    ) {
        this.evidenciaRepository =
            evidenciaRepository;

        this.mantenimientoRepository =
            mantenimientoRepository;

        if (
            directorioBase == null
            || directorioBase.isBlank()
        ) {
            throw new IllegalStateException(
                "No se configuró el directorio "
                    + "de evidencias"
            );
        }

        this.directorio = prepararDirectorio(
            Path.of(directorioBase)
                .resolve("mantenimientos")
                .toAbsolutePath()
                .normalize()
        );
    }

    @Transactional(readOnly = true)
    public List<EvidenciaMantenimientoResponse>
        listarPorMantenimiento(
            Long mantenimientoId,
            String rol
        ) {

        validarRolConsulta(rol);
        verificarMantenimiento(mantenimientoId);

        return evidenciaRepository
            .findByMantenimiento_IdOrderByFechaRegistroDesc(
                mantenimientoId
            )
            .stream()
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional
    public EvidenciaMantenimientoResponse registrar(
        Long mantenimientoId,
        TipoEvidenciaMantenimiento tipo,
        MultipartFile archivo,
        String descripcion,
        String registradoPor,
        String rol
    ) {
        validarRolEscritura(rol);

        if (tipo == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El tipo de evidencia es obligatorio"
            );
        }

        Mantenimiento mantenimiento =
            verificarMantenimiento(
                mantenimientoId
            );

        validarMantenimiento(mantenimiento);
        validarTipoPorEstado(
            mantenimiento,
            tipo
        );

        ArchivoValidado archivoValidado =
            validarArchivo(archivo);

        String nombreOriginal =
            limpiarNombreOriginal(
                archivo.getOriginalFilename(),
                archivoValidado.extension()
            );

        String nombreArchivo =
            UUID.randomUUID()
                + archivoValidado.extension();

        Path destino =
            resolverDestinoNuevo(nombreArchivo);

        boolean archivoCreado = false;

        try (
            InputStream entrada =
                archivo.getInputStream()
        ) {
            Files.copy(
                entrada,
                destino
            );

            archivoCreado = true;

            long tamanioReal =
                Files.size(destino);

            if (
                tamanioReal <= 0
                || tamanioReal > TAMANIO_MAXIMO
                || tamanioReal != archivo.getSize()
            ) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tamaño del archivo no es válido"
                );
            }

            EvidenciaMantenimiento evidencia =
                new EvidenciaMantenimiento();

            evidencia.setMantenimiento(
                mantenimiento
            );

            evidencia.setTipo(tipo);

            evidencia.setNombreOriginal(
                nombreOriginal
            );

            evidencia.setNombreArchivo(
                nombreArchivo
            );

            evidencia.setTipoContenido(
                archivoValidado.tipoContenido()
            );

            evidencia.setTamanioBytes(
                tamanioReal
            );

            evidencia.setDescripcion(
                textoOpcional(
                    descripcion,
                    LONGITUD_DESCRIPCION,
                    "La descripción"
                )
            );

            evidencia.setRegistradoPor(
                textoObligatorio(
                    registradoPor,
                    LONGITUD_REGISTRADO_POR,
                    "El usuario que registra"
                )
            );

            EvidenciaMantenimiento guardada =
                evidenciaRepository.saveAndFlush(
                    evidencia
                );

            return convertirRespuesta(guardada);

        } catch (ResponseStatusException exception) {
            if (archivoCreado) {
                eliminarArchivo(destino);
            }

            throw exception;

        } catch (IOException exception) {
            if (archivoCreado) {
                eliminarArchivo(destino);
            }

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible almacenar "
                    + "la evidencia"
            );

        } catch (RuntimeException exception) {
            if (archivoCreado) {
                eliminarArchivo(destino);
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public EvidenciaMantenimiento buscarPorId(
        Long id,
        String rol
    ) {
        validarRolConsulta(rol);

        if (id == null || id <= 0) {
            throw evidenciaNoEncontrada();
        }

        return evidenciaRepository
            .findById(id)
            .orElseThrow(
                this::evidenciaNoEncontrada
            );
    }

    @Transactional(readOnly = true)
    public Resource cargarArchivo(
        EvidenciaMantenimiento evidencia
    ) {
        if (
            evidencia == null
            || evidencia.getNombreArchivo() == null
            || evidencia.getNombreArchivo().isBlank()
        ) {
            throw evidenciaNoEncontrada();
        }

        String nombreArchivo =
            evidencia.getNombreArchivo();

        if (
            !nombreArchivo.matches(
                "^[0-9a-fA-F-]{36}"
                    + "\\.(jpg|jpeg|png|webp|pdf)$"
            )
        ) {
            throw evidenciaNoEncontrada();
        }

        Path rutaConfigurada = directorio
            .resolve(nombreArchivo)
            .normalize();

        if (!rutaConfigurada.startsWith(directorio)) {
            throw evidenciaNoEncontrada();
        }

        try {
            if (
                !Files.exists(
                    rutaConfigurada,
                    LinkOption.NOFOLLOW_LINKS
                )
                || !Files.isRegularFile(
                    rutaConfigurada,
                    LinkOption.NOFOLLOW_LINKS
                )
                || Files.isSymbolicLink(rutaConfigurada)
            ) {
                throw evidenciaNoEncontrada();
            }

            Path rutaReal =
                rutaConfigurada.toRealPath(
                    LinkOption.NOFOLLOW_LINKS
                );

            if (!rutaReal.startsWith(directorio)) {
                throw evidenciaNoEncontrada();
            }

            Resource recurso =
                new UrlResource(
                    rutaReal.toUri()
                );

            if (
                !recurso.exists()
                || !recurso.isReadable()
            ) {
                throw evidenciaNoEncontrada();
            }

            return recurso;

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (IOException exception) {
            throw evidenciaNoEncontrada();
        }
    }

    private Mantenimiento verificarMantenimiento(
        Long mantenimientoId
    ) {
        if (
            mantenimientoId == null
            || mantenimientoId <= 0
        ) {
            throw mantenimientoNoEncontrado();
        }

        return mantenimientoRepository
            .findById(mantenimientoId)
            .orElseThrow(
                this::mantenimientoNoEncontrado
            );
    }

    private void validarMantenimiento(
        Mantenimiento mantenimiento
    ) {
        if (
            mantenimiento.getEstado()
                == EstadoMantenimiento.CANCELADO
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pueden registrar evidencias "
                    + "en un mantenimiento cancelado"
            );
        }
    }

    private void validarTipoPorEstado(
        Mantenimiento mantenimiento,
        TipoEvidenciaMantenimiento tipo
    ) {
        EstadoMantenimiento estado =
            mantenimiento.getEstado();

        if (
            tipo
                == TipoEvidenciaMantenimiento.REPARACION
            && estado
                != EstadoMantenimiento.EN_MANTENIMIENTO
            && estado
                != EstadoMantenimiento.REPARADO
            && estado
                != EstadoMantenimiento.CERRADO
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La evidencia de reparación solo puede "
                    + "registrarse durante o después "
                    + "de la reparación"
            );
        }

        if (
            tipo
                == TipoEvidenciaMantenimiento.CONFORMIDAD
            && estado
                != EstadoMantenimiento.REPARADO
            && estado
                != EstadoMantenimiento.CERRADO
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La conformidad solo puede registrarse "
                    + "cuando la unidad esté reparada"
            );
        }
    }

    private ArchivoValidado validarArchivo(
        MultipartFile archivo
    ) {
        if (
            archivo == null
            || archivo.isEmpty()
            || archivo.getSize() <= 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes seleccionar un archivo"
            );
        }

        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo no puede superar los 10 MB"
            );
        }

        byte[] encabezado =
            leerEncabezado(archivo, 16);

        if (esJpeg(encabezado)) {
            return new ArchivoValidado(
                "image/jpeg",
                ".jpg"
            );
        }

        if (esPng(encabezado)) {
            return new ArchivoValidado(
                "image/png",
                ".png"
            );
        }

        if (esWebp(encabezado)) {
            return new ArchivoValidado(
                "image/webp",
                ".webp"
            );
        }

        if (esPdf(encabezado)) {
            return new ArchivoValidado(
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

    private byte[] leerEncabezado(
        MultipartFile archivo,
        int longitudMaxima
    ) {
        try (
            InputStream entrada =
                archivo.getInputStream()
        ) {
            return entrada.readNBytes(
                longitudMaxima
            );

        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No fue posible validar el archivo"
            );
        }
    }

    private boolean esJpeg(byte[] bytes) {
        return bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xFF
            && (bytes[1] & 0xFF) == 0xD8
            && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean esPng(byte[] bytes) {
        byte[] firma = {
            (byte) 0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
        };

        return bytes.length >= firma.length
            && Arrays.equals(
                Arrays.copyOf(
                    bytes,
                    firma.length
                ),
                firma
            );
    }

    private boolean esWebp(byte[] bytes) {
        return bytes.length >= 12
            && bytes[0] == 'R'
            && bytes[1] == 'I'
            && bytes[2] == 'F'
            && bytes[3] == 'F'
            && bytes[8] == 'W'
            && bytes[9] == 'E'
            && bytes[10] == 'B'
            && bytes[11] == 'P';
    }

    private boolean esPdf(byte[] bytes) {
        return bytes.length >= 5
            && bytes[0] == '%'
            && bytes[1] == 'P'
            && bytes[2] == 'D'
            && bytes[3] == 'F'
            && bytes[4] == '-';
    }

    private String limpiarNombreOriginal(
        String nombre,
        String extensionReal
    ) {
        String limpio;

        if (nombre == null || nombre.isBlank()) {
            limpio = "evidencia"
                + extensionReal;
        } else {
            limpio = nombre
                .replace('\\', '/');

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
                    ""
                )
                .trim();

            if (limpio.isBlank()) {
                limpio = "evidencia"
                    + extensionReal;
            }
        }

        int longitudMaximaBase =
            LONGITUD_NOMBRE_ORIGINAL
                - extensionReal.length();

        String nombreSinExtension =
            quitarExtension(limpio);

        if (
            nombreSinExtension.length()
                > longitudMaximaBase
        ) {
            nombreSinExtension =
                nombreSinExtension.substring(
                    0,
                    longitudMaximaBase
                );
        }

        if (nombreSinExtension.isBlank()) {
            nombreSinExtension = "evidencia";
        }

        return nombreSinExtension
            + extensionReal;
    }

    private String quitarExtension(
        String nombre
    ) {
        int posicion =
            nombre.lastIndexOf('.');

        if (posicion <= 0) {
            return nombre;
        }

        return nombre.substring(
            0,
            posicion
        );
    }

    private String textoOpcional(
        String valor,
        int longitudMaxima,
        String nombreCampo
    ) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        if (limpio.length() > longitudMaxima) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                nombreCampo
                    + " no puede superar los "
                    + longitudMaxima
                    + " caracteres"
            );
        }

        return limpio;
    }

    private String textoObligatorio(
        String valor,
        int longitudMaxima,
        String nombreCampo
    ) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                nombreCampo
                    + " es obligatorio"
            );
        }

        String limpio = valor.trim();

        if (limpio.length() > longitudMaxima) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                nombreCampo
                    + " no puede superar los "
                    + longitudMaxima
                    + " caracteres"
            );
        }

        return limpio;
    }

    private Path prepararDirectorio(
        Path ruta
    ) {
        try {
            Files.createDirectories(ruta);

            if (
                Files.isSymbolicLink(ruta)
                || !Files.isDirectory(
                    ruta,
                    LinkOption.NOFOLLOW_LINKS
                )
            ) {
                throw new IllegalStateException(
                    "El directorio de evidencias "
                        + "no es válido"
                );
            }

            return ruta.toRealPath(
                LinkOption.NOFOLLOW_LINKS
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                "No fue posible preparar el directorio "
                    + "de evidencias de mantenimiento",
                exception
            );
        }
    }

    private Path resolverDestinoNuevo(
        String nombreArchivo
    ) {
        Path destino = directorio
            .resolve(nombreArchivo)
            .normalize();

        if (
            !destino.startsWith(directorio)
            || Files.exists(
                destino,
                LinkOption.NOFOLLOW_LINKS
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No fue posible generar una ubicación "
                    + "segura para la evidencia"
            );
        }

        return destino;
    }

    private void eliminarArchivo(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // El error original tiene prioridad.
        }
    }

    private void validarRolConsulta(
        String rol
    ) {
        String rolNormalizado =
            normalizarRol(rol);

        if (
            !esGestionTotal(
                rolNormalizado
            )
            && !"OPERADOR".equals(
                rolNormalizado
            )
            && !"CONSULTA".equals(
                rolNormalizado
            )
        ) {
            throw accesoDenegado();
        }
    }

    private void validarRolEscritura(
        String rol
    ) {
        String rolNormalizado =
            normalizarRol(rol);

        if (
            !esGestionTotal(
                rolNormalizado
            )
            && !"OPERADOR".equals(
                rolNormalizado
            )
        ) {
            throw accesoDenegado();
        }
    }

    private String normalizarRol(
        String rol
    ) {
        if (rol == null) {
            return "";
        }

        return rol
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    private boolean esGestionTotal(String rol) {
        return "MASTER".equals(rol)
            || "COORDINADOR".equals(rol)
            || "ADMINISTRADOR".equals(rol);
    }

    private ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No tienes permisos para realizar "
                + "esta operación"
        );
    }

    private ResponseStatusException
        mantenimientoNoEncontrado() {

        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Mantenimiento no encontrado"
        );
    }

    private ResponseStatusException
        evidenciaNoEncontrada() {

        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Evidencia de mantenimiento "
                + "no encontrada"
        );
    }

    private EvidenciaMantenimientoResponse
        convertirRespuesta(
            EvidenciaMantenimiento evidencia
        ) {

        Mantenimiento mantenimiento =
            evidencia.getMantenimiento();

        return new EvidenciaMantenimientoResponse(
            evidencia.getId(),

            mantenimiento.getId(),
            mantenimiento.getCodigo(),

            mantenimiento.getVehiculo().getId(),
            mantenimiento.getVehiculo().getPlaca(),

            evidencia.getTipo(),

            evidencia.getNombreOriginal(),
            evidencia.getTipoContenido(),
            evidencia.getTamanioBytes(),
            evidencia.getDescripcion(),

            evidencia.getRegistradoPor(),
            evidencia.getFechaRegistro(),

            "/api/mantenimientos/evidencias/"
                + evidencia.getId()
                + "/archivo"
        );
    }

    private record ArchivoValidado(
        String tipoContenido,
        String extension
    ) {
    }
}
