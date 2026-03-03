package com.ispdiag.diagnostic;

import com.ispdiag.model.EnvironmentInfo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Coleta informacoes do ambiente de rede.
 * Sem dados pessoais, sem historico de navegacao.
 */
public class EnvironmentCollector {

    private final Context context;

    public EnvironmentCollector(Context context) {
        this.context = context;
    }

    /**
     * Coleta todas as informacoes do ambiente.
     */
    public EnvironmentInfo collect() {
        EnvironmentInfo info = new EnvironmentInfo();

        // Data e hora
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        info.setTimestamp(sdf.format(new Date()));

        // Tipo de conexao
        info.setConnectionType(getConnectionType());

        // IPv4 local
        String localIp = getLocalIpAddress();
        info.setLocalIpv4(localIp != null ? localIp : "N/A");

        // IPv4 publico (usa APIs apenas IPv4 primeiro!)
        info.setPublicIpv4(getPublicIpv4());

        // Deteccao de CGNAT
        detectCgnat(info);

        // Disponibilidade e endereco IPv6
        detectIpv6(info);

        // Servidores DNS
        info.setDnsServers(getDnsServers());

        // Gateway
        info.setGateway(getGateway());

        return info;
    }

    private String getConnectionType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
                return "Unknown";

            Network network = cm.getActiveNetwork();
            if (network == null)
                return "No Connection";

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null)
                return "Unknown";

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return getCellularGeneration();
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            } else {
                return "Other";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getCellularGeneration() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null)
                return "Mobile";

            int type = tm.getDataNetworkType();
            switch (type) {
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4G (LTE)";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G (NR)";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G (HSPA)";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return "3G (UMTS)";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "2G (EDGE)";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "2G (GPRS)";
                default:
                    return "Mobile";
            }
        } catch (SecurityException e) {
            return "Mobile";
        } catch (Exception e) {
            return "Mobile";
        }
    }

    /**
     * Obtem o endereco IPv4 publico usando APIs apenas IPv4.
     * Usa api.ipify.org (apenas IPv4) como primario, ifconfig.me como fallback.
     */
    private String getPublicIpv4() {
        // Tenta API apenas IPv4 primeiro
        String ip = fetchUrl("https://api.ipify.org");
        if (ip != null && isValidIpv4(ip)) {
            return ip;
        }

        // Fallback: icanhazip (pode retornar IPv6 em pilha dupla)
        ip = fetchUrl("https://ipv4.icanhazip.com");
        if (ip != null && isValidIpv4(ip)) {
            return ip;
        }

        // Ultimo recurso: ifconfig.me
        ip = fetchUrl("https://ifconfig.me/ip");
        if (ip != null) {
            // Se retornou IPv6, nota isso mas ainda retorna com um marcador
            if (ip.contains(":")) {
                return "N/A (IPv6-only: " + ip + ")";
            }
            return ip;
        }

        return "N/A";
    }

    private String fetchUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "ISPDiagnostic/1.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            reader.close();
            conn.disconnect();

            if (line != null) {
                return line.trim();
            }
        } catch (Exception e) {
            // ignorar, chamador lida com nulo
        }
        return null;
    }

    private boolean isValidIpv4(String ip) {
        if (ip == null)
            return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4)
            return false;
        try {
            for (String part : parts) {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255)
                    return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Detecta provavel CGNAT verificando se o IP publico esta no intervalo da RFC
     * 6598
     * (100.64.0.0/10)
     */
    private void detectCgnat(EnvironmentInfo info) {
        String publicIp = info.getPublicIpv4();
        if (publicIp == null || publicIp.startsWith("N/A")) {
            info.setCgnatDetected(false);
            info.setCgnatReason("Unable to determine (no public IP)");
            return;
        }

        // Verifica intervalo da RFC 6598
        if (isInCgnatRange(publicIp)) {
            info.setCgnatDetected(true);
            info.setCgnatReason("Public IP in RFC 6598 range (100.64.0.0/10)");
            return;
        }

        // Verifica se o IP local e privado E o IP publico e diferente
        String localIp = info.getLocalIpv4();
        if (localIp != null && !localIp.equals("N/A") && !localIp.equals(publicIp) && isPrivateIp(localIp)) {
            info.setCgnatDetected(false);
            info.setCgnatReason(
                    "NAT detected (local: " + localIp + ", public: " + publicIp + "). CGNAT not confirmed.");
        } else {
            info.setCgnatDetected(false);
            info.setCgnatReason("No CGNAT indicators detected");
        }
    }

    private boolean isInCgnatRange(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4)
                return false;
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 100 && second >= 64 && second <= 127;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPrivateIp(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4)
                return false;
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (first == 10)
                return true;
            if (first == 172 && second >= 16 && second <= 31)
                return true;
            if (first == 192 && second == 168)
                return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // ignorar
        }
        return null;
    }

    private void detectIpv6(EnvironmentInfo info) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address && !addr.isLinkLocalAddress()) {
                        info.setIpv6Available(true);
                        info.setIpv6Address(addr.getHostAddress());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // ignorar
        }
        info.setIpv6Available(false);
        info.setIpv6Address("N/A");
    }

    /**
     * Obtem os servidores DNS das LinkProperties do Android.
     */
    private String getDnsServers() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
                return "N/A";

            Network network = cm.getActiveNetwork();
            if (network == null)
                return "N/A";

            LinkProperties linkProps = cm.getLinkProperties(network);
            if (linkProps == null)
                return "N/A";

            List<InetAddress> dnsServers = linkProps.getDnsServers();
            if (dnsServers == null || dnsServers.isEmpty())
                return "N/A";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dnsServers.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(dnsServers.get(i).getHostAddress());
            }
            return sb.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Obtem o gateway padrao das rotas em LinkProperties.
     * Retorna apenas gateway IPv4.
     */
    private String getGateway() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
                return "N/A";

            Network network = cm.getActiveNetwork();
            if (network == null)
                return "N/A";

            LinkProperties linkProps = cm.getLinkProperties(network);
            if (linkProps == null)
                return "N/A";

            List<android.net.RouteInfo> routes = linkProps.getRoutes();
            if (routes == null)
                return "N/A";

            for (android.net.RouteInfo route : routes) {
                if (route.isDefaultRoute()) {
                    InetAddress gw = route.getGateway();
                    if (gw != null && gw instanceof Inet4Address) {
                        return gw.getHostAddress();
                    }
                }
            }

            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }
}
