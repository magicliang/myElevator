#!/bin/bash

echo "🚀 开始构建电梯系统JAR包..."

# 清理并构建项目
mvn clean compile

# 运行测试
mvn test

# 打包JAR
mvn package -DskipTests

# 检查构建结果
if [ -f "target/elevator-system-1.0.0.jar" ]; then
    echo "✅ JAR包构建成功！"
    echo "📦 文件位置: target/elevator-system-1.0.0.jar"
    echo "📊 文件大小: $(du -h target/elevator-system-1.0.0.jar | cut -f1)"
    
    # 创建发布目录
    mkdir -p release
    cp target/elevator-system-1.0.0.jar release/
    
    # 创建README文件
    cat > release/README.txt << 'EOF'
电梯系统JAR包使用说明
=====================

1. 运行应用:
   java -jar elevator-system-1.0.0.jar

2. 访问地址:
   - 应用: http://localhost:8080
   - H2控制台: http://localhost:8080/h2-console
     - JDBC URL: jdbc:h2:mem:b83e68zb
     - 用户名: sa
     - 密码: (留空)

3. API文档:
   - POST /api/elevators - 创建电梯
   - POST /api/elevators/{id}/requests - 创建请求
   - POST /api/elevators/{id}/step - 处理下一步
   - GET /api/elevators - 获取所有电梯
   - GET /api/elevators/{id} - 获取电梯详情
   - GET /api/elevators/{id}/requests - 获取待处理请求

4. 系统特性:
   - 基于LOOK算法的智能调度
   - 支持多电梯协同工作
   - 内存数据库，重启后数据清空
   - 完整的REST API接口

EOF
    
    echo "🎉 发布包已准备完成！"
    echo "📁 发布目录: release/"
    echo "📦 包含文件:"
    ls -la release/
else
    echo "❌ JAR包构建失败！"
    exit 1
fi