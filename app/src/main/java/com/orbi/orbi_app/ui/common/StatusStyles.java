package com.orbi.orbi_app.ui.common;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.model.WaypointStatus;

/**
 * Fonte única de verdade para a representação visual (cor + rótulo) dos status
 * de rotas e entregas conforme as regras oficiais da Orbi API.
 */
public final class StatusStyles {

    /** Par de cores (texto/ícone e fundo) + rótulo legível de um status. */
    public static final class Style {
        @ColorRes public final int foreground;
        @ColorRes public final int background;
        @StringRes public final int label;

        Style(@ColorRes int foreground, @ColorRes int background, @StringRes int label) {
            this.foreground = foreground;
            this.background = background;
            this.label = label;
        }
    }

    private StatusStyles() {
    }

    @NonNull
    public static Style of(RouteStatus status) {
        if (status == null) status = RouteStatus.ASSIGNED;
        switch (status) {
            case IN_PROGRESS:
                return new Style(R.color.status_in_progress, R.color.status_in_progress_bg, R.string.status_route_in_progress);
            case COMPLETED:
                return new Style(R.color.status_completed, R.color.status_completed_bg, R.string.status_route_completed);
            case CANCELLED:
                return new Style(R.color.status_cancelled, R.color.status_cancelled_bg, R.string.status_route_cancelled);
            case ASSIGNED:
                return new Style(R.color.status_pending, R.color.status_pending_bg, R.string.status_route_assigned);
            case PENDING:
            default:
                return new Style(R.color.status_pending, R.color.status_pending_bg, R.string.status_route_pending);
        }
    }

    @NonNull
    public static Style of(WaypointStatus status) {
        if (status == null) status = WaypointStatus.AT_PICKUP;
        switch (status) {
            case AT_PICKUP:
                return new Style(R.color.status_pending, R.color.status_pending_bg, R.string.status_wp_at_pickup);
            case IN_TRANSIT:
                return new Style(R.color.status_in_progress, R.color.status_in_progress_bg, R.string.status_wp_in_transit);
            case DELIVERED:
            case COMPLETED:
                return new Style(R.color.status_completed, R.color.status_completed_bg, R.string.status_wp_completed);
            case SKIPPED:
                return new Style(R.color.status_skipped, R.color.status_skipped_bg, R.string.status_wp_skipped);
            case CANCELLED:
                return new Style(R.color.status_cancelled, R.color.status_cancelled_bg, R.string.status_wp_cancelled);
            case REQUESTED:
            case ACCEPTED:
            case PENDING:
            default:
                return new Style(R.color.status_pending, R.color.status_pending_bg, R.string.status_wp_pending);
        }
    }

    /** Aplica rótulo e cores a uma pílula de status (estilo Widget.Orbi.StatusPill). */
    public static void applyPill(@NonNull TextView pill, @NonNull Style style) {
        pill.setText(style.label);
        pill.setTextColor(ContextCompat.getColor(pill.getContext(), style.foreground));
        tintBackground(pill, style.background);
    }

    /** Tinge o fundo de uma View individualmente (sem afetar o drawable compartilhado). */
    public static void tintBackground(@NonNull View view, @ColorRes int color) {
        ViewCompat.setBackgroundTintList(view,
                ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), color)));
    }
}
