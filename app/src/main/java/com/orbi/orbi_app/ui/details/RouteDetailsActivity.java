package com.orbi.orbi_app.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.ui.common.InsetsUtils;
import com.orbi.orbi_app.ui.map.NavigationMapActivity;

import java.util.List;

public class RouteDetailsActivity extends AppCompatActivity {

    private View rootLayout;
    private MaterialToolbar toolbar;
    private RecyclerView rvWaypoints;
    private View layoutBottomBar;
    private MaterialButton btnViewMap;
    private MaterialButton btnRouteAction;

    private RouteRepository repository;
    private RouteSummaryAdapter summaryAdapter;
    private WaypointAdapter waypointAdapter;
    private String routeId;
    private Route currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        repository = RouteRepository.getInstance();
        routeId = getIntent().getStringExtra("EXTRA_ROUTE_ID");

        rootLayout = findViewById(R.id.rootLayout);
        toolbar = findViewById(R.id.toolbar);
        rvWaypoints = findViewById(R.id.rvWaypoints);
        layoutBottomBar = findViewById(R.id.layoutBottomBar);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnRouteAction = findViewById(R.id.btnRouteAction);

        // Edge-to-edge: toolbar abaixo da status bar; rodapé estende-se por trás da barra de navegação
        InsetsUtils.applyPadding(rootLayout, InsetsUtils.TOP | InsetsUtils.HORIZONTAL, false);
        InsetsUtils.applyPadding(layoutBottomBar, InsetsUtils.BOTTOM, false);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_route_details);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_view_map) {
                openNavigationMap();
                return true;
            }
            return false;
        });

        btnViewMap.setOnClickListener(v -> openNavigationMap());
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRouteDetails();
    }

    private void setupRecyclerView() {
        rvWaypoints.setLayoutManager(new LinearLayoutManager(this));
        summaryAdapter = new RouteSummaryAdapter();
        waypointAdapter = new WaypointAdapter();
        rvWaypoints.setAdapter(new ConcatAdapter(summaryAdapter, waypointAdapter));
    }

    private void loadRouteDetails() {
        if (routeId == null) {
            finish();
            return;
        }

        // Carrega dados locais imediatos se houver
        currentRoute = repository.getRouteById(routeId);
        if (currentRoute != null) {
            bindRouteData(currentRoute);
        }

        // 1. Busca as paradas e endereços reais no endpoint GET /api/deliveries/route/{routeId}
        repository.fetchDeliveriesByRouteIdFromApi(this, routeId, new RepositoryCallback<List<Waypoint>>() {
            @Override
            public void onSuccess(List<Waypoint> waypoints) {
                currentRoute = repository.getRouteById(routeId);
                if (currentRoute != null) {
                    bindRouteData(currentRoute);
                }
            }

            @Override
            public void onError(String message) {
            }
        });

        // 2. Consulta endpoint GET /api/route/{routeId} da Orbi API para sincronizar metadados
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
        toolbar.setTitle(getString(R.string.route_label) + " " + route.getCode());
        summaryAdapter.setRoute(route);
        waypointAdapter.setWaypoints(route.getWaypoints());
        configureActionButton();
    }

    private void configureActionButton() {
        if (currentRoute == null) return;

        boolean allHandled = currentRoute.areAllWaypointsHandled();
        boolean isAlreadyCompleted = currentRoute.getStatus() == RouteStatus.COMPLETED;
        boolean isCancelled = currentRoute.getStatus() == RouteStatus.CANCELLED;
        boolean inProgress = currentRoute.getStatus() == RouteStatus.IN_PROGRESS;

        if (isAlreadyCompleted) {
            btnRouteAction.setText(R.string.status_route_completed_done);
            btnRouteAction.setIconResource(R.drawable.ic_check);
            btnRouteAction.setEnabled(false);
        } else if (isCancelled) {
            btnRouteAction.setText(R.string.status_route_cancelled);
            btnRouteAction.setIconResource(R.drawable.ic_cancel);
            btnRouteAction.setEnabled(false);
        } else if (allHandled) {
            btnRouteAction.setText(R.string.btn_complete_route);
            btnRouteAction.setIconResource(R.drawable.ic_check);
            btnRouteAction.setEnabled(true);
            btnRouteAction.setOnClickListener(v -> {
                repository.completeRoute(routeId);
                Toast.makeText(this, R.string.route_completed_toast, Toast.LENGTH_LONG).show();
                bindRouteData(currentRoute);
                finish();
            });
        } else if (inProgress) {
            // Rota já foi iniciada -> Continuar rota de onde parou
            btnRouteAction.setText(R.string.btn_continue_route);
            btnRouteAction.setIconResource(R.drawable.ic_navigation);
            btnRouteAction.setEnabled(true);
            btnRouteAction.setOnClickListener(v -> openNavigationMap());
        } else {
            // Rota ainda não iniciada -> Iniciar rota
            btnRouteAction.setText(R.string.btn_start_route);
            btnRouteAction.setIconResource(R.drawable.ic_navigation);
            btnRouteAction.setEnabled(true);
            btnRouteAction.setOnClickListener(v -> {
                repository.startRoute(routeId);
                openNavigationMap();
            });
        }
    }

    private void openNavigationMap() {
        Intent intent = new Intent(RouteDetailsActivity.this, NavigationMapActivity.class);
        intent.putExtra("EXTRA_ROUTE_ID", routeId);
        startActivity(intent);
    }
}
