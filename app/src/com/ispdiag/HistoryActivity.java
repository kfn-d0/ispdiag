package com.ispdiag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ispdiag.util.DiagnosticDatabase;

import java.util.List;

/**
 * HistoryActivity - Mostra os resultados de diagnosticos passados com data e
 * hora.
 */
public class HistoryActivity extends Activity {

    private DiagnosticDatabase db;
    private LinearLayout historyList;
    private TextView tvEmptyHistory;
    private TextView tvHistoryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DiagnosticDatabase(this);
        historyList = findViewById(R.id.history_list);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);
        tvHistoryCount = findViewById(R.id.tv_history_count);

        // Botao para limpar historico
        Button btnClear = findViewById(R.id.btn_clear_history);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Limpar Historico")
                        .setMessage("Tem certeza que deseja apagar todo o historico?")
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.clearHistory();
                                loadHistory();
                                Toast.makeText(HistoryActivity.this, "Historico limpo", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Nao", null)
                        .show();
            }
        });

        // Botao voltar
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Link do GitHub
        TextView tvGithub = findViewById(R.id.tv_github_link);
        if (tvGithub != null) {
            tvGithub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kfn-d0"));
                    startActivity(intent);
                }
            });
        }

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        historyList.removeAllViews();

        List<DiagnosticDatabase.HistoryEntry> entries = db.getAllHistory();

        if (entries.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            tvHistoryCount.setText("0 diagnosticos salvos");
            return;
        }

        tvEmptyHistory.setVisibility(View.GONE);
        tvHistoryCount.setText(entries.size() + " diagnostico(s) salvo(s)");

        LayoutInflater inflater = LayoutInflater.from(this);

        for (final DiagnosticDatabase.HistoryEntry entry : entries) {
            View itemView = inflater.inflate(R.layout.item_history, historyList, false);

            TextView tvTimestamp = itemView.findViewById(R.id.tv_history_timestamp);
            TextView tvServices = itemView.findViewById(R.id.tv_history_services);
            TextView tvOk = itemView.findViewById(R.id.tv_history_ok);
            TextView tvPartial = itemView.findViewById(R.id.tv_history_partial);
            TextView tvFail = itemView.findViewById(R.id.tv_history_fail);
            Button btnView = itemView.findViewById(R.id.btn_history_view);

            tvTimestamp.setText(entry.timestamp);
            tvServices.setText(entry.servicesSummary);
            tvOk.setText("OK: " + entry.okCount);
            tvPartial.setText("Parcial: " + entry.partialCount);
            tvFail.setText("Falha: " + entry.failCount);

            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String report = db.getReportById(entry.id);
                    if (report != null) {
                        // Mostra em um pop-up com a opcao de copiar
                        new AlertDialog.Builder(HistoryActivity.this)
                                .setTitle("Relatorio - " + entry.timestamp)
                                .setMessage(report)
                                .setPositiveButton("Copiar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(
                                                Context.CLIPBOARD_SERVICE);
                                        clipboard.setPrimaryClip(
                                                ClipData.newPlainText("Report", report));
                                        Toast.makeText(HistoryActivity.this,
                                                "Copiado!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Fechar", null)
                                .show();
                    }
                }
            });

            historyList.addView(itemView);
        }
    }
}
