package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;
import mendotran.util.Validador;

public class Pasajero extends Persona implements Inspeccionable {

    public static final double TARIFA_BOLETO = 700.0;

    private String numeroSube;
    private double saldoSube;
    private boolean boletoPago;

    public Pasajero(String dni, String nombre, String apellido,
                    String numeroSube, double saldoSube) throws EntradaInvalidaException {
        this(0, dni, nombre, apellido, numeroSube, saldoSube, false);
    }

    public Pasajero(int id, String dni, String nombre, String apellido,
                    String numeroSube, double saldoSube, boolean boletoPago) throws EntradaInvalidaException {
        super(id, dni, nombre, apellido);
        this.numeroSube = Validador.numeroSube(numeroSube);
        this.saldoSube = Validador.montoNoNegativo(saldoSube, "saldo SUBE");
        this.boletoPago = boletoPago;
    }

    public String getNumeroSube() {
        return numeroSube;
    }

    public void setNumeroSube(String numeroSube) throws EntradaInvalidaException {
        this.numeroSube = Validador.numeroSube(numeroSube);
    }

    public double getSaldoSube() {
        return saldoSube;
    }

    public void setSaldoSube(double saldoSube) throws EntradaInvalidaException {
        this.saldoSube = Validador.montoNoNegativo(saldoSube, "saldo SUBE");
    }

    public boolean isBoletoPago() {
        return boletoPago;
    }

    public void setBoletoPago(boolean boletoPago) {
        this.boletoPago = boletoPago;
    }

    /** Suma saldo a la tarjeta SUBE. El monto debe ser positivo. */
    public void cargarSaldo(double monto) throws EntradaInvalidaException {
        saldoSube += Validador.montoPositivo(monto, "monto a cargar");
    }

    /**
     * Descuenta la tarifa indicada si el saldo alcanza y marca el boleto como pago.
     * Devuelve {@code true} si pudo abonarse o {@code false} si el saldo fue insuficiente.
     */
    public boolean pagarBoleto(double tarifa) throws EntradaInvalidaException {
        double importe = Validador.montoPositivo(tarifa, "tarifa");
        if (saldoSube < importe) {
            boletoPago = false;
            return false;
        }
        saldoSube -= importe;
        boletoPago = true;
        return true;
    }

    /** Sobrecarga que utiliza la tarifa general ({@link #TARIFA_BOLETO}). */
    public boolean pagarBoleto() throws EntradaInvalidaException {
        return pagarBoleto(TARIFA_BOLETO);
    }

    @Override
    public String inspeccionar() {
        String veredicto = boletoPago ? "REGULAR" : "SIN BOLETO PAGO (posible infracción)";
        return String.format("Pasajero %s | SUBE %s | saldo %s | boleto pago: %s -> %s",
                getNombreCompleto(), numeroSube, Formato.pesos(saldoSube), Formato.siNo(boletoPago), veredicto);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | SUBE %s | saldo %s | boleto pago: %s",
                numeroSube, Formato.pesos(saldoSube), Formato.siNo(boletoPago));
    }
}
