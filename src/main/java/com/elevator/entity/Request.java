package com.elevator.entity;

import com.elevator.model.Direction;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "requests")
@Data
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int originFloor;
    private int destinationFloor;
    
    @Enumerated(EnumType.STRING)
    private Direction direction;
    
    private boolean completed;
    
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