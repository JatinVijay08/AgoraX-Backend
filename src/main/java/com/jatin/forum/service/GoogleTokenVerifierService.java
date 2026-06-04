package com.jatin.forum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class GoogleTokenVerifierService {
    @Value("${google.client-id}")
    private String clientId;

    public Map<String,Object> verify(String idToken) {
        log.info("[SERVICE] Contacting Google API to verify OAuth ID Token...");
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> payload;

        try{
            payload = restTemplate.getForObject(url, Map.class);
        }catch(Exception e){
            log.error("[SERVICE] Google Token verification failed with exception: {}", e.getMessage());
            throw new RuntimeException("Invalid Google Id token");
        }

        if(payload==null){
            log.warn("[SERVICE] Empty response body returned from Google API");
            throw new RuntimeException("Empty Response from google");
        }

        String audience = (String) payload.get("aud");
        log.info("[SERVICE] Google Token audience claim: {}, Client ID config: {}", audience, clientId);
        if(!clientId.equals(audience)){
            log.warn("[SERVICE] Google Token verification failed: aud claim mismatch");
            throw new RuntimeException("Token was not issued for this application");
        }

        log.info("[SERVICE] Google Token verification succeeded. Email={}", payload.get("email"));
        return payload;

    }

    }