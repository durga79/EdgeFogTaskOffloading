package org.edgefog.model;

/**
 * Represents the 3D location of an IoT node in the simulation environment.
 * This is used to calculate distance to UAVs and other nodes in the network.
 */
public class IoTNodeLocation {
    private final double x; // x-coordinate in meters
    private final double y; // y-coordinate in meters
    private final double z; // z-coordinate in meters (altitude)

    public IoTNodeLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
    
    /**
     * Calculate Euclidean distance to another location
     * @param other The other location
     * @return Distance in meters
     */
    public double distanceTo(IoTNodeLocation other) {
        return Math.sqrt(
            Math.pow(this.x - other.x, 2) +
            Math.pow(this.y - other.y, 2) +
            Math.pow(this.z - other.z, 2)
        );
    }
    
    /**
     * Calculate signal path loss based on distance using a simple log-distance model
     * @param other The other location
     * @return Path loss in dB
     */
    public double calculatePathLoss(IoTNodeLocation other) {
        double distance = this.distanceTo(other);
        // Free space path loss model: PL(d) = PL(d0) + 10*n*log10(d/d0)
        // where d0 is reference distance (1m), n is path loss exponent (typically 2-4)
        double referenceDistance = 1.0; // 1 meter
        double pathLossExponent = 3.0; // Urban environment
        double pathLossAtReferenceDb = 40.0; // Path loss at reference distance
        
        return pathLossAtReferenceDb + 10 * pathLossExponent * 
               Math.log10(Math.max(distance, referenceDistance) / referenceDistance);
    }
    
    /**
     * Calculate signal-to-noise ratio (SNR) based on distance and transmit power
     * @param other The other location
     * @param transmitPowerDbm Transmit power in dBm
     * @param noisePowerDbm Noise power in dBm
     * @return SNR in dB
     */
    public double calculateSNR(IoTNodeLocation other, double transmitPowerDbm, double noisePowerDbm) {
        double pathLossDb = calculatePathLoss(other);
        double receivedPowerDbm = transmitPowerDbm - pathLossDb;
        return receivedPowerDbm - noisePowerDbm;
    }
    
    @Override
    public String toString() {
        return String.format("Location(x=%.2f, y=%.2f, z=%.2f)", x, y, z);
    }
}
