package mendotran.app;

import mendotran.db.ConexionDB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Punto de entrada.
 * <ul>
 *   <li>Sin argumentos: menú interactivo (base {@code mendotran.db}, persistente).</li>
 *   <li>{@code --demo}: secuencia automática de pruebas sobre {@code mendotran_demo.db} (se recrea cada vez).</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean demo = args.length > 0 && "--demo".equals(args[0]);
        if (args.length > 0 && !demo) {
            System.out.println("Uso: java -cp \"out:lib/sqlite-jdbc.jar\" mendotran.app.Main [--demo]");
            return;
        }

        int codigoSalida = 0;
        try {
            if (demo) {
                System.setProperty(ConexionDB.PROPIEDAD_RUTA, "mendotran_demo.db");
                Files.deleteIfExists(Path.of("mendotran_demo.db"));
            }
            ConexionDB.getInstance(); // abre la conexión y crea las tablas si no existen

            if (demo) {
                new Demo().ejecutar();
            } else {
                new MenuConsola(new Scanner(System.in)).iniciar();
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            codigoSalida = 1;
        } catch (Exception e) {
            System.err.println("Error fatal (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            codigoSalida = 1;
        } finally {
            ConexionDB.cerrar();
        }
        if (codigoSalida != 0) {
            System.exit(codigoSalida);
        }
    }
}
