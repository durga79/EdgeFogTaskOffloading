package org.edgefog.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and reports metrics for the multi-UAV edge computing simulation.
 * Tracks metrics related to task processing, energy consumption, network usage, and offloading decisions.
 */
public class SimulationMetrics {
    // Task metrics
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);
    private final AtomicInteger droppedTasks = new AtomicInteger(0);
    
    // Latency metrics (in milliseconds)
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong maxLatency = new AtomicLong(0);
    private double minLatency = Double.MAX_VALUE;
    
    // Energy metrics (in joules)
    private final AtomicLong totalEnergyConsumption = new AtomicLong(0);
    private final AtomicLong deviceEnergyConsumption = new AtomicLong(0);
    private final AtomicLong uavEnergyConsumption = new AtomicLong(0);
    
    // Network metrics (in bytes)
    private final AtomicLong totalDataTransferred = new AtomicLong(0);
    
    // Offloading decision counts
    private final Map<String, AtomicInteger> offloadingDecisions = new ConcurrentHashMap<>();
    
    // Deadline metrics
    private final AtomicInteger deadlinesMet = new AtomicInteger(0);
    private final AtomicInteger deadlinesMissed = new AtomicInteger(0);
    
    // Resource utilization
    private final Map<String, Double> avgUavUtilization = new ConcurrentHashMap<>();
    
    /**
     * Record a new task created in the system
     */
    public void recordTaskCreation() {
        totalTasks.incrementAndGet();
    }
    
    /**
     * Record a completed task
     * @param latencyMs Processing latency in milliseconds
     * @param energyJ Energy consumed in joules
     * @param metDeadline Whether task met its deadline
     */
    public void recordTaskCompletion(double latencyMs, double energyJ, boolean metDeadline) {
        completedTasks.incrementAndGet();
        
        // Update latency metrics
        totalLatency.addAndGet(Math.round(latencyMs));
        if (latencyMs > maxLatency.get()) {
            maxLatency.set(Math.round(latencyMs));
        }
        if (latencyMs < minLatency) {
            minLatency = latencyMs;
        }
        
        // Update energy metrics
        totalEnergyConsumption.addAndGet(Math.round(energyJ));
        
        // Update deadline metrics
        if (metDeadline) {
            deadlinesMet.incrementAndGet();
        } else {
            deadlinesMissed.incrementAndGet();
        }
    }
    
    /**
     * Record a failed task
     */
    public void recordTaskFailure() {
        failedTasks.incrementAndGet();
    }
    
    /**
     * Record a dropped task (e.g., due to resource constraints)
     */
    public void recordTaskDropped() {
        droppedTasks.incrementAndGet();
    }
    
    /**
     * Record energy consumption by an IoT device
     * @param energyJ Energy consumed in joules
     */
    public void recordDeviceEnergyConsumption(double energyJ) {
        deviceEnergyConsumption.addAndGet(Math.round(energyJ));
    }
    
    /**
     * Record energy consumption by a UAV
     * @param energyJ Energy consumed in joules
     */
    public void recordUAVEnergyConsumption(double energyJ) {
        uavEnergyConsumption.addAndGet(Math.round(energyJ));
    }
    
    /**
     * Record data transferred over the network
     * @param bytes Number of bytes transferred
     */
    public void recordDataTransfer(long bytes) {
        totalDataTransferred.addAndGet(bytes);
    }
    
    /**
     * Record an offloading decision
     * @param decisionType Type of decision (e.g., "local", "uav", "cloud")
     */
    public void recordOffloadingDecision(String decisionType) {
        offloadingDecisions.computeIfAbsent(decisionType, k -> new AtomicInteger(0))
                          .incrementAndGet();
    }
    
    /**
     * Update the average utilization for a UAV
     * @param uavId ID of the UAV
     * @param utilization Utilization as a percentage (0-100)
     */
    public void updateUAVUtilization(String uavId, double utilization) {
        avgUavUtilization.put(uavId, utilization);
    }
    
    /**
     * Get the success rate of task completion
     * @return Percentage of tasks successfully completed
     */
    public double getSuccessRate() {
        int total = totalTasks.get();
        if (total == 0) {
            return 0.0;
        }
        return (completedTasks.get() * 100.0) / total;
    }
    
    /**
     * Get the failure rate of tasks
     * @return Percentage of tasks that failed
     */
    public double getFailureRate() {
        int total = totalTasks.get();
        if (total == 0) {
            return 0.0;
        }
        return (failedTasks.get() * 100.0) / total;
    }
    
    /**
     * Get the average latency of completed tasks
     * @return Average latency in milliseconds
     */
    public double getAverageLatency() {
        int completed = completedTasks.get();
        if (completed == 0) {
            return 0.0;
        }
        return totalLatency.get() / (double) completed;
    }
    
    /**
     * Get the average energy consumption per task
     * @return Average energy in joules
     */
    public double getAverageEnergyPerTask() {
        int completed = completedTasks.get();
        if (completed == 0) {
            return 0.0;
        }
        return totalEnergyConsumption.get() / (double) completed;
    }
    
    /**
     * Get the deadline meet rate
     * @return Percentage of tasks that met their deadlines
     */
    public double getDeadlineMeetRate() {
        int total = deadlinesMet.get() + deadlinesMissed.get();
        if (total == 0) {
            return 0.0;
        }
        return (deadlinesMet.get() * 100.0) / total;
    }
    
    /**
     * Get the average UAV utilization
     * @return Average UAV CPU utilization as a percentage
     */
    public double getAverageUAVUtilization() {
        if (avgUavUtilization.isEmpty()) {
            return 0.0;
        }
        
        double sum = avgUavUtilization.values().stream()
                                     .mapToDouble(Double::doubleValue)
                                     .sum();
        
        return sum / avgUavUtilization.size();
    }
    
    /**
     * Generate a full report of the collected metrics
     * @return A string representation of all metrics
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== SIMULATION METRICS REPORT ===\n\n");
        
        // Task statistics
        report.append("TASK STATISTICS:\n");
        report.append(String.format("  Total Tasks: %d\n", totalTasks.get()));
        report.append(String.format("  Completed Tasks: %d (%.2f%%)\n", 
                completedTasks.get(), getSuccessRate()));
        report.append(String.format("  Failed Tasks: %d (%.2f%%)\n", 
                failedTasks.get(), getFailureRate()));
        report.append(String.format("  Dropped Tasks: %d (%.2f%%)\n", 
                droppedTasks.get(), (droppedTasks.get() * 100.0) / Math.max(1, totalTasks.get())));
        
        // Deadline statistics
        report.append("\nDEADLINE STATISTICS:\n");
        report.append(String.format("  Deadlines Met: %d (%.2f%%)\n", 
                deadlinesMet.get(), getDeadlineMeetRate()));
        report.append(String.format("  Deadlines Missed: %d (%.2f%%)\n", 
                deadlinesMissed.get(), 100.0 - getDeadlineMeetRate()));
        
        // Latency statistics
        report.append("\nLATENCY STATISTICS:\n");
        report.append(String.format("  Average Latency: %.2f ms\n", getAverageLatency()));
        report.append(String.format("  Minimum Latency: %.2f ms\n", minLatency == Double.MAX_VALUE ? 0 : minLatency));
        report.append(String.format("  Maximum Latency: %d ms\n", maxLatency.get()));
        
        // Energy statistics
        report.append("\nENERGY STATISTICS:\n");
        report.append(String.format("  Total Energy Consumption: %.2f J\n", totalEnergyConsumption.get() / 1000.0));
        report.append(String.format("  Average Energy per Task: %.2f J\n", getAverageEnergyPerTask() / 1000.0));
        report.append(String.format("  Device Energy Consumption: %.2f J\n", deviceEnergyConsumption.get() / 1000.0));
        report.append(String.format("  UAV Energy Consumption: %.2f J\n", uavEnergyConsumption.get() / 1000.0));
        
        // Network statistics
        report.append("\nNETWORK STATISTICS:\n");
        report.append(String.format("  Total Data Transferred: %.2f MB\n", 
                totalDataTransferred.get() / (1024.0 * 1024.0)));
        report.append(String.format("  Average Data per Task: %.2f KB\n", 
                totalTasks.get() > 0 ? (totalDataTransferred.get() / totalTasks.get()) / 1024.0 : 0));
        
        // Offloading statistics
        report.append("\nOFFLOADING STATISTICS:\n");
        offloadingDecisions.forEach((type, count) -> 
            report.append(String.format("  %s: %d (%.2f%%)\n", 
                    type, count.get(), (count.get() * 100.0) / Math.max(1, totalTasks.get())))
        );
        
        // Resource utilization
        report.append("\nRESOURCE UTILIZATION:\n");
        report.append(String.format("  Average UAV Utilization: %.2f%%\n", getAverageUAVUtilization()));
        
        return report.toString();
    }
    
    /**
     * Get a map of metrics for visualization or further analysis
     * @return Map of metric names to values
     */
    public Map<String, Object> getMetricsMap() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Task statistics
        metrics.put("totalTasks", totalTasks.get());
        metrics.put("completedTasks", completedTasks.get());
        metrics.put("failedTasks", failedTasks.get());
        metrics.put("droppedTasks", droppedTasks.get());
        metrics.put("successRate", getSuccessRate());
        
        // Latency statistics
        metrics.put("averageLatency", getAverageLatency());
        metrics.put("minLatency", minLatency == Double.MAX_VALUE ? 0 : minLatency);
        metrics.put("maxLatency", maxLatency.get());
        
        // Energy statistics
        metrics.put("totalEnergy", totalEnergyConsumption.get());
        metrics.put("averageEnergyPerTask", getAverageEnergyPerTask());
        metrics.put("deviceEnergy", deviceEnergyConsumption.get());
        metrics.put("uavEnergy", uavEnergyConsumption.get());
        
        // Network statistics
        metrics.put("totalDataTransferred", totalDataTransferred.get());
        
        // Deadline statistics
        metrics.put("deadlinesMet", deadlinesMet.get());
        metrics.put("deadlinesMissed", deadlinesMissed.get());
        metrics.put("deadlineMeetRate", getDeadlineMeetRate());
        
        // Resource utilization
        metrics.put("averageUAVUtilization", getAverageUAVUtilization());
        metrics.put("uavUtilization", new HashMap<>(avgUavUtilization));
        
        // Offloading decisions
        Map<String, Integer> decisions = new HashMap<>();
        offloadingDecisions.forEach((type, count) -> decisions.put(type, count.get()));
        metrics.put("offloadingDecisions", decisions);
        
        return metrics;
    }
    
    /**
     * Reset all metrics to their initial values
     */
    public void reset() {
        totalTasks.set(0);
        completedTasks.set(0);
        failedTasks.set(0);
        droppedTasks.set(0);
        totalLatency.set(0);
        maxLatency.set(0);
        minLatency = Double.MAX_VALUE;
        totalEnergyConsumption.set(0);
        deviceEnergyConsumption.set(0);
        uavEnergyConsumption.set(0);
        totalDataTransferred.set(0);
        offloadingDecisions.clear();
        deadlinesMet.set(0);
        deadlinesMissed.set(0);
        avgUavUtilization.clear();
    }
}
