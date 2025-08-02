#!/bin/bash

echo "Building and running the Agent application..."
echo

# Build the project
./mvnw clean install -q
if [ $? -ne 0 ]; then
    echo "Error: Build failed!"
    exit 1
fi

# Run the JavaFX application
./mvnw javafx:run -pl ui
if [ $? -ne 0 ]; then
    echo "Error: Application failed to start!"
    exit 1
fi 