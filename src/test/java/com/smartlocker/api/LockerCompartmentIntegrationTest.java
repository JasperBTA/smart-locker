package com.smartlocker.api;

import com.smartlocker.TestcontainersConfiguration;
import com.smartlocker.domain.CompartmentSize;
import com.smartlocker.domain.CompartmentStatus;
import com.smartlocker.domain.LockerCompartment;
import com.smartlocker.repository.LockerCompartmentRepository;
import com.smartlocker.service.LockerCompartmentService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LockerCompartmentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private LockerCompartmentRepository repository;

    @Autowired
    private LockerCompartmentService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAllInBatch();
    }

    @Test
    @DisplayName("E2E lifecycle: create -> allocate first available -> occupy -> release -> out-of-service -> restore")
    void shouldExecuteFullCompartmentLifecycleViaRestApi() {
        String createBody1 = """
                {
                  "lockerIdentifier": "WAW-01",
                  "compartmentNumber": "M-02",
                  "size": "MEDIUM"
                }
                """;
        String createBody2 = """
                {
                  "lockerIdentifier": "WAW-01",
                  "compartmentNumber": "M-01",
                  "size": "MEDIUM"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(createBody1)
                .when()
                .post("/api/v1/compartments")
                .then()
                .statusCode(201)
                .body("compartmentNumber", equalTo("M-02"))
                .body("status", equalTo("AVAILABLE"));

        String firstCompartmentId = given()
                .contentType(ContentType.JSON)
                .body(createBody2)
                .when()
                .post("/api/v1/compartments")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("compartmentNumber", equalTo("M-01"))
                .body("status", equalTo("AVAILABLE"))
                .body("version", equalTo(0))
                .extract()
                .path("id");

        given()
                .when()
                .get("/api/v1/compartments/{id}", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(firstCompartmentId))
                .body("lockerIdentifier", equalTo("WAW-01"))
                .body("compartmentNumber", equalTo("M-01"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "size": "MEDIUM"
                        }
                        """)
                .when()
                .post("/api/v1/lockers/WAW-01/allocations")
                .then()
                .statusCode(200)
                .body("id", equalTo(firstCompartmentId))
                .body("compartmentNumber", equalTo("M-01"))
                .body("status", equalTo("RESERVED"))
                .body("allocationTimestamp", notNullValue())
                .body("version", equalTo(1));

        given()
                .when()
                .post("/api/v1/compartments/{id}/occupy", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("OCCUPIED"))
                .body("version", equalTo(2));

        given()
                .when()
                .post("/api/v1/compartments/{id}/release", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("AVAILABLE"))
                .body("allocationTimestamp", nullValue())
                .body("version", equalTo(3));

        given()
                .when()
                .post("/api/v1/compartments/{id}/out-of-service", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("OUT_OF_SERVICE"))
                .body("version", equalTo(4));

        given()
                .when()
                .post("/api/v1/compartments/{id}/restore", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("AVAILABLE"))
                .body("version", equalTo(5));

        given()
                .when()
                .post("/api/v1/compartments/{id}/allocate", firstCompartmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("RESERVED"))
                .body("version", equalTo(6));
    }

    @Test
    @DisplayName("Should return RFC 7807 ProblemDetail for 404, 409, and 400 error scenarios")
    void shouldReturnProblemDetailsForBusinessAndValidationErrors() {
        given()
                .when()
                .get("/api/v1/compartments/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("title", equalTo("Compartment Not Found"))
                .body("status", equalTo(404));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "lockerIdentifier": "",
                          "compartmentNumber": "A1",
                          "size": null
                        }
                        """)
                .when()
                .post("/api/v1/compartments")
                .then()
                .statusCode(400)
                .body("title", equalTo("Validation Failed"))
                .body("errors.lockerIdentifier", notNullValue())
                .body("errors.size", notNullValue());

        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "lockerIdentifier": "LUB-01",
                          "compartmentNumber": "S-01",
                          "size": "SMALL"
                        }
                        """)
                .when()
                .post("/api/v1/compartments")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "lockerIdentifier": "LUB-01",
                          "compartmentNumber": "S-01",
                          "size": "SMALL"
                        }
                        """)
                .when()
                .post("/api/v1/compartments")
                .then()
                .statusCode(409)
                .body("title", equalTo("Business Rule Conflict"));

        given()
                .when()
                .post("/api/v1/compartments/{id}/occupy", id)
                .then()
                .statusCode(409)
                .body("title", equalTo("Business Rule Conflict"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "size": "LARGE"
                        }
                        """)
                .when()
                .post("/api/v1/lockers/LUB-01/allocations")
                .then()
                .statusCode(409)
                .body("title", equalTo("Business Rule Conflict"));
    }

    @Test
    @DisplayName("Should filter compartments by status and aggregate accurate locker summary")
    void shouldListCompartmentsAndReturnLockerSummary() {
        LockerCompartment c1 = service.createCompartment("LUB-02", "A-01", CompartmentSize.SMALL);
        LockerCompartment c2 = service.createCompartment("LUB-02", "A-02", CompartmentSize.MEDIUM);
        LockerCompartment c3 = service.createCompartment("LUB-02", "A-03", CompartmentSize.LARGE);
        LockerCompartment c4 = service.createCompartment("LUB-02", "A-04", CompartmentSize.SMALL);

        service.allocateCompartment(c2.getId());
        service.allocateCompartment(c3.getId());
        service.occupyCompartment(c3.getId());
        service.markOutOfService(c4.getId());

        given()
                .when()
                .get("/api/v1/lockers/LUB-02/compartments")
                .then()
                .statusCode(200)
                .body("$", hasSize(4))
                .body("[0].id", equalTo(c1.getId().toString()))
                .body("[0].compartmentNumber", equalTo("A-01"));

        given()
                .queryParam("status", "AVAILABLE")
                .when()
                .get("/api/v1/lockers/LUB-02/compartments")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].compartmentNumber", equalTo("A-01"));

        given()
                .when()
                .get("/api/v1/lockers/LUB-02/summary")
                .then()
                .statusCode(200)
                .body("lockerIdentifier", equalTo("LUB-02"))
                .body("totalCompartments", equalTo(4))
                .body("available", equalTo(1))
                .body("reserved", equalTo(1))
                .body("occupied", equalTo(1))
                .body("outOfService", equalTo(1));
    }

    @Test
    @DisplayName("Optimistic locking (@Version): concurrent allocation allows exactly one winner")
    void shouldPreventDoubleAllocationWithOptimisticLocking() throws Exception {
        LockerCompartment created = service.createCompartment("CONC-01", "C-01", CompartmentSize.LARGE);
        UUID compartmentId = created.getId();

        CyclicBarrier bothLoadedBarrier = new CyclicBarrier(2);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        Callable<Boolean> allocateTask = () -> {
            try {
                txTemplate.executeWithoutResult(status -> {
                    LockerCompartment compartment = repository.findById(compartmentId).orElseThrow();
                    try {
                        bothLoadedBarrier.await(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    compartment.allocate();
                    repository.saveAndFlush(compartment);
                });
                return true;
            } catch (ObjectOptimisticLockingFailureException ex) {
                return false;
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = executor.invokeAll(List.of(allocateTask, allocateTask));
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            long winners = results.stream().filter(Boolean::booleanValue).count();
            long conflicts = results.stream().filter(result -> !result).count();

            assertThat(winners).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);
        }

        LockerCompartment persisted = repository.findById(compartmentId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(CompartmentStatus.RESERVED);
        assertThat(persisted.getVersion()).isEqualTo(1L);
    }
}
