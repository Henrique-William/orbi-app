package com.orbi.orbi_app.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

public class RadialHoldButton extends FrameLayout {

    public interface OnHoldTriggeredListener {
        void onTriggered();
    }

    private static final long HOLD_DURATION_MS = 3000L;

    private int neutralBgColor;
    private int neutralTextColor;
    private int radialColor;
    private float cornerRadiusPx;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint radialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF boundsRect = new RectF();

    private float radialProgress = 0f; // 0.0f a 1.0f
    private ValueAnimator holdAnimator;
    private ValueAnimator collapseAnimator;
    private boolean isTriggered = false;

    private ImageView ivIcon;
    private TextView tvLabel;
    private OnHoldTriggeredListener triggeredListener;

    public RadialHoldButton(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RadialHoldButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RadialHoldButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        cornerRadiusPx = 14f * density;

        // Cores neutras padrão
        neutralBgColor = ContextCompat.getColor(context, R.color.surface_container_high);
        neutralTextColor = ContextCompat.getColor(context, R.color.on_surface);
        radialColor = ContextCompat.getColor(context, R.color.primary);

        bgPaint.setColor(neutralBgColor);
        bgPaint.setStyle(Paint.Style.FILL);

        radialPaint.setColor(radialColor);
        radialPaint.setStyle(Paint.Style.FILL);

        // Layout de conteúdo (ícone e texto limpos)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER);
        LayoutParams contentParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER;
        addView(contentLayout, contentParams);

        ivIcon = new ImageView(context);
        int iconSize = (int) (20 * density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.rightMargin = (int) (8 * density);
        ivIcon.setLayoutParams(iconParams);
        contentLayout.addView(ivIcon);

        tvLabel = new TextView(context);
        tvLabel.setTextSize(15f);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setTextColor(neutralTextColor);
        contentLayout.addView(tvLabel);

        setupTouch();
    }

    public void setButtonConfig(int iconResId, String text, int radialColor) {
        this.radialColor = radialColor;
        this.radialPaint.setColor(radialColor);

        if (ivIcon != null && iconResId != 0) {
            ivIcon.setImageResource(iconResId);
            ivIcon.setColorFilter(radialColor);
        }
        if (tvLabel != null && text != null) {
            tvLabel.setText(text);
            tvLabel.setTextColor(radialColor);
        }
        invalidate();
    }

    public void setOnHoldTriggeredListener(OnHoldTriggeredListener listener) {
        this.triggeredListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boundsRect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(boundsRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(clipPath);

        // 1. Fundo neutro
        canvas.drawRoundRect(boundsRect, cornerRadiusPx, cornerRadiusPx, bgPaint);

        // 2. Preenchimento radial expandindo do centro para as extremidades
        if (radialProgress > 0f) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float maxRadius = (float) Math.hypot(cx, cy);
            float currentRadius = maxRadius * radialProgress;

            canvas.drawCircle(cx, cy, currentRadius, radialPaint);
        }

        // 3. Atualiza cor do texto/ícone conforme o progresso para contraste perfeito
        if (radialProgress > 0.45f) {
            tvLabel.setTextColor(Color.WHITE);
            ivIcon.setColorFilter(Color.WHITE);
        } else {
            tvLabel.setTextColor(radialColor);
            ivIcon.setColorFilter(radialColor);
        }

        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private void setupTouch() {
        setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isTriggered = false;
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

                    if (collapseAnimator != null && collapseAnimator.isRunning()) {
                        collapseAnimator.cancel();
                    }
                    if (holdAnimator != null && holdAnimator.isRunning()) {
                        holdAnimator.cancel();
                    }

                    holdAnimator = ValueAnimator.ofFloat(radialProgress, 1.0f);
                    long remainingTime = (long) (HOLD_DURATION_MS * (1.0f - radialProgress));
                    holdAnimator.setDuration(Math.max(100, remainingTime));
                    holdAnimator.setInterpolator(new LinearInterpolator());
                    holdAnimator.addUpdateListener(anim -> {
                        radialProgress = (float) anim.getAnimatedValue();
                        invalidate();

                        if (radialProgress >= 1.0f && !isTriggered) {
                            isTriggered = true;
                            triggerVibration();
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                            radialProgress = 0f;
                            invalidate();

                            if (triggeredListener != null) {
                                triggeredListener.onTriggered();
                            }
                        }
                    });
                    holdAnimator.start();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    if (holdAnimator != null) {
                        holdAnimator.cancel();
                    }

                    if (!isTriggered && radialProgress > 0f) {
                        collapseAnimator = ValueAnimator.ofFloat(radialProgress, 0f);
                        collapseAnimator.setDuration(180);
                        collapseAnimator.setInterpolator(new DecelerateInterpolator());
                        collapseAnimator.addUpdateListener(anim -> {
                            radialProgress = (float) anim.getAnimatedValue();
                            invalidate();
                        });
                        collapseAnimator.start();
                    }
                    return true;
            }
            return false;
        });
    }

    private void triggerVibration() {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(140);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
