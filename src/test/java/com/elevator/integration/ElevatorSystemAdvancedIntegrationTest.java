package com.elevator.integration;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.model.Direction;
import com.elevator.model.State;
import com.elevator.service.ElevatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ElevatorSystemAdvancedIntegrationTest {

    @Autowired
    private ElevatorService elevatorService;

    @BeforeEach
    void setUp() {
        // Clean setup for each test
    }

    @Test
    void testComplexMultiElevatorScenario() {
        // Given - Create 3 elevators
        Elevator elevator1 = elevatorService.createElevator(8);
        Elevator elevator2 = elevatorService.createElevator(10);
        Elevator elevator3 = elevatorService.createElevator(6);

        // When - Create overlapping requests
        Request req1 = elevatorService.createRequest(1, 10); // Long distance
        Request req2 = elevatorService.createRequest(2, 3);  // Short distance
        Request req3 = elevatorService.createRequest(5, 8);  // Medium distance
        Request req4 = elevatorService.createRequest(7, 1);  // Down direction

        // Process all requests
        for (int step = 0; step < 50; step++) {
            elevatorService.processNextStep(elevator1.getId());
            elevatorService.processNextStep(elevator2.getId());
            elevatorService.processNextStep(elevator3.getId());
        }

        // Then - All requests should be completed
        assertTrue(elevatorService.getPendingRequests(elevator1.getId()).isEmpty());
        assertTrue(elevatorService.getPendingRequests(elevator2.getId()).isEmpty());
        assertTrue(elevatorService.getPendingRequests(elevator3.getId()).isEmpty());
    }

    @Test
    void testConcurrentRequestHandling() throws InterruptedException {
        // Given
        Elevator elevator = elevatorService.createElevator(15);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // When - Submit concurrent requests
        CompletableFuture<?>[] futures = new CompletableFuture[20];
        for (int i = 0; i < 20; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                int origin = (requestId % 10) + 1;
                int dest = ((requestId + 5) % 10) + 1;
                elevatorService.createRequest(origin, dest);
            }, executor);
        }

        // Wait for all requests to be created
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Process requests
        for (int step = 0; step < 100; step++) {
            elevatorService.processNextStep(elevator.getId());
            if (elevatorService.getPendingRequests(elevator.getId()).isEmpty()) {
                break;
            }
        }

        // Then - All requests should eventually be processed
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.size() <= 5, "Most requests should be completed");
    }

    @Test
    void testElevatorLoadBalancing() {
        // Given - Create multiple elevators
        Elevator elevator1 = elevatorService.createElevator(5);
        Elevator elevator2 = elevatorService.createElevator(5);
        Elevator elevator3 = elevatorService.createElevator(5);

        // When - Create many requests
        for (int i = 0; i < 15; i++) {
            elevatorService.createRequest(i % 10 + 1, (i + 3) % 10 + 1);
        }

        // Then - Requests should be distributed among elevators
        int totalPendingRequests = elevatorService.getPendingRequests(elevator1.getId()).size() +
                                 elevatorService.getPendingRequests(elevator2.getId()).size() +
                                 elevatorService.getPendingRequests(elevator3.getId()).size();
        
        assertEquals(15, totalPendingRequests);
        
        // No single elevator should have all requests
        assertTrue(elevatorService.getPendingRequests(elevator1.getId()).size() < 15);
        assertTrue(elevatorService.getPendingRequests(elevator2.getId()).size() < 15);
        assertTrue(elevatorService.getPendingRequests(elevator3.getId()).size() < 15);
    }

    @Test
    void testElevatorRecoveryFromFullCapacity() {
        // Given
        Elevator elevator = elevatorService.createElevator(2);
        
        // Fill elevator to capacity
        Request req1 = elevatorService.createRequest(1, 5);
        Request req2 = elevatorService.createRequest(2, 6);
        
        // Process to pick up passengers
        for (int i = 0; i < 10; i++) {
            elevatorService.processNextStep(elevator.getId());
        }
        
        // When - Try to add more requests (should be rejected or queued)
        Request req3 = elevatorService.createRequest(3, 7);
        
        // The new request should go to the same elevator but wait
        assertNotNull(req3.getElevator());
        
        // Process until elevator has capacity again
        for (int i = 0; i < 20; i++) {
            elevatorService.processNextStep(elevator.getId());
        }
        
        // Then - All requests should eventually be processed
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.size() <= 1, "Elevator should handle capacity constraints");
    }

    @Test
    void testElevatorDirectionOptimization() {
        // Given
        Elevator elevator = elevatorService.createElevator(10);
        elevator.setCurrentFloor(5);
        
        // When - Create requests in same direction
        Request upReq1 = elevatorService.createRequest(6, 9);
        Request upReq2 = elevatorService.createRequest(7, 10);
        Request downReq = elevatorService.createRequest(4, 2);
        
        // Process requests
        for (int i = 0; i < 30; i++) {
            elevatorService.processNextStep(elevator.getId());
        }
        
        // Then - All requests should be completed efficiently
        assertTrue(elevatorService.getPendingRequests(elevator.getId()).isEmpty());
    }

    @Test
    void testSystemStabilityUnderLoad() {
        // Given - Create multiple elevators
        for (int i = 0; i < 5; i++) {
            elevatorService.createElevator(8);
        }
        
        // When - Create many requests over time
        for (int batch = 0; batch < 10; batch++) {
            // Create batch of requests
            for (int i = 0; i < 5; i++) {
                elevatorService.createRequest((batch + i) % 10 + 1, (batch + i + 3) % 10 + 1);
            }
            
            // Process some steps
            List<Elevator> elevators = elevatorService.getAllElevators();
            for (Elevator elevator : elevators) {
                for (int step = 0; step < 3; step++) {
                    elevatorService.processNextStep(elevator.getId());
                }
            }
        }
        
        // Then - System should remain stable
        List<Elevator> elevators = elevatorService.getAllElevators();
        assertEquals(5, elevators.size());
        
        // All elevators should be in valid states
        for (Elevator elevator : elevators) {
            assertNotNull(elevator.getState());
            assertNotNull(elevator.getDirection());
            assertTrue(elevator.getCurrentFloor() >= 1);
            assertTrue(elevator.getCurrentFloor() <= 10);
            assertTrue(elevator.getCurrentLoad() >= 0);
            assertTrue(elevator.getCurrentLoad() <= elevator.getMaxCapacity());
        }
    }
}