package com.orbi.orbi_app.ui.details;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.model.WaypointStatus;
import com.orbi.orbi_app.ui.common.StatusStyles;

import java.util.ArrayList;
import java.util.List;

/** Timeline vertical das paradas da rota, na ordem de entrega. */
public class WaypointAdapter extends RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder> {

    private final List<Waypoint> waypoints = new ArrayList<>();

    @SuppressWarnings("NotifyDataSetChanged")
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
        private final View layoutContent;
        private final FrameLayout layoutStepCircle;
        private final TextView tvSequence;
        private final ImageView ivStepIcon;
        private final View viewTimelineLine;
        private final TextView tvRecipient;
        private final TextView tvWaypointStatus;
        private final TextView tvAddress;
        private final MaterialButton btnPhone;
        private final TextView tvJustification;

        WaypointViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContent = itemView.findViewById(R.id.layoutContent);
            layoutStepCircle = itemView.findViewById(R.id.layoutStepCircle);
            tvSequence = itemView.findViewById(R.id.tvSequence);
            ivStepIcon = itemView.findViewById(R.id.ivStepIcon);
            viewTimelineLine = itemView.findViewById(R.id.viewTimelineLine);
            tvRecipient = itemView.findViewById(R.id.tvRecipient);
            tvWaypointStatus = itemView.findViewById(R.id.tvWaypointStatus);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnPhone = itemView.findViewById(R.id.btnPhone);
            tvJustification = itemView.findViewById(R.id.tvJustification);
        }

        void bind(Waypoint waypoint, boolean isLast) {
            Context context = itemView.getContext();
            WaypointStatus status = waypoint.getStatus();
            StatusStyles.Style style = StatusStyles.of(status);

            tvRecipient.setText(waypoint.getRecipientName());
            tvAddress.setText(waypoint.getFullAddress());
            StatusStyles.applyPill(tvWaypointStatus, style);

            // Marcador: número da sequência, ou ícone quando a parada já foi tratada
            StatusStyles.tintBackground(layoutStepCircle, style.background);
            int fg = ContextCompat.getColor(context, style.foreground);
            int icon = iconFor(status);
            if (icon != 0) {
                tvSequence.setVisibility(View.GONE);
                ivStepIcon.setVisibility(View.VISIBLE);
                ivStepIcon.setImageResource(icon);
                ImageViewCompat.setImageTintList(ivStepIcon, ColorStateList.valueOf(fg));
            } else {
                ivStepIcon.setVisibility(View.GONE);
                tvSequence.setVisibility(View.VISIBLE);
                tvSequence.setText(String.valueOf(waypoint.getSequenceOrder()));
                tvSequence.setTextColor(fg);
            }

            // Paradas finalizadas ficam visualmente secundárias
            boolean handled = waypoint.isTerminal() || status == WaypointStatus.SKIPPED;
            tvRecipient.setAlpha(handled ? 0.72f : 1f);

            viewTimelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            // Telefone: toque para discar (não requer permissão)
            String phone = waypoint.getRecipientPhone();
            if (!TextUtils.isEmpty(phone)) {
                btnPhone.setVisibility(View.VISIBLE);
                btnPhone.setText(phone);
                btnPhone.setContentDescription(context.getString(R.string.cd_call_recipient, waypoint.getRecipientName()));
                btnPhone.setOnClickListener(v -> {
                    Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.replaceAll("[^0-9+]", "")));
                    if (dial.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(dial);
                    }
                });
            } else {
                btnPhone.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(waypoint.getJustification())) {
                tvJustification.setVisibility(View.VISIBLE);
                tvJustification.setText(context.getString(R.string.waypoint_reason, waypoint.getJustification()));
            } else {
                tvJustification.setVisibility(View.GONE);
            }

            layoutContent.setContentDescription(null);
            tvRecipient.setContentDescription(context.getString(R.string.cd_waypoint,
                    waypoint.getSequenceOrder(), waypoint.getRecipientName(),
                    waypoint.getFullAddress(), context.getString(style.label)));
            tvAddress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tvWaypointStatus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        private static int iconFor(WaypointStatus status) {
            if (status == null) return 0;
            switch (status) {
                case DELIVERED:
                case COMPLETED:
                    return R.drawable.ic_check;
                case SKIPPED:
                    return R.drawable.ic_skip;
                case CANCELLED:
                    return R.drawable.ic_close;
                default:
                    return 0;
            }
        }
    }
}
