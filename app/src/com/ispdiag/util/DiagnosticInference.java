package com.ispdiag.util;

import com.ispdiag.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated inference engine for diagnostic results.
 * Analyzes layer-by-layer data and produces structured conclusions
 * with confidence levels (Alta, Media, Baixa).
 */
public class DiagnosticInference {

    public static class Finding {
        public final String icon; // [!] or [✓] or [~]
        public final String message;

        public Finding(String icon, String message) {
            this.icon = icon;
            this.message = message;
        }
    }

    public static class Conclusion {
        public final String text;
        public final String confidence; // Alta, Media, Baixa

        public Conclusion(String text, String confidence) {
            this.text = text;
            this.confidence = confidence;
        }
    }

    public static class InferenceResult {
        public final String serviceName;
        public final List<Finding> findings;
        public final List<Conclusion> conclusions;

        public InferenceResult(String serviceName) {
            this.serviceName = serviceName;
            this.findings = new ArrayList<>();
            this.conclusions = new ArrayList<>();
        }
    }

    /**
     * Executa a inferencia em um unico resultado de diagnostico.
     */
    public static InferenceResult analyze(DiagResult result) {
        InferenceResult ir = new InferenceResult(result.getTarget().getName());

        analyzeDns(result, ir);
        analyzeTcp(result, ir);
        analyzeTls(result, ir);
        analyzeHttp(result, ir);
        analyzeDnsComparison(result, ir);
        analyzeAdvanced(result, ir);

        return ir;
    }

    /**
     * Executa a inferencia em todos os resultados e no ambiente.
     */
    public static List<InferenceResult> analyzeAll(List<DiagResult> results, EnvironmentInfo env) {
        List<InferenceResult> all = new ArrayList<>();

        // Resultados globais
        InferenceResult global = new InferenceResult("Geral");
        analyzeGlobal(results, env, global);
        if (!global.findings.isEmpty() || !global.conclusions.isEmpty()) {
            all.add(global);
        }

        for (DiagResult result : results) {
            InferenceResult ir = analyze(result);
            if (!ir.findings.isEmpty() || !ir.conclusions.isEmpty()) {
                all.add(ir);
            }
        }
        return all;
    }

    private static void analyzeGlobal(List<DiagResult> results, EnvironmentInfo env, InferenceResult ir) {
        if (env != null) {
            if (env.isCgnatDetected()) {
                ir.findings.add(new Finding("[~]", "CGNAT detectado (" + env.getCgnatReason() + ")"));
            }
            if (!env.isIpv6Available()) {
                ir.findings.add(new Finding("[~]", "IPv6 indisponivel nesta rede"));
            }
        }

        // Verifica se TODOS os servicos falham na mesma camada
        int dnsFailCount = 0;
        int tcpFailCount = 0;
        int tlsFailCount = 0;
        int total = results.size();

        for (DiagResult r : results) {
            if (r.getDnsResult() != null && !r.getDnsResult().isSuccess())
                dnsFailCount++;
            if (r.getTcpPort443() != null && !r.getTcpPort443().isSuccess())
                tcpFailCount++;
            if (r.getTlsResult() != null && !r.getTlsResult().isSuccess())
                tlsFailCount++;
        }

        if (total > 1 && dnsFailCount == total) {
            ir.conclusions.add(new Conclusion(
                    "DNS falha em todos os servicos - problema no resolver do ISP ou sem conectividade",
                    "Alta"));
        } else if (total > 1 && tcpFailCount == total) {
            ir.conclusions.add(new Conclusion(
                    "TCP falha em todos os servicos - possivel queda total ou bloqueio na rede",
                    "Alta"));
        } else if (total > 2 && tlsFailCount == total) {
            ir.conclusions.add(new Conclusion(
                    "TLS falha em todos os servicos - possivel DPI/proxy interceptando conexoes",
                    "Alta"));
        }
    }

    private static void analyzeDns(DiagResult result, InferenceResult ir) {
        DnsResult dns = result.getDnsResult();
        if (dns == null)
            return;

        if (!dns.isSuccess()) {
            String errorType = dns.getErrorType() != null ? dns.getErrorType() : "ERRO";
            ir.findings.add(new Finding("[!]", "DNS falhou: " + errorType));

            if ("NXDOMAIN".equals(errorType)) {
                ir.conclusions.add(new Conclusion(
                        "Resolver retornou NXDOMAIN - dominio nao encontrado ou bloqueado",
                        "Alta"));
            } else if ("TIMEOUT".equals(errorType)) {
                ir.conclusions.add(new Conclusion(
                        "DNS timeout - resolver lento ou inacessivel",
                        "Media"));
            }
        } else {
            long dnsTime = dns.getResponseTimeMs();
            if (dnsTime > 200) {
                ir.findings.add(new Finding("[~]", "DNS lento: " + dnsTime + "ms"));
                ir.conclusions.add(new Conclusion(
                        "Latencia DNS alta - resolver do ISP sobrecarregado ou distante",
                        "Media"));
            } else {
                ir.findings.add(new Finding("[ok]", "DNS OK (" + dnsTime + "ms)"));
            }
        }
    }

    private static void analyzeTcp(DiagResult result, InferenceResult ir) {
        TcpResult tcp443 = result.getTcpPort443();
        if (tcp443 == null)
            return;

        if (!tcp443.isSuccess()) {
            String errorType = tcp443.getErrorType() != null ? tcp443.getErrorType() : "ERRO";
            ir.findings.add(new Finding("[!]", "TCP:443 falhou: " + errorType));

            if ("RST".equals(errorType) || "CONNECTION_REFUSED".equals(errorType)) {
                ir.conclusions.add(new Conclusion(
                        "Porta 443 recusada - firewall ou servico indisponivel",
                        "Alta"));
            } else if ("TIMEOUT".equals(errorType)) {
                ir.conclusions.add(new Conclusion(
                        "TCP timeout - porta filtrada, rota com problema ou host down",
                        "Media"));
            }
        } else {
            ir.findings.add(new Finding("[ok]", "TCP:443 OK (" + tcp443.getConnectTimeMs() + "ms)"));
        }
    }

    private static void analyzeTls(DiagResult result, InferenceResult ir) {
        TlsResult tls = result.getTlsResult();
        if (tls == null)
            return;

        if (!tls.isSuccess()) {
            ir.findings.add(
                    new Finding("[!]", "TLS falhou: " + (tls.getErrorType() != null ? tls.getErrorType() : "ERRO")));
            ir.conclusions.add(new Conclusion(
                    "Handshake TLS falhou - possivel interceptacao, DPI ou certificado invalido",
                    "Alta"));
        } else {
            long tcpTime = 0;
            if (result.getTcpPort443() != null && result.getTcpPort443().isSuccess()) {
                tcpTime = result.getTcpPort443().getConnectTimeMs();
            }
            long tlsTime = tls.getHandshakeTimeMs();

            ir.findings.add(new Finding("[ok]", "TLS OK (" + tlsTime + "ms) " + tls.getTlsVersion()));

            // Detecta atraso no handshake: TLS >= 3x linha de base TCP
            if (tcpTime > 0 && tcpTime <= 50 && tlsTime >= tcpTime * 3) {
                ir.findings.add(new Finding("[~]",
                        "TLS handshake " + (tlsTime / Math.max(tcpTime, 1)) + "x mais lento que TCP"));
            }
        }
    }

    private static void analyzeHttp(DiagResult result, InferenceResult ir) {
        HttpResult http = result.getHttpResult();
        if (http == null)
            return;

        if (!http.isSuccess()) {
            ir.findings.add(
                    new Finding("[!]", "HTTP falhou: " + (http.getErrorType() != null ? http.getErrorType() : "ERRO")));
        } else {
            ir.findings.add(
                    new Finding("[ok]", "HTTP OK (" + http.getTotalTimeMs() + "ms) [" + http.getStatusCode() + "]"));

            // Analise de TTFB com correlacao TCP
            long avgTtfb = http.getAvgTtfbMs();
            if (avgTtfb > 0) {
                long tcpTime = 0;
                if (result.getTcpPort443() != null && result.getTcpPort443().isSuccess()) {
                    tcpTime = result.getTcpPort443().getConnectTimeMs();
                }

                if (avgTtfb > 500) {
                    long serverDelta = avgTtfb - tcpTime;
                    ir.findings
                            .add(new Finding("[~]", "TTFB alto: " + avgTtfb + "ms (servidor: ~" + serverDelta + "ms)"));

                    if (tcpTime > 0 && tcpTime < 50) {
                        ir.conclusions.add(new Conclusion(
                                "Transporte rapido (TCP " + tcpTime + "ms) mas TTFB alto (" + avgTtfb
                                        + "ms) - processamento server-side lento, nao e problema do ISP",
                                "Alta"));
                    } else {
                        ir.conclusions.add(new Conclusion(
                                "TTFB alto (" + avgTtfb + "ms) - verificar se CDN responde de edge local ou origin",
                                "Media"));
                    }
                }
            }
        }
    }

    private static void analyzeDnsComparison(DiagResult result, InferenceResult ir) {
        List<DnsComparisonEntry> comp = result.getDnsComparison();
        if (comp == null || comp.isEmpty())
            return;

        DnsComparisonEntry sistema = null;
        DnsComparisonEntry google = null;

        for (DnsComparisonEntry e : comp) {
            if ("Sistema".equals(e.getResolverName()))
                sistema = e;
            if ("Google".equals(e.getResolverName()))
                google = e;
        }

        if (sistema != null && google != null) {
            // DNS do ISP falha mas Google funciona
            if (!sistema.isSuccess() && google.isSuccess()) {
                ir.findings.add(new Finding("[!]", "DNS sistema falhou"));
                ir.findings.add(new Finding("[ok]", "DNS Google resolve"));
                ir.conclusions.add(new Conclusion(
                        "Possivel bloqueio no resolver do ISP",
                        "Alta"));
            }
            // DNS do ISP muito mais lento que o Google
            else if (sistema.isSuccess() && google.isSuccess()) {
                long sistemaMs = sistema.getResponseTimeMs();
                long googleMs = google.getResponseTimeMs();
                if (sistemaMs > 200 && googleMs < 50) {
                    ir.findings.add(new Finding("[~]", "DNS ISP: " + sistemaMs + "ms vs Google: " + googleMs + "ms"));
                    ir.conclusions.add(new Conclusion(
                            "DNS ISP com latencia " + sistemaMs + "ms (Google: " + googleMs
                                    + "ms) - resolver sobrecarregado",
                            "Media"));
                }
            }
        }
    }

    private static void analyzeAdvanced(DiagResult result, InferenceResult ir) {
        AdvancedResult adv = result.getAdvancedResult();
        if (adv == null)
            return;

        if (adv.isSniBlocked()) {
            ir.findings.add(new Finding("[!]", "SNI bloqueado detectado"));
            ir.conclusions.add(new Conclusion(
                    "Bloqueio por SNI - DPI filtrando conexoes baseado no hostname",
                    "Alta"));
        }

        if (adv.isProxyDetected()) {
            ir.findings.add(new Finding("[!]", "Proxy/MITM detectado"));
            ir.conclusions.add(new Conclusion(
                    "Interceptacao TLS - certificado diferente do esperado",
                    "Alta"));
        }
    }

    /**
     * Gera uma string legivel com o relatorio tecnico.
     */
    public static String buildTechReport(List<DiagResult> results, EnvironmentInfo env) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== DIAGNOSTICO TECNICO ====\n\n");

        List<InferenceResult> inferences = analyzeAll(results, env);

        for (InferenceResult ir : inferences) {
            sb.append(">> ").append(ir.serviceName).append("\n");

            for (Finding f : ir.findings) {
                sb.append("  ").append(f.icon).append(" ").append(f.message).append("\n");
            }

            if (!ir.conclusions.isEmpty()) {
                sb.append("\n  Conclusao:\n");
                for (Conclusion c : ir.conclusions) {
                    sb.append("  - ").append(c.text).append("\n");
                    sb.append("    Confianca: ").append(c.confidence).append("\n");
                }
            }
            sb.append("\n");
        }

        // Linha de resumo
        int ok = 0, partial = 0, fail = 0;
        for (DiagResult r : results) {
            switch (r.getOverallStatus()) {
                case OK:
                    ok++;
                    break;
                case PARTIAL:
                    partial++;
                    break;
                case FAIL:
                    fail++;
                    break;
            }
        }
        sb.append("==== RESUMO: ").append(results.size()).append(" servicos | ");
        sb.append("OK:").append(ok).append(" Parcial:").append(partial).append(" Falha:").append(fail);
        sb.append(" ====\n");

        return sb.toString();
    }
}
