package com.elevator.entity;

import com.elevator.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    private Request request;

    @BeforeEach
    void setUp() {
        request = new Request();
    }

    @Test
    void request_ShouldAllowSettingProperties() {
        // Given
        Date createdAt = new Date();
        Date completedAt = new Date();
        Elevator elevator = new Elevator();

        // When
        request.setOriginFloor(3);
        request.setDestinationFloor(7);
        request.setDirection(Direction.UP);
        request.setCompleted(true);
        request.setPassengerPickedUp(true);
        request.setCreatedAt(createdAt);
        request.setCompletedAt(completedAt);
        request.setElevator(elevator);

        // Then
        assertEquals(3, request.getOriginFloor());
        assertEquals(7, request.getDestinationFloor());
        assertEquals(Direction.UP, request.getDirection());
        assertTrue(request.isCompleted());
        assertTrue(request.isPassengerPickedUp());
        assertEquals(createdAt, request.getCreatedAt());
        assertEquals(completedAt, request.getCompletedAt());
        assertEquals(elevator, request.getElevator());
    }

    @Test
    void request_ShouldHaveDefaultValues() {
        // Then
        assertFalse(request.isCompleted());
        assertFalse(request.isPassengerPickedUp());
        assertNull(request.getCreatedAt());
        assertNull(request.getCompletedAt());
        assertNull(request.getElevator());
    }

    @Test
    void request_ShouldSupportUpDirection() {
        // When
        request.setOriginFloor(2);
        request.setDestinationFloor(8);
        request.setDirection(Direction.UP);

        // Then
        assertEquals(Direction.UP, request.getDirection());
        assertTrue(request.getDestinationFloor() > request.getOriginFloor());
    }

    @Test
    void request_ShouldSupportDownDirection() {
        // When
        request.setOriginFloor(9);
        request.setDestinationFloor(3);
        request.setDirection(Direction.DOWN);

        // Then
        assertEquals(Direction.DOWN, request.getDirection());
        assertTrue(request.getDestinationFloor() < request.getOriginFloor());
    }

    @Test
    void request_ShouldHaveValidToString() {
        // Given
        request.setId(1L);
        request.setOriginFloor(3);
        request.setDestinationFloor(7);
        request.setDirection(Direction.UP);

        // When
        String toString = request.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("1"));
        assertTrue(toString.contains("3"));
        assertTrue(toString.contains("7"));
        assertTrue(toString.contains("UP"));
        // Should exclude elevator due to @ToString(exclude = "elevator")
        assertFalse(toString.contains("elevator"));
    }

    @Test
    void request_ShouldTrackLifecycle() {
        // Given
        Date startTime = new Date();

        // When - Request created
        request.setCreatedAt(startTime);
        assertFalse(request.isCompleted());
        assertFalse(request.isPassengerPickedUp());

        // When - Passenger picked up
        request.setPassengerPickedUp(true);
        assertTrue(request.isPassengerPickedUp());
        assertFalse(request.isCompleted());

        // When - Request completed
        Date endTime = new Date();
        request.setCompleted(true);
        request.setCompletedAt(endTime);

        // Then
        assertTrue(request.isCompleted());
        assertTrue(request.isPassengerPickedUp());
        assertEquals(startTime, request.getCreatedAt());
        assertEquals(endTime, request.getCompletedAt());
    }

    @Test
    void request_ShouldSupportSameFloorRequest() {
        // When
        request.setOriginFloor(5);
        request.setDestinationFloor(5);

        // Then
        assertEquals(request.getOriginFloor(), request.getDestinationFloor());
    }
}