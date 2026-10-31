package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.util.Formato;
import mendotran.util.Validador;

import java.util.Objects;

public class Parada implements Inspeccionable {

    private int id;
    private int numero;
    private String codigo;
    private String direccion;
    private boolean tieneGarita;

    /** Sobrecarga que genera automáticamente el código {@code MXXXX} a partir del número. */
    public Parada(int numero, String direccion, boolean tieneGarita) throws EntradaInvalidaException {
        this(0, numero, generarCodigo(numero), direccion, tieneGarita);
    }

    public Parada(int id, int numero, String codigo, String direccion, boolean tieneGarita)
            throws EntradaInvalidaException {
        this.id = id;
        this.numero = Validador.enteroPositivo(numero, "número de parada");
        this.codigo = Validador.codigoParada(codigo);
        this.direccion = Validador.texto(direccion, "dirección");
        this.tieneGarita = tieneGarita;
    }

    /** Genera el código oficial Mendotran (ej.: número 123 -> {@code M0123}). */
    public static String generarCodigo(int numero) throws EntradaInvalidaException {
        int n = Validador.enteroPositivo(numero, "número de parada");
        if (n > 9999) {
            throw new EntradaInvalidaException("El número de parada no puede superar 9999 (recibido: " + numero + ").");
        }
        return String.format("M%04d", n);
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

    public void setNumero(int numero) throws EntradaInvalidaException {
        this.numero = Validador.enteroPositivo(numero, "número de parada");
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) throws EntradaInvalidaException {
        this.codigo = Validador.codigoParada(codigo);
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Parada otra)) {
            return false;
        }
        return Objects.equals(codigo, otra.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "#" + id + " " + resumen();
    }
}
