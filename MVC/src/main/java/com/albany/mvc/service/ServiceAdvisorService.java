package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceAdvisorDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public List<ServiceAdvisorDto> getAllServiceAdvisors(String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // Debug logging for token
            log.debug("Fetching service advisors with token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            log.debug("Authorization header: {}", headers.getFirst("Authorization"));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("Response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Response body: {}", response.getBody());
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ServiceAdvisorDto>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error fetching service advisors: {}", e.getMessage());

            // Try debug endpoint to get token info
            try {
                ResponseEntity<String> debugResponse = restTemplate.exchange(
                        apiBaseUrl + "/debug/token-info",
                        HttpMethod.GET,
                        entity,
                        String.class
                );
                log.debug("Token debug info: {}", debugResponse.getBody());
            } catch (Exception ex) {
                log.error("Error fetching token debug info: {}", ex.getMessage());
            }
        }

        return Collections.emptyList();
    }

    public ServiceAdvisorDto getServiceAdvisorById(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error fetching service advisor: {}", e.getMessage());
        }

        return null;
    }

    public ServiceAdvisorDto createServiceAdvisor(ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error creating service advisor: {}", e.getMessage());
        }

        return null;
    }

    public ServiceAdvisorDto updateServiceAdvisor(Integer id, ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error updating service advisor: {}", e.getMessage());
        }

        return null;
    }

    public boolean deleteServiceAdvisor(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode() == HttpStatus.NO_CONTENT;

        } catch (Exception e) {
            log.error("Error deleting service advisor: {}", e.getMessage());
        }

        return false;
    }
}