package com.wifimanager.shared.api;

/**
 * Réponse API simple sans annotations Jackson
 */
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    
    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public ApiResponse(boolean success, String message) {
        this(success, message, null);
    }
    
    public static ApiResponse success(Object data) {
        return new ApiResponse(true, "Operation successful", data);
    }
    
    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(true, message, data);
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    
    // Méthode pour convertir en JSON simple
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":").append(success).append(",");
        sb.append("\"message\":\"").append(message).append("\"");
        if (data != null) {
            sb.append(",\"data\":");
            if (data instanceof String) {
                sb.append("\"").append(data).append("\"");
            } else {
                sb.append(data.toString());
            }
        }
        sb.append("}");
        return sb.toString();
    }
}