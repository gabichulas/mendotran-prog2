package mendotran.dao;

import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Parada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ParadaDAO implements CrudDAO<Parada> {

    private static final String COLUMNAS = "id, numero, codigo, direccion, tiene_garita";

    private final Connection conexion;

    public ParadaDAO(Connection conexion) {
        this.conexion = Objects.requireNonNull(conexion, "La conexión no puede ser nula.");
    }

    @Override
    public int crear(Parada p) throws SQLException {
        String sql = "INSERT INTO paradas (numero, codigo, direccion, tiene_garita) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getNumero());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDireccion());
            ps.setInt(4, p.isTieneGarita() ? 1 : 0);
            ps.executeUpdate();
            int id = SqlUtil.obtenerIdGenerado(ps);
            p.setId(id);
            return id;
        }
    }

    @Override
    public List<Parada> obtenerTodos() throws SQLException {
        List<Parada> resultado = new ArrayList<>();
        String sql = "SELECT " + COLUMNAS + " FROM paradas ORDER BY numero";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public Optional<Parada> obtenerPorId(int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM paradas WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Busca y filtra paradas mediante SQL por código/dirección y/o presencia de garita. */
    public List<Parada> buscar(String texto, Boolean tieneGarita) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNAS + " FROM paradas WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (UPPER(codigo) LIKE ? OR LOWER(direccion) LIKE ?)");
            params.add("%" + texto.trim().toUpperCase() + "%");
            params.add("%" + texto.trim().toLowerCase() + "%");
        }
        if (tieneGarita != null) {
            sql.append(" AND tiene_garita = ?");
            params.add(tieneGarita ? 1 : 0);
        }
        sql.append(" ORDER BY numero");

        List<Parada> resultado = new ArrayList<>();
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
        return resultado;
    }

    @Override
    public void actualizar(Parada p) throws SQLException {
        SqlUtil.exigirGuardado(p.getId(), "parada");
        String sql = "UPDATE paradas SET numero = ?, codigo = ?, direccion = ?, tiene_garita = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, p.getNumero());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDireccion());
            ps.setInt(4, p.isTieneGarita() ? 1 : 0);
            ps.setInt(5, p.getId());
            SqlUtil.exigirFilas(ps.executeUpdate(), "parada", p.getId());
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM paradas WHERE id = ?")) {
            ps.setInt(1, id);
            SqlUtil.exigirFilas(ps.executeUpdate(), "parada", id);
        }
    }

    static Parada mapear(ResultSet rs) throws SQLException {
        try {
            return new Parada(rs.getInt("id"), rs.getInt("numero"), rs.getString("codigo"),
                    rs.getString("direccion"), rs.getInt("tiene_garita") == 1);
        } catch (EntradaInvalidaException e) {
            throw new SQLException("Registro de parada inválido en la base: " + e.getMessage(), e);
        }
    }
}
