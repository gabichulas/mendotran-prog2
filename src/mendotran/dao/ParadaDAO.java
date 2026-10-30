package mendotran.dao;

import mendotran.db.ConexionDB;
import mendotran.excepciones.EntradaInvalidaException;
import mendotran.modelo.Parada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParadaDAO implements CrudDAO<Parada> {

    private static final String COLUMNAS = "id, numero, codigo, direccion, tiene_garita";

    private Connection con() throws SQLException {
        return ConexionDB.getInstance().getConexion();
    }

    @Override
    public int crear(Parada p) throws SQLException {
        String sql = "INSERT INTO paradas (numero, codigo, direccion, tiene_garita) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, p.getNumero());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDireccion());
            ps.setInt(4, p.isTieneGarita() ? 1 : 0);
            ps.executeUpdate();
        }
        int id = SqlUtil.ultimoId(con());
        p.setId(id);
        return id;
    }

    @Override
    public List<Parada> obtenerTodos() throws SQLException {
        List<Parada> resultado = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM paradas ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public Optional<Parada> obtenerPorId(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("SELECT " + COLUMNAS + " FROM paradas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public void actualizar(Parada p) throws SQLException {
        SqlUtil.exigirGuardado(p.getId(), "parada");
        String sql = "UPDATE paradas SET numero = ?, codigo = ?, direccion = ?, tiene_garita = ? WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
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
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM paradas WHERE id = ?")) {
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
