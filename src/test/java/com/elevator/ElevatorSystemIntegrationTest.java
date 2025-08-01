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
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
        // 手动清理数据而不是依赖事务回滚
        elevatorRepository.deleteAll();
        requestRepository.deleteAll();
    }

    @Test
    public void testCreateElevator() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("maxCapacity", 8)
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
            .contentType(ContentType.URLENC)
            .formParam("originFloor", 3)
            .formParam("destinationFloor", 7)
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

        // 处理：移动到3楼接载
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(3, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());

        // 处理：移动到7楼送达
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(7, elevator.getCurrentFloor());

        // 处理：完成下车逻辑
        elevatorService.processNextStep(elevator.getId());

        // 验证请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    public void testMultipleRequests() {
        // 创建电梯
        Elevator elevator = elevatorService.createElevator(10);

        // 创建多个请求并关联到电梯
        Request request1 = new Request();
        request1.setOriginFloor(2);
        request1.setDestinationFloor(5);
        request1.setDirection(Direction.UP);
        request1.setElevator(elevator);
        requestRepository.save(request1);
        // 添加起始楼层到停靠点
        elevator.getStops().add(request1.getOriginFloor());

        Request request2 = new Request();
        request2.setOriginFloor(3);
        request2.setDestinationFloor(8);
        request2.setDirection(Direction.UP);
        request2.setElevator(elevator);
        requestRepository.save(request2);
        // 添加起始楼层到停靠点
        elevator.getStops().add(request2.getOriginFloor());

        Request request3 = new Request();
        request3.setOriginFloor(1);
        request3.setDestinationFloor(4);
        request3.setDirection(Direction.UP);
        request3.setElevator(elevator);
        requestRepository.save(request3);
        // 添加起始楼层到停靠点
        elevator.getStops().add(request3.getOriginFloor());

        // 保存电梯更新
        elevatorRepository.save(elevator);

        // 处理所有请求
        for (int i = 0; i < 20; i++) {
            elevatorService.processNextStep(elevator.getId());
        }

        // 验证所有请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    public void testOptimalElevatorSelection() {
        // 创建两个电梯并设置楼层
        Elevator elevator1 = elevatorService.createElevator(10);
        elevator1.setCurrentFloor(1);
        elevator1 = elevatorRepository.save(elevator1);

        Elevator elevator2 = elevatorService.createElevator(10);
        elevator2.setCurrentFloor(8);
        elevator2 = elevatorRepository.save(elevator2);

        // 创建从5楼出发的请求
        Request request = elevatorService.createRequest(5, 10);

        // 验证选择了更近的电梯（elevator2在8楼，比elevator1在1楼更近5楼）
        assertEquals(elevator2.getId(), request.getElevator().getId());
    }

    @Test
    public void testElevatorCapacity() {
        // 创建满电梯
        Elevator elevator1 = elevatorService.createElevator(1);
        elevator1.setCurrentLoad(1);
        elevatorRepository.save(elevator1);

        // 创建有空位的电梯
        Elevator elevator2 = elevatorService.createElevator(10);

        // 创建请求，应该分配给有空位的电梯
        Request request = elevatorService.createRequest(3, 7);

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

    @Test
    public void testSameFloorRequest() {
        // 测试同楼层请求
        Elevator elevator = elevatorService.createElevator(10);
        Request request = elevatorService.createRequest(5, 5);

        // 处理请求 - 需要多次step才能从1楼到5楼并完成
        for (int i = 0; i < 15; i++) {
            elevatorService.processNextStep(elevator.getId());
            elevator = elevatorService.getElevator(elevator.getId());

            // 检查请求是否已完成
            List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
            if (pendingRequests.isEmpty()) {
                break;
            }
        }

        // 验证电梯到达5楼
        assertEquals(5, elevator.getCurrentFloor());

        // 验证请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty(), "Same floor request should be completed after reaching the floor");
    }

    @Test
    public void testEmptyElevatorSelection() {
        // 测试没有电梯时的处理
        elevatorRepository.deleteAll();

        // 应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            elevatorService.createRequest(3, 7);
        });
    }

    @Test
    public void testFullElevatorHandling() {
        // 创建满电梯
        Elevator elevator1 = elevatorService.createElevator(1);
        elevator1.setCurrentLoad(1);
        elevatorRepository.save(elevator1);

        // 创建有空位的电梯
        Elevator elevator2 = elevatorService.createElevator(10);

        // 创建请求，应该分配给有空位的电梯
        Request request = elevatorService.createRequest(3, 7);

        assertEquals(elevator2.getId(), request.getElevator().getId());
    }

    @Test
    public void testDownDirectionRequest() {
        // 测试向下请求
        Elevator elevator = elevatorService.createElevator(10);
        elevator.setCurrentFloor(8);
        elevatorRepository.save(elevator);

        Request request = elevatorService.createRequest(8, 3);

        // 验证方向正确
        assertEquals(Direction.DOWN, request.getDirection());

        // 处理请求
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());

        assertEquals(3, elevator.getCurrentFloor());

        // 验证请求已完成
        List<Request> pendingRequests = elevatorService.getPendingRequests(elevator.getId());
        assertTrue(pendingRequests.isEmpty());
    }

    @Test
    public void testConcurrentRequests() {
        // 测试并发请求处理
        Elevator elevator = elevatorService.createElevator(10);

        // 创建多个并发请求
        Request request1 = elevatorService.createRequest(2, 5);
        Request request2 = elevatorService.createRequest(3, 7);
        Request request3 = elevatorService.createRequest(1, 4);

        // 验证所有请求都分配给同一个电梯
        assertEquals(elevator.getId(), request1.getElevator().getId());
        assertEquals(elevator.getId(), request2.getElevator().getId());
        assertEquals(elevator.getId(), request3.getElevator().getId());

        // 验证停靠点集合包含所有起始楼层
        Set<Integer> stops = elevator.getStops();
        assertTrue(stops.contains(1), "Should contain floor 1");
        assertTrue(stops.contains(2), "Should contain floor 2");
        assertTrue(stops.contains(3), "Should contain floor 3");

        // 验证停靠点数量（可能有重复，所以使用>=）
        assertTrue(stops.size() >= 3, "Should have at least 3 stops");
    }

    @Test
    public void testElevatorStateTransitions() {
        // 测试电梯状态转换
        Elevator elevator = elevatorService.createElevator(10);

        // 初始状态
        assertEquals(State.IDLE, elevator.getState());
        assertEquals(Direction.IDLE, elevator.getDirection());

        // 创建请求
        elevatorService.createRequest(5, 8);

        // 第一次step：开始移动
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(State.MOVING, elevator.getState());
        assertEquals(Direction.UP, elevator.getDirection());

        // 继续处理直到到达
        for (int i = 0; i < 10; i++) {
            elevatorService.processNextStep(elevator.getId());
        }

        // 验证最终状态
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(State.IDLE, elevator.getState());
        assertEquals(Direction.IDLE, elevator.getDirection());
    }

    @Test
    public void testEdgeCaseFloorLimits() {
        // 测试边界楼层
        Elevator elevator = elevatorService.createElevator(10);

        // 测试最低楼层
        Request request1 = elevatorService.createRequest(1, 2);
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(1, elevator.getCurrentFloor());

        // 测试最高楼层（假设系统支持到10层）
        Request request2 = elevatorService.createRequest(10, 9);
        elevatorService.processNextStep(elevator.getId());
        elevator = elevatorService.getElevator(elevator.getId());
        assertEquals(10, elevator.getCurrentFloor());
    }
}