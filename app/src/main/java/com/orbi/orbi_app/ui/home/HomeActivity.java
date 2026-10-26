package com.orbi.orbi_app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.res.ColorStateList;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.data.SessionManager;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.ui.common.InsetsUtils;
import com.orbi.orbi_app.ui.details.RouteDetailsActivity;
import com.orbi.orbi_app.ui.login.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private View rootLayout;
    private TextView tvDate;
    private TextView tvGreeting;
    private ImageView ivApiStatus;
    private TextView tvApiStatus;
    private MaterialButton btnLogout;
    private ChipGroup chipGroupStatus;
    private SwipeRefreshLayout swipeRefreshHome;
    private RecyclerView rvRoutes;
    private LinearLayout layoutEmptyState;

    private RouteListAdapter adapter;
    private SessionManager sessionManager;
    private RouteRepository repository;
    private RouteStatus currentFilter = RouteStatus.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        repository = RouteRepository.getInstance();

        rootLayout = findViewById(R.id.rootLayout);
        tvDate = findViewById(R.id.tvDate);
        tvGreeting = findViewById(R.id.tvGreeting);
        ivApiStatus = findViewById(R.id.ivApiStatus);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        btnLogout = findViewById(R.id.btnLogout);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        swipeRefreshHome = findViewById(R.id.swipeRefreshHome);
        rvRoutes = findViewById(R.id.rvRoutes);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Edge-to-edge: cabeçalho abaixo da status bar; lista rola por trás da barra de navegação
        InsetsUtils.applyPadding(rootLayout, InsetsUtils.TOP | InsetsUtils.HORIZONTAL, false);
        InsetsUtils.applyPadding(rvRoutes, InsetsUtils.BOTTOM, false);
        InsetsUtils.applyPadding(layoutEmptyState, InsetsUtils.BOTTOM, false);

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
        String today = new SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(new Date());
        tvDate.setText(capitalize(today));

        String firstName = firstNameOf(sessionManager.getDriverName());
        tvGreeting.setText(TextUtils.isEmpty(firstName)
                ? getString(R.string.home_greeting_default)
                : getString(R.string.home_greeting, firstName));

        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void setupRecyclerView() {
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RouteListAdapter(route -> {
            Intent intent = new Intent(HomeActivity.this, RouteDetailsActivity.class);
            intent.putExtra("EXTRA_ROUTE_ID", route.getId());
            startActivity(intent);
        });
        rvRoutes.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
            if (checkedId == R.id.chipAssigned) {
                currentFilter = RouteStatus.ASSIGNED;
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
        swipeRefreshHome.setColorSchemeColors(
                MaterialColors.getColor(swipeRefreshHome, androidx.appcompat.R.attr.colorPrimary));
        swipeRefreshHome.setProgressBackgroundColorSchemeColor(
                MaterialColors.getColor(swipeRefreshHome, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        swipeRefreshHome.setOnRefreshListener(() -> syncWithOrbiApi(true));
    }

    private void syncWithOrbiApi(boolean showFeedback) {
        if (showFeedback) {
            swipeRefreshHome.setRefreshing(true);
        }
        setSyncStatus(R.drawable.ic_cloud_done, R.string.sync_status_syncing, 0);

        // Chamada real ao endpoint GET /api/route da Orbi API
        repository.fetchRoutesFromApi(this, new RepositoryCallback<List<Route>>() {
            @Override
            public void onSuccess(List<Route> result) {
                swipeRefreshHome.setRefreshing(false);
                setSyncStatus(R.drawable.ic_cloud_done, R.string.sync_status_online, R.color.status_completed);
                loadRoutes(currentFilter);
                if (showFeedback) {
                    Snackbar.make(rootLayout, R.string.sync_success, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                swipeRefreshHome.setRefreshing(false);
                setSyncStatus(R.drawable.ic_cloud_off, R.string.sync_status_offline, R.color.status_skipped);
                loadRoutes(currentFilter);
                if (showFeedback) {
                    Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG)
                            .setAction(R.string.btn_retry, v -> syncWithOrbiApi(true))
                            .show();
                }
            }
        });
    }

    /** Atualiza o indicador de sincronização. color = 0 usa a cor neutra do tema. */
    private void setSyncStatus(@DrawableRes int icon, @StringRes int text, @ColorRes int color) {
        int resolved = color == 0
                ? MaterialColors.getColor(tvApiStatus, com.google.android.material.R.attr.colorOnSurfaceVariant)
                : ContextCompat.getColor(this, color);
        ivApiStatus.setImageResource(icon);
        ImageViewCompat.setImageTintList(ivApiStatus, ColorStateList.valueOf(resolved));
        tvApiStatus.setText(text);
        tvApiStatus.setTextColor(resolved);
    }

    private void loadRoutes(RouteStatus filter) {
        List<Route> routes = repository.getRoutes(filter);
        adapter.setRoutes(routes);

        boolean empty = routes.isEmpty();
        rvRoutes.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_logout)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout_confirm, (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /** "carlos.silva" ou "Carlos Silva (Motorista)" → "Carlos". */
    private static String firstNameOf(String driverName) {
        if (TextUtils.isEmpty(driverName)) return "";
        String first = driverName.trim().split("[\\s._@(]+")[0];
        return capitalize(first);
    }

    private static String capitalize(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }
}
