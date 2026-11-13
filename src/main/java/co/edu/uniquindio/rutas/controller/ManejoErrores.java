package co.edu.uniquindio.rutas.controller;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.io.UncheckedIOException;

/**
 * Convierte las excepciones de la capa lógica en respuestas JSON {"error": "..."} para que la
 * página muestre qué falta (p. ej. qué comando genera un resultado) en lugar de fallar.
 */
@RestControllerAdvice
public class ManejoErrores {

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, String> pendiente(UnsupportedOperationException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler({NoSuchElementException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> faltanDatos(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> solicitudInvalida(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> entradaInvalida(Exception e) {
        return Map.of("error", "Solicitud inválida: revise los parámetros y el JSON enviado");
    }

    @ExceptionHandler(UncheckedIOException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> lecturaFallida(UncheckedIOException e) {
        return Map.of("error", "No se pudieron leer los datos locales. Revise los archivos y los comandos de preparación del README");
    }
}
