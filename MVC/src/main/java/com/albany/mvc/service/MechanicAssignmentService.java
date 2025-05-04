package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MechanicAssignmentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get new service requests that need assignment
     */
    public List<Map<String, Object>> getNewServiceRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/new",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching new service requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get assigned service requests (in progress)
     */
    public List<Map<String, Object>> getAssignedRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/assigned",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching assigned requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all mechanics (for assignment dropdown)
     */
    public List<Map<String, Object>> getAllMechanics(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/mechanics",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching mechanics: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Assign mechanics to a service request
     */
    public Map<String, Object> assignMechanicsToService(Integer serviceRequestId, Map<String, Object> assignmentData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create assignment DTO from front-end data
            Map<String, Object> assignmentDTO = new HashMap<>();
            assignmentDTO.put("serviceRequestId", serviceRequestId);

            // Add primary mechanic ID
            if (assignmentData.containsKey("primaryMechanic")) {
                assignmentDTO.put("primaryMechanicId", assignmentData.get("primaryMechanic"));
            }

            // Add additional mechanic IDs
            if (assignmentData.containsKey("additionalMechanics")) {
                assignmentDTO.put("additionalMechanicIds", assignmentData.get("additionalMechanics"));
            }

            // Add estimated completion date
            if (assignmentData.containsKey("estimatedCompletionDate")) {
                String dateStr = (String) assignmentData.get("estimatedCompletionDate");
                assignmentDTO.put("estimatedCompletionDate", dateStr);
            }

            // Add priority
            if (assignmentData.containsKey("priority")) {
                assignmentDTO.put("priority", assignmentData.get("priority"));
            }

            // Add service notes
            if (assignmentData.containsKey("serviceNotes")) {
                assignmentDTO.put("serviceNotes", assignmentData.get("serviceNotes"));
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(assignmentDTO, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-assignments/assign",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error assigning mechanics to service: {}", e.getMessage(), e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Helper method to create authentication headers
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                headers.set("Authorization", token);
            } else {
                headers.setBearerAuth(token);
            }
        }

        return headers;
    }
}