package com.ispdiag.model;

/**
 * Informacoes do ambiente de rede coletadas durante o diagnostico.
 */
public class EnvironmentInfo {
    private String connectionType; // Wi-Fi, 4G, 5G, Ethernet, Desconhecido
    private String publicIpv4;
    private boolean cgnatDetected;
    private String cgnatReason;
    private boolean ipv6Available;
    private String ipv6Address;
    private String networkOperator;
    private String wifiSsid;
    private int wifiSignalDbm;
    private String timestamp;
    private String dnsServers;
    private String gateway;
    private String localIpv4;

    public EnvironmentInfo() {
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getPublicIpv4() {
        return publicIpv4;
    }

    public void setPublicIpv4(String publicIpv4) {
        this.publicIpv4 = publicIpv4;
    }

    public boolean isCgnatDetected() {
        return cgnatDetected;
    }

    public void setCgnatDetected(boolean cgnatDetected) {
        this.cgnatDetected = cgnatDetected;
    }

    public String getCgnatReason() {
        return cgnatReason;
    }

    public void setCgnatReason(String cgnatReason) {
        this.cgnatReason = cgnatReason;
    }

    public boolean isIpv6Available() {
        return ipv6Available;
    }

    public void setIpv6Available(boolean ipv6Available) {
        this.ipv6Available = ipv6Available;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public String getNetworkOperator() {
        return networkOperator;
    }

    public void setNetworkOperator(String networkOperator) {
        this.networkOperator = networkOperator;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public int getWifiSignalDbm() {
        return wifiSignalDbm;
    }

    public void setWifiSignalDbm(int wifiSignalDbm) {
        this.wifiSignalDbm = wifiSignalDbm;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(String dnsServers) {
        this.dnsServers = dnsServers;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getLocalIpv4() {
        return localIpv4;
    }

    public void setLocalIpv4(String localIpv4) {
        this.localIpv4 = localIpv4;
    }
}
