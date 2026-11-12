package co.edu.uniquindio.rutas.repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Lectura y escritura de CSV (RFC 4180: comas, comillas dobles y "" como escape), con
 * soporte .gz por la extensión del archivo. Usado solo por la capa de datos.
 */
public final class Csv {

    private Csv() {
    }

    /** Lee un CSV con encabezado; cada fila es un mapa columna -> texto (vacío = ""). */
    public static List<Map<String, String>> leer(Path ruta) {
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(abrirEntrada(ruta), StandardCharsets.UTF_8))) {
            List<List<String>> registros = parsear(lector);
            if (registros.isEmpty()) {
                return List.of();
            }
            List<String> encabezado = registros.getFirst();
            if (!encabezado.isEmpty() && encabezado.getFirst().startsWith("﻿")) {
                encabezado.set(0, encabezado.getFirst().substring(1)); // BOM de Excel
            }
            List<Map<String, String>> filas = new ArrayList<>();
            for (List<String> registro : registros.subList(1, registros.size())) {
                if (registro.size() == 1 && registro.getFirst().isEmpty()) {
                    continue; // línea vacía
                }
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < encabezado.size(); i++) {
                    fila.put(encabezado.get(i), i < registro.size() ? registro.get(i) : "");
                }
                filas.add(fila);
            }
            return filas;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }

    /** Escribe encabezado + filas. Los valores null se escriben vacíos. */
    public static void escribir(Path ruta, List<String> encabezado, List<? extends List<?>> filas) {
        try {
            if (ruta.getParent() != null) {
                Files.createDirectories(ruta.getParent());
            }
            try (BufferedWriter escritor = new BufferedWriter(
                    new OutputStreamWriter(abrirSalida(ruta), StandardCharsets.UTF_8))) {
                escribirLinea(escritor, encabezado);
                for (List<?> fila : filas) {
                    escribirLinea(escritor, fila);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + ruta, e);
        }
    }

    private static void escribirLinea(BufferedWriter escritor, List<?> valores) throws IOException {
        List<String> celdas = new ArrayList<>(valores.size());
        for (Object valor : valores) {
            celdas.add(escapar(valor == null ? "" : String.valueOf(valor)));
        }
        escritor.write(String.join(",", celdas));
        escritor.newLine();
    }

    static String escapar(String valor) {
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")
                || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    static List<List<String>> parsear(BufferedReader lector) throws IOException {
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean enComillas = false;
        int c;
        boolean hayDatos = false;
        while ((c = lector.read()) != -1) {
            hayDatos = true;
            char ch = (char) c;
            if (enComillas) {
                if (ch == '"') {
                    lector.mark(1);
                    int siguiente = lector.read();
                    if (siguiente == '"') {
                        celda.append('"');
                    } else {
                        enComillas = false;
                        if (siguiente != -1) {
                            lector.reset();
                        }
                    }
                } else {
                    celda.append(ch);
                }
            } else if (ch == '"') {
                enComillas = true;
            } else if (ch == ',') {
                actual.add(celda.toString());
                celda.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r') {
                    lector.mark(1);
                    if (lector.read() != '\n') {
                        lector.reset();
                    }
                }
                actual.add(celda.toString());
                celda.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
                hayDatos = false;
            } else {
                celda.append(ch);
            }
        }
        if (hayDatos) {
            actual.add(celda.toString());
            registros.add(actual);
        }
        return registros;
    }

    private static InputStream abrirEntrada(Path ruta) throws IOException {
        InputStream base = Files.newInputStream(ruta);
        return ruta.toString().endsWith(".gz") ? new GZIPInputStream(base) : base;
    }

    private static OutputStream abrirSalida(Path ruta) throws IOException {
        OutputStream base = Files.newOutputStream(ruta);
        return ruta.toString().endsWith(".gz") ? new GZIPOutputStream(base) : base;
    }
}
