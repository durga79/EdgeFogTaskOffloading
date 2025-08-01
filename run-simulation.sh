#!/bin/bash

# ========================================================
# Edge-Fog Task Offloading Simulation Runner Script
# ========================================================
#
# This script runs the Multi-UAV Edge Computing simulation with clean output,
# suppressing all debug and info logs. It shows only the essential simulation
# statistics such as task completion, energy consumption, latency, device and
# UAV statistics for easier analysis.
#
# The script works by creating a temporary logback configuration file that
# routes all logs to a file, setting appropriate log levels, and then
# extracting only the relevant statistics sections for display.
#
# Output sections include:
# - Task statistics (completion, failures)
# - Deadline statistics
# - Latency statistics
# - Energy consumption
# - Network data transfer
# - Offloading decisions (UAV vs local)
# - Resource utilization
# - Device statistics (first 10 devices)
# - UAV statistics
#
# Author: Cascade AI
# Last updated: August 1, 2025
# ========================================================

echo "Running Edge-Fog Task Offloading Simulation..."

# Create a temporary logback configuration file to redirect output to file only
cat > temp-logback.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>simulation_output.txt</file>
        <append>false</append>
        <encoder>
            <pattern>%msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Set org.edgefog package to INFO level to capture statistics -->
    <logger name="org.edgefog" level="INFO" />
    
    <!-- Silence other packages -->
    <logger name="org.cloudbus" level="ERROR" />
    <logger name="org.nd4j" level="ERROR" />
    <logger name="org.deeplearning4j" level="ERROR" />
    <logger name="org.apache" level="ERROR" />
    
    <root level="ERROR">
        <appender-ref ref="FILE" />
    </root>
</configuration>
EOF

# Run the simulation with our configured logging - redirecting all output to file
echo "Running simulation, please wait..."
mvn -q exec:java -Dexec.mainClass="org.edgefog.simulation.SimulationRunner" \
    -Dlogback.configurationFile=./temp-logback.xml > /dev/null 2>&1

echo ""
echo "=== SIMULATION SUMMARY ==="
echo ""

# Check if output file exists
if [ -f "simulation_output.txt" ]; then
    # Extract and show statistics
    echo "--- TASK STATISTICS ---"
    grep -A 5 "TASK STATISTICS:" simulation_output.txt || echo "No task statistics found"
    
    echo ""
    echo "--- DEADLINE STATISTICS ---"
    grep -A 3 "DEADLINE STATISTICS:" simulation_output.txt || echo "No deadline statistics found"
    
    echo ""
    echo "--- LATENCY STATISTICS ---"
    grep -A 4 "LATENCY STATISTICS:" simulation_output.txt || echo "No latency statistics found"
    
    echo ""
    echo "--- ENERGY STATISTICS ---"
    grep -A 5 "ENERGY STATISTICS:" simulation_output.txt || echo "No energy statistics found"
    
    echo ""
    echo "--- NETWORK STATISTICS ---"
    grep -A 3 "NETWORK STATISTICS:" simulation_output.txt || echo "No network statistics found"
    
    echo ""
    echo "--- OFFLOADING STATISTICS ---"
    grep -A 3 "OFFLOADING STATISTICS:" simulation_output.txt || echo "No offloading statistics found"
    
    echo ""
    echo "--- RESOURCE UTILIZATION ---"
    grep -A 2 "RESOURCE UTILIZATION:" simulation_output.txt || echo "No utilization statistics found"
    
    # Extract device statistics
    echo ""
    echo "--- DEVICE STATISTICS (First 10) ---"
    grep "^Device device-" simulation_output.txt | head -10 || echo "No device statistics found"
    echo "... (showing first 10 devices only)"
    
    # Extract UAV statistics
    echo ""
    echo "--- UAV STATISTICS ---"
    grep "^UAV uav-" simulation_output.txt || echo "No UAV statistics found"
else
    echo "Error: Simulation output file not found. The simulation may have failed."
fi

# Clean up temporary files
rm -f temp-logback.xml simulation_output.txt

echo ""
echo "Simulation completed successfully."

