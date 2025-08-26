package com.elevator.controller;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.model.Direction;
import com.elevator.model.State;
import com.elevator.service.ElevatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ElevatorController.class)
class ElevatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ElevatorService elevatorService;

    @Autowired
    private ObjectMapper objectMapper;

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
    }

    @Test
    void createElevator_ShouldReturnCreatedElevator() throws Exception {
        // Given
        when(elevatorService.createElevator(anyInt())).thenReturn(testElevator);

        // When & Then
        mockMvc.perform(post("/api/elevators")
                .param("maxCapacity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maxCapacity").value(10))
                .andExpect(jsonPath("$.currentFloor").value(1))
                .andExpect(jsonPath("$.direction").value("IDLE"))
                .andExpect(jsonPath("$.state").value("IDLE"));

        verify(elevatorService).createElevator(10);
    }

    @Test
    void createElevator_ShouldUseDefaultCapacityWhenNotProvided() throws Exception {
        // Given
        when(elevatorService.createElevator(anyInt())).thenReturn(testElevator);

        // When & Then
        mockMvc.perform(post("/api/elevators"))
                .andExpect(status().isOk());

        verify(elevatorService).createElevator(10); // Default capacity
    }

    @Test
    void createRequest_ShouldReturnCreatedRequest() throws Exception {
        // Given
        when(elevatorService.createRequest(anyInt(), anyInt())).thenReturn(testRequest);

        // When & Then
        mockMvc.perform(post("/api/elevators/1/requests")
                .param("originFloor", "3")
                .param("destinationFloor", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originFloor").value(3))
                .andExpect(jsonPath("$.destinationFloor").value(7))
                .andExpect(jsonPath("$.direction").value("UP"))
                .andExpect(jsonPath("$.completed").value(false));

        verify(elevatorService).createRequest(3, 7);
    }

    @Test
    void processNextStep_ShouldReturnOk() throws Exception {
        // Given
        doNothing().when(elevatorService).processNextStep(anyLong());

        // When & Then
        mockMvc.perform(post("/api/elevators/1/step"))
                .andExpect(status().isOk());

        verify(elevatorService).processNextStep(1L);
    }

    @Test
    void getAllElevators_ShouldReturnElevatorList() throws Exception {
        // Given
        List<Elevator> elevators = Arrays.asList(testElevator);
        when(elevatorService.getAllElevators()).thenReturn(elevators);

        // When & Then
        mockMvc.perform(get("/api/elevators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(elevatorService).getAllElevators();
    }

    @Test
    void getElevator_ShouldReturnSpecificElevator() throws Exception {
        // Given
        when(elevatorService.getElevator(1L)).thenReturn(testElevator);

        // When & Then
        mockMvc.perform(get("/api/elevators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maxCapacity").value(10));

        verify(elevatorService).getElevator(1L);
    }

    @Test
    void getPendingRequests_ShouldReturnRequestList() throws Exception {
        // Given
        List<Request> requests = Arrays.asList(testRequest);
        when(elevatorService.getPendingRequests(1L)).thenReturn(requests);

        // When & Then
        mockMvc.perform(get("/api/elevators/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(elevatorService).getPendingRequests(1L);
    }

    @Test
    void createElevator_ShouldHandleServiceException() throws Exception {
        // Given
        when(elevatorService.createElevator(anyInt())).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/elevators")
                .param("maxCapacity", "10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getElevator_ShouldHandleNotFound() throws Exception {
        // Given
        when(elevatorService.getElevator(999L)).thenThrow(new RuntimeException("Elevator not found"));

        // When & Then
        mockMvc.perform(get("/api/elevators/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createRequest_ShouldValidateParameters() throws Exception {
        // When & Then - Missing parameters should result in bad request
        mockMvc.perform(post("/api/elevators/1/requests"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllElevators_ShouldReturnEmptyListWhenNoElevators() throws Exception {
        // Given
        when(elevatorService.getAllElevators()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/elevators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPendingRequests_ShouldReturnEmptyListWhenNoRequests() throws Exception {
        // Given
        when(elevatorService.getPendingRequests(1L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/elevators/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}