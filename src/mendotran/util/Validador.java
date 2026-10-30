package mendotran.util;

import mendotran.excepciones.EntradaInvalidaException;

import java.util.regex.Pattern;

/**
 * Validaciones centralizadas. Todas lanzan {@link EntradaInvalidaException}
 * con un mensaje claro para mostrar al usuario.
 */
public final class Validador {

    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern SUBE = Pattern.compile("\\d{16}");
    private static final Pattern CODIGO_PARADA = Pattern.compile("M\\d{4}");
    /** Formato viejo (ABC123) o Mercosur (AB123CD). */
    private static final Pattern PATENTE = Pattern.compile("[A-Z]{3}\\d{3}|[A-Z]{2}\\d{3}[A-Z]{2}");

    private Validador() {
    }

    public static String texto(String valor, String campo) throws EntradaInvalidaException {
        if (valor == null || valor.isBlank()) {
            throw new EntradaInvalidaException("El campo '" + campo + "' no puede estar vacío.");
        }
        return valor.trim();
    }

    public static String dni(String valor) throws EntradaInvalidaException {
        String limpio = texto(valor, "DNI").replace(".", "");
        if (!DNI.matcher(limpio).matches()) {
            throw new EntradaInvalidaException("DNI inválido '" + valor + "': debe tener 7 u 8 dígitos.");
        }
        return limpio;
    }

    public static String numeroSube(String valor) throws EntradaInvalidaException {
        String limpio = texto(valor, "número SUBE").replace(" ", "");
        if (!SUBE.matcher(limpio).matches()) {
            throw new EntradaInvalidaException("Número SUBE inválido '" + valor + "': debe tener 16 dígitos.");
        }
        return limpio;
    }

    public static String codigoParada(String valor) throws EntradaInvalidaException {
        String limpio = texto(valor, "código de parada").toUpperCase();
        if (!CODIGO_PARADA.matcher(limpio).matches()) {
            throw new EntradaInvalidaException(
                    "Código de parada inválido '" + valor + "': debe tener el formato MXXXX (ej. M0123).");
        }
        return limpio;
    }

    public static String patente(String valor) throws EntradaInvalidaException {
        String limpio = texto(valor, "patente").toUpperCase().replace(" ", "");
        if (!PATENTE.matcher(limpio).matches()) {
            throw new EntradaInvalidaException(
                    "Patente inválida '" + valor + "': use ABC123 o AB123CD.");
        }
        return limpio;
    }

    public static int enteroPositivo(int valor, String campo) throws EntradaInvalidaException {
        if (valor <= 0) {
            throw new EntradaInvalidaException("El campo '" + campo + "' debe ser mayor que 0 (recibido: " + valor + ").");
        }
        return valor;
    }

    public static double montoPositivo(double valor, String campo) throws EntradaInvalidaException {
        if (Double.isNaN(valor) || valor <= 0) {
            throw new EntradaInvalidaException("El campo '" + campo + "' debe ser un monto mayor que 0 (recibido: " + valor + ").");
        }
        return valor;
    }

    public static double montoNoNegativo(double valor, String campo) throws EntradaInvalidaException {
        if (Double.isNaN(valor) || valor < 0) {
            throw new EntradaInvalidaException("El campo '" + campo + "' no puede ser negativo (recibido: " + valor + ").");
        }
        return valor;
    }

    // ---- Conversión de texto ingresado por consola ----

    public static int parsearEntero(String texto, String campo) throws EntradaInvalidaException {
        if (texto == null || texto.isBlank()) {
            throw new EntradaInvalidaException("Falta ingresar un valor para '" + campo + "'.");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("'" + texto + "' no es un número entero válido para '" + campo + "'.", e);
        }
    }

    public static double parsearDecimal(String texto, String campo) throws EntradaInvalidaException {
        if (texto == null || texto.isBlank()) {
            throw new EntradaInvalidaException("Falta ingresar un valor para '" + campo + "'.");
        }
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("'" + texto + "' no es un número válido para '" + campo + "'.", e);
        }
    }

    public static boolean parsearSiNo(String texto, String campo) throws EntradaInvalidaException {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return switch (t) {
            case "s", "si", "sí", "y", "yes" -> true;
            case "n", "no" -> false;
            default -> throw new EntradaInvalidaException("Responda s/n para '" + campo + "' (recibido: '" + texto + "').");
        };
    }
}
