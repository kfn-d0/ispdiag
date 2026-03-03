package com.ispdiag.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Visualizacao do grafico de latencia - mostra cada camada (DNS, TCP, TLS,
 * HTTP) como
 * linhas separadas por servico, com valores de latencia ao lado de cada barra.
 * Tema claro: texto escuro em fundo branco.
 */
public class LatencyChartView extends View {

    private static final int COLOR_DNS = 0xFF1565C0; // Dark Blue
    private static final int COLOR_TCP = 0xFF2E7D32; // Dark Green
    private static final int COLOR_TLS = 0xFFE65100; // Dark Orange
    private static final int COLOR_HTTP = 0xFFC62828; // Dark Red

    private static final String[] LAYER_NAMES = { "DNS", "TCP", "TLS", "HTTP" };
    private static final int[] LAYER_COLORS = { COLOR_DNS, COLOR_TCP, COLOR_TLS, COLOR_HTTP };

    private Paint barPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private Paint sectionPaint;
    private Paint dividerPaint;

    private List<ServiceLatency> data = new ArrayList<>();
    private float maxLayerLatency = 0;

    public LatencyChartView(Context context) {
        super(context);
        init();
    }

    public LatencyChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF6B7C93);
        textPaint.setTextSize(24f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF1A2430);
        labelPaint.setTextSize(28f);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(0xFF1A2430);
        valuePaint.setTextSize(24f);
        valuePaint.setTypeface(Typeface.MONOSPACE);

        sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(0xFF1A2430);
        sectionPaint.setTextSize(30f);
        sectionPaint.setTypeface(Typeface.DEFAULT_BOLD);

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(0x14000000);
        dividerPaint.setStrokeWidth(1f);
    }

    public void setData(List<ServiceLatency> data) {
        this.data = data;
        maxLayerLatency = 0;
        for (ServiceLatency sl : data) {
            float[] vals = { sl.dnsMs, sl.tcpMs, sl.tlsMs, sl.httpMs };
            for (float v : vals) {
                if (v > maxLayerLatency)
                    maxLayerLatency = v;
            }
        }
        if (maxLayerLatency == 0)
            maxLayerLatency = 100;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = data.size() * 232 + 70;
        height = Math.max(height, 300);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty())
            return;

        float width = getWidth();
        float layerLabelWidth = 110f;
        float valueWidth = 140f;
        float leftMargin = 20f;
        float rightMargin = 20f;
        float barHeight = 28f;
        float layerSpacing = 40f;
        float serviceGap = 24f;

        float barLeftX = leftMargin + layerLabelWidth;
        float barMaxWidth = width - barLeftX - valueWidth - rightMargin;
        if (barMaxWidth < 80)
            barMaxWidth = 80;

        float currentY = 10f;

        for (int s = 0; s < data.size(); s++) {
            ServiceLatency sl = data.get(s);

            // Nome do servico
            sectionPaint.setTextSize(28f);
            canvas.drawText(sl.serviceName, leftMargin, currentY + 22, sectionPaint);
            currentY += 36f;

            // Desenha cada camada como uma linha separada
            float[] layerValues = { sl.dnsMs, sl.tcpMs, sl.tlsMs, sl.httpMs };

            for (int l = 0; l < 4; l++) {
                float val = layerValues[l];
                float barY = currentY;

                // Rotulo da camada
                textPaint.setTextSize(22f);
                textPaint.setColor(LAYER_COLORS[l]);
                canvas.drawText(LAYER_NAMES[l], leftMargin + 8, barY + barHeight / 2 + 7, textPaint);

                // Barra (com alfa para o tema claro)
                float barWidth = (val / maxLayerLatency) * barMaxWidth;
                if (barWidth < 2 && val > 0)
                    barWidth = 2;
                barPaint.setColor(LAYER_COLORS[l]);
                barPaint.setAlpha(160);
                canvas.drawRoundRect(
                        new RectF(barLeftX, barY, barLeftX + barWidth, barY + barHeight),
                        4f, 4f, barPaint);
                barPaint.setAlpha(255);

                // Rotulo do valor
                valuePaint.setTextSize(22f);
                valuePaint.setColor(0xFF1A2430);
                String valText = (int) val + "ms";
                float valX = barLeftX + barWidth + 10;
                if (valX + valuePaint.measureText(valText) > width - rightMargin) {
                    valX = width - rightMargin - valuePaint.measureText(valText);
                }
                canvas.drawText(valText, valX, barY + barHeight / 2 + 7, valuePaint);

                currentY += layerSpacing;
            }

            // Divisor entre servicos
            if (s < data.size() - 1) {
                currentY += 4;
                canvas.drawLine(leftMargin, currentY, width - rightMargin, currentY, dividerPaint);
                currentY += serviceGap;
            }
        }

        // Legenda na parte inferior
        currentY += 16;
        float legendX = leftMargin;
        for (int l = 0; l < 4; l++) {
            barPaint.setColor(LAYER_COLORS[l]);
            canvas.drawRoundRect(
                    new RectF(legendX, currentY - 10, legendX + 20, currentY + 4),
                    3f, 3f, barPaint);
            textPaint.setTextSize(22f);
            textPaint.setColor(0xFF6B7C93);
            canvas.drawText(LAYER_NAMES[l], legendX + 26, currentY + 2, textPaint);
            legendX += 100;
        }
    }

    /**
     * Classe de dados para latencia do servico.
     */
    public static class ServiceLatency {
        public String serviceName;
        public float dnsMs;
        public float tcpMs;
        public float tlsMs;
        public float httpMs;

        public ServiceLatency(String name, float dns, float tcp, float tls, float http) {
            this.serviceName = name;
            this.dnsMs = dns;
            this.tcpMs = tcp;
            this.tlsMs = tls;
            this.httpMs = http;
        }
    }
}
