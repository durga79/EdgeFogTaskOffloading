package org.edgefog.visualization;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.UAV;
import org.edgefog.simulation.SimulationEnvironment;
import org.edgefog.simulation.SimulationMetrics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Visualization component for the multi-UAV edge computing simulation.
 * Creates charts and graphical representations of simulation results.
 */
public class SimulationVisualizer {
    private static final Logger logger = LoggerFactory.getLogger(SimulationVisualizer.class);
    
    // Time series data for real-time visualization
    private final Map<String, List<Double>> timeSeriesData;
    private final List<Double> timePoints;
    
    // Reference to simulation environment
    private final SimulationEnvironment environment;
    
    // Sampling interval for time series data (in simulation seconds)
    private final double samplingInterval;
    
    /**
     * Constructor for SimulationVisualizer
     * @param environment The simulation environment
     * @param samplingInterval Sampling interval for time series data (in simulation seconds)
     */
    public SimulationVisualizer(SimulationEnvironment environment, double samplingInterval) {
        this.environment = environment;
        this.samplingInterval = samplingInterval;
        this.timeSeriesData = new HashMap<>();
        this.timePoints = new ArrayList<>();
        
        initializeTimeSeriesData();
    }
    
    /**
     * Initialize time series data tracking
     */
    private void initializeTimeSeriesData() {
        // Task metrics
        timeSeriesData.put("completedTasks", new ArrayList<>());
        timeSeriesData.put("failedTasks", new ArrayList<>());
        timeSeriesData.put("droppedTasks", new ArrayList<>());
        
        // Success rate
        timeSeriesData.put("successRate", new ArrayList<>());
        
        // Latency metrics
        timeSeriesData.put("averageLatency", new ArrayList<>());
        
        // Energy metrics
        timeSeriesData.put("deviceEnergy", new ArrayList<>());
        timeSeriesData.put("uavEnergy", new ArrayList<>());
        
        // Deadline metrics
        timeSeriesData.put("deadlineMeetRate", new ArrayList<>());
        
        // UAV utilization
        timeSeriesData.put("avgUAVUtilization", new ArrayList<>());
    }
    
    /**
     * Sample metrics at current time point and add to time series data
     */
    public void sampleMetrics() {
        // Get current metrics
        SimulationMetrics metrics = environment.getMetrics();
        Map<String, Object> metricsMap = metrics.getMetricsMap();
        
        // Add current time point
        timePoints.add(environment.getCurrentTime());
        
        // Sample key metrics
        for (String key : timeSeriesData.keySet()) {
            if (metricsMap.containsKey(key)) {
                Object value = metricsMap.get(key);
                if (value instanceof Number) {
                    timeSeriesData.get(key).add(((Number) value).doubleValue());
                }
            }
        }
    }
    
    /**
     * Create a time series chart of a specific metric
     * @param title Chart title
     * @param xAxisLabel X-axis label
     * @param yAxisLabel Y-axis label
     * @param metricKeys List of metrics to plot
     * @param metricLabels Labels for each metric
     * @return JFreeChart instance
     */
    public JFreeChart createTimeSeriesChart(String title, String xAxisLabel, String yAxisLabel,
                                           List<String> metricKeys, List<String> metricLabels) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        // Add each metric series
        for (int i = 0; i < metricKeys.size(); i++) {
            String key = metricKeys.get(i);
            String label = metricLabels.get(i);
            
            if (!timeSeriesData.containsKey(key)) {
                logger.warn("No time series data found for metric: {}", key);
                continue;
            }
            
            XYSeries series = new XYSeries(label);
            List<Double> values = timeSeriesData.get(key);
            
            // Add data points
            for (int j = 0; j < Math.min(timePoints.size(), values.size()); j++) {
                series.add(timePoints.get(j), values.get(j));
            }
            
            dataset.addSeries(series);
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        // Customize chart
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesLinesVisible(i, true);
        }
        plot.setRenderer(renderer);
        
        return chart;
    }
    
    /**
     * Create a bar chart for comparing metrics
     * @param title Chart title
     * @param xAxisLabel X-axis label
     * @param yAxisLabel Y-axis label
     * @param categoryLabels Category labels
     * @param values Values for each category
     * @return JFreeChart instance
     */
    public JFreeChart createBarChart(String title, String xAxisLabel, String yAxisLabel,
                                    List<String> categoryLabels, List<Number> values) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (int i = 0; i < Math.min(categoryLabels.size(), values.size()); i++) {
            dataset.addValue(values.get(i), "Value", categoryLabels.get(i));
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        
        // Customize chart
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Set y-axis to start at 0
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLowerBound(0);
        
        return chart;
    }
    
    /**
     * Create charts for final metrics and display in a GUI window
     * @param metrics The simulation metrics to visualize
     */
    public void showFinalMetricsWindow(SimulationMetrics metrics) {
        JFrame frame = new JFrame("Multi-UAV Edge Computing Simulation Results");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 2));
        
        // Create task completion chart
        List<String> taskLabels = Arrays.asList("Completed", "Failed", "Dropped");
        Map<String, Object> metricsMap = metrics.getMetricsMap();
        List<Number> taskValues = Arrays.asList(
                (Number) metricsMap.get("completedTasks"),
                (Number) metricsMap.get("failedTasks"),
                (Number) metricsMap.get("droppedTasks")
        );
        JFreeChart taskChart = createBarChart("Task Statistics", "Task Status", "Count", 
                taskLabels, taskValues);
        
        // Create latency chart
        List<String> latencyLabels = Arrays.asList("Average", "Minimum", "Maximum");
        List<Number> latencyValues = Arrays.asList(
                (Number) metricsMap.get("averageLatency"),
                (Number) metricsMap.get("minLatency"),
                (Number) metricsMap.get("maxLatency")
        );
        JFreeChart latencyChart = createBarChart("Latency Statistics (ms)", "Latency Type", "Milliseconds", 
                latencyLabels, latencyValues);
        
        // Create energy chart
        List<String> energyLabels = Arrays.asList("Device", "UAV", "Total");
        List<Number> energyValues = Arrays.asList(
                (Number) metricsMap.get("deviceEnergy"),
                (Number) metricsMap.get("uavEnergy"),
                (Number) metricsMap.get("totalEnergy")
        );
        JFreeChart energyChart = createBarChart("Energy Consumption (J)", "Component", "Joules", 
                energyLabels, energyValues);
        
        // Create offloading decision pie chart
        Map<String, Integer> decisions = (Map<String, Integer>) metricsMap.get("offloadingDecisions");
        DefaultCategoryDataset decisionDataset = new DefaultCategoryDataset();
        if (decisions != null) {
            for (Map.Entry<String, Integer> entry : decisions.entrySet()) {
                decisionDataset.addValue(entry.getValue(), "Count", entry.getKey());
            }
        }
        JFreeChart decisionChart = ChartFactory.createBarChart(
                "Offloading Decisions", "Target", "Count", decisionDataset, 
                PlotOrientation.VERTICAL, false, true, false);
        
        // Add charts to frame
        frame.add(new ChartPanel(taskChart));
        frame.add(new ChartPanel(latencyChart));
        frame.add(new ChartPanel(energyChart));
        frame.add(new ChartPanel(decisionChart));
        
        // Display frame
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    /**
     * Save charts as PNG files
     * @param metrics The simulation metrics to visualize
     * @param outputDirectory Directory to save images
     */
    public void saveChartsToFile(SimulationMetrics metrics, String outputDirectory) {
        try {
            // Create output directory if it doesn't exist
            File dir = new File(outputDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Generate timestamp for filenames
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            
            // Create and save task chart
            List<String> taskLabels = Arrays.asList("Completed", "Failed", "Dropped");
            Map<String, Object> metricsMap = metrics.getMetricsMap();
            List<Number> taskValues = Arrays.asList(
                    (Number) metricsMap.get("completedTasks"),
                    (Number) metricsMap.get("failedTasks"),
                    (Number) metricsMap.get("droppedTasks")
            );
            JFreeChart taskChart = createBarChart("Task Statistics", "Task Status", "Count", 
                    taskLabels, taskValues);
            ChartUtils.saveChartAsPNG(new File(outputDirectory + "/tasks_" + timestamp + ".png"), 
                    taskChart, 800, 600);
            
            // Create and save latency chart
            List<String> latencyLabels = Arrays.asList("Average", "Minimum", "Maximum");
            List<Number> latencyValues = Arrays.asList(
                    (Number) metricsMap.get("averageLatency"),
                    (Number) metricsMap.get("minLatency"),
                    (Number) metricsMap.get("maxLatency")
            );
            JFreeChart latencyChart = createBarChart("Latency Statistics (ms)", "Latency Type", "Milliseconds", 
                    latencyLabels, latencyValues);
            ChartUtils.saveChartAsPNG(new File(outputDirectory + "/latency_" + timestamp + ".png"), 
                    latencyChart, 800, 600);
            
            // Create and save energy chart
            List<String> energyLabels = Arrays.asList("Device", "UAV", "Total");
            List<Number> energyValues = Arrays.asList(
                    (Number) metricsMap.get("deviceEnergy"),
                    (Number) metricsMap.get("uavEnergy"),
                    (Number) metricsMap.get("totalEnergy")
            );
            JFreeChart energyChart = createBarChart("Energy Consumption (J)", "Component", "Joules", 
                    energyLabels, energyValues);
            ChartUtils.saveChartAsPNG(new File(outputDirectory + "/energy_" + timestamp + ".png"), 
                    energyChart, 800, 600);
            
            // Save time series charts if available
            if (!timePoints.isEmpty()) {
                // Task completion rate over time
                JFreeChart taskTimeChart = createTimeSeriesChart(
                        "Task Completion Over Time", "Simulation Time (s)", "Count",
                        Arrays.asList("completedTasks", "failedTasks", "droppedTasks"),
                        Arrays.asList("Completed", "Failed", "Dropped")
                );
                ChartUtils.saveChartAsPNG(new File(outputDirectory + "/task_time_" + timestamp + ".png"), 
                        taskTimeChart, 800, 600);
                
                // Success rate over time
                JFreeChart successRateChart = createTimeSeriesChart(
                        "Success Rate Over Time", "Simulation Time (s)", "Success Rate (%)",
                        Collections.singletonList("successRate"),
                        Collections.singletonList("Success Rate")
                );
                ChartUtils.saveChartAsPNG(new File(outputDirectory + "/success_rate_" + timestamp + ".png"), 
                        successRateChart, 800, 600);
                
                // UAV utilization over time
                JFreeChart utilizationChart = createTimeSeriesChart(
                        "UAV Utilization Over Time", "Simulation Time (s)", "Utilization (%)",
                        Collections.singletonList("avgUAVUtilization"),
                        Collections.singletonList("Average Utilization")
                );
                ChartUtils.saveChartAsPNG(new File(outputDirectory + "/utilization_" + timestamp + ".png"), 
                        utilizationChart, 800, 600);
            }
            
            logger.info("Charts saved to directory: {}", outputDirectory);
            
        } catch (IOException e) {
            logger.error("Error saving charts: {}", e.getMessage());
        }
    }
    
    /**
     * Create a visualization of UAV and device positions
     * @return JPanel containing the visualization
     */
    public JPanel createPositionVisualization() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // Set up the visualization
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Calculate scale factors
                double scaleX = getWidth() / environment.getAreaWidth();
                double scaleY = getHeight() / environment.getAreaLength();
                double scale = Math.min(scaleX, scaleY);
                
                // Draw IoT devices
                g2d.setColor(Color.BLUE);
                for (IoTDevice device : environment.getDevices()) {
                    int x = (int) (device.getLocation().getX() * scale);
                    int y = (int) (device.getLocation().getY() * scale);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
                
                // Draw UAVs
                g2d.setColor(Color.RED);
                for (UAV uav : environment.getUAVs()) {
                    int x = (int) (uav.getLocation().getX() * scale);
                    int y = (int) (uav.getLocation().getY() * scale);
                    g2d.fillOval(x - 5, y - 5, 10, 10);
                    
                    // Draw UAV range circle
                    g2d.setColor(new Color(255, 0, 0, 30)); // Transparent red
                    int radius = (int) (uav.getCommunicationRange() * scale);
                    g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                    g2d.setColor(Color.RED);
                }
                
                // Draw legend
                g2d.setColor(Color.BLACK);
                g2d.drawString("IoT Devices (Blue)", 10, 20);
                g2d.drawString("UAVs (Red)", 10, 40);
            }
        };
    }
    
    /**
     * Start a real-time visualization window
     */
    public void startRealTimeVisualization() {
        JFrame frame = new JFrame("Multi-UAV Edge Computing Simulation");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create position visualization panel
        JPanel positionPanel = createPositionVisualization();
        positionPanel.setPreferredSize(new Dimension(600, 600));
        
        // Create a panel for metrics
        JPanel metricsPanel = new JPanel(new GridLayout(3, 2));
        
        // Create metric labels
        JLabel completedTasksLabel = new JLabel("Completed Tasks: 0");
        JLabel successRateLabel = new JLabel("Success Rate: 0.0%");
        JLabel avgLatencyLabel = new JLabel("Average Latency: 0.0 ms");
        JLabel deadlineMeetRateLabel = new JLabel("Deadline Meet Rate: 0.0%");
        JLabel energyLabel = new JLabel("Energy Consumption: 0.0 J");
        JLabel uavUtilLabel = new JLabel("UAV Utilization: 0.0%");
        
        metricsPanel.add(completedTasksLabel);
        metricsPanel.add(successRateLabel);
        metricsPanel.add(avgLatencyLabel);
        metricsPanel.add(deadlineMeetRateLabel);
        metricsPanel.add(energyLabel);
        metricsPanel.add(uavUtilLabel);
        
        // Add panels to frame
        frame.add(positionPanel, BorderLayout.CENTER);
        frame.add(metricsPanel, BorderLayout.SOUTH);
        
        // Display frame
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Start update timer
        javax.swing.Timer timer = new javax.swing.Timer(100, e -> {
            // Update position visualization
            positionPanel.repaint();
            
            // Update metric labels
            SimulationMetrics metrics = environment.getMetrics();
            Map<String, Object> metricsMap = metrics.getMetricsMap();
            
            completedTasksLabel.setText("Completed Tasks: " + metricsMap.get("completedTasks"));
            successRateLabel.setText(String.format("Success Rate: %.2f%%", metrics.getSuccessRate()));
            avgLatencyLabel.setText(String.format("Average Latency: %.2f ms", metrics.getAverageLatency()));
            deadlineMeetRateLabel.setText(String.format("Deadline Meet Rate: %.2f%%", metrics.getDeadlineMeetRate()));
            energyLabel.setText(String.format("Energy: %.2f J", 
                    ((Number) metricsMap.get("totalEnergy")).doubleValue() / 1000.0));
            uavUtilLabel.setText(String.format("UAV Utilization: %.2f%%", metrics.getAverageUAVUtilization()));
        });
        timer.start();
    }
}
