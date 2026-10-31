package mendotran.dao;

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
import java.util.Objects;
import java.util.Optional;

/**
 * DAO de líneas. Persiste la línea y sus relaciones (paradas ordenadas y vehículos)
 * dentro de una transacción sobre la conexión activa.
 */
public class LineaDAO implements CrudDAO<Linea> {

    private final Connection conexion;
    private final ParadaDAO paradaDAO;

    public LineaDAO(Connection conexion) {
        this.conexion = Objects.requireNonNull(conexion, "La conexión no puede ser nula.");
        this.paradaDAO = new ParadaDAO(conexion);
    }

    @Override
    public int crear(Linea linea) throws SQLException {
        boolean autoCommitPrevio = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            int id;
            try (PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO lineas (codigo, nombre) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.executeUpdate();
                id = SqlUtil.obtenerIdGenerado(ps);
            }
            linea.setId(id);
            guardarRelaciones(linea);
            conexion.commit();
            return id;
        } catch (SQLException e) {
            conexion.rollback();
            linea.setId(0);
            throw e;
        } finally {
            conexion.setAutoCommit(autoCommitPrevio);
        }
    }

    @Override
    public List<Linea> obtenerTodos() throws SQLException {
        List<Linea> lineas = new ArrayList<>();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, codigo, nombre FROM lineas ORDER BY codigo")) {
            leerFilasBase(rs, lineas);
        }
        hidratarRelaciones(lineas);
        return lineas;
    }

    public Optional<Linea> obtenerPorId(int id) throws SQLException {
        List<Linea> lineas = new ArrayList<>(1);
        try (PreparedStatement ps = conexion.prepareStatement("SELECT id, codigo, nombre FROM lineas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                leerFilasBase(rs, lineas);
            }
        }
        if (lineas.isEmpty()) {
            return Optional.empty();
        }
        hidratarRelaciones(lineas);
        return Optional.of(lineas.get(0));
    }

    /** Busca líneas mediante SQL por código o nombre. */
    public List<Linea> buscarPorTexto(String texto) throws SQLException {
        if (texto == null || texto.isBlank()) {
            return obtenerTodos();
        }
        String sql = "SELECT id, codigo, nombre FROM lineas "
                + "WHERE UPPER(codigo) LIKE ? OR LOWER(nombre) LIKE ? ORDER BY codigo";
        List<Linea> lineas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, "%" + texto.trim().toUpperCase() + "%");
            ps.setString(2, "%" + texto.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                leerFilasBase(rs, lineas);
            }
        }
        hidratarRelaciones(lineas);
        return lineas;
    }

    @Override
    public void actualizar(Linea linea) throws SQLException {
        SqlUtil.exigirGuardado(linea.getId(), "línea");
        boolean autoCommitPrevio = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conexion.prepareStatement(
                    "UPDATE lineas SET codigo = ?, nombre = ? WHERE id = ?")) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.setInt(3, linea.getId());
                SqlUtil.exigirFilas(ps.executeUpdate(), "línea", linea.getId());
            }
            borrarRelaciones(linea.getId());
            guardarRelaciones(linea);
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(autoCommitPrevio);
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM lineas WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "línea", id);
        }
    }

    private void borrarRelaciones(int lineaId) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM linea_parada WHERE linea_id = ?")) {
            ps.setInt(1, lineaId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM vehiculo_linea WHERE linea_id = ?")) {
            ps.setInt(1, lineaId);
            ps.executeUpdate();
        }
    }

    private void guardarRelaciones(Linea linea) throws SQLException {
        List<Parada> paradas = linea.getRecorrido().getParadas();
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO linea_parada (linea_id, parada_id, orden) VALUES (?, ?, ?)")) {
            for (int orden = 0; orden < paradas.size(); orden++) {
                Parada p = paradas.get(orden);
                if (p.getId() == 0) {
                    paradaDAO.crear(p);
                }
                ps.setInt(1, linea.getId());
                ps.setInt(2, p.getId());
                ps.setInt(3, orden);
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO vehiculo_linea (vehiculo_id, linea_id) VALUES (?, ?)")) {
            for (Vehiculo v : linea.getVehiculos()) {
                SqlUtil.exigirGuardado(v.getId(), "vehículo (interno " + v.getInterno() + ")");
                ps.setInt(1, v.getId());
                ps.setInt(2, linea.getId());
                ps.executeUpdate();
            }
        }
    }

    private void leerFilasBase(ResultSet rs, List<Linea> destino) throws SQLException {
        try {
            while (rs.next()) {
                destino.add(new Linea(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre")));
            }
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de línea inválido en la base: " + e.getMessage(), e);
        }
    }

    private void hidratarRelaciones(List<Linea> lineas) throws SQLException {
        try {
            for (Linea linea : lineas) {
                cargarParadas(linea);
                cargarVehiculos(linea);
            }
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de línea inválido en la base: " + e.getMessage(), e);
        }
    }

    private void cargarParadas(Linea linea) throws SQLException, EntradaInvalidaException {
        String sql = "SELECT p.id, p.numero, p.codigo, p.direccion, p.tiene_garita "
                + "FROM paradas p JOIN linea_parada lp ON lp.parada_id = p.id "
                + "WHERE lp.linea_id = ? ORDER BY lp.orden";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, linea.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linea.agregarParada(ParadaDAO.mapear(rs));
                }
            }
        }
    }

    private void cargarVehiculos(Linea linea) throws SQLException {
        String sql = "SELECT v.id, v.tipo, v.interno, v.patente, v.capacidad, v.en_servicio, "
                + "v.tiene_rampa, v.cantidad_duplas "
                + "FROM vehiculos v JOIN vehiculo_linea vl ON vl.vehiculo_id = v.id "
                + "WHERE vl.linea_id = ? ORDER BY v.interno";
        List<Vehiculo> vehiculos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, linea.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehiculos.add(VehiculoDAO.mapear(rs));
                }
            }
        }
        for (Vehiculo v : vehiculos) {
            linea.agregarVehiculo(v);
            VehiculoDAO.cargarLineas(conexion, v);
        }
    }
}
