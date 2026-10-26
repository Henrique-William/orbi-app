package com.orbi.orbi_app.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.ui.common.StatusStyles;

import java.util.ArrayList;
import java.util.List;

/** Lista vertical de rotas atribuídas ao motorista. */
public class RouteListAdapter extends RecyclerView.Adapter<RouteListAdapter.RouteViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }

    private final List<Route> routes = new ArrayList<>();
    private final OnRouteClickListener listener;

    public RouteListAdapter(OnRouteClickListener listener) {
        this.listener = listener;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    public void setRoutes(List<Route> newRoutes) {
        this.routes.clear();
        if (newRoutes != null) {
            this.routes.addAll(newRoutes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_card, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        holder.bind(routes.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoute;
        private final TextView tvRouteCode;
        private final TextView tvRouteStatus;
        private final TextView tvTotalStops;
        private final TextView tvEstimatedTime;
        private final TextView tvProgressLabel;
        private final LinearProgressIndicator progressBarRoute;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoute = itemView.findViewById(R.id.cardRoute);
            tvRouteCode = itemView.findViewById(R.id.tvRouteCode);
            tvRouteStatus = itemView.findViewById(R.id.tvRouteStatus);
            tvTotalStops = itemView.findViewById(R.id.tvTotalStops);
            tvEstimatedTime = itemView.findViewById(R.id.tvEstimatedTime);
            tvProgressLabel = itemView.findViewById(R.id.tvProgressLabel);
            progressBarRoute = itemView.findViewById(R.id.progressBarRoute);
        }

        void bind(Route route, OnRouteClickListener listener) {
            Context context = itemView.getContext();

            StatusStyles.Style style = StatusStyles.of(route.getStatus());
            StatusStyles.applyPill(tvRouteStatus, style);

            int total = route.getTotalWaypoints();
            int completed = route.getCompletedWaypoints();
            int minutes = route.getEstimatedDurationMinutes();
            String stops = context.getResources().getQuantityString(R.plurals.route_stops_count, total, total);

            tvRouteCode.setText(route.getCode());
            tvTotalStops.setText(stops);
            tvEstimatedTime.setText(context.getString(R.string.route_duration, minutes));
            tvProgressLabel.setText(context.getString(R.string.route_progress_fraction, completed, total));
            progressBarRoute.setProgressCompat(total > 0 ? (completed * 100) / total : 0, false);

            // Um único anúncio coerente para leitores de tela
            cardRoute.setContentDescription(context.getString(R.string.cd_route_card,
                    route.getCode(), context.getString(style.label), stops, minutes, completed, total));

            cardRoute.setOnClickListener(v -> {
                if (listener != null) listener.onRouteClick(route);
            });
        }
    }
}
