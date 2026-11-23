package com.orbi.orbi_app.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
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
 * Fonte única de verdade para a representação visual (cor, borda e rótulo) dos status
 * de rotas e entregas conforme o Design System Orbi (.badge.*).
 */
public final class StatusStyles {

    /** Conjunto de cores (texto/ícone, fundo, borda) + rótulo legível de um status. */
    public static final class Style {
        @ColorRes public final int foreground;
        @ColorRes public final int background;
        @ColorRes public final int border;
        @StringRes public final int label;

        Style(@ColorRes int foreground, @ColorRes int background, @StringRes int label) {
            this(foreground, background, R.color.badge_border, label);
        }

        Style(@ColorRes int foreground,
              @ColorRes int background,
              @ColorRes int border,
              @StringRes int label) {
            this.foreground = foreground;
            this.background = background;
            this.border = border;
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
                return new Style(
                        R.color.badge_blue_fg,
                        R.color.badge_blue_bg,
                        R.color.badge_blue_border,
                        R.string.status_route_in_progress
                );
            case COMPLETED:
                return new Style(
                        R.color.badge_dark_fg,
                        R.color.badge_dark_bg,
                        R.color.badge_dark_border,
                        R.string.status_route_completed
                );
            case CANCELLED:
                return new Style(
                        R.color.status_cancelled,
                        R.color.status_cancelled_bg,
                        R.color.badge_danger_border,
                        R.string.status_route_cancelled
                );
            case ASSIGNED:
                return new Style(
                        R.color.badge_neutral_fg,
                        R.color.badge_neutral_bg,
                        R.color.badge_neutral_border,
                        R.string.status_route_assigned
                );
            case PENDING:
            default:
                return new Style(
                        R.color.badge_outline_fg,
                        R.color.badge_outline_bg,
                        R.color.badge_outline_border,
                        R.string.status_route_pending
                );
        }
    }

    @NonNull
    public static Style of(WaypointStatus status) {
        if (status == null) status = WaypointStatus.AT_PICKUP;
        switch (status) {
            case AT_PICKUP:
                return new Style(
                        R.color.badge_neutral_fg,
                        R.color.badge_neutral_bg,
                        R.color.badge_neutral_border,
                        R.string.status_wp_at_pickup
                );
            case IN_TRANSIT:
                return new Style(
                        R.color.badge_blue_fg,
                        R.color.badge_blue_bg,
                        R.color.badge_blue_border,
                        R.string.status_wp_in_transit
                );
            case DELIVERED:
            case COMPLETED:
                return new Style(
                        R.color.badge_dark_fg,
                        R.color.badge_dark_bg,
                        R.color.badge_dark_border,
                        R.string.status_wp_completed
                );
            case SKIPPED:
                return new Style(
                        R.color.status_skipped,
                        R.color.status_skipped_bg,
                        R.color.badge_warning_border,
                        R.string.status_wp_skipped
                );
            case CANCELLED:
                return new Style(
                        R.color.status_cancelled,
                        R.color.status_cancelled_bg,
                        R.color.badge_danger_border,
                        R.string.status_wp_cancelled
                );
            case REQUESTED:
            case ACCEPTED:
            case PENDING:
            default:
                return new Style(
                        R.color.badge_outline_fg,
                        R.color.badge_outline_bg,
                        R.color.badge_outline_border,
                        R.string.status_wp_pending
                );
        }
    }

    /** Aplica rótulo, borda 1dp, fundo e ponto indicador (.badge::before) a uma pílula de status. */
    public static void applyPill(@NonNull TextView pill, @NonNull Style style) {
        Context context = pill.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        int fgColor = ContextCompat.getColor(context, style.foreground);
        int bgColor = ContextCompat.getColor(context, style.background);
        int borderColor = ContextCompat.getColor(context, style.border);

        pill.setText(style.label);
        pill.setTextColor(fgColor);
        ViewCompat.setBackgroundTintList(pill, null);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(999f * density);
        pillBg.setColor(bgColor);
        pillBg.setStroke(Math.max(1, Math.round(1f * density)), borderColor);
        pill.setBackground(pillBg);

        int dotSize = Math.max(1, Math.round(6f * density));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(fgColor);
        dot.setSize(dotSize, dotSize);
        dot.setBounds(0, 0, dotSize, dotSize);

        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        pill.setCompoundDrawablePadding(Math.max(1, Math.round(6f * density)));
    }

    /** Tinge o fundo de uma View individualmente (sem afetar o drawable compartilhado). */
    public static void tintBackground(@NonNull View view, @ColorRes int color) {
        ViewCompat.setBackgroundTintList(view,
                ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), color)));
    }
}
