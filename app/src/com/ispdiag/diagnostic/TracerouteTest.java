package com.ispdiag.diagnostic;

import com.ispdiag.model.TracerouteHop;
import com.ispdiag.model.TracerouteResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

/**
 * Traceroute simplificado usando ping com TTL crescente.
 * Funciona sem root usando Runtime.exec("ping").
 * Corrigido: nao repete mais o salto de destino multiplas vezes.
 */
public class TracerouteTest {

    private static final int MAX_HOPS = 20;
    private static final int TIMEOUT_MS = 2000;

    public TracerouteResult execute(String host) {
        TracerouteResult result = new TracerouteResult(host);
        long startTime = System.currentTimeMillis();

        try {
            // Resolve o IP do destino primeiro
            InetAddress targetAddr = InetAddress.getByName(host);
            String targetIp = targetAddr.getHostAddress();
            result.setTargetIp(targetIp);

            for (int ttl = 1; ttl <= MAX_HOPS; ttl++) {
                TracerouteHop hop = new TracerouteHop(ttl);

                try {
                    // Usa ping com TTL
                    ProcessBuilder pb = new ProcessBuilder(
                            "/system/bin/ping", "-c", "1", "-t", String.valueOf(ttl),
                            "-W", "2", host);
                    pb.redirectErrorStream(true);
                    long hopStart = System.currentTimeMillis();
                    Process process = pb.start();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    process.waitFor();
                    long hopTime = System.currentTimeMillis() - hopStart;
                    reader.close();

                    String outStr = output.toString();

                    // Verifica se chegamos ao destino primeiro
                    if (outStr.contains("bytes from") || outStr.contains("time=")) {
                        // Destino alcancado
                        hop.setAddress(targetIp);
                        hop.setHostname(host);
                        hop.setTimeout(false);

                        // Extrai o tempo da saida do ping
                        String timeStr = extractTimeFromPing(outStr);
                        if (timeStr != null) {
                            try {
                                hop.setRttMs((long) Float.parseFloat(timeStr));
                            } catch (NumberFormatException e) {
                                hop.setRttMs(hopTime);
                            }
                        } else {
                            hop.setRttMs(hopTime);
                        }

                        result.addHop(hop);
                        result.setCompleted(true);
                        result.setTotalHops(ttl);
                        break; // Para aqui - chegamos ao destino
                    } else if (outStr.contains("From") || outStr.contains("from")) {
                        // TTL excedido - salto intermediario
                        String hopIp = extractIpFromPing(outStr);
                        if (hopIp != null) {
                            // Verifica se este salto intermediario ja e o destino
                            if (hopIp.equals(targetIp)) {
                                hop.setAddress(targetIp);
                                hop.setHostname(host);
                                hop.setTimeout(false);
                                hop.setRttMs(hopTime);
                                result.addHop(hop);
                                result.setCompleted(true);
                                result.setTotalHops(ttl);
                                break; // Para - destino alcancado via TTL excedido
                            }

                            hop.setAddress(hopIp);
                            hop.setTimeout(false);
                            hop.setRttMs(hopTime);

                            // Tenta DNS reverso
                            try {
                                InetAddress hopAddr = InetAddress.getByName(hopIp);
                                String hostname = hopAddr.getCanonicalHostName();
                                if (!hostname.equals(hopIp)) {
                                    hop.setHostname(hostname);
                                } else {
                                    hop.setHostname(hopIp);
                                }
                            } catch (Exception e) {
                                hop.setHostname(hopIp);
                            }
                        }
                    }
                    // senao timeout

                } catch (Exception e) {
                    // Timeout ou erro para este salto
                    hop.setTimeout(true);
                }

                result.addHop(hop);
            }

        } catch (Exception e) {
            result.setCompleted(false);
        }

        result.setTotalTimeMs(System.currentTimeMillis() - startTime);
        if (!result.isCompleted()) {
            result.setTotalHops(result.getHops().size());
        }
        return result;
    }

    private String extractIpFromPing(String output) {
        // Padrao: "From 10.0.0.1" ou "from 10.0.0.1:"
        try {
            int fromIdx = output.toLowerCase().indexOf("from ");
            if (fromIdx >= 0) {
                String sub = output.substring(fromIdx + 5).trim();
                // Extrai IP (para no espaco, dois-pontos ou parenteses)
                StringBuilder ip = new StringBuilder();
                for (char c : sub.toCharArray()) {
                    if (c == ' ' || c == ':' || c == ')' || c == '\n')
                        break;
                    if (c == '(') {
                        ip.setLength(0);
                        continue;
                    }
                    ip.append(c);
                }
                return ip.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractTimeFromPing(String output) {
        // Padrao: "time=XX.X ms"
        try {
            int timeIdx = output.indexOf("time=");
            if (timeIdx >= 0) {
                String sub = output.substring(timeIdx + 5);
                StringBuilder time = new StringBuilder();
                for (char c : sub.toCharArray()) {
                    if (c == ' ' || c == 'm')
                        break;
                    time.append(c);
                }
                return time.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
