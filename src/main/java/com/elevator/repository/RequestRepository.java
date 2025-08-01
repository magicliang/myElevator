package com.elevator.repository;

import com.elevator.entity.Request;
import com.elevator.model.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByCompletedFalse();
    List<Request> findByElevatorIdAndCompletedFalse(Long elevatorId);
    List<Request> findByOriginFloorAndDirectionAndCompletedFalse(int floor, Direction direction);
}