package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.excepciones.PosicionInvalidaException;
import mendotran.util.Validador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Linea {

    private int id;
    private String codigo;
    private String nombre;
    private final Recorrido recorrido = new Recorrido();
    private final List<Vehiculo> vehiculos = new ArrayList<>();

    public Linea(String codigo, String nombre) throws EntradaInvalidaException {
        this(0, codigo, nombre);
    }

    public Linea(int id, String codigo, String nombre) throws EntradaInvalidaException {
        this.id = id;
        this.codigo = Validador.texto(codigo, "código de línea");
        this.nombre = Validador.texto(nombre, "nombre de línea");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) throws EntradaInvalidaException {
        this.codigo = Validador.texto(codigo, "código de línea");
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) throws EntradaInvalidaException {
        this.nombre = Validador.texto(nombre, "nombre de línea");
    }

    public Recorrido getRecorrido() {
        return recorrido;
    }

    public List<Vehiculo> getVehiculos() {
        return Collections.unmodifiableList(vehiculos);
    }

    /** Agrega el vehículo (relación de agregación bidireccional con {@link Vehiculo}). */
    public void agregarVehiculo(Vehiculo v) {
        if (v != null && !vehiculos.contains(v)) {
            vehiculos.add(v);
            v.agregarLinea(this);
        }
    }

    public void quitarVehiculo(Vehiculo v) {
        if (vehiculos.remove(v)) {
            v.quitarLinea(this);
        }
    }

    /** Delega en el {@link Recorrido} compuesto para agregar una parada. */
    public void agregarParada(Parada p) throws EntradaInvalidaException {
        recorrido.agregarParada(p);
    }

    /** Sobrecarga: agrega una lista ordenada de paradas al recorrido de la línea. */
    public void agregarParada(List<Parada> paradas) throws EntradaInvalidaException {
        if (paradas == null) {
            throw new EntradaInvalidaException("La lista de paradas no puede ser nula.");
        }
        for (Parada p : paradas) {
            recorrido.agregarParada(p);
        }
    }

    public Parada obtenerParadaEn(int posicion) throws PosicionInvalidaException {
        return recorrido.obtenerPorIndice(posicion);
    }

    public boolean quitarParada(String codigo) {
        return recorrido.quitarParada(codigo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Linea otra)) {
            return false;
        }
        return Objects.equals(codigo, otra.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return String.format("#%d Línea %s \"%s\" - %d paradas, %d vehículos",
                id, codigo, nombre, recorrido.cantidad(), vehiculos.size());
    }
}
