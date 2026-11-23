package com.orbi.orbi_app.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.orbi.orbi_app.R;

/**
 * Slide-to-confirm (Orbi Design System · .m3-slide-track).
 * Ao arrastar o thumb da esquerda para a direita, uma caixa sólida acompanha o dedo
 * e preenche a trilha. Soltando após 90% do curso, a ação é confirmada.
 */
public class HorizontalSlideToConfirmView extends FrameLayout {

    public interface OnSlideConfirmedListener {
        void onConfirmed();
    }

    private static final float CONFIRM_THRESHOLD = 0.9f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textOnFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF fillRect = new RectF();
    private final Path trackClip = new Path();

    private FrameLayout thumbView;
    private String label;
    private float marginPx;
    private float strokePx;

    private float startRawX = 0f;
    private float currentTranslationX = 0f;
    private boolean isArmed = false;
    private boolean isDragging = false;
    private boolean isLocked = false;
    private ValueAnimator slideAnimator;
    private OnSlideConfirmedListener confirmedListener;

    public HorizontalSlideToConfirmView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public HorizontalSlideToConfirmView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalSlideToConfirmView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setClickable(true);
        float density = getResources().getDisplayMetrics().density;
        marginPx = 4f * density;
        strokePx = 1f * density;

        int ink = ContextCompat.getColor(context, R.color.nav_action_complete);
        int surface = ContextCompat.getColor(context, R.color.surface);

        trackPaint.setColor(ContextCompat.getColor(context, R.color.surface_container));
        trackStrokePaint.setStyle(Paint.Style.STROKE);
        trackStrokePaint.setStrokeWidth(strokePx);
        trackStrokePaint.setColor(ContextCompat.getColor(context, R.color.outline_variant));
        fillPaint.setColor(ink);

        textPaint.setColor(ContextCompat.getColor(context, R.color.ink_secondary));
        textPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textOnFillPaint.set(textPaint);
        textOnFillPaint.setColor(surface);

        label = context.getString(R.string.slide_to_deliver);
        setContentDescription(label);

        // Thumb: círculo claro com seta, que fica sobre a caixa escura de preenchimento
        thumbView = new FrameLayout(context);
        thumbView.setBackgroundResource(R.drawable.bg_flat_slide_thumb);
        thumbView.setElevation(2f * density);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_chevron_right);
        icon.setColorFilter(ink);
        int iconSize = (int) (24 * density);
        thumbView.addView(icon, new LayoutParams(iconSize, iconSize, Gravity.CENTER));

        int thumbSize = (int) (48 * density);
        LayoutParams thumbParams = new LayoutParams(thumbSize, thumbSize);
        thumbParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        thumbParams.leftMargin = (int) marginPx;
        addView(thumbView, thumbParams);
    }

    public void setOnSlideConfirmedListener(OnSlideConfirmedListener listener) {
        this.confirmedListener = listener;
    }

    public void setLabel(String label) {
        if (label != null) {
            this.label = label;
            setContentDescription(label);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackRect.set(0, 0, w, h);
        trackClip.reset();
        trackClip.addRoundRect(trackRect, h / 2f, h / 2f, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float r = h / 2f;
        canvas.drawRoundRect(trackRect, r, r, trackPaint);

        float textX = getWidth() / 2f + thumbView.getWidth() / 4f;
        float textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        float maxT = Math.max(1f, getMaxTranslation());
        textPaint.setAlpha((int) (255 * Math.max(0.35f, 1f - currentTranslationX / maxT)));
        canvas.drawText(label, textX, textY, textPaint);

        // Caixa que acompanha o dedo: vai da borda esquerda até a borda direita do thumb
        float fillRight = thumbView.getLeft() + currentTranslationX + thumbView.getWidth() + marginPx;
        if (currentTranslationX > 0.5f) {
            canvas.save();
            canvas.clipPath(trackClip);
            fillRect.set(0, 0, fillRight, h);
            canvas.drawRoundRect(fillRect, r, r, fillPaint);
            canvas.clipRect(fillRect);
            canvas.drawText(label, textX, textY, textOnFillPaint);
            canvas.restore();
        }

        float half = strokePx / 2f;
        canvas.drawRoundRect(half, half, getWidth() - half, h - half, r - half, r - half, trackStrokePaint);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLocked) return true;
        float maxT = getMaxTranslation();
        if (maxT <= 0) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float thumbStart = thumbView.getLeft() + currentTranslationX;
                float slop = 16f * getResources().getDisplayMetrics().density;
                if (event.getX() < thumbStart - slop || event.getX() > thumbStart + thumbView.getWidth() + slop) {
                    // Toque fora do thumb: pequeno "nudge" indicando que é preciso arrastar
                    nudge();
                    return true;
                }
                if (slideAnimator != null) slideAnimator.cancel();
                startRawX = event.getRawX() - currentTranslationX;
                isArmed = false;
                isDragging = true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                thumbView.animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).start();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!isDragging) return true;
                float dx = Math.max(0f, Math.min(maxT, event.getRawX() - startRawX));
                applyTranslation(dx);
                boolean armedNow = dx / maxT >= CONFIRM_THRESHOLD;
                if (armedNow && !isArmed) triggerHaptic();
                isArmed = armedNow;
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                thumbView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (!isDragging) return true;
                isDragging = false;
                if (isArmed && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    isLocked = true;
                    animateTo(maxT, 120, () -> {
                        if (confirmedListener != null) confirmedListener.onConfirmed();
                        postDelayed(this::resetSmoothly, 350);
                    });
                } else {
                    resetSmoothly();
                }
                return true;
            }
        }
        return true;
    }

    private void nudge() {
        if (isDragging || (slideAnimator != null && slideAnimator.isRunning())) return;
        float d = 14f * getResources().getDisplayMetrics().density;
        animateTo(d, 120, () -> animateTo(0f, 220, null));
    }

    private void applyTranslation(float x) {
        currentTranslationX = x;
        thumbView.setTranslationX(x);
        invalidate();
    }

    private void animateTo(float target, long duration, @Nullable Runnable end) {
        if (slideAnimator != null) slideAnimator.cancel();
        slideAnimator = ValueAnimator.ofFloat(currentTranslationX, target);
        slideAnimator.setDuration(duration);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        slideAnimator.addUpdateListener(a -> applyTranslation((float) a.getAnimatedValue()));
        if (end != null) {
            slideAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                private boolean cancelled = false;

                @Override
                public void onAnimationCancel(android.animation.Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (!cancelled) end.run();
                }
            });
        }
        slideAnimator.start();
    }

    private float getMaxTranslation() {
        return Math.max(0f, getWidth() - thumbView.getWidth() - 2 * marginPx);
    }

    public void resetSmoothly() {
        isArmed = false;
        isLocked = false;
        animateTo(0f, 240, null);
    }

    private void triggerHaptic() {
        View v = thumbView;
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(40);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
