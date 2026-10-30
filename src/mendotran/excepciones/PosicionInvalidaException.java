package mendotran.excepciones;

/**
 * Se lanza cuando se accede a una posición inexistente de una colección
 * (por ejemplo, una parada fuera del rango de un {@code Recorrido}).
 */
public class PosicionInvalidaException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int posicion;
    private final int tamanio;

    public PosicionInvalidaException(int posicion, int tamanio) {
        super(construirMensaje(posicion, tamanio));
        this.posicion = posicion;
        this.tamanio = tamanio;
    }

    private static String construirMensaje(int posicion, int tamanio) {
        if (tamanio == 0) {
            return "Posición " + posicion + " inválida: la colección está vacía.";
        }
        return "Posición " + posicion + " fuera de rango (válido: 0 a " + (tamanio - 1) + ").";
    }

    public int getPosicion() {
        return posicion;
    }

    public int getTamanio() {
        return tamanio;
    }
}
