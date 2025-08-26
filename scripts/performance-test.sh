#!/bin/bash

# 性能测试脚本

set -e

echo "Starting Elevator System Performance Test..."

# 检查应用是否运行
if ! curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Error: Elevator system is not running on localhost:8080"
    echo "Please start the application first: mvn spring-boot:run"
    exit 1
fi

# 创建测试电梯
echo "Creating test elevators..."
for i in {1..5}; do
    curl -X POST "http://localhost:8080/api/elevators?maxCapacity=10" > /dev/null 2>&1
    echo "Created elevator $i"
done

# 并发请求测试
echo "Running concurrent request test..."
for i in {1..100}; do
    {
        elevator_id=$((RANDOM % 5 + 1))
        origin_floor=$((RANDOM % 10 + 1))
        destination_floor=$((RANDOM % 10 + 1))
        
        if [ $origin_floor -ne $destination_floor ]; then
            curl -X POST "http://localhost:8080/api/elevators/$elevator_id/requests?originFloor=$origin_floor&destinationFloor=$destination_floor" > /dev/null 2>&1
        fi
    } &
done

wait

echo "Concurrent requests completed. Processing steps..."

# 处理步骤测试
for step in {1..50}; do
    for elevator_id in {1..5}; do
        curl -X POST "http://localhost:8080/api/elevators/$elevator_id/step" > /dev/null 2>&1
    done
    echo "Completed step $step"
done

# 获取最终状态
echo "Final system state:"
curl -s http://localhost:8080/api/elevators | jq '.'

echo "Performance test completed successfully!"