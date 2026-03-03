package com.ispdiag.model;

import java.util.List;

/**
 * Resultado do teste de requisicao HTTP HEAD.
 * Aprimorado com analise de TTFB (Tempo ate o Primeiro Byte)
 * e media de multiplas iteracoes.
 */
public class HttpResult {
    private boolean success;
    private int statusCode;
    private long totalTimeMs;
    private List<String> redirectChain;
    private String errorType; // TIMEOUT, CONNECTION_REFUSED, ERRO
    private String errorMessage;

    // Campos de analise de TTFB
    private long ttfbMs; // Tempo ate o primeiro byte (processamento do servidor)
    private long connectTimeMs; // Tempo de conexao TCP+TLS para esta requisicao
    private int iterations; // Numero de iteracoes do teste
    private long minTtfbMs; // TTFB minimo entre iteracoes
    private long maxTtfbMs; // TTFB maximo entre iteracoes
    private long avgTtfbMs; // TTFB medio entre iteracoes
    private String ttfbAnalysis; // Analise legivel por humanos

    public HttpResult() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public List<String> getRedirectChain() {
        return redirectChain;
    }

    public void setRedirectChain(List<String> redirectChain) {
        this.redirectChain = redirectChain;
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

    public long getTtfbMs() {
        return ttfbMs;
    }

    public void setTtfbMs(long ttfbMs) {
        this.ttfbMs = ttfbMs;
    }

    public long getConnectTimeMs() {
        return connectTimeMs;
    }

    public void setConnectTimeMs(long connectTimeMs) {
        this.connectTimeMs = connectTimeMs;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public long getMinTtfbMs() {
        return minTtfbMs;
    }

    public void setMinTtfbMs(long minTtfbMs) {
        this.minTtfbMs = minTtfbMs;
    }

    public long getMaxTtfbMs() {
        return maxTtfbMs;
    }

    public void setMaxTtfbMs(long maxTtfbMs) {
        this.maxTtfbMs = maxTtfbMs;
    }

    public long getAvgTtfbMs() {
        return avgTtfbMs;
    }

    public void setAvgTtfbMs(long avgTtfbMs) {
        this.avgTtfbMs = avgTtfbMs;
    }

    public String getTtfbAnalysis() {
        return ttfbAnalysis;
    }

    public void setTtfbAnalysis(String ttfbAnalysis) {
        this.ttfbAnalysis = ttfbAnalysis;
    }
}
