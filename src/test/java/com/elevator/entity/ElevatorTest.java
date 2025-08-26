package com.elevator.entity;

import com.elevator.model.Direction;
import com.elevator.model.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorTest {

    private Elevator elevator;

    @BeforeEach
    void setUp() {
        elevator = new Elevator();
    }

    @Test
    void elevator_ShouldHaveDefaultValues() {
        // Then
        assertEquals(0, elevator.getCurrentLoad());
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(Direction.IDLE, elevator.getDirection());
        assertEquals(State.IDLE, elevator.getState());
        assertNotNull(elevator.getStops());
        assertNotNull(elevator.getRequests());
    }

    @Test
    void elevator_ShouldAllowSettingProperties() {
        // When
        elevator.setMaxCapacity(15);
        elevator.setCurrentLoad(5);
        elevator.setCurrentFloor(8);
        elevator.setDirection(Direction.UP);
        elevator.setState(State.MOVING);

        // Then
        assertEquals(15, elevator.getMaxCapacity());
        assertEquals(5, elevator.getCurrentLoad());
        assertEquals(8, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());
        assertEquals(State.MOVING, elevator.getState());
    }

    @Test
    void elevator_ShouldManageStops() {
        // Given
        elevator.setStops(new HashSet<>());

        // When
        elevator.getStops().add(3);
        elevator.getStops().add(7);
        elevator.getStops().add(5);

        // Then
        assertEquals(3, elevator.getStops().size());
        assertTrue(elevator.getStops().contains(3));
        assertTrue(elevator.getStops().contains(7));
        assertTrue(elevator.getStops().contains(5));
    }

    @Test
    void elevator_ShouldHandleDuplicateStops() {
        // Given
        elevator.setStops(new HashSet<>());

        // When
        elevator.getStops().add(5);
        elevator.getStops().add(5); // Duplicate

        // Then
        assertEquals(1, elevator.getStops().size()); // Set should contain only unique values
        assertTrue(elevator.getStops().contains(5));
    }

    @Test
    void elevator_ShouldAllowRemovingStops() {
        // Given
        elevator.setStops(new HashSet<>());
        elevator.getStops().add(3);
        elevator.getStops().add(7);

        // When
        elevator.getStops().remove(3);

        // Then
        assertEquals(1, elevator.getStops().size());
        assertFalse(elevator.getStops().contains(3));
        assertTrue(elevator.getStops().contains(7));
    }

    @Test
    void elevator_ShouldHaveValidToString() {
        // Given
        elevator.setId(1L);
        elevator.setMaxCapacity(10);
        elevator.setCurrentFloor(5);

        // When
        String toString = elevator.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("1"));
        assertTrue(toString.contains("10"));
        assertTrue(toString.contains("5"));
        // Should exclude requests due to @ToString(exclude = "requests")
        assertFalse(toString.contains("requests"));
    }

    @Test
    void elevator_ShouldSupportEqualsAndHashCode() {
        // Given
        Elevator elevator1 = new Elevator();
        elevator1.setId(1L);
        elevator1.setMaxCapacity(10);

        Elevator elevator2 = new Elevator();
        elevator2.setId(1L);
        elevator2.setMaxCapacity(10);

        Elevator elevator3 = new Elevator();
        elevator3.setId(2L);
        elevator3.setMaxCapacity(10);

        // Then
        assertEquals(elevator1, elevator2);
        assertNotEquals(elevator1, elevator3);
        assertEquals(elevator1.hashCode(), elevator2.hashCode());
    }
}