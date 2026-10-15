package com.orbi.orbi_app.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.ui.map.NavigationMapActivity;

public class RouteDetailsActivity extends AppCompatActivity {

    private View rootLayout;
    private ImageButton btnBack;
    private TextView tvToolbarTitle;
    private TextView tvHeaderStatus;
    private TextView tvRouteSummaryCode;
    private TextView tvStopsProgress;
    private LinearProgressIndicator progressBarDetails;
    private RecyclerView rvWaypoints;
    private MaterialButton btnRouteAction;

    private RouteRepository repository;
    private WaypointAdapter adapter;
    private String routeId;
    private Route currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        repository = RouteRepository.getInstance();
        routeId = getIntent().getStringExtra("EXTRA_ROUTE_ID");

        rootLayout = findViewById(R.id.rootLayout);
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);
        tvRouteSummaryCode = findViewById(R.id.tvRouteSummaryCode);
        tvStopsProgress = findViewById(R.id.tvStopsProgress);
        progressBarDetails = findViewById(R.id.progressBarDetails);
        rvWaypoints = findViewById(R.id.rvWaypoints);
        btnRouteAction = findViewById(R.id.btnRouteAction);

        // Previne colisão com a barra superior de status do Android
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack.setOnClickListener(v -> finish());
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRouteDetails();
    }

    private void setupRecyclerView() {
        rvWaypoints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaypointAdapter();
        rvWaypoints.setAdapter(adapter);
    }

    private void loadRouteDetails() {
        if (routeId == null) {
            finish();
            return;
        }

        // Carrega dados locais imediatos
        currentRoute = repository.getRouteById(routeId);
        if (currentRoute != null) {
            bindRouteData(currentRoute);
        }

        // Consulta endpoint GET /api/route/{routeId} da Orbi API para sincronizar
        repository.fetchRouteByIdFromApi(this, routeId, new RepositoryCallback<Route>() {
            @Override
            public void onSuccess(Route result) {
                currentRoute = result;
                bindRouteData(result);
            }

            @Override
            public void onError(String message) {
                // Continua exibindo dados locais
                if (currentRoute != null) {
                    bindRouteData(currentRoute);
                }
            }
        });
    }

    private void bindRouteData(Route route) {
        tvToolbarTitle.setText("Rota " + route.getCode());
        tvRouteSummaryCode.setText(route.getCode());
        tvHeaderStatus.setText(route.getStatus().getDisplayName());

        int total = route.getTotalWaypoints();
        int completed = route.getCompletedWaypoints();
        tvStopsProgress.setText(completed + " de " + total + " paradas concluídas • ~"
                + route.getEstimatedDurationMinutes() + " min estimados");

        int progressPercent = total > 0 ? (completed * 100) / total : 0;
        progressBarDetails.setProgress(progressPercent);

        adapter.setWaypoints(route.getWaypoints());

        configureActionButton();
    }

    private void configureActionButton() {
        if (currentRoute == null) return;

        boolean allHandled = currentRoute.areAllWaypointsHandled();
        boolean isAlreadyCompleted = currentRoute.getStatus() == RouteStatus.COMPLETED;

        if (allHandled || isAlreadyCompleted) {
            btnRouteAction.setText(R.string.btn_complete_route);
            btnRouteAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_completed));
            btnRouteAction.setOnClickListener(v -> {
                repository.completeRoute(routeId);
                Toast.makeText(this, R.string.route_completed_toast, Toast.LENGTH_LONG).show();
                finish();
            });
        } else {
            btnRouteAction.setText(R.string.btn_start_route);
            btnRouteAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            btnRouteAction.setOnClickListener(v -> {
                repository.startRoute(routeId);
                Intent intent = new Intent(RouteDetailsActivity.this, NavigationMapActivity.class);
                intent.putExtra("EXTRA_ROUTE_ID", routeId);
                startActivity(intent);
            });
        }
    }
}
