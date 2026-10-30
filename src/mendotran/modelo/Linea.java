package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Linea {

    private int id;
    private String codigo;
    private String nombre;
    private final Recorrido recorrido = new Recorrido();
    private final List<Vehiculo> vehiculos = new ArrayList<>();

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

    /** Agrega el vehículo (relación bidireccional con {@link Vehiculo}). */
    public void agregarVehiculo(Vehiculo vehiculo) {
        if (vehiculo != null && !vehiculos.contains(vehiculo)) {
            vehiculos.add(vehiculo);
            vehiculo.agregarLinea(this);
        }
    }

    public void quitarVehiculo(Vehiculo vehiculo) {
        if (vehiculos.remove(vehiculo)) {
            vehiculo.quitarLinea(this);
        }
    }

    @Override
    public String toString() {
        return String.format("#%d Línea %s \"%s\" - %d paradas, %d vehículos",
                id, codigo, nombre, recorrido.cantidad(), vehiculos.size());
    }
}
