package mendotran.modelo;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.excepciones.PosicionInvalidaException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Secuencia ordenada de paradas. Las posiciones son índices que empiezan en 0. */
public class Recorrido {

    private final List<Parada> paradas = new ArrayList<>();

    public void agregarParada(Parada parada) throws EntradaInvalidaException {
        if (parada == null) {
            throw new EntradaInvalidaException("No se puede agregar una parada nula al recorrido.");
        }
        paradas.add(parada);
    }

    /** Inserta una parada en una posición (0 a cantidad, inclusive). */
    public void agregarParada(int posicion, Parada parada) throws EntradaInvalidaException, PosicionInvalidaException {
        if (parada == null) {
            throw new EntradaInvalidaException("No se puede agregar una parada nula al recorrido.");
        }
        if (posicion < 0 || posicion > paradas.size()) {
            throw new PosicionInvalidaException(posicion, paradas.size());
        }
        paradas.add(posicion, parada);
    }

    public Parada obtenerParada(int posicion) throws PosicionInvalidaException {
        verificarPosicion(posicion);
        return paradas.get(posicion);
    }

    public Parada eliminarParada(int posicion) throws PosicionInvalidaException {
        verificarPosicion(posicion);
        return paradas.remove(posicion);
    }

    public int cantidad() {
        return paradas.size();
    }

    public List<Parada> getParadas() {
        return Collections.unmodifiableList(paradas);
    }

    /** Busca con un bucle while; devuelve la posición o -1 si no está. */
    public int buscarPorCodigo(String codigo) {
        int i = 0;
        while (i < paradas.size()) {
            if (paradas.get(i).getCodigo().equalsIgnoreCase(codigo)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** Muestra el recorrido de la primera a la última parada, usando un Iterator. */
    public String mostrarNormal() {
        if (paradas.isEmpty()) {
            return "  (recorrido sin paradas)";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Parada> it = paradas.iterator();
        int indice = 0;
        while (it.hasNext()) {
            sb.append(String.format("  [%d] %s%n", indice++, it.next().resumen()));
        }
        return sb.toString().stripTrailing();
    }

    /** Muestra el recorrido de la última a la primera parada (vuelta), usando un bucle for. */
    public String mostrarInvertido() {
        if (paradas.isEmpty()) {
            return "  (recorrido sin paradas)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = paradas.size() - 1; i >= 0; i--) {
            sb.append(String.format("  [%d] %s%n", i, paradas.get(i).resumen()));
        }
        return sb.toString().stripTrailing();
    }

    private void verificarPosicion(int posicion) throws PosicionInvalidaException {
        if (posicion < 0 || posicion >= paradas.size()) {
            throw new PosicionInvalidaException(posicion, paradas.size());
        }
    }
}
