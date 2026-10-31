package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public class Metrotranvia extends Vehiculo {

    private static final double COSTO_POR_DUPLA = 400_000.0;
    private static final double COSTO_POR_PLAZA = 50.0;

    private int cantidadDuplas;

    public Metrotranvia(int interno, String patente, int capacidad,
                        boolean enServicio, int cantidadDuplas) throws EntradaInvalidaException {
        this(0, interno, patente, capacidad, enServicio, cantidadDuplas);
    }

    public Metrotranvia(int id, int interno, String patente, int capacidad,
                        boolean enServicio, int cantidadDuplas) throws EntradaInvalidaException {
        super(id, interno, patente, capacidad, enServicio);
        this.cantidadDuplas = Validador.enteroPositivo(cantidadDuplas, "cantidad de duplas");
    }

    public int getCantidadDuplas() {
        return cantidadDuplas;
    }

    public void setCantidadDuplas(int cantidadDuplas) throws EntradaInvalidaException {
        this.cantidadDuplas = Validador.enteroPositivo(cantidadDuplas, "cantidad de duplas");
    }

    @Override
    public String getTipo() {
        return "METROTRANVIA";
    }

    @Override
    public double calcularCostoOperativo() {
        return cantidadDuplas * COSTO_POR_DUPLA + getCapacidad() * COSTO_POR_PLAZA;
    }

    @Override
    public String inspeccionar() {
        return super.inspeccionar() + " | duplas: " + cantidadDuplas;
    }
}
