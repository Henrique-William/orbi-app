package com.orbi.orbi_app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.data.SessionManager;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.ui.details.RouteDetailsActivity;
import com.orbi.orbi_app.ui.login.LoginActivity;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private View rootLayout;
    private TextView tvGreeting;
    private TextView tvApiStatus;
    private ImageButton btnLogout;
    private ChipGroup chipGroupStatus;
    private SwipeRefreshLayout swipeRefreshHome;
    private RecyclerView rvRoutesCarousel;
    private LinearLayout layoutEmptyState;

    private RouteCarouselAdapter adapter;
    private SessionManager sessionManager;
    private RouteRepository repository;
    private RouteStatus currentFilter = RouteStatus.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        repository = RouteRepository.getInstance();

        rootLayout = findViewById(R.id.rootLayout);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        btnLogout = findViewById(R.id.btnLogout);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        swipeRefreshHome = findViewById(R.id.swipeRefreshHome);
        rvRoutesCarousel = findViewById(R.id.rvRoutesCarousel);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Ajuste contra sobreposição da barra de status do sistema
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupHeader();
        setupRecyclerView();
        setupFilters();
        setupSwipeRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncWithOrbiApi(false);
    }

    private void setupHeader() {
        String driverName = sessionManager.getDriverName();
        tvGreeting.setText("Olá, " + driverName);
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvRoutesCarousel.setLayoutManager(layoutManager);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvRoutesCarousel);

        adapter = new RouteCarouselAdapter(route -> {
            Intent intent = new Intent(HomeActivity.this, RouteDetailsActivity.class);
            intent.putExtra("EXTRA_ROUTE_ID", route.getId());
            startActivity(intent);
        });

        rvRoutesCarousel.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
            if (checkedId == R.id.chipPending) {
                currentFilter = RouteStatus.PENDING;
            } else if (checkedId == R.id.chipDelayed) {
                currentFilter = RouteStatus.DELAYED;
            } else if (checkedId == R.id.chipInProgress) {
                currentFilter = RouteStatus.IN_PROGRESS;
            } else if (checkedId == R.id.chipCompleted) {
                currentFilter = RouteStatus.COMPLETED;
            } else {
                currentFilter = RouteStatus.ALL;
            }
            loadRoutes(currentFilter);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshHome.setColorSchemeResources(R.color.primary);
        swipeRefreshHome.setOnRefreshListener(() -> syncWithOrbiApi(true));
    }

    private void syncWithOrbiApi(boolean showFeedback) {
        if (showFeedback) {
            swipeRefreshHome.setRefreshing(true);
        }

        // Chamada real ao endpoint GET /api/route da Orbi API
        repository.fetchRoutesFromApi(this, new RepositoryCallback<List<Route>>() {
            @Override
            public void onSuccess(List<Route> result) {
                swipeRefreshHome.setRefreshing(false);
                tvApiStatus.setText("Sincronizado com Orbi API (/api/route)");
                tvApiStatus.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.status_completed));
                loadRoutes(currentFilter);
                if (showFeedback) {
                    Toast.makeText(HomeActivity.this, "Rotas sincronizadas com a Orbi API!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                swipeRefreshHome.setRefreshing(false);
                tvApiStatus.setText("Modo Offline • " + message);
                tvApiStatus.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.status_delayed));
                loadRoutes(currentFilter);
                if (showFeedback) {
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadRoutes(RouteStatus filter) {
        List<Route> routes = repository.getRoutes(filter);
        adapter.setRoutes(routes);

        if (routes.isEmpty()) {
            rvRoutesCarousel.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvRoutesCarousel.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair do Orbi")
                .setMessage("Deseja realmente encerrar a sessão?")
                .setPositiveButton("Sim, Sair", (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
