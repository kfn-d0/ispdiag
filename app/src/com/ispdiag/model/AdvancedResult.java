package com.ispdiag.model;

/**
 * Resultados avancados: MTU, bloqueio de SNI, deteccao de proxy.
 */
public class AdvancedResult {
    // Deteccao de MTU
    private int detectedMtu;
    private boolean mtuFragmentation;
    private String mtuDetails;

    // Bloqueio de SNI
    private boolean sniBlocked;
    private String sniDetails;
    private long sniTestTimeMs;

    // Deteccao de Proxy/Interceptador
    private boolean proxyDetected;
    private String proxyType;
    private String proxyDetails;
    private String expectedCertIssuer;
    private String actualCertIssuer;
    private boolean certMismatch;

    public AdvancedResult() {
    }

    // MTU
    public int getDetectedMtu() {
        return detectedMtu;
    }

    public void setDetectedMtu(int detectedMtu) {
        this.detectedMtu = detectedMtu;
    }

    public boolean isMtuFragmentation() {
        return mtuFragmentation;
    }

    public void setMtuFragmentation(boolean mtuFragmentation) {
        this.mtuFragmentation = mtuFragmentation;
    }

    public String getMtuDetails() {
        return mtuDetails;
    }

    public void setMtuDetails(String mtuDetails) {
        this.mtuDetails = mtuDetails;
    }

    // SNI
    public boolean isSniBlocked() {
        return sniBlocked;
    }

    public void setSniBlocked(boolean sniBlocked) {
        this.sniBlocked = sniBlocked;
    }

    public String getSniDetails() {
        return sniDetails;
    }

    public void setSniDetails(String sniDetails) {
        this.sniDetails = sniDetails;
    }

    public long getSniTestTimeMs() {
        return sniTestTimeMs;
    }

    public void setSniTestTimeMs(long sniTestTimeMs) {
        this.sniTestTimeMs = sniTestTimeMs;
    }

    // Proxy
    public boolean isProxyDetected() {
        return proxyDetected;
    }

    public void setProxyDetected(boolean proxyDetected) {
        this.proxyDetected = proxyDetected;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getProxyDetails() {
        return proxyDetails;
    }

    public void setProxyDetails(String proxyDetails) {
        this.proxyDetails = proxyDetails;
    }

    public String getExpectedCertIssuer() {
        return expectedCertIssuer;
    }

    public void setExpectedCertIssuer(String expectedCertIssuer) {
        this.expectedCertIssuer = expectedCertIssuer;
    }

    public String getActualCertIssuer() {
        return actualCertIssuer;
    }

    public void setActualCertIssuer(String actualCertIssuer) {
        this.actualCertIssuer = actualCertIssuer;
    }

    public boolean isCertMismatch() {
        return certMismatch;
    }

    public void setCertMismatch(boolean certMismatch) {
        this.certMismatch = certMismatch;
    }
}
