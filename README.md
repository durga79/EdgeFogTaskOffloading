# Deep Learning-Based Task Offloading in Multi-UAV Edge Computing

## Project Overview
This project implements a proof-of-concept for deep learning-based task offloading in a Multi-UAV aided Mobile Edge Computing (MEC) environment for IoT applications. The implementation is based on the IEEE research paper "A Deep Learning Approach for Task Offloading in Multi-UAV Aided Mobile Edge Computing" (2022). The system demonstrates how computational tasks from IoT edge nodes can be efficiently offloaded to UAV-mounted edge servers using deep learning techniques.

## Research Paper
- **Title:** A Deep Learning Approach for Task Offloading in Multi-UAV Aided Mobile Edge Computing
- **Published:** 2022, IEEE
- **URL:** https://ieeexplore.ieee.org/document/9899418/

## Key Features
- Simulation of IoT edge nodes generating computation-intensive tasks with varying requirements
- Implementation of multiple UAV-mounted edge servers with dynamic mobility
- Deep learning-based decision making for intelligent task offloading
- Wireless network simulation incorporating realistic connectivity models
- Comparative analysis with baseline offloading approaches
- Performance evaluation based on energy consumption, latency, and resource utilization

## System Architecture
1. **IoT Layer:** Edge nodes generating computation-intensive tasks
2. **UAV-MEC Layer:** Multiple UAVs equipped with edge computing capabilities
3. **Task Offloading Controller:** Deep learning-based decision engine for offloading
4. **Cloud Layer:** Centralized cloud resources for overflow processing

## Project Components

### Main CloudSim Plus Integration
The main project integrates with CloudSim Plus for accurate simulation of the edge-cloud environment.

### Standalone Demo Implementation
A simplified standalone demo (`EdgeFogTaskOffloadingDemo`) has been implemented to showcase the core functionality without full CloudSim Plus dependencies. This demo includes:
- Basic IoT device and UAV modeling
- Task generation and offloading logic
- Deep learning-based offloading controller using DL4J
- Performance metrics collection and reporting

## Implementation Details
- Built using CloudSim Plus for edge-cloud simulation
- DL4J (DeepLearning4J) for implementing the deep learning components
- Three-tier architecture reflecting the paper's design
- Realistic wireless network modeling (Wi-Fi, LTE, etc.)
- Energy consumption models for IoT devices and UAV-mounted edge servers

## Complete Setup and Run Commands

### Main Project with CloudSim Plus
```bash
# Navigate to the project directory
cd /home/wexa/CascadeProjects/EdgeFogTaskOffloading

# Clean and compile
mvn clean compile

# Run the simple simulation demo
mvn exec:java -Dexec.mainClass="org.edgefog.demo.SimpleSimulationDemo"

# Run the full application (when dependencies are resolved)
mvn exec:java -Dexec.mainClass="org.edgefog.EdgeFogTaskOffloading"
```

### Standalone Demo
```bash
# Navigate to the demo directory
cd /home/wexa/CascadeProjects/EdgeFogTaskOffloadingDemo

# Clean and compile
mvn clean compile

# Run the basic task offloading demo
mvn exec:java -Dexec.mainClass="org.edgefog.TaskOffloadingDemo"

# Run the deep learning-based task offloading demo
mvn exec:java -Dexec.mainClass="org.edgefog.DeepLearningTaskOffloadingDemo"
```

## Evaluation Metrics
- Energy consumption
- Task processing latency
- Network bandwidth usage
- Offloading success rate
- Resource utilization across UAV-mounted edge servers

## Project Structure
- `src/main/java/org/edgefog/` - Source code
  - `model/` - Model classes (Task, IoTDevice, UAV, etc.)
  - `offloading/` - Offloading decision components
  - `simulation/` - Simulation environment and metrics
  - `visualization/` - Visualization components
- `src/main/resources/` - Configuration files and ML models
- `docs/` - Documentation and research paper

## Dependencies

### Main Project
```xml
<dependencies>
    <!-- CloudSim Plus -->
    <dependency>
        <groupId>org.cloudsimplus</groupId>
        <artifactId>cloudsim-plus</artifactId>
        <version>6.4.3</version>
    </dependency>
    
    <!-- DeepLearning4J -->
    <dependency>
        <groupId>org.deeplearning4j</groupId>
        <artifactId>deeplearning4j-core</artifactId>
        <version>1.0.0-beta7</version>
    </dependency>
    <dependency>
        <groupId>org.nd4j</groupId>
        <artifactId>nd4j-native-platform</artifactId>
        <version>1.0.0-beta7</version>
    </dependency>
    
    <!-- Other dependencies -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.36</version>
    </dependency>
</dependencies>
```

## Requirements
- Java 11+
- Maven 3.6+
- Minimum 4GB RAM recommended for DL4J operations
# EdgeFogTaskOffloading
