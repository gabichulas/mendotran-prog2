package mendotran.app;

import mendotran.dao.LineaDAO;
import mendotran.dao.ParadaDAO;
import mendotran.dao.PersonaDAO;
import mendotran.dao.VehiculoDAO;
import mendotran.modelo.Chofer;
import mendotran.modelo.Colectivo;
import mendotran.modelo.Empleado;
import mendotran.modelo.Inspeccionable;
import mendotran.modelo.Inspector;
import mendotran.modelo.Linea;
import mendotran.modelo.Metrotranvia;
import mendotran.modelo.Parada;
import mendotran.modelo.Pasajero;
import mendotran.modelo.Persona;
import mendotran.modelo.Recorrido;
import mendotran.modelo.Vehiculo;
import mendotran.util.Formato;
import mendotran.util.Validador;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Secuencia automática de pruebas: CRUD en SQLite, polimorfismo con Inspeccionable
 * y manejo de excepciones personalizadas. Se ejecuta con {@code --demo}.
 */
public class Demo {

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private final PersonaDAO personaDAO = new PersonaDAO();
    private final VehiculoDAO vehiculoDAO = new VehiculoDAO();
    private final ParadaDAO paradaDAO = new ParadaDAO();
    private final LineaDAO lineaDAO = new LineaDAO();

    private int excepcionesControladas = 0;
    private int errorresInesperados = 0;

    public void ejecutar() throws Exception {
        System.out.println("##############################################");
        System.out.println("#   SISTEMA DE GESTIÓN MENDOTRAN - DEMO      #");
        System.out.println("##############################################");

        // ------------------------------------------------------------------
        seccion("1. CRUD de VEHÍCULOS (tabla única con columna 'tipo')");
        Colectivo c1 = new Colectivo(0, 101, "AB123CD", 40, false, true);
        Colectivo c2 = new Colectivo(0, 102, "ABC123", 38, false, false);
        Metrotranvia mt = new Metrotranvia(0, 201, "MT001AA", 250, false, 2);
        Colectivo temporal = new Colectivo(0, 999, "ZZ999ZZ", 30, false, false);
        for (Vehiculo v : List.of(c1, c2, mt, temporal)) {
            int id = vehiculoDAO.crear(v);
            System.out.println("  CREATE -> id " + id + "  " + v);
        }
        System.out.println("  READ   -> " + vehiculoDAO.obtenerTodos().size() + " vehículos en la base:");
        for (Vehiculo v : vehiculoDAO.obtenerTodos()) {
            System.out.println("            " + v);
        }
        c1.ponerEnServicio();
        mt.ponerEnServicio();
        vehiculoDAO.actualizar(c1);
        vehiculoDAO.actualizar(mt);
        Vehiculo c1Releido = vehiculoDAO.obtenerPorId(c1.getId()).orElseThrow();
        System.out.println("  UPDATE -> releído desde la base: " + c1Releido);
        vehiculoDAO.eliminar(temporal.getId());
        System.out.println("  DELETE -> eliminado el interno 999; quedan " + vehiculoDAO.obtenerTodos().size() + " vehículos");

        // ------------------------------------------------------------------
        seccion("2. CRUD de PERSONAS (Pasajero / Chofer / Inspector en una tabla)");
        Pasajero ana = new Pasajero(0, "30123456", "Ana", "Pérez", "6061260000000001", 1000.0);
        Pasajero luis = new Pasajero(0, "28456789", "Luis", "Gómez", "6061260000000002", 200.0);
        Pasajero borrar = new Pasajero(0, "40111222", "Temporal", "Borrar", "6061260000000003", 0);
        Chofer chofer = new Chofer(0, "25987654", "Carlos", "Ruiz", "LEG-001", "D1-445566", c1);
        Inspector insp = new Inspector(0, "27111333", "María", "Torres", "LEG-900", "Zona Centro (Ciudad de Mendoza)");
        for (Persona p : List.of(ana, luis, borrar, chofer, insp)) {
            int id = personaDAO.crear(p);
            System.out.println("  CREATE -> id " + id + "  " + p);
        }
        System.out.println("  READ   -> personas en la base:");
        for (Persona p : personaDAO.obtenerTodos()) {
            System.out.println("            " + p);
        }
        ana.cargarSaldo(500);
        personaDAO.actualizar(ana);
        ana.pagarBoleto();
        personaDAO.actualizar(ana);
        Persona anaReleida = personaDAO.obtenerPorId(ana.getId()).orElseThrow();
        System.out.println("  UPDATE -> Ana cargó $500 y pagó un boleto: " + anaReleida);
        personaDAO.eliminar(borrar.getId());
        System.out.println("  DELETE -> eliminado 'Temporal Borrar'; quedan " + personaDAO.obtenerTodos().size() + " personas");
        for (Chofer ch : personaDAO.obtenerPorClase(Chofer.class)) {
            System.out.println("  JOIN   -> chofer releído con su vehículo resuelto: " + ch.mostrarRol());
        }

        // ------------------------------------------------------------------
        seccion("3. CRUD de PARADAS y LÍNEAS (con recorrido ordenado)");
        Parada p1 = new Parada(0, 1, "M0001", "Av. San Martín 1200, Ciudad", true);
        Parada p2 = new Parada(0, 2, "M0002", "Plaza Independencia, Ciudad", true);
        Parada p3 = new Parada(0, 3, "M0003", "Av. Las Heras 800, Ciudad", false);
        Parada p4 = new Parada(0, 4, "M0004", "Terminal del Sol, Guaymallén", true);
        Parada p5 = new Parada(0, 5, "M0005", "Acceso Este 3200, Guaymallén", false);
        Parada extra = new Parada(0, 99, "M0099", "Parada de prueba", false);
        for (Parada p : List.of(p1, p2, p3, p4, p5, extra)) {
            paradaDAO.crear(p);
        }
        System.out.println("  CREATE -> " + paradaDAO.obtenerTodos().size() + " paradas creadas");
        p5.setTieneGarita(true);
        paradaDAO.actualizar(p5);
        System.out.println("  UPDATE -> " + paradaDAO.obtenerPorId(p5.getId()).orElseThrow());
        paradaDAO.eliminar(extra.getId());
        System.out.println("  DELETE -> eliminada M0099; quedan " + paradaDAO.obtenerTodos().size() + " paradas");

        Linea l62 = new Linea(0, "62", "Ciudad - Guaymallén");
        for (Parada p : List.of(p1, p2, p3, p4, p5)) {
            l62.getRecorrido().agregarParada(p);
        }
        c1.ponerEnServicio(l62);   // sobrecarga con línea
        c2.ponerEnServicio(l62);
        vehiculoDAO.actualizar(c2);
        Linea lMt = new Linea(0, "MT", "Metrotranvía Mendoza");
        lMt.getRecorrido().agregarParada(p2);
        lMt.getRecorrido().agregarParada(p4);
        mt.ponerEnServicio(lMt);
        lineaDAO.crear(l62);
        lineaDAO.crear(lMt);
        System.out.println("  CREATE -> líneas guardadas con sus paradas ordenadas y vehículos:");
        for (Linea l : lineaDAO.obtenerTodos()) {
            System.out.println("            " + l);
        }

        l62.getRecorrido().eliminarParada(2); // saca M0003
        l62.setNombre("Ciudad - Guaymallén (directo)");
        lineaDAO.actualizar(l62);
        Linea l62Releida = lineaDAO.obtenerPorId(l62.getId()).orElseThrow();
        System.out.println("  UPDATE -> línea 62 sin M0003, releída: " + l62Releida);
        lineaDAO.eliminar(lMt.getId());
        System.out.println("  DELETE -> eliminada la línea MT; quedan " + lineaDAO.obtenerTodos().size() + " línea(s)");

        // ------------------------------------------------------------------
        seccion("4. RECORRIDO: Iterator, bucles y PosicionInvalidaException");
        Recorrido recorrido = l62Releida.getRecorrido();
        System.out.println("  Ida (Iterator):");
        System.out.println(recorrido.mostrarNormal());
        System.out.println("  Vuelta (for descendente):");
        System.out.println(recorrido.mostrarInvertido());
        System.out.println("  Posición de M0004 (while): " + recorrido.buscarPorCodigo("M0004"));
        System.out.println("  Posición de M0123 (while): " + recorrido.buscarPorCodigo("M0123") + "  (-1 = no está)");
        System.out.print("  Recorrido manual con Iterator explícito: ");
        Iterator<Parada> it = recorrido.getParadas().iterator();
        while (it.hasNext()) {
            System.out.print(it.next().getCodigo() + (it.hasNext() ? " -> " : "\n"));
        }
        intentar("obtenerParada(-1) en un recorrido de " + recorrido.cantidad() + " paradas",
                () -> recorrido.obtenerParada(-1));
        intentar("obtenerParada(99)", () -> recorrido.obtenerParada(99));
        intentar("agregarParada en la posición 50", () -> recorrido.agregarParada(50, p1));
        intentar("eliminarParada(4) con solo 4 paradas (índices 0 a 3)", () -> recorrido.eliminarParada(4));
        intentar("obtenerParada(0) en un recorrido vacío", () -> new Recorrido().obtenerParada(0));

        // ------------------------------------------------------------------
        seccion("5. POLIMORFISMO: el Inspector audita todo lo 'Inspeccionable'");
        List<Inspeccionable> objetivos = new ArrayList<>();
        objetivos.add(ana);
        objetivos.add(luis);
        objetivos.add(c1);
        objetivos.add(c2);
        objetivos.add(mt);
        objetivos.add(p1);
        objetivos.add(p3);
        for (Inspeccionable objetivo : objetivos) {
            System.out.println(insp.auditar(objetivo));
        }
        System.out.println(insp.auditar(c2, "Sin rampa: no cumple con accesibilidad (sobrecarga con observación)"));

        System.out.println("\n  Polimorfismo de Empleado.mostrarRol():");
        List<Empleado> empleados = List.of(chofer, insp);
        for (Empleado e : empleados) {
            System.out.println("    " + e.getNombreCompleto() + " -> " + e.mostrarRol());
        }
        System.out.println("  Polimorfismo de Vehiculo.calcularCostoOperativo():");
        for (Vehiculo v : List.of(c1, c2, mt)) {
            System.out.println("    " + v.getTipo() + " interno " + v.getInterno() + " -> "
                    + Formato.pesos(v.calcularCostoOperativo()));
        }

        // ------------------------------------------------------------------
        seccion("6. EXCEPCIONES PERSONALIZADAS ante datos inválidos");
        intentar("DNI con letras ('12AB')",
                () -> new Pasajero(0, "12AB", "X", "Y", "6061260000000009", 0));
        intentar("nombre vacío", () -> new Pasajero(0, "30999888", "   ", "Y", "6061260000000009", 0));
        intentar("SUBE de 5 dígitos", () -> new Pasajero(0, "30999888", "X", "Y", "12345", 0));
        intentar("patente 'PAT-INVALIDA'", () -> new Colectivo(0, 500, "PAT-INVALIDA", 30, false, true));
        intentar("capacidad 0", () -> new Colectivo(0, 500, "AA111BB", 0, false, true));
        intentar("código de parada 'X12' (debe ser MXXXX)", () -> new Parada(0, 7, "X12", "Calle Falsa 123", false));
        intentar("cargarSaldo(-50)", () -> ana.cargarSaldo(-50));
        intentar("pagarBoleto() con saldo insuficiente (Luis tiene $200)", luis::pagarBoleto);
        intentar("auditar(null)", () -> insp.auditar(null));
        intentar("auditar con observación en blanco", () -> insp.auditar(p1, "   "));
        intentar("texto 'abc' como número entero", () -> Validador.parsearEntero("abc", "interno"));
        intentar("respuesta 'quizás' en un campo s/n", () -> Validador.parsearSiNo("quizás", "tiene rampa"));
        intentar("DNI duplicado al guardar en SQLite",
                () -> personaDAO.crear(new Pasajero(0, "30123456", "Otra", "Persona", "6061260000000008", 0)));
        intentar("eliminar un vehículo inexistente (id 12345)", () -> vehiculoDAO.eliminar(12345));
        intentar("actualizar una entidad que nunca se guardó",
                () -> vehiculoDAO.actualizar(new Colectivo(0, 700, "CC333DD", 20, false, false)));

        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("==============================================");
        System.out.println(" DEMO FINALIZADA");
        System.out.println("  Excepciones controladas (esperadas): " + excepcionesControladas);
        System.out.println("  Intentos que NO fallaron como se esperaba: " + errorresInesperados);
        System.out.println("  Base de datos de la demo: mendotran_demo.db");
        System.out.println("==============================================");
    }

    private void seccion(String titulo) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println(" " + titulo);
        System.out.println("----------------------------------------------");
    }

    /** Ejecuta una acción que se espera que falle y muestra la excepción capturada. */
    private void intentar(String descripcion, Accion accion) {
        try {
            accion.ejecutar();
            errorresInesperados++;
            System.out.println("  ?? " + descripcion + " -> NO lanzó excepción (inesperado)");
        } catch (Exception e) {
            excepcionesControladas++;
            System.out.println("  OK " + descripcion);
            System.out.println("       capturada " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
