package com.ispdiag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ispdiag.model.*;

import java.util.List;

/**
 * DetailActivity detalhes tecnicos completos incluindo traceroute e
 * testes avancados.
 */
public class DetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        List<DiagResult> results = MainActivity.lastResults;
        int index = ResultActivity.selectedResultIndex;

        if (results == null || index < 0 || index >= results.size()) {
            finish();
            return;
        }

        DiagResult result = results.get(index);
        ServiceTarget target = result.getTarget();

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvUrl = findViewById(R.id.tv_detail_url);
        tvTitle.setText(target.getName());
        tvUrl.setText(target.getUrl());

        LinearLayout content = findViewById(R.id.detail_content);

        // Status geral
        addSection(content, "STATUS GERAL", result.getOverallStatus().name() +
                " - Tempo total: " + result.getTotalTimeMs() + "ms");

        // Detalhes do dns
        if (result.getDnsResult() != null) {
            DnsResult dns = result.getDnsResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ").append(dns.isSuccess() ? "OK" : "FALHA").append("\n");
            sb.append("Tempo: ").append(dns.getResponseTimeMs()).append("ms\n");
            if (dns.isSuccess()) {
                if (dns.getIpv4Addresses() != null && !dns.getIpv4Addresses().isEmpty()) {
                    sb.append("IPv4: ");
                    for (int i = 0; i < dns.getIpv4Addresses().size(); i++) {
                        if (i > 0)
                            sb.append(", ");
                        sb.append(dns.getIpv4Addresses().get(i));
                    }
                    sb.append("\n");
                }
                if (dns.getIpv6Addresses() != null && !dns.getIpv6Addresses().isEmpty()) {
                    sb.append("IPv6: ");
                    for (int i = 0; i < dns.getIpv6Addresses().size(); i++) {
                        if (i > 0)
                            sb.append(", ");
                        sb.append(dns.getIpv6Addresses().get(i));
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("Tipo de Erro: ").append(dns.getErrorType()).append("\n");
                sb.append("Mensagem: ").append(dns.getErrorMessage()).append("\n");
            }
            addSection(content, "DNS RESOLUTION", sb.toString());
        }

        // Detalhes do tcp
        if (result.getTcpPort80() != null) {
            addSection(content, "TCP PORT 80", formatTcp(result.getTcpPort80()));
        }
        if (result.getTcpPort443() != null) {
            addSection(content, "TCP PORT 443", formatTcp(result.getTcpPort443()));
        }

        // Detalhes do tls
        if (result.getTlsResult() != null) {
            TlsResult tls = result.getTlsResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ").append(tls.isSuccess() ? "OK" : "FALHA").append("\n");
            sb.append("Tempo: ").append(tls.getHandshakeTimeMs()).append("ms\n");
            sb.append("SNI: ").append(tls.getSniUsed()).append("\n");
            if (tls.isSuccess()) {
                sb.append("Versao TLS: ").append(tls.getTlsVersion()).append("\n");
                sb.append("Cipher Suite: ").append(tls.getCipherSuite()).append("\n");
                sb.append("Cert Subject: ").append(tls.getCertSubject()).append("\n");
                sb.append("Cert Issuer: ").append(tls.getCertIssuer()).append("\n");
                sb.append("Valido de: ").append(tls.getCertValidFrom()).append("\n");
                sb.append("Valido ate: ").append(tls.getCertValidTo()).append("\n");
                sb.append("Cert Valido: ").append(tls.isCertValid() ? "Sim" : "Nao").append("\n");
            } else {
                sb.append("Tipo de Erro: ").append(tls.getErrorType()).append("\n");
                sb.append("Mensagem: ").append(tls.getErrorMessage()).append("\n");
            }
            addSection(content, "TLS HANDSHAKE", sb.toString());
        }

        // Detalhes http e analise de ttfb
        if (result.getHttpResult() != null) {
            HttpResult http = result.getHttpResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ").append(http.isSuccess() ? "OK" : "FALHA").append("\n");
            sb.append("Status Code: ").append(http.getStatusCode()).append("\n");
            sb.append("Tempo Total: ").append(http.getTotalTimeMs()).append("ms\n");
            if (http.getRedirectChain() != null && !http.getRedirectChain().isEmpty()) {
                sb.append("Redirects: ").append(http.getRedirectChain().size()).append("\n");
                for (String redirect : http.getRedirectChain()) {
                    sb.append("  > ").append(redirect).append("\n");
                }
            }
            if (!http.isSuccess() && http.getErrorMessage() != null) {
                sb.append("Erro: ").append(http.getErrorType()).append("\n");
                sb.append("Mensagem: ").append(http.getErrorMessage()).append("\n");
            }
            addSection(content, "HTTP REQUEST", sb.toString());

            // Secao TTFB apenas numeros sem texto de diagnostico
            if (http.isSuccess() && http.getAvgTtfbMs() > 0) {
                StringBuilder ttfbSb = new StringBuilder();
                ttfbSb.append("TTFB medio: ").append(http.getAvgTtfbMs()).append("ms\n");
                ttfbSb.append("TTFB min: ").append(http.getMinTtfbMs()).append("ms\n");
                ttfbSb.append("TTFB max: ").append(http.getMaxTtfbMs()).append("ms\n");
                ttfbSb.append("Iteracoes: ").append(http.getIterations()).append("x");
                addSection(content, "TTFB - TEMPO DE RESPOSTA DO SERVIDOR", ttfbSb.toString());
            }
        }

        // Traceroute
        if (result.getTracerouteResult() != null) {
            TracerouteResult tr = result.getTracerouteResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Destino: ").append(tr.getTargetHost()).append("\n");
            sb.append("IP Destino: ").append(tr.getTargetIp() != null ? tr.getTargetIp() : "N/A").append("\n");
            sb.append("Completo: ").append(tr.isCompleted() ? "Sim" : "Nao").append("\n");
            sb.append("Hops: ").append(tr.getTotalHops()).append("\n");
            sb.append("Tempo Total: ").append(tr.getTotalTimeMs()).append("ms\n\n");

            for (TracerouteHop hop : tr.getHops()) {
                sb.append(String.format("%2d  ", hop.getHopNumber()));
                if (hop.isTimeout()) {
                    sb.append("*  timeout\n");
                } else {
                    sb.append(hop.getAddress());
                    if (!hop.getHostname().equals(hop.getAddress())) {
                        sb.append(" (").append(hop.getHostname()).append(")");
                    }
                    sb.append("  ").append(hop.getRttMs()).append("ms\n");
                }
            }
            sb.append("\n--- Nota ---\n");
            sb.append("Traceroute via ICMP pode ser incompleto\n");
            sb.append("por politica de rede (rate-limit, filtro\n");
            sb.append("de backbone ou rota assimetrica).");
            addSection(content, "TRACEROUTE", sb.toString());
        }

        // Comparacao de dns
        if (result.getDnsComparison() != null && !result.getDnsComparison().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (DnsComparisonEntry entry : result.getDnsComparison()) {
                sb.append(String.format("%-12s", entry.getResolverName()));
                if (entry.isSuccess()) {
                    sb.append(entry.getResolvedIp());
                    sb.append("  ").append(entry.getResponseTimeMs()).append("ms");
                } else {
                    sb.append("FALHA");
                    if (entry.getErrorMessage() != null) {
                        sb.append("  ").append(entry.getErrorMessage());
                    }
                }
                sb.append("\n");
            }
            addSection(content, "DNS COMPARISON", sb.toString());
        }

        // Resultados avancados
        if (result.getAdvancedResult() != null) {
            AdvancedResult adv = result.getAdvancedResult();

            // MTU
            StringBuilder mtuSb = new StringBuilder();
            mtuSb.append("MTU Detectado: ").append(adv.getDetectedMtu()).append(" bytes\n");
            mtuSb.append("Fragmentacao: ").append(adv.isMtuFragmentation() ? "Sim" : "Nao").append("\n");
            mtuSb.append(adv.getMtuDetails() != null ? adv.getMtuDetails() : "");
            addSection(content, "DETECCAO DE MTU", mtuSb.toString());

            // Bloqueio de SNI
            StringBuilder sniSb = new StringBuilder();
            sniSb.append("SNI Bloqueado: ").append(adv.isSniBlocked() ? "SIM" : "Nao").append("\n");
            sniSb.append("Tempo Teste: ").append(adv.getSniTestTimeMs()).append("ms\n");
            sniSb.append(adv.getSniDetails() != null ? adv.getSniDetails() : "");
            addSection(content, "VERIFICACAO DE SNI", sniSb.toString());

            // Deteccao de proxy
            StringBuilder proxySb = new StringBuilder();
            proxySb.append("Proxy Detectado: ").append(adv.isProxyDetected() ? "SIM" : "Nao").append("\n");
            if (adv.isProxyDetected()) {
                proxySb.append("Tipo: ").append(adv.getProxyType()).append("\n");
            }
            if (adv.getActualCertIssuer() != null) {
                proxySb.append("Emissor Atual: ").append(adv.getActualCertIssuer()).append("\n");
            }
            if (adv.getExpectedCertIssuer() != null) {
                proxySb.append("Emissor Esperado: ").append(adv.getExpectedCertIssuer()).append("\n");
            }
            proxySb.append("Cert Mismatch: ").append(adv.isCertMismatch() ? "Sim" : "Nao").append("\n");
            proxySb.append(adv.getProxyDetails() != null ? adv.getProxyDetails() : "");
            addSection(content, "DETECCAO DE PROXY/INTERCEPTOR", proxySb.toString());
        }

        // Botao voltar
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String formatTcp(TcpResult tcp) {
        StringBuilder sb = new StringBuilder();
        sb.append("Porta: ").append(tcp.getPort()).append("\n");
        sb.append("Status: ").append(tcp.isSuccess() ? "OK" : "FALHA").append("\n");
        sb.append("Tempo: ").append(tcp.getConnectTimeMs()).append("ms\n");
        if (!tcp.isSuccess()) {
            sb.append("Tipo de Erro: ").append(tcp.getErrorType()).append("\n");
            sb.append("Mensagem: ").append(tcp.getErrorMessage()).append("\n");
        }
        return sb.toString();
    }

    private void addSection(LinearLayout parent, String title, String sectionContent) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundResource(R.drawable.bg_card_rounded);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(10));
        section.setLayoutParams(params);
        section.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Define a cor da secao com base no tipo
        int titleColor = getResources().getColor(R.color.primary, getTheme());
        if (title.contains("MTU") || title.contains("SNI") || title.contains("PROXY")) {
            titleColor = getResources().getColor(R.color.status_partial, getTheme());
        } else if (title.contains("ALERTA")) {
            titleColor = getResources().getColor(R.color.status_fail, getTheme());
        } else if (title.contains("TTFB")) {
            titleColor = getResources().getColor(R.color.accent, getTheme());
        }

        TextView tvSectionTitle = new TextView(this);
        tvSectionTitle.setText(title);
        tvSectionTitle.setTextColor(titleColor);
        tvSectionTitle.setTextSize(10);
        tvSectionTitle.setTypeface(null, Typeface.BOLD);
        tvSectionTitle.setLetterSpacing(0.15f);
        section.addView(tvSectionTitle);

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(getResources().getColor(R.color.divider, getTheme()));
        section.addView(divider);

        TextView tvContent = new TextView(this);
        tvContent.setText(sectionContent);
        tvContent.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        tvContent.setTextSize(12);
        tvContent.setTypeface(Typeface.MONOSPACE);
        tvContent.setLineSpacing(dpToPx(2), 1.0f);
        section.addView(tvContent);

        parent.addView(section);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
