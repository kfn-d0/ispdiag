package com.ispdiag.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado completo do traceroute para um host.
 */
public class TracerouteResult {
    private String targetHost;
    private String targetIp;
    private boolean completed;
    private int totalHops;
    private long totalTimeMs;
    private List<TracerouteHop> hops;

    public TracerouteResult(String targetHost) {
        this.targetHost = targetHost;
        this.hops = new ArrayList<>();
    }

    public String getTargetHost() {
        return targetHost;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getTotalHops() {
        return totalHops;
    }

    public void setTotalHops(int totalHops) {
        this.totalHops = totalHops;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public List<TracerouteHop> getHops() {
        return hops;
    }

    public void addHop(TracerouteHop hop) {
        this.hops.add(hop);
    }
}
