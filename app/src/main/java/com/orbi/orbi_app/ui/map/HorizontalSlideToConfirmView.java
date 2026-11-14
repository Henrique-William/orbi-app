package com.orbi.orbi_app.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.orbi.orbi_app.R;

public class HorizontalSlideToConfirmView extends FrameLayout {

    public interface OnSlideConfirmedListener {
        void onConfirmed();
    }

    private View trackView;
    private View progressFillView;
    private FrameLayout thumbView;
    private ImageView ivThumbIcon;
    private TextView tvSlideHint;

    private float startRawX = 0f;
    private float currentTranslationX = 0f;
    private boolean isArmed = false;
    private boolean isDragging = false;
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
        float density = getResources().getDisplayMetrics().density;
        int heightPx = (int) (56 * density);
        int thumbSizePx = (int) (48 * density);
        int marginPx = (int) (4 * density);

        // 1. Trilha de fundo (Flat Design)
        trackView = new View(context);
        trackView.setBackgroundResource(R.drawable.bg_flat_slide_track);
        LayoutParams trackParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        addView(trackView, trackParams);

        // 1b. Preenchimento progressivo que acompanha o dedo até o limite
        progressFillView = new View(context);
        progressFillView.setBackgroundResource(R.drawable.bg_flat_slide_thumb);
        progressFillView.setAlpha(0.22f);
        LayoutParams fillParams = new LayoutParams(thumbSizePx, thumbSizePx);
        fillParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        fillParams.leftMargin = marginPx;
        addView(progressFillView, fillParams);

        // 2. Texto informativo centralizado
        tvSlideHint = new TextView(context);
        tvSlideHint.setText(R.string.slide_to_deliver);
        tvSlideHint.setTextColor(ContextCompat.getColor(context, R.color.nav_action_complete));
        tvSlideHint.setTextSize(14f);
        tvSlideHint.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSlideHint.setGravity(Gravity.CENTER);
        LayoutParams textParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        addView(tvSlideHint, textParams);

        // 3. Thumb / Botão deslizante
        thumbView = new FrameLayout(context);
        thumbView.setBackgroundResource(R.drawable.bg_flat_slide_thumb);
        thumbView.setElevation(2f * density);

        ivThumbIcon = new ImageView(context);
        ivThumbIcon.setImageResource(R.drawable.ic_check);
        ivThumbIcon.setColorFilter(ContextCompat.getColor(context, R.color.white));
        int iconSizePx = (int) (24 * density);
        LayoutParams iconParams = new LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER);
        thumbView.addView(ivThumbIcon, iconParams);

        LayoutParams thumbParams = new LayoutParams(thumbSizePx, thumbSizePx);
        thumbParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        thumbParams.leftMargin = marginPx;
        addView(thumbView, thumbParams);

        setClickable(true);
        setupTouchListener();
    }

    public void setOnSlideConfirmedListener(OnSlideConfirmedListener listener) {
        this.confirmedListener = listener;
    }

    public void setLabel(String label) {
        if (tvSlideHint != null && label != null) {
            tvSlideHint.setText(label);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Permite iniciar o arraste tocando no thumb ou na metade esquerda da barra
            float touchX = ev.getX();
            float thumbRight = thumbView.getLeft() + currentTranslationX + thumbView.getWidth() + (24f * getResources().getDisplayMetrics().density);
            if (touchX <= thumbRight) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleSlideMotion(event);
    }

    private void setupTouchListener() {
        thumbView.setOnTouchListener((v, event) -> handleSlideMotion(event));
    }

    private boolean handleSlideMotion(MotionEvent event) {
        float maxTranslationX = getMaxTranslation();
        if (maxTranslationX <= 0) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (slideAnimator != null && slideAnimator.isRunning()) {
                    slideAnimator.cancel();
                }
                startRawX = event.getRawX() - currentTranslationX;
                isArmed = false;
                isDragging = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;

                // Segue exatamente o movimento do dedo entre 0 (limite esquerdo) e maxTranslationX (limite direito)
                float deltaX = event.getRawX() - startRawX;
                if (deltaX < 0f) deltaX = 0f;
                if (deltaX > maxTranslationX) deltaX = maxTranslationX;

                applyThumbTranslation(deltaX, maxTranslationX);

                float progress = deltaX / maxTranslationX;
                if (progress >= 0.88f) {
                    if (!isArmed) {
                        isArmed = true;
                        triggerHaptic(getContext(), thumbView);
                    }
                } else {
                    isArmed = false;
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                isDragging = false;

                if (isArmed) {
                    triggerHaptic(getContext(), thumbView);
                    animateThumbTo(maxTranslationX, 120, () -> {
                        if (confirmedListener != null) {
                            confirmedListener.onConfirmed();
                        }
                        resetSmoothly();
                    });
                } else {
                    resetSmoothly();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                isDragging = false;
                isArmed = false;
                resetSmoothly();
                return true;
        }
        return false;
    }

    private void applyThumbTranslation(float translationX, float maxTranslationX) {
        currentTranslationX = translationX;
        thumbView.setTranslationX(translationX);

        if (progressFillView != null) {
            ViewGroup.LayoutParams lp = progressFillView.getLayoutParams();
            lp.width = (int) (thumbView.getWidth() + translationX);
            progressFillView.setLayoutParams(lp);
        }

        if (maxTranslationX > 0f && tvSlideHint != null) {
            float progress = translationX / maxTranslationX;
            tvSlideHint.setAlpha(Math.max(0f, 1f - 1.4f * progress));
        }
    }

    private void animateThumbTo(float targetX, long durationMs, Runnable endAction) {
        if (slideAnimator != null && slideAnimator.isRunning()) {
            slideAnimator.cancel();
        }
        final float maxTranslationX = Math.max(1f, getMaxTranslation());
        slideAnimator = ValueAnimator.ofFloat(currentTranslationX, targetX);
        slideAnimator.setDuration(durationMs);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        slideAnimator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            applyThumbTranslation(val, maxTranslationX);
        });
        if (endAction != null) {
            slideAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    endAction.run();
                }
            });
        }
        slideAnimator.start();
    }

    private float getMaxTranslation() {
        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (4 * density);
        return Math.max(0f, getWidth() - thumbView.getWidth() - (2 * marginPx));
    }

    public void resetSmoothly() {
        isArmed = false;
        animateThumbTo(0f, 220, null);
    }

    private void triggerHaptic(Context context, View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
