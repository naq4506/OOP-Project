package com.example.client.controller;

import java.util.Arrays;
import java.util.Map;

import com.example.client.dto.ClientRequest;
import com.example.client.dto.ClientResponse;

public class ClientTest {
    public static void main(String[] args) {
        ClientController client = new ClientController("http://localhost:8080");

        ClientRequest request = new ClientRequest();
        request.setDisasterName("Flood");
        request.setStartDate("2025-11-01");
        request.setEndDate("2025-11-15");
        request.setKeyword("river");
        request.setPlatforms(Arrays.asList("facebook"));
        request.setAnalysisType("DAMAGE");

        ClientResponse<Map<String, Object>> response = client.sendAnalysis(request);

        System.out.println("Success? " + response.isSuccess());
        System.out.println("Data: " + response.getData());
        System.out.println("Error: " + response.getErrorMessage());
    }
}
