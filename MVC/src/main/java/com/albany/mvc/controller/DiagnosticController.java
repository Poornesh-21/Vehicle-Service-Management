package com.albany.mvc.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @GetMapping("/test-auth")
    @ResponseBody
    public String testAuth() {
        String url = apiBaseUrl + "/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "info.albanyservice@gmail.com");
        requestBody.put("password", "admin@albany");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Sending test request to: {}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            log.info("Response status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());

            StringBuilder result = new StringBuilder();
            result.append("<h3>API Connection Test</h3>");
            result.append("<p>Status: ").append(response.getStatusCode()).append("</p>");
            result.append("<p>Response Body:</p>");
            result.append("<pre>").append(response.getBody()).append("</pre>");

            // Extract token and role
            String body = response.getBody();
            if (body != null) {
                try {
                    JsonNode rootNode = objectMapper.readTree(body);
                    String token = rootNode.has("token") ? rootNode.get("token").asText() : "Not Found";
                    String role = rootNode.has("role") ? rootNode.get("role").asText() : "Not Found";

                    result.append("<p><strong>Extracted Token:</strong> ")
                            .append(token.substring(0, Math.min(20, token.length())))
                            .append("...</p>");
                    result.append("<p><strong>Extracted Role:</strong> ").append(role).append("</p>");
                } catch (Exception e) {
                    result.append("<p><strong>Error parsing JSON:</strong> ").append(e.getMessage()).append("</p>");
                }
            }

            return result.toString();
        } catch (Exception e) {
            log.error("Error calling API: ", e);
            return "Error: " + e.getMessage() + "<br><pre>" + e.toString() + "</pre>";
        }
    }
}