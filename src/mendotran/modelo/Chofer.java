package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Validador;

public class Chofer extends Empleado {

    private String licencia;
    private Vehiculo vehiculoAsignado; // puede ser null

    public Chofer(int id, String dni, String nombre, String apellido, String legajo,
                  String licencia, Vehiculo vehiculoAsignado) throws EntradaInvalidaException {
        super(id, dni, nombre, apellido, legajo);
        this.licencia = Validador.texto(licencia, "licencia");
        this.vehiculoAsignado = vehiculoAsignado;
    }

    public String getLicencia() {
        return licencia;
    }

    public Vehiculo getVehiculoAsignado() {
        return vehiculoAsignado;
    }

    public void asignarVehiculo(Vehiculo vehiculo) {
        this.vehiculoAsignado = vehiculo;
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
