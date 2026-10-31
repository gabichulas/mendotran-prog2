package mendotran.dao;

import java.sql.SQLException;
import java.util.List;

/** Contrato genérico de persistencia: Create, Read, Update, Delete. */
public interface CrudDAO<T> {

    /** Inserta la entidad y devuelve el id generado (también lo asigna a la entidad). */
    int crear(T entidad) throws SQLException;

    List<T> obtenerTodos() throws SQLException;

    void actualizar(T entidad) throws SQLException;

    void eliminar(int id) throws SQLException;
}
