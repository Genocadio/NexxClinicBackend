#!/bin/bash
# Build JAR locally first (much faster, enables caching)
echo "Building JAR locally..."
./gradlew bootJar --no-daemon || exit 1

# Verify JAR was created
if [ ! -f build/libs/nexxclinic-*-SNAPSHOT.jar ]; then
    echo "ERROR: JAR build failed or file not found"
    exit 1
fi

echo "JAR built successfully. Now building and pushing Docker image..."

# Build and push multi-platform Docker image using buildx
docker buildx build --platform linux/amd64,linux/arm64 -t genoyves/nexclinic:latest --push .
