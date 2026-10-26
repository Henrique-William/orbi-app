package com.orbi.orbi_app.ui.details;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.ui.common.StatusStyles;

/** Cabeçalho (item único) com o resumo da rota, exibido acima da timeline de paradas. */
public class RouteSummaryAdapter extends RecyclerView.Adapter<RouteSummaryAdapter.SummaryViewHolder> {

    @Nullable
    private Route route;

    public void setRoute(@Nullable Route route) {
        boolean hadItem = this.route != null;
        this.route = route;
        if (route == null && hadItem) {
            notifyItemRemoved(0);
        } else if (route != null && !hadItem) {
            notifyItemInserted(0);
        } else if (route != null) {
            notifyItemChanged(0);
        }
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.header_route_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        if (route != null) holder.bind(route);
    }

    @Override
    public int getItemCount() {
        return route == null ? 0 : 1;
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRouteSummaryCode;
        private final TextView tvHeaderStatus;
        private final TextView tvStatTotal;
        private final TextView tvStatCompleted;
        private final TextView tvStatDuration;
        private final View statTotal;
        private final View statCompleted;
        private final View statDuration;
        private final LinearProgressIndicator progressBarDetails;

        SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteSummaryCode = itemView.findViewById(R.id.tvRouteSummaryCode);
            tvHeaderStatus = itemView.findViewById(R.id.tvHeaderStatus);
            tvStatTotal = itemView.findViewById(R.id.tvStatTotal);
            tvStatCompleted = itemView.findViewById(R.id.tvStatCompleted);
            tvStatDuration = itemView.findViewById(R.id.tvStatDuration);
            statTotal = itemView.findViewById(R.id.statTotal);
            statCompleted = itemView.findViewById(R.id.statCompleted);
            statDuration = itemView.findViewById(R.id.statDuration);
            progressBarDetails = itemView.findViewById(R.id.progressBarDetails);
        }

        void bind(Route route) {
            Context context = itemView.getContext();
            int total = route.getTotalWaypoints();
            int completed = route.getCompletedWaypoints();
            int minutes = route.getEstimatedDurationMinutes();
            int percent = total > 0 ? (completed * 100) / total : 0;

            tvRouteSummaryCode.setText(route.getCode());
            StatusStyles.applyPill(tvHeaderStatus, StatusStyles.of(route.getStatus()));

            tvStatTotal.setText(String.valueOf(total));
            tvStatCompleted.setText(String.valueOf(completed));
            tvStatDuration.setText(context.getString(R.string.route_duration, minutes));

            // Cada métrica é lida como "valor rótulo" por leitores de tela
            statTotal.setContentDescription(total + " " + context.getString(R.string.stat_stops));
            statCompleted.setContentDescription(completed + " " + context.getString(R.string.stat_completed));
            statDuration.setContentDescription(context.getString(R.string.stat_duration) + " "
                    + context.getString(R.string.route_duration, minutes));

            progressBarDetails.setProgressCompat(percent, true);
            progressBarDetails.setContentDescription(context.getString(R.string.cd_route_progress, percent));
        }
    }
}
