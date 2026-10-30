package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;
import mendotran.util.Validador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Vehiculo implements Inspeccionable {

    private int id;
    private final int interno;
    private String patente;
    private final int capacidad;
    private boolean enServicio;
    private final List<Linea> lineas = new ArrayList<>();

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

    /** Texto específico de cada subclase para el informe de inspección. */
    protected abstract String detalleEspecifico();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInterno() {
        return interno;
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

    public boolean isEnServicio() {
        return enServicio;
    }

    /** Pone el vehículo en servicio (sin asociarlo a una línea). */
    public void ponerEnServicio() {
        this.enServicio = true;
    }

    /** Sobrecarga: pone el vehículo en servicio y lo asocia a una línea. */
    public void ponerEnServicio(Linea linea) throws EntradaInvalidaException {
        if (linea == null) {
            throw new EntradaInvalidaException("Debe indicar una línea para poner el vehículo en servicio.");
        }
        agregarLinea(linea);
        this.enServicio = true;
    }

    public void retirarDeServicio() {
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
        return String.format("%s interno %d (patente %s) | %s | capacidad %d | líneas: %s | costo operativo: %s | %s",
                getTipo(), interno, patente, enServicio ? "EN SERVICIO" : "FUERA DE SERVICIO",
                capacidad, lineasTxt, Formato.pesos(calcularCostoOperativo()), detalleEspecifico());
    }

    @Override
    public String toString() {
        return String.format("#%d %s interno %d [%s] cap. %d - %s",
                id, getTipo(), interno, patente, capacidad, enServicio ? "en servicio" : "fuera de servicio");
    }
}
