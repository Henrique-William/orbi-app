package com.orbi.orbi_app.ui.map;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.AddressGeocoder;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto;
import com.orbi.orbi_app.data.remote.dto.GeometryDto;
import com.orbi.orbi_app.data.remote.dto.SegmentDto;
import com.orbi.orbi_app.data.remote.dto.StepDto;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.model.WaypointStatus;
import com.orbi.orbi_app.ui.home.HomeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NavigationMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private View rootLayout;
    private MaterialCardView cardTopBanner;
    private MaterialCardView cardBottomControls;
    private MapView mapView;
    private GoogleMap googleMap;
    private MaterialButton btnCloseNav;

    // Banner Superior de Manobra (Padrão Uber Driver)
    private ImageView ivManeuverIcon;
    private TextView tvManeuverDistance;
    private TextView tvManeuverInstruction;
    private View layoutNextManeuver;
    private ImageView ivNextManeuverIcon;
    private TextView tvNextManeuverInstruction;
    private TextView tvNavHeading;
    private TextView tvGpsStatus;
    private View viewGpsDot;

    // Velocímetro e Controles Flutuantes do Mapa
    private TextView tvCurrentSpeed;
    private FloatingActionButton fabRouteOverview;
    private FloatingActionButton fabRecenter;
    private boolean isOverviewMode = false;

    // Painel Inferior Ancorado
    private View layoutSheetContent;
    private View layoutSheetHeader;
    private ImageView ivSheetChevron;
    private TextView tvNavSequence;
    private TextView tvNavRecipient;
    private TextView tvNavAddress;
    private TextView tvNavDistance;
    private TextView tvNavDuration;
    private TextView tvNavArrivalTime;
    private MaterialButton btnCallRecipient;

    // Painel Inferior Expansível, Detalhes da Rota e Ações Flat Design
    private FrameLayout layoutDragBadge;
    private LinearLayout layoutExpandableContent;
    private TextView tvExpandedRouteCode;
    private TextView tvExpandedStopStage;
    private TextView tvExpandedRouteProgress;
    private LinearProgressIndicator progressRouteStops;
    private TextView tvExpandedNextStop;
    private RadialHoldButton btnRadialSkip;
    private RadialHoldButton btnRadialCancel;
    private HorizontalSlideToConfirmView slideConcludeDelivery;
    private boolean isSheetExpanded = false;
    private ValueAnimator sheetAnimator;

    // Dados e Estado
    private RouteRepository repository;
    private String routeId;
    private Route currentRoute;
    private Waypoint currentWaypoint;

    // Camadas do Google Maps & Motor Uber Directions
    private Marker driverMarker;
    private ValueAnimator puckAnimator;
    private Marker destinationMarker;
    private Circle destinationHaloCircle;
    private final List<Marker> routeStopMarkers = new ArrayList<>();
    private Polyline routePolyline;
    private Polyline routePolylineCasing;
    private Polyline curbWalkingPolyline;
    private LatLng currentDriverLatLng;
    private LatLng currentTargetLatLng;
    private boolean isFollowingDriver = true;
    private List<LatLng> currentRoutePoints;
    private final List<RouteStep> currentRouteSteps = new ArrayList<>();
    private int currentStepIndex = 0;
    private double currentRouteTotalDistanceMeters = 0.0;
    private double currentRouteTotalDurationSeconds = 0.0;
    private long lastRouteCalculationTime = 0;
    private LatLng lastRouteOrigin = null;
    private boolean isRealGpsAcquired = false;

    // Sensores: GPS e Orientação/Bússola
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTrackingLocation = false;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private SensorEventListener sensorListener;
    private float currentAzimuth = 0f;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_map);

        repository = RouteRepository.getInstance();
        routeId = getIntent().getStringExtra("EXTRA_ROUTE_ID");

        initViews();
        setupWindowInsets();
        setupDragActionButtons();
        setupOrientationSensor();
        setupLocationClient();
        setupListeners();

        // Inicialização do Google Maps MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        loadCurrentNavigationTarget();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        cardTopBanner = findViewById(R.id.cardTopBanner);
        cardBottomControls = findViewById(R.id.cardBottomControls);
        mapView = findViewById(R.id.mapView);
        btnCloseNav = findViewById(R.id.btnCloseNav);

        ivManeuverIcon = findViewById(R.id.ivManeuverIcon);
        tvManeuverDistance = findViewById(R.id.tvManeuverDistance);
        tvManeuverInstruction = findViewById(R.id.tvManeuverInstruction);
        layoutNextManeuver = findViewById(R.id.layoutNextManeuver);
        ivNextManeuverIcon = findViewById(R.id.ivNextManeuverIcon);
        tvNextManeuverInstruction = findViewById(R.id.tvNextManeuverInstruction);
        tvNavHeading = findViewById(R.id.tvNavHeading);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        viewGpsDot = findViewById(R.id.viewGpsDot);

        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        fabRouteOverview = findViewById(R.id.fabRouteOverview);
        fabRecenter = findViewById(R.id.fabRecenter);
        layoutSheetContent = findViewById(R.id.layoutSheetContent);
        layoutSheetHeader = findViewById(R.id.layoutSheetHeader);
        ivSheetChevron = findViewById(R.id.ivSheetChevron);

        // Alça e Conteúdo Expansível
        layoutDragBadge = findViewById(R.id.layoutDragBadge);
        layoutExpandableContent = findViewById(R.id.layoutExpandableContent);

        // Resumo detalhado da rota (dentro do conteúdo expansível)
        tvExpandedRouteCode = findViewById(R.id.tvExpandedRouteCode);
        tvExpandedStopStage = findViewById(R.id.tvExpandedStopStage);
        tvExpandedRouteProgress = findViewById(R.id.tvExpandedRouteProgress);
        progressRouteStops = findViewById(R.id.progressRouteStops);
        tvExpandedNextStop = findViewById(R.id.tvExpandedNextStop);

        // Métricas e Endereço (Sempre Visíveis durante a rota)
        tvNavSequence = findViewById(R.id.tvNavSequence);
        tvNavAddress = findViewById(R.id.tvNavAddress);
        tvNavDistance = findViewById(R.id.tvNavDistance);
        tvNavDuration = findViewById(R.id.tvNavDuration);
        tvNavArrivalTime = findViewById(R.id.tvNavArrivalTime);

        // Informações do Destinatário (Dentro do conteúdo expansível)
        tvNavRecipient = findViewById(R.id.tvNavRecipient);
        btnCallRecipient = findViewById(R.id.btnCallRecipient);

        // Botões de Ação Flat (Dentro do conteúdo expansível)
        btnRadialSkip = findViewById(R.id.btnRadialSkip);
        btnRadialCancel = findViewById(R.id.btnRadialCancel);
        slideConcludeDelivery = findViewById(R.id.slideConcludeDelivery);

        blockTouchPassThrough(cardBottomControls);
        blockTouchPassThrough(cardTopBanner);
        blockTouchPassThrough(findViewById(R.id.cardSpeedometer));
    }

    /**
     * Impede que toques em áreas "vazias" dos painéis sobrepostos (padding, textos, divisores)
     * atravessem a UI e movam o mapa por baixo. Os filhos interativos continuam recebendo
     * os eventos normalmente, pois o listener só é acionado quando nenhum filho consome o toque.
     */
    private void blockTouchPassThrough(View overlay) {
        if (overlay == null) return;
        overlay.setOnTouchListener((v, event) -> true);
    }

    private void setupWindowInsets() {
        final int topSpacing = dpToPx(12);
        final int sheetBottomPadding = layoutSheetContent.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (cardTopBanner.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams topParams = (ViewGroup.MarginLayoutParams) cardTopBanner.getLayoutParams();
                topParams.topMargin = systemBars.top + topSpacing;
                cardTopBanner.setLayoutParams(topParams);
            }

            // O painel inferior é ancorado na borda: a barra de navegação vira padding interno
            layoutSheetContent.setPadding(
                    layoutSheetContent.getPaddingLeft(),
                    layoutSheetContent.getPaddingTop(),
                    layoutSheetContent.getPaddingRight(),
                    sheetBottomPadding + systemBars.bottom);

            return insets;
        });

        // Mantém a área útil do mapa (câmera e logo do Google) entre os painéis sobrepostos
        View.OnLayoutChangeListener overlayListener = (v, l, t, r, b, ol, ot, or, ob) -> {
            if (sheetAnimator == null || !sheetAnimator.isRunning()) {
                updateMapPadding();
            }
        };
        cardTopBanner.addOnLayoutChangeListener(overlayListener);
        cardBottomControls.addOnLayoutChangeListener(overlayListener);
    }

    private void updateMapPadding() {
        if (googleMap == null || cardTopBanner.getHeight() == 0 || cardBottomControls.getHeight() == 0) return;
        int topBannerBottom = cardTopBanner.getBottom();
        int bottomSheetTop = Math.max(0, rootLayout.getHeight() - cardBottomControls.getTop());
        if (isOverviewMode) {
            googleMap.setPadding(dpToPx(24), topBannerBottom + dpToPx(16), dpToPx(24), bottomSheetTop + dpToPx(16));
        } else {
            int availableHeight = Math.max(0, rootLayout.getHeight() - topBannerBottom - bottomSheetTop);
            // Desloca o centro focal da câmera para o terço inferior da área visível (visão Uber Driver 3D)
            int lookAheadOffset = (int) (availableHeight * 0.34f);
            googleMap.setPadding(0, topBannerBottom + lookAheadOffset, 0, bottomSheetTop);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setGpsStatus(@androidx.annotation.StringRes int textRes, int dotColor) {
        if (tvGpsStatus != null) {
            tvGpsStatus.setText(textRes);
        }
        if (viewGpsDot != null) {
            viewGpsDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));
        }
    }

    /**
     * Configura o painel inferior expansível e as ações em Flat Design:
     * - Durante a rota, exibe apenas métricas e endereço.
     * - Ao arrastar a alça para cima ou tocar, revela com animação suave as informações do cliente e botões.
     * - Botões neutros Pular e Cancelar com expansão radial colorida em 3 segundos.
     * - Slide horizontal para conclusão com vibração tátil.
     */
    private void setupDragActionButtons() {
        if (btnRadialSkip != null) {
            btnRadialSkip.setButtonConfig(
                    R.drawable.ic_skip,
                    getString(R.string.btn_skip_stop),
                    ContextCompat.getColor(this, R.color.nav_action_skip)
            );
            btnRadialSkip.setOnHoldTriggeredListener(this::handleSkipStop);
        }

        if (btnRadialCancel != null) {
            btnRadialCancel.setButtonConfig(
                    R.drawable.ic_cancel,
                    getString(R.string.btn_cancel_stop),
                    ContextCompat.getColor(this, R.color.nav_action_cancel)
            );
            btnRadialCancel.setOnHoldTriggeredListener(this::handleCancelStop);
        }

        if (slideConcludeDelivery != null) {
            slideConcludeDelivery.setOnSlideConfirmedListener(this::handleConcludeStop);
        }

        setupExpandableSheetGesture();
    }

    private void setupExpandableSheetGesture() {
        if (layoutExpandableContent == null) return;

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        View.OnTouchListener headerTouchListener = new View.OnTouchListener() {
            private float downRawY = 0f;
            private float downRawX = 0f;
            private int startHeight = 0;
            private int maxExpandedHeight = 0;
            private int currentDraggedHeight = 0;
            private boolean isDraggingVertical = false;
            private boolean limitHapticFired = false;
            private VelocityTracker velocityTracker;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (sheetAnimator != null && sheetAnimator.isRunning()) {
                            sheetAnimator.cancel();
                        }
                        updateExpandedRouteDetails();
                        maxExpandedHeight = measureExpandableContentHeight();
                        startHeight = (layoutExpandableContent.getVisibility() == View.VISIBLE)
                                ? (layoutExpandableContent.getHeight() > 0 ? layoutExpandableContent.getHeight() : maxExpandedHeight)
                                : 0;
                        currentDraggedHeight = startHeight;
                        downRawY = event.getRawY();
                        downRawX = event.getRawX();
                        isDraggingVertical = false;
                        limitHapticFired = false;
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        } else {
                            velocityTracker.clear();
                        }
                        velocityTracker.addMovement(event);
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (velocityTracker != null) {
                            velocityTracker.addMovement(event);
                        }
                        float dy = event.getRawY() - downRawY;
                        float dx = event.getRawX() - downRawX;

                        if (!isDraggingVertical && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                            isDraggingVertical = true;
                            if (maxExpandedHeight <= 0) {
                                maxExpandedHeight = measureExpandableContentHeight();
                            }
                        }

                        if (isDraggingVertical && maxExpandedHeight > 0) {
                            // O painel acompanha 1:1 o movimento vertical do dedo entre 0 (fechado) e maxExpandedHeight (limite aberto)
                            int newHeight = Math.round(startHeight - dy);
                            if (newHeight <= 0) {
                                newHeight = 0;
                                if (!limitHapticFired && startHeight > 0) {
                                    limitHapticFired = true;
                                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                                }
                            } else if (newHeight >= maxExpandedHeight) {
                                newHeight = maxExpandedHeight;
                                if (!limitHapticFired && startHeight < maxExpandedHeight) {
                                    limitHapticFired = true;
                                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                                }
                            } else {
                                limitHapticFired = false;
                            }

                            currentDraggedHeight = newHeight;
                            float progress = newHeight / (float) maxExpandedHeight;

                            if (newHeight > 0 && layoutExpandableContent.getVisibility() != View.VISIBLE) {
                                layoutExpandableContent.setVisibility(View.VISIBLE);
                            }
                            layoutExpandableContent.getLayoutParams().height = newHeight;
                            layoutExpandableContent.setAlpha(progress);
                            layoutExpandableContent.requestLayout();

                            if (ivSheetChevron != null) {
                                ivSheetChevron.setRotation(180f * progress);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float totalDy = event.getRawY() - downRawY;
                        float totalDx = event.getRawX() - downRawX;

                        float yVelocity = 0f;
                        if (velocityTracker != null) {
                            velocityTracker.addMovement(event);
                            velocityTracker.computeCurrentVelocity(1000);
                            yVelocity = velocityTracker.getYVelocity();
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }

                        if (isDraggingVertical && maxExpandedHeight > 0) {
                            boolean shouldExpand;
                            if (yVelocity < -380f) {
                                shouldExpand = true;
                            } else if (yVelocity > 380f) {
                                shouldExpand = false;
                            } else {
                                shouldExpand = currentDraggedHeight >= (maxExpandedHeight * 0.45f);
                            }
                            animateSheetToState(shouldExpand, currentDraggedHeight, maxExpandedHeight);
                        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                                && Math.abs(totalDy) < touchSlop * 1.5f
                                && Math.abs(totalDx) < touchSlop * 1.5f) {
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            v.performClick();
                            toggleSheetExpansion(!isSheetExpanded);
                        }
                        isDraggingVertical = false;
                        return true;
                }
                return false;
            }
        };

        if (layoutSheetHeader != null) {
            layoutSheetHeader.setOnTouchListener(headerTouchListener);
        }
        if (layoutDragBadge != null) {
            layoutDragBadge.setOnTouchListener(headerTouchListener);
        }
        View layoutAddressInfo = findViewById(R.id.layoutAddressInfo);
        if (layoutAddressInfo != null) {
            layoutAddressInfo.setOnTouchListener(headerTouchListener);
        }
    }

    private int measureExpandableContentHeight() {
        if (cardBottomControls == null || layoutExpandableContent == null || layoutSheetContent == null) return 0;
        int availableWidth = cardBottomControls.getWidth()
                - layoutSheetContent.getPaddingLeft()
                - layoutSheetContent.getPaddingRight();
        if (availableWidth <= 0) return 0;

        layoutExpandableContent.measure(
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        return layoutExpandableContent.getMeasuredHeight();
    }

    private void toggleSheetExpansion(boolean expand) {
        if (cardBottomControls == null || layoutExpandableContent == null) return;
        updateExpandedRouteDetails();
        int maxExpandedHeight = measureExpandableContentHeight();
        int fromHeight = (layoutExpandableContent.getVisibility() == View.VISIBLE)
                ? (layoutExpandableContent.getHeight() > 0 ? layoutExpandableContent.getHeight() : (isSheetExpanded ? maxExpandedHeight : 0))
                : 0;
        animateSheetToState(expand, fromHeight, maxExpandedHeight);
    }

    private void animateSheetToState(boolean expand, int fromHeight, int maxExpandedHeight) {
        isSheetExpanded = expand;

        if (sheetAnimator != null && sheetAnimator.isRunning()) {
            sheetAnimator.cancel();
        }

        if (maxExpandedHeight <= 0) {
            layoutExpandableContent.setVisibility(expand ? View.VISIBLE : View.GONE);
            layoutExpandableContent.setAlpha(expand ? 1f : 0f);
            layoutExpandableContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutExpandableContent.requestLayout();
            if (ivSheetChevron != null) {
                ivSheetChevron.setRotation(expand ? 180f : 0f);
            }
            updateMapPadding();
            return;
        }

        final int targetHeight = expand ? maxExpandedHeight : 0;
        if (expand && layoutExpandableContent.getVisibility() != View.VISIBLE) {
            layoutExpandableContent.getLayoutParams().height = Math.max(0, fromHeight);
            layoutExpandableContent.setAlpha(fromHeight / (float) maxExpandedHeight);
            layoutExpandableContent.setVisibility(View.VISIBLE);
            layoutExpandableContent.requestLayout();
        }

        long duration = Math.max(120L, Math.min(260L,
                (long) (260f * (Math.abs(targetHeight - fromHeight) / (float) maxExpandedHeight))));

        sheetAnimator = ValueAnimator.ofInt(fromHeight, targetHeight);
        sheetAnimator.setDuration(duration);
        sheetAnimator.setInterpolator(new FastOutSlowInInterpolator());
        sheetAnimator.addUpdateListener(anim -> {
            int h = (int) anim.getAnimatedValue();
            float progress = h / (float) maxExpandedHeight;
            layoutExpandableContent.getLayoutParams().height = h;
            layoutExpandableContent.setAlpha(progress);
            layoutExpandableContent.requestLayout();
            if (ivSheetChevron != null) {
                ivSheetChevron.setRotation(180f * progress);
            }
        });
        sheetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (expand) {
                    layoutExpandableContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    layoutExpandableContent.setAlpha(1f);
                    layoutExpandableContent.setVisibility(View.VISIBLE);
                } else {
                    layoutExpandableContent.setVisibility(View.GONE);
                    layoutExpandableContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    layoutExpandableContent.setAlpha(1f);
                }
                if (ivSheetChevron != null) {
                    ivSheetChevron.setRotation(expand ? 180f : 0f);
                }
                layoutExpandableContent.requestLayout();
                cardBottomControls.post(() -> updateMapPadding());
            }
        });
        sheetAnimator.start();
    }

    private void setupOrientationSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                        float[] rotationMatrix = new float[9];
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                        float[] orientationValues = new float[3];
                        SensorManager.getOrientation(rotationMatrix, orientationValues);
                        float azimuthDeg = (float) Math.toDegrees(orientationValues[0]);
                        if (azimuthDeg < 0) azimuthDeg += 360f;
                        onDeviceOrientationChanged(azimuthDeg);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
        }
    }

    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    onDriverLocationUpdated(loc);
                }
            }
        };

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                    if ((fine != null && fine) || (coarse != null && coarse)) {
                        startLocationTracking();
                    } else {
                        setGpsStatus(R.string.nav_gps_demo, ContextCompat.getColor(this, R.color.nav_action_skip));
                        useFallbackDriverPosition();
                    }
                }
        );
    }

    private void setupListeners() {
        btnCloseNav.setOnClickListener(v -> finish());

        fabRecenter.setOnClickListener(v -> {
            isOverviewMode = false;
            isFollowingDriver = true;
            updateMapPadding();
            recenterOnDriver();
        });

        if (fabRouteOverview != null) {
            fabRouteOverview.setOnClickListener(v -> toggleRouteOverview());
        }

        btnCallRecipient.setOnClickListener(v -> {
            if (currentWaypoint != null && !TextUtils.isEmpty(currentWaypoint.getRecipientPhone())) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentWaypoint.getRecipientPhone()));
                startActivity(dialIntent);
            } else {
                Toast.makeText(this, "Telefone do destinatário não cadastrado.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Mapa limpo em 2D (sem prédios 3D extrudados ou mapas internos)
        googleMap.setBuildingsEnabled(false);
        googleMap.setIndoorEnabled(false);

        // Configurações visuais e de controles
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);

        // Aplica o estilo personalizado dos JSONs anexos
        applyCustomMapStyle();
        updateMapPadding();
        hideGoogleWatermark();
        mapView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> removeGoogleLogoView(mapView));

        // Detecta se o usuário arrastou o mapa manualmente para pausar o acompanhamento automático
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingDriver = false;
                if (fabRecenter != null) {
                    fabRecenter.show();
                }
            }
        });

        // Inicializa marcadores se a parada e o motorista já estiverem prontos
        if (currentTargetLatLng != null) {
            updateDestinationMarker();
        }
        if (currentDriverLatLng != null) {
            updateDriverMarker();
            recenterOnDriver();
        }

        // Renderiza imediatamente a rota se já estiver calculada, ou calcula agora
        if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
            drawSolidPolyline(currentRoutePoints);
        } else if (currentDriverLatLng != null && currentTargetLatLng != null) {
            calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
        }
    }

    private void applyCustomMapStyle() {
        if (googleMap == null) return;

        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int styleResource = (nightMode == Configuration.UI_MODE_NIGHT_YES) ? R.raw.map_style_dark : R.raw.map_style;

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, styleResource));
            if (!success) {
                // Tenta estilo claro se o escuro falhar
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadCurrentNavigationTarget() {
        if (routeId == null) {
            finish();
            return;
        }

        currentRoute = repository.getRouteById(routeId);
        if (currentRoute == null || currentRoute.getWaypoints() == null || currentRoute.getWaypoints().isEmpty()) {
            // Busca diretamente as paradas de GET /api/deliveries/route/{routeId}
            repository.fetchDeliveriesByRouteIdFromApi(this, routeId, new RepositoryCallback<List<Waypoint>>() {
                @Override
                public void onSuccess(List<Waypoint> waypoints) {
                    currentRoute = repository.getRouteById(routeId);
                    displayCurrentNavigationTarget();
                }

                @Override
                public void onError(String message) {
                    displayCurrentNavigationTarget();
                }
            });
            return;
        }

        displayCurrentNavigationTarget();

        // Atualização em background das entregas para garantir sincronização contínua com a API
        repository.fetchDeliveriesByRouteIdFromApi(this, routeId, new RepositoryCallback<List<Waypoint>>() {
            @Override
            public void onSuccess(List<Waypoint> waypoints) {
                if (currentWaypoint == null || !currentWaypoint.getFullAddress().equals(
                        currentRoute.getNextPendingWaypoint() != null ? currentRoute.getNextPendingWaypoint().getFullAddress() : "")) {
                    displayCurrentNavigationTarget();
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void displayCurrentNavigationTarget() {
        if (currentRoute == null) {
            finish();
            return;
        }

        currentWaypoint = currentRoute.getNextPendingWaypoint();
        if (currentWaypoint == null) {
            handleAllStopsHandledOrNextRoute();
            return;
        }

        // Recolhe o painel ao carregar uma nova parada para focar nas métricas e endereço
        if (isSheetExpanded) {
            toggleSheetExpansion(false);
        }

        // 1. Atualizar Informações do Cockpit Inferior e Resumo Expandido da Rota
        tvNavSequence.setText(currentWaypoint.getFormattedSequence());
        tvNavRecipient.setText(currentWaypoint.getRecipientName());
        tvNavAddress.setText(currentWaypoint.getFullAddress());
        btnCallRecipient.setContentDescription(getString(R.string.cd_call_recipient, currentWaypoint.getRecipientName()));
        updateExpandedRouteDetails();

        // Atualiza e reinicia o slide de conclusão
        if (slideConcludeDelivery != null) {
            slideConcludeDelivery.setLabel(getString(R.string.slide_to_deliver));
            slideConcludeDelivery.resetSmoothly();
        }

        // 2. Atualizar Banner Superior (Uber Driver Maneuver)
        if (tvNavHeading != null) {
            tvNavHeading.setText(getString(R.string.nav_heading_to, currentWaypoint.getSequenceOrder()) + " · " + currentWaypoint.getRecipientName());
        }
        tvManeuverInstruction.setText(getString(R.string.nav_maneuver_follow));
        ivManeuverIcon.setImageResource(R.drawable.ic_turn_straight);
        tvManeuverDistance.setText(R.string.nav_calculating);

        // 3. Geocodificar e traçar rota até o Endereço Real retornado pela API
        resolveRealDeliveryCoordinates();

        // 4. Iniciar ou atualizar Rastreamento GPS
        requestLocationUpdates();
    }

    /**
     * Atualiza as informações extras da rota exibidas quando o painel inferior é expandido.
     */
    private void updateExpandedRouteDetails() {
        if (currentRoute == null || currentWaypoint == null) return;

        if (tvExpandedRouteCode != null) {
            String code = !TextUtils.isEmpty(currentRoute.getCode())
                    ? currentRoute.getCode()
                    : "ROTA-ORBI-" + currentRoute.getId();
            tvExpandedRouteCode.setText(code);
        }

        if (tvExpandedStopStage != null) {
            if (currentWaypoint.isAtPickup()) {
                tvExpandedStopStage.setText(R.string.status_wp_at_pickup);
            } else {
                tvExpandedStopStage.setText(R.string.status_wp_in_transit);
            }
        }

        int total = Math.max(1, currentRoute.getTotalWaypoints());
        int completed = currentRoute.getCompletedWaypoints();
        int currentSeq = currentWaypoint.getSequenceOrder();

        if (tvExpandedRouteProgress != null) {
            tvExpandedRouteProgress.setText(getString(R.string.nav_route_progress_summary, currentSeq, total, completed));
        }

        if (progressRouteStops != null) {
            int progressPercent = (int) Math.round(((completed + 0.35) / total) * 100.0);
            progressRouteStops.setProgress(Math.max(8, Math.min(100, progressPercent)));
        }

        if (tvExpandedNextStop != null) {
            Waypoint nextFollowing = findFollowingPendingWaypoint(currentWaypoint.getDeliveryId());
            if (nextFollowing != null) {
                tvExpandedNextStop.setText(getString(
                        R.string.nav_next_stop_preview,
                        nextFollowing.getSequenceOrder(),
                        nextFollowing.getFullAddress()
                ));
            } else {
                tvExpandedNextStop.setText(R.string.nav_last_stop_preview);
            }
        }
    }

    private Waypoint findFollowingPendingWaypoint(long activeDeliveryId) {
        if (currentRoute == null || currentRoute.getWaypoints() == null) return null;
        boolean passedActive = false;
        for (Waypoint w : currentRoute.getWaypoints()) {
            if (w.getDeliveryId() == activeDeliveryId) {
                passedActive = true;
                continue;
            }
            if (passedActive && !w.isTerminal()) {
                return w;
            }
        }
        return null;
    }

    /**
     * Resolve a latitude e longitude reais a partir do endereço string da entrega.
     */
    private void resolveRealDeliveryCoordinates() {
        if (currentWaypoint == null) return;

        // Se o Waypoint já possui coordenadas válidas conhecidas (e não o fallback padrão da Sé), usa imediatamente
        boolean hasSpecificCoords = currentWaypoint.getLatitude() != 0.0 && currentWaypoint.getLongitude() != 0.0
                && !(Math.abs(currentWaypoint.getLatitude() - (-23.550520)) < 0.0001 && Math.abs(currentWaypoint.getLongitude() - (-46.633308)) < 0.0001);

        if (hasSpecificCoords) {
            currentTargetLatLng = new LatLng(currentWaypoint.getLatitude(), currentWaypoint.getLongitude());
            updateDestinationMarker();
            if (currentDriverLatLng != null) {
                calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
            }
            return;
        }

        String address = currentWaypoint.getFullAddress();
        AddressGeocoder.geocodeAddress(this, address, new AddressGeocoder.GeocodeCallback() {
            @Override
            public void onGeocoded(double latitude, double longitude) {
                currentWaypoint.setLatitude(latitude);
                currentWaypoint.setLongitude(longitude);
                currentTargetLatLng = new LatLng(latitude, longitude);

                runOnUiThread(() -> {
                    updateDestinationMarker();
                    if (currentDriverLatLng != null) {
                        calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (currentTargetLatLng == null) {
                    currentTargetLatLng = new LatLng(currentWaypoint.getLatitude(), currentWaypoint.getLongitude());
                    runOnUiThread(() -> {
                        updateDestinationMarker();
                        if (currentDriverLatLng != null) {
                            calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
                        }
                    });
                }
            }
        });
    }

    private void updateDestinationMarker() {
        if (googleMap == null || currentTargetLatLng == null || currentWaypoint == null) return;

        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        if (destinationHaloCircle != null) {
            destinationHaloCircle.remove();
        }

        destinationHaloCircle = googleMap.addCircle(new CircleOptions()
                .center(currentTargetLatLng)
                .radius(24.0)
                .strokeWidth(3f)
                .strokeColor(0x55DC2626)
                .fillColor(0x22DC2626)
                .zIndex(7f));

        BitmapDescriptor pinIcon = createStopPinDescriptor(currentWaypoint.getSequenceOrder());
        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(currentTargetLatLng)
                .title("Parada " + currentWaypoint.getFormattedSequence())
                .snippet(currentWaypoint.getFullAddress())
                .icon(pinIcon)
                .zIndex(15f)
                .anchor(0.5f, 1.0f));

        renderAllRouteStopMarkers();
    }

    /**
     * Renderiza todas as demais paradas da rota no mapa (concluídas com check verde e próximas numeradas),
     * proporcionando visão completa da jornada multi-paradas estilo Uber Driver.
     */
    private void renderAllRouteStopMarkers() {
        if (googleMap == null) return;
        for (Marker m : routeStopMarkers) {
            m.remove();
        }
        routeStopMarkers.clear();

        if (currentRoute == null || currentRoute.getWaypoints() == null) return;

        for (Waypoint wp : currentRoute.getWaypoints()) {
            if (currentWaypoint != null && wp.getDeliveryId() == currentWaypoint.getDeliveryId()) {
                continue;
            }
            boolean hasCoords = wp.getLatitude() != 0.0 && wp.getLongitude() != 0.0
                    && !(Math.abs(wp.getLatitude() - (-23.550520)) < 0.0001 && Math.abs(wp.getLongitude() - (-46.633308)) < 0.0001);
            if (!hasCoords) continue;

            LatLng pos = new LatLng(wp.getLatitude(), wp.getLongitude());
            BitmapDescriptor icon = createSecondaryStopPinDescriptor(wp.getSequenceOrder(), wp.getStatus());
            Marker m = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Parada " + wp.getFormattedSequence())
                    .snippet(wp.getFullAddress())
                    .icon(icon)
                    .zIndex(wp.isTerminal() ? 11f : 13f)
                    .anchor(0.5f, 0.5f));
            if (m != null) {
                routeStopMarkers.add(m);
            }
        }
    }

    private void updateDriverMarker() {
        if (googleMap == null || currentDriverLatLng == null) return;

        if (driverMarker == null) {
            BitmapDescriptor carIcon = createDriverArrowDescriptor();
            driverMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentDriverLatLng)
                    .title("Você está aqui")
                    .icon(carIcon)
                    .flat(true)
                    .zIndex(20f)
                    .anchor(0.5f, 0.5f)
                    .rotation(currentAzimuth));
        } else {
            animateDriverPuckTo(currentDriverLatLng, currentAzimuth);
        }
    }

    private void animateDriverPuckTo(final LatLng targetPos, final float targetBearing) {
        if (driverMarker == null || targetPos == null) return;

        final LatLng startPos = driverMarker.getPosition();
        final float startBearing = driverMarker.getRotation();
        if (startPos == null) {
            driverMarker.setPosition(targetPos);
            driverMarker.setRotation(targetBearing);
            return;
        }

        if (puckAnimator != null && puckAnimator.isRunning()) {
            puckAnimator.cancel();
        }

        float deltaAngle = ((targetBearing - startBearing + 540f) % 360f) - 180f;
        final float normalizedDeltaAngle = deltaAngle;

        puckAnimator = ValueAnimator.ofFloat(0f, 1f);
        puckAnimator.setDuration(520L);
        puckAnimator.addUpdateListener(anim -> {
            if (driverMarker == null) return;
            float f = (float) anim.getAnimatedValue();
            double lat = startPos.latitude + (targetPos.latitude - startPos.latitude) * f;
            double lng = startPos.longitude + (targetPos.longitude - startPos.longitude) * f;
            float rot = (startBearing + normalizedDeltaAngle * f + 360f) % 360f;
            driverMarker.setPosition(new LatLng(lat, lng));
            driverMarker.setRotation(rot);
        });
        puckAnimator.start();
    }

    private void onDeviceOrientationChanged(float azimuth) {
        // Se o veículo estiver parado, usa a bússola para girar suavemente o puck
        if (driverMarker != null && !isRealGpsAcquired) {
            currentAzimuth = azimuth;
            driverMarker.setRotation(azimuth);
        }
    }

    private void recenterOnDriver() {
        if (googleMap != null && currentDriverLatLng != null) {
            isOverviewMode = false;
            LatLng lookAheadTarget = computeOffsetLatLng(currentDriverLatLng, 80.0, currentAzimuth);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(lookAheadTarget)
                    .zoom(15.8f)
                    .tilt(45.0f)
                    .bearing(currentAzimuth)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 550, null);
        }
    }

    /**
     * Alterna entre a Câmera 3D de Perseguição (Uber Follow Mode) e a Visão Geral 2D da Rota Atual Completa.
     */
    private void toggleRouteOverview() {
        if (googleMap == null) return;

        if (isOverviewMode) {
            isOverviewMode = false;
            isFollowingDriver = true;
            updateMapPadding();
            recenterOnDriver();
            return;
        }

        isOverviewMode = true;
        isFollowingDriver = false;
        updateMapPadding();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int includedCount = 0;

        if (currentDriverLatLng != null) {
            builder.include(currentDriverLatLng);
            includedCount++;
        }
        if (currentTargetLatLng != null) {
            builder.include(currentTargetLatLng);
            includedCount++;
        } else if (currentWaypoint != null && currentWaypoint.getLatitude() != 0.0 && currentWaypoint.getLongitude() != 0.0) {
            builder.include(new LatLng(currentWaypoint.getLatitude(), currentWaypoint.getLongitude()));
            includedCount++;
        }
        if (currentRoutePoints != null && !currentRoutePoints.isEmpty()) {
            for (LatLng pt : currentRoutePoints) {
                builder.include(pt);
                includedCount++;
            }
        }

        if (includedCount >= 2) {
            try {
                LatLngBounds bounds = builder.build();
                if (bounds.northeast.equals(bounds.southwest)) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 16f), 600, null);
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPx(36)), 600, null);
                }
            } catch (Exception e) {
                if (currentDriverLatLng != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentDriverLatLng, 16f), 600, null);
                }
            }
        } else if (currentDriverLatLng != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentDriverLatLng, 16f), 600, null);
        }
    }

    private void hideGoogleWatermark() {
        if (mapView == null) return;
        mapView.post(() -> removeGoogleLogoView(mapView));
        mapView.postDelayed(() -> removeGoogleLogoView(mapView), 500);
        mapView.postDelayed(() -> removeGoogleLogoView(mapView), 1500);
    }

    private void removeGoogleLogoView(View v) {
        if (v == null) return;
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                removeGoogleLogoView(vg.getChildAt(i));
            }
        } else if (v instanceof ImageView) {
            Object tag = v.getTag();
            CharSequence desc = v.getContentDescription();
            String tagStr = tag != null ? tag.toString() : "";
            String descStr = desc != null ? desc.toString() : "";
            if ("GoogleWatermark".equalsIgnoreCase(tagStr)
                    || "Google".equalsIgnoreCase(descStr)
                    || descStr.toLowerCase().contains("google")) {
                v.setVisibility(View.GONE);
                v.setAlpha(0f);
                v.setScaleX(0f);
                v.setScaleY(0f);
                v.setTranslationY(10000f);
            }
        }
    }

    private LatLng computeOffsetLatLng(LatLng from, double distanceMeters, float bearingDegrees) {
        if (from == null || distanceMeters <= 0.0) return from;
        double earthRadius = 6371009.0;
        double distRad = distanceMeters / earthRadius;
        double bearingRad = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(from.latitude);
        double lng1 = Math.toRadians(from.longitude);

        double sinLat1 = Math.sin(lat1);
        double cosLat1 = Math.cos(lat1);
        double sinDist = Math.sin(distRad);
        double cosDist = Math.cos(distRad);

        double sinLat2 = sinLat1 * cosDist + cosLat1 * sinDist * Math.cos(bearingRad);
        double lat2 = Math.asin(sinLat2);
        double y = Math.sin(bearingRad) * sinDist * cosLat1;
        double x = cosDist - sinLat1 * sinLat2;
        double lng2 = lng1 + Math.atan2(y, x);

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2));
    }

    private float computeBearingBetween(LatLng from, LatLng to) {
        if (from == null || to == null) return currentAzimuth;
        double lat1 = Math.toRadians(from.latitude);
        double lng1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lng2 = Math.toRadians(to.longitude);
        double dLng = lng2 - lng1;
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (float) ((brng + 360.0) % 360.0);
    }

    private void requestLocationUpdates() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            startLocationTracking();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationTracking() {
        if (isTrackingLocation) return;

        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(500)
                    .setMinUpdateDistanceMeters(1f)
                    .build();

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            isTrackingLocation = true;
            setGpsStatus(R.string.nav_gps_acquiring, ContextCompat.getColor(this, R.color.nav_action_skip));

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, loc -> {
                if (loc != null) {
                    onDriverLocationUpdated(loc);
                } else {
                    useFallbackDriverPosition();
                }
            });
        } catch (SecurityException e) {
            useFallbackDriverPosition();
        }
    }

    private void stopLocationTracking() {
        if (isTrackingLocation && fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTrackingLocation = false;
        }
    }

    private void useFallbackDriverPosition() {
        if (isRealGpsAcquired) return;

        if (currentTargetLatLng != null) {
            currentDriverLatLng = new LatLng(
                    currentTargetLatLng.latitude - 0.0055,
                    currentTargetLatLng.longitude - 0.0045
            );
        } else {
            currentDriverLatLng = new LatLng(-23.5614, -46.6559); // Av. Paulista
        }

        updateDriverMarker();
        recenterOnDriver();
        if (currentTargetLatLng != null && (currentRoutePoints == null || currentRoutePoints.isEmpty())) {
            calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
        }
    }

    private void onDriverLocationUpdated(Location location) {
        if (location == null) return;
        isRealGpsAcquired = true;
        setGpsStatus(R.string.nav_gps_active, ContextCompat.getColor(this, R.color.nav_gps_ok));

        currentDriverLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (tvCurrentSpeed != null) {
            int speedKmh = location.hasSpeed() ? Math.max(0, Math.round(location.getSpeed() * 3.6f)) : 0;
            tvCurrentSpeed.setText(String.valueOf(speedKmh));
        }

        if (location.hasBearing() && location.getSpeed() > 0.5f) {
            currentAzimuth = location.getBearing();
        }

        // 1. Se a rota já está traçada, recorta a Polyline a partir do ponto do motorista (Uber Polyline Trimming),
        //    faz o Road Snapping do puck na via e atualiza o progresso Turn-by-Turn
        if (currentRoutePoints != null && currentRoutePoints.size() >= 2) {
            trimPolylineToDriverPosition(currentDriverLatLng);
            updateTurnByTurnProgress(currentDriverLatLng);
        } else if (currentTargetLatLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    currentTargetLatLng.latitude, currentTargetLatLng.longitude,
                    results
            );
            updateNavigationEtaUi(results[0]);
        }

        updateDriverMarker();

        // 2. Verifica se o motorista desviou da rota (> 40m fora da Polyline) para recalcular (Rerouting)
        checkAndRecalculateRouteIfNeeded();

        if (isFollowingDriver) {
            recenterOnDriver();
        }
    }

    /**
     * Verifica se não há rota traçada ou se o motorista saiu da rota planejada (> 40m de desvio perpendicular).
     * Evita recálculos desnecessários quando o motorista está seguindo normalmente a linha azul.
     */
    private void checkAndRecalculateRouteIfNeeded() {
        if (currentDriverLatLng == null || currentTargetLatLng == null) return;

        long now = System.currentTimeMillis();
        boolean shouldRecalculate = false;

        if (routePolyline == null || currentRoutePoints == null || currentRoutePoints.size() < 2) {
            shouldRecalculate = true;
        } else if (now - lastRouteCalculationTime > 6000) {
            double distFromRouteMeters = minDistanceToPolylineMeters(currentDriverLatLng, currentRoutePoints);
            if (distFromRouteMeters > 40.0) {
                shouldRecalculate = true;
            }
        }

        if (shouldRecalculate) {
            calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
        }
    }

    /**
     * Calcula a rota rodoviária curva a curva entre a localização atual do motorista e o endereço da parada,
     * extraindo todos os Steps (manobras, coordenadas de curva, distâncias e nomes de vias) no padrão Uber Directions.
     */
    private void calculateAndRenderRoute(LatLng start, LatLng end) {
        if (start == null || end == null) return;

        lastRouteCalculationTime = System.currentTimeMillis();
        lastRouteOrigin = start;

        String url = String.format(Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson&steps=true",
                start.longitude, start.latitude,
                end.longitude, end.latitude);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "OrbiApp-Android/1.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchOrbiApiDirectionsOrFallback(start, end);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String bodyStr = response.body().string();
                        JSONObject json = new JSONObject(bodyStr);
                        JSONArray routes = json.optJSONArray("routes");
                        if (routes != null && routes.length() > 0) {
                            JSONObject first = routes.getJSONObject(0);
                            double distanceMeters = first.optDouble("distance", 1000.0);
                            double durationSec = first.optDouble("duration", 180.0);

                            JSONObject geometry = first.getJSONObject("geometry");
                            JSONArray coordinates = geometry.getJSONArray("coordinates");

                            List<LatLng> points = new ArrayList<>();
                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray coord = coordinates.getJSONArray(i);
                                points.add(new LatLng(coord.getDouble(1), coord.getDouble(0)));
                            }

                            JSONArray legs = first.optJSONArray("legs");
                            List<RouteStep> parsedSteps = new ArrayList<>();
                            if (legs != null && legs.length() > 0) {
                                JSONArray stepsJson = legs.getJSONObject(0).optJSONArray("steps");
                                parsedSteps = parseRouteSteps(stepsJson, end);
                            }
                            final List<RouteStep> finalSteps = parsedSteps;

                            runOnUiThread(() -> {
                                currentRouteTotalDistanceMeters = distanceMeters;
                                currentRouteTotalDurationSeconds = durationSec;
                                currentRoutePoints = points;
                                currentRouteSteps.clear();
                                currentRouteSteps.addAll(finalSteps);
                                currentStepIndex = (currentRouteSteps.size() > 1 && "depart".equalsIgnoreCase(currentRouteSteps.get(0).type)) ? 1 : 0;

                                drawSolidPolyline(points);
                                updateBottomSheetMetrics(distanceMeters, durationSec);
                                updateTurnByTurnProgress(currentDriverLatLng != null ? currentDriverLatLng : start);
                            });
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
                fetchOrbiApiDirectionsOrFallback(start, end);
            }
        });
    }

    /**
     * Fallback secundário consultando GET /api/route/{id}/directions na Orbi API antes de usar linha direta.
     */
    private void fetchOrbiApiDirectionsOrFallback(LatLng start, LatLng end) {
        if (routeId == null) {
            runOnUiThread(() -> renderDirectFallbackLine(start, end));
            return;
        }
        repository.fetchRouteDirections(this, routeId, new RepositoryCallback<DirectionsResponseDto>() {
            @Override
            public void onSuccess(DirectionsResponseDto dto) {
                GeometryDto geo = dto != null ? dto.getGeometry() : null;
                if (geo != null && geo.getCoordinates() != null && geo.getCoordinates().size() >= 2) {
                    List<LatLng> apiPoints = new ArrayList<>();
                    for (List<Double> coord : geo.getCoordinates()) {
                        if (coord != null && coord.size() >= 2) {
                            apiPoints.add(new LatLng(coord.get(1), coord.get(0)));
                        }
                    }
                    if (apiPoints.size() >= 2) {
                        currentRouteTotalDistanceMeters = dto.getDistanceMeters() > 0 ? dto.getDistanceMeters() : computePolylineLengthMeters(apiPoints);
                        currentRouteTotalDurationSeconds = dto.getDurationSeconds() > 0 ? dto.getDurationSeconds() : (currentRouteTotalDistanceMeters / 9.5);
                        currentRoutePoints = apiPoints;
                        currentRouteSteps.clear();
                        if (dto.getSegments() != null) {
                            for (SegmentDto seg : dto.getSegments()) {
                                if (seg.getSteps() != null) {
                                    for (StepDto s : seg.getSteps()) {
                                        RouteStep rs = new RouteStep();
                                        rs.streetName = s.getName() != null ? s.getName() : "";
                                        rs.instruction = !TextUtils.isEmpty(s.getInstruction()) ? s.getInstruction() : getString(R.string.nav_maneuver_follow);
                                        rs.distanceMeters = s.getDistanceMeters();
                                        rs.durationSeconds = s.getDurationSeconds();
                                        rs.iconRes = resolveIconForInstruction(rs.instruction);
                                        rs.location = end;
                                        currentRouteSteps.add(rs);
                                    }
                                }
                            }
                        }
                        currentStepIndex = 0;
                        drawSolidPolyline(apiPoints);
                        updateBottomSheetMetrics(currentRouteTotalDistanceMeters, currentRouteTotalDurationSeconds);
                        updateTurnByTurnProgress(currentDriverLatLng != null ? currentDriverLatLng : start);
                        return;
                    }
                }
                renderDirectFallbackLine(start, end);
            }

            @Override
            public void onError(String message) {
                renderDirectFallbackLine(start, end);
            }
        });
    }

    private static class RouteStep {
        String instruction;
        String streetName;
        String modifier;
        String type;
        double distanceMeters;
        double durationSeconds;
        int iconRes;
        LatLng location;
    }

    private List<RouteStep> parseRouteSteps(JSONArray steps, LatLng destinationFallback) {
        List<RouteStep> result = new ArrayList<>();
        if (steps == null || steps.length() == 0) return result;

        for (int i = 0; i < steps.length(); i++) {
            try {
                JSONObject step = steps.getJSONObject(i);
                JSONObject maneuver = step.optJSONObject("maneuver");
                String streetName = step.optString("name", "");
                double stepDist = step.optDouble("distance", 0.0);
                double stepDur = step.optDouble("duration", 0.0);

                RouteStep info = new RouteStep();
                info.streetName = streetName;
                info.distanceMeters = stepDist;
                info.durationSeconds = stepDur;
                info.type = maneuver != null ? maneuver.optString("type", "turn") : "turn";
                info.modifier = maneuver != null ? maneuver.optString("modifier", "straight") : "straight";

                if (maneuver != null && maneuver.optJSONArray("location") != null) {
                    JSONArray locArr = maneuver.getJSONArray("location");
                    if (locArr.length() >= 2) {
                        info.location = new LatLng(locArr.getDouble(1), locArr.getDouble(0));
                    }
                }
                if (info.location == null) {
                    info.location = destinationFallback;
                }

                buildStepInstructionAndIcon(info);
                result.add(info);
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private void buildStepInstructionAndIcon(RouteStep info) {
        String street = info.streetName;
        boolean hasStreet = !TextUtils.isEmpty(street);

        if ("arrive".equalsIgnoreCase(info.type)) {
            info.instruction = getString(R.string.nav_maneuver_arriving);
            info.iconRes = R.drawable.ic_place;
        } else if ("roundabout".equalsIgnoreCase(info.type) || "rotary".equalsIgnoreCase(info.type)) {
            info.instruction = hasStreet ? "Na rotatória, pegue a saída para " + street : "Pegue a saída na rotatória";
            info.iconRes = info.modifier.contains("left") ? R.drawable.ic_turn_left : R.drawable.ic_turn_right;
        } else if (info.modifier.contains("uturn")) {
            info.instruction = hasStreet ? "Faça o retorno na " + street : "Faça o retorno logo à frente";
            info.iconRes = R.drawable.ic_turn_left;
        } else if (info.modifier.contains("slight left")) {
            info.instruction = hasStreet ? "Mantenha-se à esquerda na " + street : "Mantenha-se à esquerda";
            info.iconRes = R.drawable.ic_turn_left;
        } else if (info.modifier.contains("slight right")) {
            info.instruction = hasStreet ? "Mantenha-se à direita na " + street : "Mantenha-se à direita";
            info.iconRes = R.drawable.ic_turn_right;
        } else if (info.modifier.contains("left")) {
            info.instruction = hasStreet ? "Vire à esquerda na " + street : "Vire à esquerda";
            info.iconRes = R.drawable.ic_turn_left;
        } else if (info.modifier.contains("right")) {
            info.instruction = hasStreet ? "Vire à direita na " + street : "Vire à direita";
            info.iconRes = R.drawable.ic_turn_right;
        } else {
            info.instruction = hasStreet ? "Siga em frente na " + street : getString(R.string.nav_maneuver_follow);
            info.iconRes = R.drawable.ic_turn_straight;
        }
    }

    private int resolveIconForInstruction(String instruction) {
        if (instruction == null) return R.drawable.ic_turn_straight;
        String lower = instruction.toLowerCase(Locale.ROOT);
        if (lower.contains("esquerda") || lower.contains("left")) return R.drawable.ic_turn_left;
        if (lower.contains("direita") || lower.contains("right")) return R.drawable.ic_turn_right;
        if (lower.contains("cheg") || lower.contains("destin") || lower.contains("arrive")) return R.drawable.ic_place;
        return R.drawable.ic_turn_straight;
    }

    /**
     * Projeta a coordenada GPS do motorista no segmento mais próximo da Polyline e recorta os pontos
     * já ultrapassados (Uber Polyline Trimming), atualizando também a distância rodoviária restante.
     */
    private void trimPolylineToDriverPosition(LatLng driverPos) {
        if (driverPos == null || currentRoutePoints == null || currentRoutePoints.size() < 2) return;

        int closestSegmentIndex = 0;
        double minDistanceSq = Double.MAX_VALUE;
        LatLng bestProjectedPoint = currentRoutePoints.get(0);

        for (int i = 0; i < currentRoutePoints.size() - 1; i++) {
            LatLng a = currentRoutePoints.get(i);
            LatLng b = currentRoutePoints.get(i + 1);
            LatLng projected = projectPointOnSegment(driverPos, a, b);
            double dLat = driverPos.latitude - projected.latitude;
            double dLng = driverPos.longitude - projected.longitude;
            double distSq = dLat * dLat + dLng * dLng;
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closestSegmentIndex = i;
                bestProjectedPoint = projected;
            }
        }

        List<LatLng> trimmedPoints = new ArrayList<>(currentRoutePoints.size() - closestSegmentIndex + 1);
        trimmedPoints.add(bestProjectedPoint);
        for (int i = closestSegmentIndex + 1; i < currentRoutePoints.size(); i++) {
            trimmedPoints.add(currentRoutePoints.get(i));
        }

        // Uber Road-Snapping: se o motorista está a menos de 25m da linha da rua, crava o puck sobre a via
        // e alinha o rumo da câmera/seta com a tangente do segmento rodoviário
        float[] snapDist = new float[1];
        Location.distanceBetween(
                driverPos.latitude, driverPos.longitude,
                bestProjectedPoint.latitude, bestProjectedPoint.longitude,
                snapDist
        );
        if (snapDist[0] < 25f) {
            currentDriverLatLng = bestProjectedPoint;
            if (trimmedPoints.size() >= 2) {
                LatLng nextPt = trimmedPoints.get(1);
                float[] segLen = new float[1];
                Location.distanceBetween(bestProjectedPoint.latitude, bestProjectedPoint.longitude,
                        nextPt.latitude, nextPt.longitude, segLen);
                if (segLen[0] > 1.5f) {
                    currentAzimuth = computeBearingBetween(bestProjectedPoint, nextPt);
                }
            }
        }

        currentRoutePoints = trimmedPoints;
        if (routePolyline != null) {
            routePolyline.setPoints(trimmedPoints);
        } else {
            drawSolidPolyline(trimmedPoints);
        }

        double remainingRoadMeters = computePolylineLengthMeters(trimmedPoints);
        double remainingDurationSec;
        if (currentRouteTotalDistanceMeters > 10.0 && currentRouteTotalDurationSeconds > 0.0) {
            remainingDurationSec = currentRouteTotalDurationSeconds * (remainingRoadMeters / currentRouteTotalDistanceMeters);
        } else {
            remainingDurationSec = (remainingRoadMeters / 9.7); // ~35 km/h urbano
        }
        updateBottomSheetMetrics(remainingRoadMeters, remainingDurationSec);
    }

    /**
     * Atualiza o Banner Superior Turn-by-Turn de acordo com a distância real até a próxima esquina/manobra,
     * avançando automaticamente de Step quando o motorista cruza a conversão (< 28m) e exibindo a faixa "A seguir".
     */
    private void updateTurnByTurnProgress(LatLng driverPos) {
        if (driverPos == null) return;

        if (currentRouteSteps.isEmpty()) {
            if (layoutNextManeuver != null) {
                layoutNextManeuver.setVisibility(View.GONE);
            }
            if (currentTargetLatLng != null) {
                float[] d = new float[1];
                Location.distanceBetween(driverPos.latitude, driverPos.longitude,
                        currentTargetLatLng.latitude, currentTargetLatLng.longitude, d);
                tvManeuverDistance.setText(formatStepDistance(d[0]));
                if (d[0] < 120f) {
                    tvManeuverInstruction.setText(R.string.nav_maneuver_arriving);
                    ivManeuverIcon.setImageResource(R.drawable.ic_place);
                } else {
                    tvManeuverInstruction.setText(R.string.nav_maneuver_follow);
                    ivManeuverIcon.setImageResource(R.drawable.ic_turn_straight);
                }
            }
            return;
        }

        if (currentStepIndex >= currentRouteSteps.size()) {
            currentStepIndex = currentRouteSteps.size() - 1;
        }

        RouteStep activeStep = currentRouteSteps.get(currentStepIndex);
        float[] distToStep = new float[1];
        if (activeStep.location != null) {
            Location.distanceBetween(
                    driverPos.latitude, driverPos.longitude,
                    activeStep.location.latitude, activeStep.location.longitude,
                    distToStep
            );
        } else {
            distToStep[0] = (float) activeStep.distanceMeters;
        }

        // Se o motorista já chegou na esquina da manobra atual (< 28m), avança para a próxima manobra
        if (distToStep[0] < 28f && currentStepIndex < currentRouteSteps.size() - 1) {
            currentStepIndex++;
            activeStep = currentRouteSteps.get(currentStepIndex);
            if (activeStep.location != null) {
                Location.distanceBetween(
                        driverPos.latitude, driverPos.longitude,
                        activeStep.location.latitude, activeStep.location.longitude,
                        distToStep
                );
            }
        }

        double displayDist = distToStep[0] > 5f ? distToStep[0] : activeStep.distanceMeters;
        tvManeuverDistance.setText(formatStepDistance(displayDist));

        if (!TextUtils.isEmpty(activeStep.instruction)) {
            tvManeuverInstruction.setText(activeStep.instruction);
        }
        if (activeStep.iconRes != 0) {
            ivManeuverIcon.setImageResource(activeStep.iconRes);
        }

        // Faixa secundária "A seguir" (Uber Stacked Next Maneuver)
        if (layoutNextManeuver != null) {
            if (currentStepIndex + 1 < currentRouteSteps.size()) {
                RouteStep nextStep = currentRouteSteps.get(currentStepIndex + 1);
                layoutNextManeuver.setVisibility(View.VISIBLE);
                if (ivNextManeuverIcon != null && nextStep.iconRes != 0) {
                    ivNextManeuverIcon.setImageResource(nextStep.iconRes);
                }
                if (tvNextManeuverInstruction != null && !TextUtils.isEmpty(nextStep.instruction)) {
                    tvNextManeuverInstruction.setText(nextStep.instruction);
                }
            } else {
                layoutNextManeuver.setVisibility(View.GONE);
            }
        }
    }

    private String formatStepDistance(double meters) {
        if (meters < 1000.0) {
            int rounded = Math.max(10, (int) (Math.round(meters / 10.0) * 10));
            return String.format(Locale.getDefault(), "Em %d m", rounded);
        }
        return String.format(Locale.getDefault(), "Em %.1f km", meters / 1000.0);
    }

    private LatLng projectPointOnSegment(LatLng p, LatLng a, LatLng b) {
        double ax = a.longitude;
        double ay = a.latitude;
        double bx = b.longitude;
        double by = b.latitude;
        double px = p.longitude;
        double py = p.latitude;

        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-12) return a;

        double t = ((px - ax) * dx + (py - ay) * dy) / lenSq;
        t = Math.max(0.0, Math.min(1.0, t));
        return new LatLng(ay + t * dy, ax + t * dx);
    }

    private double minDistanceToPolylineMeters(LatLng p, List<LatLng> polyline) {
        if (p == null || polyline == null || polyline.isEmpty()) return Double.MAX_VALUE;
        if (polyline.size() == 1) {
            float[] r = new float[1];
            Location.distanceBetween(p.latitude, p.longitude, polyline.get(0).latitude, polyline.get(0).longitude, r);
            return r[0];
        }

        double minMeters = Double.MAX_VALUE;
        float[] res = new float[1];
        for (int i = 0; i < polyline.size() - 1; i++) {
            LatLng proj = projectPointOnSegment(p, polyline.get(i), polyline.get(i + 1));
            Location.distanceBetween(p.latitude, p.longitude, proj.latitude, proj.longitude, res);
            if (res[0] < minMeters) {
                minMeters = res[0];
            }
        }
        return minMeters;
    }

    private double computePolylineLengthMeters(List<LatLng> points) {
        if (points == null || points.size() < 2) return 0.0;
        double total = 0.0;
        float[] seg = new float[1];
        for (int i = 0; i < points.size() - 1; i++) {
            LatLng a = points.get(i);
            LatLng b = points.get(i + 1);
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, seg);
            total += seg[0];
        }
        return total;
    }

    private void renderDirectFallbackLine(LatLng start, LatLng end) {
        List<LatLng> directPoints = new ArrayList<>();
        directPoints.add(start);
        directPoints.add(end);
        currentRoutePoints = directPoints;
        currentRouteSteps.clear();
        drawSolidPolyline(directPoints);

        float[] res = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, res);
        updateNavigationEtaUi(res[0]);
    }

    /**
     * Traça a rota moderna em estilo Flat Minimalista (Orbi Design System / Uber):
     * Linha sólida direta com Azul Orbi (#2557D6) e terminação arredondada, sem contorno/casing
     * para um visual clean e moderno.
     */
    private void drawSolidPolyline(List<LatLng> points) {
        currentRoutePoints = points;
        if (googleMap == null || points == null || points.isEmpty()) return;

        clearRoutePolyline();

        routePolyline = googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(18f)
                .color(0xFF2557D6) // Azul Orbi oficial (#2557D6)
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND)
                .geodesic(true)
                .zIndex(10));

        // Linha pontilhada sutil da rua até a porta do endereço (estilo Uber Curb-to-Door)
        LatLng lastRoadPoint = points.get(points.size() - 1);
        if (currentTargetLatLng != null) {
            float[] curbDist = new float[1];
            Location.distanceBetween(lastRoadPoint.latitude, lastRoadPoint.longitude,
                    currentTargetLatLng.latitude, currentTargetLatLng.longitude, curbDist);
            if (curbDist[0] > 4f && curbDist[0] < 250f) {
                List<PatternItem> dotPattern = Arrays.asList(new Dot(), new Gap(14f));
                curbWalkingPolyline = googleMap.addPolyline(new PolylineOptions()
                        .add(lastRoadPoint, currentTargetLatLng)
                        .width(10f)
                        .color(0xFF666666)
                        .pattern(dotPattern)
                        .zIndex(8));
            }
        }
    }

    private void clearRoutePolyline() {
        if (routePolylineCasing != null) {
            routePolylineCasing.remove();
            routePolylineCasing = null;
        }
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
        if (curbWalkingPolyline != null) {
            curbWalkingPolyline.remove();
            curbWalkingPolyline = null;
        }
    }

    /**
     * Atualiza exclusivamente as métricas do bloco inferior (Duração, Distância Total Restante e Horário de Chegada),
     * sem sobrescrever a instrução ou distância da próxima manobra no banner superior.
     */
    private void updateBottomSheetMetrics(double distanceMeters, double durationSeconds) {
        double distKm = distanceMeters / 1000.0;
        int durationMinutes = (int) Math.max(1, Math.round(durationSeconds / 60.0));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, durationMinutes);
        String arrivalTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());

        if (distanceMeters < 1000.0) {
            tvNavDistance.setText(String.format(Locale.getDefault(), "%d m", Math.max(10, (int) distanceMeters)));
        } else {
            tvNavDistance.setText(String.format(Locale.getDefault(), "%.1f km", distKm));
        }
        tvNavDuration.setText(String.format(Locale.getDefault(), "%d min", durationMinutes));
        tvNavArrivalTime.setText(getString(R.string.nav_arrival_at, arrivalTime));
    }

    private void updateNavigationEtaUi(float distMeters) {
        double distKm = distMeters / 1000.0;
        double durationSec = (distKm / 35.0) * 3600.0;
        updateBottomSheetMetrics(distMeters, durationSec);

        if (currentRouteSteps.isEmpty()) {
            tvManeuverDistance.setText(formatStepDistance(distMeters));
        }
    }

    /**
     * Desenha o puck de navegação estilo Google Maps Navigation:
     * disco branco translúcido com borda suave + chevron direcional azul facetado apontando para frente.
     */
    private BitmapDescriptor createDriverArrowDescriptor() {
        int size = 144;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float center = size / 2f;

        // 1. Sombra externa suave do disco
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x28000000);
        canvas.drawCircle(center, center + 2f, 60f, shadowPaint);

        // 2. Disco branco translúcido (puck do Google Maps Navigation)
        Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        discPaint.setColor(0xEBFFFFFF);
        discPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, 56f, discPaint);

        // 3. Borda sutil do disco
        Paint discBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        discBorderPaint.setColor(0xFFD2E3FC);
        discBorderPaint.setStyle(Paint.Style.STROKE);
        discBorderPaint.setStrokeWidth(2.5f);
        canvas.drawCircle(center, center, 56f, discBorderPaint);

        // 4. Seta azul (Chevron do Google Maps) com leve efeito facetado esquerdo/direito
        float topY = center - 30f;
        float wingY = center + 24f;
        float notchY = center + 11f;
        float wingX = 25f;

        // Faceta esquerda (azul Google #4285F4 / #1A73E8)
        Paint leftWingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leftWingPaint.setColor(0xFF2B7DE9);
        leftWingPaint.setStyle(Paint.Style.FILL);

        Path leftPath = new Path();
        leftPath.moveTo(center, topY);
        leftPath.lineTo(center - wingX, wingY);
        leftPath.lineTo(center, notchY);
        leftPath.close();
        canvas.drawPath(leftPath, leftWingPaint);

        // Faceta direita (azul ligeiramente mais profundo para relevo #175ABC)
        Paint rightWingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rightWingPaint.setColor(0xFF185ABC);
        rightWingPaint.setStyle(Paint.Style.FILL);

        Path rightPath = new Path();
        rightPath.moveTo(center, topY);
        rightPath.lineTo(center + wingX, wingY);
        rightPath.lineTo(center, notchY);
        rightPath.close();
        canvas.drawPath(rightPath, rightWingPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private BitmapDescriptor createStopPinDescriptor(int sequence) {
        int width = 96;
        int height = 118;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(0xFFDC2626); // Vermelho de parada
        pinPaint.setStyle(Paint.Style.FILL);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);

        Path pinPath = new Path();
        float radius = 42f;
        float centerX = width / 2f;
        float centerY = radius + 6f;

        pinPath.moveTo(centerX, height - 4f);
        pinPath.quadTo(centerX - radius, centerY + 20f, centerX - radius, centerY);
        pinPath.arcTo(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 180, 180, false);
        pinPath.quadTo(centerX + radius, centerY + 20f, centerX, height - 4f);
        pinPath.close();

        canvas.drawPath(pinPath, pinPaint);
        canvas.drawPath(pinPath, borderPaint);

        Paint innerCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerCircle.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, 24f, innerCircle);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFDC2626);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(String.valueOf(sequence), centerX, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Desenha pinos circulares numerados compactos estilo Uber Multi-Stop para as demais paradas da rota:
     * - Pendentes seguintes: azul-marinho escuro (#0F172A) com borda branca
     * - Concluídas: verde (#16A34A)
     * - Puladas/Canceladas: cinza (#64748B)
     */
    private BitmapDescriptor createSecondaryStopPinDescriptor(int sequence, WaypointStatus status) {
        int size = 68;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float center = size / 2f;
        float radius = 26f;

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x33000000);
        canvas.drawCircle(center, center + 2f, radius + 2f, shadowPaint);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (status == WaypointStatus.DELIVERED) {
            fillPaint.setColor(0xFF16A34A); // Verde concluído
        } else if (status == WaypointStatus.SKIPPED || status == WaypointStatus.CANCELLED) {
            fillPaint.setColor(0xFF64748B); // Cinza pulado/cancelado
        } else {
            fillPaint.setColor(0xFF0F172A); // Azul-marinho Uber para próximas paradas
        }
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, radius, fillPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        canvas.drawCircle(center, center, radius, borderPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = center - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(String.valueOf(sequence), center, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void handleConcludeStop() {
        if (currentWaypoint == null) return;
        long deliveryId = currentWaypoint.getDeliveryId();

        repository.completeDelivery(this, routeId, deliveryId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                currentWaypoint.setStatus(WaypointStatus.DELIVERED);
                currentWaypoint.setCompletedAt(System.currentTimeMillis());
                Toast.makeText(NavigationMapActivity.this, R.string.stop_completed, Toast.LENGTH_SHORT).show();
                checkNextOrFinish();
            }

            @Override
            public void onError(String message) {
                if (slideConcludeDelivery != null) {
                    slideConcludeDelivery.resetSmoothly();
                }
                Toast.makeText(NavigationMapActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSkipStop() {
        if (currentWaypoint == null) return;
        showReasonDialog(false, currentWaypoint.getDeliveryId());
    }

    private void handleCancelStop() {
        if (currentWaypoint == null) return;
        showReasonDialog(true, currentWaypoint.getDeliveryId());
    }

    private void showReasonDialog(boolean isCancel, long deliveryId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_justification, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        RadioGroup rgReasons = dialogView.findViewById(R.id.rgReasons);
        RadioButton rbReason1 = dialogView.findViewById(R.id.rbReason1);
        RadioButton rbReason2 = dialogView.findViewById(R.id.rbReason2);
        RadioButton rbReason3 = dialogView.findViewById(R.id.rbReason3);
        RadioButton rbReason4 = dialogView.findViewById(R.id.rbReason4);
        TextInputLayout tilOtherReason = dialogView.findViewById(R.id.tilOtherReason);
        TextInputEditText etOtherReason = dialogView.findViewById(R.id.etOtherReason);
        MaterialButton btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        MaterialButton btnDialogConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

        if (isCancel) {
            tvDialogTitle.setText(R.string.dialog_cancel_title);
            tvDialogSubtitle.setText(R.string.dialog_cancel_subtitle);
            rbReason1.setText(R.string.reason_customer_cancelled);
            rbReason2.setText(R.string.reason_address_not_found);
            rbReason3.setText(R.string.reason_damaged_cargo);
            rbReason4.setText(R.string.reason_other);
            btnDialogConfirm.setText(R.string.btn_confirm_cancel);
            btnDialogConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.nav_action_cancel));
        } else {
            tvDialogTitle.setText(R.string.dialog_skip_title);
            tvDialogSubtitle.setText(R.string.dialog_skip_subtitle);
            rbReason1.setText(R.string.reason_recipient_absent);
            rbReason2.setText(R.string.reason_blocked_road);
            rbReason3.setText(R.string.reason_business_closed);
            rbReason4.setText(R.string.reason_other);
            btnDialogConfirm.setText(R.string.btn_confirm_skip);
            btnDialogConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.nav_action_skip));
        }
        btnDialogConfirm.setTextColor(ContextCompat.getColor(this, R.color.surface));

        btnDialogConfirm.setEnabled(false);
        rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
            btnDialogConfirm.setEnabled(checkedId != -1);
            tilOtherReason.setVisibility(checkedId == R.id.rbReason4 ? View.VISIBLE : View.GONE);
        });

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogConfirm.setOnClickListener(v -> {
            String justification;
            int checkedId = rgReasons.getCheckedRadioButtonId();

            if (checkedId == R.id.rbReason1) {
                justification = rbReason1.getText().toString();
            } else if (checkedId == R.id.rbReason2) {
                justification = rbReason2.getText().toString();
            } else if (checkedId == R.id.rbReason3) {
                justification = rbReason3.getText().toString();
            } else {
                justification = etOtherReason.getText() != null ? etOtherReason.getText().toString().trim() : "";
                if (TextUtils.isEmpty(justification)) {
                    justification = "Outro Motivo";
                }
            }

            dialog.dismiss();

            final String finalJustification = justification;
            if (isCancel) {
                repository.cancelDelivery(NavigationMapActivity.this, routeId, deliveryId, finalJustification, new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        currentWaypoint.setStatus(WaypointStatus.CANCELLED);
                        currentWaypoint.setJustification(finalJustification);
                        Toast.makeText(NavigationMapActivity.this, R.string.stop_cancelled, Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }

                    @Override
                    public void onError(String message) {
                        currentWaypoint.setStatus(WaypointStatus.CANCELLED);
                        currentWaypoint.setJustification(finalJustification);
                        Toast.makeText(NavigationMapActivity.this, R.string.stop_cancelled_local, Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }
                });
            } else {
                repository.skipDelivery(NavigationMapActivity.this, routeId, deliveryId, finalJustification, new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        currentWaypoint.setStatus(WaypointStatus.SKIPPED);
                        currentWaypoint.setJustification(finalJustification);
                        Toast.makeText(NavigationMapActivity.this, R.string.stop_skipped, Toast.LENGTH_SHORT).show();
                        checkNextOrFinish();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(NavigationMapActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    /**
     * Avança para a próxima parada da rota. Se a rota atual foi totalmente finalizada,
     * pula automaticamente para a próxima rota pendente da lista do motorista.
     */
    private void checkNextOrFinish() {
        Waypoint next = currentRoute.getNextPendingWaypoint();
        if (next != null) {
            currentWaypoint = next;
            clearRoutePolyline();
            currentRoutePoints = null;
            currentRouteSteps.clear();
            currentStepIndex = 0;
            currentTargetLatLng = null;
            lastRouteOrigin = null;
            loadCurrentNavigationTarget();
        } else {
            handleAllStopsHandledOrNextRoute();
        }
    }

    private void handleAllStopsHandledOrNextRoute() {
        repository.completeRoute(this, routeId);

        // Procura se há uma próxima rota ativa (pulando rotas já finalizadas)
        Route nextRoute = repository.getNextActiveRoute(routeId);
        if (nextRoute != null) {
            Toast.makeText(this, "Rota concluída! Iniciando próxima rota: " + nextRoute.getCode(), Toast.LENGTH_LONG).show();
            routeId = nextRoute.getId();
            currentRoute = nextRoute;
            repository.startRoute(this, routeId);
            clearRoutePolyline();
            currentRoutePoints = null;
            currentRouteSteps.clear();
            currentStepIndex = 0;
            currentTargetLatLng = null;
            lastRouteOrigin = null;
            loadCurrentNavigationTarget();
        } else {
            showAllStopsHandledDialog();
        }
    }

    private void showAllStopsHandledDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.all_stops_handled_title)
                .setMessage(R.string.all_stops_handled_message)
                .setPositiveButton(R.string.btn_finish_route_now, (dialog, which) -> {
                    Toast.makeText(this, R.string.route_completed_toast, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(NavigationMapActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_back_to_details, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (isTrackingLocation) {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (sensorManager != null && sensorListener != null) {
            sensorManager.unregisterListener(sensorListener);
        }
        stopLocationTracking();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
        stopLocationTracking();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        if (mapView != null) mapView.onSaveInstanceState(mapViewBundle);
    }
}
