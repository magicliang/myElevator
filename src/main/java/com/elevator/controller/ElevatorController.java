package com.elevator.controller;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.service.ElevatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elevators")
@RequiredArgsConstructor
public class ElevatorController {
    
    private final ElevatorService elevatorService;
    
    @PostMapping
    public ResponseEntity<Elevator> createElevator(@RequestParam(defaultValue = "10") int maxCapacity) {
        Elevator elevator = elevatorService.createElevator(maxCapacity);
        return ResponseEntity.ok(elevator);
    }
    
    @PostMapping("/{elevatorId}/requests")
    public ResponseEntity<Request> createRequest(
            @PathVariable Long elevatorId,
            @RequestParam int originFloor,
            @RequestParam int destinationFloor) {
        Request request = elevatorService.createRequest(originFloor, destinationFloor);
        return ResponseEntity.ok(request);
    }
    
    @PostMapping("/{elevatorId}/step")
    public ResponseEntity<Void> processNextStep(@PathVariable Long elevatorId) {
        elevatorService.processNextStep(elevatorId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    public ResponseEntity<List<Elevator>> getAllElevators() {
        return ResponseEntity.ok(elevatorService.getAllElevators());
    }
    
    @GetMapping("/{elevatorId}")
    public ResponseEntity<Elevator> getElevator(@PathVariable Long elevatorId) {
        return ResponseEntity.ok(elevatorService.getElevator(elevatorId));
    }
    
    @GetMapping("/{elevatorId}/requests")
    public ResponseEntity<List<Request>> getPendingRequests(@PathVariable Long elevatorId) {
        return ResponseEntity.ok(elevatorService.getPendingRequests(elevatorId));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
