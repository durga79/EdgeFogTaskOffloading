package org.edgefog.model;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a UAV (Unmanned Aerial Vehicle) equipped with edge computing capabilities.
 * UAVs serve as mobile edge servers in the simulation.
 */
public class UAV {
    private final String id;
    private IoTNodeLocation location;
    private final double maxSpeed;        // maximum speed in m/s
    private final double maxFlightTime;   // maximum flight time in seconds
    private double remainingEnergy;       // remaining energy in joules
    private final double totalEnergy;     // total energy capacity in joules
    private final int mips;               // processing capacity in Million Instructions Per Second
    private final int ram;                // RAM in MB
    private final int storage;            // Storage in GB
    
    // Current UAV status
    private UAVStatus status;
    
    // Current task processing information
    private final List<Task> assignedTasks;
    private double currentLoad;  // percentage of CPU utilization (0-1)
    private int completedTaskCount = 0;  // Counter for completed tasks
    
    // Networking capabilities
    private final double bandwidth;       // in bits per second
    private final double transmitPower;   // in mW (milliwatts)
    private final double communicationRange; // in meters
    
    // Movement
    private IoTNodeLocation targetLocation;
    private double currentSpeed;

    private UAV(Builder builder) {
        this.id = builder.id;
        this.location = builder.location;
        this.maxSpeed = builder.maxSpeed;
        this.maxFlightTime = builder.maxFlightTime;
        this.totalEnergy = builder.totalEnergy;
        this.remainingEnergy = builder.totalEnergy;
        this.mips = builder.mips;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.status = UAVStatus.IDLE;
        this.assignedTasks = new ArrayList<>();
        this.currentLoad = 0.0;
        this.bandwidth = builder.bandwidth;
        this.transmitPower = builder.transmitPower;
        this.communicationRange = builder.communicationRange;
        this.targetLocation = null;
        this.currentSpeed = 0.0;
    }

    public String getId() {
        return id;
    }

    public IoTNodeLocation getLocation() {
        return location;
    }
    
    public void setLocation(IoTNodeLocation location) {
        this.location = location;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getMaxFlightTime() {
        return maxFlightTime;
    }

    public double getRemainingEnergy() {
        return remainingEnergy;
    }
    
    /**
     * Get the total energy capacity of this UAV
     * @return Total energy in joules
     */
    public double getTotalEnergy() {
        return totalEnergy;
    }
    
    public void consumeEnergy(double amount) {
        this.remainingEnergy = Math.max(0, this.remainingEnergy - amount);
        if (this.remainingEnergy <= 0) {
            this.status = UAVStatus.OUT_OF_ENERGY;
        }
    }
    
    public double getEnergyPercentage() {
        return (remainingEnergy / totalEnergy) * 100.0;
    }

    public int getMips() {
        return mips;
    }

    public int getRam() {
        return ram;
    }

    public int getStorage() {
        return storage;
    }
    
    public UAVStatus getStatus() {
        return status;
    }
    
    public void setStatus(UAVStatus status) {
        this.status = status;
    }
    
    public List<Task> getAssignedTasks() {
        return new ArrayList<>(assignedTasks); // Return copy to prevent external modification
    }
    
    /**
     * Assign a task to this UAV
     * @param task The task to assign
     * @return true if successful, false if UAV cannot accept more tasks
     */
    public boolean assignTask(Task task) {
        System.out.println("[DEBUG] UAV " + id + ": Attempting to assign task " + task.getId());
        System.out.println("[DEBUG] UAV " + id + ": Current status=" + status + ", Current load=" + String.format("%.2f%%", currentLoad * 100));
        
        // Check if UAV can accept the task
        if (status != UAVStatus.IDLE && status != UAVStatus.PROCESSING) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot assign task - UAV is not IDLE or PROCESSING, current status: " + status);
            return false;
        }
        
        // Calculate the additional load this task would create
        double additionalLoad = calculateTaskLoadPercentage(task);
        System.out.println("[DEBUG] UAV " + id + ": Task load calculation: additional=" + String.format("%.2f%%", additionalLoad * 100) + 
                         ", current=" + String.format("%.2f%%", currentLoad * 100) + 
                         ", total=" + String.format("%.2f%%", (currentLoad + additionalLoad) * 100));
        
        if (currentLoad + additionalLoad > 1.0) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot assign task - would exceed capacity");
            return false; // Reject if overloaded
        }
        
        // Update task status and assign
        task.setStatus(Task.TaskStatus.PROCESSING);
        task.setAssignedResourceId(this.id);
        assignedTasks.add(task);
        
        // Update UAV status
        currentLoad += additionalLoad;
        if (status == UAVStatus.IDLE) {
            status = UAVStatus.PROCESSING;
        }
        
        System.out.println("[DEBUG] UAV " + id + ": Successfully assigned task " + task.getId() + ", new load: " + String.format("%.2f%%", currentLoad * 100));
        return true;
    }
    
    /**
     * Complete a task and update UAV status
     * @param task The task to complete
     * @return true if successful, false if task wasn't assigned to this UAV
     */
    public boolean completeTask(Task task) {
        if (!assignedTasks.contains(task)) {
            System.out.println("[DEBUG] UAV " + id + ": Task " + task.getId() + " not found in assigned tasks list");
            return false;
        }
        
        // Update task and UAV status
        task.setStatus(Task.TaskStatus.COMPLETED);
        assignedTasks.remove(task);
        currentLoad -= calculateTaskLoadPercentage(task);
        completedTaskCount++;  // Increment completed task counter
        
        // Calculate and consume energy for task processing
        double energyUsed = calculateTaskProcessingEnergy(task);
        consumeEnergy(energyUsed);
        
        System.out.println("[DEBUG] UAV " + id + ": Completed task " + task.getId() + ", total completed: " + 
                          completedTaskCount + ", energy used: " + String.format("%.2f", energyUsed) + " J");
        
        if (assignedTasks.isEmpty()) {
            status = UAVStatus.IDLE;
        }
        
        return true;
    }
    
    /**
     * Calculate what percentage of CPU this task would use
     * @param task The task to evaluate
     * @return CPU load as a percentage (0-1)
     */
    private double calculateTaskLoadPercentage(Task task) {
        // Simple model: load = task length / (MIPS * time to complete)
        // Assuming task needs to complete by deadline
        double timeToComplete = task.getDeadline();
        return task.getLength() / (mips * timeToComplete);
    }
    
    public double getCurrentLoad() {
        return currentLoad;
    }
    
    /**
     * Get the number of tasks completed by this UAV
     * @return Number of completed tasks
     */
    public int getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    public double getBandwidth() {
        return bandwidth;
    }
    
    public double getTransmitPower() {
        return transmitPower;
    }
    
    public double getCommunicationRange() {
        return communicationRange;
    }
    
    public IoTNodeLocation getTargetLocation() {
        return targetLocation;
    }
    
    public void setTargetLocation(IoTNodeLocation targetLocation) {
        this.targetLocation = targetLocation;
        if (targetLocation != null) {
            this.status = UAVStatus.MOVING;
            this.currentSpeed = maxSpeed; // Start moving at max speed
        }
    }
    
    public double getCurrentSpeed() {
        return currentSpeed;
    }
    
    /**
     * Update UAV position based on movement towards target
     * @param timeStep Time step in seconds
     */
    public void updatePosition(double timeStep) {
        if (targetLocation == null || status == UAVStatus.OUT_OF_ENERGY) {
            currentSpeed = 0.0;
            return;
        }
        
        // Calculate distance to target
        double distance = location.distanceTo(targetLocation);
        if (distance < 0.1) { // Within 10cm, consider arrived
            location = targetLocation;
            targetLocation = null;
            currentSpeed = 0.0;
            status = assignedTasks.isEmpty() ? UAVStatus.IDLE : UAVStatus.PROCESSING;
            return;
        }
        
        // Calculate movement vector
        double dx = targetLocation.getX() - location.getX();
        double dy = targetLocation.getY() - location.getY();
        double dz = targetLocation.getZ() - location.getZ();
        
        // Normalize vector
        double magnitude = Math.sqrt(dx*dx + dy*dy + dz*dz);
        dx /= magnitude;
        dy /= magnitude;
        dz /= magnitude;
        
        // Calculate movement distance in this time step
        double moveDistance = Math.min(distance, currentSpeed * timeStep);
        
        // Update position
        double newX = location.getX() + dx * moveDistance;
        double newY = location.getY() + dy * moveDistance;
        double newZ = location.getZ() + dz * moveDistance;
        location = new IoTNodeLocation(newX, newY, newZ);
        
        // Consume energy for movement
        // Simplified energy model: E = k * distance * weight
        double movementEnergyCost = calculateMovementEnergy(moveDistance);
        consumeEnergy(movementEnergyCost);
    }
    
    /**
     * Calculate energy consumed for movement
     * @param distance Distance moved in meters
     * @return Energy consumed in joules
     */
    private double calculateMovementEnergy(double distance) {
        // Simplified energy model for UAV movement
        // E = k * d where k is energy per meter constant
        double energyPerMeter = 0.1; // 0.1 joules per meter (example value)
        return energyPerMeter * distance;
    }
    
    /**
     * Calculate energy required to process a task
     * @param task The task to process
     * @return Energy in joules
     */
    public double calculateTaskProcessingEnergy(Task task) {
        // Simplified energy model: E = a * task_length + b
        // where a is energy per MI and b is base energy cost
        double energyPerMI = 0.01; // 0.01 joules per MI (example value)
        double baseEnergy = 0.5;   // 0.5 joules base cost
        return (energyPerMI * task.getLength()) + baseEnergy;
    }
    
    /**
     * Check if a node is in communication range of this UAV
     * @param nodeLocation Location to check
     * @return true if in range, false otherwise
     */
    public boolean isInRangeOf(IoTNodeLocation nodeLocation) {
        double distance = location.distanceTo(nodeLocation);
        boolean inRange = distance <= communicationRange;
        System.out.println("[DEBUG] UAV " + id + ": isInRangeOf check - Distance to node: " + 
                         String.format("%.2f", distance) + "m, Communication range: " + 
                         String.format("%.2f", communicationRange) + "m, In range: " + inRange);
        return inRange;
    }
    
    /**
     * Calculate the estimated time to complete a task
     * @param task The task to evaluate
     * @return Estimated completion time in seconds
     */
    public double estimateTaskCompletionTime(Task task) {
        // Basic model: time = task length / available MIPS
        // Available MIPS = total MIPS * (1 - current load)
        double availableMips = mips * (1 - currentLoad);
        return task.getLength() / Math.max(1, availableMips); // Avoid division by zero
    }
    
    /**
     * Check if the UAV has enough resources to process a task
     */
    public boolean canProcessTask(Task task) {
        System.out.println("[DEBUG] UAV " + id + ": Checking if can process task " + task.getId());
        System.out.println("[DEBUG] UAV " + id + ": Status=" + status + ", MIPS=" + mips + ", Energy=" + 
                         String.format("%.2f", remainingEnergy) + "/" + String.format("%.2f", totalEnergy) + 
                         ", Load=" + String.format("%.2f", currentLoad * 100) + "%");
        
        // Check if UAV is operational
        if (status == UAVStatus.OUT_OF_ENERGY || status == UAVStatus.MAINTENANCE) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot process task - UAV not operational, status=" + status);
            return false;
        }
        
        // Check if task can be completed within deadline
        double estimatedTime = estimateTaskCompletionTime(task);
        if (estimatedTime > task.getDeadline()) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot process task - deadline check failed. " + 
                             "Est. time: " + String.format("%.2f", estimatedTime) + "s, " + 
                             "Deadline: " + String.format("%.2f", task.getDeadline()) + "s");
            return false;
        }
        
        // Check if there's enough energy
        double requiredEnergy = calculateTaskProcessingEnergy(task);
        if (requiredEnergy > remainingEnergy) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot process task - insufficient energy. " + 
                             "Required: " + String.format("%.2f", requiredEnergy) + "J, " + 
                             "Available: " + String.format("%.2f", remainingEnergy) + "J");
            return false;
        }
        
        // Check if there's enough processing capacity
        double additionalLoad = calculateTaskLoadPercentage(task);
        boolean hasCapacity = (currentLoad + additionalLoad) <= 1.0;
        
        if (!hasCapacity) {
            System.out.println("[DEBUG] UAV " + id + ": Cannot process task - insufficient capacity. " + 
                             "Current load: " + String.format("%.2f", currentLoad * 100) + "%, " + 
                             "Additional: " + String.format("%.2f", additionalLoad * 100) + "%");
        } else {
            System.out.println("[DEBUG] UAV " + id + ": CAN process task " + task.getId() + 
                             ", Est. time: " + String.format("%.2f", estimatedTime) + "s, " + 
                             "Energy req: " + String.format("%.2f", requiredEnergy) + "J");
        }
        
        return hasCapacity;
    }
    
    @Override
    public String toString() {
        return String.format("UAV{id='%s', location=%s, status=%s, energy=%.2f%%, load=%.2f%%, tasks=%d}",
                id, location, status, getEnergyPercentage(), currentLoad * 100, assignedTasks.size());
    }
    
    /**
     * Builder pattern for UAV creation
     */
    public static class Builder {
        private String id;
        private IoTNodeLocation location;
        private double maxSpeed = 10.0;        // Default: 10 m/s
        private double maxFlightTime = 1800.0; // Default: 30 minutes
        private double totalEnergy = 18000.0;  // Default: 5 kWh
        private int mips = 10000;              // Default: 10,000 MIPS
        private int ram = 4096;                // Default: 4 GB
        private int storage = 64;              // Default: 64 GB
        private double bandwidth = 100 * 1024 * 1024; // Default: 100 Mbps
        private double transmitPower = 100.0;  // Default: 100 mW
        private double communicationRange = 1000.0; // Default: 1000 meters (increased from 500 for better coverage)
        
        public Builder() {
            this.id = UUID.randomUUID().toString();
        }
        
        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        
        public Builder withLocation(IoTNodeLocation location) {
            this.location = location;
            return this;
        }
        
        public Builder withMaxSpeed(double maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }
        
        public Builder withMaxFlightTime(double maxFlightTime) {
            this.maxFlightTime = maxFlightTime;
            return this;
        }
        
        public Builder withTotalEnergy(double totalEnergy) {
            this.totalEnergy = totalEnergy;
            return this;
        }
        
        public Builder withMips(int mips) {
            this.mips = mips;
            return this;
        }
        
        public Builder withRam(int ram) {
            this.ram = ram;
            return this;
        }
        
        public Builder withStorage(int storage) {
            this.storage = storage;
            return this;
        }
        
        public Builder withBandwidth(double bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }
        
        public Builder withTransmitPower(double transmitPower) {
            this.transmitPower = transmitPower;
            return this;
        }
        
        public Builder withCommunicationRange(double communicationRange) {
            this.communicationRange = communicationRange;
            return this;
        }
        
        public UAV build() {
            if (location == null) {
                throw new IllegalStateException("UAV location cannot be null");
            }
            return new UAV(this);
        }
    }
    
    /**
     * Represents the status of a UAV
     */
    public enum UAVStatus {
        IDLE,           // UAV is operational but not processing tasks
        PROCESSING,     // UAV is processing one or more tasks
        MOVING,         // UAV is moving to a new location
        OUT_OF_ENERGY,  // UAV has depleted its energy
        MAINTENANCE     // UAV is undergoing maintenance
    }
}
