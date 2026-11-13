package com.eex.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de bout en bout de la chaîne de sécurité, sur une base SQLite temporaire initialisée
 * avec le jeu de données de DataInitializationConfig :
 * Alice → client 1 / compte 1 AMAZON (1000), Bernard → client 2 / compte 2 BORNEO (2000),
 * Chloé → client 3 / compte 3 COMORA (3000), David → client 4 (aucun compte).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    private static final Path DB_FILE = createTempDb();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_FILE.toAbsolutePath());
    }

    private static Path createTempDb() {
        try {
            Path file = Files.createTempFile("eval-test-", ".db");
            File f = file.toFile();
            f.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    record Credentials(String username, String password) {
    }

    // ---------- Authentification ----------

    @Test
    void protectedRoutesRequireAToken() throws Exception {
        for (String url : new String[]{"/api/accounts", "/api/accounts/1", "/api/users", "/api/users/me",
                "/api/clients/me", "/api/clients/1"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mvc.perform(put("/api/accounts/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginWithUnknownUserFails() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Mallory\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithMissingFieldsIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Alice\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateEndpointAcceptsValidTokenOnly() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(post("/api/auth/validate").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/validate").header("Authorization", "Bearer forged"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/validate"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Client ----------

    @Test
    void userSeesOwnClient() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/clients/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.legalEntity").value(true));
        mvc.perform(get("/api/clients/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotSeeAnotherClient() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/clients/2").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientListingAndMutationsAreNotExposed() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/clients").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/clients/2").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }

    // ---------- Comptes ----------

    @Test
    void userSeesOnlyOwnClientAccounts() throws Exception {
        String token = login("Bernard", "bernardSecret");

        mvc.perform(get("/api/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].clientId").value(2))
                .andExpect(jsonPath("$[0].client").doesNotExist());
    }

    @Test
    void clientWithoutAccountsGetsEmptyList() throws Exception {
        String token = login("David", "davidSecret");

        mvc.perform(get("/api/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void userCannotReadAnotherClientsAccount() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/accounts/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts/2").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/client/2").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void searchIsScopedToOwnClient() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/accounts/search").param("name", "BORNEO").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/accounts/search").param("name", "AMAZON").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void accountUpdateChangesOnlyTheName() throws Exception {
        String token = login("Chloé", "chloeSecret");

        mvc.perform(put("/api/accounts/3")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"COMORA PRO\",\"amount\":999999,\"clientId\":1,\"id\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("COMORA PRO"))
                .andExpect(jsonPath("$.amount").value(3000.0))
                .andExpect(jsonPath("$.clientId").value(3));

        mvc.perform(patch("/api/accounts/3")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"COMORA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("COMORA"))
                .andExpect(jsonPath("$.amount").value(3000.0));
    }

    @Test
    void accountUpdateWithOnlyAmountIsRejected() throws Exception {
        String token = login("Chloé", "chloeSecret");

        mvc.perform(put("/api/accounts/3")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/accounts/3")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/accounts/3").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.amount").value(3000.0));
    }

    @Test
    void userCannotRenameAnotherClientsAccount() throws Exception {
        String alice = login("Alice", "aliceSecret");
        String bernard = login("Bernard", "bernardSecret");

        mvc.perform(put("/api/accounts/2")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"HACKED\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/accounts/2").header("Authorization", bearer(bernard)))
                .andExpect(jsonPath("$.name").value("BORNEO"));
    }

    @Test
    void accountsCannotBeCreatedOrDeleted() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(post("/api/accounts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"amount\":1000000,\"clientId\":1}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/accounts/1").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/accounts/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    // ---------- Utilisateurs ----------

    @Test
    void userSeesOwnClientUsersWithoutPasswords() throws Exception {
        String token = login("Alice", "aliceSecret");

        String body = mvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("Alice"))
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get(0).has("password")).isFalse();
        assertThat(body).doesNotContain("$2a$");
    }

    @Test
    void userSeesOwnProfile() throws Exception {
        String token = login("Emma", "emmaSecret");

        mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("Emma"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void userCannotSeeUsersOfAnotherClient() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/users/2").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users/search").param("username", "Bernard").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void usersCannotBeCreatedModifiedOrDeleted() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(post("/api/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"evil\",\"password\":\"x\",\"clientId\":2}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(put("/api/users/1")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"x\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/users/2").header("Authorization", bearer(token)))
                .andExpect(status().is4xxClientError());

        // Le mot de passe d'Alice n'a pas changé.
        login("Alice", "aliceSecret");
    }

    @Test
    void responsesNeverLeakInternals() throws Exception {
        String token = login("Alice", "aliceSecret");

        mvc.perform(get("/api/accounts/abc").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .doesNotContain("Exception", "at com.eex"));
    }
}
