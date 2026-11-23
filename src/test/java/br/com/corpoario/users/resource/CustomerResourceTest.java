package br.com.corpoario.users.resource;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.corpoario.users.entity.Account;
import br.com.corpoario.users.entity.AccountRole;
import br.com.corpoario.users.repository.AccountRepository;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CustomerResourceTest {
    private static final String PASSWORD = "a-plaintext-password";

    @Inject
    AccountRepository accounts;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        accounts.deleteAll();
    }

    @Test
    void shouldRegisterCustomerWithNormalisedEmailAndWithoutExposingPassword() {
        given()
            .contentType(JSON)
            .body(registration("  Cliente@Example.COM  ", PASSWORD))
            .when().post("/customers")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo("cliente@example.com"))
            .body("role", equalTo("CUSTOMER"))
            .body("status", equalTo("ACTIVE"))
            .body("createdAt", notNullValue())
            .body("password", nullValue())
            .body("passwordHash", nullValue());

        List<Account> stored = accounts.listAll();
        assertEquals(1, stored.size());
        assertEquals("cliente@example.com", stored.get(0).getEmail());
    }

    @Test
    void shouldPersistOnlyAStrongHashAndNeverThePlaintextPassword() {
        given()
            .contentType(JSON)
            .body(registration("cliente@example.com", PASSWORD))
            .when().post("/customers")
            .then()
            .statusCode(201);

        String storedHash = storedPasswordHashes().get(0);

        assertTrue(storedHash.startsWith("$2a$12$"));
        assertFalse(storedHash.contains(PASSWORD));
        assertTrue(BcryptUtil.matches(PASSWORD, storedHash));
    }

    @Test
    void shouldAlwaysAssignCustomerRoleIgnoringPrivilegedFieldsInPayload() {
        given()
            .contentType(JSON)
            .body("""
                {"email": "artista@example.com", "password": "%s", "role": "ARTIST", "status": "DISABLED"}
                """.formatted(PASSWORD))
            .when().post("/customers")
            .then()
            .statusCode(201)
            .body("role", equalTo("CUSTOMER"))
            .body("status", equalTo("ACTIVE"));

        List<Account> stored = accounts.listAll();
        assertEquals(1, stored.size());
        assertEquals(AccountRole.CUSTOMER, stored.get(0).getRole());
    }

    @Test
    void shouldRejectDuplicateEmailRegardlessOfCaseAndWhitespace() {
        given()
            .contentType(JSON)
            .body(registration("cliente@example.com", PASSWORD))
            .when().post("/customers")
            .then()
            .statusCode(201);

        given()
            .contentType(JSON)
            .body(registration(" CLIENTE@Example.com ", "another-password"))
            .when().post("/customers")
            .then()
            .statusCode(409)
            .body("status", equalTo(409))
            .body("error", equalTo("Email already registered"))
            .body("message", equalTo("An account with this email already exists"));

        assertEquals(1, accounts.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"password\": \"a-plaintext-password\"}",
        "{\"email\": \"   \", \"password\": \"a-plaintext-password\"}",
        "{\"email\": \"not-an-email\", \"password\": \"a-plaintext-password\"}",
        "{\"email\": \"cliente@localhost\", \"password\": \"a-plaintext-password\"}",
        "{\"email\": \"cliente@example.com\"}",
        "{\"email\": \"cliente@example.com\", \"password\": \"        \"}",
        "{\"email\": \"cliente@example.com\", \"password\": \"short\"}",
        "null",
        "{not json"
    })
    void shouldRejectInvalidInputWithoutPersistingAnything(String body) {
        given()
            .contentType(JSON)
            .body(body)
            .when().post("/customers")
            .then()
            .statusCode(400);

        assertEquals(0, accounts.count());
    }

    @Test
    void shouldAcceptPasswordOfExactly72BytesAndKeepItFullyVerifiable() {
        String password = "é".repeat(36);

        given()
            .contentType(JSON)
            .body(registration("cliente@example.com", password))
            .when().post("/customers")
            .then()
            .statusCode(201);

        assertTrue(BcryptUtil.matches(password, storedPasswordHashes().get(0)));
    }

    @Test
    void shouldAcceptEmailOfExactly254Characters() {
        given()
            .contentType(JSON)
            .body(registration(emailOfLength(254), PASSWORD))
            .when().post("/customers")
            .then()
            .statusCode(201);
    }

    @Test
    void shouldRejectPasswordOver72BytesAndEmailOver254Characters() {
        for (String body : List.of(
                registration("cliente@example.com", "x".repeat(73)),
                registration("cliente@example.com", "é".repeat(37)),
                registration(emailOfLength(255), PASSWORD))) {
            given()
                .contentType(JSON)
                .body(body)
                .when().post("/customers")
                .then()
                .statusCode(400);
        }

        assertEquals(0, accounts.count());
    }

    @Test
    void shouldDescribeValidationErrors() {
        given()
            .contentType(JSON)
            .body(registration("not-an-email", "short"))
            .when().post("/customers")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("error", equalTo("Validation error"))
            .body("message", equalTo("Email must be a valid address; Password must have at least 8 characters"));
    }

    private static String emailOfLength(int length) {
        // Short labels keep the address valid so only the overall length is under test.
        int topLevelDomainLength = length % 2 == 0 ? 2 : 3;
        return "a@" + "b.".repeat((length - 2 - topLevelDomainLength) / 2) + "c".repeat(topLevelDomainLength);
    }

    private static String registration(String email, String password) {
        return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
    }

    @SuppressWarnings("unchecked")
    private List<String> storedPasswordHashes() {
        return QuarkusTransaction.requiringNew().call(() -> entityManager
            .createNativeQuery("select password_hash from user_accounts")
            .getResultList());
    }
}
