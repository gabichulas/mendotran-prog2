package mendotran.dao;

import mendotran.db.ConexionDB;
import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Chofer;
import mendotran.modelo.Inspector;
import mendotran.modelo.Pasajero;
import mendotran.modelo.Persona;
import mendotran.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de personas con Single Table Inheritance: Pasajero, Chofer e Inspector
 * viven en la tabla {@code personas}, diferenciados por la columna {@code tipo}.
 */
public class PersonaDAO implements CrudDAO<Persona> {

    private static final String COLUMNAS =
            "id, tipo, dni, nombre, apellido, numero_sube, saldo_sube, boleto_pago, legajo, licencia, vehiculo_id, zona";

    private final VehiculoDAO vehiculoDAO = new VehiculoDAO();

    private Connection con() throws SQLException {
        return ConexionDB.getInstance().getConexion();
    }

    @Override
    public int crear(Persona p) throws SQLException {
        String sql = "INSERT INTO personas (tipo, dni, nombre, apellido, numero_sube, saldo_sube, boleto_pago, "
                + "legajo, licencia, vehiculo_id, zona) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, p);
            ps.executeUpdate();
        }
        int id = SqlUtil.ultimoId(con());
        p.setId(id);
        return id;
    }

    @Override
    public List<Persona> obtenerTodos() throws SQLException {
        List<Persona> resultado = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM personas ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    /** Devuelve solo las personas de una clase (Pasajero.class, Empleado.class, Chofer.class...). */
    public <T extends Persona> List<T> obtenerPorClase(Class<T> clase) throws SQLException {
        List<T> resultado = new ArrayList<>();
        for (Persona p : obtenerTodos()) {
            if (clase.isInstance(p)) {
                resultado.add(clase.cast(p));
            }
        }
        return resultado;
    }

    public Optional<Persona> obtenerPorId(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM personas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public void actualizar(Persona p) throws SQLException {
        SqlUtil.exigirGuardado(p.getId(), "persona");
        String sql = "UPDATE personas SET tipo = ?, dni = ?, nombre = ?, apellido = ?, numero_sube = ?, "
                + "saldo_sube = ?, boleto_pago = ?, legajo = ?, licencia = ?, vehiculo_id = ?, zona = ? WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, p);
            ps.setInt(12, p.getId());
            SqlUtil.exigirFilas(ps.executeUpdate(), "persona", p.getId());
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM personas WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "persona", id);
        }
    }

    /** Carga los 11 parámetros comunes a INSERT y UPDATE. */
    private void enlazar(PreparedStatement ps, Persona p) throws SQLException {
        ps.setString(2, p.getDni());
        ps.setString(3, p.getNombre());
        ps.setString(4, p.getApellido());
        ps.setNull(5, Types.VARCHAR);   // numero_sube
        ps.setNull(6, Types.REAL);      // saldo_sube
        ps.setNull(7, Types.INTEGER);   // boleto_pago
        ps.setNull(8, Types.VARCHAR);   // legajo
        ps.setNull(9, Types.VARCHAR);   // licencia
        ps.setNull(10, Types.INTEGER);  // vehiculo_id
        ps.setNull(11, Types.VARCHAR);  // zona

        if (p instanceof Pasajero pas) {
            ps.setString(1, "PASAJERO");
            ps.setString(5, pas.getNumeroSube());
            ps.setDouble(6, pas.getSaldoSube());
            ps.setInt(7, pas.isBoletoPago() ? 1 : 0);
        } else if (p instanceof Chofer ch) {
            ps.setString(1, "CHOFER");
            ps.setString(8, ch.getLegajo());
            ps.setString(9, ch.getLicencia());
            Vehiculo v = ch.getVehiculoAsignado();
            if (v != null) {
                SqlUtil.exigirGuardado(v.getId(), "vehículo asignado");
                ps.setInt(10, v.getId());
            }
        } else if (p instanceof Inspector in) {
            ps.setString(1, "INSPECTOR");
            ps.setString(8, in.getLegajo());
            ps.setString(11, in.getZona());
        } else {
            throw new SQLException("Tipo de persona no soportado: " + p.getClass().getSimpleName());
        }
    }

    private Persona mapear(ResultSet rs) throws SQLException {
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
                int vehiculoId = rs.getInt("vehiculo_id");
                boolean sinVehiculo = rs.wasNull();
                Vehiculo vehiculo = sinVehiculo ? null : vehiculoDAO.obtenerPorId(vehiculoId).orElse(null);
                return new Chofer(id, dni, nombre, apellido, rs.getString("legajo"),
                        rs.getString("licencia"), vehiculo);
            }
            if ("INSPECTOR".equals(tipo)) {
                return new Inspector(id, dni, nombre, apellido, rs.getString("legajo"), rs.getString("zona"));
            }
            throw new SQLException("Tipo de persona desconocido en la base: " + tipo);
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de persona inválido en la base: " + e.getMessage(), e);
        }
    }
}
