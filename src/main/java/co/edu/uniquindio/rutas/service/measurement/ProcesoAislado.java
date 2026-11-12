package co.edu.uniquindio.rutas.service.measurement;

import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Programa de la JVM hija que lanza {@link Medidor#medirConLimite}. No se ejecuta a mano.
 *
 * <p>Argumentos: clase de la {@link Tarea}, repeticiones, calentamientos, umbral (s). Lee la
 * entrada serializada por la entrada estándar y escribe {@link Mensaje}s serializados por la
 * salida estándar (por eso NADA más puede imprimir en System.out en este proceso).
 */
public final class ProcesoAislado {

    private ProcesoAislado() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        Tarea tarea = (Tarea) Class.forName(args[0]).getDeclaredConstructor().newInstance();
        int repeticiones = Integer.parseInt(args[1]);
        int calentamientos = Integer.parseInt(args[2]);
        double umbralMs = Double.parseDouble(args[3]) * 1000.0;

        Serializable entrada;
        try (ObjectInputStream in = new ObjectInputStream(System.in)) {
            entrada = (Serializable) in.readObject();
        }
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(System.out));
        try {
            Serializable resultado = null;
            boolean unaSola = false;
            for (int i = 0; i < calentamientos && !unaSola; i++) {
                Medidor.Ejecucion<Serializable> e =
                        Medidor.ejecutar(() -> tarea.ejecutar(entrada));
                resultado = e.resultado();
                if (e.ms() > umbralMs) {
                    enviar(out, new Mensaje(Mensaje.Tipo.REPETICION, e.ms(), null, null));
                    unaSola = true;
                } else {
                    enviar(out, new Mensaje(Mensaje.Tipo.CALENTAMIENTO, e.ms(), null, null));
                }
            }
            for (int i = 0; i < repeticiones && !unaSola; i++) {
                Medidor.Ejecucion<Serializable> e =
                        Medidor.ejecutar(() -> tarea.ejecutar(entrada));
                resultado = e.resultado();
                enviar(out, new Mensaje(Mensaje.Tipo.REPETICION, e.ms(), null, null));
                if (i == 0 && calentamientos == 0 && e.ms() > umbralMs) {
                    unaSola = true;
                }
            }
            enviar(out, new Mensaje(Mensaje.Tipo.FIN, 0, resultado, null));
        } catch (RuntimeException | Error e) {
            enviar(out, new Mensaje(Mensaje.Tipo.ERROR, 0, null, e.toString()));
        }
        out.close();
        System.exit(0);
    }

    private static void enviar(ObjectOutputStream out, Mensaje m) throws java.io.IOException {
        out.writeObject(m);
        out.flush();
        out.reset();
    }
}
