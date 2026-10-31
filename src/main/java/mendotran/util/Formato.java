package mendotran.util;

import java.util.Locale;

/** Utilidades de formato para mostrar datos en consola. */
public final class Formato {

    private static final Locale AR = Locale.forLanguageTag("es-AR");

    private Formato() {
    }

    public static String pesos(double valor) {
        return String.format(AR, "$%,.2f", valor);
    }

    public static String siNo(boolean valor) {
        return valor ? "sí" : "no";
    }
}
