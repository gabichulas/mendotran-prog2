package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;

public class Colectivo extends Vehiculo {

    private static final double COSTO_BASE = 50_000.0;
    private static final double COSTO_POR_PLAZA = 100.0;
    private static final double ADICIONAL_RAMPA = 5_000.0;

    private boolean tieneRampa;

    public Colectivo(int id, int interno, String patente, int capacidad,
                     boolean enServicio, boolean tieneRampa) throws EntradaInvalidaException {
        super(id, interno, patente, capacidad, enServicio);
        this.tieneRampa = tieneRampa;
    }

    public boolean isTieneRampa() {
        return tieneRampa;
    }

    public void setTieneRampa(boolean tieneRampa) {
        this.tieneRampa = tieneRampa;
    }

    @Override
    public String getTipo() {
        return "COLECTIVO";
    }

    @Override
    public double calcularCostoOperativo() {
        return COSTO_BASE + getCapacidad() * COSTO_POR_PLAZA + (tieneRampa ? ADICIONAL_RAMPA : 0);
    }

    @Override
    protected String detalleEspecifico() {
        return "rampa: " + Formato.siNo(tieneRampa) + (tieneRampa ? "" : " (NO ACCESIBLE)");
    }
}
