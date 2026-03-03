package com.ispdiag.model;

/**
 * Resultado do teste de conexao TCP.
 */
public class TcpResult {
    private int port;
    private boolean success;
    private long connectTimeMs;
    private String errorType; // TIMEOUT, RST, UNREACHABLE, ERRO
    private String errorMessage;

    public TcpResult() {
    }

    public TcpResult(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getConnectTimeMs() {
        return connectTimeMs;
    }

    public void setConnectTimeMs(long connectTimeMs) {
        this.connectTimeMs = connectTimeMs;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
