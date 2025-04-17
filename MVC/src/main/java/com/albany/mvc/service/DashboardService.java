package com.albany.mvc.service;

import com.albany.mvc.dto.DashboardDTO;
import com.albany.mvc.dto.ServiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public DashboardDTO getDashboardData(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<DashboardDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/dashboard",
                    HttpMethod.GET,
                    entity,
                    DashboardDTO.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching dashboard data: {}", e.getMessage(), e);
            return null;
        }
    }

    public ServiceRequestDto assignServiceAdvisor(Integer requestId, Integer advisorId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Integer> requestBody = Collections.singletonMap("advisorId", advisorId);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/dashboard/assign/" + requestId,
                    HttpMethod.PUT,
                    entity,
                    ServiceRequestDto.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            return null;
        }
    }

    public ServiceRequestDto updateServiceRequestStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/dashboard/status/" + requestId,
                    HttpMethod.PUT,
                    entity,
                    ServiceRequestDto.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error updating service request status: {}", e.getMessage(), e);
            return null;
        }
    }
}