package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthLoginContractTest {
    private static final String LOGIN_PATH = "/auth/login";
    private static final int TOKEN_TTL_SECONDS = 3600;

    @Test
    void successfulLoginKeepsResponseContract() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"trainer\",\"password\":\"Trainer123!\",\"client\":\"angular-workshop\"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", is(TOKEN_TTL_SECONDS))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("user.username", is("trainer"))
                .body("user.displayName", is("Workshop Trainer"))
                .body("user.role", is("TRAINER"))
                .body("user", hasKey("username"))
                .body("user", hasKey("displayName"))
                .body("user", hasKey("role"));
    }

    @Test
    void legacyTokenKeepsUsernameRoleAndExpiryLayout() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post(LOGIN_PATH)
                .then().statusCode(200)
                .extract().path("token");

        String[] parts = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split("\\|");

        assertEquals(4, parts.length);
        assertEquals("student", parts[0]);
        assertEquals("STUDENT", parts[1]);
        long expiry = Long.parseLong(parts[2]);
        long now = Instant.now().getEpochSecond();
        assertTrue(expiry > now + TOKEN_TTL_SECONDS - 60 && expiry <= now + TOKEN_TTL_SECONDS);
        assertEquals("WORKSHOP_SECRET_2026", parts[3]);
    }

    @Test
    void rejectsWrongPasswordWithUnauthorized() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrecta\"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(401)
                .body("message", notNullValue());
    }

    @Test
    void rejectsUnknownUserWithUnauthorized() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"nadie\",\"password\":\"Workshop123!\"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(401)
                .body("message", notNullValue());
    }

    @Test
    void usernameWithApostropheIsRejectedAsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"O'Neil\",\"password\":\"Workshop123!\"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    void rejectsBlankUsernameWithBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"   \",\"password\":\"Workshop123!\"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankPasswordWithBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void ignoresUnknownRequestFields() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"extra\":1}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(200);
    }
}
