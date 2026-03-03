package com.ispdiag;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ispdiag.model.DiagResult;
import com.ispdiag.model.DnsComparisonEntry;
import com.ispdiag.model.EnvironmentInfo;
import com.ispdiag.view.LatencyChartView;

import java.util.ArrayList;
import java.util.List;

/**
 * ResultActivity - Mostra os resultados do diagnostico, grafico de latencia, e
 * comparacao de DNS.
 */
public class ResultActivity extends Activity {

    public static int selectedResultIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        List<DiagResult> results = MainActivity.lastResults;
        EnvironmentInfo envInfo = MainActivity.lastEnvInfo;

        if (results == null) {
            finish();
            return;
        }

        // Resumo do ambiente
        TextView tvEnvSummary = findViewById(R.id.tv_env_summary);
        if (envInfo != null) {
            StringBuilder envText = new StringBuilder();
            envText.append("Conexao: ").append(envInfo.getConnectionType()).append("\n");
            envText.append("IPv4 Local: ").append(envInfo.getLocalIpv4()).append("\n");
            envText.append("IPv4 Publico: ").append(envInfo.getPublicIpv4()).append("\n");
            envText.append("CGNAT: ").append(envInfo.isCgnatDetected() ? "Detectado" : "Nao detectado").append("\n");
            if (envInfo.isIpv6Available() && envInfo.getIpv6Address() != null
                    && !envInfo.getIpv6Address().equals("N/A")) {
                envText.append("IPv6: ").append(envInfo.getIpv6Address()).append("\n");
            } else {
                envText.append("IPv6: Indisponivel\n");
            }
            String dns = envInfo.getDnsServers();
            envText.append("DNS: ").append(dns != null ? dns : "N/A").append("\n");
            String gw = envInfo.getGateway();
            envText.append("Gateway IPv4: ").append(gw != null ? gw : "N/A");
            tvEnvSummary.setText(envText.toString());
        } else {
            tvEnvSummary.setText("Informacoes de ambiente indisponiveis");
        }

        // Cria cartoes de resultados
        LinearLayout resultsList = findViewById(R.id.results_list);
        LayoutInflater inflater = LayoutInflater.from(this);

        // Adiciona grafico de latencia
        addLatencyChart(resultsList, results);

        // Adiciona secao de comparacao de DNS
        addDnsComparison(resultsList, results);

        // Cartoes de resultados de servico
        for (int i = 0; i < results.size(); i++) {
            DiagResult result = results.get(i);
            View itemView = inflater.inflate(R.layout.item_result, resultsList, false);

            View statusIndicator = itemView.findViewById(R.id.status_indicator);
            TextView tvName = itemView.findViewById(R.id.tv_result_name);
            TextView tvStatus = itemView.findViewById(R.id.tv_result_status);
            tvName.setText(result.getTarget().getName());

            int statusColor;
            String statusText;
            int badgeDrawable;
            int dotDrawable;

            switch (result.getOverallStatus()) {
                case OK:
                    statusColor = getResources().getColor(R.color.status_ok, getTheme());
                    statusText = "OK";
                    badgeDrawable = R.drawable.badge_ok;
                    dotDrawable = R.drawable.dot_ok;
                    break;
                case PARTIAL:
                    statusColor = getResources().getColor(R.color.status_partial, getTheme());
                    statusText = "PARCIAL";
                    badgeDrawable = R.drawable.badge_partial;
                    dotDrawable = R.drawable.dot_partial;
                    break;
                default:
                    statusColor = getResources().getColor(R.color.status_fail, getTheme());
                    statusText = "FALHA";
                    badgeDrawable = R.drawable.badge_fail;
                    dotDrawable = R.drawable.dot_fail;
                    break;
            }

            statusIndicator.setBackgroundResource(dotDrawable);
            tvStatus.setText(statusText);
            tvStatus.setTextColor(statusColor);
            tvStatus.setBackgroundResource(badgeDrawable);

            setTestStatus(itemView, R.id.tv_dns_status,
                    result.getDnsResult() != null && result.getDnsResult().isSuccess());
            setTestStatus(itemView, R.id.tv_tcp_status,
                    result.getTcpPort443() != null && result.getTcpPort443().isSuccess());
            setTestStatus(itemView, R.id.tv_tls_status,
                    result.getTlsResult() != null && result.getTlsResult().isSuccess());

            TextView tvHttpStatus = itemView.findViewById(R.id.tv_http_status);
            if (result.getHttpResult() != null) {
                if (result.getHttpResult().isSuccess()) {
                    tvHttpStatus.setText(String.valueOf(result.getHttpResult().getStatusCode()));
                    tvHttpStatus.setTextColor(getResources().getColor(R.color.status_ok, getTheme()));
                } else {
                    tvHttpStatus.setText("ERR");
                    tvHttpStatus.setTextColor(getResources().getColor(R.color.status_fail, getTheme()));
                }
            } else {
                tvHttpStatus.setText("--");
                tvHttpStatus.setTextColor(getResources().getColor(R.color.text_hint, getTheme()));
            }

            TextView tvTotalTime = itemView.findViewById(R.id.tv_total_time);
            String timeText = "Total: " + result.getTotalTimeMs() + "ms";
            if (result.getHttpResult() != null && result.getHttpResult().isSuccess()
                    && result.getHttpResult().getAvgTtfbMs() > 0) {
                timeText += "  |  TTFB: " + result.getHttpResult().getAvgTtfbMs() + "ms";
            }
            tvTotalTime.setText(timeText);

            final int index = i;
            Button btnDetails = itemView.findViewById(R.id.btn_details);
            btnDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedResultIndex = index;
                    Intent intent = new Intent(ResultActivity.this, DetailActivity.class);
                    startActivity(intent);
                }
            });

            resultsList.addView(itemView);
        }

        // Botao de relatorio
        Button btnReport = findViewById(R.id.btn_report);
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, ReportActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Adiciona grafico de latencia a vista de resultados.
     */
    private void addLatencyChart(LinearLayout parent, List<DiagResult> results) {
        // Titulo da secao
        TextView title = new TextView(this);
        title.setText("LATENCIA POR CAMADA");
        title.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        title.setTextSize(11);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(dpToPx(4), 0, 0, dpToPx(8));
        title.setLayoutParams(titleParams);
        parent.addView(title);

        // Constroi os dados do grafico
        List<LatencyChartView.ServiceLatency> chartData = new ArrayList<>();
        for (DiagResult r : results) {
            float dns = r.getDnsResult() != null ? r.getDnsResult().getResponseTimeMs() : 0;
            float tcp = r.getTcpPort443() != null ? r.getTcpPort443().getConnectTimeMs() : 0;
            float tls = r.getTlsResult() != null ? r.getTlsResult().getHandshakeTimeMs() : 0;
            float http = r.getHttpResult() != null ? r.getHttpResult().getTotalTimeMs() : 0;
            chartData.add(new LatencyChartView.ServiceLatency(
                    r.getTarget().getName(), dns, tcp, tls, http));
        }

        LatencyChartView chart = new LatencyChartView(this);
        chart.setData(chartData);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chartParams.setMargins(0, 0, 0, dpToPx(16));
        chart.setLayoutParams(chartParams);
        chart.setBackgroundResource(R.drawable.bg_card_rounded);
        chart.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        parent.addView(chart);
    }

    /**
     * Adiciona secao de comparacao de DNS.
     */
    private void addDnsComparison(LinearLayout parent, List<DiagResult> results) {
        // Verifica se algum resultado possui dados de comparacao DNS
        boolean hasDnsComp = false;
        for (DiagResult r : results) {
            if (r.getDnsComparison() != null && !r.getDnsComparison().isEmpty()) {
                hasDnsComp = true;
                break;
            }
        }
        if (!hasDnsComp)
            return;

        // Titulo da secao
        TextView title = new TextView(this);
        title.setText("COMPARACAO DNS");
        title.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        title.setTextSize(11);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(dpToPx(4), 0, 0, dpToPx(8));
        title.setLayoutParams(titleParams);
        parent.addView(title);

        // Mostra a comparacao DNS do primeiro servico como exemplo
        for (DiagResult r : results) {
            if (r.getDnsComparison() == null || r.getDnsComparison().isEmpty())
                continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_rounded);
            card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(10));
            card.setLayoutParams(cardParams);

            // Nome do servico
            TextView svcName = new TextView(this);
            svcName.setText(r.getTarget().getName());
            svcName.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            svcName.setTextSize(14);
            svcName.setTypeface(null, Typeface.BOLD);
            card.addView(svcName);

            // Entradas de DNS
            for (DnsComparisonEntry entry : r.getDnsComparison()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, dpToPx(4), 0, 0);
                row.setLayoutParams(rowParams);

                // Nome do resolver
                TextView tvResolver = new TextView(this);
                tvResolver.setText(entry.getResolverName());
                tvResolver.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                tvResolver.setTextSize(11);
                tvResolver.setWidth(dpToPx(90));
                row.addView(tvResolver);

                // IP
                TextView tvIp = new TextView(this);
                tvIp.setTextSize(11);
                tvIp.setTypeface(Typeface.MONOSPACE);
                LinearLayout.LayoutParams ipParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvIp.setLayoutParams(ipParams);
                if (entry.isSuccess()) {
                    tvIp.setText(entry.getResolvedIp());
                    tvIp.setTextColor(getResources().getColor(R.color.status_ok, getTheme()));
                } else {
                    tvIp.setText("FALHA");
                    tvIp.setTextColor(getResources().getColor(R.color.status_fail, getTheme()));
                }
                row.addView(tvIp);

                // Tempo
                TextView tvTime = new TextView(this);
                tvTime.setText(entry.getResponseTimeMs() + "ms");
                tvTime.setTextColor(getResources().getColor(R.color.text_hint, getTheme()));
                tvTime.setTextSize(10);
                row.addView(tvTime);

                card.addView(row);
            }

            parent.addView(card);
        }
    }

    private void setTestStatus(View parent, int viewId, boolean success) {
        TextView tv = parent.findViewById(viewId);
        if (success) {
            tv.setText("OK");
            tv.setTextColor(getResources().getColor(R.color.status_ok, getTheme()));
        } else {
            tv.setText("ERR");
            tv.setTextColor(getResources().getColor(R.color.status_fail, getTheme()));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
