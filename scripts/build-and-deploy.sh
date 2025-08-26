#!/bin/bash

set -e

echo "Building Elevator System..."

# Build the application
echo "Step 1: Building Maven project..."
mvn clean package -DskipTests

# Build Docker image
echo "Step 2: Building Docker image..."
docker build -t elevator-system:latest .

# Apply Kubernetes manifests
echo "Step 3: Deploying to Kubernetes..."
kubectl apply -k k8s/

# Wait for deployment to be ready
echo "Step 4: Waiting for deployment to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/elevator-system -n elevator-system

# Get service information
echo "Step 5: Getting service information..."
kubectl get services -n elevator-system

echo "Deployment completed successfully!"
echo "Access the application at: http://elevator.local"
echo "Or port-forward: kubectl port-forward svc/elevator-service 8080:80 -n elevator-system"