package com.ispdiag.util;

import com.ispdiag.model.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Constroi relatorios JSON estruturados a partir de resultados de diagnostico.
 * Aprimorado com traceroute, comparacao DNS e dados de testes avancados.
 */
public class JsonBuilder {

    public static String buildReport(List<DiagResult> results, EnvironmentInfo envInfo) {
        try {
            JSONObject root = new JSONObject();
            root.put("report_type", "ISP Access Diagnostic");
            root.put("version", "2.0");

            if (envInfo != null) {
                root.put("environment", buildEnvironment(envInfo));
            }

            JSONArray servicesArray = new JSONArray();
            for (DiagResult result : results) {
                servicesArray.put(buildServiceResult(result));
            }
            root.put("services", servicesArray);

            root.put("summary", buildSummary(results));

            return root.toString(2);
        } catch (JSONException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static JSONObject buildEnvironment(EnvironmentInfo info) throws JSONException {
        JSONObject env = new JSONObject();
        env.put("timestamp", info.getTimestamp());
        env.put("connection_type", info.getConnectionType());
        env.put("local_ipv4", info.getLocalIpv4());
        env.put("public_ipv4", info.getPublicIpv4());
        env.put("cgnat_detected", info.isCgnatDetected());
        env.put("cgnat_reason", info.getCgnatReason());
        env.put("ipv6_available", info.isIpv6Available());
        if (info.getIpv6Address() != null) {
            env.put("ipv6_address", info.getIpv6Address());
        }
        env.put("dns_servers", info.getDnsServers());
        env.put("gateway", info.getGateway());
        return env;
    }

    private static JSONObject buildServiceResult(DiagResult result) throws JSONException {
        JSONObject service = new JSONObject();
        ServiceTarget target = result.getTarget();

        service.put("name", target.getName());
        service.put("url", target.getUrl());
        service.put("host", target.getHost());
        service.put("overall_status", result.getOverallStatus().name());
        service.put("total_time_ms", result.getTotalTimeMs());
        service.put("timestamp", result.getTimestamp());

        if (result.getDnsResult() != null) {
            service.put("dns", buildDns(result.getDnsResult()));
        }
        if (result.getTcpPort80() != null) {
            service.put("tcp_80", buildTcp(result.getTcpPort80()));
        }
        if (result.getTcpPort443() != null) {
            service.put("tcp_443", buildTcp(result.getTcpPort443()));
        }
        if (result.getTlsResult() != null) {
            service.put("tls", buildTls(result.getTlsResult()));
        }
        if (result.getHttpResult() != null) {
            service.put("http", buildHttp(result.getHttpResult()));
        }

        // Traceroute
        if (result.getTracerouteResult() != null) {
            service.put("traceroute", buildTraceroute(result.getTracerouteResult()));
        }

        // Comparacao DNS
        if (result.getDnsComparison() != null && !result.getDnsComparison().isEmpty()) {
            JSONArray dnsComp = new JSONArray();
            for (DnsComparisonEntry entry : result.getDnsComparison()) {
                JSONObject e = new JSONObject();
                e.put("resolver", entry.getResolverName());
                e.put("resolver_ip", entry.getResolverIp());
                e.put("success", entry.isSuccess());
                e.put("response_time_ms", entry.getResponseTimeMs());
                if (entry.isSuccess()) {
                    e.put("resolved_ip", entry.getResolvedIp());
                } else {
                    e.put("error", entry.getErrorMessage());
                }
                dnsComp.put(e);
            }
            service.put("dns_comparison", dnsComp);
        }

        // Avancado
        if (result.getAdvancedResult() != null) {
            service.put("advanced", buildAdvanced(result.getAdvancedResult()));
        }

        return service;
    }

    private static JSONObject buildTraceroute(TracerouteResult tr) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("target_host", tr.getTargetHost());
        obj.put("target_ip", tr.getTargetIp());
        obj.put("completed", tr.isCompleted());
        obj.put("total_hops", tr.getTotalHops());
        obj.put("total_time_ms", tr.getTotalTimeMs());

        JSONArray hops = new JSONArray();
        for (TracerouteHop hop : tr.getHops()) {
            JSONObject h = new JSONObject();
            h.put("hop", hop.getHopNumber());
            h.put("address", hop.getAddress());
            h.put("hostname", hop.getHostname());
            h.put("rtt_ms", hop.getRttMs());
            h.put("timeout", hop.isTimeout());
            hops.put(h);
        }
        obj.put("hops", hops);
        return obj;
    }

    private static JSONObject buildAdvanced(AdvancedResult adv) throws JSONException {
        JSONObject obj = new JSONObject();

        JSONObject mtu = new JSONObject();
        mtu.put("detected_mtu", adv.getDetectedMtu());
        mtu.put("fragmentation", adv.isMtuFragmentation());
        mtu.put("details", adv.getMtuDetails());
        obj.put("mtu", mtu);

        JSONObject sni = new JSONObject();
        sni.put("blocked", adv.isSniBlocked());
        sni.put("test_time_ms", adv.getSniTestTimeMs());
        sni.put("details", adv.getSniDetails());
        obj.put("sni_blocking", sni);

        JSONObject proxy = new JSONObject();
        proxy.put("detected", adv.isProxyDetected());
        proxy.put("type", adv.getProxyType());
        proxy.put("actual_cert_issuer", adv.getActualCertIssuer());
        proxy.put("expected_cert_issuer", adv.getExpectedCertIssuer());
        proxy.put("cert_mismatch", adv.isCertMismatch());
        proxy.put("details", adv.getProxyDetails());
        obj.put("proxy_detection", proxy);

        return obj;
    }

    private static JSONObject buildDns(DnsResult dns) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("success", dns.isSuccess());
        obj.put("response_time_ms", dns.getResponseTimeMs());
        if (dns.isSuccess()) {
            if (dns.getIpv4Addresses() != null) {
                obj.put("ipv4", new JSONArray(dns.getIpv4Addresses()));
            }
            if (dns.getIpv6Addresses() != null) {
                obj.put("ipv6", new JSONArray(dns.getIpv6Addresses()));
            }
        } else {
            obj.put("error_type", dns.getErrorType());
            obj.put("error_message", dns.getErrorMessage());
        }
        return obj;
    }

    private static JSONObject buildTcp(TcpResult tcp) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("port", tcp.getPort());
        obj.put("success", tcp.isSuccess());
        obj.put("connect_time_ms", tcp.getConnectTimeMs());
        if (!tcp.isSuccess()) {
            obj.put("error_type", tcp.getErrorType());
            obj.put("error_message", tcp.getErrorMessage());
        }
        return obj;
    }

    private static JSONObject buildTls(TlsResult tls) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("success", tls.isSuccess());
        obj.put("handshake_time_ms", tls.getHandshakeTimeMs());
        obj.put("sni_used", tls.getSniUsed());
        if (tls.isSuccess()) {
            obj.put("tls_version", tls.getTlsVersion());
            obj.put("cipher_suite", tls.getCipherSuite());
            JSONObject cert = new JSONObject();
            cert.put("subject", tls.getCertSubject());
            cert.put("issuer", tls.getCertIssuer());
            cert.put("valid_from", tls.getCertValidFrom());
            cert.put("valid_to", tls.getCertValidTo());
            cert.put("is_valid", tls.isCertValid());
            obj.put("certificate", cert);
        } else {
            obj.put("error_type", tls.getErrorType());
            obj.put("error_message", tls.getErrorMessage());
        }
        return obj;
    }

    private static JSONObject buildHttp(HttpResult http) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("success", http.isSuccess());
        obj.put("status_code", http.getStatusCode());
        obj.put("total_time_ms", http.getTotalTimeMs());
        if (http.getRedirectChain() != null && !http.getRedirectChain().isEmpty()) {
            obj.put("redirects", new JSONArray(http.getRedirectChain()));
        }
        if (!http.isSuccess()) {
            obj.put("error_type", http.getErrorType());
            obj.put("error_message", http.getErrorMessage());
        }
        return obj;
    }

    private static JSONObject buildSummary(List<DiagResult> results) throws JSONException {
        JSONObject summary = new JSONObject();
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
        summary.put("total", results.size());
        summary.put("ok", ok);
        summary.put("partial", partial);
        summary.put("fail", fail);
        return summary;
    }

    /**
     * Constroi um relatorio de texto formatado (legivel por humanos).
     */
    public static String buildTextReport(List<DiagResult> results, EnvironmentInfo envInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  ISP ACCESS DIAGNOSTIC REPORT v2.0\n");
        sb.append("========================================\n\n");

        if (envInfo != null) {
            sb.append(">> AMBIENTE\n");
            sb.append("  Timestamp:    ").append(envInfo.getTimestamp()).append("\n");
            sb.append("  Conexao:      ").append(envInfo.getConnectionType()).append("\n");
            sb.append("  IPv4 Local:   ").append(envInfo.getLocalIpv4()).append("\n");
            sb.append("  IPv4 Publico: ").append(envInfo.getPublicIpv4()).append("\n");
            sb.append("  CGNAT:        ").append(envInfo.isCgnatDetected() ? "Sim" : "Nao");
            if (envInfo.getCgnatReason() != null)
                sb.append(" (").append(envInfo.getCgnatReason()).append(")");
            sb.append("\n");
            sb.append("  IPv6:         ");
            if (envInfo.isIpv6Available() && envInfo.getIpv6Address() != null) {
                sb.append(envInfo.getIpv6Address());
            } else {
                sb.append("Indisponivel");
            }
            sb.append("\n");
            sb.append("  DNS:          ").append(envInfo.getDnsServers() != null ? envInfo.getDnsServers() : "N/A")
                    .append("\n");
            sb.append("  Gateway:      ").append(envInfo.getGateway() != null ? envInfo.getGateway() : "N/A")
                    .append("\n\n");
        }

        for (DiagResult result : results) {
            ServiceTarget target = result.getTarget();
            sb.append("----------------------------------------\n");
            sb.append(">> ").append(target.getName()).append("\n");
            sb.append("  URL: ").append(target.getUrl()).append("\n");
            sb.append("  Status: ").append(result.getOverallStatus().name()).append("\n");
            sb.append("  Tempo Total: ").append(result.getTotalTimeMs()).append("ms\n\n");

            if (result.getDnsResult() != null) {
                DnsResult dns = result.getDnsResult();
                sb.append("  DNS: ").append(dns.isSuccess() ? "OK" : "FALHA");
                sb.append(" (").append(dns.getResponseTimeMs()).append("ms)\n");
            }
            if (result.getTcpPort443() != null) {
                TcpResult tcp = result.getTcpPort443();
                sb.append("  TCP:443: ").append(tcp.isSuccess() ? "OK" : "FALHA");
                sb.append(" (").append(tcp.getConnectTimeMs()).append("ms)\n");
            }
            if (result.getTlsResult() != null) {
                TlsResult tls = result.getTlsResult();
                sb.append("  TLS: ").append(tls.isSuccess() ? "OK" : "FALHA");
                sb.append(" (").append(tls.getHandshakeTimeMs()).append("ms)\n");
            }
            if (result.getHttpResult() != null) {
                HttpResult http = result.getHttpResult();
                sb.append("  HTTP: ").append(http.isSuccess() ? "OK" : "FALHA");
                sb.append(" (").append(http.getTotalTimeMs()).append("ms)");
                if (http.getStatusCode() > 0)
                    sb.append(" [").append(http.getStatusCode()).append("]");
                sb.append("\n");
            }

            // Resumo do traceroute
            if (result.getTracerouteResult() != null) {
                TracerouteResult tr = result.getTracerouteResult();
                sb.append("  Traceroute: ").append(tr.getTotalHops()).append(" hops");
                sb.append(tr.isCompleted() ? " (completo)" : " (incompleto)");
                sb.append(" ").append(tr.getTotalTimeMs()).append("ms\n");
            }

            // Resumo avancado
            if (result.getAdvancedResult() != null) {
                AdvancedResult adv = result.getAdvancedResult();
                sb.append("  MTU: ").append(adv.getDetectedMtu()).append(" bytes\n");
                sb.append("  SNI Block: ").append(adv.isSniBlocked() ? "SIM" : "Nao").append("\n");
                sb.append("  Proxy: ").append(adv.isProxyDetected() ? "SIM" : "Nao").append("\n");
            }

            sb.append("\n");
        }

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
        sb.append("========================================\n");
        sb.append("RESUMO: ").append(results.size()).append(" servicos testados\n");
        sb.append("  OK: ").append(ok).append("\n");
        sb.append("  Parcial: ").append(partial).append("\n");
        sb.append("  Falha: ").append(fail).append("\n");
        sb.append("========================================\n");

        return sb.toString();
    }
}
