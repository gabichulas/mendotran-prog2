package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public class Inspector extends Empleado {

    private String zona;

    public Inspector(String dni, String nombre, String apellido, int legajo, String zona)
            throws EntradaInvalidaException {
        this(0, dni, nombre, apellido, legajo, zona);
    }

    public Inspector(int id, String dni, String nombre, String apellido, int legajo, String zona)
            throws EntradaInvalidaException {
        super(id, dni, nombre, apellido, legajo);
        this.zona = Validador.texto(zona, "zona");
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) throws EntradaInvalidaException {
        this.zona = Validador.texto(zona, "zona");
    }

    @Override
    public String mostrarRol() {
        return "Inspector - zona " + zona;
    }

    /**
     * Audita cualquier objeto {@link Inspeccionable} (Pasajero, Vehiculo, Parada).
     * Sobrecarga sin observación.
     */
    public String auditar(Inspeccionable objetivo) throws EntradaInvalidaException {
        return auditar(objetivo, "sin observaciones");
    }

    /** Sobrecarga que agrega una observación del inspector. */
    public String auditar(Inspeccionable objetivo, String observacion) throws EntradaInvalidaException {
        if (objetivo == null) {
            throw new EntradaInvalidaException("No hay un objetivo para auditar (valor nulo).");
        }
        String obs = Validador.texto(observacion, "observación");
        return String.format("[Auditoría de %s | zona %s]%n    %s%n    Observación: %s",
                getNombreCompleto(), zona, objetivo.inspeccionar(), obs);
    }

    @Override
    public String toString() {
        return super.toString() + " | " + mostrarRol();
    }
}
