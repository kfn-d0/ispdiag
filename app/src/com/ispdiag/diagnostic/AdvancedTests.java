package com.ispdiag.diagnostic;

import com.ispdiag.model.AdvancedResult;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Testes avancados de diagnostico: deteccao de MTU, bloqueio de SNI,
 * deteccao de proxy/interceptador.
 */
public class AdvancedTests {

    private final int timeoutMs;

    // CA conhecidos e confiaveis para servicos principais
    private static final String[][] TRUSTED_ISSUERS = {
            { "google.com", "Google Trust Services" },
            { "tiktok.com", "DigiCert" },
            { "whatsapp.com", "DigiCert" },
            { "instagram.com", "DigiCert" },
            { "steampowered.com", "" },
            { "netflix.com", "DigiCert" },
    };

    public AdvancedTests(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public AdvancedResult runAll(String host) {
        AdvancedResult result = new AdvancedResult();

        // Deteccao de MTU
        detectMtu(host, result);

        // Deteccao de bloqueio de SNI
        detectSniBlocking(host, result);

        // Deteccao de proxy/interceptador
        detectProxy(host, result);

        return result;
    }

    /**
     * Deteccao de MTU utilizando pacotes UDP com tamanhos crescentes.
     * Envia pacotes UDP e verifica se ha fragmentacao.
     */
    private void detectMtu(String host, AdvancedResult result) {
        int mtu = 1500; // Padrao
        try {
            InetAddress addr = InetAddress.getByName(host);

            // Testa tamanhos comuns de MTU
            int[] testSizes = { 1500, 1492, 1480, 1472, 1400, 1300, 1200, 1000, 576 };

            for (int size : testSizes) {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(2000);

                    // Cria pacote com bit DF (Don't Fragment)
                    byte[] data = new byte[size - 28]; // subtrai cabecalhos IP+UDP
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, addr, 33434 // porta traceroute
                    );

                    socket.send(packet);
                    mtu = size;
                    socket.close();
                    break; // Maior tamanho que funciona

                } catch (Exception e) {
                    // Este tamanho falhou, tenta um menor
                    continue;
                }
            }

            result.setDetectedMtu(mtu);
            result.setMtuFragmentation(mtu < 1500);

            if (mtu < 1500) {
                result.setMtuDetails("MTU reduzido detectado (" + mtu + " bytes). "
                        + "Possivel PPPoE, VPN ou tunelamento.");
            } else {
                result.setMtuDetails("MTU padrao (1500 bytes). Sem fragmentacao detectada.");
            }

        } catch (Exception e) {
            result.setDetectedMtu(0);
            result.setMtuDetails("Nao foi possivel detectar MTU: " + e.getMessage());
        }
    }

    /**
     * Deteccao de bloqueio de SNI.
     * Compara handshake TLS com SNI correto vs sem SNI.
     */
    private void detectSniBlocking(String host, AdvancedResult result) {
        long startTime = System.currentTimeMillis();

        try {
            // Teste 1: TLS normal com SNI correto
            boolean normalWorks = testTlsConnection(host, host);

            // Teste 2: TLS com SNI invalido ou vazio
            boolean noSniWorks = testTlsConnection(host, "");

            result.setSniTestTimeMs(System.currentTimeMillis() - startTime);

            if (normalWorks && !noSniWorks) {
                // Caso normal - SNI obrigatorio
                result.setSniBlocked(false);
                result.setSniDetails("SNI funcional. Servidor requer SNI (comportamento normal).");
            } else if (!normalWorks && noSniWorks) {
                // Possivel bloqueio de SNI
                result.setSniBlocked(true);
                result.setSniDetails("ALERTA: Conexao com SNI falhou mas sem SNI funcionou. "
                        + "Possivel bloqueio de SNI pelo ISP ou firewall.");
            } else if (!normalWorks && !noSniWorks) {
                // Ambos falham - provavelmente bloqueado totalmente
                result.setSniBlocked(false);
                result.setSniDetails("TLS falhou com e sem SNI. Servico pode estar bloqueado "
                        + "completamente ou servidor indisponivel.");
            } else {
                result.setSniBlocked(false);
                result.setSniDetails("TLS funcional com e sem SNI. Sem indicios de bloqueio.");
            }

        } catch (Exception e) {
            result.setSniTestTimeMs(System.currentTimeMillis() - startTime);
            result.setSniBlocked(false);
            result.setSniDetails("Erro no teste de SNI: " + e.getMessage());
        }
    }

    private boolean testTlsConnection(String host, String sni) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            Socket rawSocket = new Socket(addr, 443);
            rawSocket.setSoTimeout(timeoutMs);

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket;

            if (sni != null && !sni.isEmpty()) {
                sslSocket = (SSLSocket) factory.createSocket(rawSocket, sni, 443, true);
            } else {
                sslSocket = (SSLSocket) factory.createSocket(rawSocket, "", 443, true);
            }

            sslSocket.startHandshake();
            sslSocket.close();
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deteccao de proxy/interceptador.
     * Verifica se o emissor do certificado TLS corresponde ao CA confiavel
     * esperado.
     */
    private void detectProxy(String host, AdvancedResult result) {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, 443);
            socket.setSoTimeout(timeoutMs);
            socket.startHandshake();

            Certificate[] certs = socket.getSession().getPeerCertificates();
            socket.close();

            if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certs[0];
                String issuer = cert.getIssuerX500Principal().getName();

                result.setActualCertIssuer(issuer);

                // Compara com os emissores conhecidos
                String expectedIssuer = getExpectedIssuer(host);
                result.setExpectedCertIssuer(expectedIssuer);

                if (expectedIssuer != null && !expectedIssuer.isEmpty()) {
                    boolean matches = issuer.toLowerCase().contains(expectedIssuer.toLowerCase());
                    result.setCertMismatch(!matches);

                    if (!matches) {
                        result.setProxyDetected(true);
                        result.setProxyType("TLS Interception / MITM Proxy");
                        result.setProxyDetails("ALERTA: Certificado emitido por '"
                                + issuer + "' em vez do esperado '" + expectedIssuer
                                + "'. Possivel proxy transparente, firewall corporativo ou MITM.");
                    } else {
                        result.setProxyDetected(false);
                        result.setProxyDetails("Certificado corresponde ao emissor esperado. "
                                + "Sem indicios de interceptacao.");
                    }
                } else {
                    result.setProxyDetected(false);
                    result.setProxyDetails("Emissor: " + issuer + ". "
                            + "Sem referencia para comparacao.");
                }
            }

        } catch (Exception e) {
            result.setProxyDetected(false);
            result.setProxyDetails("Nao foi possivel verificar: " + e.getMessage());
        }
    }

    private String getExpectedIssuer(String host) {
        for (String[] entry : TRUSTED_ISSUERS) {
            if (host.contains(entry[0])) {
                return entry[1];
            }
        }
        return null;
    }
}
