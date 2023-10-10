package io.mvnpm;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MavenRepositoryApiTest {

    @Test
    public void testBasicPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/lit/2.4.0/lit-2.4.0.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testStarDependencyPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/types/codemirror/5.60.5/codemirror-5.60.5.pom")
                .then().log().all().and()
                .statusCode(200);
    }

    @Test
    public void testIgnoreBetaPom() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
                .when().get("/maven2/org/mvnpm/at/vaadin/tabs/23.2.3/vaadin-23.2.3.pom")
                .then().log().all().and()
                .statusCode(200);
    }

}
