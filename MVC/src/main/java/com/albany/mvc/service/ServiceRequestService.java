package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final RestTemplate restTemplate;
    
    @Value("${api.base-url}")
    private String apiBaseUrl;
    
    public List<ServiceRequestDto> getAllServiceRequests(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ServiceRequestDto>> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests", 
                    HttpMethod.GET, 
                    entity, 
                    new ParameterizedTypeReference<List<ServiceRequestDto>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    public List<ServiceRequestDto> getServiceRequestsByStatus(String status, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ServiceRequestDto>> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/status/" + status, 
                    HttpMethod.GET, 
                    entity, 
                    new ParameterizedTypeReference<List<ServiceRequestDto>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching service requests by status: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    public ServiceRequestDto getServiceRequestById(Integer id, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + id, 
                    HttpMethod.GET, 
                    entity, 
                    ServiceRequestDto.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching service request: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public ServiceRequestDto createServiceRequest(ServiceRequestDto requestDto, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<ServiceRequestDto> entity = new HttpEntity<>(requestDto, headers);
            
            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests", 
                    HttpMethod.POST, 
                    entity, 
                    ServiceRequestDto.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
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
                    apiBaseUrl + "/service-requests/" + requestId + "/assign", 
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
                    apiBaseUrl + "/service-requests/" + requestId + "/status", 
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