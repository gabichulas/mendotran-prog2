package mendotran.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Utilidades JDBC compartidas por los DAO (visibilidad de paquete). */
final class SqlUtil {

    private SqlUtil() {
    }

    /** Obtiene la clave primaria autogenerada tras ejecutar un INSERT. */
    static int obtenerIdGenerado(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("No se pudo obtener el id generado.");
        }
    }

    /** Falla si un UPDATE o DELETE no afectó ninguna fila. */
    static void exigirFilas(int filas, String entidad, int id) throws SQLException {
        if (filas == 0) {
            throw new SQLException("No existe " + entidad + " con id " + id + ".");
        }
    }

    static void exigirGuardado(int id, String entidad) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Ese/a " + entidad + " todavía no fue guardado/a en la base (id " + id + ").");
        }
    }
}
