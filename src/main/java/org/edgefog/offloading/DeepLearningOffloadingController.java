package org.edgefog.offloading;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.edgefog.model.IoTDevice;
import org.edgefog.model.Task;
import org.edgefog.model.UAV;
import org.edgefog.simulation.SimulationMetrics;

/**
 * Deep Learning based task offloading controller for Multi-UAV Mobile Edge Computing.
 * This controller implements the DL approach from the IEEE paper to make offloading decisions.
 */
public class DeepLearningOffloadingController implements OffloadingController {
    private static final Logger logger = LoggerFactory.getLogger(DeepLearningOffloadingController.class);
    
    private MultiLayerNetwork model;
    private final int inputFeatures;
    private final int numUAVs;
    private final int outputSize;
    private final boolean enableLocalExecution;
    private final double learningRate;
    
    // Experience replay buffer for training
    private final List<OffloadingExperience> experienceBuffer;
    private final int experienceBufferSize;
    
    // Simulation metrics for performance evaluation
    private final SimulationMetrics metrics;
    
    /**
     * Constructor for DeepLearningOffloadingController
     * @param numUAVs Number of UAVs in the system
     * @param enableLocalExecution Whether local execution is an option
     * @param experienceBufferSize Size of the experience replay buffer
     */
    public DeepLearningOffloadingController(int numUAVs, boolean enableLocalExecution, 
                                           int experienceBufferSize, SimulationMetrics metrics) {
        this.numUAVs = numUAVs;
        this.enableLocalExecution = enableLocalExecution;
        this.outputSize = enableLocalExecution ? numUAVs + 1 : numUAVs;
        this.experienceBufferSize = experienceBufferSize;
        this.experienceBuffer = new ArrayList<>(experienceBufferSize);
        this.metrics = metrics;
        
        // Input features:
        // 1. Task features: length, input size, output size, deadline, priority
        // 2. IoT device features: location, CPU, battery level
        // 3. UAV features: for each UAV: location, CPU load, battery level
        this.inputFeatures = 5 + 3 + (numUAVs * 3);
        this.learningRate = 0.001;
        
        // Build and initialize the neural network model
        buildModel();
    }
    
    /**
     * Build the deep learning model for offloading decisions
     */
    private void buildModel() {
        logger.info("Building deep learning model with {} input features and {} output classes", 
                inputFeatures, outputSize);
        
        // Create neural network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(learningRate))
                .l2(0.0001)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(inputFeatures)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(128)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(64)
                        .nOut(outputSize)
                        .activation(Activation.SOFTMAX)
                        .build())
                .build();
        
        // Initialize the model
        model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(100));
        
        logger.info("Deep learning model initialized successfully");
    }
    
    /**
     * Load a pre-trained model from file
     * @param modelFile Path to the model file
     * @throws IOException If model cannot be loaded
     */
    public void loadModel(String modelFile) throws IOException {
        logger.info("Loading model from file: {}", modelFile);
        model = ModelSerializer.restoreMultiLayerNetwork(new File(modelFile));
        logger.info("Model loaded successfully");
    }
    
    /**
     * Save the current model to file
     * @param modelFile Path to save the model
     * @throws IOException If model cannot be saved
     */
    public void saveModel(String modelFile) throws IOException {
        logger.info("Saving model to file: {}", modelFile);
        ModelSerializer.writeModel(model, new File(modelFile), true);
        logger.info("Model saved successfully");
    }
    
    /**
     * Make an offloading decision for a task
     * @param task The task to be offloaded
     * @param device The IoT device that generated the task
     * @param availableUAVs List of available UAVs for offloading
     * @return OffloadingDecision with target UAV or null for local execution
     */
    @Override
    public OffloadingDecision makeOffloadingDecision(Task task, IoTDevice device, List<UAV> availableUAVs) {
        logger.debug("Making offloading decision for task {} from device {}", task.getId(), device.getId());
        
        // Ensure we have the correct number of UAVs
        if (availableUAVs.size() > numUAVs) {
            logger.warn("More UAVs available than model was configured for. Using only the first {} UAVs", numUAVs);
            availableUAVs = availableUAVs.subList(0, numUAVs);
        } else if (availableUAVs.size() < numUAVs) {
            logger.warn("Fewer UAVs available ({}) than model was configured for ({})", availableUAVs.size(), numUAVs);
            // Add dummy UAVs with high load and low battery to discourage selection
            while (availableUAVs.size() < numUAVs) {
                UAV dummyUAV = new UAV.Builder()
                        .withLocation(device.getLocation())  // Same location as device
                        .withMips(1)                         // Minimal processing power
                        .withTotalEnergy(1)                  // Minimal energy
                        .build();
                availableUAVs.add(dummyUAV);
            }
        }
        
        // Create input feature vector
        INDArray features = createFeatureVector(task, device, availableUAVs);
        
        // Get model prediction
        INDArray output = model.output(features);
        
        // Get the index of the maximum probability
        int decision = Nd4j.argMax(output, 1).getInt(0);
        
        // Create offloading decision based on model output
        if (decision < numUAVs) {
            // Offload to UAV
            UAV selectedUAV = availableUAVs.get(decision);
            double estimatedEnergy = device.calculateOffloadingEnergy(task, selectedUAV);
            double estimatedLatency = device.estimateTotalOffloadingTime(task, selectedUAV);
            
            // Check if the selected UAV can actually handle this task
            if (!selectedUAV.canProcessTask(task)) {
                logger.warn("DL model selected UAV {} that cannot process task {}. Falling back to local execution.",
                        selectedUAV.getId(), task.getId());
                
                // Record that this was actually executed locally despite model suggesting UAV
                metrics.recordOffloadingDecision("local");
                
                return new OffloadingDecision(OffloadingTarget.LOCAL, null, 
                        device.estimateLocalExecutionTime(task),
                        task.calculateLocalEnergyConsumption(device.getCpuPowerConsumption()));
            }
            
            // Record actual UAV offloading only after confirming UAV can process it
            metrics.recordOffloadingDecision("uav");
            
            return new OffloadingDecision(OffloadingTarget.UAV, selectedUAV, estimatedLatency, estimatedEnergy);
        } else if (enableLocalExecution) {
            // Local execution
            double estimatedLatency = device.estimateLocalExecutionTime(task);
            double estimatedEnergy = task.calculateLocalEnergyConsumption(device.getCpuPowerConsumption());
            
            // Record local offloading decision
            metrics.recordOffloadingDecision("local");
            
            return new OffloadingDecision(OffloadingTarget.LOCAL, null, estimatedLatency, estimatedEnergy);
        } else {
            // Model error or constraint violation - default to local execution
            logger.warn("Model selected local execution when not enabled. Using default UAV.");
            UAV defaultUAV = availableUAVs.get(0);
            
            // Check if the default UAV can actually handle this task
            if (!defaultUAV.canProcessTask(task)) {
                // If default UAV can't process, fall back to local
                logger.warn("Default UAV {} cannot process task {}. Forcing local execution.",
                        defaultUAV.getId(), task.getId());
                
                metrics.recordOffloadingDecision("local");
                return new OffloadingDecision(OffloadingTarget.LOCAL, null,
                        device.estimateLocalExecutionTime(task),
                        task.calculateLocalEnergyConsumption(device.getCpuPowerConsumption()));
            }
            
            double estimatedEnergy = device.calculateOffloadingEnergy(task, defaultUAV);
            double estimatedLatency = device.estimateTotalOffloadingTime(task, defaultUAV);
            
            // Record default UAV offloading
            metrics.recordOffloadingDecision("uav");
            
            return new OffloadingDecision(OffloadingTarget.UAV, defaultUAV, estimatedLatency, estimatedEnergy);
        }
    }
    
    /**
     * Create feature vector for the neural network based on task, device and UAV characteristics
     * @param task Task to be offloaded
     * @param device Source IoT device
     * @param availableUAVs Available UAVs for offloading
     * @return INDArray containing the feature vector
     */
    private INDArray createFeatureVector(Task task, IoTDevice device, List<UAV> availableUAVs) {
        double[] features = new double[inputFeatures];
        int idx = 0;
        
        // Task features - normalize to reasonable ranges
        features[idx++] = task.getLength() / 10000.0;             // Normalize MI to range ~0-1
        features[idx++] = task.getInputDataSize() / (1024.0 * 1024.0); // Convert to MB
        features[idx++] = task.getOutputDataSize() / (1024.0 * 1024.0); // Convert to MB
        features[idx++] = task.getDeadline() / 10.0;              // Normalize seconds
        features[idx++] = task.getPriority() / 10.0;              // Already 0-1 range
        
        // IoT device features
        features[idx++] = device.getLocation().getX() / 1000.0;   // Normalize to km
        features[idx++] = device.getLocation().getY() / 1000.0;   // Normalize to km
        features[idx++] = device.getBatteryPercentage() / 100.0;  // Already 0-1 range
        
        // UAV features for each UAV
        for (UAV uav : availableUAVs) {
            features[idx++] = uav.getLocation().getX() / 1000.0;  // Normalize to km
            features[idx++] = uav.getLocation().getY() / 1000.0;  // Normalize to km
            features[idx++] = uav.getEnergyPercentage() / 100.0;  // Already 0-1 range
        }
        
        return Nd4j.create(new double[][]{features});
    }
    
    /**
     * Record experience for training
     * @param task Task that was offloaded
     * @param device Source IoT device
     * @param availableUAVs Available UAVs at the time of decision
     * @param decision The offloading decision that was made
     * @param reward The reward received (based on latency, energy, etc.)
     */
    public void recordExperience(Task task, IoTDevice device, List<UAV> availableUAVs, 
                                 OffloadingDecision decision, double reward) {
        // Create feature vector for the state
        INDArray features = createFeatureVector(task, device, availableUAVs);
        
        // Determine action index
        int actionIndex;
        if (decision.getTarget() == OffloadingTarget.LOCAL) {
            actionIndex = numUAVs; // Local execution is the last index
        } else {
            // Find the index of the selected UAV
            UAV selectedUAV = decision.getSelectedUAV();
            actionIndex = availableUAVs.indexOf(selectedUAV);
            if (actionIndex == -1) {
                logger.warn("Selected UAV not found in available UAVs list. Using index 0.");
                actionIndex = 0;
            }
        }
        
        // Create and store experience
        OffloadingExperience experience = new OffloadingExperience(features, actionIndex, reward);
        
        // Add to buffer, removing oldest if full
        if (experienceBuffer.size() >= experienceBufferSize) {
            experienceBuffer.remove(0);
        }
        experienceBuffer.add(experience);
    }
    
    /**
     * Train the model using accumulated experience
     * @param batchSize Size of training batches
     * @param epochs Number of training epochs
     */
    public void trainModel(int batchSize, int epochs) {
        if (experienceBuffer.size() < batchSize) {
            logger.info("Not enough experience for training. Need {} but have {}", 
                    batchSize, experienceBuffer.size());
            return;
        }
        
        logger.info("Training model with {} experiences over {} epochs", experienceBuffer.size(), epochs);
        
        // Prepare training data
        int trainSize = (experienceBuffer.size() / batchSize) * batchSize; // Ensure divisible by batch size
        INDArray inputFeatures = Nd4j.create(new long[]{trainSize, this.inputFeatures});
        INDArray outputLabels = Nd4j.create(new long[]{trainSize, outputSize});
        
        // Fill training data
        for (int i = 0; i < trainSize; i++) {
            OffloadingExperience exp = experienceBuffer.get(i);
            inputFeatures.putRow(i, exp.getFeatures());
            
            // Create one-hot encoding of the action
            INDArray oneHot = Nd4j.zeros(outputSize);
            oneHot.putScalar(exp.getAction(), exp.getReward());
            outputLabels.putRow(i, oneHot);
        }
        
        // Train model
        for (int i = 0; i < epochs; i++) {
            model.fit(inputFeatures, outputLabels);
        }
        
        logger.info("Model training completed");
    }
    
    /**
     * Class representing a single experience for training
     */
    private static class OffloadingExperience {
        private final INDArray features;
        private final int action;
        private final double reward;
        
        public OffloadingExperience(INDArray features, int action, double reward) {
            this.features = features;
            this.action = action;
            this.reward = reward;
        }
        
        public INDArray getFeatures() {
            return features;
        }
        
        public int getAction() {
            return action;
        }
        
        public double getReward() {
            return reward;
        }
    }
}
