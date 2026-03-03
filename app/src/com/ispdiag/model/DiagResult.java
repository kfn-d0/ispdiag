package com.ispdiag.model;

import java.util.List;

/**
 * Resultado completo do diagnostico para um unico alvo de servico.
 */
public class DiagResult {
    public enum OverallStatus {
        OK, PARTIAL, FAIL
    }

    private ServiceTarget target;
    private OverallStatus overallStatus;
    private DnsResult dnsResult;
    private TcpResult tcpPort80;
    private TcpResult tcpPort443;
    private TlsResult tlsResult;
    private HttpResult httpResult;
    private TracerouteResult tracerouteResult;
    private List<DnsComparisonEntry> dnsComparison;
    private AdvancedResult advancedResult;
    private long totalTimeMs;
    private String timestamp;

    public DiagResult() {
    }

    public DiagResult(ServiceTarget target) {
        this.target = target;
    }

    public void computeOverallStatus() {
        boolean allOk = true;
        boolean anyOk = false;

        if (dnsResult != null) {
            if (dnsResult.isSuccess())
                anyOk = true;
            else
                allOk = false;
        }
        if (tcpPort443 != null) {
            if (tcpPort443.isSuccess())
                anyOk = true;
            else
                allOk = false;
        }
        if (tlsResult != null) {
            if (tlsResult.isSuccess())
                anyOk = true;
            else
                allOk = false;
        }
        if (httpResult != null) {
            if (httpResult.isSuccess())
                anyOk = true;
            else
                allOk = false;
        }

        if (allOk && anyOk) {
            overallStatus = OverallStatus.OK;
        } else if (anyOk) {
            overallStatus = OverallStatus.PARTIAL;
        } else {
            overallStatus = OverallStatus.FAIL;
        }
    }

    public ServiceTarget getTarget() {
        return target;
    }

    public void setTarget(ServiceTarget target) {
        this.target = target;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public DnsResult getDnsResult() {
        return dnsResult;
    }

    public void setDnsResult(DnsResult dnsResult) {
        this.dnsResult = dnsResult;
    }

    public TcpResult getTcpPort80() {
        return tcpPort80;
    }

    public void setTcpPort80(TcpResult tcpPort80) {
        this.tcpPort80 = tcpPort80;
    }

    public TcpResult getTcpPort443() {
        return tcpPort443;
    }

    public void setTcpPort443(TcpResult tcpPort443) {
        this.tcpPort443 = tcpPort443;
    }

    public TlsResult getTlsResult() {
        return tlsResult;
    }

    public void setTlsResult(TlsResult tlsResult) {
        this.tlsResult = tlsResult;
    }

    public HttpResult getHttpResult() {
        return httpResult;
    }

    public void setHttpResult(HttpResult httpResult) {
        this.httpResult = httpResult;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public TracerouteResult getTracerouteResult() {
        return tracerouteResult;
    }

    public void setTracerouteResult(TracerouteResult tr) {
        this.tracerouteResult = tr;
    }

    public List<DnsComparisonEntry> getDnsComparison() {
        return dnsComparison;
    }

    public void setDnsComparison(List<DnsComparisonEntry> dc) {
        this.dnsComparison = dc;
    }

    public AdvancedResult getAdvancedResult() {
        return advancedResult;
    }

    public void setAdvancedResult(AdvancedResult ar) {
        this.advancedResult = ar;
    }
}
