package com.ispdiag.diagnostic;

import com.ispdiag.model.TcpResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;

/**
 * Teste de conexao TCP.
 * Testa a conexao TCP na porta especificada, mede o tempo para estabelecer,
 * identifica condicoes de timeout, RST ou inalcansavel.
 */
public class TcpTest {

    private final int timeoutMs;

    public TcpTest(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Testa conexao TCP para o host:porta.
     */
    public TcpResult execute(String host, int port) {
        TcpResult result = new TcpResult(port);
        long startTime = System.currentTimeMillis();

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long elapsed = System.currentTimeMillis() - startTime;

            result.setSuccess(true);
            result.setConnectTimeMs(elapsed);

        } catch (SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setConnectTimeMs(elapsed);
            result.setErrorType("TIMEOUT");
            result.setErrorMessage("Connection timed out after " + timeoutMs + "ms");

        } catch (ConnectException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setConnectTimeMs(elapsed);
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("refused")) {
                result.setErrorType("RST");
                result.setErrorMessage("Connection refused (RST)");
            } else {
                result.setErrorType("ERROR");
                result.setErrorMessage(msg);
            }

        } catch (NoRouteToHostException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setConnectTimeMs(elapsed);
            result.setErrorType("UNREACHABLE");
            result.setErrorMessage("No route to host");

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setConnectTimeMs(elapsed);
            result.setErrorType("ERROR");
            result.setErrorMessage(e.getMessage());

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        return result;
    }
}
