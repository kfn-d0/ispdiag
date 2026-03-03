package com.ispdiag.diagnostic;

import com.ispdiag.model.HttpResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Teste de Requisicao HTTP com analise de TTFB (Tempo ate o Primeiro Byte).
 * Executa 3 iteracoes para obter medidas confiaveis de TTFB.
 * TTFB = tempo do envio da requisicao ate o recebimento do primeiro byte de
 * resposta.
 * Isso mede o tempo de processamento no servidor (backend, CDN, WAF, etc.)
 * NAO a latencia de transporte.
 *
 * totalTimeMs reporta apenas o tempo da PRIMEIRA requisicao (nao cumulativo).
 */
public class HttpTest {

    private final int timeoutMs;
    private final int maxRedirects;
    private static final int TTFB_ITERATIONS = 3;

    public HttpTest(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.maxRedirects = 5;
    }

    /**
     * Executa teste HTTP com analise de TTFB (3 iteracoes).
     * totalTimeMs reflete uma unica requisicao, NAO a soma de todas as iteracoes.
     */
    public HttpResult execute(String urlStr) {
        HttpResult result = new HttpResult();
        List<String> redirectChain = new ArrayList<>();

        try {
            // Primeiro: segue redirecionamentos para encontrar a URL final
            String finalUrl = followRedirects(urlStr, redirectChain);
            result.setRedirectChain(redirectChain);

            // Executa iteracoes de TTFB na URL final
            long[] ttfbValues = new long[TTFB_ITERATIONS];
            int successCount = 0;
            int lastStatusCode = -1;

            for (int i = 0; i < TTFB_ITERATIONS; i++) {
                try {
                    TtfbMeasurement m = measureTtfb(finalUrl);
                    ttfbValues[i] = m.ttfbMs;
                    lastStatusCode = m.statusCode;
                    successCount++;
                } catch (Exception e) {
                    ttfbValues[i] = -1;
                }
                // Espera entre as iteracoes para garantir uma nova conexao
                if (i < TTFB_ITERATIONS - 1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        /* ignorar */ }
                }
            }

            result.setStatusCode(lastStatusCode);
            result.setIterations(TTFB_ITERATIONS);

            if (successCount > 0) {
                result.setSuccess(lastStatusCode >= 200 && lastStatusCode < 400);

                // Calcula minimo, maximo e media de TTFB
                long min = Long.MAX_VALUE, max = 0, sum = 0;
                int count = 0;
                long firstSuccessful = -1;
                for (long t : ttfbValues) {
                    if (t >= 0) {
                        if (firstSuccessful < 0)
                            firstSuccessful = t;
                        if (t < min)
                            min = t;
                        if (t > max)
                            max = t;
                        sum += t;
                        count++;
                    }
                }
                long avg = count > 0 ? sum / count : 0;

                // totalTimeMs = primeiro TTFB com sucesso (representa uma unica requisicao)
                result.setTotalTimeMs(firstSuccessful >= 0 ? firstSuccessful : avg);

                result.setTtfbMs(firstSuccessful >= 0 ? firstSuccessful : avg);
                result.setMinTtfbMs(min);
                result.setMaxTtfbMs(max);
                result.setAvgTtfbMs(avg);

                // Gera analise
                result.setTtfbAnalysis(analyzeTtfb(avg, min, max, count));
            } else {
                result.setSuccess(false);
                result.setTotalTimeMs(0);
                result.setErrorType("ERROR");
                result.setErrorMessage("All TTFB iterations failed");
            }

        } catch (java.net.SocketTimeoutException e) {
            result.setSuccess(false);
            result.setErrorType("TIMEOUT");
            result.setErrorMessage("HTTP request timed out");

        } catch (java.net.ConnectException e) {
            result.setSuccess(false);
            result.setErrorType("CONNECTION_REFUSED");
            result.setErrorMessage(e.getMessage());

        } catch (IOException e) {
            result.setSuccess(false);
            result.setErrorType("ERROR");
            result.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorType("ERROR");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Segue os redirecionamentos manualmente, retorna a URL final.
     */
    private String followRedirects(String urlStr, List<String> redirectChain) throws Exception {
        String currentUrl = urlStr;
        int redirectCount = 0;

        while (redirectCount <= maxRedirects) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "ISPDiagnostic/1.0");

            int statusCode = conn.getResponseCode();

            if (statusCode >= 300 && statusCode < 400) {
                String location = conn.getHeaderField("Location");
                if (location != null) {
                    redirectChain.add(currentUrl + " -> " + statusCode);
                    if (location.startsWith("/")) {
                        URL base = new URL(currentUrl);
                        currentUrl = base.getProtocol() + "://" + base.getHost() + location;
                    } else {
                        currentUrl = location;
                    }
                    redirectCount++;
                    conn.disconnect();
                    continue;
                }
            }

            conn.disconnect();
            break;
        }

        return currentUrl;
    }

    /**
     * Mede TTFB para uma unica requisicao.
     * Usa GET com cabecalho Range para evitar baixar o corpo inteiro.
     */
    private TtfbMeasurement measureTtfb(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "ISPDiagnostic/1.0");
            conn.setRequestProperty("Range", "bytes=0-0");
            conn.setRequestProperty("Connection", "close");

            long requestStart = System.currentTimeMillis();

            // getResponseCode() bloqueia ate os cabecalhos chegarem = TTFB
            int statusCode = conn.getResponseCode();
            long ttfb = System.currentTimeMillis() - requestStart;

            TtfbMeasurement m = new TtfbMeasurement();
            m.ttfbMs = ttfb;
            m.statusCode = statusCode;
            return m;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Analyze TTFB and generate human-readable explanation.
     */
    private String analyzeTtfb(long avgMs, long minMs, long maxMs, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("TTFB medio: ").append(avgMs).append("ms");
        sb.append(" (min: ").append(minMs).append("ms, max: ").append(maxMs).append("ms)");
        sb.append(" [").append(count).append("x]\n");

        if (avgMs <= 50) {
            sb.append("Excelente - Resposta via CDN edge local");
        } else if (avgMs <= 150) {
            sb.append("Bom - Servidor/CDN respondendo rapidamente");
        } else if (avgMs <= 300) {
            sb.append("Moderado - Possivel cache miss ou origin distante");
        } else if (avgMs <= 600) {
            sb.append("Alto - Processamento backend/WAF/redirecionamento interno");
        } else {
            sb.append("Muito alto - Origin lento, rate limit ou servidor sobrecarregado");
        }

        // Variability check
        if (count >= 2 && maxMs > 0 && minMs > 0) {
            long variance = maxMs - minMs;
            if (variance > avgMs * 0.5) {
                sb.append("\n! Alta variancia: possivel cache inconsistente ou load balancer");
            }
        }

        return sb.toString();
    }

    private static class TtfbMeasurement {
        long ttfbMs;
        int statusCode;
    }
}
