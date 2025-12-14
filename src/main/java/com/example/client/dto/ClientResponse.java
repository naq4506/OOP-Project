package com.example.client.dto;

import java.io.Serializable;

public class ClientResponse<T> implements Serializable {

    private boolean success;
    private T data;
    private String errorMessage;

    // --- QUAN TRỌNG: Cần thêm Constructor rỗng này để Jackson hoạt động ---
    public ClientResponse() {
    }
    // ----------------------------------------------------------------------

    public ClientResponse(T data) {
        this.success = true;
        this.data = data;
        this.errorMessage = null;
    }

    public ClientResponse(String errorMessage) {
        this.success = false;
        this.data = null;
        this.errorMessage = errorMessage;
    }

    public ClientResponse(T data, boolean success, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> ClientResponse<T> success(T data) {
        return new ClientResponse<>(data);
    }

    public static <T> ClientResponse<T> error(String errorMessage) {
        return new ClientResponse<>(errorMessage);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}