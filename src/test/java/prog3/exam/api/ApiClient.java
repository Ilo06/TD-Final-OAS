package prog3.exam.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080";
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = BASE_URL;
        this.restTemplate = new RestTemplate();
    }

    // =========================
    // 🔹 GET
    // =========================

    public <T> T get(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> T get(String path, ParameterizedTypeReference<T> typeRef) {
        return exchange(path, HttpMethod.GET, null, typeRef);
    }

    // =========================
    // 🔹 POST
    // =========================

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(path, HttpMethod.POST, body, responseType);
    }

    public <T> T post(String path, Object body, ParameterizedTypeReference<T> typeRef) {
        return exchange(path, HttpMethod.POST, body, typeRef);
    }

    // =========================
    // 🔹 PUT
    // =========================

    public <T> T put(String path, Object body, Class<T> responseType) {
        return exchange(path, HttpMethod.PUT, body, responseType);
    }

    public <T> T put(String path, Object body, ParameterizedTypeReference<T> typeRef) {
        return exchange(path, HttpMethod.PUT, body, typeRef);
    }

    // =========================
    // 🔹 DELETE
    // =========================

    public void delete(String path) {
        exchange(path, HttpMethod.DELETE, null, Void.class);
    }

    public <T> T delete(String path, ParameterizedTypeReference<T> typeRef) {
        return exchange(path, HttpMethod.DELETE, null, typeRef);
    }

    // =========================
    // 🔁 CORE METHODS
    // =========================

    private <T> T exchange(String path,
                           HttpMethod method,
                           Object body,
                           Class<T> responseType) {

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    baseUrl + path,
                    method,
                    buildEntity(body),
                    responseType
            );
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            throw buildException(e);
        }
    }

    private <T> T exchange(String path,
                           HttpMethod method,
                           Object body,
                           ParameterizedTypeReference<T> typeRef) {

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    baseUrl + path,
                    method,
                    buildEntity(body),
                    typeRef
            );
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            throw buildException(e);
        }
    }

    // =========================
    // 🔧 HELPERS
    // =========================

    private HttpEntity<?> buildEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private RuntimeException buildException(HttpStatusCodeException e) {
        return new RuntimeException(
                "HTTP Error: " + e.getStatusCode() +
                        " | Body: " + e.getResponseBodyAsString(),
                e
        );
    }
}