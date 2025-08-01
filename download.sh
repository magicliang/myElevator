#!/bin/bash

echo "📥 电梯系统JAR包下载脚本"
echo "================================"

# 确保构建脚本可执行
chmod +x build-jar.sh

# 执行构建
./build-jar.sh

# 创建下载信息
cat > release/DOWNLOAD_INFO.txt << 'EOF'
电梯系统JAR包下载信息
=====================

文件名: elevator-system-1.0.0.jar
版本: 1.0.0
构建时间: $(date)
文件大小: $(du -h release/elevator-system-1.0.0.jar | cut -f1)

系统要求:
- Java 17或更高版本
- 内存: 512MB以上
- 端口: 8080 (可配置)

快速开始:
1. 下载JAR包
2. 运行: java -jar elevator-system-1.0.0.jar
3. 访问: http://localhost:8080

API测试:
curl -X POST "http://localhost:8080/api/elevators?maxCapacity=10"
curl -X POST "http://localhost:8080/api/elevators/1/requests?originFloor=1&destinationFloor=5"

技术支持:
- 基于SpringBoot 3.2.0
- 使用H2内存数据库
- 支持LOOK电梯调度算法
EOF

echo ""
echo "✅ 构建完成！JAR包已准备就绪"
echo ""
echo "📦 下载文件:"
echo "   release/elevator-system-1.0.0.jar"
echo "   release/README.txt"
echo "   release/DOWNLOAD_INFO.txt"
echo ""
echo "📊 文件详情:"
ls -lh release/
echo ""
echo "🚀 使用方法:"
echo "   1. 下载 release/elevator-system-1.0.0.jar"
echo "   2. 运行: java -jar elevator-system-1.0.0.jar"
echo "   3. 访问: http://localhost:8080"