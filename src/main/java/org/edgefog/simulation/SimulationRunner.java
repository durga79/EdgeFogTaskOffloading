package org.edgefog.simulation;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.UAV;
import org.edgefog.offloading.DeepLearningOffloadingController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Runner class for configuring and executing simulations
 */
public class SimulationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunner.class);
    
    // Default simulation parameters
    private static final int DEFAULT_NUM_DEVICES = 30;
    private static final int DEFAULT_NUM_UAVS = 5;
    private static final double DEFAULT_AREA_WIDTH = 1000.0;  // meters
    private static final double DEFAULT_AREA_LENGTH = 1000.0; // meters
    private static final double DEFAULT_AREA_HEIGHT = 150.0;  // meters
    private static final double DEFAULT_TIME_STEP = 0.1;      // seconds
    private static final double DEFAULT_DURATION = 600.0;     // seconds (10 minutes)
    
    /**
     * Main method to run the simulation
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Multi-UAV Edge Computing Simulation");
        
        // Parse command line arguments
        int numDevices = DEFAULT_NUM_DEVICES;
        int numUAVs = DEFAULT_NUM_UAVS;
        double duration = DEFAULT_DURATION;
        boolean saveToCsv = false;
        
        if (args.length > 0) {
            try {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("--devices") && i < args.length - 1) {
                        numDevices = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("--uavs") && i < args.length - 1) {
                        numUAVs = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("--duration") && i < args.length - 1) {
                        duration = Double.parseDouble(args[++i]);
                    } else if (args[i].equals("--csv")) {
                        saveToCsv = true;
                    }
                }
            } catch (NumberFormatException e) {
                logger.error("Error parsing arguments: {}", e.getMessage());
                printUsage();
                return;
            }
        }
        
        // Create and run the simulation
        runSimulation(numDevices, numUAVs, duration, saveToCsv);
    }
    
    /**
     * Run the simulation with specified parameters
     * @param numDevices Number of IoT devices
     * @param numUAVs Number of UAVs
     * @param duration Duration in simulation seconds
     * @param saveToCsv Whether to save results to CSV
     */
    public static void runSimulation(int numDevices, int numUAVs, double duration, boolean saveToCsv) {
        logger.info("Configuring simulation with {} devices, {} UAVs for {} seconds",
                numDevices, numUAVs, duration);
        
        // Create simulation environment
        SimulationEnvironment env = new SimulationEnvironment(
                DEFAULT_AREA_WIDTH, DEFAULT_AREA_LENGTH, DEFAULT_AREA_HEIGHT, DEFAULT_TIME_STEP);
        
        // Initialize environment with devices and UAVs
        env.initialize(numDevices, numUAVs);
        
        // Run the simulation
        long startTime = System.currentTimeMillis();
        env.run(duration);
        long endTime = System.currentTimeMillis();
        
        // Display results
        SimulationMetrics metrics = env.getMetrics();
        String report = metrics.generateReport();
        logger.info("Simulation completed in {} ms", (endTime - startTime));
        logger.info("\n{}", report);
        
        // Output device and UAV statistics
        printDeviceStatistics(env);
        printUAVStatistics(env);
        
        // Save metrics to CSV if requested
        if (saveToCsv) {
            saveResultsToCSV(metrics, numDevices, numUAVs, duration);
        }
    }
    
    /**
     * Print statistics about IoT devices
     * @param env The simulation environment
     */
    private static void printDeviceStatistics(SimulationEnvironment env) {
        StringBuilder stats = new StringBuilder();
        stats.append("\n=== DEVICE STATISTICS ===\n");
        
        for (IoTDevice device : env.getDevices()) {
            stats.append("Device " + device.getId() + ": CPU=" + device.getCpuMips() + " MIPS, Memory=" + 
                    device.getMemoryMb() + " MB, Battery=" + String.format("%.2f", device.getRemainingBattery()) + "/" + 
                    String.format("%.2f", device.getBatteryCapacity()) + " J, Tasks Generated=" + 
                    device.getTasksGenerated() + "\n");
        }
        
        logger.info(stats.toString());
    }
    
    /**
     * Print statistics about UAVs
     * @param env The simulation environment
     */
    private static void printUAVStatistics(SimulationEnvironment env) {
        StringBuilder stats = new StringBuilder();
        stats.append("\n=== UAV STATISTICS ===\n");
        
        for (UAV uav : env.getUAVs()) {
            stats.append("UAV " + uav.getId() + ": CPU=" + uav.getMips() + " MIPS, Energy=" + 
                    String.format("%.2f", uav.getRemainingEnergy()) + "/" + String.format("%.2f", uav.getTotalEnergy()) + 
                    " J, Tasks Processed=" + uav.getCompletedTaskCount() + ", Current Load=" + 
                    String.format("%.2f%%", uav.getCurrentLoad() * 100) + "\n");
        }
        
        logger.info(stats.toString());
    }
    
    /**
     * Save simulation results to CSV file
     * @param metrics The metrics to save
     * @param numDevices Number of devices in simulation
     * @param numUAVs Number of UAVs in simulation
     * @param duration Duration of simulation
     */
    private static void saveResultsToCSV(SimulationMetrics metrics, int numDevices, int numUAVs, double duration) {
        try {
            // Create results directory if it doesn't exist
            Files.createDirectories(Paths.get("results"));
            
            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = String.format("results/sim_%dd_%du_%ds_%s.csv", 
                    numDevices, numUAVs, (int)duration, timestamp);
            
            // Write metrics to CSV
            try (FileWriter writer = new FileWriter(filename)) {
                Map<String, Object> metricsMap = metrics.getMetricsMap();
                
                // Write header
                writer.append("Metric,Value\n");
                
                // Write configuration
                writer.append("num_devices,").append(String.valueOf(numDevices)).append("\n");
                writer.append("num_uavs,").append(String.valueOf(numUAVs)).append("\n");
                writer.append("duration,").append(String.valueOf(duration)).append("\n");
                
                // Write metrics
                for (Map.Entry<String, Object> entry : metricsMap.entrySet()) {
                    if (!(entry.getValue() instanceof Map)) {
                        writer.append(entry.getKey()).append(",")
                              .append(entry.getValue().toString()).append("\n");
                    }
                }
            }
            
            logger.info("Results saved to {}", filename);
            
        } catch (IOException e) {
            logger.error("Failed to save results to CSV: {}", e.getMessage());
        }
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar EdgeFogTaskOffloading.jar [options]");
        System.out.println("Options:");
        System.out.println("  --devices <num>   : Number of IoT devices (default: " + DEFAULT_NUM_DEVICES + ")");
        System.out.println("  --uavs <num>      : Number of UAVs (default: " + DEFAULT_NUM_UAVS + ")");
        System.out.println("  --duration <sec>  : Simulation duration in seconds (default: " + DEFAULT_DURATION + ")");
        System.out.println("  --csv             : Save results to CSV file");
    }
    
    /**
     * Create and configure a reinforcement learning offloading controller
     * @param numUAVs Number of UAVs in the system
     * @param metrics Simulation metrics
     * @return Configured controller
     */
    public static DeepLearningOffloadingController createRLController(int numUAVs, SimulationMetrics metrics) {
        return new DeepLearningOffloadingController(numUAVs, true, 1000, metrics);
    }
}
