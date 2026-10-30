package mendotran.dao;

import mendotran.db.ConexionDB;
import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Linea;
import mendotran.modelo.Parada;
import mendotran.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de líneas. Persiste la línea y sus relaciones (paradas ordenadas y vehículos)
 * dentro de una transacción. Las paradas nuevas (id 0) se crean automáticamente;
 * los vehículos deben haberse guardado antes.
 */
public class LineaDAO implements CrudDAO<Linea> {

    private final ParadaDAO paradaDAO = new ParadaDAO();

    private Connection con() throws SQLException {
        return ConexionDB.getInstance().getConexion();
    }

    @Override
    public int crear(Linea linea) throws SQLException {
        Connection c = con();
        boolean autoCommitPrevio = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO lineas (codigo, nombre) VALUES (?, ?)")) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.executeUpdate();
            }
            int id = SqlUtil.ultimoId(c);
            linea.setId(id);
            guardarRelaciones(c, linea);
            c.commit();
            return id;
        } catch (SQLException e) {
            c.rollback();
            linea.setId(0);
            throw e;
        } finally {
            c.setAutoCommit(autoCommitPrevio);
        }
    }

    @Override
    public List<Linea> obtenerTodos() throws SQLException {
        List<Linea> resultado = new ArrayList<>();
        try (Statement st = con().createStatement();
             ResultSet rs = st.executeQuery("SELECT id, codigo, nombre FROM lineas ORDER BY id")) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public Optional<Linea> obtenerPorId(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("SELECT id, codigo, nombre FROM lineas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public void actualizar(Linea linea) throws SQLException {
        SqlUtil.exigirGuardado(linea.getId(), "línea");
        Connection c = con();
        boolean autoCommitPrevio = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement("UPDATE lineas SET codigo = ?, nombre = ? WHERE id = ?")) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.setInt(3, linea.getId());
                SqlUtil.exigirFilas(ps.executeUpdate(), "línea", linea.getId());
            }
            borrarRelaciones(c, linea.getId());
            guardarRelaciones(c, linea);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(autoCommitPrevio);
        }
    }

    /** Elimina la línea; sus relaciones caen por ON DELETE CASCADE (las paradas y vehículos permanecen). */
    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM lineas WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "línea", id);
        }
    }

    // ---- Relaciones ----

    private void borrarRelaciones(Connection c, int lineaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM linea_parada WHERE linea_id = ?")) {
            ps.setInt(1, lineaId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM vehiculo_linea WHERE linea_id = ?")) {
            ps.setInt(1, lineaId);
            ps.executeUpdate();
        }
    }

    private void guardarRelaciones(Connection c, Linea linea) throws SQLException {
        List<Parada> paradas = linea.getRecorrido().getParadas();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO linea_parada (linea_id, parada_id, orden) VALUES (?, ?, ?)")) {
            for (int orden = 0; orden < paradas.size(); orden++) {
                Parada p = paradas.get(orden);
                if (p.getId() == 0) {
                    paradaDAO.crear(p); // misma conexión => misma transacción
                }
                ps.setInt(1, linea.getId());
                ps.setInt(2, p.getId());
                ps.setInt(3, orden);
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vehiculo_linea (vehiculo_id, linea_id) VALUES (?, ?)")) {
            for (Vehiculo v : linea.getVehiculos()) {
                SqlUtil.exigirGuardado(v.getId(), "vehículo (interno " + v.getInterno() + ")");
                ps.setInt(1, v.getId());
                ps.setInt(2, linea.getId());
                ps.executeUpdate();
            }
        }
    }

    private Linea mapear(ResultSet rs) throws SQLException {
        try {
            Linea linea = new Linea(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"));
            cargarParadas(linea);
            cargarVehiculos(linea);
            return linea;
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de línea inválido en la base: " + e.getMessage(), e);
        }
    }

    private void cargarParadas(Linea linea) throws SQLException, EntradaInvalidaException {
        String sql = "SELECT p.id, p.numero, p.codigo, p.direccion, p.tiene_garita "
                + "FROM paradas p JOIN linea_parada lp ON lp.parada_id = p.id "
                + "WHERE lp.linea_id = ? ORDER BY lp.orden";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, linea.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linea.getRecorrido().agregarParada(ParadaDAO.mapear(rs));
                }
            }
        }
    }

    private void cargarVehiculos(Linea linea) throws SQLException {
        String sql = "SELECT v.id, v.tipo, v.interno, v.patente, v.capacidad, v.en_servicio, "
                + "v.tiene_rampa, v.cantidad_duplas "
                + "FROM vehiculos v JOIN vehiculo_linea vl ON vl.vehiculo_id = v.id "
                + "WHERE vl.linea_id = ? ORDER BY v.id";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, linea.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linea.agregarVehiculo(VehiculoDAO.mapear(rs));
                }
            }
        }
    }
}
