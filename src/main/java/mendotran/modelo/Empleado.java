package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public abstract class Empleado extends Persona {

    private int legajo;

    protected Empleado(String dni, String nombre, String apellido, int legajo)
            throws EntradaInvalidaException {
        this(0, dni, nombre, apellido, legajo);
    }

    protected Empleado(int id, String dni, String nombre, String apellido, int legajo)
            throws EntradaInvalidaException {
        super(id, dni, nombre, apellido);
        this.legajo = Validador.enteroPositivo(legajo, "legajo");
    }

    public int getLegajo() {
        return legajo;
    }

    public void setLegajo(int legajo) throws EntradaInvalidaException {
        this.legajo = Validador.enteroPositivo(legajo, "legajo");
    }

    /** Cada tipo de empleado describe su rol (polimorfismo). */
    public abstract String mostrarRol();

    @Override
    public String toString() {
        return super.toString() + " | legajo " + legajo;
    }
}
