package com.orbi.orbi_app.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.RouteStatus;

import java.util.ArrayList;
import java.util.List;

public class RouteCarouselAdapter extends RecyclerView.Adapter<RouteCarouselAdapter.RouteViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }

    private final List<Route> routes = new ArrayList<>();
    private final OnRouteClickListener listener;

    public RouteCarouselAdapter(OnRouteClickListener listener) {
        this.listener = listener;
    }

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
        Route route = routes.get(position);
        holder.bind(route, listener);
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
        private final MaterialButton btnOpenDetails;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoute = itemView.findViewById(R.id.cardRoute);
            tvRouteCode = itemView.findViewById(R.id.tvRouteCode);
            tvRouteStatus = itemView.findViewById(R.id.tvRouteStatus);
            tvTotalStops = itemView.findViewById(R.id.tvTotalStops);
            tvEstimatedTime = itemView.findViewById(R.id.tvEstimatedTime);
            tvProgressLabel = itemView.findViewById(R.id.tvProgressLabel);
            progressBarRoute = itemView.findViewById(R.id.progressBarRoute);
            btnOpenDetails = itemView.findViewById(R.id.btnOpenDetails);
        }

        public void bind(Route route, OnRouteClickListener listener) {
            Context context = itemView.getContext();

            tvRouteCode.setText(route.getCode());
            tvRouteStatus.setText(route.getStatus().getDisplayName());
            applyStatusColors(context, route.getStatus());

            int total = route.getTotalWaypoints();
            int completed = route.getCompletedWaypoints();
            tvTotalStops.setText(total + " " + (total == 1 ? "parada" : "paradas"));
            tvEstimatedTime.setText("~" + route.getEstimatedDurationMinutes() + " min");

            tvProgressLabel.setText("Progresso: " + completed + "/" + total + " concluídas");
            int progressPercent = total > 0 ? (completed * 100) / total : 0;
            progressBarRoute.setProgress(progressPercent);

            View.OnClickListener clickAction = v -> {
                if (listener != null) {
                    listener.onRouteClick(route);
                }
            };

            cardRoute.setOnClickListener(clickAction);
            btnOpenDetails.setOnClickListener(clickAction);
        }

        private void applyStatusColors(Context context, RouteStatus status) {
            int textColor;
            int bgColor;

            switch (status) {
                case PENDING:
                    textColor = ContextCompat.getColor(context, R.color.status_pending);
                    bgColor = ContextCompat.getColor(context, R.color.status_pending_bg);
                    break;
                case DELAYED:
                    textColor = ContextCompat.getColor(context, R.color.status_delayed);
                    bgColor = ContextCompat.getColor(context, R.color.status_delayed_bg);
                    break;
                case IN_PROGRESS:
                    textColor = ContextCompat.getColor(context, R.color.status_in_progress);
                    bgColor = ContextCompat.getColor(context, R.color.status_in_progress_bg);
                    break;
                case COMPLETED:
                    textColor = ContextCompat.getColor(context, R.color.status_completed);
                    bgColor = ContextCompat.getColor(context, R.color.status_completed_bg);
                    break;
                default:
                    textColor = ContextCompat.getColor(context, R.color.text_primary);
                    bgColor = ContextCompat.getColor(context, R.color.card_stroke);
                    break;
            }

            tvRouteStatus.setTextColor(textColor);
            tvRouteStatus.getBackground().setTint(bgColor);
        }
    }
}
