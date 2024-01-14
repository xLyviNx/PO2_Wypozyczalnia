package org.projektpo2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Interfejs umożliwiający ustawienie wartości dla przygotowanego zapytania SQL.
 */
public interface PreparedStatementSetter {
    /**
     * Ustawia wartości dla przygotowanego zapytania SQL.
     *
     * @param preparedStatement Przygotowane zapytanie SQL.
     * @throws SQLException Wyjątek SQL.
     */
    void setValues(PreparedStatement preparedStatement) throws SQLException;
}
