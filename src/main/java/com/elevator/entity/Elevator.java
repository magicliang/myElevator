package com.elevator.entity;

import com.elevator.model.Direction;
import com.elevator.model.State;
import javax.persistence.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "elevators")
@Data
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

    // 新增停靠点集合
    @ElementCollection
    private Set<Integer> stops = new HashSet<>();

    @OneToMany(mappedBy = "elevator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Request> requests = new HashSet<>();
}