# 电梯调度系统 (Elevator System)

一个基于Spring Boot的智能电梯调度系统，采用LOOK算法实现高效的多电梯协调调度。

## 🚀 功能特性

- **智能调度**: 基于LOOK算法的电梯调度
- **多电梯支持**: 支持多电梯协调工作
- **实时监控**: 完整的监控和指标收集
- **容器化部署**: 支持Docker和Kubernetes部署
- **高可用性**: 健康检查和自动恢复
- **可扩展性**: 水平扩展支持

## 📋 系统要求

- Java 17+
- Maven 3.6+
- Docker (可选)
- Kubernetes (可选)

## 🛠️ 快速开始

### 本地开发

```bash
# 克隆项目
git clone <repository-url>
cd myElevator

# 运行测试
mvn test

# 启动应用
mvn spring-boot:run
```

### Docker部署

```bash
# 构建镜像
docker build -t elevator-system:latest .

# 运行容器
docker run -p 8080:8080 elevator-system:latest

# 或使用docker-compose
docker-compose up -d
```

### Kubernetes部署

```bash
# 给脚本执行权限
chmod +x scripts/build-and-deploy.sh

# 构建并部署
./scripts/build-and-deploy.sh

# 清理部署
./scripts/cleanup.sh
```

## 🧪 测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试
```bash
# 单元测试
mvn test -Dtest=ElevatorServiceTest

# 集成测试
mvn test -Dtest=ElevatorSystemIntegrationTest

# 性能测试
mvn test -Dtest=ElevatorPerformanceTest
```

### 测试覆盖率
```bash
mvn jacoco:report
# 查看报告: target/site/jacoco/index.html
```

## 📊 API文档

### 电梯管理

#### 创建电梯
```http
POST /api/elevators?maxCapacity=10
```

#### 获取所有电梯
```http
GET /api/elevators
```

#### 获取特定电梯
```http
GET /api/elevators/{id}
```

### 请求管理

#### 创建请求
```http
POST /api/elevators/{id}/requests?originFloor=1&destinationFloor=5
```

#### 获取电梯请求
```http
GET /api/elevators/{id}/requests
```

#### 处理下一步
```http
POST /api/elevators/{id}/step
```

## 🔧 配置

### 应用配置文件

- `application.properties` - 默认配置
- `application-test.properties` - 测试环境配置
- `application-k8s.properties` - Kubernetes环境配置

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `default` |
| `SERVER_PORT` | 服务端口 | `8080` |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | 暴露的端点 | `health,info,metrics` |

## 📈 监控

### 健康检查
```http
GET /actuator/health
```

### 指标收集
```http
GET /actuator/metrics
GET /actuator/prometheus
```

### Grafana仪表板
访问 `http://localhost:3000` (docker-compose部署时)
- 用户名: admin
- 密码: admin

## 🏗️ 架构设计

详细的系统设计文档请参考 [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md)

### 核心组件

- **ElevatorController**: REST API控制器
- **ElevatorService**: 核心业务逻辑和调度算法
- **Elevator**: 电梯实体模型
- **Request**: 请求实体模型

### 调度算法

系统采用LOOK算法进行电梯调度：
1. 电梯在当前方向上移动
2. 处理所有同方向的请求
3. 到达边界后改变方向
4. 重复上述过程

## 🚀 部署指南

### Kubernetes部署清单

```yaml
# 命名空间
kubectl apply -f k8s/namespace.yaml

# 配置映射
kubectl apply -f k8s/configmap.yaml

# 部署
kubectl apply -f k8s/deployment.yaml

# 服务
kubectl apply -f k8s/service.yaml

# 入口
kubectl apply -f k8s/ingress.yaml

# 自动扩展
kubectl apply -f k8s/hpa.yaml
```

### 使用Kustomize
```bash
kubectl apply -k k8s/
```

## 🔍 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   lsof -i :8080
   
   # 修改端口
   export SERVER_PORT=8081
   ```

2. **内存不足**
   ```bash
   # 调整JVM参数
   export JAVA_OPTS="-Xmx512m -Xms256m"
   ```

3. **数据库连接问题**
   ```bash
   # 检查H2控制台
   curl http://localhost:8080/h2-console
   ```

### 日志查看

```bash
# 应用日志
kubectl logs -f deployment/elevator-system -n elevator-system

# 系统事件
kubectl get events -n elevator-system
```

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

### 代码规范

- 遵循Java编码规范
- 添加适当的单元测试
- 更新相关文档
- 确保所有测试通过

## 📝 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 支持

如有问题或建议，请：
- 创建Issue
- 发送邮件至 [your-email@example.com]
- 查看 [Wiki](wiki-url) 获取更多信息

## 🔄 版本历史

- **v1.0.0** - 初始版本
  - 基本电梯调度功能
  - LOOK算法实现
  - REST API接口
  - 单元测试和集成测试
  - Docker和Kubernetes支持
  - 监控和指标收集

## 🎯 路线图

- [ ] 机器学习预测调度
- [ ] 物联网传感器集成
- [ ] 移动端应用
- [ ] 高级分析仪表板
- [ ] 多租户支持