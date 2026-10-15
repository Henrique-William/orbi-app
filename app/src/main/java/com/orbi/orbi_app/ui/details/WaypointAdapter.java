package com.orbi.orbi_app.ui.details;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.model.WaypointStatus;

import java.util.ArrayList;
import java.util.List;

public class WaypointAdapter extends RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder> {

    private final List<Waypoint> waypoints = new ArrayList<>();

    public void setWaypoints(List<Waypoint> newWaypoints) {
        this.waypoints.clear();
        if (newWaypoints != null) {
            this.waypoints.addAll(newWaypoints);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WaypointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waypoint, parent, false);
        return new WaypointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaypointViewHolder holder, int position) {
        Waypoint waypoint = waypoints.get(position);
        boolean isLast = (position == getItemCount() - 1);
        holder.bind(waypoint, isLast);
    }

    @Override
    public int getItemCount() {
        return waypoints.size();
    }

    static class WaypointViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout layoutStepCircle;
        private final TextView tvSequence;
        private final View viewTimelineLine;
        private final TextView tvRecipient;
        private final TextView tvWaypointStatus;
        private final TextView tvAddress;
        private final TextView tvPhone;
        private final TextView tvJustification;

        public WaypointViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutStepCircle = itemView.findViewById(R.id.layoutStepCircle);
            tvSequence = itemView.findViewById(R.id.tvSequence);
            viewTimelineLine = itemView.findViewById(R.id.viewTimelineLine);
            tvRecipient = itemView.findViewById(R.id.tvRecipient);
            tvWaypointStatus = itemView.findViewById(R.id.tvWaypointStatus);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvJustification = itemView.findViewById(R.id.tvJustification);
        }

        public void bind(Waypoint waypoint, boolean isLast) {
            Context context = itemView.getContext();

            tvSequence.setText(String.valueOf(waypoint.getSequenceOrder()));
            tvRecipient.setText(waypoint.getRecipientName());
            tvAddress.setText(waypoint.getFullAddress());
            tvPhone.setText("Contato: " + waypoint.getRecipientPhone());
            tvWaypointStatus.setText(waypoint.getStatus().getDisplayName());

            // Controle da linha conectora da timeline
            viewTimelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            applyStatusColors(context, waypoint.getStatus());

            if (!TextUtils.isEmpty(waypoint.getJustification())) {
                tvJustification.setVisibility(View.VISIBLE);
                tvJustification.setText("Motivo: " + waypoint.getJustification());
            } else {
                tvJustification.setVisibility(View.GONE);
            }
        }

        private void applyStatusColors(Context context, WaypointStatus status) {
            int textColor;
            int bgColor;

            switch (status) {
                case PENDING:
                    textColor = ContextCompat.getColor(context, R.color.status_pending);
                    bgColor = ContextCompat.getColor(context, R.color.status_pending_bg);
                    break;
                case IN_TRANSIT:
                    textColor = ContextCompat.getColor(context, R.color.status_in_progress);
                    bgColor = ContextCompat.getColor(context, R.color.status_in_progress_bg);
                    break;
                case COMPLETED:
                    textColor = ContextCompat.getColor(context, R.color.status_completed);
                    bgColor = ContextCompat.getColor(context, R.color.status_completed_bg);
                    break;
                case SKIPPED:
                    textColor = ContextCompat.getColor(context, R.color.status_skipped);
                    bgColor = ContextCompat.getColor(context, R.color.status_skipped_bg);
                    break;
                case CANCELLED:
                    textColor = ContextCompat.getColor(context, R.color.status_cancelled);
                    bgColor = ContextCompat.getColor(context, R.color.status_cancelled_bg);
                    break;
                default:
                    textColor = ContextCompat.getColor(context, R.color.text_primary);
                    bgColor = ContextCompat.getColor(context, R.color.card_stroke);
                    break;
            }

            tvWaypointStatus.setTextColor(textColor);
            tvWaypointStatus.getBackground().setTint(bgColor);

            // Estilização do círculo da Timeline
            layoutStepCircle.getBackground().setTint(bgColor);
            tvSequence.setTextColor(textColor);
        }
    }
}
