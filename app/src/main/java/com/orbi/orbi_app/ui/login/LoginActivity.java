package com.orbi.orbi_app.ui.login;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.SessionManager;
import com.orbi.orbi_app.data.remote.ApiClient;
import com.orbi.orbi_app.data.remote.OrbiApiService;
import com.orbi.orbi_app.data.remote.dto.LoginRequestDto;
import com.orbi.orbi_app.data.remote.dto.LoginResponseDto;
import com.orbi.orbi_app.ui.common.InsetsUtils;
import com.orbi.orbi_app.ui.home.HomeActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String SERVER_CLOUD = "https://orbi-api-em6s.onrender.com/";
    private static final String SERVER_LOCAL = "http://localhost:8080/";
    private static final String DEMO_EMAIL = "carlos.silva@exemplo.com";

    private View rootLayout;
    private Chip chipServerUrl;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnOfflineMode;
    private ProgressBar progressBarLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        rootLayout = findViewById(R.id.rootLayout);
        chipServerUrl = findViewById(R.id.chipServerUrl);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnOfflineMode = findViewById(R.id.btnOfflineMode);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        // Edge-to-edge: respeita barras do sistema e o teclado
        InsetsUtils.applyPadding(rootLayout, InsetsUtils.ALL, true);

        setupVersionLabel();
        updateServerChipText();
        chipServerUrl.setOnClickListener(v -> showServerConfigDialog());

        btnLogin.setOnClickListener(v -> performLogin());
        btnOfflineMode.setOnClickListener(v -> handleOfflineFallback(textOf(etEmail)));

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin();
                return true;
            }
            return false;
        });

        clearErrorOnTyping(etEmail, tilEmail);
        clearErrorOnTyping(etPassword, tilPassword);
    }

    private void setupVersionLabel() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((android.widget.TextView) findViewById(R.id.tvVersion))
                    .setText(getString(R.string.version_label, version));
        } catch (PackageManager.NameNotFoundException ignored) {
            findViewById(R.id.tvVersion).setVisibility(View.GONE);
        }
    }

    private void updateServerChipText() {
        String host = sessionManager.getServerUrl()
                .replace("https://", "")
                .replace("http://", "")
                .replaceAll("/+$", "");
        chipServerUrl.setText(getString(R.string.server_chip, host));
        chipServerUrl.setContentDescription(getString(R.string.cd_server_chip, host));
    }

    private void showServerConfigDialog() {
        String[] options = {
                getString(R.string.server_option_cloud),
                getString(R.string.server_option_localhost),
                getString(R.string.server_option_custom)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.server_dialog_title)
                .setIcon(R.drawable.ic_dns)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        applyServer(SERVER_CLOUD);
                    } else if (which == 1) {
                        applyServer(SERVER_LOCAL);
                    } else {
                        showCustomUrlPrompt();
                    }
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private void showCustomUrlPrompt() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_server_url, null);
        TextInputEditText input = content.findViewById(R.id.etServerUrl);
        input.setText(sessionManager.getServerUrl());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.server_custom_title)
                .setView(content)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String customUrl = textOf(input);
                    if (!TextUtils.isEmpty(customUrl)) {
                        applyServer(customUrl);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void applyServer(String url) {
        ApiClient.setBaseUrl(url, this);
        updateServerChipText();
        Snackbar.make(rootLayout, getString(R.string.server_saved, url), Snackbar.LENGTH_SHORT).show();
    }

    private void performLogin() {
        String email = textOf(etEmail);
        String password = textOf(etPassword);

        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.login_email_required));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.login_password_required));
            valid = false;
        }
        if (!valid) {
            (TextUtils.isEmpty(email) ? etEmail : etPassword).requestFocus();
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
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    navigateToHome();
                } else if (response.code() == 401) {
                    tilPassword.setError(getString(R.string.login_invalid_credentials));
                    etPassword.requestFocus();
                } else {
                    Snackbar.make(rootLayout, getString(R.string.login_http_error, response.code()),
                            Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponseDto> call, Throwable t) {
                setLoading(false);
                new MaterialAlertDialogBuilder(LoginActivity.this)
                        .setIcon(R.drawable.ic_cloud_off)
                        .setTitle(R.string.login_connection_failed_title)
                        .setMessage(getString(R.string.login_connection_failed_message, sessionManager.getServerUrl()))
                        .setPositiveButton(R.string.btn_demo, (d, w) -> handleOfflineFallback(email))
                        .setNegativeButton(R.string.btn_change_server, (d, w) -> showServerConfigDialog())
                        .setNeutralButton(R.string.btn_close, null)
                        .show();
            }
        });
    }

    private void handleOfflineFallback(String email) {
        if (TextUtils.isEmpty(email)) email = DEMO_EMAIL;
        String fallbackToken = "offline_jwt_token_" + System.currentTimeMillis();
        String driverName = "Carlos Silva";
        sessionManager.saveSession(email, fallbackToken, driverName, "driver_1");
        Toast.makeText(this, R.string.login_offline_toast, Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnOfflineMode.setEnabled(!isLoading);
        tilEmail.setEnabled(!isLoading);
        tilPassword.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "" : getString(R.string.btn_login));
        btnLogin.setContentDescription(isLoading ? getString(R.string.login_loading) : null);
        progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private static void clearErrorOnTyping(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
