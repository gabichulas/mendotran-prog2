package mendotran.excepciones;

/**
 * Se lanza cuando un dato ingresado (por consola o por código) no cumple las
 * reglas de validación del dominio: DNI mal formado, patente inválida,
 * monto negativo, campo vacío, saldo insuficiente, etc.
 */
public class EntradaInvalidaException extends Exception {

    private static final long serialVersionUID = 1L;

    public EntradaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public EntradaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
