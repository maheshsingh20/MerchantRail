package dev.merchantrail.transaction.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error response DTO.
 */
public class ErrorResponse {
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("message")
    private String message;
    
    public ErrorResponse() {}
    
    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
