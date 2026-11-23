package com.orbi.orbi_app.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.orbi.orbi_app.R;

/**
 * Botão "segure para confirmar" (Orbi Design System · .m3-hold-btn).
 * Pílula neutra (surface + borda 1dp) que é preenchida da esquerda para a direita
 * em tom carbono durante {@link #HOLD_DURATION_MS}. Ao completar, dispara o listener.
 */
public class RadialHoldButton extends FrameLayout {

    public interface OnHoldTriggeredListener {
        void onTriggered();
    }

    private static final long HOLD_DURATION_MS = 1000L;

    private int surfaceColor;
    private int lineColor;
    private int inkColor;
    private int fillColor;
    private float cornerRadiusPx;
    private float strokePx;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF boundsRect = new RectF();
    private final RectF fillRect = new RectF();

    private float progress = 0f;
    private boolean contentInverted = false;
    private ValueAnimator holdAnimator;
    private ValueAnimator collapseAnimator;
    private boolean isTriggered = false;

    private ImageView ivIcon;
    private TextView tvLabel;
    private OnHoldTriggeredListener triggeredListener;

    public RadialHoldButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public RadialHoldButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadialHoldButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        cornerRadiusPx = 999f * density;
        strokePx = 1f * density;

        surfaceColor = ContextCompat.getColor(context, R.color.surface);
        lineColor = ContextCompat.getColor(context, R.color.outline_variant);
        inkColor = ContextCompat.getColor(context, R.color.on_surface);
        fillColor = inkColor;

        bgPaint.setColor(surfaceColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokePx);
        strokePaint.setColor(lineColor);
        fillPaint.setColor(fillColor);
        fillPaint.setAlpha(Math.round(255 * 0.25f));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        LayoutParams contentParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER;
        addView(content, contentParams);

        ivIcon = new ImageView(context);
        int iconSize = (int) (18 * density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.rightMargin = (int) (8 * density);
        content.addView(ivIcon, iconParams);

        tvLabel = new TextView(context);
        tvLabel.setTextSize(14f);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(tvLabel);

        applyContentColor(false);
        setupTouch();
    }

    /** Configura ícone, texto e cor do preenchimento com 25% de opacidade. */
    public void setButtonConfig(int iconResId, String text, int fillColor) {
        this.fillColor = fillColor;
        fillPaint.setColor(fillColor);
        fillPaint.setAlpha(Math.round(255 * 0.25f));
        if (iconResId != 0) ivIcon.setImageResource(iconResId);
        if (text != null) {
            tvLabel.setText(text);
            setContentDescription(text + " (segure 1 segundo)");
        }
        applyContentColor(false);
        invalidate();
    }

    public void setOnHoldTriggeredListener(OnHoldTriggeredListener listener) {
        this.triggeredListener = listener;
    }

    private void applyContentColor(boolean inverted) {
        contentInverted = false;
        tvLabel.setTextColor(inkColor);
        ivIcon.setColorFilter(inkColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boundsRect.set(0, 0, w, h);
        float r = Math.min(cornerRadiusPx, h / 2f);
        clipPath.reset();
        clipPath.addRoundRect(boundsRect, r, r, Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float r = Math.min(cornerRadiusPx, getHeight() / 2f);
        canvas.save();
        canvas.clipPath(clipPath);
        canvas.drawRoundRect(boundsRect, r, r, bgPaint);

        if (progress > 0f) {
            fillRect.set(0, 0, getWidth() * progress, getHeight());
            canvas.drawRect(fillRect, fillPaint);
        }

        super.dispatchDraw(canvas);
        canvas.restore();

        float half = strokePx / 2f;
        canvas.drawRoundRect(half, half, getWidth() - half, getHeight() - half, r, r, strokePaint);
    }

    private void setProgress(float value) {
        progress = value;
        invalidate();
    }

    private void setupTouch() {
        setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isTriggered = false;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

                    if (collapseAnimator != null) collapseAnimator.cancel();
                    if (holdAnimator != null) holdAnimator.cancel();

                    holdAnimator = ValueAnimator.ofFloat(progress, 1f);
                    holdAnimator.setDuration(Math.max(80L, (long) (HOLD_DURATION_MS * (1f - progress))));
                    holdAnimator.setInterpolator(new LinearInterpolator());
                    holdAnimator.addUpdateListener(anim -> {
                        setProgress((float) anim.getAnimatedValue());
                        if (progress >= 1f && !isTriggered) {
                            isTriggered = true;
                            triggerVibration();
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            if (triggeredListener != null) triggeredListener.onTriggered();
                            v.postDelayed(() -> animateCollapse(), 250);
                        }
                    });
                    holdAnimator.start();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    if (holdAnimator != null) holdAnimator.cancel();
                    if (!isTriggered) animateCollapse();
                    return true;
            }
            return true;
        });
    }

    private void animateCollapse() {
        if (progress <= 0f) return;
        if (collapseAnimator != null) collapseAnimator.cancel();
        collapseAnimator = ValueAnimator.ofFloat(progress, 0f);
        collapseAnimator.setDuration(200);
        collapseAnimator.setInterpolator(new DecelerateInterpolator());
        collapseAnimator.addUpdateListener(anim -> setProgress((float) anim.getAnimatedValue()));
        collapseAnimator.start();
    }

    private void triggerVibration() {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(80);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
