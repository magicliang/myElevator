#!/bin/bash

echo "Building Elevator System..."
mvn clean install

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Running integration tests..."
    mvn test
else
    echo "Build failed!"
    exit 1
fi