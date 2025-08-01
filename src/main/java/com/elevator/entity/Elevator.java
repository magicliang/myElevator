package com.elevator.entity;

import com.elevator.model.Direction;
import com.elevator.model.State;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "elevators")
@Data
public class Elevator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int currentFloor;
    
    @Enumerated(EnumType.STRING)
    private Direction direction;
    
    @Enumerated(EnumType.STRING)
    private State state;
    
    private int maxCapacity;
    private int currentLoad;
    
    @OneToMany(mappedBy = "elevator", cascade = CascadeType.ALL)
    private Set<Request> requests = new HashSet<>();
    
    public Elevator() {
        this.currentFloor = 1;
        this.direction = Direction.IDLE;
        this.state = State.IDLE;
        this.maxCapacity = 10;
        this.currentLoad = 0;
    }
}