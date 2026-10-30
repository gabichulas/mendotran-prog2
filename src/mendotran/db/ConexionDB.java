package mendotran.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Patrón Singleton: una única instancia gestiona la conexión JDBC con SQLite
 * y crea las tablas la primera vez que se usa.
 *
 * <p>La ruta del archivo se toma de la propiedad de sistema {@code mendotran.db}
 * (por defecto {@code mendotran.db} en el directorio actual).
 *
 * <p>Modelo de tablas:
 * <ul>
 *   <li>{@code personas}: Single Table Inheritance (columna {@code tipo}: PASAJERO, CHOFER, INSPECTOR).</li>
 *   <li>{@code vehiculos}: Single Table Inheritance (columna {@code tipo}: COLECTIVO, METROTRANVIA).</li>
 *   <li>{@code paradas}, {@code lineas}: tablas propias.</li>
 *   <li>{@code linea_parada} (con {@code orden}) y {@code vehiculo_linea}: relaciones.</li>
 * </ul>
 */
public final class ConexionDB {

    public static final String PROPIEDAD_RUTA = "mendotran.db";

    /** Sentencias de creación del esquema (públicas para poder inspeccionarlas en pruebas). */
    public static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS vehiculos ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " tipo TEXT NOT NULL CHECK (tipo IN ('COLECTIVO','METROTRANVIA')),"
                    + " interno INTEGER NOT NULL UNIQUE,"
                    + " patente TEXT NOT NULL UNIQUE,"
                    + " capacidad INTEGER NOT NULL CHECK (capacidad > 0),"
                    + " en_servicio INTEGER NOT NULL DEFAULT 0,"
                    + " tiene_rampa INTEGER,"
                    + " cantidad_duplas INTEGER)",

            "CREATE TABLE IF NOT EXISTS personas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " tipo TEXT NOT NULL CHECK (tipo IN ('PASAJERO','CHOFER','INSPECTOR')),"
                    + " dni TEXT NOT NULL UNIQUE,"
                    + " nombre TEXT NOT NULL,"
                    + " apellido TEXT NOT NULL,"
                    + " numero_sube TEXT,"
                    + " saldo_sube REAL,"
                    + " boleto_pago INTEGER,"
                    + " legajo TEXT,"
                    + " licencia TEXT,"
                    + " vehiculo_id INTEGER REFERENCES vehiculos(id) ON DELETE SET NULL,"
                    + " zona TEXT)",

            "CREATE TABLE IF NOT EXISTS paradas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " numero INTEGER NOT NULL,"
                    + " codigo TEXT NOT NULL UNIQUE,"
                    + " direccion TEXT NOT NULL,"
                    + " tiene_garita INTEGER NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS lineas ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " codigo TEXT NOT NULL UNIQUE,"
                    + " nombre TEXT NOT NULL)",

            "CREATE TABLE IF NOT EXISTS linea_parada ("
                    + " linea_id INTEGER NOT NULL REFERENCES lineas(id) ON DELETE CASCADE,"
                    + " parada_id INTEGER NOT NULL REFERENCES paradas(id) ON DELETE CASCADE,"
                    + " orden INTEGER NOT NULL,"
                    + " PRIMARY KEY (linea_id, orden))",

            "CREATE TABLE IF NOT EXISTS vehiculo_linea ("
                    + " vehiculo_id INTEGER NOT NULL REFERENCES vehiculos(id) ON DELETE CASCADE,"
                    + " linea_id INTEGER NOT NULL REFERENCES lineas(id) ON DELETE CASCADE,"
                    + " PRIMARY KEY (vehiculo_id, linea_id))"
    );

    private static ConexionDB instancia;

    private final String url;
    private Connection conexion;

    private ConexionDB() throws SQLException {
        String ruta = System.getProperty(PROPIEDAD_RUTA, "mendotran.db");
        this.url = "jdbc:sqlite:" + ruta;
        abrir();
        crearTablas();
    }

    /** Devuelve la única instancia (la crea, junto con las tablas, en el primer uso). */
    public static synchronized ConexionDB getInstance() throws SQLException {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    /** Cierra la conexión y permite que una próxima llamada a getInstance() cree otra. */
    public static synchronized void cerrar() {
        if (instancia != null) {
            instancia.cerrarConexion();
            instancia = null;
        }
    }

    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            abrir();
        }
        return conexion;
    }

    private void abrir() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver SQLite. Agregue sqlite-jdbc.jar al classpath "
                    + "(ej.: java -cp \"out:lib/sqlite-jdbc.jar\" ...).", e);
        }
        conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    private void crearTablas() throws SQLException {
        try (Statement st = conexion.createStatement()) {
            for (String sql : DDL) {
                st.execute(sql);
            }
        }
    }

    private void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            System.err.println("Aviso: no se pudo cerrar la conexión: " + e.getMessage());
        }
    }
}
