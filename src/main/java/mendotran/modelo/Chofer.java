package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public class Chofer extends Empleado {

    private String licencia;
    private Vehiculo vehiculoAsignado;

    public Chofer(String dni, String nombre, String apellido, int legajo,
                  String licencia, Vehiculo vehiculoAsignado) throws EntradaInvalidaException {
        this(0, dni, nombre, apellido, legajo, licencia, vehiculoAsignado);
    }

    public Chofer(int id, String dni, String nombre, String apellido, int legajo,
                  String licencia, Vehiculo vehiculoAsignado) throws EntradaInvalidaException {
        super(id, dni, nombre, apellido, legajo);
        this.licencia = Validador.texto(licencia, "licencia");
        this.vehiculoAsignado = vehiculoAsignado;
    }

    public String getLicencia() {
        return licencia;
    }

    public void setLicencia(String licencia) throws EntradaInvalidaException {
        this.licencia = Validador.texto(licencia, "licencia");
    }

    public Vehiculo getVehiculoAsignado() {
        return vehiculoAsignado;
    }

    public void asignarVehiculo(Vehiculo v) throws EntradaInvalidaException {
        if (v == null) {
            throw new EntradaInvalidaException("Debe indicar un vehículo válido para asignar.");
        }
        this.vehiculoAsignado = v;
    }

    public void desasignarVehiculo() {
        this.vehiculoAsignado = null;
    }

    @Override
    public String mostrarRol() {
        String vehiculo = vehiculoAsignado == null
                ? "sin vehículo asignado"
                : vehiculoAsignado.getTipo() + " interno " + vehiculoAsignado.getInterno();
        return "Chofer (licencia " + licencia + ") - " + vehiculo;
    }

    @Override
    public String toString() {
        return super.toString() + " | " + mostrarRol();
    }
}
