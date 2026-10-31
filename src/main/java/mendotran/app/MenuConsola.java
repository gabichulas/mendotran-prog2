package mendotran.app;

import mendotran.dao.LineaDAO;
import mendotran.dao.ParadaDAO;
import mendotran.dao.PersonaDAO;
import mendotran.dao.VehiculoDAO;
import mendotran.excepciones.EntradaInvalidaException;
import mendotran.excepciones.PosicionInvalidaException;
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
import mendotran.modelo.Vehiculo;
import mendotran.util.Formato;
import mendotran.util.Validador;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Menú interactivo por consola. Ningún error de entrada interrumpe el programa. */
public class MenuConsola {

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private final Scanner in;
    private final PersonaDAO personaDAO;
    private final VehiculoDAO vehiculoDAO;
    private final ParadaDAO paradaDAO;
    private final LineaDAO lineaDAO;

    public MenuConsola(Scanner in, Connection conexion) {
        this.in = in;
        this.personaDAO = new PersonaDAO(conexion);
        this.vehiculoDAO = new VehiculoDAO(conexion);
        this.paradaDAO = new ParadaDAO(conexion);
        this.lineaDAO = new LineaDAO(conexion);
    }

    public void iniciar() {
        System.out.println("=== Sistema de Gestión Mendotran ===");
        while (true) {
            System.out.println();
            System.out.println("--- MENÚ PRINCIPAL ---");
            System.out.println("  1) Pasajeros");
            System.out.println("  2) Vehículos (colectivos y metrotranvía)");
            System.out.println("  3) Paradas");
            System.out.println("  4) Personal (choferes e inspectores)");
            System.out.println("  5) Líneas y recorridos");
            System.out.println("  6) Inspección (polimorfismo)");
            System.out.println("  0) Salir");
            String op = leer("Opción");
            if (op == null) {
                System.out.println();
                return;
            }
            switch (op.trim()) {
                case "1" -> menuPasajeros();
                case "2" -> menuVehiculos();
                case "3" -> menuParadas();
                case "4" -> menuPersonal();
                case "5" -> menuLineas();
                case "6" -> menuInspeccion();
                case "0" -> {
                    System.out.println("¡Hasta luego!");
                    return;
                }
                default -> System.out.println("  ✘ Opción inválida: '" + op.trim() + "'.");
            }
        }
    }

    // =====================================================================
    // Infraestructura de menús
    // =====================================================================

    private void submenu(String titulo, String[] etiquetas, Accion[] acciones) {
        while (true) {
            System.out.println();
            System.out.println("--- " + titulo + " ---");
            for (int i = 0; i < etiquetas.length; i++) {
                System.out.println("  " + (i + 1) + ") " + etiquetas[i]);
            }
            System.out.println("  0) Volver");
            String op = leer("Opción");
            if (op == null || op.trim().equals("0")) {
                return;
            }
            try {
                int n = Validador.parsearEntero(op, "opción");
                if (n < 1 || n > acciones.length) {
                    throw new EntradaInvalidaException("La opción " + n + " no existe (use 0 a " + acciones.length + ").");
                }
                acciones[n - 1].ejecutar();
            } catch (EntradaInvalidaException | PosicionInvalidaException e) {
                System.out.println("  ✘ " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("  ✘ Error de base de datos: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  ✘ Error inesperado (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
    }

    private String leer(String prompt) {
        System.out.print(prompt + ": ");
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private String pedir(String prompt) throws EntradaInvalidaException {
        String s = leer(prompt);
        if (s == null) {
            throw new EntradaInvalidaException("Se cerró la entrada de datos.");
        }
        return s;
    }

    private int pedirEntero(String prompt) throws EntradaInvalidaException {
        return Validador.parsearEntero(pedir(prompt), prompt);
    }

    private double pedirDecimal(String prompt) throws EntradaInvalidaException {
        return Validador.parsearDecimal(pedir(prompt), prompt);
    }

    private boolean pedirSiNo(String prompt) throws EntradaInvalidaException {
        return Validador.parsearSiNo(pedir(prompt + " (s/n)"), prompt);
    }

    private Boolean pedirFiltroSiNo(String prompt) throws EntradaInvalidaException {
        String texto = pedir(prompt + " (s/n, vacío = todos)");
        return texto.isBlank() ? null : Validador.parsearSiNo(texto, prompt);
    }

    private List<Integer> pedirIds(String prompt) throws EntradaInvalidaException {
        String texto = pedir(prompt);
        List<Integer> ids = new ArrayList<>();
        if (texto.isBlank()) {
            return ids;
        }
        for (String parte : texto.split(",")) {
            ids.add(Validador.parsearEntero(parte, "id"));
        }
        return ids;
    }

    private static <T> void imprimirLista(List<T> lista, String vacio) {
        if (lista.isEmpty()) {
            System.out.println("  (" + vacio + ")");
        }
        for (T elemento : lista) {
            System.out.println("  " + elemento);
        }
    }

    // =====================================================================
    // Búsquedas auxiliares por ID
    // =====================================================================

    private Persona buscarPersona(int id) throws SQLException, EntradaInvalidaException {
        return personaDAO.obtenerPorId(id)
                .orElseThrow(() -> new EntradaInvalidaException("No existe una persona con id " + id + "."));
    }

    private Pasajero buscarPasajero(int id) throws SQLException, EntradaInvalidaException {
        Persona p = buscarPersona(id);
        if (!(p instanceof Pasajero pasajero)) {
            throw new EntradaInvalidaException("La persona con id " + id + " no es un pasajero.");
        }
        return pasajero;
    }

    private Vehiculo buscarVehiculo(int id) throws SQLException, EntradaInvalidaException {
        return vehiculoDAO.obtenerPorId(id)
                .orElseThrow(() -> new EntradaInvalidaException("No existe un vehículo con id " + id + "."));
    }

    private Parada buscarParada(int id) throws SQLException, EntradaInvalidaException {
        return paradaDAO.obtenerPorId(id)
                .orElseThrow(() -> new EntradaInvalidaException("No existe una parada con id " + id + "."));
    }

    private Linea buscarLinea(int id) throws SQLException, EntradaInvalidaException {
        return lineaDAO.obtenerPorId(id)
                .orElseThrow(() -> new EntradaInvalidaException("No existe una línea con id " + id + "."));
    }

    // =====================================================================
    // 1) Pasajeros
    // =====================================================================

    private void menuPasajeros() {
        submenu("PASAJEROS",
                new String[]{
                        "Listar pasajeros",
                        "Buscar pasajero por DNI o nombre (SQL)",
                        "Registrar pasajero",
                        "Modificar pasajero",
                        "Cargar saldo SUBE",
                        "Pagar boleto",
                        "Eliminar pasajero"
                },
                new Accion[]{
                        () -> imprimirLista(personaDAO.obtenerPorClase(Pasajero.class), "no hay pasajeros"),
                        this::filtrarPasajeros,
                        this::crearPasajero,
                        this::modificarPasajero,
                        this::cargarSaldo,
                        this::pagarBoleto,
                        () -> eliminarPersona("pasajero")
                });
    }

    private void filtrarPasajeros() throws Exception {
        String texto = pedir("DNI, nombre o apellido a buscar");
        imprimirLista(personaDAO.buscar(texto, "PASAJERO"), "sin coincidencias");
    }

    private void crearPasajero() throws Exception {
        String dni = pedir("DNI (7-8 dígitos)");
        String nombre = pedir("Nombre");
        String apellido = pedir("Apellido");
        String sube = pedir("Número SUBE (16 dígitos)");
        double saldo = pedirDecimal("Saldo inicial");
        Pasajero p = new Pasajero(dni, nombre, apellido, sube, saldo);
        int id = personaDAO.crear(p);
        System.out.println("  ✔ Pasajero registrado con id " + id + ".");
    }

    private void modificarPasajero() throws Exception {
        Pasajero p = buscarPasajero(pedirEntero("ID del pasajero"));
        String nombre = pedir("Nuevo nombre (vacío = mantener '" + p.getNombre() + "')");
        if (!nombre.isBlank()) {
            p.setNombre(nombre);
        }
        String apellido = pedir("Nuevo apellido (vacío = mantener '" + p.getApellido() + "')");
        if (!apellido.isBlank()) {
            p.setApellido(apellido);
        }
        String sube = pedir("Nueva SUBE (vacío = mantener '" + p.getNumeroSube() + "')");
        if (!sube.isBlank()) {
            p.setNumeroSube(sube);
        }
        personaDAO.actualizar(p);
        System.out.println("  ✔ Pasajero actualizado: " + p);
    }

    private void cargarSaldo() throws Exception {
        Pasajero p = buscarPasajero(pedirEntero("ID del pasajero"));
        p.cargarSaldo(pedirDecimal("Monto a cargar"));
        personaDAO.actualizar(p);
        System.out.println("  ✔ " + p);
    }

    private void pagarBoleto() throws Exception {
        Pasajero p = buscarPasajero(pedirEntero("ID del pasajero"));
        String txtTarifa = pedir("Tarifa (vacío = general " + Formato.pesos(Pasajero.TARIFA_BOLETO) + ")");
        boolean pagado = txtTarifa.isBlank()
                ? p.pagarBoleto()
                : p.pagarBoleto(Validador.parsearDecimal(txtTarifa, "tarifa"));
        personaDAO.actualizar(p);
        if (pagado) {
            System.out.println("  ✔ Boleto abonado. " + p);
        } else {
            System.out.println("  ✘ Saldo insuficiente para abonar el boleto. " + p);
        }
    }

    private void eliminarPersona(String que) throws Exception {
        int id = pedirEntero("ID del " + que + " a eliminar");
        personaDAO.eliminar(id);
        System.out.println("  ✔ Eliminado.");
    }

    // =====================================================================
    // 2) Vehículos
    // =====================================================================

    private void menuVehiculos() {
        submenu("VEHÍCULOS",
                new String[]{
                        "Listar vehículos",
                        "Buscar / filtrar vehículos (SQL)",
                        "Registrar colectivo",
                        "Registrar metrotranvía",
                        "Modificar vehículo",
                        "Poner / dejar fuera de servicio",
                        "Eliminar vehículo"
                },
                new Accion[]{
                        () -> imprimirLista(vehiculoDAO.obtenerTodos(), "no hay vehículos"),
                        this::filtrarVehiculos,
                        this::crearColectivo,
                        this::crearMetrotranvia,
                        this::modificarVehiculo,
                        this::cambiarServicio,
                        () -> {
                            vehiculoDAO.eliminar(pedirEntero("ID del vehículo a eliminar"));
                            System.out.println("  ✔ Eliminado.");
                        }
                });
    }

    private void filtrarVehiculos() throws Exception {
        String texto = pedir("Patente o interno (vacío = cualquiera)");
        String opTipo = pedir("Tipo: 1) Colectivo  2) Metrotranvía  (vacío = ambos)").trim();
        String tipo = switch (opTipo) {
            case "" -> null;
            case "1" -> "COLECTIVO";
            case "2" -> "METROTRANVIA";
            default -> throw new EntradaInvalidaException("Tipo inválido '" + opTipo + "'.");
        };
        Boolean enServicio = pedirFiltroSiNo("¿En servicio?");
        imprimirLista(vehiculoDAO.buscar(texto, tipo, enServicio), "sin vehículos que coincidan");
    }

    private void crearColectivo() throws Exception {
        int interno = pedirEntero("Número de interno");
        String patente = pedir("Patente (ABC123 o AB123CD)");
        int capacidad = pedirEntero("Capacidad");
        boolean rampa = pedirSiNo("¿Tiene rampa?");
        Colectivo c = new Colectivo(interno, patente, capacidad, false, rampa);
        int id = vehiculoDAO.crear(c);
        System.out.println("  ✔ Colectivo registrado con id " + id + ".");
    }

    private void crearMetrotranvia() throws Exception {
        int interno = pedirEntero("Número de interno");
        String patente = pedir("Código/patente (ABC123 o AB123CD)");
        int capacidad = pedirEntero("Capacidad");
        int duplas = pedirEntero("Cantidad de duplas");
        Metrotranvia m = new Metrotranvia(interno, patente, capacidad, false, duplas);
        int id = vehiculoDAO.crear(m);
        System.out.println("  ✔ Metrotranvía registrado con id " + id + ".");
    }

    private void modificarVehiculo() throws Exception {
        Vehiculo v = buscarVehiculo(pedirEntero("ID del vehículo"));
        String patente = pedir("Nueva patente (vacío = mantener '" + v.getPatente() + "')");
        if (!patente.isBlank()) {
            v.setPatente(patente);
        }
        String cap = pedir("Nueva capacidad (vacío = mantener " + v.getCapacidad() + ")");
        if (!cap.isBlank()) {
            v.setCapacidad(Validador.parsearEntero(cap, "capacidad"));
        }
        if (v instanceof Colectivo c) {
            Boolean rampa = pedirFiltroSiNo("¿Tiene rampa?");
            if (rampa != null) {
                c.setTieneRampa(rampa);
            }
        } else if (v instanceof Metrotranvia m) {
            String duplas = pedir("Cantidad de duplas (vacío = mantener " + m.getCantidadDuplas() + ")");
            if (!duplas.isBlank()) {
                m.setCantidadDuplas(Validador.parsearEntero(duplas, "cantidad de duplas"));
            }
        }
        vehiculoDAO.actualizar(v);
        System.out.println("  ✔ Vehículo actualizado: " + v);
    }

    private void cambiarServicio() throws Exception {
        Vehiculo v = buscarVehiculo(pedirEntero("ID del vehículo"));
        if (pedirSiNo("¿Poner en servicio? (n = fuera de servicio)")) {
            String motivo = pedir("Motivo de alta en servicio (vacío = sin motivo)");
            if (motivo.isBlank()) {
                v.ponerEnServicio();
            } else {
                v.ponerEnServicio(motivo);
            }
        } else {
            v.fueraDeServicio();
        }
        vehiculoDAO.actualizar(v);
        System.out.println("  ✔ " + v);
    }

    // =====================================================================
    // 3) Paradas
    // =====================================================================

    private void menuParadas() {
        submenu("PARADAS",
                new String[]{
                        "Listar paradas",
                        "Buscar / filtrar paradas (SQL)",
                        "Registrar parada",
                        "Modificar parada",
                        "Eliminar parada"
                },
                new Accion[]{
                        () -> imprimirLista(paradaDAO.obtenerTodos(), "no hay paradas"),
                        this::filtrarParadas,
                        this::crearParada,
                        this::modificarParada,
                        () -> {
                            paradaDAO.eliminar(pedirEntero("ID de la parada a eliminar"));
                            System.out.println("  ✔ Eliminada.");
                        }
                });
    }

    private void filtrarParadas() throws Exception {
        String texto = pedir("Código o dirección (vacío = cualquiera)");
        Boolean garita = pedirFiltroSiNo("¿Con garita?");
        imprimirLista(paradaDAO.buscar(texto, garita), "sin paradas que coincidan");
    }

    private void crearParada() throws Exception {
        int numero = pedirEntero("Número de parada (1 a 9999)");
        String codigoSugerido = Parada.generarCodigo(numero);
        String codigo = pedir("Código (vacío = autogenerar " + codigoSugerido + ")");
        String direccion = pedir("Dirección");
        boolean garita = pedirSiNo("¿Tiene garita?");
        Parada p = codigo.isBlank()
                ? new Parada(numero, direccion, garita)
                : new Parada(0, numero, codigo, direccion, garita);
        int id = paradaDAO.crear(p);
        System.out.println("  ✔ Parada registrada con id " + id + ": " + p.resumen());
    }

    private void modificarParada() throws Exception {
        Parada p = buscarParada(pedirEntero("ID de la parada"));
        String direccion = pedir("Nueva dirección (vacío = mantener '" + p.getDireccion() + "')");
        if (!direccion.isBlank()) {
            p.setDireccion(direccion);
        }
        Boolean garita = pedirFiltroSiNo("¿Tiene garita?");
        if (garita != null) {
            p.setTieneGarita(garita);
        }
        paradaDAO.actualizar(p);
        System.out.println("  ✔ Parada actualizada: " + p);
    }

    // =====================================================================
    // 4) Personal
    // =====================================================================

    private void menuPersonal() {
        submenu("PERSONAL",
                new String[]{
                        "Listar empleados",
                        "Buscar / filtrar personal (SQL)",
                        "Registrar chofer",
                        "Registrar inspector",
                        "Asignar o desasignar vehículo a chofer",
                        "Modificar zona de inspector",
                        "Eliminar empleado"
                },
                new Accion[]{
                        this::listarEmpleados,
                        this::filtrarPersonal,
                        this::crearChofer,
                        this::crearInspector,
                        this::gestionarVehiculoChofer,
                        this::modificarInspector,
                        () -> eliminarPersona("empleado")
                });
    }

    private void listarEmpleados() throws SQLException {
        List<Empleado> empleados = personaDAO.obtenerPorClase(Empleado.class);
        if (empleados.isEmpty()) {
            System.out.println("  (no hay empleados)");
        }
        for (Empleado e : empleados) {
            System.out.println("  #" + e.getId() + " " + e.getNombreCompleto() + " [legajo " + e.getLegajo()
                    + "] -> " + e.mostrarRol());
        }
    }

    private void filtrarPersonal() throws Exception {
        String texto = pedir("DNI, nombre o apellido (vacío = cualquiera)");
        String opRol = pedir("Rol: 1) Chofer  2) Inspector  (vacío = ambos)").trim();
        String tipo = switch (opRol) {
            case "" -> "EMPLEADO";
            case "1" -> "CHOFER";
            case "2" -> "INSPECTOR";
            default -> throw new EntradaInvalidaException("Rol inválido '" + opRol + "'.");
        };
        imprimirLista(personaDAO.buscar(texto, tipo), "sin empleados que coincidan");
    }

    private void crearChofer() throws Exception {
        String dni = pedir("DNI (7-8 dígitos)");
        String nombre = pedir("Nombre");
        String apellido = pedir("Apellido");
        int legajo = pedirEntero("Legajo numérico");
        String licencia = pedir("Licencia");
        String idVehiculo = pedir("ID del vehículo asignado (vacío = ninguno)");
        Vehiculo v = idVehiculo.isBlank() ? null : buscarVehiculo(Validador.parsearEntero(idVehiculo, "id de vehículo"));
        Chofer c = new Chofer(dni, nombre, apellido, legajo, licencia, v);
        int id = personaDAO.crear(c);
        System.out.println("  ✔ Chofer registrado con id " + id + ".");
    }

    private void crearInspector() throws Exception {
        String dni = pedir("DNI (7-8 dígitos)");
        String nombre = pedir("Nombre");
        String apellido = pedir("Apellido");
        int legajo = pedirEntero("Legajo numérico");
        String zona = pedir("Zona");
        Inspector i = new Inspector(dni, nombre, apellido, legajo, zona);
        int id = personaDAO.crear(i);
        System.out.println("  ✔ Inspector registrado con id " + id + ".");
    }

    private void gestionarVehiculoChofer() throws Exception {
        Persona p = buscarPersona(pedirEntero("ID del chofer"));
        if (!(p instanceof Chofer chofer)) {
            throw new EntradaInvalidaException("La persona con id " + p.getId() + " no es un chofer.");
        }
        String idVehiculo = pedir("ID del vehículo a asignar (vacío = desasignar actual)");
        if (idVehiculo.isBlank()) {
            chofer.desasignarVehiculo();
        } else {
            chofer.asignarVehiculo(buscarVehiculo(Validador.parsearEntero(idVehiculo, "id de vehículo")));
        }
        personaDAO.actualizar(chofer);
        System.out.println("  ✔ " + chofer);
    }

    private void modificarInspector() throws Exception {
        Persona p = buscarPersona(pedirEntero("ID del inspector"));
        if (!(p instanceof Inspector inspector)) {
            throw new EntradaInvalidaException("La persona con id " + p.getId() + " no es un inspector.");
        }
        inspector.setZona(pedir("Nueva zona"));
        personaDAO.actualizar(inspector);
        System.out.println("  ✔ " + inspector);
    }

    // =====================================================================
    // 5) Líneas y recorridos
    // =====================================================================

    private void menuLineas() {
        submenu("LÍNEAS Y RECORRIDOS",
                new String[]{
                        "Listar líneas con su recorrido",
                        "Buscar línea por código o nombre (SQL)",
                        "Crear línea",
                        "Modificar línea (nombre / agregar parada o vehículo)",
                        "Ver recorrido de ida y vuelta",
                        "Consultar parada en posición",
                        "Quitar parada por código",
                        "Eliminar línea"
                },
                new Accion[]{
                        this::listarLineas,
                        this::filtrarLineas,
                        this::crearLinea,
                        this::modificarLinea,
                        this::verRecorrido,
                        this::consultarPosicion,
                        this::quitarParada,
                        () -> {
                            lineaDAO.eliminar(pedirEntero("ID de la línea a eliminar"));
                            System.out.println("  ✔ Eliminada.");
                        }
                });
    }

    private void listarLineas() throws SQLException {
        List<Linea> lineas = lineaDAO.obtenerTodos();
        if (lineas.isEmpty()) {
            System.out.println("  (no hay líneas)");
        }
        for (Linea l : lineas) {
            System.out.println("  " + l);
            l.getRecorrido().mostrarNormal();
        }
    }

    private void filtrarLineas() throws Exception {
        String texto = pedir("Código o nombre de línea");
        imprimirLista(lineaDAO.buscarPorTexto(texto), "sin líneas que coincidan");
    }

    private void crearLinea() throws Exception {
        String codigo = pedir("Código de línea");
        String nombre = pedir("Nombre");
        List<Integer> idsParadas = pedirIds("IDs de paradas en orden, separados por coma");
        List<Integer> idsVehiculos = pedirIds("IDs de vehículos, separados por coma (vacío = ninguno)");
        Linea linea = new Linea(codigo, nombre);
        List<Parada> paradas = new ArrayList<>();
        for (int id : idsParadas) {
            paradas.add(buscarParada(id));
        }
        linea.agregarParada(paradas);
        for (int id : idsVehiculos) {
            linea.agregarVehiculo(buscarVehiculo(id));
        }
        int id = lineaDAO.crear(linea);
        System.out.println("  ✔ Línea creada con id " + id + ".");
    }

    private void modificarLinea() throws Exception {
        Linea l = buscarLinea(pedirEntero("ID de la línea"));
        String nombre = pedir("Nuevo nombre (vacío = mantener '" + l.getNombre() + "')");
        if (!nombre.isBlank()) {
            l.setNombre(nombre);
        }
        for (int idParada : pedirIds("IDs de paradas a agregar al final (vacío = ninguna)")) {
            l.agregarParada(buscarParada(idParada));
        }
        for (int idVehiculo : pedirIds("IDs de vehículos a asociar (vacío = ninguno)")) {
            l.agregarVehiculo(buscarVehiculo(idVehiculo));
        }
        lineaDAO.actualizar(l);
        System.out.println("  ✔ Línea actualizada: " + l);
    }

    private void verRecorrido() throws Exception {
        Linea l = buscarLinea(pedirEntero("ID de la línea"));
        System.out.println("  " + l);
        System.out.println("  Ida:");
        l.getRecorrido().mostrarNormal();
        System.out.println("  Vuelta:");
        l.getRecorrido().mostrarInvertido();
    }

    private void consultarPosicion() throws Exception {
        Linea l = buscarLinea(pedirEntero("ID de la línea"));
        int pos = pedirEntero("Posición (empieza en 0)");
        Parada p = l.obtenerParadaEn(pos);
        System.out.println("  ✔ En la posición " + pos + ": " + p.resumen());
    }

    private void quitarParada() throws Exception {
        Linea l = buscarLinea(pedirEntero("ID de la línea"));
        String codigo = pedir("Código de la parada a quitar (ej. M0003)");
        if (!l.quitarParada(codigo)) {
            throw new EntradaInvalidaException("La parada '" + codigo + "' no forma parte de la línea " + l.getCodigo() + ".");
        }
        lineaDAO.actualizar(l);
        System.out.println("  ✔ Se quitó " + codigo.trim().toUpperCase() + " del recorrido.");
    }

    // =====================================================================
    // 6) Inspección
    // =====================================================================

    private void menuInspeccion() {
        submenu("INSPECCIÓN",
                new String[]{"Auditar un pasajero, vehículo o parada"},
                new Accion[]{this::auditar});
    }

    private void auditar() throws Exception {
        Persona persona = buscarPersona(pedirEntero("ID del inspector"));
        if (!(persona instanceof Inspector inspector)) {
            throw new EntradaInvalidaException("La persona con id " + persona.getId() + " no es un inspector.");
        }
        System.out.println("  Qué se audita: 1) Pasajero  2) Vehículo  3) Parada");
        int tipo = pedirEntero("Tipo");
        int id = pedirEntero("ID del objetivo");
        Inspeccionable objetivo = switch (tipo) {
            case 1 -> buscarPasajero(id);
            case 2 -> buscarVehiculo(id);
            case 3 -> buscarParada(id);
            default -> throw new EntradaInvalidaException("Tipo de objetivo inválido: " + tipo + " (use 1, 2 o 3).");
        };
        String obs = pedir("Observación (vacío = sin observaciones)");
        System.out.println(obs.isBlank() ? inspector.auditar(objetivo) : inspector.auditar(objetivo, obs));
    }
}
