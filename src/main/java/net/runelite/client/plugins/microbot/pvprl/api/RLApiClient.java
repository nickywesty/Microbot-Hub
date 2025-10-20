package net.runelite.client.plugins.microbot.pvprl.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with the Python serve-api server
 * Handles socket connection, request/response, and error handling
 */
@Slf4j
public class RLApiClient {
    private final String host;
    private final int port;
    private final int connectionTimeout;
    private final int requestTimeout;
    private final Gson gson;
    private final ExecutorService executor;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private boolean connected = false;
    private long lastRequestTime = 0;
    private int successfulRequests = 0;
    private int failedRequests = 0;

    public RLApiClient(String host, int port, int connectionTimeout, int requestTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.requestTimeout = requestTimeout;
        this.gson = new GsonBuilder().create();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Connect to the Python API server
     */
    public boolean connect() {
        if (connected) {
            log.warn("Already connected to API server");
            return true;
        }

        try {
            log.info("Connecting to PVP RL API at {}:{}...", host, port);
            socket = new Socket(host, port);
            socket.setSoTimeout(requestTimeout);

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            log.info("Successfully connected to PVP RL API");
            return true;

        } catch (IOException e) {
            log.error("Failed to connect to API server: {}", e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * Disconnect from the API server
     */
    public void disconnect() {
        if (!connected) {
            return;
        }

        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();

            connected = false;
            log.info("Disconnected from PVP RL API");

        } catch (IOException e) {
            log.error("Error during disconnect: {}", e.getMessage());
        }
    }

    /**
     * Send a prediction request and get action response
     *
     * @param modelName       Name of the model to use
     * @param observation     Observation vector (frame-stacked if needed)
     * @param actionMasks     Action masks for each action head
     * @param deterministic   Use deterministic sampling
     * @return Action vector from AI, or null on error
     */
    public int[] predict(String modelName, float[][] observation, boolean[][] actionMasks, boolean deterministic) {
        if (!connected) {
            log.error("Not connected to API server");
            return null;
        }

        try {
            // Build request
            ApiRequest request = new ApiRequest();
            request.model = modelName;
            request.obs = observation;
            request.actionMasks = actionMasks;
            request.deterministic = deterministic;
            request.returnLogProb = false;
            request.returnEntropy = false;
            request.returnValue = false;
            request.returnProbs = false;
            request.extensions = List.of();

            // Send request
            String requestJson = gson.toJson(request) + "\n";
            long startTime = System.currentTimeMillis();

            writer.write(requestJson);
            writer.flush();

            // Receive response
            String responseLine = reader.readLine();
            if (responseLine == null) {
                log.error("Received null response from API");
                failedRequests++;
                return null;
            }

            ApiResponse response = gson.fromJson(responseLine, ApiResponse.class);

            long elapsedMs = System.currentTimeMillis() - startTime;
            lastRequestTime = elapsedMs;
            successfulRequests++;

            log.debug("API prediction completed in {}ms", elapsedMs);

            return response.action;

        } catch (SocketTimeoutException e) {
            log.error("API request timed out after {}ms", requestTimeout);
            failedRequests++;
            return null;

        } catch (IOException e) {
            log.error("IO error during API request: {}", e.getMessage());
            connected = false;
            failedRequests++;
            return null;

        } catch (Exception e) {
            log.error("Unexpected error during API request", e);
            failedRequests++;
            return null;
        }
    }

    /**
     * Send prediction request asynchronously
     */
    public CompletableFuture<int[]> predictAsync(String modelName, float[][] observation,
                                                  boolean[][] actionMasks, boolean deterministic) {
        return CompletableFuture.supplyAsync(
            () -> predict(modelName, observation, actionMasks, deterministic),
            executor
        );
    }

    /**
     * Check if client is connected
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Get last request latency in milliseconds
     */
    public long getLastRequestTime() {
        return lastRequestTime;
    }

    /**
     * Get total successful requests
     */
    public int getSuccessfulRequests() {
        return successfulRequests;
    }

    /**
     * Get total failed requests
     */
    public int getFailedRequests() {
        return failedRequests;
    }

    /**
     * Shutdown the client and executor
     */
    public void shutdown() {
        disconnect();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // Inner classes for JSON serialization
    private static class ApiRequest {
        String model;
        float[][] obs; // Frame-stacked observations
        boolean[][] actionMasks;
        boolean deterministic;
        boolean returnLogProb;
        boolean returnEntropy;
        boolean returnValue;
        boolean returnProbs;
        List<String> extensions;
    }

    private static class ApiResponse {
        int[] action;
        Double logProb;
        double[] entropy;
        double[] values;
        double[][] probs;
        Object[] extensionResults;
    }
}
