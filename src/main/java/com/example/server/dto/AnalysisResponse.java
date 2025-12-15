package com.example.server.dto;

import java.io.Serializable;

public class AnalysisResponse<T> implements Serializable {

    private boolean success;      // true nếu xử lý thành công, false nếu có lỗi
    private T data;               // dữ liệu kết quả, null nếu lỗi
    private String errorMessage;  // mô tả lỗi nếu có



    public AnalysisResponse(T data) {
        this.success = true;
        this.data = data;
        this.errorMessage = null;
    }

    public AnalysisResponse(String errorMessage) {
        this.success = false;
        this.data = null;
        this.errorMessage = errorMessage;
    }

    public AnalysisResponse(T data, boolean success, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }


    public static <T> AnalysisResponse<T> success(T data) {
        return new AnalysisResponse<>(data);
    }

    public static <T> AnalysisResponse<T> error(String errorMessage) {
        return new AnalysisResponse<>(errorMessage);
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
