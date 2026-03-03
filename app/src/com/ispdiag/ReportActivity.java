package com.ispdiag;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ispdiag.model.DiagResult;
import com.ispdiag.model.EnvironmentInfo;
import com.ispdiag.util.DiagnosticInference;
import com.ispdiag.util.JsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ReportActivity - Mostra relatorio tecnico e exporta para JSON.
 * Visao padrao: relatorio tecnico legivel com inferencia automatica.
 * Alternar: troca entre relatorio tecnico e JSON bruto.
 */
public class ReportActivity extends Activity {

    private String jsonReport;
    private String techReport;
    private boolean showingJson = false;
    private TextView tvReport;
    private TextView tvSubtitle;
    private Button btnToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        List<DiagResult> results = MainActivity.lastResults;
        EnvironmentInfo envInfo = MainActivity.lastEnvInfo;

        if (results == null) {
            finish();
            return;
        }

        // Cria os relatorios
        jsonReport = JsonBuilder.buildReport(results, envInfo);
        techReport = DiagnosticInference.buildTechReport(results, envInfo);

        // Mostra na tela
        tvReport = findViewById(R.id.tv_report);
        tvSubtitle = findViewById(R.id.tv_report_subtitle);
        tvReport.setText(techReport); // Padrao: relatorio tecnico

        // Botao de alternar visao
        btnToggle = findViewById(R.id.btn_toggle_view);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showingJson = !showingJson;
                    if (showingJson) {
                        tvReport.setText(jsonReport);
                        tvReport.setTextSize(10);
                        btnToggle.setText("Tecnico");
                        if (tvSubtitle != null)
                            tvSubtitle.setText("JSON estruturado");
                    } else {
                        tvReport.setText(techReport);
                        tvReport.setTextSize(11);
                        btnToggle.setText("JSON");
                        if (tvSubtitle != null)
                            tvSubtitle.setText("Relatorio tecnico com inferencia");
                    }
                }
            });
        }

        // Botao Copiar
        Button btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toCopy = showingJson ? jsonReport : techReport;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ISP Diagnostic Report", toCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ReportActivity.this, getString(R.string.report_copied), Toast.LENGTH_SHORT).show();
            }
        });

        // Exportar JSON
        Button btnExport = findViewById(R.id.btn_export);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportJson();
            }
        });

        // Compartilhar (envia o relatorio tecnico para melhor legibilidade)
        Button btnShare = findViewById(R.id.btn_share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ISP Access Diagnostic Report");
                shareIntent.putExtra(Intent.EXTRA_TEXT, techReport);
                startActivity(Intent.createChooser(shareIntent, "Compartilhar"));
            }
        });
    }

    private void exportJson() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir = getExternalFilesDir(null);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String filename = "isp_diagnostic_" + timestamp + ".json";
            File file = new File(downloadsDir, filename);

            FileWriter writer = new FileWriter(file);
            writer.write(jsonReport);
            writer.close();

            Toast.makeText(this,
                    String.format(getString(R.string.json_saved), file.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            try {
                File appDir = getExternalFilesDir(null);
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String filename = "isp_diagnostic_" + timestamp + ".json";
                File file = new File(appDir, filename);

                FileWriter writer = new FileWriter(file);
                writer.write(jsonReport);
                writer.close();

                Toast.makeText(this,
                        String.format(getString(R.string.json_saved), file.getAbsolutePath()),
                        Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(this, "Erro ao salvar: " + e2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
