package com.palmas.transporte.plataforma.web;


import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> manejarMetodoNoPermitido(
        HttpRequestMethodNotSupportedException exception
    ) {
        ResponseEntity<Map<String, Object>> resultado = respuesta(
            HttpStatus.METHOD_NOT_ALLOWED,
            "El metodo HTTP no esta permitido para esta ruta.",
            null
        );
        org.springframework.http.HttpHeaders cabeceras =
            new org.springframework.http.HttpHeaders();
        java.util.Set<org.springframework.http.HttpMethod> permitidos =
            exception.getSupportedHttpMethods();
        if (permitidos != null && !permitidos.isEmpty()) {
            cabeceras.setAllow(permitidos);
        }
        return new ResponseEntity<>(resultado.getBody(), cabeceras,
            HttpStatus.METHOD_NOT_ALLOWED);
    }

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            ApiExceptionHandler.class
        );

    @ExceptionHandler(
        ObjectOptimisticLockingFailureException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarConflictoConcurrente(
            ObjectOptimisticLockingFailureException exception
        ) {

        return respuesta(
            HttpStatus.CONFLICT,
            "El registro fue modificado por otro usuario. "
                + "Actualiza la información e inténtalo nuevamente.",
            null
        );
    }

    @ExceptionHandler(
        DataIntegrityViolationException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarIntegridad(
            DataIntegrityViolationException exception
        ) {

        String codigo = generarCodigoError();

        LOGGER.warn(
            "Conflicto de integridad. codigo={}",
            codigo,
            exception
        );

        return respuesta(
            HttpStatus.CONFLICT,
            "La operación entra en conflicto con "
                + "información registrada actualmente.",
            codigo
        );
    }

    @ExceptionHandler(
        MaxUploadSizeExceededException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarArchivoDemasiadoGrande(
            MaxUploadSizeExceededException exception
        ) {

        return respuesta(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "El archivo supera el tamaño máximo permitido.",
            null
        );
    }

    @ExceptionHandler(
        MethodArgumentNotValidException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarValidacion(
            MethodArgumentNotValidException exception
        ) {

        String mensaje = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse(
                "Revisa los datos ingresados."
            );

        return respuesta(
            HttpStatus.BAD_REQUEST,
            mensaje,
            null
        );
    }

    @ExceptionHandler(
        ConstraintViolationException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarRestricciones(
            ConstraintViolationException exception
        ) {

        String mensaje = exception
            .getConstraintViolations()
            .stream()
            .findFirst()
            .map(violacion ->
                violacion.getMessage()
            )
            .orElse(
                "Revisa los datos ingresados."
            );

        return respuesta(
            HttpStatus.BAD_REQUEST,
            mensaje,
            null
        );
    }

    @ExceptionHandler(
        HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarJsonInvalido(
            HttpMessageNotReadableException exception
        ) {

        return respuesta(
            HttpStatus.BAD_REQUEST,
            "El contenido de la solicitud no es válido.",
            null
        );
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarTipoInvalido(
            MethodArgumentTypeMismatchException exception
        ) {

        return respuesta(
            HttpStatus.BAD_REQUEST,
            "Uno de los parámetros tiene un formato inválido.",
            null
        );
    }

    @ExceptionHandler(
        IllegalArgumentException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarArgumentoInvalido(
            IllegalArgumentException exception
        ) {

        String mensaje =
            exception.getMessage() != null
                && !exception.getMessage().isBlank()
                    ? exception.getMessage()
                    : "La solicitud contiene datos inválidos.";

        return respuesta(
            HttpStatus.BAD_REQUEST,
            mensaje,
            null
        );
    }

    @ExceptionHandler(
        ResponseStatusException.class
    )
    public ResponseEntity<Map<String, Object>>
        manejarEstadoHttp(
            ResponseStatusException exception
        ) {

        String mensaje =
            exception.getReason() != null
                && !exception.getReason().isBlank()
                    ? exception.getReason()
                    : mensajePredeterminado(
                        HttpStatus.valueOf(
                            exception
                                .getStatusCode()
                                .value()
                        )
                    );

        return respuesta(
            HttpStatus.valueOf(
                exception
                    .getStatusCode()
                    .value()
            ),
            mensaje,
            null
        );
    }

    /*
     * Última barrera: evita devolver al cliente nombres
     * de clases, consultas SQL o trazas internas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
        manejarErrorNoControlado(
            Exception exception
        ) {

        String codigo = generarCodigoError();

        LOGGER.error(
            "Error no controlado. codigo={}",
            codigo,
            exception
        );

        return respuesta(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "No fue posible completar la operación. "
                + "Comunícate con soporte e indica "
                + "el código del error.",
            codigo
        );
    }

    private ResponseEntity<Map<String, Object>>
        respuesta(
            HttpStatus estado,
            String mensaje,
            String codigoError
        ) {

        Map<String, Object> cuerpo =
            new LinkedHashMap<>();

        cuerpo.put(
            "fecha",
            LocalDateTime.now()
        );

        cuerpo.put(
            "estado",
            estado.value()
        );

        cuerpo.put(
            "error",
            estado.getReasonPhrase()
        );

        cuerpo.put(
            "mensaje",
            mensaje
        );

        if (codigoError != null) {
            cuerpo.put(
                "codigoError",
                codigoError
            );
        }

        return ResponseEntity
            .status(estado)
            .body(cuerpo);
    }

    private String generarCodigoError() {
        return UUID
            .randomUUID()
            .toString()
            .substring(0, 8)
            .toUpperCase();
    }

    private String mensajePredeterminado(
            HttpStatus estado) {

        if (estado == HttpStatus.UNAUTHORIZED) {
            return "No fue posible autenticar la solicitud.";
        }

        if (estado == HttpStatus.FORBIDDEN) {
            return "No tienes permiso para realizar esta operación.";
        }

        if (estado == HttpStatus.NOT_FOUND) {
            return "El recurso solicitado no existe.";
        }

        if (estado == HttpStatus.CONFLICT) {
            return "La operación presenta un conflicto.";
        }

        return "No fue posible procesar la solicitud.";
    }
}
