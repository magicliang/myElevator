package com.elevator.service;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.model.Direction;
import com.elevator.model.State;
import com.elevator.repository.ElevatorRepository;
import com.elevator.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElevatorService {

    private final ElevatorRepository elevatorRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public Elevator createElevator(int maxCapacity) {
        Elevator elevator = new Elevator();
        elevator.setMaxCapacity(maxCapacity);
        return elevatorRepository.save(elevator);
    }

    @Transactional
    public Request createRequest(int originFloor, int destinationFloor) {
        Request request = new Request();
        request.setOriginFloor(originFloor);
        request.setDestinationFloor(destinationFloor);
        request.setDirection(destinationFloor > originFloor ? Direction.UP : Direction.DOWN);

        Elevator optimalElevator = findOptimalElevator(request);
        request.setElevator(optimalElevator);

        // 将起始楼层添加到电梯的停靠点集合
        optimalElevator.getStops().add(originFloor);
        elevatorRepository.save(optimalElevator); // 保存电梯的停靠点更新

        return requestRepository.save(request);
    }

    public Elevator findOptimalElevator(Request request) {
        List<Elevator> elevators = elevatorRepository.findAll();

        log.info("Finding optimal elevator for request: origin={}, dest={}",
                 request.getOriginFloor(), request.getDestinationFloor());

        Elevator selected = elevators.stream()
                .min(Comparator.comparingInt(elevator -> calculateCost(elevator, request)))
                .orElseThrow(() -> new RuntimeException("No elevators available"));

        log.info("Selected elevator: id={}, currentFloor={}, cost={}",
                 selected.getId(), selected.getCurrentFloor(),
                 calculateCost(selected, request));

        return selected;
    }

    private int calculateCost(Elevator elevator, Request request) {
        // 如果电梯已满，返回最大成本
        if (elevator.getCurrentLoad() >= elevator.getMaxCapacity()) {
            log.info("Elevator {} is full, cost=MAX", elevator.getId());
            return Integer.MAX_VALUE;
        }

        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();
        int requestFloor = request.getOriginFloor();

        log.info("Calculating cost - elevator: {}, currentFloor: {}, direction: {}, requestFloor: {}",
                 elevator.getId(), currentFloor, currentDirection, requestFloor);

        // 电梯空闲状态
        if (currentDirection == Direction.IDLE) {
            int cost = Math.abs(currentFloor - requestFloor);
            log.info("IDLE state, cost: {}", cost);
            return cost;
        }

        // 同方向
        if ((currentDirection == Direction.UP && request.getDirection() == Direction.UP && requestFloor >= currentFloor) ||
            (currentDirection == Direction.DOWN && request.getDirection() == Direction.DOWN && requestFloor <= currentFloor)) {
            int cost = Math.abs(currentFloor - requestFloor);
            log.info("Same direction, cost: {}", cost);
            return cost;
        }

        // 反方向或需要绕行
        int cost;
        if (currentDirection == Direction.UP) {
            cost = (10 - currentFloor) + (10 - requestFloor); // 假设最高10层
            log.info("UP direction, need to turn around, cost: {}", cost);
        } else {
            cost = (currentFloor - 1) + (requestFloor - 1); // 假设最低1层
            log.info("DOWN direction, need to turn around, cost: {}", cost);
        }
        return cost;
    }

    @Transactional
    public void processNextStep(Long elevatorId) {
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));

        List<Request> pendingRequests = requestRepository.findByElevatorIdAndCompletedFalse(elevatorId);

        if (pendingRequests.isEmpty()) {
            elevator.setDirection(Direction.IDLE);
            elevator.setState(State.IDLE);
            elevatorRepository.save(elevator);
            return;
        }

        // 使用LOOK算法处理请求
        processLookAlgorithm(elevator, pendingRequests);
    }

    private void processLookAlgorithm(Elevator elevator, List<Request> requests) {
        int currentFloor = elevator.getCurrentFloor();
        Direction direction = elevator.getDirection();

        log.info("Processing LOOK algorithm - Elevator: {}, Current floor: {}, Direction: {}, Pending requests: {}",
                 elevator.getId(), currentFloor, direction, requests.size());

        // 直接使用电梯的停靠点集合
        Set<Integer> stops = new HashSet<>(elevator.getStops());
        log.info("All stops from elevator: {}", stops);

        // 根据当前方向确定下一个停靠楼层
        Optional<Integer> nextStop = findNextStop(currentFloor, direction, stops);

        log.info("Next stop: {}", nextStop);

        if (nextStop.isPresent()) {
            int targetFloor = nextStop.get();

            // 如果方向是IDLE，根据目标楼层设置方向
            if (direction == Direction.IDLE && targetFloor != currentFloor) {
                direction = targetFloor > currentFloor ? Direction.UP : Direction.DOWN;
                elevator.setDirection(direction);
                log.info("Setting initial direction to: {}", direction);
            }

            if (targetFloor == currentFloor) {
                // 到达目标楼层
                log.info("Arrived at target floor: {}", currentFloor);
                elevator.setState(State.STOPPED);
                openDoor(elevator);
                handleFloorArrival(elevator, currentFloor);

                // 关键修复：处理完当前楼层后，如果有剩余停靠点，继续处理
                if (!elevator.getStops().isEmpty()) {
                    log.info("Still have stops remaining: {}, continuing processing", elevator.getStops());
                    // 递归调用继续处理下一个停靠点
                    processLookAlgorithm(elevator, requests);
                    return;
                }
            } else {
                // 移动电梯
                log.info("Moving elevator from {} to {}", currentFloor, targetFloor);
                elevator.setState(State.MOVING);
                elevator.setDirection(targetFloor > currentFloor ? Direction.UP : Direction.DOWN);
                elevator.setCurrentFloor(targetFloor);
            }
        } else {
            // 没有停靠点，设置为空闲
            log.info("No stops available, setting to IDLE");
            elevator.setDirection(Direction.IDLE);
            elevator.setState(State.IDLE);
        }

        elevatorRepository.save(elevator);
    }

    private Optional<Integer> findNextStop(int currentFloor, Direction direction, Set<Integer> stops) {
        log.info("Finding next stop - currentFloor: {}, direction: {}, stops: {}", currentFloor, direction, stops);

        if (stops.isEmpty()) {
            log.info("No stops available");
            return Optional.empty();
        }

        // 如果方向是IDLE，选择最近的请求
        if (direction == Direction.IDLE) {
            Optional<Integer> nearest = stops.stream()
                    .min(Comparator.comparingInt(floor -> Math.abs(floor - currentFloor)));
            log.info("Direction is IDLE, choosing nearest stop: {}", nearest);
            return nearest;
        }

        if (direction == Direction.UP) {
            Optional<Integer> next = stops.stream()
                    .filter(floor -> floor >= currentFloor)
                    .min(Integer::compareTo);
            if (next.isPresent()) {
                log.info("Next stop UP: {}", next);
                return next;
            } else {
                // 没有向上的停靠点，改变方向
                Optional<Integer> downNext = stops.stream()
                        .filter(floor -> floor <= currentFloor)
                        .max(Integer::compareTo);
                log.info("No more UP stops, changing to DOWN: {}", downNext);
                return downNext;
            }
        } else if (direction == Direction.DOWN) {
            Optional<Integer> next = stops.stream()
                    .filter(floor -> floor <= currentFloor)
                    .max(Integer::compareTo);
            if (next.isPresent()) {
                log.info("Next stop DOWN: {}", next);
                return next;
            } else {
                // 没有向下的停靠点，改变方向
                Optional<Integer> upNext = stops.stream()
                        .filter(floor -> floor >= currentFloor)
                        .min(Integer::compareTo);
                log.info("No more DOWN stops, changing to UP: {}", upNext);
                return upNext;
            }
        }

        log.info("No valid direction, returning empty");
        return Optional.empty();
    }

    private void openDoor(Elevator elevator) {
        elevator.setState(State.DOOR_OPEN);
        log.info("Elevator {} door opened at floor {}", elevator.getId(), elevator.getCurrentFloor());
    }

    private void handleFloorArrival(Elevator elevator, int floor) {
        List<Request> requests = requestRepository.findByElevatorIdAndCompletedFalse(elevator.getId());

        // 处理到达该楼层的请求
        for (Request request : requests) {
            if (request.getOriginFloor() == floor && !request.isPassengerPickedUp()) {
                log.info("Passenger picked up at floor {} by elevator {}", floor, elevator.getId());
                request.setPassengerPickedUp(true);
                elevator.setCurrentLoad(elevator.getCurrentLoad() + 1);

                // 关键修复：乘客接上后立即将目的地楼层添加到停靠点
                elevator.getStops().add(request.getDestinationFloor());
                log.info("Added destination floor {} to stops after pickup", request.getDestinationFloor());
            }

            if (request.getDestinationFloor() == floor && request.isPassengerPickedUp() && !request.isCompleted()) {
                log.info("Passenger dropped off at floor {} by elevator {}", floor, elevator.getId());
                request.setCompleted(true);
                request.setCompletedAt(new Date());
                elevator.setCurrentLoad(Math.max(0, elevator.getCurrentLoad() - 1));
            }
        }

        // 关键修复：从停靠点集合中移除已处理的当前楼层
        elevator.getStops().remove(floor);
        log.info("Removed floor {} from stops after processing", floor);

        requestRepository.saveAll(requests);
        elevatorRepository.save(elevator);
    }

    public List<Elevator> getAllElevators() {
        return elevatorRepository.findAll();
    }

    public Elevator getElevator(Long id) {
        return elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
    }

    public List<Request> getPendingRequests(Long elevatorId) {
        return requestRepository.findByElevatorIdAndCompletedFalse(elevatorId);
    }
}