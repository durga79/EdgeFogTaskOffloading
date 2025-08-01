package org.edgefog.demo;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.IoTNodeLocation;
import org.edgefog.model.Task;
import org.edgefog.model.UAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simplified demo to run the Multi-UAV edge computing simulation
 * without requiring the full dependencies.
 */
public class SimpleSimulationDemo {
    private static final Logger logger = LoggerFactory.getLogger(SimpleSimulationDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting simplified Multi-UAV Edge Computing demo");
        
        // Create simulation area
        double areaWidth = 500.0;
        double areaLength = 500.0;
        double areaHeight = 100.0;
        
        // Create IoT devices
        List<IoTDevice> devices = createDevices(5, areaWidth, areaLength);
        
        // Create UAVs
        List<UAV> uavs = createUAVs(2, areaWidth, areaLength, areaHeight);
        
        // Simulate for a few steps
        simulateTaskOffloading(devices, uavs, 10);
        
        // Print results
        printResults(devices, uavs);
    }
    
    private static List<IoTDevice> createDevices(int count, double width, double length) {
        List<IoTDevice> devices = new ArrayList<>();
        Random random = new Random(42);  // Fixed seed for reproducibility
        
        logger.info("Creating {} IoT devices", count);
        
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * length;
            double z = 1.0; // Ground level
            
            IoTDevice device = new IoTDevice.Builder()
                    .withId("device-" + i)
                    .withLocation(new IoTNodeLocation(x, y, z))
                    .withCpuMips(500 + random.nextInt(1500)) // 500-2000 MIPS
                    .withMemoryMb(256 + random.nextInt(768)) // 256-1024 MB
                    .withBatteryCapacity(18000 + random.nextInt(18000)) // 18000-36000 joules
                    .withTaskGenerationRate(0.2 + random.nextDouble() * 0.3) // Higher rate for demo
                    .withWirelessTechnology(IoTDevice.WirelessTechnology.WIFI)
                    .addSupportedTaskType(Task.TaskType.ENVIRONMENTAL_MONITORING)
                    .build();
            
            devices.add(device);
            logger.info("Created device {} at location ({}, {}, {}), CPU: {} MIPS", 
                    device.getId(), x, y, z, device.getCpuMips());
        }
        
        return devices;
    }
    
    private static List<UAV> createUAVs(int count, double width, double length, double height) {
        List<UAV> uavs = new ArrayList<>();
        Random random = new Random(24);  // Fixed seed for reproducibility
        
        logger.info("Creating {} UAVs", count);
        
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * length;
            double z = height * 0.5 + random.nextDouble() * height * 0.5; // 50-100% of max height
            
            UAV uav = new UAV.Builder()
                    .withId("uav-" + i)
                    .withLocation(new IoTNodeLocation(x, y, z))
                    .withMaxSpeed(10.0 + random.nextDouble() * 5.0)
                    .withMaxFlightTime(1800.0)
                    .withMips(8000 + random.nextInt(4000))
                    .withRam(4096)
                    .withStorage(32768)
                    .withBandwidth(100)
                    .withTotalEnergy(36000.0)
                    .withCommunicationRange(200.0)
                    .withTransmitPower(20.0)
                    .build();
            
            uavs.add(uav);
            logger.info("Created UAV {} at location ({}, {}, {}), CPU: {} MIPS, Range: {} m", 
                    uav.getId(), x, y, z, uav.getMips(), uav.getCommunicationRange());
        }
        
        return uavs;
    }
    
    private static void simulateTaskOffloading(List<IoTDevice> devices, List<UAV> uavs, int steps) {
        logger.info("Starting simulation for {} steps", steps);
        
        Random random = new Random(123);
        
        // Simulate steps
        for (int step = 0; step < steps; step++) {
            logger.info("=== Simulation step {} ===", step + 1);
            
            // Generate tasks from devices
            for (IoTDevice device : devices) {
                // Generate tasks with probability based on generation rate
                if (random.nextDouble() < device.getTaskGenerationRate()) {
                    Task task = device.generateTask();
                    logger.info("Device {} generated task {} of type {}", 
                            device.getId(), task.getId(), task.getType());
                    
                    // Find available UAVs for offloading
                    List<UAV> availableUAVs = findAvailableUAVs(device, uavs);
                    
                    if (availableUAVs.isEmpty()) {
                        // No UAVs in range, process locally
                        boolean success = device.processTaskLocally(task);
                        if (success) {
                            logger.info("Task {} processed locally on device {}", 
                                    task.getId(), device.getId());
                        } else {
                            logger.info("Device {} failed to process task {} locally", 
                                    device.getId(), task.getId());
                        }
                    } else {
                        // Choose best UAV based on simple heuristic (lowest load)
                        UAV bestUAV = findBestUAV(task, device, availableUAVs);
                        
                        // Offload task
                        boolean success = device.offloadTask(task, bestUAV);
                        if (success) {
                            logger.info("Device {} offloaded task {} to UAV {}", 
                                    device.getId(), task.getId(), bestUAV.getId());
                            
                            // Simulate task completion after processing
                            double processingTime = bestUAV.estimateTaskCompletionTime(task);
                            bestUAV.completeTask(task);
                            logger.info("UAV {} completed task {} in {:.2f} seconds", 
                                    bestUAV.getId(), task.getId(), processingTime);
                        } else {
                            logger.info("Failed to offload task {} from device {} to UAV {}", 
                                    task.getId(), device.getId(), bestUAV.getId());
                        }
                    }
                }
            }
            
            // Update UAV positions
            for (UAV uav : uavs) {
                // Simple random movement
                double dx = (random.nextDouble() - 0.5) * uav.getMaxSpeed();
                double dy = (random.nextDouble() - 0.5) * uav.getMaxSpeed();
                double dz = (random.nextDouble() - 0.5) * uav.getMaxSpeed() * 0.2; // Less vertical movement
                
                // Get current location
                IoTNodeLocation currentLoc = uav.getLocation();
                
                // Calculate new location
                IoTNodeLocation newLoc = new IoTNodeLocation(
                        Math.max(0, Math.min(500, currentLoc.getX() + dx)),
                        Math.max(0, Math.min(500, currentLoc.getY() + dy)),
                        Math.max(50, Math.min(100, currentLoc.getZ() + dz))
                );
                
                // Update UAV location
                uav.setLocation(newLoc);
                logger.info("UAV {} moved to location ({}, {}, {})", 
                        uav.getId(), newLoc.getX(), newLoc.getY(), newLoc.getZ());
            }
        }
    }
    
    private static List<UAV> findAvailableUAVs(IoTDevice device, List<UAV> allUAVs) {
        List<UAV> availableUAVs = new ArrayList<>();
        // Find UAVs in range
        for (UAV uav : allUAVs) {
            if (uav.getLocation().distanceTo(device.getLocation()) <= uav.getCommunicationRange() && 
                (uav.getStatus() == UAV.UAVStatus.IDLE || uav.getStatus() == UAV.UAVStatus.PROCESSING)) {
                availableUAVs.add(uav);
            }
        }
        
        return availableUAVs;
    }
    
    private static UAV findBestUAV(Task task, IoTDevice device, List<UAV> availableUAVs) {
        UAV bestUAV = availableUAVs.get(0);
        double bestScore = Double.MAX_VALUE;
        
        for (UAV uav : availableUAVs) {
            // Calculate a score based on load and distance
            double load = uav.getCurrentLoad();
            double distance = uav.getLocation().distanceTo(device.getLocation());
            double score = (load * 0.7) + (distance * 0.3); // Weight load more than distance
            
            if (score < bestScore) {
                bestScore = score;
                bestUAV = uav;
            }
        }
        
        return bestUAV;
    }
    
    private static void printResults(List<IoTDevice> devices, List<UAV> uavs) {
        logger.info("\n=== SIMULATION RESULTS ===");
        
        // Print device statistics
        logger.info("\nDEVICE STATISTICS:");
        for (IoTDevice device : devices) {
            logger.info("Device {}: Generated tasks: {}, Battery: {}/{} J", 
                    device.getId(), device.getTasksGenerated(), 
                    device.getRemainingBattery(), device.getBatteryCapacity());
        }
        
        // Print UAV statistics
        logger.info("\nUAV STATISTICS:");
        for (UAV uav : uavs) {
            logger.info("UAV {}: Tasks processed: {}, Energy: {}/{} J, Load: {}%", 
                    uav.getId(), uav.getCompletedTaskCount(), 
                    uav.getRemainingEnergy(), uav.getTotalEnergy(),
                    uav.getCurrentLoad() * 100);
        }
        
        // Print overall statistics
        int totalTasks = 0;
        int locallyProcessed = 0;
        int offloaded = 0;
        
        for (IoTDevice device : devices) {
            totalTasks += device.getTasksGenerated();
            locallyProcessed += device.getLocallyProcessedCount();
        }
        
        for (UAV uav : uavs) {
            offloaded += uav.getCompletedTaskCount();
        }
        
        logger.info("\nOVERALL STATISTICS:");
        logger.info("Total tasks generated: {}", totalTasks);
        logger.info("Locally processed tasks: {} ({}%)", 
                locallyProcessed, totalTasks > 0 ? locallyProcessed * 100.0 / totalTasks : 0);
        logger.info("Offloaded tasks: {} ({}%)", 
                offloaded, totalTasks > 0 ? offloaded * 100.0 / totalTasks : 0);
        logger.info("Simulation completed successfully");
    }
}
