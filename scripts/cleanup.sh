#!/bin/bash

echo "Cleaning up Elevator System deployment..."

# Delete Kubernetes resources
kubectl delete -k k8s/ --ignore-not-found=true

# Remove Docker image (optional)
read -p "Do you want to remove the Docker image? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker rmi elevator-system:latest || true
fi

echo "Cleanup completed!"