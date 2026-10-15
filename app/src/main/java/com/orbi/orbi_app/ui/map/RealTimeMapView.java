package com.orbi.orbi_app.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.orbi.orbi_app.R;
import com.orbi.orbi_app.model.Waypoint;

public class RealTimeMapView extends View {

    private Paint backgroundPaint;
    private Paint roadBorderPaint;
    private Paint roadFillPaint;
    private Paint greenAreaPaint;
    private Paint buildingPaint;
    private Paint polylineGlowPaint;
    private Paint polylinePaint;
    private Paint driverPinPaint;
    private Paint driverPulsePaint;
    private Paint destinationPinPaint;
    private Paint pinTextPaint;
    private Paint streetTextPaint;

    private float driverProgress = 0.15f; // Progresso na rota de 0.0 a 1.0
    private float pulseRadius = 30f;
    private ValueAnimator pulseAnimator;
    private ValueAnimator movementAnimator;

    private Waypoint currentTargetWaypoint;

    public RealTimeMapView(Context context) {
        super(context);
        init();
    }

    public RealTimeMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealTimeMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Context context = getContext();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.map_background));

        roadBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadBorderPaint.setColor(ContextCompat.getColor(context, R.color.map_road_border));
        roadBorderPaint.setStyle(Paint.Style.STROKE);

        roadFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadFillPaint.setColor(ContextCompat.getColor(context, R.color.map_road));
        roadFillPaint.setStyle(Paint.Style.STROKE);

        greenAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenAreaPaint.setColor(ContextCompat.getColor(context, R.color.map_green_area));
        greenAreaPaint.setStyle(Paint.Style.FILL);

        buildingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buildingPaint.setColor(ContextCompat.getColor(context, R.color.map_building));
        buildingPaint.setStyle(Paint.Style.FILL);

        polylineGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        polylineGlowPaint.setColor(ContextCompat.getColor(context, R.color.route_polyline_glow));
        polylineGlowPaint.setStyle(Paint.Style.STROKE);
        polylineGlowPaint.setStrokeWidth(24f);
        polylineGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        polylineGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        polylinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        polylinePaint.setColor(ContextCompat.getColor(context, R.color.route_polyline));
        polylinePaint.setStyle(Paint.Style.STROKE);
        polylinePaint.setStrokeWidth(12f);
        polylinePaint.setStrokeCap(Paint.Cap.ROUND);
        polylinePaint.setStrokeJoin(Paint.Join.ROUND);

        driverPinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        driverPinPaint.setColor(ContextCompat.getColor(context, R.color.driver_pin));
        driverPinPaint.setStyle(Paint.Style.FILL);

        driverPulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        driverPulsePaint.setColor(ContextCompat.getColor(context, R.color.driver_pin_pulse));
        driverPulsePaint.setStyle(Paint.Style.FILL);

        destinationPinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        destinationPinPaint.setColor(ContextCompat.getColor(context, R.color.destination_pin));
        destinationPinPaint.setStyle(Paint.Style.FILL);

        pinTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinTextPaint.setColor(Color.WHITE);
        pinTextPaint.setTextSize(26f);
        pinTextPaint.setTextAlign(Paint.Align.CENTER);
        pinTextPaint.setFakeBoldText(true);

        streetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        streetTextPaint.setColor(Color.parseColor("#9AA0A6"));
        streetTextPaint.setTextSize(22f);
        streetTextPaint.setTextAlign(Paint.Align.LEFT);

        startAnimations();
    }

    private void startAnimations() {
        pulseAnimator = ValueAnimator.ofFloat(20f, 50f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        pulseAnimator.addUpdateListener(animation -> {
            pulseRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();

        // Animação suave simulando deslocamento na via
        movementAnimator = ValueAnimator.ofFloat(0.15f, 0.45f);
        movementAnimator.setDuration(8000);
        movementAnimator.setRepeatCount(ValueAnimator.INFINITE);
        movementAnimator.setRepeatMode(ValueAnimator.REVERSE);
        movementAnimator.setInterpolator(new LinearInterpolator());
        movementAnimator.addUpdateListener(animation -> {
            driverProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        movementAnimator.start();
    }

    public void setTargetWaypoint(Waypoint waypoint) {
        this.currentTargetWaypoint = waypoint;
        driverProgress = 0.15f; // Reinicia posição do veículo em relação ao novo destino
        invalidate();
    }

    public void setDirections(com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto directions) {
        if (directions != null && directions.getSegments() != null && !directions.getSegments().isEmpty()) {
            driverProgress = 0.15f;
            invalidate();
        }
    }

    public void recenter() {
        driverProgress = 0.15f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 1. Fundo do Mapa (estilo Google Maps)
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // 2. Parques e Áreas Verdes decorativas
        canvas.drawRoundRect(new RectF(width * 0.05f, height * 0.10f, width * 0.35f, height * 0.25f), 16f, 16f, greenAreaPaint);
        canvas.drawRoundRect(new RectF(width * 0.65f, height * 0.55f, width * 0.95f, height * 0.70f), 16f, 16f, greenAreaPaint);

        // 3. Quarteirões urbanos
        canvas.drawRoundRect(new RectF(width * 0.10f, height * 0.35f, width * 0.35f, height * 0.50f), 12f, 12f, buildingPaint);
        canvas.drawRoundRect(new RectF(width * 0.65f, height * 0.20f, width * 0.90f, height * 0.35f), 12f, 12f, buildingPaint);

        // 4. Malha viária (Ruas e Avenidas)
        drawStreetNetwork(canvas, width, height);

        // 5. Traçado Vetorial da Rota (Polyline em tempo real do motorista até a parada)
        Path polylinePath = new Path();
        float startX = width * 0.50f;
        float startY = height * 0.75f;

        float corner1X = width * 0.50f;
        float corner1Y = height * 0.45f;

        float corner2X = width * 0.75f;
        float corner2Y = height * 0.45f;

        float destX = width * 0.75f;
        float destY = height * 0.25f;

        polylinePath.moveTo(startX, startY);
        polylinePath.lineTo(corner1X, corner1Y);
        polylinePath.lineTo(corner2X, corner2Y);
        polylinePath.lineTo(destX, destY);

        // Brilho externo da rota + Linha sólida
        canvas.drawPath(polylinePath, polylineGlowPaint);
        canvas.drawPath(polylinePath, polylinePaint);

        // 6. Marcador da Próxima Parada (Destino)
        drawDestinationPin(canvas, destX, destY);

        // 7. Posição Atual do Motorista (com animação de pulso e direção)
        float currentX = startX;
        float currentY = startY - (driverProgress * (startY - corner1Y) * 2f);
        if (driverProgress > 0.3f) {
            currentX = corner1X + ((driverProgress - 0.3f) * (corner2X - corner1X) * 2.5f);
            currentY = corner1Y;
        }

        drawDriverVehicle(canvas, currentX, currentY);
    }

    private void drawStreetNetwork(Canvas canvas, int width, int height) {
        float mainRoadWidth = 44f;
        float secondaryRoadWidth = 26f;

        // Vias Horizontais
        drawRoad(canvas, 0, height * 0.25f, width, height * 0.25f, secondaryRoadWidth);
        drawRoad(canvas, 0, height * 0.45f, width, height * 0.45f, mainRoadWidth);
        drawRoad(canvas, 0, height * 0.75f, width, height * 0.75f, mainRoadWidth);

        // Vias Verticais
        drawRoad(canvas, width * 0.20f, 0, width * 0.20f, height, secondaryRoadWidth);
        drawRoad(canvas, width * 0.50f, 0, width * 0.50f, height, mainRoadWidth);
        drawRoad(canvas, width * 0.75f, 0, width * 0.75f, height, secondaryRoadWidth);

        // Rótulos de vias sutis
        canvas.drawText("Av. Principal", width * 0.52f, height * 0.65f, streetTextPaint);
        canvas.drawText("Rua de Acesso", width * 0.22f, height * 0.43f, streetTextPaint);
    }

    private void drawRoad(Canvas canvas, float startX, float startY, float stopX, float stopY, float strokeWidth) {
        roadBorderPaint.setStrokeWidth(strokeWidth + 4f);
        canvas.drawLine(startX, startY, stopX, stopY, roadBorderPaint);

        roadFillPaint.setStrokeWidth(strokeWidth);
        canvas.drawLine(startX, startY, stopX, stopY, roadFillPaint);
    }

    private void drawDriverVehicle(Canvas canvas, float x, float y) {
        // Pulso do GPS
        driverPulsePaint.setAlpha((int) ((1f - (pulseRadius - 20f) / 30f) * 120));
        canvas.drawCircle(x, y, pulseRadius, driverPulsePaint);

        // Círculo branco de contorno
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, 16f, borderPaint);

        // Círculo do veículo
        canvas.drawCircle(x, y, 12f, driverPinPaint);

        // Seta direcional indicando sentido do deslocamento
        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.FILL);

        Path arrowPath = new Path();
        arrowPath.moveTo(x, y - 9f);
        arrowPath.lineTo(x - 5f, y + 4f);
        arrowPath.lineTo(x, y + 1f);
        arrowPath.lineTo(x + 5f, y + 4f);
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);
    }

    private void drawDestinationPin(Canvas canvas, float x, float y) {
        float pinRadius = 24f;

        // Sombra do marcador
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#40000000"));
        canvas.drawOval(new RectF(x - 14f, y + 20f, x + 14f, y + 28f), shadowPaint);

        // Corpo do Pin de entrega
        Path pinPath = new Path();
        pinPath.moveTo(x, y + 24f); // Ponta do pin
        pinPath.cubicTo(x - 22f, y + 8f, x - 24f, y - 16f, x, y - 24f);
        pinPath.cubicTo(x + 24f, y - 16f, x + 22f, y + 8f, x, y + 24f);
        pinPath.close();

        canvas.drawPath(pinPath, destinationPinPaint);

        // Número da sequência ou ícone
        String label = (currentTargetWaypoint != null)
                ? String.valueOf(currentTargetWaypoint.getSequenceOrder()) : "1";
        canvas.drawText(label, x, y - 3f, pinTextPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (movementAnimator != null) movementAnimator.cancel();
    }
}
