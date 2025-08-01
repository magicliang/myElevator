# 电梯系统 - SpringBoot实现

基于SpringBoot和H2内存数据库的电梯调度系统实现。

## 功能特性

- **智能调度**: 使用LOOK算法实现高效的电梯调度
- **多电梯支持**: 支持多部电梯的协同调度
- **负载均衡**: 根据电梯容量和当前负载智能分配请求
- **实时监控**: 提供REST API查看电梯状态和请求队列
- **完整测试**: 包含全面的集成测试用例

## 技术栈

- **后端**: SpringBoot 3.2.0
- **数据库**: H2内存数据库
- **测试**: JUnit 5 + RestAssured
- **构建**: Maven

## 快速开始

### 1. 构建项目
```bash
mvn clean install
```

### 2. 运行应用
```bash
mvn spring-boot:run
```

### 3. 访问API
- 应用地址: http://localhost:8080
- H2控制台: http://localhost:8080/h2-console
  - JDBC URL: jdbc:h2:mem:b83e68zb
  - 用户名: sa
  - 密码: (留空)

## API文档

### 创建电梯
```bash
POST /api/elevators?maxCapacity=10
```

### 创建请求
```bash
POST /api/elevators/{elevatorId}/requests?originFloor=1&destinationFloor=5
```

### 处理下一步
```bash
POST /api/elevators/{elevatorId}/step
```

### 获取所有电梯
```bash
GET /api/elevators
```

### 获取电梯详情
```bash
GET /api/elevators/{elevatorId}
```

### 获取待处理请求
```bash
GET /api/elevators/{elevatorId}/requests
```

## 测试

运行集成测试：
```bash
mvn test
```

## 设计亮点

1. **LOOK算法**: 实现了经典的电梯调度算法，提高运行效率
2. **最优选择**: 根据距离、方向、负载等因素选择最优电梯
3. **状态管理**: 完整的状态机实现，包括移动、停止、开门等状态
4. **数据持久化**: 使用JPA实现请求和电梯状态的持久化
5. **并发安全**: 使用Spring事务管理确保数据一致性

## 扩展可能

- 添加WebSocket实时推送电梯状态
- 实现高峰期特殊调度策略
- 添加乘客权重和优先级
- 集成监控和告警系统
- 支持电梯故障处理