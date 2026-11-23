package cl.duoc.gymflow.catalog.eventos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Arma el texto "campo: antes → después" de una edición, solo con los campos que cambiaron. */
public final class Cambios {

    private final List<String> lista = new ArrayList<>();

    public Cambios campo(String nombre, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            lista.add(nombre + " " + texto(antes) + " → " + texto(despues));
        }
        return this;
    }

    /** Ej.: "cupo 20 → 25; plan BASICO → PLUS", o "sin cambios". */
    public String texto() {
        return lista.isEmpty() ? "sin cambios" : String.join("; ", lista);
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return "(vacío)";
        }
        return valor instanceof String s ? "'" + s + "'" : valor.toString();
    }
}
