package org.edgefog.offloading;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.Task;
import org.edgefog.model.UAV;

import java.util.List;

/**
 * Interface for task offloading controllers that make decisions about
 * where to execute computational tasks (locally or on which UAV).
 */
public interface OffloadingController {
    
    /**
     * Make an offloading decision for a task
     * @param task The task to be offloaded
     * @param device The IoT device that generated the task
     * @param availableUAVs List of available UAVs for offloading
     * @return OffloadingDecision containing the target and selected UAV (if applicable)
     */
    OffloadingDecision makeOffloadingDecision(Task task, IoTDevice device, List<UAV> availableUAVs);
}
