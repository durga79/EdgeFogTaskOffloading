package org.edgefog.offloading;

/**
 * Enum representing possible targets for task offloading
 */
public enum OffloadingTarget {
    LOCAL, // Execute task on the local IoT device
    UAV,   // Offload task to a UAV-mounted edge server
    CLOUD  // Offload task to the cloud (for future extension)
}
