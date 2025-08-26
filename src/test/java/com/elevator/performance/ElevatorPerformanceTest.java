package com.elevator.performance;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.service.ElevatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ElevatorPerformanceTest {

    @Autowired
    private ElevatorService elevatorService;

    @Test
    void testHighVolumeRequestProcessing() {
        // Given
        int numberOfElevators = 5;
        int numberOfRequests = 100;
        
        // Create elevators
        List<Elevator> elevators = new ArrayList<>();
        for (int i = 0; i < numberOfElevators; i++) {
            elevators.add(elevatorService.createElevator(10));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When - Create many requests
        Random random = new Random();
        List<Request> requests = new ArrayList<>();
        for (int i = 0; i < numberOfRequests; i++) {
            int origin = random.nextInt(10) + 1;
            int destination = random.nextInt(10) + 1;
            while (destination == origin) {
                destination = random.nextInt(10) + 1;
            }
            requests.add(elevatorService.createRequest(origin, destination));
        }

        stopWatch.stop();
        long requestCreationTime = stopWatch.getLastTaskTimeMillis();

        // Then
        assertEquals(numberOfRequests, requests.size());
        assertTrue(requestCreationTime < 5000, "Request creation should complete within 5 seconds");
    }

    @Test
    void testElevatorSelectionPerformance() {
        // Given
        int numberOfElevators = 20;
        
        // Create many elevators at different floors
        Random random = new Random();
        for (int i = 0; i < numberOfElevators; i++) {
            Elevator elevator = elevatorService.createElevator(10);
            elevator.setCurrentFloor(random.nextInt(10) + 1);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When - Find optimal elevator many times
        for (int i = 0; i < 50; i++) {
            elevatorService.createRequest(random.nextInt(10) + 1, random.nextInt(10) + 1);
        }

        stopWatch.stop();
        long selectionTime = stopWatch.getLastTaskTimeMillis();

        // Then
        assertTrue(selectionTime < 2000, "Elevator selection should be fast even with many elevators");
    }
}