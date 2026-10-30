package mendotran.dao;

import mendotran.db.ConexionDB;
import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Colectivo;
import mendotran.modelo.Metrotranvia;
import mendotran.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehiculoDAO implements CrudDAO<Vehiculo> {

    private static final String COLUMNAS =
            "id, tipo, interno, patente, capacidad, en_servicio, tiene_rampa, cantidad_duplas";

    private Connection con() throws SQLException {
        return ConexionDB.getInstance().getConexion();
    }

    @Override
    public int crear(Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculos (tipo, interno, patente, capacidad, en_servicio, tiene_rampa, cantidad_duplas) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, v);
            ps.executeUpdate();
        }
        int id = SqlUtil.ultimoId(con());
        v.setId(id);
        return id;
    }

    @Override
    public List<Vehiculo> obtenerTodos() throws SQLException {
        List<Vehiculo> resultado = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM vehiculos ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public Optional<Vehiculo> obtenerPorId(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM vehiculos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public void actualizar(Vehiculo v) throws SQLException {
        SqlUtil.exigirGuardado(v.getId(), "vehículo");
        String sql = "UPDATE vehiculos SET tipo = ?, interno = ?, patente = ?, capacidad = ?, "
                + "en_servicio = ?, tiene_rampa = ?, cantidad_duplas = ? WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, v);
            ps.setInt(8, v.getId());
            SqlUtil.exigirFilas(ps.executeUpdate(), "vehículo", v.getId());
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM vehiculos WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "vehículo", id);
        }
    }

    /** Carga los 7 parámetros comunes a INSERT y UPDATE. */
    private void enlazar(PreparedStatement ps, Vehiculo v) throws SQLException {
        ps.setString(1, v.getTipo());
        ps.setInt(2, v.getInterno());
        ps.setString(3, v.getPatente());
        ps.setInt(4, v.getCapacidad());
        ps.setInt(5, v.isEnServicio() ? 1 : 0);
        ps.setNull(6, Types.INTEGER);
        ps.setNull(7, Types.INTEGER);
        if (v instanceof Colectivo c) {
            ps.setInt(6, c.isTieneRampa() ? 1 : 0);
        } else if (v instanceof Metrotranvia m) {
            ps.setInt(7, m.getCantidadDuplas());
        }
    }

    /** Convierte la fila actual en el Vehiculo concreto según la columna {@code tipo}. */
    static Vehiculo mapear(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            String tipo = rs.getString("tipo");
            int interno = rs.getInt("interno");
            String patente = rs.getString("patente");
            int capacidad = rs.getInt("capacidad");
            boolean enServicio = rs.getInt("en_servicio") == 1;
            if ("COLECTIVO".equals(tipo)) {
                return new Colectivo(id, interno, patente, capacidad, enServicio, rs.getInt("tiene_rampa") == 1);
            }
            if ("METROTRANVIA".equals(tipo)) {
                return new Metrotranvia(id, interno, patente, capacidad, enServicio, rs.getInt("cantidad_duplas"));
            }
            throw new SQLException("Tipo de vehículo desconocido en la base: " + tipo);
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de vehículo inválido en la base: " + e.getMessage(), e);
        }
    }
}
