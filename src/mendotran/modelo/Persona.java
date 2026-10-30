package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

/** Raíz de la jerarquía de personas. */
public abstract class Persona {

    private int id;
    private final String dni;
    private String nombre;
    private String apellido;

    protected Persona(int id, String dni, String nombre, String apellido) throws EntradaInvalidaException {
        this.id = id;
        this.dni = Validador.dni(dni);
        this.nombre = Validador.texto(nombre, "nombre");
        this.apellido = Validador.texto(apellido, "apellido");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setNombre(String nombre) throws EntradaInvalidaException {
        this.nombre = Validador.texto(nombre, "nombre");
    }

    public void setApellido(String apellido) throws EntradaInvalidaException {
        this.apellido = Validador.texto(apellido, "apellido");
    }

    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public String toString() {
        return String.format("#%d %s (DNI %s)", id, getNombreCompleto(), dni);
    }
}
