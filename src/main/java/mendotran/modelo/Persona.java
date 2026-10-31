package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

import java.util.Objects;

/** Raíz de la jerarquía de personas. */
public abstract class Persona {

    private int id;
    private String dni;
    private String nombre;
    private String apellido;

    protected Persona(String dni, String nombre, String apellido) throws EntradaInvalidaException {
        this(0, dni, nombre, apellido);
    }

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

    public void setDni(String dni) throws EntradaInvalidaException {
        this.dni = Validador.dni(dni);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) throws EntradaInvalidaException {
        this.nombre = Validador.texto(nombre, "nombre");
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) throws EntradaInvalidaException {
        this.apellido = Validador.texto(apellido, "apellido");
    }

    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Persona otra)) {
            return false;
        }
        return Objects.equals(dni, otra.dni);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dni);
    }

    @Override
    public String toString() {
        return String.format("#%d %s (DNI %s)", id, getNombreCompleto(), dni);
    }
}
