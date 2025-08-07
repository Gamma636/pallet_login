package com.pallet.pallet_login.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String API_KEY = "AIzaSyBfTKQXCLRqBB3xfH28_3yaZyPHTfqCdSg"; // Replace if needed
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public boolean compareFaces(String storedBase64Image, String loginBase64Image) throws Exception {
        String prompt = "Are these two images of the same person? Reply only with 'Yes' or 'No'.";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> image1 = Map.of("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", storedBase64Image
        ));
        Map<String, Object> image2 = Map.of("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", loginBase64Image
        ));

        Map<String, Object> content = Map.of("parts", List.of(textPart, image1, image2), "role", "user");
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Debug logs (optional)
        System.out.println("🔁 Gemini Response: " + response.body());

        Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");

        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) contentMap.get("parts");

            String answer = parts.get(0).get("text").toLowerCase().trim();
            return answer.contains("yes");
        } else {
            return false;
        }
    }

}
