package com.ispdiag.diagnostic;

import com.ispdiag.model.TlsResult;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Teste de Handshake TLS.
 * Realiza handshake TLS sem baixar conteudo.
 * Captura versao, cipher suite, detalhes do certificado e SNI.
 */
public class TlsTest {

    private final int timeoutMs;

    public TlsTest(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executa teste de handshake TLS contra o host:443.
     */
    public TlsResult execute(String host) {
        TlsResult result = new TlsResult();
        result.setSniUsed(host);
        long startTime = System.currentTimeMillis();

        SSLSocket sslSocket = null;
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) factory.createSocket();
            sslSocket.setSoTimeout(timeoutMs);

            // Ativa SNI
            javax.net.ssl.SSLParameters params = sslSocket.getSSLParameters();
            java.util.List<javax.net.ssl.SNIServerName> serverNames = new java.util.ArrayList<>();
            serverNames.add(new javax.net.ssl.SNIHostName(host));
            params.setServerNames(serverNames);
            sslSocket.setSSLParameters(params);

            // Conecta e efetua handshake
            sslSocket.connect(new java.net.InetSocketAddress(host, 443), timeoutMs);
            sslSocket.startHandshake();

            long elapsed = System.currentTimeMillis() - startTime;
            result.setHandshakeTimeMs(elapsed);

            SSLSession session = sslSocket.getSession();

            // Versao TLS
            result.setTlsVersion(session.getProtocol());

            // Cipher Suite
            result.setCipherSuite(session.getCipherSuite());

            // Informacoes do certificado
            Certificate[] certs = session.getPeerCertificates();
            if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                X509Certificate x509 = (X509Certificate) certs[0];
                result.setCertSubject(x509.getSubjectDN().getName());
                result.setCertIssuer(x509.getIssuerDN().getName());
                result.setCertValidFrom(x509.getNotBefore().toString());
                result.setCertValidTo(x509.getNotAfter().toString());

                // Verifica validade
                try {
                    x509.checkValidity(new Date());
                    result.setCertValid(true);
                } catch (Exception e) {
                    result.setCertValid(false);
                }
            }

            result.setSuccess(true);

        } catch (javax.net.ssl.SSLHandshakeException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setHandshakeTimeMs(elapsed);
            result.setSuccess(false);
            result.setErrorType("HANDSHAKE_FAILED");
            result.setErrorMessage(e.getMessage());

        } catch (javax.net.ssl.SSLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setHandshakeTimeMs(elapsed);
            result.setSuccess(false);
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("certificate")) {
                result.setErrorType("CERT_ERROR");
            } else {
                result.setErrorType("HANDSHAKE_FAILED");
            }
            result.setErrorMessage(msg);

        } catch (java.net.SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setHandshakeTimeMs(elapsed);
            result.setSuccess(false);
            result.setErrorType("TIMEOUT");
            result.setErrorMessage("TLS handshake timed out");

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setHandshakeTimeMs(elapsed);
            result.setSuccess(false);
            result.setErrorType("ERROR");
            result.setErrorMessage(e.getMessage());

        } finally {
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }
}
