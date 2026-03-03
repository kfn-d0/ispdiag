package com.ispdiag.model;

/**
 * Resultado do teste de handshake TLS.
 */
public class TlsResult {
    private boolean success;
    private long handshakeTimeMs;
    private String tlsVersion;
    private String cipherSuite;
    private String certSubject;
    private String certIssuer;
    private String certValidFrom;
    private String certValidTo;
    private boolean certValid;
    private String sniUsed;
    private String errorType; // HANDSHAKE_FAILED, CERT_ERROR, TIMEOUT, ERRO
    private String errorMessage;

    public TlsResult() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getHandshakeTimeMs() {
        return handshakeTimeMs;
    }

    public void setHandshakeTimeMs(long handshakeTimeMs) {
        this.handshakeTimeMs = handshakeTimeMs;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

    public void setCipherSuite(String cipherSuite) {
        this.cipherSuite = cipherSuite;
    }

    public String getCertSubject() {
        return certSubject;
    }

    public void setCertSubject(String certSubject) {
        this.certSubject = certSubject;
    }

    public String getCertIssuer() {
        return certIssuer;
    }

    public void setCertIssuer(String certIssuer) {
        this.certIssuer = certIssuer;
    }

    public String getCertValidFrom() {
        return certValidFrom;
    }

    public void setCertValidFrom(String certValidFrom) {
        this.certValidFrom = certValidFrom;
    }

    public String getCertValidTo() {
        return certValidTo;
    }

    public void setCertValidTo(String certValidTo) {
        this.certValidTo = certValidTo;
    }

    public boolean isCertValid() {
        return certValid;
    }

    public void setCertValid(boolean certValid) {
        this.certValid = certValid;
    }

    public String getSniUsed() {
        return sniUsed;
    }

    public void setSniUsed(String sniUsed) {
        this.sniUsed = sniUsed;
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
