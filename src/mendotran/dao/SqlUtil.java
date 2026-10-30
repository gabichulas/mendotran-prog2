package mendotran.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Utilidades JDBC compartidas por los DAO (visibilidad de paquete). */
final class SqlUtil {

    private SqlUtil() {
    }

    /** Id del último INSERT hecho en esta conexión. */
    static int ultimoId(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No se pudo obtener el id generado.");
        }
    }

    /** Falla si un UPDATE/DELETE no afectó ninguna fila. */
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
