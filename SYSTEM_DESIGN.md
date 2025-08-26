# 电梯系统设计文档

## 1. 系统概述

### 1.1 问题陈述
设计一个多电梯调度系统，能够高效处理乘客请求，优化等待时间和能耗。

### 1.2 功能需求
- 处理乘客的上下楼请求
- 多电梯协调调度
- 实时状态监控
- 负载均衡
- 故障处理

### 1.3 非功能需求
- **可用性**: 99.9% 系统可用性
- **性能**: 平均响应时间 < 100ms
- **可扩展性**: 支持100+电梯
- **并发性**: 支持1000+并发请求
- **一致性**: 强一致性保证

## 2. 系统架构

### 2.1 高层架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   API Gateway   │    │   Monitoring    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Elevator Service│    │ Request Service │    │ Analytics Service│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │    Database     │
                    └─────────────────┘
```

### 2.2 核心组件

#### 2.2.1 电梯控制器 (ElevatorController)
- **职责**: 处理HTTP请求，参数验证
- **接口**:
  - `POST /api/elevators` - 创建电梯
  - `POST /api/elevators/{id}/requests` - 创建请求
  - `POST /api/elevators/{id}/step` - 处理下一步
  - `GET /api/elevators` - 获取所有电梯
  - `GET /api/elevators/{id}` - 获取特定电梯
  - `GET /api/elevators/{id}/requests` - 获取待处理请求

#### 2.2.2 电梯服务 (ElevatorService)
- **职责**: 核心业务逻辑，调度算法
- **关键方法**:
  - `findOptimalElevator()` - 最优电梯选择
  - `processNextStep()` - LOOK算法实现
  - `calculateCost()` - 成本计算

#### 2.2.3 数据模型
```java
// 电梯实体
class Elevator {
    Long id;
    int maxCapacity;
    int currentLoad;
    int currentFloor;
    Direction direction;  // UP, DOWN, IDLE
    State state;         // MOVING, STOPPED, DOOR_OPEN, IDLE
    Set<Integer> stops;  // 停靠楼层集合
}

// 请求实体
class Request {
    Long id;
    int originFloor;
    int destinationFloor;
    Direction direction;
    boolean completed;
    boolean passengerPickedUp;
    Date createdAt;
    Date completedAt;
    Elevator elevator;
}
```

## 3. 核心算法

### 3.1 电梯调度算法 - LOOK算法

LOOK算法是SCAN算法的优化版本，电梯在一个方向上移动直到没有更多请求，然后改变方向。

```java
private void processLookAlgorithm(Elevator elevator, List<Request> requests) {
    // 1. 收集所有停靠点
    Set<Integer> stops = elevator.getStops();
    
    // 2. 根据当前方向找到下一个停靠点
    Optional<Integer> nextStop = findNextStop(currentFloor, direction, stops);
    
    // 3. 移动到下一个停靠点
    if (nextStop.isPresent()) {
        moveToFloor(elevator, nextStop.get());
        handleFloorArrival(elevator, nextStop.get());
    }
}
```

### 3.2 最优电梯选择算法

基于多因素的成本计算：

```java
private int calculateCost(Elevator elevator, Request request) {
    // 因素1: 容量检查
    if (elevator.getCurrentLoad() >= elevator.getMaxCapacity()) {
        return Integer.MAX_VALUE;
    }
    
    // 因素2: 距离成本
    int distance = Math.abs(elevator.getCurrentFloor() - request.getOriginFloor());
    
    // 因素3: 方向匹配
    if (isSameDirection(elevator, request)) {
        return distance; // 同方向，成本较低
    }
    
    // 因素4: 反方向惩罚
    return distance + calculateTurnAroundCost(elevator);
}
```

## 4. 数据存储设计

### 4.1 数据库选择
- **开发/测试**: H2内存数据库
- **生产环境**: PostgreSQL (推荐) 或 MySQL

### 4.2 表结构

```sql
-- 电梯表
CREATE TABLE elevators (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    max_capacity INT NOT NULL,
    current_load INT DEFAULT 0,
    current_floor INT DEFAULT 1,
    direction VARCHAR(10) DEFAULT 'IDLE',
    state VARCHAR(20) DEFAULT 'IDLE',
    stops TEXT -- JSON格式存储停靠点集合
);

-- 请求表
CREATE TABLE requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    origin_floor INT NOT NULL,
    destination_floor INT NOT NULL,
    direction VARCHAR(10) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    passenger_picked_up BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    elevator_id BIGINT,
    FOREIGN KEY (elevator_id) REFERENCES elevators(id)
);
```

## 5. 可扩展性设计

### 5.1 水平扩展策略

#### 5.1.1 微服务拆分
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Elevator Service│    │ Request Service │    │ Dispatch Service│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │ Message Queue   │
                    │   (Kafka/RabbitMQ)│
                    └─────────────────┘
```

#### 5.1.2 数据分片策略
- **按楼层分片**: 不同楼层范围的请求路由到不同服务实例
- **按电梯分片**: 每个服务实例管理特定的电梯组
- **按时间分片**: 历史数据和实时数据分离存储

### 5.2 缓存策略

```java
@Cacheable("elevators")
public List<Elevator> getAllElevators() {
    return elevatorRepository.findAll();
}

@CacheEvict(value = "elevators", allEntries = true)
public Elevator updateElevator(Elevator elevator) {
    return elevatorRepository.save(elevator);
}
```

## 6. 性能优化

### 6.1 数据库优化
```sql
-- 索引优化
CREATE INDEX idx_requests_elevator_completed ON requests(elevator_id, completed);
CREATE INDEX idx_requests_floor_direction ON requests(origin_floor, direction, completed);
CREATE INDEX idx_elevators_floor_direction ON elevators(current_floor, direction);
```

### 6.2 算法优化
- **预测性调度**: 基于历史数据预测请求模式
- **批量处理**: 批量更新电梯状态
- **异步处理**: 非关键路径异步执行

### 6.3 内存优化
```java
// 使用对象池减少GC压力
@Component
public class RequestPool {
    private final Queue<Request> pool = new ConcurrentLinkedQueue<>();
    
    public Request borrowRequest() {
        Request request = pool.poll();
        return request != null ? request : new Request();
    }
    
    public void returnRequest(Request request) {
        request.reset();
        pool.offer(request);
    }
}
```

## 7. 监控和可观测性

### 7.1 关键指标
- **业务指标**:
  - 平均等待时间
  - 电梯利用率
  - 请求完成率
  - 系统吞吐量

- **技术指标**:
  - API响应时间
  - 数据库连接池使用率
  - JVM内存使用
  - CPU使用率

### 7.2 监控架构
```
Application → Micrometer → Prometheus → Grafana
     ↓
  Structured Logs → ELK Stack
     ↓
  Distributed Tracing → Jaeger
```

### 7.3 告警策略
```yaml
# Prometheus告警规则
groups:
- name: elevator-system
  rules:
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High response time detected"
      
  - alert: ElevatorSystemDown
    expr: up{job="elevator-system"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Elevator system is down"
```

## 8. 安全性设计

### 8.1 认证和授权
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/elevators/**").hasRole("OPERATOR")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

### 8.2 输入验证
```java
@PostMapping("/{elevatorId}/requests")
public ResponseEntity<Request> createRequest(
        @PathVariable @Positive Long elevatorId,
        @RequestParam @Min(1) @Max(100) int originFloor,
        @RequestParam @Min(1) @Max(100) int destinationFloor) {
    // 业务逻辑
}
```

## 9. 容灾和高可用

### 9.1 故障处理策略
- **电梯故障**: 自动重新分配请求到其他电梯
- **服务故障**: 熔断器模式，降级服务
- **数据库故障**: 主从切换，读写分离

### 9.2 备份和恢复
```yaml
# 数据库备份策略
apiVersion: batch/v1
kind: CronJob
metadata:
  name: db-backup
spec:
  schedule: "0 2 * * *"  # 每天凌晨2点
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:13
            command: ["pg_dump", "-h", "postgres", "-U", "user", "elevatordb"]
```

## 10. 部署和运维

### 10.1 容器化部署
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/elevator-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 10.2 Kubernetes部署
- **水平扩展**: HPA基于CPU和内存使用率
- **滚动更新**: 零停机部署
- **健康检查**: Liveness和Readiness探针
- **资源限制**: 合理的CPU和内存限制

### 10.3 CI/CD流水线
```yaml
# GitHub Actions示例
name: CI/CD Pipeline
on:
  push:
    branches: [main]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Run tests
      run: mvn test
  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
    - name: Build Docker image
      run: docker build -t elevator-system:${{ github.sha }} .
    - name: Deploy to K8s
      run: kubectl set image deployment/elevator-system elevator-system=elevator-system:${{ github.sha }}
```

## 11. 成本优化

### 11.1 资源优化
- **自动扩缩容**: 基于业务负载动态调整实例数量
- **资源预留**: 合理设置资源请求和限制
- **存储优化**: 冷热数据分离，历史数据归档

### 11.2 性能调优
- **JVM调优**: 合理的堆内存设置和GC策略
- **连接池优化**: 数据库连接池大小调优
- **缓存策略**: 多级缓存减少数据库访问

## 12. 未来扩展

### 12.1 智能化功能
- **机器学习**: 基于历史数据预测请求模式
- **动态调度**: 实时调整调度策略
- **能耗优化**: 基于能耗模型的调度优化

### 12.2 物联网集成
- **传感器数据**: 集成重量传感器、门传感器等
- **实时监控**: 电梯运行状态实时监控
- **预测性维护**: 基于传感器数据预测维护需求

这个设计文档涵盖了从系统架构到具体实现的各个方面，遵循了系统设计的最佳实践，确保系统的可扩展性、可靠性和可维护性。