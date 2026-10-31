package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;
import mendotran.util.Validador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Vehiculo implements Inspeccionable {

    private int id;
    private int interno;
    private String patente;
    private int capacidad;
    private boolean enServicio;
    private final List<Linea> lineas = new ArrayList<>();

    protected Vehiculo(int interno, String patente, int capacidad, boolean enServicio)
            throws EntradaInvalidaException {
        this(0, interno, patente, capacidad, enServicio);
    }

    protected Vehiculo(int id, int interno, String patente, int capacidad, boolean enServicio)
            throws EntradaInvalidaException {
        this.id = id;
        this.interno = Validador.enteroPositivo(interno, "interno");
        this.patente = Validador.patente(patente);
        this.capacidad = Validador.enteroPositivo(capacidad, "capacidad");
        this.enServicio = enServicio;
    }

    /** Identificador de tipo usado también por la capa de persistencia. */
    public abstract String getTipo();

    /** Costo operativo diario estimado, según el tipo de vehículo. */
    public abstract double calcularCostoOperativo();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInterno() {
        return interno;
    }

    public void setInterno(int interno) throws EntradaInvalidaException {
        this.interno = Validador.enteroPositivo(interno, "interno");
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) throws EntradaInvalidaException {
        this.patente = Validador.patente(patente);
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) throws EntradaInvalidaException {
        this.capacidad = Validador.enteroPositivo(capacidad, "capacidad");
    }

    public boolean isEnServicio() {
        return enServicio;
    }

    public void setEnServicio(boolean enServicio) {
        this.enServicio = enServicio;
    }

    /** Pone el vehículo en servicio sin indicar motivo. */
    public void ponerEnServicio() {
        this.enServicio = true;
    }

    /** Sobrecarga: pone el vehículo en servicio dejando constancia del motivo. */
    public void ponerEnServicio(String motivo) throws EntradaInvalidaException {
        String motivoValido = Validador.texto(motivo, "motivo");
        this.enServicio = true;
        System.out.println("  [Servicio] " + getTipo() + " interno " + interno + " en servicio (" + motivoValido + ")");
    }

    public void fueraDeServicio() {
        this.enServicio = false;
    }

    public List<Linea> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    /** Asocia la línea (relación bidireccional con {@link Linea}). */
    public void agregarLinea(Linea linea) {
        if (linea != null && !lineas.contains(linea)) {
            lineas.add(linea);
            linea.agregarVehiculo(this);
        }
    }

    public void quitarLinea(Linea linea) {
        if (lineas.remove(linea)) {
            linea.quitarVehiculo(this);
        }
    }

    @Override
    public String inspeccionar() {
        StringBuilder codigos = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) {
                codigos.append(", ");
            }
            codigos.append(lineas.get(i).getCodigo());
        }
        String lineasTxt = lineas.isEmpty() ? "ninguna" : codigos.toString();
        return String.format("%s interno %d (patente %s) | %s | capacidad %d | líneas: %s | costo operativo: %s",
                getTipo(), interno, patente, enServicio ? "EN SERVICIO" : "FUERA DE SERVICIO",
                capacidad, lineasTxt, Formato.pesos(calcularCostoOperativo()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehiculo otro)) {
            return false;
        }
        return interno == otro.interno;
    }

    @Override
    public int hashCode() {
        return Objects.hash(interno);
    }

    @Override
    public String toString() {
        return String.format("#%d %s interno %d [%s] cap. %d - %s",
                id, getTipo(), interno, patente, capacidad, enServicio ? "en servicio" : "fuera de servicio");
    }
}
