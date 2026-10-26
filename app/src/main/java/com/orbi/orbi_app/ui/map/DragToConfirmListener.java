package com.orbi.orbi_app.ui.map;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class DragToConfirmListener implements View.OnTouchListener {

    public interface OnDragConfirmedCallback {
        void onConfirmed();
    }

    private final View dragZoneView;
    private final int normalZoneDrawableRes;
    private final int activeZoneDrawableRes;
    private final OnDragConfirmedCallback callback;

    private float startRawY = 0f;
    private boolean isArmed = false;
    private final float thresholdPx;

    public DragToConfirmListener(View dragZoneView, int normalZoneDrawableRes, int activeZoneDrawableRes, OnDragConfirmedCallback callback) {
        this.dragZoneView = dragZoneView;
        this.normalZoneDrawableRes = normalZoneDrawableRes;
        this.activeZoneDrawableRes = activeZoneDrawableRes;
        this.callback = callback;

        float density = dragZoneView.getContext().getResources().getDisplayMetrics().density;
        this.thresholdPx = 65f * density; // Limite vertical em pixels para sair da área demarcada
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startRawY = event.getRawY();
                isArmed = false;
                view.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).start();
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getRawY() - startRawY;

                // Permite apenas arrastar para CIMA (deltaY negativo)
                if (deltaY < 0) {
                    view.setTranslationY(deltaY);

                    if (Math.abs(deltaY) >= thresholdPx) {
                        if (!isArmed) {
                            isArmed = true;
                            triggerVibration(view.getContext(), view);
                            dragZoneView.setBackgroundResource(activeZoneDrawableRes);
                        }
                    } else {
                        if (isArmed) {
                            isArmed = false;
                            dragZoneView.setBackgroundResource(normalZoneDrawableRes);
                        }
                    }
                } else {
                    view.setTranslationY(0f);
                }
                return true;

            case MotionEvent.ACTION_UP:
                view.getParent().requestDisallowInterceptTouchEvent(false);
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();

                if (isArmed) {
                    triggerVibration(view.getContext(), view);
                    view.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                dragZoneView.setBackgroundResource(normalZoneDrawableRes);
                                if (callback != null) {
                                    callback.onConfirmed();
                                }
                            })
                            .start();
                } else {
                    // Retorna com animação elástica suave à base
                    view.animate()
                            .translationY(0f)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .setDuration(280)
                            .withEndAction(() -> dragZoneView.setBackgroundResource(normalZoneDrawableRes))
                            .start();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                view.getParent().requestDisallowInterceptTouchEvent(false);
                isArmed = false;
                dragZoneView.setBackgroundResource(normalZoneDrawableRes);
                view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationY(0f)
                        .setDuration(200)
                        .start();
                return true;
        }
        return false;
    }

    private void triggerVibration(Context context, View view) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(55);
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        } catch (Exception e) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }
}
