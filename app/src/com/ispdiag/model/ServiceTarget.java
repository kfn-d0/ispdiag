package com.ispdiag.model;

/**
 * Representa um alvo de servico para testes de diagnostico.
 */
public class ServiceTarget {
    private String name;
    private String url;
    private String host;
    private boolean selected;

    public ServiceTarget(String name, String url) {
        this.name = name;
        this.url = url;
        this.host = extractHost(url);
        this.selected = false;
    }

    private String extractHost(String url) {
        String h = url;
        if (h.contains("://")) {
            h = h.substring(h.indexOf("://") + 3);
        }
        if (h.contains("/")) {
            h = h.substring(0, h.indexOf("/"));
        }
        if (h.contains(":")) {
            h = h.substring(0, h.indexOf(":"));
        }
        return h;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
