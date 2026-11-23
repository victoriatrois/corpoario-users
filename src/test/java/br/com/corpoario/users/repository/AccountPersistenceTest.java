package br.com.corpoario.users.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.corpoario.users.entity.Account;
import br.com.corpoario.users.entity.AccountRole;
import br.com.corpoario.users.entity.AccountStatus;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AccountPersistenceTest {
    private static final String PASSWORD = "a-plaintext-password";
    private static final String PASSWORD_HASH = BcryptUtil.bcryptHash(PASSWORD);

    @Inject
    AccountRepository accounts;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        accounts.deleteAll();
    }

    @Test
    void shouldNormaliseEmailAndPersistStableAccountData() {
        Account account = new Account(
                "  Cliente@Example.COM  ",
                PASSWORD_HASH,
                AccountRole.CUSTOMER);

        QuarkusTransaction.requiringNew().run(() -> accounts.persist(account));
        Account persisted = QuarkusTransaction.requiringNew()
                .call(() -> accounts.findByEmail("cliente@example.com").orElseThrow());

        assertNotNull(persisted.getId());
        assertEquals("cliente@example.com", persisted.getEmail());
        assertEquals(AccountRole.CUSTOMER, persisted.getRole());
        assertEquals(AccountStatus.ACTIVE, persisted.getStatus());
        assertNotNull(persisted.getCreatedAt());
        assertEquals(persisted.getCreatedAt(), persisted.getUpdatedAt());
    }

    @Test
    void shouldPersistArtistRole() {
        Account account = new Account("artist@example.com", PASSWORD_HASH, AccountRole.ARTIST);

        QuarkusTransaction.requiringNew().run(() -> accounts.persist(account));
        Account persisted = QuarkusTransaction.requiringNew()
                .call(() -> accounts.findByEmail("artist@example.com").orElseThrow());

        assertEquals(AccountRole.ARTIST, persisted.getRole());
    }

    @Test
    void shouldReadDisabledStatusFromDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("committed", insertWithValues(connection, "disabled@example.com", "CUSTOMER", "DISABLED"));
        }

        Account persisted = QuarkusTransaction.requiringNew()
                .call(() -> accounts.findByEmail("disabled@example.com").orElseThrow());

        assertEquals(AccountStatus.DISABLED, persisted.getStatus());
    }

    @Test
    void shouldPersistPasswordHashWithoutPlaintext() throws SQLException {
        Account account = new Account("hash@example.com", PASSWORD_HASH, AccountRole.CUSTOMER);
        QuarkusTransaction.requiringNew().run(() -> accounts.persist(account));

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT password_hash FROM user_accounts WHERE email = ?")) {
            statement.setString(1, "hash@example.com");
            var result = statement.executeQuery();
            assertTrue(result.next());
            String stored = result.getString("password_hash");
            assertFalse(stored.contains(PASSWORD));
            assertTrue(BcryptUtil.matches(PASSWORD, stored));
        }
    }

    @Test
    void shouldRejectPlaintextPasswordWithoutExposingIt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Account("plain@example.com", PASSWORD, AccountRole.CUSTOMER));

        assertFalse(exception.getMessage().contains(PASSWORD));
        QuarkusTransaction.requiringNew().run(() -> assertEquals(0, accounts.count()));
    }

    @Test
    void shouldRejectEmailVariantsOfAnExistingAccount() {
        QuarkusTransaction.requiringNew().run(() -> accounts.persist(
                new Account("same@example.com", PASSWORD_HASH, AccountRole.CUSTOMER)));

        assertThrows(PersistenceException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            accounts.persist(new Account("  SAME@Example.COM ", PASSWORD_HASH, AccountRole.CUSTOMER));
            accounts.flush();
        }));

        QuarkusTransaction.requiringNew().run(() -> assertEquals(1, accounts.count()));
    }

    @Test
    void shouldAllowAtMostOneConcurrentAccountPerEmail() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> insertAccount(ready, start));
            Future<String> second = executor.submit(() -> insertAccount(ready, start));
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }

            String firstResult = first.get(20, TimeUnit.SECONDS);
            String secondResult = second.get(20, TimeUnit.SECONDS);
            assertTrue(
                    (firstResult.equals("committed") && secondResult.equals("23505"))
                            || (secondResult.equals("committed") && firstResult.equals("23505")),
                    () -> "Unexpected outcomes: " + firstResult + ", " + secondResult);
        }

        QuarkusTransaction.requiringNew().run(() -> assertEquals(1, accounts.count()));
    }

    @Test
    void shouldRejectUnsupportedRolesAndStatuses() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("23514", insertWithValues(connection, "invalid-role@example.com", "INVALID", "ACTIVE"));
            assertEquals("23514", insertWithValues(connection, "invalid-status@example.com", "CUSTOMER", "INVALID"));
        }
    }

    private String insertAccount(CountDownLatch ready, CountDownLatch start) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent inserts did not start");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO user_accounts
                        (id, email, password_hash, role, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)) {
                statement.setQueryTimeout(10);
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, "same@example.com");
                statement.setString(3, PASSWORD_HASH);
                statement.setString(4, "CUSTOMER");
                statement.setString(5, "ACTIVE");
                statement.executeUpdate();
                connection.commit();
                return "committed";
            } catch (SQLException exception) {
                connection.rollback();
                return exception.getSQLState();
            }
        }
    }

    private String insertWithValues(
            Connection connection,
            String email,
            String role,
            String status) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_accounts
                    (id, email, password_hash, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, email);
            statement.setString(3, PASSWORD_HASH);
            statement.setString(4, role);
            statement.setString(5, status);
            statement.executeUpdate();
            return "committed";
        } catch (SQLException exception) {
            return exception.getSQLState();
        }
    }
}
