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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Secuencia automática de demostración: CRUD y búsquedas SQL en SQLite,
 * polimorfismo con {@link Inspeccionable}, estructuras de iteración y manejo
 * de excepciones personalizadas. Se ejecuta con {@code --demo}.
 */
public class Demo {

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private final PersonaDAO personaDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ParadaDAO paradaDAO;
    private final LineaDAO lineaDAO;

    private int excepcionesControladas = 0;
    private int erroresInesperados = 0;

    public Demo(Connection conexion) {
        this.personaDAO = new PersonaDAO(conexion);
        this.vehiculoDAO = new VehiculoDAO(conexion);
        this.paradaDAO = new ParadaDAO(conexion);
        this.lineaDAO = new LineaDAO(conexion);
    }

    public void ejecutar() throws Exception {
        System.out.println("##############################################");
        System.out.println("#   SISTEMA DE GESTIÓN MENDOTRAN - DEMO      #");
        System.out.println("##############################################");

        // ------------------------------------------------------------------
        seccion("1. CRUD y filtrado SQL de VEHÍCULOS");
        Colectivo c1 = new Colectivo(101, "AB123CD", 40, false, true);
        Colectivo c2 = new Colectivo(102, "ABC123", 38, false, false);
        Metrotranvia mt = new Metrotranvia(201, "MT001AA", 250, false, 2);
        Colectivo temporal = new Colectivo(999, "ZZ999ZZ", 30, false, false);
        for (Vehiculo v : List.of(c1, c2, mt, temporal)) {
            int id = vehiculoDAO.crear(v);
            System.out.println("  CREATE -> id " + id + "  " + v);
        }
        c1.ponerEnServicio();
        mt.ponerEnServicio("Inicio frecuencia matutina");
        vehiculoDAO.actualizar(c1);
        vehiculoDAO.actualizar(mt);
        c2.ponerEnServicio();
        c2.fueraDeServicio();
        vehiculoDAO.actualizar(c2);

        System.out.println("  SQL FILTER (en servicio = sí): "
                + vehiculoDAO.buscar(null, null, true).size() + " vehículo(s)");
        vehiculoDAO.eliminar(temporal.getId());
        System.out.println("  DELETE -> eliminado interno 999; quedan " + vehiculoDAO.obtenerTodos().size() + " vehículos");

        // ------------------------------------------------------------------
        seccion("2. CRUD y filtrado SQL de PERSONAS (Pasajero / Chofer / Inspector)");
        Pasajero ana = new Pasajero("30123456", "Ana", "Pérez", "6061260000000001", 1000.0);
        Pasajero luis = new Pasajero("28456789", "Luis", "Gómez", "6061260000000002", 200.0);
        Pasajero borrar = new Pasajero("40111222", "Temporal", "Borrar", "6061260000000003", 0);
        Chofer chofer = new Chofer("25987654", "Carlos", "Ruiz", 1001, "D1-445566", c1);
        Inspector insp = new Inspector("27111333", "María", "Torres", 9001, "Zona Centro (Ciudad de Mendoza)");
        for (Persona p : List.of(ana, luis, borrar, chofer, insp)) {
            int id = personaDAO.crear(p);
            System.out.println("  CREATE -> id " + id + "  " + p);
        }

        ana.cargarSaldo(500);
        boolean pagoAna = ana.pagarBoleto(Pasajero.TARIFA_BOLETO);
        boolean pagoLuis = luis.pagarBoleto(Pasajero.TARIFA_BOLETO);
        personaDAO.actualizar(ana);
        personaDAO.actualizar(luis);
        System.out.println("  UPDATE -> Ana pagó boleto (" + pagoAna + "): " + personaDAO.obtenerPorId(ana.getId()).orElseThrow());
        System.out.println("  UPDATE -> Luis intentó pagar con $200 (" + pagoLuis + "): " + personaDAO.obtenerPorId(luis.getId()).orElseThrow());

        chofer.desasignarVehiculo();
        chofer.asignarVehiculo(c1);
        personaDAO.actualizar(chofer);
        personaDAO.eliminar(borrar.getId());
        System.out.println("  SQL FILTER (búsqueda 'Ruiz' en EMPLEADO): " + personaDAO.buscar("Ruiz", "EMPLEADO"));
        for (Chofer ch : personaDAO.obtenerPorClase(Chofer.class)) {
            System.out.println("  SQL JOIN -> chofer releído con vehículo: " + ch.mostrarRol());
        }

        // ------------------------------------------------------------------
        seccion("3. CRUD de PARADAS y LÍNEAS (composición con Recorrido y agregación con Vehiculo)");
        Parada p1 = new Parada(1, "Av. San Martín 1200, Ciudad", true);
        Parada p2 = new Parada(2, "Plaza Independencia, Ciudad", true);
        Parada p3 = new Parada(3, "Av. Las Heras 800, Ciudad", false);
        Parada p4 = new Parada(4, "Terminal del Sol, Guaymallén", true);
        Parada p5 = new Parada(5, "Acceso Este 3200, Guaymallén", false);
        Parada extra = new Parada(99, "Parada de prueba", false);
        for (Parada p : List.of(p1, p2, p3, p4, p5, extra)) {
            paradaDAO.crear(p);
        }
        System.out.println("  CREATE -> " + paradaDAO.obtenerTodos().size() + " paradas con código generado (ej. " + p1.getCodigo() + ")");
        p5.setTieneGarita(true);
        paradaDAO.actualizar(p5);
        System.out.println("  SQL FILTER (paradas con garita en Guaymallén): " + paradaDAO.buscar("Guaymallén", true));
        paradaDAO.eliminar(extra.getId());

        Linea l62 = new Linea("62", "Ciudad - Guaymallén");
        l62.agregarParada(List.of(p1, p2, p3, p4, p5)); // sobrecarga con List<Parada>
        l62.agregarVehiculo(c1);
        l62.agregarVehiculo(c2);

        Linea lMt = new Linea("MT", "Metrotranvía Mendoza");
        lMt.agregarParada(p2);
        lMt.agregarParada(p4);
        lMt.agregarVehiculo(mt);

        lineaDAO.crear(l62);
        lineaDAO.crear(lMt);
        System.out.println("  CREATE -> líneas guardadas:");
        for (Linea l : lineaDAO.obtenerTodos()) {
            System.out.println("            " + l);
        }

        l62.quitarParada("M0003"); // usa Iterator.remove() internamente
        l62.setNombre("Ciudad - Guaymallén (directo)");
        lineaDAO.actualizar(l62);
        Linea l62Releida = lineaDAO.obtenerPorId(l62.getId()).orElseThrow();
        System.out.println("  UPDATE -> línea 62 sin M0003, releída: " + l62Releida);
        System.out.println("  SQL FILTER (líneas con 'Guaymallén'): " + lineaDAO.buscarPorTexto("Guaymallén"));
        lineaDAO.eliminar(lMt.getId());
        System.out.println("  DELETE -> eliminada la línea MT; quedan " + lineaDAO.obtenerTodos().size() + " línea(s)");

        // ------------------------------------------------------------------
        seccion("4. RECORRIDO: Iterator, for, while y PosicionInvalidaException");
        Recorrido recorrido = l62Releida.getRecorrido();
        System.out.println("  Ida (Iterator):");
        recorrido.mostrarNormal();
        System.out.println("  Vuelta (for descendente):");
        recorrido.mostrarInvertido();
        System.out.println("  Parada en índice 1: " + l62Releida.obtenerParadaEn(1).resumen());
        System.out.println("  Posición de M0004 (while): " + recorrido.buscarPorCodigo("M0004"));
        System.out.println("  Posición de M0123 (while): " + recorrido.buscarPorCodigo("M0123") + "  (-1 = no está)");
        System.out.print("  Recorrido manual con Iterator explícito: ");
        Iterator<Parada> it = recorrido.getParadas().iterator();
        while (it.hasNext()) {
            System.out.print(it.next().getCodigo() + (it.hasNext() ? " -> " : "\n"));
        }
        intentar("obtenerParadaEn(-1) en un recorrido de " + recorrido.cantidad() + " paradas",
                () -> l62Releida.obtenerParadaEn(-1));
        intentar("obtenerPorIndice(99)", () -> recorrido.obtenerPorIndice(99));
        intentar("agregarParada en la posición 50", () -> recorrido.agregarParada(50, p1));
        intentar("obtenerPorIndice(0) en un recorrido vacío", () -> new Recorrido().obtenerPorIndice(0));

        // ------------------------------------------------------------------
        seccion("5. POLIMORFISMO: el Inspector audita entidades releídas de SQLite");
        List<Inspeccionable> objetivos = new ArrayList<>();
        objetivos.add(personaDAO.obtenerPorId(ana.getId()).map(Pasajero.class::cast).orElseThrow());
        objetivos.add(personaDAO.obtenerPorId(luis.getId()).map(Pasajero.class::cast).orElseThrow());
        Vehiculo c1Db = vehiculoDAO.obtenerPorId(c1.getId()).orElseThrow();
        Vehiculo c2Db = vehiculoDAO.obtenerPorId(c2.getId()).orElseThrow();
        Vehiculo mtDb = vehiculoDAO.obtenerPorId(mt.getId()).orElseThrow();
        objetivos.add(c1Db);
        objetivos.add(c2Db);
        objetivos.add(mtDb);
        objetivos.add(paradaDAO.obtenerPorId(p1.getId()).orElseThrow());
        objetivos.add(paradaDAO.obtenerPorId(p3.getId()).orElseThrow());
        for (Inspeccionable objetivo : objetivos) {
            System.out.println(insp.auditar(objetivo));
        }
        System.out.println(insp.auditar(c2Db, "Sin rampa: no cumple con accesibilidad (sobrecarga con observación)"));

        System.out.println("\n  Polimorfismo de Empleado.mostrarRol():");
        for (Empleado e : personaDAO.obtenerPorClase(Empleado.class)) {
            System.out.println("    " + e.getNombreCompleto() + " -> " + e.mostrarRol());
        }
        System.out.println("  Polimorfismo de Vehiculo.calcularCostoOperativo():");
        for (Vehiculo v : List.of(c1Db, c2Db, mtDb)) {
            System.out.println("    " + v.getTipo() + " interno " + v.getInterno() + " -> "
                    + Formato.pesos(v.calcularCostoOperativo()));
        }

        // ------------------------------------------------------------------
        seccion("6. EXCEPCIONES PERSONALIZADAS e integridad SQLite");
        intentar("DNI con letras ('12AB')",
                () -> new Pasajero("12AB", "X", "Y", "6061260000000009", 0));
        intentar("nombre vacío", () -> new Pasajero("30999888", "   ", "Y", "6061260000000009", 0));
        intentar("SUBE de 5 dígitos", () -> new Pasajero("30999888", "X", "Y", "12345", 0));
        intentar("legajo negativo en empleado", () -> new Inspector("31222333", "Juan", "Paz", -5, "Centro"));
        intentar("patente 'PAT-INVALIDA'", () -> new Colectivo(500, "PAT-INVALIDA", 30, false, true));
        intentar("capacidad 0", () -> new Colectivo(500, "AA111BB", 0, false, true));
        intentar("código de parada 'X12' (debe ser MXXXX)", () -> new Parada(0, 7, "X12", "Calle Falsa 123", false));
        intentar("generarCodigo(0)", () -> Parada.generarCodigo(0));
        intentar("cargarSaldo(-50)", () -> ana.cargarSaldo(-50));
        intentar("pagarBoleto(-100) con tarifa inválida", () -> ana.pagarBoleto(-100));
        intentar("auditar(null)", () -> insp.auditar(null));
        intentar("auditar con observación en blanco", () -> insp.auditar(p1, "   "));
        intentar("texto 'abc' como número entero", () -> Validador.parsearEntero("abc", "interno"));
        intentar("respuesta 'quizás' en un campo s/n", () -> Validador.parsearSiNo("quizás", "tiene rampa"));
        intentar("DNI duplicado al guardar en SQLite",
                () -> personaDAO.crear(new Pasajero("30123456", "Otra", "Persona", "6061260000000008", 0)));
        intentar("legajo duplicado al guardar en SQLite",
                () -> personaDAO.crear(new Inspector("32555666", "Pedro", "Soto", 1001, "Sur")));
        intentar("eliminar un vehículo inexistente (id 12345)", () -> vehiculoDAO.eliminar(12345));
        intentar("actualizar una entidad que nunca se guardó",
                () -> vehiculoDAO.actualizar(new Colectivo(700, "CC333DD", 20, false, false)));

        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("==============================================");
        System.out.println(" DEMO FINALIZADA");
        System.out.println("  Excepciones controladas (esperadas): " + excepcionesControladas);
        System.out.println("  Intentos que NO fallaron como se esperaba: " + erroresInesperados);
        System.out.println("  Base de datos de la demo: mendotran_demo.db");
        System.out.println("==============================================");
        if (erroresInesperados > 0) {
            throw new IllegalStateException("Fallaron " + erroresInesperados + " validaciones en la demo.");
        }
    }

    private void seccion(String titulo) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println(" " + titulo);
        System.out.println("----------------------------------------------");
    }

    private void intentar(String descripcion, Accion accion) {
        try {
            accion.ejecutar();
            erroresInesperados++;
            System.out.println("  ?? " + descripcion + " -> NO lanzó excepción (inesperado)");
        } catch (Exception e) {
            excepcionesControladas++;
            System.out.println("  OK " + descripcion);
            System.out.println("       capturada " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
