package org.edgefog;

import org.edgefog.simulation.SimulationEnvironment;
import org.edgefog.simulation.SimulationMetrics;
import org.edgefog.simulation.SimulationRunner;
import org.edgefog.visualization.SimulationVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main application class for the Edge-Fog Task Offloading Simulation
 * with Deep Learning-based offloading decisions
 */
public class EdgeFogTaskOffloading {
    private static final Logger logger = LoggerFactory.getLogger(EdgeFogTaskOffloading.class);
    
    /**
     * Entry point for the application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Edge-Fog Task Offloading Application");
        
        if (args.length > 0 && args[0].equals("--gui")) {
            // Launch GUI mode
            SwingUtilities.invokeLater(EdgeFogTaskOffloading::createAndShowGUI);
        } else if (args.length > 0 && args[0].equals("--help")) {
            // Show help
            printUsage();
        } else {
            // Run in command line mode with default or provided parameters
            SimulationRunner.main(args);
        }
    }
    
    /**
     * Create and show the GUI for the application
     */
    private static void createAndShowGUI() {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set system look and feel", e);
        }
        
        // Create main frame
        JFrame frame = new JFrame("Edge-Fog Task Offloading Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create parameter panel
        JPanel paramPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        paramPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add parameter inputs
        paramPanel.add(new JLabel("Number of IoT Devices:"));
        JTextField devicesField = new JTextField("30", 5);
        paramPanel.add(devicesField);
        
        paramPanel.add(new JLabel("Number of UAVs:"));
        JTextField uavsField = new JTextField("5", 5);
        paramPanel.add(uavsField);
        
        paramPanel.add(new JLabel("Simulation Duration (seconds):"));
        JTextField durationField = new JTextField("600", 5);
        paramPanel.add(durationField);
        
        paramPanel.add(new JLabel("Visualization Mode:"));
        JComboBox<String> vizModeCombo = new JComboBox<>(new String[]{"Real-time", "Final Charts", "Both"});
        paramPanel.add(vizModeCombo);
        
        JCheckBox saveResultsCheckbox = new JCheckBox("Save Results to CSV", true);
        paramPanel.add(saveResultsCheckbox);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton runButton = new JButton("Run Simulation");
        buttonPanel.add(runButton);
        
        // Create status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        JTextArea statusArea = new JTextArea(10, 40);
        statusArea.setEditable(false);
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        // Add panels to frame
        frame.add(paramPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
        
        // Configure run button action
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Disable button during simulation
                runButton.setEnabled(false);
                
                // Clear status area
                statusArea.setText("");
                
                // Get parameters
                int numDevices;
                int numUAVs;
                double duration;
                try {
                    numDevices = Integer.parseInt(devicesField.getText());
                    numUAVs = Integer.parseInt(uavsField.getText());
                    duration = Double.parseDouble(durationField.getText());
                } catch (NumberFormatException ex) {
                    statusArea.append("Error: Invalid parameter values\n");
                    runButton.setEnabled(true);
                    return;
                }
                
                // Validate parameters
                if (numDevices <= 0 || numUAVs <= 0 || duration <= 0) {
                    statusArea.append("Error: Parameters must be positive values\n");
                    runButton.setEnabled(true);
                    return;
                }
                
                // Create result directory
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String resultDir = "results/sim_" + timestamp;
                try {
                    Files.createDirectories(Paths.get(resultDir));
                } catch (Exception ex) {
                    statusArea.append("Error creating results directory: " + ex.getMessage() + "\n");
                    runButton.setEnabled(true);
                    return;
                }
                
                // Run simulation in a background thread
                new Thread(() -> {
                    try {
                        // Create simulation environment
                        statusArea.append("Initializing simulation...\n");
                        SimulationEnvironment env = new SimulationEnvironment(
                                1000.0, 1000.0, 150.0, 0.1);
                        env.initialize(numDevices, numUAVs);
                        
                        // Create visualizer
                        SimulationVisualizer visualizer = new SimulationVisualizer(env, 1.0);
                        
                        // Start real-time visualization if selected
                        String vizMode = (String) vizModeCombo.getSelectedItem();
                        if ("Real-time".equals(vizMode) || "Both".equals(vizMode)) {
                            SwingUtilities.invokeLater(visualizer::startRealTimeVisualization);
                        }
                        
                        // Create sample timer for time series data
                        Timer sampleTimer = new Timer(1000, event -> visualizer.sampleMetrics());
                        sampleTimer.start();
                        
                        // Run the simulation
                        statusArea.append("Running simulation with " + numDevices + " devices and " + 
                                numUAVs + " UAVs for " + duration + " seconds...\n");
                        long startTime = System.currentTimeMillis();
                        env.run(duration);
                        long endTime = System.currentTimeMillis();
                        sampleTimer.stop();
                        
                        // Display results
                        SimulationMetrics metrics = env.getMetrics();
                        statusArea.append("Simulation completed in " + (endTime - startTime) + " ms\n");
                        statusArea.append(metrics.generateReport());
                        
                        // Show final charts if selected
                        if ("Final Charts".equals(vizMode) || "Both".equals(vizMode)) {
                            SwingUtilities.invokeLater(() -> visualizer.showFinalMetricsWindow(metrics));
                        }
                        
                        // Save charts to files
                        visualizer.saveChartsToFile(metrics, resultDir);
                        statusArea.append("Charts saved to " + resultDir + "\n");
                        
                        // Save results to CSV if selected
                        if (saveResultsCheckbox.isSelected()) {
                            String csvFile = resultDir + "/metrics.csv";
                            // Use the saveResultsToCSV method directly
                            new File(resultDir).mkdirs(); // Ensure directory exists
                            SimulationRunner.runSimulation(numDevices, numUAVs, duration, true);
                            statusArea.append("Results saved to CSV\n");
                        }
                        
                    } catch (Exception ex) {
                        statusArea.append("Error during simulation: " + ex.getMessage() + "\n");
                        ex.printStackTrace();
                    } finally {
                        // Re-enable button
                        SwingUtilities.invokeLater(() -> runButton.setEnabled(true));
                    }
                }).start();
            }
        });
        
        // Finalize and show frame
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    /**
     * Print usage information for command line mode
     */
    private static void printUsage() {
        System.out.println("Edge-Fog Task Offloading Simulation");
        System.out.println("Usage:");
        System.out.println("  java -jar EdgeFogTaskOffloading.jar [options]");
        System.out.println("Options:");
        System.out.println("  --help                Show this help message");
        System.out.println("  --gui                 Launch GUI mode");
        System.out.println("  --devices <num>       Number of IoT devices (default: 30)");
        System.out.println("  --uavs <num>          Number of UAVs (default: 5)");
        System.out.println("  --duration <sec>      Simulation duration in seconds (default: 600)");
        System.out.println("  --csv                 Save results to CSV file");
        System.out.println("Examples:");
        System.out.println("  java -jar EdgeFogTaskOffloading.jar");
        System.out.println("  java -jar EdgeFogTaskOffloading.jar --gui");
        System.out.println("  java -jar EdgeFogTaskOffloading.jar --devices 50 --uavs 10 --duration 1200 --csv");
    }
}
