// src/main/java/com/eshop/frontend/dto/AuthenticationResponse.java
package com.eshop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // Make sure this import is present

public class AuthenticationResponse {

    // CHANGE THIS LINE: The JSON key is "token", not "jwtToken"
    @JsonProperty("token") // <--- THIS IS THE FIX!
    private String jwtToken; // Keep the field name as jwtToken for consistency within your frontend code if you wish

    // Default constructor (important for Jackson deserialization)
    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    // Optional: Keep the toString() for better debugging
    @Override
    public String toString() {
        return "AuthenticationResponse{" +
               "jwtToken='" + (jwtToken != null ? "exists, length=" + jwtToken.length() : "null") + '\'' +
               '}';
    }
}