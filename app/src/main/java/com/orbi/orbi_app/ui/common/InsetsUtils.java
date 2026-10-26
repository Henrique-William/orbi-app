package com.orbi.orbi_app.ui.common;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilitários para layouts edge-to-edge: aplicam os insets das barras do sistema
 * (e opcionalmente do teclado) preservando o padding/margem originais do XML.
 */
public final class InsetsUtils {

    public static final int LEFT = 1;
    public static final int TOP = 1 << 1;
    public static final int RIGHT = 1 << 2;
    public static final int BOTTOM = 1 << 3;
    public static final int HORIZONTAL = LEFT | RIGHT;
    public static final int ALL = LEFT | TOP | RIGHT | BOTTOM;

    private InsetsUtils() {
    }

    /**
     * Soma os insets das barras do sistema ao padding da View nos lados indicados.
     *
     * @param sides      combinação de {@link #LEFT}, {@link #TOP}, {@link #RIGHT}, {@link #BOTTOM}
     * @param includeIme se true, o lado inferior considera também a altura do teclado
     */
    public static void applyPadding(@NonNull View view, int sides, boolean includeIme) {
        final int l = view.getPaddingLeft();
        final int t = view.getPaddingTop();
        final int r = view.getPaddingRight();
        final int b = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = systemInsets(windowInsets);
            int bottomInset = bars.bottom;
            if (includeIme) {
                bottomInset = Math.max(bottomInset, windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            }
            v.setPadding(
                    l + ((sides & LEFT) != 0 ? bars.left : 0),
                    t + ((sides & TOP) != 0 ? bars.top : 0),
                    r + ((sides & RIGHT) != 0 ? bars.right : 0),
                    b + ((sides & BOTTOM) != 0 ? bottomInset : 0));
            return windowInsets;
        });
        requestApplyInsetsWhenAttached(view);
    }

    /** Soma os insets das barras do sistema às margens da View nos lados indicados. */
    public static void applyMargin(@NonNull View view, int sides) {
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams initial = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        final int l = initial.leftMargin;
        final int t = initial.topMargin;
        final int r = initial.rightMargin;
        final int b = initial.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = systemInsets(windowInsets);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.leftMargin = l + ((sides & LEFT) != 0 ? bars.left : 0);
            lp.topMargin = t + ((sides & TOP) != 0 ? bars.top : 0);
            lp.rightMargin = r + ((sides & RIGHT) != 0 ? bars.right : 0);
            lp.bottomMargin = b + ((sides & BOTTOM) != 0 ? bars.bottom : 0);
            v.setLayoutParams(lp);
            return windowInsets;
        });
        requestApplyInsetsWhenAttached(view);
    }

    private static Insets systemInsets(WindowInsetsCompat insets) {
        return insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
    }

    private static void requestApplyInsetsWhenAttached(View view) {
        if (view.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(view);
        } else {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    v.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(v);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });
        }
    }
}
