package com.elevator.entity;

import com.elevator.model.Direction;
import com.elevator.model.State;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "elevators")
@Getter
@Setter
@ToString(exclude = "requests")
public class Elevator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int maxCapacity;
    private int currentLoad = 0;
    private int currentFloor = 1;

    @Enumerated(EnumType.STRING)
    private Direction direction = Direction.IDLE;

    @Enumerated(EnumType.STRING)
    private State state = State.IDLE;

    @OneToMany(mappedBy = "elevator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Request> requests = new HashSet<>();
}