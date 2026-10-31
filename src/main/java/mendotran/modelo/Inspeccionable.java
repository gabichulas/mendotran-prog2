package mendotran.modelo;

/**
 * Contrato para todo objeto que un {@link Inspector} puede auditar.
 * Es el eje del polimorfismo del sistema: Pasajero, Vehiculo y Parada
 * se inspeccionan de forma distinta a través de la misma interfaz.
 */
public interface Inspeccionable {

    /** Devuelve un informe de inspección en una línea. */
    String inspeccionar();
}
