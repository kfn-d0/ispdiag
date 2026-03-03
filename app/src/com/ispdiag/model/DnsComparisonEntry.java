package com.ispdiag.model;

/**
 * Entrada de comparacao DNS para um unico resolvedor.
 */
public class DnsComparisonEntry {
    private String resolverName;
    private String resolverIp;
    private boolean success;
    private long responseTimeMs;
    private String resolvedIp;
    private String errorMessage;

    public DnsComparisonEntry(String resolverName, String resolverIp) {
        this.resolverName = resolverName;
        this.resolverIp = resolverIp;
    }

    public String getResolverName() {
        return resolverName;
    }

    public String getResolverIp() {
        return resolverIp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getResolvedIp() {
        return resolvedIp;
    }

    public void setResolvedIp(String resolvedIp) {
        this.resolvedIp = resolvedIp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
