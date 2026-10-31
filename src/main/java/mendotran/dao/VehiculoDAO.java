package mendotran.dao;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Colectivo;
import mendotran.modelo.Linea;
import mendotran.modelo.Metrotranvia;
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

public class VehiculoDAO implements CrudDAO<Vehiculo> {

    private static final String COLUMNAS =
            "id, tipo, interno, patente, capacidad, en_servicio, tiene_rampa, cantidad_duplas";

    private final Connection conexion;

    public VehiculoDAO(Connection conexion) {
        this.conexion = Objects.requireNonNull(conexion, "La conexión no puede ser nula.");
    }

    @Override
    public int crear(Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculos (tipo, interno, patente, capacidad, en_servicio, tiene_rampa, cantidad_duplas) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            enlazar(ps, v);
            ps.executeUpdate();
            int id = SqlUtil.obtenerIdGenerado(ps);
            v.setId(id);
            return id;
        }
    }

    @Override
    public List<Vehiculo> obtenerTodos() throws SQLException {
        List<Vehiculo> resultado = new ArrayList<>();
        String sql = "SELECT " + COLUMNAS + " FROM vehiculos ORDER BY interno";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        for (Vehiculo v : resultado) {
            cargarLineas(conexion, v);
        }
        return resultado;
    }

    public Optional<Vehiculo> obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM vehiculos WHERE id = ?";
        Vehiculo v;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                v = mapear(rs);
            }
        }
        cargarLineas(conexion, v);
        return Optional.of(v);
    }

    /** Busca y filtra vehículos con SQL por patente/interno, tipo y/o estado de servicio. */
    public List<Vehiculo> buscar(String texto, String tipo, Boolean enServicio) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNAS + " FROM vehiculos WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (UPPER(patente) LIKE ? OR CAST(interno AS TEXT) LIKE ?)");
            String patron = "%" + texto.trim().toUpperCase() + "%";
            params.add(patron);
            params.add(patron);
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND tipo = ?");
            params.add(tipo.trim().toUpperCase());
        }
        if (enServicio != null) {
            sql.append(" AND en_servicio = ?");
            params.add(enServicio ? 1 : 0);
        }
        sql.append(" ORDER BY interno");

        List<Vehiculo> resultado = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        for (Vehiculo v : resultado) {
            cargarLineas(conexion, v);
        }
        return resultado;
    }

    @Override
    public void actualizar(Vehiculo v) throws SQLException {
        SqlUtil.exigirGuardado(v.getId(), "vehículo");
        String sql = "UPDATE vehiculos SET tipo = ?, interno = ?, patente = ?, capacidad = ?, "
                + "en_servicio = ?, tiene_rampa = ?, cantidad_duplas = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            enlazar(ps, v);
            ps.setInt(8, v.getId());
            SqlUtil.exigirFilas(ps.executeUpdate(), "vehículo", v.getId());
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM vehiculos WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "vehículo", id);
        }
    }

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

    static Vehiculo mapear(ResultSet rs) throws SQLException {
        return mapearConPrefijo(rs, "");
    }

    static Vehiculo mapearConPrefijo(ResultSet rs, String prefijo) throws SQLException {
        try {
            int id = rs.getInt(prefijo + "id");
            String tipo = rs.getString(prefijo + "tipo");
            int interno = rs.getInt(prefijo + "interno");
            String patente = rs.getString(prefijo + "patente");
            int capacidad = rs.getInt(prefijo + "capacidad");
            boolean enServicio = rs.getInt(prefijo + "en_servicio") == 1;
            if ("COLECTIVO".equals(tipo)) {
                return new Colectivo(id, interno, patente, capacidad, enServicio,
                        rs.getInt(prefijo + "tiene_rampa") == 1);
            }
            if ("METROTRANVIA".equals(tipo)) {
                return new Metrotranvia(id, interno, patente, capacidad, enServicio,
                        rs.getInt(prefijo + "cantidad_duplas"));
            }
            throw new SQLException("Tipo de vehículo desconocido en la base: " + tipo);
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de vehículo inválido en la base: " + e.getMessage(), e);
        }
    }

    /** Hidrata las líneas asociadas desde {@code vehiculo_linea} sin generar recursión con {@link LineaDAO}. */
    static void cargarLineas(Connection c, Vehiculo vehiculo) throws SQLException {
        String sql = "SELECT l.id, l.codigo, l.nombre "
                + "FROM lineas l JOIN vehiculo_linea vl ON vl.linea_id = l.id "
                + "WHERE vl.vehiculo_id = ? ORDER BY l.codigo";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vehiculo.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Linea linea = new Linea(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"));
                    vehiculo.agregarLinea(linea);
                }
            }
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Línea inválida asociada al vehículo: " + e.getMessage(), e);
        }
    }
}
