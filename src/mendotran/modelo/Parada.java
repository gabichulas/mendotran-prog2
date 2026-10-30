package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;
import mendotran.util.Validador;

public class Parada implements Inspeccionable {

    private int id;
    private int numero;
    private String codigo; // formato MXXXX
    private String direccion;
    private boolean tieneGarita;

    public Parada(int id, int numero, String codigo, String direccion, boolean tieneGarita)
            throws EntradaInvalidaException {
        this.id = id;
        this.numero = Validador.enteroPositivo(numero, "número de parada");
        this.codigo = Validador.codigoParada(codigo);
        this.direccion = Validador.texto(direccion, "dirección");
        this.tieneGarita = tieneGarita;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) throws EntradaInvalidaException {
        this.direccion = Validador.texto(direccion, "dirección");
    }

    public boolean isTieneGarita() {
        return tieneGarita;
    }

    public void setTieneGarita(boolean tieneGarita) {
        this.tieneGarita = tieneGarita;
    }

    /** Descripción corta, usada al listar recorridos. */
    public String resumen() {
        return String.format("[%s] parada %d - %s%s", codigo, numero, direccion, tieneGarita ? " (garita)" : "");
    }

    @Override
    public String inspeccionar() {
        String veredicto = tieneGarita ? "refugio disponible" : "SIN REFUGIO para pasajeros";
        return String.format("Parada %s (n.º %d) en %s | garita: %s -> %s",
                codigo, numero, direccion, Formato.siNo(tieneGarita), veredicto);
    }

    @Override
    public String toString() {
        return "#" + id + " " + resumen();
    }
}
