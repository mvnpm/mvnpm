package org.mvnpm;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MavenRepositoryApiTest {

    @Test
    public void testHelloEndpoint() {
        RestAssured.given().header("User-Agent", "m2e/unit-test")
          .when().get("/maven2/org/mvnpm/lit/2.4.0/lit-2.4.0.pom")
          .then().log().all().and()
             .statusCode(200);
    }

}