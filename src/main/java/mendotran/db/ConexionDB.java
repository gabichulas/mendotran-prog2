package mendotran.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Gestiona la única conexión JDBC con SQLite durante la ejecución del programa.
 *
 * <p>Implementa {@link AutoCloseable} para abrirse una sola vez al iniciar la aplicación
 * (habilitando claves foráneas y creando las tablas si no existen) y cerrarse al finalizar.
 */
public final class ConexionDB implements AutoCloseable {

    public static final String ARCHIVO_POR_DEFECTO = "mendotran.db";

    /** Sentencias DDL del esquema relacional. */
    public static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS vehiculos ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " tipo TEXT NOT NULL CHECK (tipo IN ('COLECTIVO','METROTRANVIA')),"
                    + " interno INTEGER NOT NULL UNIQUE CHECK (interno > 0),"
                    + " patente TEXT NOT NULL UNIQUE,"
                    + " capacidad INTEGER NOT NULL CHECK (capacidad > 0),"
                    + " en_servicio INTEGER NOT NULL DEFAULT 0 CHECK (en_servicio IN (0, 1)),"
                    + " tiene_rampa INTEGER CHECK (tiene_rampa IN (0, 1)),"
                    + " cantidad_duplas INTEGER CHECK (cantidad_duplas IS NULL OR cantidad_duplas > 0))",

            "CREATE TABLE IF NOT EXISTS personas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " tipo TEXT NOT NULL CHECK (tipo IN ('PASAJERO','CHOFER','INSPECTOR')),"
                    + " dni TEXT NOT NULL UNIQUE,"
                    + " nombre TEXT NOT NULL,"
                    + " apellido TEXT NOT NULL,"
                    + " numero_sube TEXT UNIQUE,"
                    + " saldo_sube REAL CHECK (saldo_sube IS NULL OR saldo_sube >= 0),"
                    + " boleto_pago INTEGER CHECK (boleto_pago IN (0, 1)),"
                    + " legajo INTEGER UNIQUE CHECK (legajo IS NULL OR legajo > 0),"
                    + " licencia TEXT,"
                    + " vehiculo_id INTEGER REFERENCES vehiculos(id) ON DELETE SET NULL,"
                    + " zona TEXT)",

            "CREATE TABLE IF NOT EXISTS paradas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " numero INTEGER NOT NULL UNIQUE CHECK (numero > 0),"
                    + " codigo TEXT NOT NULL UNIQUE,"
                    + " direccion TEXT NOT NULL,"
                    + " tiene_garita INTEGER NOT NULL DEFAULT 0 CHECK (tiene_garita IN (0, 1)))",

            "CREATE TABLE IF NOT EXISTS lineas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " codigo TEXT NOT NULL UNIQUE,"
                    + " nombre TEXT NOT NULL)",

            "CREATE TABLE IF NOT EXISTS linea_parada ("
                    + " linea_id INTEGER NOT NULL REFERENCES lineas(id) ON DELETE CASCADE,"
                    + " parada_id INTEGER NOT NULL REFERENCES paradas(id) ON DELETE CASCADE,"
                    + " orden INTEGER NOT NULL CHECK (orden >= 0),"
                    + " PRIMARY KEY (linea_id, orden))",

            "CREATE TABLE IF NOT EXISTS vehiculo_linea ("
                    + " vehiculo_id INTEGER NOT NULL REFERENCES vehiculos(id) ON DELETE CASCADE,"
                    + " linea_id INTEGER NOT NULL REFERENCES lineas(id) ON DELETE CASCADE,"
                    + " PRIMARY KEY (vehiculo_id, linea_id))"
    );

    private final Connection conexion;

    public ConexionDB() throws SQLException {
        this(ARCHIVO_POR_DEFECTO);
    }

    public ConexionDB(String rutaArchivo) throws SQLException {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            throw new IllegalArgumentException("La ruta de la base de datos no puede estar vacía.");
        }
        this.conexion = DriverManager.getConnection("jdbc:sqlite:" + rutaArchivo.trim());
        try {
            inicializarEsquema();
        } catch (SQLException e) {
            cerrarSilenciosamente();
            throw e;
        }
    }

    public Connection getConexion() {
        return conexion;
    }

    private void inicializarEsquema() throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            for (String sql : DDL) {
                st.execute(sql);
            }
        }
    }

    private void cerrarSilenciosamente() {
        try {
            conexion.close();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void close() throws SQLException {
        if (!conexion.isClosed()) {
            conexion.close();
        }
    }
}
