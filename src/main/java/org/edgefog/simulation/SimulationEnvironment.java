package org.edgefog.simulation;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.IoTNodeLocation;
import org.edgefog.model.Task;
import org.edgefog.model.UAV;
import org.edgefog.offloading.DeepLearningOffloadingController;
import org.edgefog.offloading.OffloadingController;
import org.edgefog.offloading.OffloadingDecision;
import org.edgefog.offloading.OffloadingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The simulation environment for multi-UAV edge computing.
 * This class manages all entities in the simulation (devices, UAVs) and coordinates their interactions.
 */
public class SimulationEnvironment {
    private static final Logger logger = LoggerFactory.getLogger(SimulationEnvironment.class);
    
    // Simulation area dimensions in meters
    private final double areaWidth;
    private final double areaLength;
    private final double areaHeight;
    
    // Simulation entities
    private final List<IoTDevice> devices;
    private final List<UAV> uavs;
    private final Queue<Task> taskQueue;
    
    // Offloading controller
    private final OffloadingController offloadingController;
    
    // Simulation metrics
    private final SimulationMetrics metrics;
    
    // Simulation parameters
    private double currentTime;
    private final double timeStep;
    private boolean isRunning;
    
    /**
     * Get the width of the simulation area
     * @return Width in meters
     */
    public double getAreaWidth() {
        return areaWidth;
    }
    
    /**
     * Get the length of the simulation area
     * @return Length in meters
     */
    public double getAreaLength() {
        return areaLength;
    }
    
    /**
     * Get the height of the simulation area
     * @return Height in meters
     */
    public double getAreaHeight() {
        return areaHeight;
    }
    
    // Task processing tracking
    private final Map<String, TaskProcessingInfo> processingTasks;
    
    /**
     * Constructor for SimulationEnvironment
     * @param areaWidth Width of the simulation area in meters
     * @param areaLength Length of the simulation area in meters
     * @param areaHeight Height of the simulation area in meters
     * @param timeStep Time step for simulation in seconds
     */
    public SimulationEnvironment(double areaWidth, double areaLength, double areaHeight, double timeStep) {
        this.areaWidth = areaWidth;
        this.areaLength = areaLength;
        this.areaHeight = areaHeight;
        this.timeStep = timeStep;
        
        this.devices = new ArrayList<>();
        this.uavs = new ArrayList<>();
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.metrics = new SimulationMetrics();
        this.offloadingController = new DeepLearningOffloadingController(5, true, 1000, metrics);
        
        this.currentTime = 0.0;
        this.isRunning = false;
        this.processingTasks = new HashMap<>();
    }
    
    /**
     * Add an IoT device to the simulation
     * @param device The IoT device to add
     */
    public void addDevice(IoTDevice device) {
        devices.add(device);
    }
    
    /**
     * Add a UAV to the simulation
     * @param uav The UAV to add
     */
    public void addUAV(UAV uav) {
        uavs.add(uav);
    }
    
    /**
     * Generate random IoT devices for the simulation
     * @param count Number of devices to generate
     */
    public void generateRandomDevices(int count) {
        logger.info("Generating {} random IoT devices", count);
        
        Random random = ThreadLocalRandom.current();
        
        for (int i = 0; i < count; i++) {
            // Generate random location within the simulation area
            double x = random.nextDouble() * areaWidth;
            double y = random.nextDouble() * areaLength;
            double z = 2.0; // Ground level (2 meters for typical device height)
            
            // Create device with random type and characteristics
            IoTDevice device = new IoTDevice.Builder()
                    .withId("device-" + i)
                    .withLocation(new IoTNodeLocation(x, y, z))
                    .withCpuMips(500 + random.nextInt(1500)) // 500-2000 MIPS
                    .withMemoryMb(256 + random.nextInt(768)) // 256-1024 MB
                    .withBatteryCapacity(18000 + random.nextInt(18000)) // 18000-36000 joules (5-10 Wh)
                    .withTaskGenerationRate(0.05 + random.nextDouble() * 0.2) // 0.05-0.25 tasks per second
                    .withWirelessTechnology(getRandomWirelessTechnology(random))
                    .build();
            
            // Add task types based on device capabilities
            if (device.getCpuMips() < 1000) {
                // Low-power devices support simple tasks
                device = new IoTDevice.Builder()
                        .withId(device.getId())
                        .withLocation(device.getLocation())
                        .withCpuMips(device.getCpuMips())
                        .withMemoryMb(device.getMemoryMb())
                        .withBatteryCapacity(device.getBatteryCapacity())
                        .withTaskGenerationRate(device.getTaskGenerationRate())
                        .withWirelessTechnology(device.getWirelessTech())
                        .addSupportedTaskType(Task.TaskType.ENVIRONMENTAL_MONITORING)
                        .addSupportedTaskType(Task.TaskType.SMART_AGRICULTURE)
                        .build();
            } else if (device.getCpuMips() < 1500) {
                // Medium-power devices support more complex tasks
                device = new IoTDevice.Builder()
                        .withId(device.getId())
                        .withLocation(device.getLocation())
                        .withCpuMips(device.getCpuMips())
                        .withMemoryMb(device.getMemoryMb())
                        .withBatteryCapacity(device.getBatteryCapacity())
                        .withTaskGenerationRate(device.getTaskGenerationRate())
                        .withWirelessTechnology(device.getWirelessTech())
                        .addSupportedTaskType(Task.TaskType.TRAFFIC_MONITORING)
                        .addSupportedTaskType(Task.TaskType.HEALTH_MONITORING)
                        .addSupportedTaskType(Task.TaskType.INDUSTRIAL_CONTROL)
                        .build();
            } else {
                // High-power devices support all task types
                device = new IoTDevice.Builder()
                        .withId(device.getId())
                        .withLocation(device.getLocation())
                        .withCpuMips(device.getCpuMips())
                        .withMemoryMb(device.getMemoryMb())
                        .withBatteryCapacity(device.getBatteryCapacity())
                        .withTaskGenerationRate(device.getTaskGenerationRate())
                        .withWirelessTechnology(device.getWirelessTech())
                        .addSupportedTaskType(Task.TaskType.REAL_TIME_VIDEO_ANALYTICS)
                        .addSupportedTaskType(Task.TaskType.EMERGENCY_RESPONSE)
                        .build();
            }
            
            addDevice(device);
        }
        
        logger.info("Generated {} IoT devices", count);
    }
    
    /**
     * Generate random UAVs for the simulation
     * @param count Number of UAVs to generate
     */
    public void generateRandomUAVs(int count) {
        logger.info("Generating {} random UAVs", count);
        
        Random random = ThreadLocalRandom.current();
        
        for (int i = 0; i < count; i++) {
            // Generate random location within the simulation area
            double x = random.nextDouble() * areaWidth;
            double y = random.nextDouble() * areaLength;
            double z = 50.0 + random.nextDouble() * 50.0; // 50-100 meters height
            
            // Create UAV with random characteristics
            UAV uav = new UAV.Builder()
                    .withId("uav-" + i)
                    .withLocation(new IoTNodeLocation(x, y, z))
                    .withMaxSpeed(10.0 + random.nextDouble() * 10.0) // 10-20 m/s
                    .withMaxFlightTime(1800.0 + random.nextDouble() * 1800.0) // 30-60 minutes
                    .withMips(8000 + random.nextInt(8000)) // 8000-16000 MIPS
                    .withTotalEnergy(36000.0 + random.nextDouble() * 36000.0) // 10-20 Wh
                    .withCommunicationRange(800.0 + random.nextDouble() * 400.0) // 800-1200 meters (increased for better coverage)
                    .build();
            
            addUAV(uav);
        }
        
        logger.info("Generated {} UAVs", count);
    }
    
    /**
     * Get a random wireless technology for an IoT device
     * @param random Random number generator
     * @return A randomly selected wireless technology
     */
    private IoTDevice.WirelessTechnology getRandomWirelessTechnology(Random random) {
        IoTDevice.WirelessTechnology[] technologies = IoTDevice.WirelessTechnology.values();
        return technologies[random.nextInt(technologies.length)];
    }
    
    /**
     * Initialize the simulation with the given number of devices and UAVs
     * @param numDevices Number of IoT devices
     * @param numUAVs Number of UAVs
     */
    public void initialize(int numDevices, int numUAVs) {
        logger.info("Initializing simulation with {} devices and {} UAVs", numDevices, numUAVs);
        
        // Clear existing entities
        devices.clear();
        uavs.clear();
        taskQueue.clear();
        processingTasks.clear();
        metrics.reset();
        
        // Generate random devices and UAVs
        generateRandomDevices(numDevices);
        generateRandomUAVs(numUAVs);
        
        currentTime = 0.0;
        isRunning = false;
        
        logger.info("Simulation initialized");
    }
    
    /**
     * Run the simulation for a specified duration
     * @param duration Duration to run in simulation seconds
     */
    public void run(double duration) {
        logger.info("Starting simulation for {} seconds", duration);
        
        isRunning = true;
        double endTime = currentTime + duration;
        
        while (isRunning && currentTime < endTime) {
            step();
        }
        
        logger.info("Simulation completed at time {}", currentTime);
    }
    
    /**
     * Advance the simulation by one time step
     */
    public void step() {
        // Update UAV positions
        updateUAVs();
        
        // Generate tasks from devices
        generateTasks();
        
        // Process task queue
        processTasks();
        
        // Update task processing status
        updateTaskProcessing();
        
        // Update simulation time
        currentTime += timeStep;
    }
    
    /**
     * Update UAV positions and status
     */
    private void updateUAVs() {
        for (UAV uav : uavs) {
            // Update position based on movement
            uav.updatePosition(timeStep);
            
            // Update metrics
            metrics.updateUAVUtilization(uav.getId(), uav.getCurrentLoad() * 100.0);
        }
    }
    
    /**
     * Generate new tasks from IoT devices based on their generation rates
     */
    private void generateTasks() {
        Random random = ThreadLocalRandom.current();
        
        for (IoTDevice device : devices) {
            // Determine if device generates a task in this time step
            double taskProbability = device.getTaskGenerationRate() * timeStep;
            if (random.nextDouble() < taskProbability) {
                // Generate a new task
                Task task = device.generateTask();
                task.setSubmissionTime((long) (currentTime * 1000)); // Convert to milliseconds
                task.setStatus(Task.TaskStatus.READY);
                
                // Add to task queue
                taskQueue.add(task);
                
                // Record task creation in metrics
                metrics.recordTaskCreation();
                
                logger.debug("Device {} generated task {}", device.getId(), task.getId());
            }
        }
    }
    
    /**
     * Process tasks in the queue
     */
    private void processTasks() {
        // Process up to a maximum number of tasks per step to avoid excessive computation
        int maxTasksPerStep = 20;
        int tasksProcessed = 0;
        
        while (!taskQueue.isEmpty() && tasksProcessed < maxTasksPerStep) {
            Task task = taskQueue.poll();
            tasksProcessed++;
            
            // Find the source device for this task
            IoTDevice sourceDevice = findDeviceAtLocation(task.getSourceLocation());
            if (sourceDevice == null) {
                logger.warn("Could not find source device for task {}", task.getId());
                metrics.recordTaskFailure();
                continue;
            }
            
            // Get available UAVs for this task
            List<UAV> availableUAVs = getAvailableUAVs(sourceDevice);
            
            if (availableUAVs.isEmpty()) {
                // No UAVs available, either process locally or drop the task
                if (sourceDevice.estimateLocalExecutionTime(task) <= task.getDeadline()) {
                    // Process locally if deadline can be met
                    if (sourceDevice.processTaskLocally(task)) {
                        // Calculate energy and time
                        double energyUsed = task.calculateLocalEnergyConsumption(sourceDevice.getCpuPowerConsumption());
                        double executionTime = sourceDevice.estimateLocalExecutionTime(task) * 1000; // to ms
                        
                        metrics.recordDeviceEnergyConsumption(energyUsed);
                        metrics.recordTaskCompletion(executionTime, energyUsed, true);
                        
                        logger.debug("Task {} processed locally on device {}", task.getId(), sourceDevice.getId());
                    } else {
                        metrics.recordTaskFailure();
                        logger.debug("Device {} failed to process task {} locally", sourceDevice.getId(), task.getId());
                    }
                } else {
                    // Drop task if deadline can't be met
                    metrics.recordTaskDropped();
                    logger.debug("Task {} dropped due to no available UAVs and local deadline constraint", task.getId());
                }
                continue;
            }
            
            // Make offloading decision
            OffloadingDecision decision = offloadingController.makeOffloadingDecision(task, sourceDevice, availableUAVs);
            
            // Execute the decision
            if (decision.getTarget() == OffloadingTarget.LOCAL) {
                // Process locally
                if (sourceDevice.processTaskLocally(task)) {
                    double energyUsed = task.calculateLocalEnergyConsumption(sourceDevice.getCpuPowerConsumption());
                    double executionTime = sourceDevice.estimateLocalExecutionTime(task) * 1000; // to ms
                    
                    metrics.recordDeviceEnergyConsumption(energyUsed);
                    metrics.recordTaskCompletion(executionTime, energyUsed, true);
                    
                    logger.debug("Task {} processed locally on device {} by decision", 
                            task.getId(), sourceDevice.getId());
                } else {
                    metrics.recordTaskFailure();
                    logger.debug("Device {} failed to process task {} locally by decision", 
                            sourceDevice.getId(), task.getId());
                }
            } else {
                // Offload to selected UAV
                UAV selectedUAV = decision.getSelectedUAV();
                System.out.println("[DEBUG] Attempting to offload task " + task.getId() + " to UAV " + selectedUAV.getId());
                
                // Verify UAV can process task before offloading
                if (selectedUAV.canProcessTask(task)) {
                    System.out.println("[DEBUG] UAV " + selectedUAV.getId() + " CAN process task " + task.getId());
                    
                    if (sourceDevice.offloadTask(task, selectedUAV)) {
                        // Record energy used for transmission
                        double transmissionEnergy = sourceDevice.calculateOffloadingEnergy(task, selectedUAV);
                        metrics.recordDeviceEnergyConsumption(transmissionEnergy);
                        metrics.recordDataTransfer(task.getInputDataSize() + task.getOutputDataSize());
                        
                        // Start processing task
                        TaskProcessingInfo processingInfo = new TaskProcessingInfo(task, sourceDevice, selectedUAV, currentTime);
                        processingTasks.put(task.getId(), processingInfo);
                        
                        System.out.println("[DEBUG] Successfully offloaded task " + task.getId() + " to UAV " + 
                                selectedUAV.getId() + ", added to processing tasks map");
                        
                        logger.debug("Task {} offloaded from device {} to UAV {}", 
                                task.getId(), sourceDevice.getId(), selectedUAV.getId());
                    } else {
                        metrics.recordTaskFailure();
                        System.out.println("[DEBUG] Failed to offload task " + task.getId() + " from device " + 
                                sourceDevice.getId() + " to UAV " + selectedUAV.getId() + 
                                " - UAV.offloadTask returned false");
                        logger.debug("Failed to offload task {} from device {} to UAV {}", 
                                task.getId(), sourceDevice.getId(), selectedUAV.getId());
                    }
                } else {
                    // UAV cannot process this task
                    System.out.println("[DEBUG] UAV " + selectedUAV.getId() + " CANNOT process task " + task.getId() + 
                            " - UAV.canProcessTask returned false");
                    metrics.recordTaskFailure();
                    logger.debug("UAV {} cannot process task {} from device {}", 
                            selectedUAV.getId(), task.getId(), sourceDevice.getId());
                }
            }
        }
    }
    
    /**
     * Update the status of tasks being processed on UAVs
     */
    private void updateTaskProcessing() {
        // Create a list of tasks to remove (to avoid ConcurrentModificationException)
        List<String> completedTasks = new ArrayList<>();
        
        // Check each task in processing
        for (Map.Entry<String, TaskProcessingInfo> entry : processingTasks.entrySet()) {
            String taskId = entry.getKey();
            TaskProcessingInfo info = entry.getValue();
            
            // Calculate time spent processing
            double processingTime = currentTime - info.startTime;
            double expectedCompletionTime = info.selectedUAV.estimateTaskCompletionTime(info.task);
            
            // Check if task is completed
            if (processingTime >= expectedCompletionTime) {
                // Complete the task
                boolean completed = info.selectedUAV.completeTask(info.task);
                
                // Calculate energy used - already consumed in UAV.completeTask() but we need to record it in metrics
                double processingEnergy = info.selectedUAV.calculateTaskProcessingEnergy(info.task);
                metrics.recordUAVEnergyConsumption(processingEnergy);
                
                System.out.println("[DEBUG] SimulationEnvironment: Task " + info.task.getId() + 
                    " processed by UAV " + info.selectedUAV.getId() + 
                    ", completion status: " + completed + 
                    ", energy recorded in metrics: " + String.format("%.2f", processingEnergy) + " J");
                
                // Calculate total latency (transmission + processing)
                double totalLatency = (processingTime + 
                        info.sourceDevice.estimateTotalOffloadingTime(info.task, info.selectedUAV)) * 1000; // to ms
                
                // Check if deadline was met
                boolean deadlineMet = totalLatency / 1000.0 <= info.task.getDeadline();
                
                // Record completion
                metrics.recordTaskCompletion(totalLatency, 
                        info.sourceDevice.calculateOffloadingEnergy(info.task, info.selectedUAV) + processingEnergy,
                        deadlineMet);
                
                // Add to completed list
                completedTasks.add(taskId);
                
                logger.debug("Task {} completed on UAV {} after {} seconds", 
                        info.task.getId(), info.selectedUAV.getId(), processingTime);
            }
        }
        
        // Remove completed tasks
        for (String taskId : completedTasks) {
            processingTasks.remove(taskId);
        }
    }
    
    /**
     * Find the IoT device at a given location
     * @param location The location to search for
     * @return The device at the location or null if none found
     */
    private IoTDevice findDeviceAtLocation(IoTNodeLocation location) {
        for (IoTDevice device : devices) {
            if (device.getLocation().equals(location)) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Get UAVs that are available for offloading from a specific device
     * @param device The device to offload from
     * @return List of UAVs in range and available
     */
    private List<UAV> getAvailableUAVs(IoTDevice device) {
        List<UAV> availableUAVs = new ArrayList<>();
        
        for (UAV uav : uavs) {
            // Check if UAV is in range and operational
            if (uav.isInRangeOf(device.getLocation()) && 
                (uav.getStatus() == UAV.UAVStatus.IDLE || uav.getStatus() == UAV.UAVStatus.PROCESSING)) {
                availableUAVs.add(uav);
            }
        }
        
        return availableUAVs;
    }
    
    /**
     * Get the current simulation time
     * @return Current time in seconds
     */
    public double getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Stop the simulation
     */
    public void stop() {
        isRunning = false;
    }
    
    /**
     * Get the simulation metrics
     * @return The metrics object
     */
    public SimulationMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Get the list of IoT devices
     * @return List of devices
     */
    public List<IoTDevice> getDevices() {
        return new ArrayList<>(devices);
    }
    
    /**
     * Get the list of UAVs
     * @return List of UAVs
     */
    public List<UAV> getUAVs() {
        return new ArrayList<>(uavs);
    }
    
    /**
     * Class to track information about tasks being processed
     */
    private static class TaskProcessingInfo {
        final Task task;
        final IoTDevice sourceDevice;
        final UAV selectedUAV;
        final double startTime;
        
        public TaskProcessingInfo(Task task, IoTDevice sourceDevice, UAV selectedUAV, double startTime) {
            this.task = task;
            this.sourceDevice = sourceDevice;
            this.selectedUAV = selectedUAV;
            this.startTime = startTime;
        }
    }
}
