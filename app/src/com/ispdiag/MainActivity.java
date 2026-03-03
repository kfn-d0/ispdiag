package com.ispdiag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ispdiag.diagnostic.DiagnosticRunner;
import com.ispdiag.model.DiagResult;
import com.ispdiag.model.EnvironmentInfo;
import com.ispdiag.model.ServiceTarget;
import com.ispdiag.util.DiagnosticDatabase;
import com.ispdiag.util.JsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MainActivity - Selecao de servicos e inicializacao do diagnostico.
 * Aprimorado com selecao de IPv4/IPv6 e analise de TTFB.
 */
public class MainActivity extends Activity {

    private static final int TIMEOUT_MS = 10000;

    private static final String[][] DEFAULT_SERVICES = {
            { "Google", "https://www.google.com" },
            { "YouTube", "https://www.youtube.com" },
            { "TikTok", "https://www.tiktok.com" },
            { "Twitch", "https://www.twitch.tv" },
            { "WhatsApp", "https://www.whatsapp.com" },
            { "Instagram", "https://www.instagram.com" },
            { "Steam", "https://store.steampowered.com" },
            { "Netflix", "https://www.netflix.com" },
            { "Globo", "https://www.globo.com" },
            { "Shopee", "https://shopee.com.br" },
            { "Mercado Livre", "https://www.mercadolivre.com.br" },
    };

    private LinearLayout serviceListLayout;
    private CheckBox cbSelectAll;
    private CheckBox cbAdvanced;
    private CheckBox cbIpv6;
    private EditText etCustomDomain;
    private Button btnAddDomain;
    private Button btnRun;
    private Button btnHistory;
    private View progressContainer;
    private TextView tvProgress;
    private TextView tvProgressLayer;

    private List<ServiceTarget> services = new ArrayList<>();
    private List<CheckBox> serviceCheckboxes = new ArrayList<>();
    private Handler mainHandler;

    // Referencias estaticas para passar resultados entre telas
    public static List<DiagResult> lastResults;
    public static EnvironmentInfo lastEnvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());

        serviceListLayout = findViewById(R.id.service_list);
        cbSelectAll = findViewById(R.id.cb_select_all);
        cbAdvanced = findViewById(R.id.cb_advanced);
        cbIpv6 = findViewById(R.id.cb_ipv6);
        etCustomDomain = findViewById(R.id.et_custom_domain);
        btnAddDomain = findViewById(R.id.btn_add_domain);
        btnRun = findViewById(R.id.btn_run);
        btnHistory = findViewById(R.id.btn_history);
        progressContainer = findViewById(R.id.progress_container);
        tvProgress = findViewById(R.id.tv_progress);
        tvProgressLayer = findViewById(R.id.tv_progress_layer);

        // Inicializa servicos padrao
        for (String[] svc : DEFAULT_SERVICES) {
            services.add(new ServiceTarget(svc[0], svc[1]));
        }
        buildServiceList();

        // Selecionar todos
        cbSelectAll.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                for (int i = 0; i < serviceCheckboxes.size(); i++) {
                    serviceCheckboxes.get(i).setChecked(isChecked);
                    services.get(i).setSelected(isChecked);
                }
            }
        });

        // Adicionar dominio customizado
        btnAddDomain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCustomDomain();
            }
        });

        // Iniciar diagnostico
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDiagnostic();
            }
        });

        // Historico
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    private void buildServiceList() {
        serviceListLayout.removeAllViews();
        serviceCheckboxes.clear();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < services.size(); i++) {
            ServiceTarget svc = services.get(i);
            View itemView = inflater.inflate(R.layout.item_service, serviceListLayout, false);

            CheckBox cb = itemView.findViewById(R.id.cb_service);
            TextView tvName = itemView.findViewById(R.id.tv_service_name);
            TextView tvUrl = itemView.findViewById(R.id.tv_service_url);

            tvName.setText(svc.getName());
            String displayUrl = svc.getUrl().replace("https://", "").replace("http://", "");
            if (displayUrl.startsWith("www."))
                displayUrl = displayUrl.substring(4);
            tvUrl.setText(displayUrl);
            cb.setChecked(svc.isSelected());

            final int index = i;
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    services.get(index).setSelected(isChecked);
                }
            });

            serviceCheckboxes.add(cb);
            serviceListLayout.addView(itemView);
        }
    }

    private void addCustomDomain() {
        String domain = etCustomDomain.getText().toString().trim();
        if (domain.isEmpty())
            return;

        String url;
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            url = "https://" + domain;
        } else {
            url = domain;
        }

        String name = domain.replace("https://", "").replace("http://", "");
        if (name.startsWith("www."))
            name = name.substring(4);
        if (name.contains("/"))
            name = name.substring(0, name.indexOf("/"));

        ServiceTarget newService = new ServiceTarget(name, url);
        newService.setSelected(true);
        services.add(newService);
        buildServiceList();

        etCustomDomain.setText("");
        Toast.makeText(this, name + " adicionado", Toast.LENGTH_SHORT).show();
    }

    private void runDiagnostic() {
        List<ServiceTarget> selected = new ArrayList<>();
        for (ServiceTarget svc : services) {
            if (svc.isSelected()) {
                selected.add(svc);
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_selection), Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progresso
        btnRun.setEnabled(false);
        btnRun.setAlpha(0.5f);
        btnHistory.setEnabled(false);
        progressContainer.setVisibility(View.VISIBLE);
        tvProgress.setText(getString(R.string.label_running));
        if (tvProgressLayer != null)
            tvProgressLayer.setText("");

        boolean advancedEnabled = cbAdvanced != null && cbAdvanced.isChecked();
        boolean useIpv6 = cbIpv6 != null && cbIpv6.isChecked();

        DiagnosticRunner runner = new DiagnosticRunner(this, TIMEOUT_MS);
        runner.setRunAdvanced(advancedEnabled);
        runner.setForceIpv6(useIpv6);

        new Thread(new Runnable() {
            @Override
            public void run() {
                runner.runDiagnostics(selected, new DiagnosticRunner.ProgressCallback() {
                    @Override
                    public void onServiceStart(String serviceName, int index, int total) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvProgress.setText(serviceName + " (" + (index + 1) + "/" + total + ")");
                                if (tvProgressLayer != null)
                                    tvProgressLayer.setText("Iniciando...");
                            }
                        });
                    }

                    @Override
                    public void onLayerStart(String serviceName, String layerName, int index, int total) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (tvProgressLayer != null) {
                                    tvProgressLayer.setText(layerName);
                                }
                            }
                        });
                    }

                    @Override
                    public void onServiceComplete(DiagResult result, int index, int total) {
                    }

                    @Override
                    public void onAllComplete(List<DiagResult> results, EnvironmentInfo envInfo) {
                        // Salvar no historico
                        saveToHistory(results, envInfo);

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnRun.setEnabled(true);
                                btnRun.setAlpha(1.0f);
                                btnHistory.setEnabled(true);
                                progressContainer.setVisibility(View.GONE);

                                lastResults = results;
                                lastEnvInfo = envInfo;

                                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnRun.setEnabled(true);
                                btnRun.setAlpha(1.0f);
                                btnHistory.setEnabled(true);
                                progressContainer.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void saveToHistory(List<DiagResult> results, EnvironmentInfo envInfo) {
        try {
            DiagnosticDatabase db = new DiagnosticDatabase(this);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());
            String timestamp = sdf.format(new Date());

            int ok = 0, partial = 0, fail = 0;
            StringBuilder serviceNames = new StringBuilder();
            for (DiagResult r : results) {
                if (serviceNames.length() > 0)
                    serviceNames.append(", ");
                serviceNames.append(r.getTarget().getName());
                switch (r.getOverallStatus()) {
                    case OK:
                        ok++;
                        break;
                    case PARTIAL:
                        partial++;
                        break;
                    case FAIL:
                        fail++;
                        break;
                }
            }

            String jsonReport = JsonBuilder.buildReport(results, envInfo);

            db.saveReport(timestamp, results.size(), ok, partial, fail,
                    serviceNames.toString(), jsonReport);
            db.close();
        } catch (Exception e) {
            // Ignora erro se falhar ao salvar no historico
        }
    }
}
