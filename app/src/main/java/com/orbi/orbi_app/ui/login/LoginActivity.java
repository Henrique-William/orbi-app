package com.orbi.orbi_app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.SessionManager;
import com.orbi.orbi_app.data.remote.ApiClient;
import com.orbi.orbi_app.data.remote.OrbiApiService;
import com.orbi.orbi_app.data.remote.dto.LoginRequestDto;
import com.orbi.orbi_app.data.remote.dto.LoginResponseDto;
import com.orbi.orbi_app.ui.home.HomeActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private View rootLayout;
    private Chip chipServerUrl;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnOfflineMode;
    private ProgressBar progressBarLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        rootLayout = findViewById(R.id.rootLayout);
        chipServerUrl = findViewById(R.id.chipServerUrl);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnOfflineMode = findViewById(R.id.btnOfflineMode);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        // Ajuste contra sobreposição da barra de status do Android
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        updateServerChipText();
        chipServerUrl.setOnClickListener(v -> showServerConfigDialog());

        btnLogin.setOnClickListener(v -> performLogin());
        btnOfflineMode.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "carlos.silva@exemplo.com";
            handleOfflineFallback(email);
        });
    }

    private void updateServerChipText() {
        String url = sessionManager.getServerUrl().replace("http://", "").replace("/", "");
        chipServerUrl.setText("API: " + url);
    }

    private void showServerConfigDialog() {
        String[] options = {"Emulador (https://orbi-api-em6s.onrender.com/)", "Localhost (http://localhost:8080/)", "IP Customizado"};

        new AlertDialog.Builder(this)
                .setTitle("Configurar Endereço da Orbi API")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        ApiClient.setBaseUrl("https://orbi-api-em6s.onrender.com/", this);
                        updateServerChipText();
                        Toast.makeText(this, "Servidor definido: https://orbi-api-em6s.onrender.com", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        ApiClient.setBaseUrl("http://localhost:8080/", this);
                        updateServerChipText();
                        Toast.makeText(this, "Servidor definido: localhost:8080", Toast.LENGTH_SHORT).show();
                    } else {
                        showCustomUrlPrompt();
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void showCustomUrlPrompt() {
        final EditText input = new EditText(this);
        input.setText(sessionManager.getServerUrl());
        input.setHint("http://192.168.1.X:8080/");

        new AlertDialog.Builder(this)
                .setTitle("Digitar URL da Orbi API")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String customUrl = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(customUrl)) {
                        ApiClient.setBaseUrl(customUrl, this);
                        updateServerChipText();
                        Toast.makeText(this, "Nova URL salva!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.login_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Chamada real ao endpoint POST /api/auth/login
        OrbiApiService service = ApiClient.getApiService(this);
        service.login(new LoginRequestDto(email, password)).enqueue(new Callback<LoginResponseDto>() {
            @Override
            public void onResponse(Call<LoginResponseDto> call, Response<LoginResponseDto> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    String driverName = email.split("@")[0];
                    sessionManager.saveSession(email, token, driverName);
                    Toast.makeText(LoginActivity.this, "Conectado à Orbi API com sucesso!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                } else if (response.code() == 401) {
                    Toast.makeText(LoginActivity.this, "Credenciais inválidas na Orbi API (HTTP 401)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Erro Orbi API (HTTP " + response.code() + "): " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponseDto> call, Throwable t) {
                setLoading(false);
                String currentUrl = sessionManager.getServerUrl();
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Falha ao Conectar à Orbi API")
                        .setMessage("Não foi possível alcançar o servidor em:\n" + currentUrl + "\n\nDetalhes: " + t.getMessage() + "\n\nVerifique se o backend Spring Boot está em execução ou teste no Modo Offline.")
                        .setPositiveButton("Modo Offline", (d, w) -> handleOfflineFallback(email))
                        .setNegativeButton("Trocar Servidor", (d, w) -> showServerConfigDialog())
                        .setNeutralButton("OK", null)
                        .show();
            }
        });
    }

    private void handleOfflineFallback(String email) {
        String fallbackToken = "offline_jwt_token_" + System.currentTimeMillis();
        String driverName = "Carlos Silva (Motorista)";
        sessionManager.saveSession(email, fallbackToken, driverName);
        Toast.makeText(this, "Acessando em Modo Offline / Demonstração", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setText("");
            btnLogin.setEnabled(false);
            progressBarLogin.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText(R.string.btn_login);
            btnLogin.setEnabled(true);
            progressBarLogin.setVisibility(View.GONE);
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
