package com.ispdiag.diagnostic;

import com.ispdiag.model.DnsResult;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Teste de Resolucao DNS.
 * Resolve dominio usando DNS do sistema, mede tempo de resposta,
 * identifica enderecos IPv4/IPv6 e detecta falhas.
 */
public class DnsTest {

    private final int timeoutMs;

    public DnsTest(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executa pesquisa DNS para o host fornecido.
     */
    public DnsResult execute(String host) {
        DnsResult result = new DnsResult();
        long startTime = System.currentTimeMillis();

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            long elapsed = System.currentTimeMillis() - startTime;
            result.setResponseTimeMs(elapsed);

            List<String> ipv4 = new ArrayList<>();
            List<String> ipv6 = new ArrayList<>();

            for (InetAddress addr : addresses) {
                if (addr instanceof Inet4Address) {
                    ipv4.add(addr.getHostAddress());
                } else if (addr instanceof Inet6Address) {
                    ipv6.add(addr.getHostAddress());
                }
            }

            result.setIpv4Addresses(ipv4);
            result.setIpv6Addresses(ipv6);
            result.setSuccess(true);

        } catch (UnknownHostException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setResponseTimeMs(elapsed);
            result.setSuccess(false);

            String msg = e.getMessage();
            if (msg != null && msg.contains("NXDOMAIN")) {
                result.setErrorType("NXDOMAIN");
            } else if (elapsed >= timeoutMs) {
                result.setErrorType("TIMEOUT");
            } else {
                result.setErrorType("NXDOMAIN");
            }
            result.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setResponseTimeMs(elapsed);
            result.setSuccess(false);
            result.setErrorType("ERROR");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }
}
