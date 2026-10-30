package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public abstract class Empleado extends Persona {

    private final String legajo;

    protected Empleado(int id, String dni, String nombre, String apellido, String legajo)
            throws EntradaInvalidaException {
        super(id, dni, nombre, apellido);
        this.legajo = Validador.texto(legajo, "legajo");
    }

    public String getLegajo() {
        return legajo;
    }

    /** Cada tipo de empleado describe su rol (polimorfismo). */
    public abstract String mostrarRol();

    @Override
    public String toString() {
        return super.toString() + " | legajo " + legajo;
    }
}
