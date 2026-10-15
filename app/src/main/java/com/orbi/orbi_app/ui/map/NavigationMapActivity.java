package com.orbi.orbi_app.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.ui.home.HomeActivity;

import java.util.Locale;

public class NavigationMapActivity extends AppCompatActivity {

    private View rootLayout;
    private View cardTopBanner;
    private View cardBottomControls;
    private RealTimeMapView mapView;
    private ImageButton btnCloseNav;
    private TextView tvNavSequence;
    private TextView tvNavRecipient;
    private TextView tvNavAddress;
    private TextView tvNavEta;
    private FloatingActionButton fabRecenter;

    private MaterialButton btnCancelStop;
    private MaterialButton btnSkipStop;
    private MaterialButton btnCompleteStop;

    private RouteRepository repository;
    private String routeId;
    private Route currentRoute;
    private Waypoint currentWaypoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_map);

        repository = RouteRepository.getInstance();
        routeId = getIntent().getStringExtra("EXTRA_ROUTE_ID");

        rootLayout = findViewById(R.id.rootLayout);
        cardTopBanner = findViewById(R.id.cardTopBanner);
        cardBottomControls = findViewById(R.id.cardBottomControls);
        mapView = findViewById(R.id.mapView);
        btnCloseNav = findViewById(R.id.btnCloseNav);
        tvNavSequence = findViewById(R.id.tvNavSequence);
        tvNavRecipient = findViewById(R.id.tvNavRecipient);
        tvNavAddress = findViewById(R.id.tvNavAddress);
        tvNavEta = findViewById(R.id.tvNavEta);
        fabRecenter = findViewById(R.id.fabRecenter);

        btnCancelStop = findViewById(R.id.btnCancelStop);
        btnSkipStop = findViewById(R.id.btnSkipStop);
        btnCompleteStop = findViewById(R.id.btnCompleteStop);

        // Ajusta margens dinamicamente para respeitar a barra de status e a barra de navegação do Android
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (cardTopBanner.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams topParams = (ViewGroup.MarginLayoutParams) cardTopBanner.getLayoutParams();
                topParams.topMargin = systemBars.top + 16;
                cardTopBanner.setLayoutParams(topParams);
            }

            if (cardBottomControls.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams bottomParams = (ViewGroup.MarginLayoutParams) cardBottomControls.getLayoutParams();
                bottomParams.bottomMargin = systemBars.bottom + 16;
                cardBottomControls.setLayoutParams(bottomParams);
            }

            return insets;
        });

        btnCloseNav.setOnClickListener(v -> finish());
        fabRecenter.setOnClickListener(v -> mapView.recenter());

        btnCompleteStop.setOnClickListener(v -> handleCompleteStop());
        btnSkipStop.setOnClickListener(v -> showJustificationDialog(ActionType.SKIP));
        btnCancelStop.setOnClickListener(v -> showJustificationDialog(ActionType.CANCEL));

        loadCurrentNavigationTarget();
    }

    private void loadCurrentNavigationTarget() {
        if (routeId == null) {
            finish();
            return;
        }

        currentRoute = repository.getRouteById(routeId);
        if (currentRoute == null) {
            finish();
            return;
        }

        currentWaypoint = currentRoute.getNextPendingWaypoint();

        if (currentWaypoint == null) {
            showAllStopsHandledDialog();
            return;
        }

        // Atualiza painel superior minimalista
        tvNavSequence.setText(currentWaypoint.getFormattedSequence());
        tvNavRecipient.setText(currentWaypoint.getRecipientName());
        tvNavAddress.setText(currentWaypoint.getFullAddress());
        tvNavEta.setText("Calculando rota Orbi API...");

        // Atualiza traçado do mapa em tempo real
        mapView.setTargetWaypoint(currentWaypoint);

        // Consulta direções turn-by-turn reais via GET /api/route/{id}/directions da Orbi API
        repository.fetchRouteDirections(this, routeId, new RepositoryCallback<DirectionsResponseDto>() {
            @Override
            public void onSuccess(DirectionsResponseDto directions) {
                if (directions != null) {
                    mapView.setDirections(directions);
                    double distKm = directions.getDistanceMeters() / 1000.0;
                    int minutes = (int) Math.round(directions.getDurationSeconds() / 60.0);
                    if (minutes <= 0) minutes = 3;
                    tvNavEta.setText(String.format(Locale.getDefault(), "~%.1f km • %d min restantes", distKm, minutes));
                }
            }

            @Override
            public void onError(String message) {
                tvNavEta.setText("~850 m • 3 min restantes");
            }
        });
    }

    private void handleCompleteStop() {
        if (currentWaypoint == null) return;

        long deliveryId = currentWaypoint.getDeliveryId();

        // Integração com PATCH /api/deliveries/{id}/next-status da Orbi API
        repository.advanceDelivery(this, routeId, deliveryId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(NavigationMapActivity.this, "Status avançado na Orbi API (next-status)!", Toast.LENGTH_SHORT).show();
                checkNextOrFinish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(NavigationMapActivity.this, message, Toast.LENGTH_SHORT).show();
                checkNextOrFinish();
            }
        });
    }

    private enum ActionType {
        SKIP,
        CANCEL
    }

    private void showJustificationDialog(ActionType type) {
        if (currentWaypoint == null) return;

        final long deliveryId = currentWaypoint.getDeliveryId();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_justification, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        RadioGroup rgReasons = dialogView.findViewById(R.id.rgReasons);
        RadioButton rbReason1 = dialogView.findViewById(R.id.rbReason1);
        RadioButton rbReason2 = dialogView.findViewById(R.id.rbReason2);
        RadioButton rbReason3 = dialogView.findViewById(R.id.rbReason3);
        RadioButton rbReason4 = dialogView.findViewById(R.id.rbReason4);
        TextInputLayout tilOtherReason = dialogView.findViewById(R.id.tilOtherReason);
        TextInputEditText etOtherReason = dialogView.findViewById(R.id.etOtherReason);
        MaterialButton btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        MaterialButton btnDialogConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

        if (type == ActionType.SKIP) {
            tvDialogTitle.setText(R.string.dialog_skip_title);
            tvDialogSubtitle.setText(R.string.dialog_skip_subtitle);
            rbReason1.setText(R.string.reason_recipient_absent);
            rbReason2.setText(R.string.reason_blocked_road);
            rbReason3.setText(R.string.reason_business_closed);
            btnDialogConfirm.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.status_skipped));
        } else {
            tvDialogTitle.setText(R.string.dialog_cancel_title);
            tvDialogSubtitle.setText(R.string.dialog_cancel_subtitle);
            rbReason1.setText(R.string.reason_customer_cancelled);
            rbReason2.setText(R.string.reason_address_not_found);
            rbReason3.setText(R.string.reason_damaged_cargo);
            btnDialogConfirm.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this, R.color.status_cancelled));
        }

        rbReason4.setText(R.string.reason_other);

        rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbReason4) {
                tilOtherReason.setVisibility(View.VISIBLE);
            } else {
                tilOtherReason.setVisibility(View.GONE);
            }
        });

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogConfirm.setOnClickListener(v -> {
            String justification;
            int checkedId = rgReasons.getCheckedRadioButtonId();

            if (checkedId == R.id.rbReason1) {
                justification = rbReason1.getText().toString();
            } else if (checkedId == R.id.rbReason2) {
                justification = rbReason2.getText().toString();
            } else if (checkedId == R.id.rbReason3) {
                justification = rbReason3.getText().toString();
            } else {
                justification = etOtherReason.getText() != null ? etOtherReason.getText().toString().trim() : "";
                if (TextUtils.isEmpty(justification)) {
                    justification = "Outro Motivo";
                }
            }

            dialog.dismiss();

            if (type == ActionType.SKIP) {
                repository.skipWaypoint(NavigationMapActivity.this, routeId, deliveryId, justification, new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(NavigationMapActivity.this, "Endereço pulado.", Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }

                    @Override
                    public void onError(String message) {
                        checkNextOrFinish();
                    }
                });
            } else {
                // Integração real com PATCH /api/deliveries/{id}/cancel da Orbi API
                repository.cancelDelivery(NavigationMapActivity.this, routeId, deliveryId, justification, new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(NavigationMapActivity.this, "Entrega cancelada na Orbi API (cancel)!", Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(NavigationMapActivity.this, "Entrega cancelada localmente.", Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }
                });
            }
        });

        dialog.show();
    }

    private void checkNextOrFinish() {
        Waypoint next = currentRoute.getNextPendingWaypoint();
        if (next != null) {
            currentWaypoint = next;
            loadCurrentNavigationTarget();
        } else {
            showAllStopsHandledDialog();
        }
    }

    private void showAllStopsHandledDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.all_stops_handled_title)
                .setMessage(R.string.all_stops_handled_message)
                .setPositiveButton("Concluir Rota Agora", (dialog, which) -> {
                    repository.completeRoute(routeId);
                    Toast.makeText(this, R.string.route_completed_toast, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(NavigationMapActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Voltar aos Detalhes", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
