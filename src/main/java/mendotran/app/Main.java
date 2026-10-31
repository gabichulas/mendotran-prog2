package mendotran.app;

import mendotran.db.ConexionDB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Punto de entrada de la aplicación.
 * <ul>
 *   <li>Sin argumentos: menú interactivo por consola (base {@code mendotran.db}).</li>
 *   <li>{@code --demo}: demostración automática completa sobre {@code mendotran_demo.db}.</li>
 * </ul>
 */
public final class Main {

    private static final String ARCHIVO_DEMO = "mendotran_demo.db";

    private Main() {
    }

    public static void main(String[] args) {
        boolean demo = args.length == 1 && "--demo".equals(args[0]);
        if (args.length > 0 && !demo) {
            System.out.println("Uso: java -jar target/mendotran.jar [--demo]");
            return;
        }

        try {
            if (demo) {
                Files.deleteIfExists(Path.of(ARCHIVO_DEMO));
            }
            String archivo = demo ? ARCHIVO_DEMO : ConexionDB.ARCHIVO_POR_DEFECTO;

            try (ConexionDB db = new ConexionDB(archivo)) {
                if (demo) {
                    new Demo(db.getConexion()).ejecutar();
                } else {
                    new MenuConsola(new Scanner(System.in), db.getConexion()).iniciar();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error fatal (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            System.exit(1);
        }
    }
}
