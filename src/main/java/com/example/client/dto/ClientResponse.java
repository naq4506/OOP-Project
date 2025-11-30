package com.example.client.dto;

import java.io.Serializable;

public class ClientResponse<T> implements Serializable {

    private boolean success;      // true nếu xử lý thành công, false nếu có lỗi
    private T data;               // dữ liệu kết quả, null nếu lỗi
    private String errorMessage;  // mô tả lỗi nếu có



    /** Constructor cho kết quả thành công */
    public ClientResponse(T data) {
        this.success = true;
        this.data = data;
        this.errorMessage = null;
    }

    /** Constructor cho lỗi */
    public ClientResponse(String errorMessage) {
        this.success = false;
        this.data = null;
        this.errorMessage = errorMessage;
    }

    /** Constructor linh hoạt: vừa data vừa lỗi (partial result) */
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


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // ===========================
    // Helpers
    // ===========================

    public boolean hasError() {
        return !success;
    }

    @Override
    public String toString() {
        if (success) {
            return "AnalysisResponse{success=true, data=" + data + "}";
        } else {
            return "AnalysisResponse{success=false, errorMessage='" + errorMessage + "'}";
        }
    }
}
