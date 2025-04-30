package com.albany.mvc.service;

import com.albany.mvc.dto.MechanicDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MechanicService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public List<MechanicDto> getAllMechanics(String token) {
        String url = apiBaseUrl + "/mechanics";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.debug("Fetching mechanics with token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Response body: {}", response.getBody());
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<MechanicDto>>() {}
                );
            }

        } catch (Exception e) {
            log.error("Error fetching mechanics: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    public MechanicDto getMechanicById(Integer id, String token) {
        String url = apiBaseUrl + "/mechanics/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<MechanicDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MechanicDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error fetching mechanic: {}", e.getMessage());
        }

        return null;
    }

    public MechanicDto createMechanic(MechanicDto mechanicDto, String token) {
        String url = apiBaseUrl + "/mechanics";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MechanicDto> entity = new HttpEntity<>(mechanicDto, headers);

        try {
            ResponseEntity<MechanicDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MechanicDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error creating mechanic: {}", e.getMessage());
        }

        return null;
    }

    public MechanicDto updateMechanic(Integer id, MechanicDto mechanicDto, String token) {
        String url = apiBaseUrl + "/mechanics/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MechanicDto> entity = new HttpEntity<>(mechanicDto, headers);

        try {
            ResponseEntity<MechanicDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    MechanicDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error updating mechanic: {}", e.getMessage());
        }

        return null;
    }

    public boolean deleteMechanic(Integer id, String token) {
        String url = apiBaseUrl + "/mechanics/" + id;

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
            log.error("Error deleting mechanic: {}", e.getMessage());
        }

        return false;
    }
    
    public Map<String, Long> getMechanicCounts(String token) {
        String url = apiBaseUrl + "/mechanics/counts";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Long>>() {}
                );
            }

        } catch (Exception e) {
            log.error("Error fetching mechanic counts: {}", e.getMessage());
        }

        return new HashMap<>();
    }
}