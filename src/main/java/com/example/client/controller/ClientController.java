package com.example.client.controller;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class ClientController {

    private final String serverUrl;
    private final ObjectMapper mapper;

    public ClientController(String serverUrl) {
        this.serverUrl = serverUrl;
        this.mapper = new ObjectMapper();
    }

    /**
     * Send analysis request to server
     */
    public ClientResponse<Map<String, Object>> sendAnalysis(ClientRequest request) {

        // 1️⃣ Validate input on client side
        ClientResponse<Map<String, Object>> validationError = validateRequest(request);
        if (validationError != null) return validationError;

        try {
            URL url = new URL(serverUrl + "/api/disaster/analyze/all");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000); // 10 giây để kết nối
            connection.setReadTimeout(600000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // 2️⃣ Write request JSON
            try (OutputStream os = connection.getOutputStream()) {
                mapper.writeValue(os, request);
            }

            // 3️⃣ Read response
            int status = connection.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ?
                    connection.getInputStream() : connection.getErrorStream();

            TypeReference<ClientResponse<Map<String, Object>>> typeRef =
                    new TypeReference<>() {};

            ClientResponse<Map<String, Object>> response = mapper.readValue(is, typeRef);

            // 4️⃣ Return server response
            return response;

        } catch (DateTimeParseException e) {
            return ClientResponse.error("Invalid date format. Use YYYY-MM-DD");

        } catch (Exception e) {
            return ClientResponse.error("Connection or server error: " + e.getMessage());
        }
    }

    // ===============================
    // CLIENT-SIDE VALIDATION
    // ===============================
    private ClientResponse<Map<String, Object>> validateRequest(ClientRequest request) {
        if (request.getDisasterName() == null || request.getDisasterName().isEmpty()) {
            return ClientResponse.error("Disaster name must not be empty");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            return ClientResponse.error("Start date and end date must not be empty");
        }

        try {
            LocalDate start = LocalDate.parse(request.getStartDate());
            LocalDate end = LocalDate.parse(request.getEndDate());
            if (start.isAfter(end)) {
                return ClientResponse.error("Start date cannot be after end date");
            }
        } catch (DateTimeParseException e) {
            return ClientResponse.error("Invalid date format. Use YYYY-MM-DD");
        }

        if (request.getPlatforms() == null || request.getPlatforms().isEmpty()) {
            return ClientResponse.error("At least one platform must be provided");
        }

        if (request.getAnalysisType() == null || request.getAnalysisType().isEmpty()) {
            return ClientResponse.error("At least one analysis type must be provided");
        }

        return null; // validation passed
    }
}

