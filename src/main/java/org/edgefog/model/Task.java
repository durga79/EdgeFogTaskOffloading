package org.edgefog.model;

import java.util.UUID;

/**
 * Represents an IoT task to be processed in the Multi-UAV Edge Computing environment.
 * Tasks have varying requirements for CPU, network, energy and sensitivity to delay.
 */
public class Task {
    private final String id;
    private final long length;           // in Million Instructions (MI)
    private final long inputDataSize;    // in bytes - data to be transferred to the edge/cloud
    private final long outputDataSize;   // in bytes - results to be returned
    private final double deadline;       // in seconds
    private final double maxEnergy;      // maximum energy allowed for task execution (in joules)
    private final IoTNodeLocation sourceLocation; // Location of the source IoT node
    private final TaskType type;
    private final int priority;          // 1-10, where 10 is highest priority
    private long submissionTime;         // timestamp when task was submitted
    private TaskStatus status;
    private String assignedResourceId;    // ID of the resource where task is assigned

    private Task(Builder builder) {
        this.id = builder.id;
        this.length = builder.length;
        this.inputDataSize = builder.inputDataSize;
        this.outputDataSize = builder.outputDataSize;
        this.deadline = builder.deadline;
        this.maxEnergy = builder.maxEnergy;
        this.sourceLocation = builder.sourceLocation;
        this.type = builder.type;
        this.priority = builder.priority;
        this.submissionTime = builder.submissionTime;
        this.status = builder.status;
        this.assignedResourceId = null;
    }

    public String getId() {
        return id;
    }

    public long getLength() {
        return length;
    }

    public long getInputDataSize() {
        return inputDataSize;
    }

    public long getOutputDataSize() {
        return outputDataSize;
    }

    public double getDeadline() {
        return deadline;
    }
    
    public double getMaxEnergy() {
        return maxEnergy;
    }
    
    public IoTNodeLocation getSourceLocation() {
        return sourceLocation;
    }

    public TaskType getType() {
        return type;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public long getSubmissionTime() {
        return submissionTime;
    }
    
    public void setSubmissionTime(long submissionTime) {
        this.submissionTime = submissionTime;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getAssignedResourceId() {
        return assignedResourceId;
    }
    
    public void setAssignedResourceId(String assignedResourceId) {
        this.assignedResourceId = assignedResourceId;
    }
    
    // Calculate task utility based on priority, deadline and length
    public double calculateUtility() {
        return priority * (1000.0 / length) * (1.0 / deadline);
    }
    
    // Calculate potential energy consumption if task is executed locally
    public double calculateLocalEnergyConsumption(double cpuPowerConsumption) {
        // Simplified energy model: Energy = Power * Time
        // Time is proportional to task length
        return cpuPowerConsumption * (length / 1000.0); // Normalize by 1000 MI
    }
    
    // Calculate potential energy consumption for task transmission
    public double calculateTransmissionEnergy(double transmitPower, double bandwidth) {
        // Simplified energy model: E = P * (data size / data rate)
        return transmitPower * (inputDataSize / bandwidth);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + "'" +
                ", type=" + type +
                ", length=" + length + " MI" +
                ", inputDataSize=" + inputDataSize + " bytes" +
                ", outputDataSize=" + outputDataSize + " bytes" +
                ", deadline=" + deadline + " s" +
                ", priority=" + priority +
                ", status=" + status +
                ", assignedTo='" + (assignedResourceId != null ? assignedResourceId : "unassigned") + "'" +
                '}';
    }
    
    /**
     * Builder pattern for Task creation
     */
    public static class Builder {
        private String id;
        private long length;
        private long inputDataSize;
        private long outputDataSize;
        private double deadline;
        private double maxEnergy;
        private IoTNodeLocation sourceLocation;
        private TaskType type;
        private int priority;
        private long submissionTime;
        private TaskStatus status;
        
        public Builder() {
            this.id = UUID.randomUUID().toString();
            this.status = TaskStatus.CREATED;
            this.submissionTime = System.currentTimeMillis();
            this.priority = 5; // Default priority
        }
        
        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        
        public Builder withLength(long length) {
            this.length = length;
            return this;
        }
        
        public Builder withInputDataSize(long inputDataSize) {
            this.inputDataSize = inputDataSize;
            return this;
        }
        
        public Builder withOutputDataSize(long outputDataSize) {
            this.outputDataSize = outputDataSize;
            return this;
        }
        
        public Builder withDeadline(double deadline) {
            this.deadline = deadline;
            return this;
        }
        
        public Builder withMaxEnergy(double maxEnergy) {
            this.maxEnergy = maxEnergy;
            return this;
        }
        
        public Builder withSourceLocation(IoTNodeLocation location) {
            this.sourceLocation = location;
            return this;
        }
        
        public Builder withType(TaskType type) {
            this.type = type;
            return this;
        }
        
        public Builder withPriority(int priority) {
            this.priority = Math.min(10, Math.max(1, priority)); // Ensure 1-10 range
            return this;
        }
        
        public Builder withSubmissionTime(long submissionTime) {
            this.submissionTime = submissionTime;
            return this;
        }
        
        public Builder withStatus(TaskStatus status) {
            this.status = status;
            return this;
        }
        
        public Task build() {
            return new Task(this);
        }
    }
    
    /**
     * Represents different types of IoT applications with varying characteristics
     */
    public enum TaskType {
        REAL_TIME_VIDEO_ANALYTICS(5000, 2048, 256, 600.0, 10),      // High priority, increased deadline to 600s
        ENVIRONMENTAL_MONITORING(1000, 100, 20, 800.0, 4),         // Low priority, increased deadline to 800s
        EMERGENCY_RESPONSE(3000, 500, 200, 400.0, 10),              // High priority, increased deadline to 400s
        INDUSTRIAL_CONTROL(2000, 50, 30, 500.0, 8),                 // High priority, increased deadline to 500s
        SMART_AGRICULTURE(800, 150, 30, 1000.0, 3),                 // Low priority, increased deadline to 1000s
        TRAFFIC_MONITORING(1500, 1024, 100, 700.0, 6),              // Medium priority, increased deadline to 700s
        HEALTH_MONITORING(2500, 200, 50, 550.0, 9);                 // High priority, increased deadline to 550s
        
        private final long avgLength;         // average task length in MI
        private final long avgInputSize;      // average input size in KB
        private final long avgOutputSize;     // average output size in KB
        private final double avgDeadline;     // average deadline in seconds
        private final int defaultPriority;    // default priority (1-10)
        
        TaskType(long avgLength, long avgInputSize, long avgOutputSize, double avgDeadline, int defaultPriority) {
            this.avgLength = avgLength;
            this.avgInputSize = avgInputSize * 1024; // Convert KB to bytes
            this.avgOutputSize = avgOutputSize * 1024; // Convert KB to bytes
            this.avgDeadline = avgDeadline;
            this.defaultPriority = defaultPriority;
        }
        
        public long getAvgLength() {
            return avgLength;
        }
        
        public long getAvgInputSize() {
            return avgInputSize;
        }
        
        public long getAvgOutputSize() {
            return avgOutputSize;
        }
        
        public double getAvgDeadline() {
            return avgDeadline;
        }
        
        public int getDefaultPriority() {
            return defaultPriority;
        }
    }
    
    /**
     * Represents the status of a task in its lifecycle
     */
    public enum TaskStatus {
        CREATED,            // Task created but not yet submitted
        READY,              // Task ready for processing
        WAITING,            // Task waiting for assignment
        TRANSFERRING,       // Task being transferred to the processing node
        PROCESSING,         // Task currently being processed
        COMPLETED,          // Task completed successfully
        FAILED,             // Task execution failed
        DROPPED,            // Task dropped due to constraints (e.g., missed deadline)
        RETURNED            // Task results returned to the source
    }
}
