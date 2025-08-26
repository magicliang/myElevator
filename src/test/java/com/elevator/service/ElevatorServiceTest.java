package com.elevator.service;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.model.Direction;
import com.elevator.model.State;
import com.elevator.repository.ElevatorRepository;
import com.elevator.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElevatorServiceTest {

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private ElevatorService elevatorService;

    private Elevator testElevator;
    private Request testRequest;

    @BeforeEach
    void setUp() {
        testElevator = new Elevator();
        testElevator.setId(1L);
        testElevator.setMaxCapacity(10);
        testElevator.setCurrentFloor(1);
        testElevator.setDirection(Direction.IDLE);
        testElevator.setState(State.IDLE);
        testElevator.setStops(new HashSet<>());

        testRequest = new Request();
        testRequest.setId(1L);
        testRequest.setOriginFloor(3);
        testRequest.setDestinationFloor(7);
        testRequest.setDirection(Direction.UP);
        testRequest.setCompleted(false);
        testRequest.setPassengerPickedUp(false);
    }

    @Test
    void createElevator_ShouldReturnElevatorWithCorrectCapacity() {
        // Given
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(testElevator);

        // When
        Elevator result = elevatorService.createElevator(10);

        // Then
        assertNotNull(result);
        assertEquals(10, result.getMaxCapacity());
        verify(elevatorRepository).save(any(Elevator.class));
    }

    @Test
    void findOptimalElevator_ShouldSelectClosestElevator() {
        // Given
        Elevator elevator1 = new Elevator();
        elevator1.setId(1L);
        elevator1.setCurrentFloor(1);
        elevator1.setMaxCapacity(10);
        elevator1.setCurrentLoad(0);

        Elevator elevator2 = new Elevator();
        elevator2.setId(2L);
        elevator2.setCurrentFloor(5);
        elevator2.setMaxCapacity(10);
        elevator2.setCurrentLoad(0);

        when(elevatorRepository.findAll()).thenReturn(Arrays.asList(elevator1, elevator2));

        Request request = new Request();
        request.setOriginFloor(6);
        request.setDestinationFloor(10);
        request.setDirection(Direction.UP);

        // When
        Elevator result = elevatorService.findOptimalElevator(request);

        // Then
        assertEquals(2L, result.getId()); // elevator2 is closer to floor 6
    }

    @Test
    void findOptimalElevator_ShouldAvoidFullElevator() {
        // Given
        Elevator fullElevator = new Elevator();
        fullElevator.setId(1L);
        fullElevator.setCurrentFloor(3);
        fullElevator.setMaxCapacity(5);
        fullElevator.setCurrentLoad(5); // Full

        Elevator availableElevator = new Elevator();
        availableElevator.setId(2L);
        availableElevator.setCurrentFloor(8);
        availableElevator.setMaxCapacity(10);
        availableElevator.setCurrentLoad(2);

        when(elevatorRepository.findAll()).thenReturn(Arrays.asList(fullElevator, availableElevator));

        Request request = new Request();
        request.setOriginFloor(4);
        request.setDestinationFloor(6);
        request.setDirection(Direction.UP);

        // When
        Elevator result = elevatorService.findOptimalElevator(request);

        // Then
        assertEquals(2L, result.getId()); // Should select available elevator
    }

    @Test
    void findOptimalElevator_ShouldThrowExceptionWhenNoElevatorsAvailable() {
        // Given
        when(elevatorRepository.findAll()).thenReturn(Collections.emptyList());

        Request request = new Request();
        request.setOriginFloor(3);
        request.setDestinationFloor(7);

        // When & Then
        assertThrows(RuntimeException.class, () -> elevatorService.findOptimalElevator(request));
    }

    @Test
    void createRequest_ShouldSetCorrectDirection() {
        // Given
        when(elevatorRepository.findAll()).thenReturn(Collections.singletonList(testElevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(testElevator);
        when(requestRepository.save(any(Request.class))).thenReturn(testRequest);

        // When - UP direction
        Request upRequest = elevatorService.createRequest(3, 7);

        // Then
        assertEquals(Direction.UP, upRequest.getDirection());

        // When - DOWN direction
        Request downRequest = elevatorService.createRequest(8, 2);

        // Then
        assertEquals(Direction.DOWN, downRequest.getDirection());
    }

    @Test
    void processNextStep_ShouldSetElevatorToIdleWhenNoPendingRequests() {
        // Given
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(testElevator));
        when(requestRepository.findByElevatorIdAndCompletedFalse(1L)).thenReturn(Collections.emptyList());
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(testElevator);

        // When
        elevatorService.processNextStep(1L);

        // Then
        verify(elevatorRepository).save(argThat(elevator -> 
            elevator.getDirection() == Direction.IDLE && 
            elevator.getState() == State.IDLE
        ));
    }

    @Test
    void processNextStep_ShouldThrowExceptionWhenElevatorNotFound() {
        // Given
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> elevatorService.processNextStep(999L));
    }

    @Test
    void getAllElevators_ShouldReturnAllElevators() {
        // Given
        List<Elevator> elevators = Arrays.asList(testElevator);
        when(elevatorRepository.findAll()).thenReturn(elevators);

        // When
        List<Elevator> result = elevatorService.getAllElevators();

        // Then
        assertEquals(1, result.size());
        assertEquals(testElevator, result.get(0));
    }

    @Test
    void getElevator_ShouldReturnElevatorWhenExists() {
        // Given
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(testElevator));

        // When
        Elevator result = elevatorService.getElevator(1L);

        // Then
        assertEquals(testElevator, result);
    }

    @Test
    void getElevator_ShouldThrowExceptionWhenNotExists() {
        // Given
        when(elevatorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> elevatorService.getElevator(999L));
    }

    @Test
    void getPendingRequests_ShouldReturnPendingRequestsForElevator() {
        // Given
        List<Request> pendingRequests = Arrays.asList(testRequest);
        when(requestRepository.findByElevatorIdAndCompletedFalse(1L)).thenReturn(pendingRequests);

        // When
        List<Request> result = elevatorService.getPendingRequests(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }

    @Test
    void createRequest_ShouldAddOriginFloorToElevatorStops() {
        // Given
        testElevator.setStops(new HashSet<>());
        when(elevatorRepository.findAll()).thenReturn(Collections.singletonList(testElevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(testElevator);
        when(requestRepository.save(any(Request.class))).thenReturn(testRequest);

        // When
        elevatorService.createRequest(5, 8);

        // Then
        verify(elevatorRepository).save(argThat(elevator -> 
            elevator.getStops().contains(5)
        ));
    }

    @Test
    void findOptimalElevator_ShouldConsiderElevatorDirection() {
        // Given
        Elevator upElevator = new Elevator();
        upElevator.setId(1L);
        upElevator.setCurrentFloor(3);
        upElevator.setDirection(Direction.UP);
        upElevator.setMaxCapacity(10);
        upElevator.setCurrentLoad(0);

        Elevator idleElevator = new Elevator();
        idleElevator.setId(2L);
        idleElevator.setCurrentFloor(7);
        idleElevator.setDirection(Direction.IDLE);
        idleElevator.setMaxCapacity(10);
        idleElevator.setCurrentLoad(0);

        when(elevatorRepository.findAll()).thenReturn(Arrays.asList(upElevator, idleElevator));

        Request upRequest = new Request();
        upRequest.setOriginFloor(5);
        upRequest.setDestinationFloor(8);
        upRequest.setDirection(Direction.UP);

        // When
        Elevator result = elevatorService.findOptimalElevator(upRequest);

        // Then
        assertEquals(1L, result.getId()); // Should prefer elevator going in same direction
    }
}