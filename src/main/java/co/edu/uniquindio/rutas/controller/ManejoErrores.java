package co.edu.uniquindio.rutas.controller;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
