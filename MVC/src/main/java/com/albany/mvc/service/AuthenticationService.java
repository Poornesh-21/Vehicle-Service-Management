package com.albany.mvc.service;

import com.albany.mvc.dto.AuthRequest;
import com.albany.mvc.dto.AuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public AuthResponse authenticate(AuthRequest request) {
        String url = apiBaseUrl + "/auth/login";
        log.info("Attempting to authenticate user: {} with API endpoint: {}", request.getEmail(), url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.debug("Request details: {}", entity);
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    url,
                    entity,
                    String.class
            );

            log.info("Raw response status: {}", rawResponse.getStatusCode());
            log.info("Raw response body: {}", rawResponse.getBody());

            // Process the response body using Jackson instead of manual string parsing
            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null) {
                try {
                    // Parse the JSON properly
                    JsonNode rootNode = objectMapper.readTree(rawResponse.getBody());

                    AuthResponse authResponse = new AuthResponse();

                    // Extract the token
                    if (rootNode.has("token")) {
                        authResponse.setToken(rootNode.get("token").asText());
                    }

                    // Extract userId if present
                    if (rootNode.has("userId")) {
                        authResponse.setUserId(rootNode.get("userId").asInt());
                    }

                    // Extract email if present
                    if (rootNode.has("email")) {
                        authResponse.setEmail(rootNode.get("email").asText());
                    }

                    // Extract firstName if present
                    if (rootNode.has("firstName")) {
                        authResponse.setFirstName(rootNode.get("firstName").asText());
                    }

                    // Extract lastName if present
                    if (rootNode.has("lastName")) {
                        authResponse.setLastName(rootNode.get("lastName").asText());
                    }

                    // Extract role - this is what we need most
                    if (rootNode.has("role")) {
                        if (rootNode.get("role").isTextual()) {
                            // If role is a string
                            authResponse.setRole(rootNode.get("role").asText());
                        } else {
                            // If role is something else, convert to string
                            authResponse.setRole(rootNode.get("role").toString());
                        }
                    }

                    log.info("Successfully parsed auth response: token={}, role={}",
                            authResponse.getToken() != null ? "present" : "missing",
                            authResponse.getRole());

                    return authResponse;
                } catch (Exception e) {
                    log.error("Error parsing JSON response: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to parse authentication response: " + e.getMessage());
                }
            } else {
                log.error("API returned success but with null or invalid body");
                throw new RuntimeException("Invalid response from authentication server");
            }
        } catch (HttpClientErrorException e) {
            log.error("API returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("Error communicating with API: {}", e.getMessage(), e);
            throw new RuntimeException("Error connecting to authentication server: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            throw new RuntimeException("Authentication failed due to an unexpected error: " + e.getMessage());
        }
    }
}