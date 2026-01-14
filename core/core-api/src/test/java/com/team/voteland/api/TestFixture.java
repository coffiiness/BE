package com.team.voteland.api;

import java.util.UUID;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.api.v1.response.LoginResponse;
import com.team.voteland.domain.user.api.v1.response.UserResponse;

@Component
public class TestFixture {

    private final TestRestTemplate restTemplate;

    private String accessToken;

    public TestFixture(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TestRestTemplate client() {
        return restTemplate;
    }

    public String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    public String randomPassword() {
        return "password123";
    }

    public String randomName() {
        return "TestUser-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public UserResponse signUp() {
        return signUp(randomEmail(), randomPassword(), randomName());
    }

    public UserResponse signUp(String email, String password, String name) {
        SignUpRequest request = new SignUpRequest(email, password, name);
        return restTemplate.postForObject("/api/v1/users/signup", request, UserResponse.class);
    }

    public LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return restTemplate.postForObject("/api/v1/users/login", request, LoginResponse.class);
    }

    public void createUserThenSetAsDefault() {
        String email = randomEmail();
        String password = randomPassword();
        signUp(email, password, randomName());
        LoginResponse loginResponse = login(email, password);
        this.accessToken = loginResponse.accessToken();
    }

    public HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }

    public <T> HttpEntity<T> withAuth(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    public HttpEntity<Void> withAuth() {
        return new HttpEntity<>(authHeaders());
    }

    public void clearAuth() {
        this.accessToken = null;
    }

}