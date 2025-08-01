package org.edgefog.offloading;

import org.edgefog.model.UAV;

/**
 * Represents a decision made by the offloading controller about where to execute a task.
 * Includes the target (local or UAV) and associated metrics like estimated latency and energy consumption.
 */
public class OffloadingDecision {
    private final OffloadingTarget target;
    private final UAV selectedUAV;
    private final double estimatedLatency;   // in seconds
    private final double estimatedEnergy;    // in joules
    
    /**
     * Constructor for OffloadingDecision
     * @param target The target for execution (LOCAL or UAV)
     * @param selectedUAV The selected UAV (null if target is LOCAL)
     * @param estimatedLatency Estimated latency in seconds
     * @param estimatedEnergy Estimated energy consumption in joules
     */
    public OffloadingDecision(OffloadingTarget target, UAV selectedUAV, double estimatedLatency, double estimatedEnergy) {
        this.target = target;
        this.selectedUAV = selectedUAV;
        this.estimatedLatency = estimatedLatency;
        this.estimatedEnergy = estimatedEnergy;
    }
    
    /**
     * Get the offloading target
     * @return The offloading target (LOCAL or UAV)
     */
    public OffloadingTarget getTarget() {
        return target;
    }
    
    /**
     * Get the selected UAV for task offloading
     * @return The selected UAV or null if target is LOCAL
     */
    public UAV getSelectedUAV() {
        return selectedUAV;
    }
    
    /**
     * Get the estimated latency of the offloading decision
     * @return Estimated latency in seconds
     */
    public double getEstimatedLatency() {
        return estimatedLatency;
    }
    
    /**
     * Get the estimated energy consumption of the offloading decision
     * @return Estimated energy consumption in joules
     */
    public double getEstimatedEnergy() {
        return estimatedEnergy;
    }
    
    @Override
    public String toString() {
        if (target == OffloadingTarget.LOCAL) {
            return String.format("OffloadingDecision{target=LOCAL, estimatedLatency=%.2fs, estimatedEnergy=%.2fJ}",
                    estimatedLatency, estimatedEnergy);
        } else {
            return String.format("OffloadingDecision{target=UAV, selectedUAV=%s, estimatedLatency=%.2fs, estimatedEnergy=%.2fJ}",
                    selectedUAV.getId(), estimatedLatency, estimatedEnergy);
        }
    }
}
