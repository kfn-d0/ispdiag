package com.ispdiag.model;

/**
 * Representa um unico salto em um traceroute.
 */
public class TracerouteHop {
    private int hopNumber;
    private String address;
    private String hostname;
    private long rttMs;
    private boolean timeout;

    public TracerouteHop(int hopNumber) {
        this.hopNumber = hopNumber;
        this.timeout = true;
        this.address = "*";
        this.hostname = "*";
        this.rttMs = -1;
    }

    public int getHopNumber() {
        return hopNumber;
    }

    public void setHopNumber(int hopNumber) {
        this.hopNumber = hopNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public long getRttMs() {
        return rttMs;
    }

    public void setRttMs(long rttMs) {
        this.rttMs = rttMs;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void setTimeout(boolean timeout) {
        this.timeout = timeout;
    }
}
