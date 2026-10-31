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

    public void agregarParada(Parada p) throws EntradaInvalidaException {
        if (p == null) {
            throw new EntradaInvalidaException("No se puede agregar una parada nula al recorrido.");
        }
        paradas.add(p);
    }

    /** Sobrecarga: inserta una parada en una posición específica (0 a cantidad, inclusive). */
    public void agregarParada(int posicion, Parada p) throws EntradaInvalidaException, PosicionInvalidaException {
        if (p == null) {
            throw new EntradaInvalidaException("No se puede agregar una parada nula al recorrido.");
        }
        if (posicion < 0 || posicion > paradas.size()) {
            throw new PosicionInvalidaException(posicion, paradas.size());
        }
        paradas.add(posicion, p);
    }

    /** Elimina la primera ocurrencia de la parada con ese código empleando un {@link Iterator}. */
    public boolean quitarParada(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        Iterator<Parada> it = paradas.iterator();
        while (it.hasNext()) {
            if (it.next().getCodigo().equalsIgnoreCase(codigo.trim())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public Parada obtenerPorIndice(int indice) throws PosicionInvalidaException {
        verificarPosicion(indice);
        return paradas.get(indice);
    }

    public int cantidad() {
        return paradas.size();
    }

    public List<Parada> getParadas() {
        return Collections.unmodifiableList(paradas);
    }

    /** Busca con un bucle {@code while}; devuelve el índice o -1 si no está. */
    public int buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return -1;
        }
        int i = 0;
        while (i < paradas.size()) {
            if (paradas.get(i).getCodigo().equalsIgnoreCase(codigo.trim())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** Imprime el recorrido de la primera a la última parada usando un {@link Iterator}. */
    public void mostrarNormal() {
        if (paradas.isEmpty()) {
            System.out.println("  (recorrido sin paradas)");
            return;
        }
        Iterator<Parada> it = paradas.iterator();
        int indice = 0;
        while (it.hasNext()) {
            System.out.printf("  [%d] %s%n", indice++, it.next().resumen());
        }
    }

    /** Imprime el recorrido de la última a la primera parada (vuelta) usando un bucle {@code for}. */
    public void mostrarInvertido() {
        if (paradas.isEmpty()) {
            System.out.println("  (recorrido sin paradas)");
            return;
        }
        for (int i = paradas.size() - 1; i >= 0; i--) {
            System.out.printf("  [%d] %s%n", i, paradas.get(i).resumen());
        }
    }

    private void verificarPosicion(int indice) throws PosicionInvalidaException {
        if (indice < 0 || indice >= paradas.size()) {
            throw new PosicionInvalidaException(indice, paradas.size());
        }
    }
}
