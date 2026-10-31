package mendotran.dao;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Chofer;
import mendotran.modelo.Empleado;
import mendotran.modelo.Inspector;
import mendotran.modelo.Pasajero;
import mendotran.modelo.Persona;
import mendotran.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * DAO de personas con Single Table Inheritance: Pasajero, Chofer e Inspector
 * viven en la tabla {@code personas}, diferenciados por la columna {@code tipo}.
 */
public class PersonaDAO implements CrudDAO<Persona> {

    private static final String SELECT_BASE =
            "SELECT p.id, p.tipo, p.dni, p.nombre, p.apellido, p.numero_sube, p.saldo_sube, p.boleto_pago, "
                    + "p.legajo, p.licencia, p.vehiculo_id, p.zona, "
                    + "v.id AS v_id, v.tipo AS v_tipo, v.interno AS v_interno, v.patente AS v_patente, "
                    + "v.capacidad AS v_capacidad, v.en_servicio AS v_en_servicio, "
                    + "v.tiene_rampa AS v_tiene_rampa, v.cantidad_duplas AS v_cantidad_duplas "
                    + "FROM personas p "
                    + "LEFT JOIN vehiculos v ON v.id = p.vehiculo_id";

    private final Connection conexion;

    public PersonaDAO(Connection conexion) {
        this.conexion = Objects.requireNonNull(conexion, "La conexión no puede ser nula.");
    }

    @Override
    public int crear(Persona p) throws SQLException {
        String sql = "INSERT INTO personas (tipo, dni, nombre, apellido, numero_sube, saldo_sube, boleto_pago, "
                + "legajo, licencia, vehiculo_id, zona) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            enlazar(ps, p);
            ps.executeUpdate();
            int id = SqlUtil.obtenerIdGenerado(ps);
            p.setId(id);
            return id;
        }
    }

    @Override
    public List<Persona> obtenerTodos() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY p.id";
        List<Persona> resultado = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapearFila(rs));
            }
        }
        hidratarLineasVehiculos(resultado);
        return resultado;
    }

    /** Filtra directamente mediante SQL según el subtipo de {@link Persona} solicitado. */
    public <T extends Persona> List<T> obtenerPorClase(Class<T> clase) throws SQLException {
        String condicion;
        String parametroTipo = null;

        if (clase == Pasajero.class) {
            condicion = " WHERE p.tipo = ?";
            parametroTipo = "PASAJERO";
        } else if (clase == Chofer.class) {
            condicion = " WHERE p.tipo = ?";
            parametroTipo = "CHOFER";
        } else if (clase == Inspector.class) {
            condicion = " WHERE p.tipo = ?";
            parametroTipo = "INSPECTOR";
        } else if (clase == Empleado.class) {
            condicion = " WHERE p.tipo IN ('CHOFER', 'INSPECTOR')";
        } else if (clase == Persona.class) {
            condicion = "";
        } else {
            throw new SQLException("Clase de persona no soportada: " + clase.getSimpleName());
        }

        String sql = SELECT_BASE + condicion + " ORDER BY p.id";
        List<Persona> personas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (parametroTipo != null) {
                ps.setString(1, parametroTipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    personas.add(mapearFila(rs));
                }
            }
        }
        hidratarLineasVehiculos(personas);

        List<T> resultado = new ArrayList<>(personas.size());
        for (Persona p : personas) {
            resultado.add(clase.cast(p));
        }
        return resultado;
    }

    /** Busca personas en SQL por DNI, nombre o apellido, opcionalmente acotado por tipo/rol. */
    public List<Persona> buscar(String texto, String filtroTipo) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (p.dni LIKE ? OR LOWER(p.nombre) LIKE ? OR LOWER(p.apellido) LIKE ?)");
            String patron = "%" + texto.trim().toLowerCase() + "%";
            params.add("%" + texto.trim() + "%");
            params.add(patron);
            params.add(patron);
        }
        if (filtroTipo != null && !filtroTipo.isBlank()) {
            String t = filtroTipo.trim().toUpperCase();
            if ("EMPLEADO".equals(t)) {
                sql.append(" AND p.tipo IN ('CHOFER', 'INSPECTOR')");
            } else {
                sql.append(" AND p.tipo = ?");
                params.add(t);
            }
        }
        sql.append(" ORDER BY p.id");

        List<Persona> resultado = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearFila(rs));
                }
            }
        }
        hidratarLineasVehiculos(resultado);
        return resultado;
    }

    public Optional<Persona> obtenerPorId(int id) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.id = ?";
        List<Persona> lista = new ArrayList<>(1);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lista.add(mapearFila(rs));
                }
            }
        }
        if (lista.isEmpty()) {
            return Optional.empty();
        }
        hidratarLineasVehiculos(lista);
        return Optional.of(lista.get(0));
    }

    @Override
    public void actualizar(Persona p) throws SQLException {
        SqlUtil.exigirGuardado(p.getId(), "persona");
        String sql = "UPDATE personas SET tipo = ?, dni = ?, nombre = ?, apellido = ?, numero_sube = ?, "
                + "saldo_sube = ?, boleto_pago = ?, legajo = ?, licencia = ?, vehiculo_id = ?, zona = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            enlazar(ps, p);
            ps.setInt(12, p.getId());
            SqlUtil.exigirFilas(ps.executeUpdate(), "persona", p.getId());
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM personas WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "persona", id);
        }
    }

    private void enlazar(PreparedStatement ps, Persona p) throws SQLException {
        ps.setString(2, p.getDni());
        ps.setString(3, p.getNombre());
        ps.setString(4, p.getApellido());
        ps.setNull(5, Types.VARCHAR);   // numero_sube
        ps.setNull(6, Types.REAL);      // saldo_sube
        ps.setNull(7, Types.INTEGER);   // boleto_pago
        ps.setNull(8, Types.INTEGER);   // legajo
        ps.setNull(9, Types.VARCHAR);   // licencia
        ps.setNull(10, Types.INTEGER);  // vehiculo_id
        ps.setNull(11, Types.VARCHAR);  // zona

        if (p instanceof Empleado emp) {
            ps.setInt(8, emp.getLegajo());
        }

        if (p instanceof Pasajero pas) {
            ps.setString(1, "PASAJERO");
            ps.setString(5, pas.getNumeroSube());
            ps.setDouble(6, pas.getSaldoSube());
            ps.setInt(7, pas.isBoletoPago() ? 1 : 0);
        } else if (p instanceof Chofer ch) {
            ps.setString(1, "CHOFER");
            ps.setString(9, ch.getLicencia());
            Vehiculo v = ch.getVehiculoAsignado();
            if (v != null) {
                SqlUtil.exigirGuardado(v.getId(), "vehículo asignado");
                ps.setInt(10, v.getId());
            }
        } else if (p instanceof Inspector in) {
            ps.setString(1, "INSPECTOR");
            ps.setString(11, in.getZona());
        } else {
            throw new SQLException("Tipo de persona no soportado: " + p.getClass().getSimpleName());
        }
    }

    private void hidratarLineasVehiculos(List<Persona> personas) throws SQLException {
        for (Persona p : personas) {
            if (p instanceof Chofer ch && ch.getVehiculoAsignado() != null) {
                VehiculoDAO.cargarLineas(conexion, ch.getVehiculoAsignado());
            }
        }
    }

    private Persona mapearFila(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            String tipo = rs.getString("tipo");
            String dni = rs.getString("dni");
            String nombre = rs.getString("nombre");
            String apellido = rs.getString("apellido");

            if ("PASAJERO".equals(tipo)) {
                return new Pasajero(id, dni, nombre, apellido, rs.getString("numero_sube"),
                        rs.getDouble("saldo_sube"), rs.getInt("boleto_pago") == 1);
            }
            if ("CHOFER".equals(tipo)) {
                rs.getInt("v_id");
                Vehiculo vehiculo = rs.wasNull() ? null : VehiculoDAO.mapearConPrefijo(rs, "v_");
                return new Chofer(id, dni, nombre, apellido, rs.getInt("legajo"),
                        rs.getString("licencia"), vehiculo);
            }
            if ("INSPECTOR".equals(tipo)) {
                return new Inspector(id, dni, nombre, apellido, rs.getInt("legajo"), rs.getString("zona"));
            }
            throw new SQLException("Tipo de persona desconocido en la base: " + tipo);
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de persona inválido en la base: " + e.getMessage(), e);
        }
    }
}
