package com.elevator.entity;

import com.elevator.model.Direction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Entity
@Table(name = "requests")
@Getter
@Setter
@ToString(exclude = "elevator")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int originFloor;
    private int destinationFloor;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private boolean completed;

    private boolean passengerPickedUp;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @ManyToOne
    @JoinColumn(name = "elevator_id")
    private Elevator elevator;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}