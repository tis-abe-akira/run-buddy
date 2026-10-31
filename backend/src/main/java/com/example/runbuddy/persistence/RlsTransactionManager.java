package com.example.runbuddy.persistence;

import com.example.runbuddy.shared.CurrentUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Starts every transaction by telling PostgreSQL who the current user is, which is what the row
 * level security policies and the user_id column defaults read (see V1 migration).
 *
 * <p>The setting is transaction-local ({@code set_config(..., true)}), so it never leaks to the
 * next borrower of a pooled connection. Work outside a transaction sees no rows and cannot write.
 * This works the same for every library that runs on Spring's JDBC transactions.
 */
public class RlsTransactionManager extends JdbcTransactionManager {

    private final CurrentUser currentUser;

    public RlsTransactionManager(DataSource dataSource, CurrentUser currentUser) {
        super(dataSource);
        this.currentUser = currentUser;
    }

    @Override
    protected void prepareTransactionalConnection(Connection connection, TransactionDefinition definition)
            throws SQLException {
        super.prepareTransactionalConnection(connection, definition);
        try (PreparedStatement statement = connection.prepareStatement("select set_config('app.user_id', ?, true)")) {
            statement.setString(1, currentUser.id());
            statement.execute();
        }
    }
}
