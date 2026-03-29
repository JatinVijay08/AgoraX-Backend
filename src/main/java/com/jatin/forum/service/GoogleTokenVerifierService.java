package com.jatin.forum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleTokenVerifierService {
    @Value("${google.client-id}")
    private String clientId;

    public Map<String,Object> verify(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> payload;

        try{
            payload = restTemplate.getForObject(url, Map.class);
        }catch(Exception e){
            throw new RuntimeException("Invalid Google Id token");
        }

        if(payload==null){
            throw new RuntimeException("Empty Response from google");
        }

        String audience = (String) payload.get("aud");
        if(!clientId.equals(audience)){
            throw new RuntimeException("Token was not issued for this application");
        }

        return payload;

    }

    }