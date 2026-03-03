package com.ispdiag.diagnostic;

import com.ispdiag.model.DnsComparisonEntry;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DNS Comparison Test - queries multiple DNS resolvers and compares results.
 * Fixed: System DNS now uses raw UDP query instead of cached InetAddress.
 */
public class DnsComparisonTest {

    private static final int DNS_PORT = 53;
    private static final int TIMEOUT_MS = 3000;

    private static final String[][] RESOLVERS = {
            { "Google", "8.8.8.8" },
            { "Google 2", "8.8.4.4" },
            { "Cloudflare", "1.1.1.1" },
            { "Cloudflare 2", "1.0.0.1" },
            { "OpenDNS", "208.67.222.222" },
            { "Quad9", "9.9.9.9" },
    };

    private Context context;

    public DnsComparisonTest() {
        this.context = null;
    }

    public DnsComparisonTest(Context ctx) {
        this.context = ctx;
    }

    public List<DnsComparisonEntry> compare(String host) {
        List<DnsComparisonEntry> results = new ArrayList<>();

        // Obtem os enderecos dos servidores DNS do sistema
        List<String> systemDnsServers = getSystemDnsServers();

        // Consulta o DNS do sistema usando UDP bruto (nao InetAddress que usa cache)
        if (!systemDnsServers.isEmpty()) {
            // Usa o primeiro servidor DNS IPv4 do sistema
            String systemDnsIp = systemDnsServers.get(0);
            DnsComparisonEntry systemEntry = new DnsComparisonEntry("Sistema", systemDnsIp);
            try {
                long queryStart = System.currentTimeMillis();
                String ip = queryDns(host, systemDnsIp);
                systemEntry.setResponseTimeMs(System.currentTimeMillis() - queryStart);
                if (ip != null) {
                    systemEntry.setSuccess(true);
                    systemEntry.setResolvedIp(ip);
                } else {
                    systemEntry.setSuccess(false);
                    systemEntry.setErrorMessage("No response");
                }
            } catch (Exception e) {
                systemEntry.setSuccess(false);
                systemEntry.setErrorMessage(e.getMessage());
            }
            results.add(systemEntry);
        } else {
            // Fallback: usa InetAddress mas com a ressalva de que pode estar em cache
            DnsComparisonEntry systemEntry = new DnsComparisonEntry("Sistema", "default");
            long start = System.currentTimeMillis();
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                systemEntry.setResponseTimeMs(System.currentTimeMillis() - start);
                systemEntry.setSuccess(true);
                if (addrs.length > 0) {
                    systemEntry.setResolvedIp(addrs[0].getHostAddress());
                }
            } catch (Exception e) {
                systemEntry.setResponseTimeMs(System.currentTimeMillis() - start);
                systemEntry.setSuccess(false);
                systemEntry.setErrorMessage(e.getMessage());
            }
            results.add(systemEntry);
        }

        // Consulta cada resolvedor
        for (String[] resolver : RESOLVERS) {
            DnsComparisonEntry entry = new DnsComparisonEntry(resolver[0], resolver[1]);
            try {
                long queryStart = System.currentTimeMillis();
                String ip = queryDns(host, resolver[1]);
                entry.setResponseTimeMs(System.currentTimeMillis() - queryStart);
                if (ip != null) {
                    entry.setSuccess(true);
                    entry.setResolvedIp(ip);
                } else {
                    entry.setSuccess(false);
                    entry.setErrorMessage("No response");
                }
            } catch (Exception e) {
                entry.setSuccess(false);
                entry.setErrorMessage(e.getMessage());
            }
            results.add(entry);
        }

        return results;
    }

    /**
     * Get system DNS server IPs from LinkProperties.
     */
    private List<String> getSystemDnsServers() {
        List<String> servers = new ArrayList<>();
        if (context == null)
            return servers;

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null)
                return servers;

            Network network = cm.getActiveNetwork();
            if (network == null)
                return servers;

            LinkProperties linkProps = cm.getLinkProperties(network);
            if (linkProps == null)
                return servers;

            List<InetAddress> dnsServers = linkProps.getDnsServers();
            if (dnsServers == null)
                return servers;

            for (InetAddress addr : dnsServers) {
                if (addr instanceof Inet4Address) {
                    servers.add(addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            // ignorar
        }
        return servers;
    }

    /**
     * Simple DNS A record query using raw UDP socket.
     */
    private String queryDns(String host, String dnsServer) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT_MS);

        byte[] query = buildDnsQuery(host);
        InetAddress serverAddr = InetAddress.getByName(dnsServer);
        DatagramPacket queryPacket = new DatagramPacket(query, query.length, serverAddr, DNS_PORT);
        socket.send(queryPacket);

        byte[] response = new byte[512];
        DatagramPacket responsePacket = new DatagramPacket(response, response.length);
        socket.receive(responsePacket);
        socket.close();

        return parseDnsResponse(response, responsePacket.getLength());
    }

    private byte[] buildDnsQuery(String host) {
        // Constroi uma consulta simples de registro DNS A
        Random rand = new Random();
        int txId = rand.nextInt(65535);

        String[] labels = host.split("\\.");
        int queryLen = 12; // cabecalho
        for (String label : labels) {
            queryLen += 1 + label.length();
        }
        queryLen += 1 + 4; // terminador nulo + tipo + classe

        byte[] query = new byte[queryLen];
        ByteBuffer buf = ByteBuffer.wrap(query);

        // Cabecalho
        buf.putShort((short) txId); // Transaction ID
        buf.putShort((short) 0x0100); // Flags: consulta padrao, recursao desejada
        buf.putShort((short) 1); // Questions: 1
        buf.putShort((short) 0); // RRs de resposta
        buf.putShort((short) 0); // Authority RRs
        buf.putShort((short) 0); // RRs adicionais

        // Pergunta
        for (String label : labels) {
            buf.put((byte) label.length());
            buf.put(label.getBytes());
        }
        buf.put((byte) 0); // Fim do dominio
        buf.putShort((short) 1); // Type A
        buf.putShort((short) 1); // Classe IN

        return query;
    }

    private String parseDnsResponse(byte[] data, int length) {
        if (length < 12)
            return null;

        ByteBuffer buf = ByteBuffer.wrap(data, 0, length);
        buf.getShort(); // TX ID
        short flags = buf.getShort();
        int qdCount = buf.getShort() & 0xFFFF;
        int anCount = buf.getShort() & 0xFFFF;
        buf.getShort(); // NS count
        buf.getShort(); // AR count

        // Verifica se a resposta contem respostas
        if (anCount == 0)
            return null;

        // Pula a secao de perguntas
        for (int i = 0; i < qdCount; i++) {
            skipDnsName(buf);
            buf.getShort(); // Type
            buf.getShort(); // Classe
        }

        // Analisa a primeira resposta record A
        for (int i = 0; i < anCount; i++) {
            skipDnsName(buf);
            short type = buf.getShort();
            buf.getShort(); // Classe
            buf.getInt(); // TTL
            short rdLen = buf.getShort();

            if (type == 1 && rdLen == 4) { // Registro A
                int a = buf.get() & 0xFF;
                int b = buf.get() & 0xFF;
                int c = buf.get() & 0xFF;
                int d = buf.get() & 0xFF;
                return a + "." + b + "." + c + "." + d;
            } else {
                // Pula este registro de dados
                for (int j = 0; j < rdLen; j++)
                    buf.get();
            }
        }

        return null;
    }

    private void skipDnsName(ByteBuffer buf) {
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == 0)
                break;
            if ((b & 0xC0) == 0xC0) {
                buf.get(); // pula byte do ponteiro
                break;
            } else {
                for (int i = 0; i < (b & 0x3F); i++)
                    buf.get();
            }
        }
    }
}
