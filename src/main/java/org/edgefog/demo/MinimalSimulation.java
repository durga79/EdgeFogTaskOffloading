package org.edgefog.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A minimal simulation that demonstrates the core concepts of task offloading
 * without requiring external dependencies.
 */
public class MinimalSimulation {

    public static void main(String[] args) {
        System.out.println("Starting Minimal Task Offloading Simulation");
        
        // Create simulation environment
        MinimalSimulation sim = new MinimalSimulation();
        sim.runSimulation();
    }
    
    public void runSimulation() {
        // Simulation parameters
        int numIoTDevices = 5;
        int numUAVs = 3;
        int simulationSteps = 20;
        
        // Create IoT devices and UAVs
        List<IoTDevice> devices = new ArrayList<>();
        List<UAV> uavs = new ArrayList<>();
        
        System.out.println("Initializing " + numIoTDevices + " IoT devices and " + numUAVs + " UAVs");
        
        // Create IoT devices with random positions
        Random random = new Random(42);
        for (int i = 0; i < numIoTDevices; i++) {
            double x = random.nextDouble() * 500;
            double y = random.nextDouble() * 500;
            devices.add(new IoTDevice("device-" + i, x, y, 500 + random.nextInt(1500)));
        }
        
        // Create UAVs with random positions
        for (int i = 0; i < numUAVs; i++) {
            double x = random.nextDouble() * 500;
            double y = random.nextDouble() * 500;
            double z = 50 + random.nextDouble() * 50;
            uavs.add(new UAV("uav-" + i, x, y, z, 5000 + random.nextInt(5000)));
        }
        
        // Print initial positions
        System.out.println("\nInitial Positions:");
        for (IoTDevice device : devices) {
            System.out.println("Device " + device.id + " at (" + 
                    String.format("%.1f", device.x) + ", " + 
                    String.format("%.1f", device.y) + "), CPU: " + device.cpuCapacity + " MIPS");
        }
        
        for (UAV uav : uavs) {
            System.out.println("UAV " + uav.id + " at (" + 
                    String.format("%.1f", uav.x) + ", " + 
                    String.format("%.1f", uav.y) + ", " + 
                    String.format("%.1f", uav.z) + "), CPU: " + uav.cpuCapacity + " MIPS");
        }
        
        // Run simulation
        System.out.println("\nRunning simulation for " + simulationSteps + " steps");
        
        // Initialize counters for statistics
        int totalTasks = 0;
        int locallyProcessed = 0;
        int offloadedTasks = 0;
        int failedTasks = 0;
        
        // Simulation loop
        for (int step = 0; step < simulationSteps; step++) {
            System.out.println("\n--- Step " + (step+1) + " ---");
            
            // Generate tasks from IoT devices
            for (IoTDevice device : devices) {
                // Each device has 30% chance to generate a task per step
                if (random.nextDouble() < 0.3) {
                    Task task = device.generateTask();
                    totalTasks++;
                    
                    System.out.println("Device " + device.id + " generated task " + task.id);
                    
                    // Find nearby UAVs (within 200m)
                    List<UAV> nearbyUAVs = new ArrayList<>();
                    for (UAV uav : uavs) {
                        double distance = distance(device.x, device.y, 0, uav.x, uav.y, uav.z);
                        if (distance <= 200) {
                            nearbyUAVs.add(uav);
                        }
                    }
                    
                    // Decide whether to offload or process locally
                    if (nearbyUAVs.isEmpty() || 
                            (device.cpuCapacity >= task.requiredMips && random.nextBoolean())) {
                        // Process locally if no UAVs nearby or device has enough capacity (50% chance)
                        if (device.cpuCapacity >= task.requiredMips) {
                            System.out.println("Task " + task.id + " processed locally on device " + device.id);
                            device.energyConsumed += task.energyLocal;
                            locallyProcessed++;
                        } else {
                            System.out.println("Task " + task.id + " failed - insufficient local capacity");
                            failedTasks++;
                        }
                    } else {
                        // Offload to the UAV with the highest capacity
                        UAV bestUAV = nearbyUAVs.get(0);
                        for (UAV uav : nearbyUAVs) {
                            if (uav.availableCpuCapacity > bestUAV.availableCpuCapacity) {
                                bestUAV = uav;
                            }
                        }
                        
                        // Check if UAV has enough capacity
                        if (bestUAV.availableCpuCapacity >= task.requiredMips) {
                            System.out.println("Task " + task.id + " offloaded to UAV " + bestUAV.id);
                            bestUAV.processTask(task);
                            device.energyConsumed += task.energyTransmit;
                            offloadedTasks++;
                        } else {
                            System.out.println("Task " + task.id + " failed - insufficient UAV capacity");
                            failedTasks++;
                        }
                    }
                }
            }
            
            // Move UAVs (random movement)
            for (UAV uav : uavs) {
                // Reset available capacity each step
                uav.availableCpuCapacity = uav.cpuCapacity;
                
                // Move randomly
                uav.x += (random.nextDouble() - 0.5) * 20; // Move up to 10m in each direction
                uav.y += (random.nextDouble() - 0.5) * 20;
                uav.z += (random.nextDouble() - 0.5) * 5;  // Less vertical movement
                
                // Keep within bounds
                uav.x = Math.max(0, Math.min(500, uav.x));
                uav.y = Math.max(0, Math.min(500, uav.y));
                uav.z = Math.max(50, Math.min(100, uav.z));
            }
        }
        
        // Print statistics
        System.out.println("\n--- Simulation Results ---");
        System.out.println("Total tasks generated: " + totalTasks);
        System.out.println("Tasks processed locally: " + locallyProcessed + 
                " (" + String.format("%.1f%%", (100.0 * locallyProcessed / totalTasks)) + ")");
        System.out.println("Tasks offloaded to UAVs: " + offloadedTasks + 
                " (" + String.format("%.1f%%", (100.0 * offloadedTasks / totalTasks)) + ")");
        System.out.println("Failed tasks: " + failedTasks + 
                " (" + String.format("%.1f%%", (100.0 * failedTasks / totalTasks)) + ")");
        
        // Print energy consumption
        System.out.println("\nDevice Energy Consumption:");
        for (IoTDevice device : devices) {
            System.out.println("Device " + device.id + ": " + 
                    String.format("%.2f J", device.energyConsumed));
        }
        
        // Print UAV statistics
        System.out.println("\nUAV Statistics:");
        for (UAV uav : uavs) {
            System.out.println("UAV " + uav.id + ": Processed tasks: " + uav.tasksProcessed);
        }
    }
    
    /**
     * Calculate Euclidean distance between two 3D points
     */
    private double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
    }
    
    /**
     * Simple IoT device class
     */
    class IoTDevice {
        String id;
        double x, y; // Location
        int cpuCapacity; // MIPS
        int taskIdCounter = 0;
        double energyConsumed = 0;
        
        public IoTDevice(String id, double x, double y, int cpuCapacity) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.cpuCapacity = cpuCapacity;
        }
        
        /**
         * Generate a new task
         */
        public Task generateTask() {
            Random random = new Random();
            int mips = 200 + random.nextInt(1000); // 200-1200 MIPS
            double deadline = 2 + random.nextDouble() * 8; // 2-10 seconds
            double dataSize = 50 + random.nextInt(950); // 50-1000 KB
            
            Task task = new Task(
                id + "-task-" + (++taskIdCounter),
                mips,
                deadline,
                dataSize
            );
            
            return task;
        }
    }
    
    /**
     * Simple UAV class
     */
    class UAV {
        String id;
        double x, y, z; // Location
        int cpuCapacity; // MIPS
        int availableCpuCapacity;
        int tasksProcessed = 0;
        
        public UAV(String id, double x, double y, double z, int cpuCapacity) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.cpuCapacity = cpuCapacity;
            this.availableCpuCapacity = cpuCapacity;
        }
        
        /**
         * Process a task (reduce available capacity)
         */
        public void processTask(Task task) {
            this.availableCpuCapacity -= task.requiredMips;
            this.tasksProcessed++;
        }
    }
    
    /**
     * Simple Task class
     */
    class Task {
        String id;
        int requiredMips;
        double deadline;
        double dataSize; // KB
        double energyLocal; // J
        double energyTransmit; // J
        
        public Task(String id, int requiredMips, double deadline, double dataSize) {
            this.id = id;
            this.requiredMips = requiredMips;
            this.deadline = deadline;
            this.dataSize = dataSize;
            
            // Calculate energy consumption based on MIPS and data size
            this.energyLocal = 0.01 * requiredMips; // 0.01 J per MIPS
            this.energyTransmit = 0.005 * dataSize; // 0.005 J per KB
        }
    }
}
