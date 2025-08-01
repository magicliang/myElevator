package com.elevator;

import com.elevator.entity.Elevator;
import com.elevator.entity.Request;
import com.elevator.model.Direction;
import com.elevator.model.State;
import com.elevator.repository.ElevatorRepository;
import com.elevator.repository.RequestRepository;
import com.elevator.service.ElevatorService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ElevatorSystemIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ElevatorService elevatorService;

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private RequestRepository requestRepository;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        elevatorRepository.deleteAll();
        requestRepository.deleteAll();
    }

    @Test
    public void testCreateElevator() {
        given()
            .contentType(ContentType.JSON)
            .param("maxCapacity", 8)
        .when()
            .post("/api/elevators")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("maxCapacity", equalTo(8))
            .body("currentFloor", equalTo(1))
            .body("direction", equalTo("IDLE"))
            .body("state", equalTo("IDLE"));
    }

    @Test
    public void testCreateRequest() {
        // 创建电梯
        Elevator elevator = elevatorService.createElevator(10);
        
        // 创建请求
        given()
            .contentType(ContentType.JSON)
            .param("originFloor", 3)
            .param("destinationFloor", 7)
        .when()
            .post("/api/elevators/" + elevator.getId() + "/requests")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("originFloor", equalTo(3))
            .body("destinationFloor", equalTo(7))
            .body("direction", equalTo("UP"))
            .body("completed", equalTo(false));
    }

    @Test
    public void testElevatorMovement() {
        // 创建电梯
        Elevator elevator = elevatorService.createElevator(10);
        
        // 创建请求：从3楼到7楼
        Request request = elevatorService.createRequest(3, 7);
        
        // 验证初始状态
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(Direction.IDLE, elevator.getDirection());
        
        // 处理第一步：移动到3楼
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(3, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());
        
        // 处理后续步骤：移动到7楼
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(7, elevator.getCurrentFloor());
        
        // 验证请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    public void testMultipleRequests() {
        // 创建电梯
        Elevator elevator = elevatorService.createElevator(10);
        
        // 创建多个请求
        elevatorService.createRequest(2, 5);
        elevatorService.createRequest(3, 8);
        elevatorService.createRequest(1, 4);
        
        // 处理所有请求
        for (int i = 0; i < 10; i++) {
            elevatorService.processNextStep(elevator.getId());
        }
        
        // 验证所有请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    public void testOptimalElevatorSelection() {
        // 创建两个电梯
        Elevator elevator1 = elevatorService.createElevator(10);
        elevator1.setCurrentFloor(1);
        elevatorRepository.save(elevator1);
        
        Elevator elevator2 = elevatorService.createElevator(10);
        elevator2.setCurrentFloor(8);
        elevatorRepository.save(elevator2);
        
        // 创建从5楼出发的请求
        Request request = elevatorService.createRequest(5, 10);
        
        // 验证选择了更近的电梯（elevator1在1楼，比elevator2在8楼更近5楼）
        assertEquals(elevator1.getId(), request.getElevator().getId());
    }

    @Test
    public void testElevatorCapacity() {
        // 创建小容量电梯
        Elevator elevator = elevatorService.createElevator(1);
        elevator.setCurrentLoad(1);
        elevatorRepository.save(elevator);
        
        // 创建另一个电梯
        Elevator elevator2 = elevatorService.createElevator(10);
        
        // 创建请求，应该分配给有空位的电梯
        Request request = elevatorService.createRequest(3, 7);
        
        // 验证选择了有空位的电梯
        assertEquals(elevator2.getId(), request.getElevator().getId());
    }

    @Test
    public void testGetAllElevators() {
        elevatorService.createElevator(10);
        elevatorService.createElevator(8);
        
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/elevators")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }

    @Test
    public void testGetPendingRequests() {
        Elevator elevator = elevatorService.createElevator(10);
        elevatorService.createRequest(2, 5);
        elevatorService.createRequest(3, 7);
        
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/elevators/" + elevator.getId() + "/requests")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }
}