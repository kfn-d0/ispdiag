package com.ispdiag.diagnostic;

import android.content.Context;

import com.ispdiag.model.AdvancedResult;
import com.ispdiag.model.DiagResult;
import com.ispdiag.model.DnsComparisonEntry;
import com.ispdiag.model.DnsResult;
import com.ispdiag.model.EnvironmentInfo;
import com.ispdiag.model.HttpResult;
import com.ispdiag.model.ServiceTarget;
import com.ispdiag.model.TcpResult;
import com.ispdiag.model.TlsResult;
import com.ispdiag.model.TracerouteResult;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Orquestra todos os testes de diagnostico para uma lista de alvos de servico.
 * Aprimorado com selecao de modo IPv4/IPv6 e analise de TTFB.
 */
public class DiagnosticRunner {

    /**
     * Callback aprimorado com progresso por camada.
     */
    public interface ProgressCallback {
        void onServiceStart(String serviceName, int index, int total);

        void onLayerStart(String serviceName, String layerName, int index, int total);

        void onServiceComplete(DiagResult result, int index, int total);

        void onAllComplete(List<DiagResult> results, EnvironmentInfo envInfo);

        void onError(String message);
    }

    private final Context context;
    private final int timeoutMs;
    private final DnsTest dnsTest;
    private final TcpTest tcpTest;
    private final TlsTest tlsTest;
    private final HttpTest httpTest;
    private final TracerouteTest tracerouteTest;
    private final DnsComparisonTest dnsComparisonTest;
    private final AdvancedTests advancedTests;
    private final EnvironmentCollector envCollector;
    private boolean runAdvanced = false;
    private boolean forceIpv6 = false;

    public DiagnosticRunner(Context context, int timeoutMs) {
        this.context = context;
        this.timeoutMs = timeoutMs;
        this.dnsTest = new DnsTest(timeoutMs);
        this.tcpTest = new TcpTest(timeoutMs);
        this.tlsTest = new TlsTest(timeoutMs);
        this.httpTest = new HttpTest(timeoutMs);
        this.tracerouteTest = new TracerouteTest();
        this.dnsComparisonTest = new DnsComparisonTest(context);
        this.advancedTests = new AdvancedTests(timeoutMs);
        this.envCollector = new EnvironmentCollector(context);
    }

    public void setRunAdvanced(boolean runAdvanced) {
        this.runAdvanced = runAdvanced;
    }

    public void setForceIpv6(boolean forceIpv6) {
        this.forceIpv6 = forceIpv6;
    }

    /**
     * Executa o diagnostico em todos os alvos selecionados.
     * Este metodo bloqueia - chame a partir de uma thread em segundo plano.
     */
    public void runDiagnostics(List<ServiceTarget> targets, ProgressCallback callback) {
        // Define a preferencia IPv4/IPv6 a nivel da JVM
        String previousPref = System.getProperty("java.net.preferIPv6Addresses", "false");
        String previousPrefV4 = System.getProperty("java.net.preferIPv4Stack", "false");
        if (forceIpv6) {
            System.setProperty("java.net.preferIPv6Addresses", "true");
            System.setProperty("java.net.preferIPv4Stack", "false");
        } else {
            System.setProperty("java.net.preferIPv6Addresses", "false");
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        try {
            // Coleta informacoes do ambiente primeiro
            EnvironmentInfo envInfo = envCollector.collect();

            List<DiagResult> results = new ArrayList<>();
            int total = targets.size();

            for (int i = 0; i < total; i++) {
                ServiceTarget target = targets.get(i);
                callback.onServiceStart(target.getName(), i, total);

                DiagResult result = runSingleDiagnostic(target, callback, i, total);
                results.add(result);

                callback.onServiceComplete(result, i, total);
            }

            callback.onAllComplete(results, envInfo);

        } catch (Exception e) {
            callback.onError("Diagnostic failed: " + e.getMessage());
        } finally {
            // Restaura as configuracoes anteriores
            System.setProperty("java.net.preferIPv6Addresses", previousPref);
            System.setProperty("java.net.preferIPv4Stack", previousPrefV4);
        }
    }

    /**
     * Resolve host para IP especifico com base na preferencia IPv4/IPv6.
     */
    private String resolveForProtocol(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (forceIpv6 && addr instanceof Inet6Address) {
                    return addr.getHostAddress();
                } else if (!forceIpv6 && addr instanceof Inet4Address) {
                    return addr.getHostAddress();
                }
            }
            // fallback: retorna o primeiro disponivel
            if (addresses.length > 0) {
                return addresses[0].getHostAddress();
            }
        } catch (Exception e) {
            // ignorar erro
        }
        return null;
    }

    /**
     * Executa todas as camadas de teste para um unico servico.
     */
    private DiagResult runSingleDiagnostic(ServiceTarget target,
            ProgressCallback callback,
            int index, int total) {
        DiagResult result = new DiagResult(target);
        long totalStart = System.currentTimeMillis();

        // Data e hora
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        result.setTimestamp(sdf.format(new Date()));

        String host = target.getHost();
        String url = target.getUrl();

        // Camada 1: DNS
        callback.onLayerStart(target.getName(), "DNS", index, total);
        DnsResult dns = dnsTest.execute(host);
        result.setDnsResult(dns);

        // Prossegue apenas se o DNS funcionou
        if (dns.isSuccess()) {
            // Resolve para IP especifico para o protocolo selecionado
            String resolvedIp = resolveForProtocol(host);
            String connectHost = (resolvedIp != null) ? resolvedIp : host;

            // Camada 2: TCP porta 80
            callback.onLayerStart(target.getName(), "TCP:80", index, total);
            TcpResult tcp80 = tcpTest.execute(connectHost, 80);
            result.setTcpPort80(tcp80);

            // Camada 2: TCP porta 443
            callback.onLayerStart(target.getName(), "TCP:443", index, total);
            TcpResult tcp443 = tcpTest.execute(connectHost, 443);
            result.setTcpPort443(tcp443);

            // Camada 3: TLS (apenas se TCP 443 funcionou)
            if (tcp443.isSuccess()) {
                callback.onLayerStart(target.getName(), "TLS", index, total);
                TlsResult tls = tlsTest.execute(host);
                result.setTlsResult(tls);

                // Camada 4: HTTP com analise de TTFB (apenas se TLS funcionou)
                if (tls.isSuccess()) {
                    callback.onLayerStart(target.getName(), "HTTP (3x TTFB)", index, total);
                    HttpResult http = httpTest.execute(url);
                    result.setHttpResult(http);
                }
            }

            // Comparacao de DNS
            callback.onLayerStart(target.getName(), "DNS Compare", index, total);
            List<DnsComparisonEntry> dnsComp = dnsComparisonTest.compare(host);
            result.setDnsComparison(dnsComp);

            // Traceroute (simplificado)
            callback.onLayerStart(target.getName(), "Traceroute", index, total);
            TracerouteResult traceroute = tracerouteTest.execute(host);
            result.setTracerouteResult(traceroute);

            // Testes avancados (se ativado)
            if (runAdvanced) {
                callback.onLayerStart(target.getName(), "Advanced", index, total);
                AdvancedResult advanced = advancedTests.runAll(host);
                result.setAdvancedResult(advanced);
            }
        }

        long totalEnd = System.currentTimeMillis();
        result.setTotalTimeMs(totalEnd - totalStart);

        // Calcula o status geral
        result.computeOverallStatus();

        return result;
    }
}
