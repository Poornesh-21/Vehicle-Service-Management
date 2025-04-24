package com.albany.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/admin/api/vehicle-tracking/bill")
@RequiredArgsConstructor
@Slf4j
public class BillDownloadController {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;


    @GetMapping("/download/{serviceId}")
    public void downloadBill(
            @PathVariable Integer serviceId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            } catch (IOException e) {
                log.error("Error sending unauthorized response", e);
            }
            return;
        }

        log.info("Processing bill download request for service ID: {}", serviceId);

        try {
            // Setup headers for the API request
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(validToken);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Call the REST API with byte[] response type to get the PDF data
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + serviceId + "/download",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                byte[] pdfBytes = apiResponse.getBody();
                
                // Set response headers
                response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                
                // Get content disposition header from API response if available
                String contentDisposition = null;
                HttpHeaders apiHeaders = apiResponse.getHeaders();
                if (apiHeaders != null && apiHeaders.getContentDisposition() != null) {
                    contentDisposition = apiHeaders.getContentDisposition().toString();
                }
                
                // If content disposition not provided by API, create a default one
                if (contentDisposition == null) {
                    contentDisposition = "attachment; filename=bill_" + serviceId + ".pdf";
                }
                
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
                response.setContentLength(pdfBytes.length);
                
                // Write PDF data to response
                try (OutputStream out = response.getOutputStream()) {
                    out.write(pdfBytes);
                    out.flush();
                    log.info("Bill PDF successfully sent to client for service ID: {}", serviceId);
                }
            } else {
                log.error("API returned non-success status: {}", apiResponse.getStatusCode());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get bill from API");
            }
        } catch (Exception e) {
            log.error("Error downloading bill: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download bill: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Error sending error response", ex);
            }
        }
    }

    /**
     * Helper method to get a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                log.debug("Using token from session");
                return sessionToken;
            }
        }

        log.warn("No valid token found from any source");
        return null;
    }
}