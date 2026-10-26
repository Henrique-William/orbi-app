package com.orbi.orbi_app.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.orbi.orbi_app.R;
import com.orbi.orbi_app.data.AddressGeocoder;
import com.orbi.orbi_app.data.RepositoryCallback;
import com.orbi.orbi_app.data.RouteRepository;
import com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto;
import com.orbi.orbi_app.data.remote.dto.GeometryDto;
import com.orbi.orbi_app.model.Route;
import com.orbi.orbi_app.model.Waypoint;
import com.orbi.orbi_app.ui.home.HomeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private TextView tvNavHeading;
    private TextView tvGpsStatus;
    private View viewGpsDot;

    // Recentralizar
    private FloatingActionButton fabRecenter;

    // Painel Inferior Ancorado
    private View layoutSheetContent;
    private TextView tvNavSequence;
    private TextView tvNavRecipient;
    private TextView tvNavAddress;
    private TextView tvNavDistance;
    private TextView tvNavDuration;
    private TextView tvNavArrivalTime;
    private MaterialButton btnCallRecipient;

    // Zonas Demarcadas e Botões Circulares Drag-to-Confirm (Pular e Avançar/Concluir)
    private FrameLayout layoutZoneSkip;
    private FrameLayout layoutZoneComplete;
    private FrameLayout btnCircleSkip;
    private FrameLayout btnCircleComplete;
    private TextView tvCompleteLabel;
    private ImageView ivCompleteIcon;

    // Dados e Estado
    private RouteRepository repository;
    private String routeId;
    private Route currentRoute;
    private Waypoint currentWaypoint;

    // Camadas do Google Maps
    private Marker driverMarker;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private Polyline routePolylineCasing;
    private LatLng currentDriverLatLng;
    private LatLng currentTargetLatLng;
    private boolean isFollowingDriver = true;
    private List<LatLng> currentRoutePoints;
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
        tvNavHeading = findViewById(R.id.tvNavHeading);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        viewGpsDot = findViewById(R.id.viewGpsDot);

        fabRecenter = findViewById(R.id.fabRecenter);
        layoutSheetContent = findViewById(R.id.layoutSheetContent);

        tvNavSequence = findViewById(R.id.tvNavSequence);
        tvNavRecipient = findViewById(R.id.tvNavRecipient);
        tvNavAddress = findViewById(R.id.tvNavAddress);
        tvNavDistance = findViewById(R.id.tvNavDistance);
        tvNavDuration = findViewById(R.id.tvNavDuration);
        tvNavArrivalTime = findViewById(R.id.tvNavArrivalTime);
        btnCallRecipient = findViewById(R.id.btnCallRecipient);

        layoutZoneSkip = findViewById(R.id.layoutZoneSkip);
        layoutZoneComplete = findViewById(R.id.layoutZoneComplete);

        btnCircleSkip = findViewById(R.id.btnCircleSkip);
        btnCircleComplete = findViewById(R.id.btnCircleComplete);

        tvCompleteLabel = findViewById(R.id.tvCompleteLabel);
        ivCompleteIcon = findViewById(R.id.ivCompleteIcon);
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
        View.OnLayoutChangeListener overlayListener = (v, l, t, r, b, ol, ot, or, ob) -> updateMapPadding();
        cardTopBanner.addOnLayoutChangeListener(overlayListener);
        cardBottomControls.addOnLayoutChangeListener(overlayListener);
    }

    private void updateMapPadding() {
        if (googleMap == null || cardTopBanner.getHeight() == 0 || cardBottomControls.getHeight() == 0) return;
        int topBannerBottom = cardTopBanner.getBottom();
        int bottomSheetTop = Math.max(0, rootLayout.getHeight() - cardBottomControls.getTop());
        int availableHeight = Math.max(0, rootLayout.getHeight() - topBannerBottom - bottomSheetTop);
        // Desloca o centro focal da câmera para o terço inferior da área visível (visão Google Maps Navigation)
        int lookAheadOffset = (int) (availableHeight * 0.42f);
        googleMap.setPadding(0, topBannerBottom + lookAheadOffset, 0, bottomSheetTop);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setGpsStatus(@androidx.annotation.StringRes int textRes, int dotColor) {
        tvGpsStatus.setText(textRes);
        viewGpsDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));
    }

    /**
     * Configura o gesto de segurar e arrastar para fora da área demarcada (Drag-to-Confirm)
     * com vibração no limiar e disparo somente ao soltar o dedo fora da área.
     */
    private void setupDragActionButtons() {
        // 1. Ação PULAR (Âmbar) - Opções: Enviar para o final da fila ou Cancelar
        btnCircleSkip.setOnTouchListener(new DragToConfirmListener(
                layoutZoneSkip,
                R.drawable.bg_drag_zone_skip,
                R.drawable.bg_drag_zone_skip_active,
                this::handleSkipStop
        ));

        // 2. Ação AVANÇAR / CONCLUIR (Verde) - Coletar (se AT_PICKUP) ou Entregar (se IN_TRANSIT)
        btnCircleComplete.setOnTouchListener(new DragToConfirmListener(
                layoutZoneComplete,
                R.drawable.bg_drag_zone_complete,
                R.drawable.bg_drag_zone_complete_active,
                this::handleCompleteStop
        ));
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
            isFollowingDriver = true;
            recenterOnDriver();
        });

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

        // 1. Atualizar Informações do Cockpit Inferior
        tvNavSequence.setText(currentWaypoint.getFormattedSequence());
        tvNavRecipient.setText(currentWaypoint.getRecipientName());
        tvNavAddress.setText(currentWaypoint.getFullAddress());
        btnCallRecipient.setContentDescription(getString(R.string.cd_call_recipient, currentWaypoint.getRecipientName()));

        // Atualiza a ação contextual do botão de avanço (Coleta vs Entrega conforme Orbi API)
        if (currentWaypoint.isAtPickup()) {
            if (tvCompleteLabel != null) tvCompleteLabel.setText(R.string.nav_action_pickup);
            if (ivCompleteIcon != null) ivCompleteIcon.setImageResource(R.drawable.ic_navigation);
        } else {
            if (tvCompleteLabel != null) tvCompleteLabel.setText(R.string.nav_action_deliver);
            if (ivCompleteIcon != null) ivCompleteIcon.setImageResource(R.drawable.ic_check);
        }

        // 2. Atualizar Banner Superior (Uber Driver Maneuver)
        tvNavHeading.setText(getString(R.string.nav_heading_to, currentWaypoint.getSequenceOrder()) + " · " + currentWaypoint.getRecipientName());
        tvManeuverInstruction.setText(getString(R.string.nav_maneuver_follow));
        ivManeuverIcon.setImageResource(R.drawable.ic_turn_straight);
        tvManeuverDistance.setText(R.string.nav_calculating);

        // 3. Geocodificar e traçar rota até o Endereço Real retornado pela API
        resolveRealDeliveryCoordinates();

        // 4. Iniciar ou atualizar Rastreamento GPS
        requestLocationUpdates();
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

        BitmapDescriptor pinIcon = createStopPinDescriptor(currentWaypoint.getSequenceOrder());
        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(currentTargetLatLng)
                .title("Parada " + currentWaypoint.getFormattedSequence())
                .snippet(currentWaypoint.getFullAddress())
                .icon(pinIcon)
                .zIndex(15f)
                .anchor(0.5f, 1.0f));
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
            driverMarker.setPosition(currentDriverLatLng);
            driverMarker.setRotation(currentAzimuth);
        }
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
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentDriverLatLng)
                    .zoom(18.0f)
                    .tilt(45.0f)
                    .bearing(currentAzimuth)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 500, null);
        }
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

        if (location.hasBearing() && location.getSpeed() > 0.5f) {
            currentAzimuth = location.getBearing();
        }

        updateDriverMarker();

        if (currentTargetLatLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    currentTargetLatLng.latitude, currentTargetLatLng.longitude,
                    results
            );
            updateNavigationEtaUi(results[0]);
        }

        checkAndRecalculateRouteIfNeeded();

        if (isFollowingDriver) {
            recenterOnDriver();
        }
    }

    private void checkAndRecalculateRouteIfNeeded() {
        if (currentDriverLatLng == null || currentTargetLatLng == null) return;

        long now = System.currentTimeMillis();
        boolean forceRecalculate = false;

        if (routePolyline == null || currentRoutePoints == null || currentRoutePoints.isEmpty()) {
            forceRecalculate = true;
        } else if (lastRouteOrigin != null) {
            float[] distMoved = new float[1];
            Location.distanceBetween(
                    lastRouteOrigin.latitude, lastRouteOrigin.longitude,
                    currentDriverLatLng.latitude, currentDriverLatLng.longitude,
                    distMoved
            );
            // Se o motorista se deslocou mais de 35 metros desde a última rota gerada E já se passaram ao menos 5s
            if (distMoved[0] > 35f && (now - lastRouteCalculationTime > 5000)) {
                forceRecalculate = true;
            }
        }

        if (forceRecalculate) {
            calculateAndRenderRoute(currentDriverLatLng, currentTargetLatLng);
        }
    }

    /**
     * Calcula a rota rodoviária curva a curva entre a localização atual do aparelho e a parada ativa,
     * utilizando o motor OSRM com cabeçalho de aplicação correto e manobras ativas (estilo Google Maps / Waze).
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
                runOnUiThread(() -> renderDirectFallbackLine(start, end));
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

                            // Extração de manobras para o banner estilo Uber / Google Maps
                            JSONArray legs = first.optJSONArray("legs");
                            ManeuverInfo nextManeuver = null;
                            if (legs != null && legs.length() > 0) {
                                JSONArray steps = legs.getJSONObject(0).optJSONArray("steps");
                                nextManeuver = parseNextManeuver(steps);
                            }
                            final ManeuverInfo maneuver = nextManeuver;

                            runOnUiThread(() -> {
                                currentRoutePoints = points;
                                drawSolidPolyline(points);
                                updateMetrics(distanceMeters, durationSec);
                                if (maneuver != null) {
                                    applyManeuverToUi(maneuver);
                                }
                            });
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
                runOnUiThread(() -> renderDirectFallbackLine(start, end));
            }
        });
    }

    private static class ManeuverInfo {
        String instruction;
        String modifier;
        String type;
        double distanceMeters;
        int iconRes;
    }

    private ManeuverInfo parseNextManeuver(JSONArray steps) {
        if (steps == null || steps.length() == 0) return null;
        try {
            int index = (steps.length() > 1) ? 1 : 0;
            JSONObject step = steps.getJSONObject(index);
            JSONObject maneuver = step.optJSONObject("maneuver");
            String streetName = step.optString("name", "");
            double stepDist = step.optDouble("distance", 0.0);

            ManeuverInfo info = new ManeuverInfo();
            info.distanceMeters = stepDist;
            info.type = maneuver != null ? maneuver.optString("type", "turn") : "turn";
            info.modifier = maneuver != null ? maneuver.optString("modifier", "straight") : "straight";

            if ("arrive".equalsIgnoreCase(info.type)) {
                info.instruction = getString(R.string.nav_maneuver_arriving);
                info.iconRes = R.drawable.ic_place;
            } else if (info.modifier.contains("left")) {
                info.instruction = TextUtils.isEmpty(streetName)
                        ? "Vire à esquerda"
                        : "Vire à esquerda na " + streetName;
                info.iconRes = R.drawable.ic_turn_left;
            } else if (info.modifier.contains("right")) {
                info.instruction = TextUtils.isEmpty(streetName)
                        ? "Vire à direita"
                        : "Vire à direita na " + streetName;
                info.iconRes = R.drawable.ic_turn_right;
            } else {
                info.instruction = TextUtils.isEmpty(streetName)
                        ? getString(R.string.nav_maneuver_follow)
                        : "Siga em frente na " + streetName;
                info.iconRes = R.drawable.ic_turn_straight;
            }
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private void applyManeuverToUi(ManeuverInfo maneuver) {
        if (tvManeuverInstruction != null && !TextUtils.isEmpty(maneuver.instruction)) {
            tvManeuverInstruction.setText(maneuver.instruction);
        }
        if (ivManeuverIcon != null && maneuver.iconRes != 0) {
            ivManeuverIcon.setImageResource(maneuver.iconRes);
        }
    }

    private void renderDirectFallbackLine(LatLng start, LatLng end) {
        List<LatLng> directPoints = new ArrayList<>();
        directPoints.add(start);
        directPoints.add(end);
        currentRoutePoints = directPoints;
        drawSolidPolyline(directPoints);

        float[] res = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, res);
        updateNavigationEtaUi(res[0]);
    }

    /**
     * Traça a rota com alto contraste estilo Google Maps Navigation:
     * contorno azul-marinho escuro espesso (#0A2E70) + núcleo azul intenso (#1A73E8).
     */
    private void drawSolidPolyline(List<LatLng> points) {
        currentRoutePoints = points;
        if (googleMap == null || points == null || points.isEmpty()) return;

        clearRoutePolyline();

        routePolylineCasing = googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(28f)
                .color(0xFF0A2E70) // Contorno azul-marinho profundo de alto contraste
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND)
                .geodesic(true)
                .zIndex(9));

        routePolyline = googleMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(18f)
                .color(0xFF1A73E8) // Azul vibrante oficial Google Maps Navigation
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND)
                .geodesic(true)
                .zIndex(10));
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
    }

    private void updateMetrics(double distanceMeters, double durationSeconds) {
        double distKm = distanceMeters / 1000.0;
        int durationMinutes = (int) Math.round(durationSeconds / 60.0);
        if (durationMinutes <= 0) durationMinutes = 3;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, durationMinutes);
        String arrivalTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());

        tvNavDistance.setText(String.format(Locale.getDefault(), "%.1f km", distKm));
        tvNavDuration.setText(String.format(Locale.getDefault(), "%d min", durationMinutes));
        tvNavArrivalTime.setText(getString(R.string.nav_arrival_at, arrivalTime));

        if (distKm < 1.0) {
            tvManeuverDistance.setText(String.format(Locale.getDefault(), "Em %d m", (int) distanceMeters));
        } else {
            tvManeuverDistance.setText(String.format(Locale.getDefault(), "Em %.1f km", distKm));
        }

        if (distanceMeters < 150) {
            tvManeuverInstruction.setText(R.string.nav_maneuver_arriving);
        } else {
            tvManeuverInstruction.setText(getString(R.string.nav_maneuver_follow));
        }
    }

    private void updateNavigationEtaUi(float distMeters) {
        double distKm = distMeters / 1000.0;
        int min = (int) Math.max(2, Math.round((distKm / 35.0) * 60.0));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, min);
        String arrivalTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());

        tvNavDistance.setText(String.format(Locale.getDefault(), "%.1f km", distKm));
        tvNavDuration.setText(String.format(Locale.getDefault(), "%d min", min));
        tvNavArrivalTime.setText(getString(R.string.nav_arrival_at, arrivalTime));

        if (distKm < 1.0) {
            tvManeuverDistance.setText(String.format(Locale.getDefault(), "Em %d m", (int) distMeters));
        } else {
            tvManeuverDistance.setText(String.format(Locale.getDefault(), "Em %.1f km", distKm));
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

    private void handleCompleteStop() {
        if (currentWaypoint == null) return;

        long deliveryId = currentWaypoint.getDeliveryId();
        boolean wasAtPickup = currentWaypoint.isAtPickup();

        repository.advanceDelivery(this, routeId, deliveryId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (wasAtPickup) {
                    Toast.makeText(NavigationMapActivity.this, R.string.stop_pickup_confirmed, Toast.LENGTH_SHORT).show();
                    // Agora a parada está em trânsito; atualiza cockpit e traçado para o endereço de entrega
                    loadCurrentNavigationTarget();
                } else {
                    Toast.makeText(NavigationMapActivity.this, R.string.stop_completed, Toast.LENGTH_SHORT).show();
                    checkNextOrFinish();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(NavigationMapActivity.this, message, Toast.LENGTH_SHORT).show();
                checkNextOrFinish();
            }
        });
    }

    private void handleSkipStop() {
        if (currentWaypoint == null) return;
        showSkipOptionsDialog();
    }

    private void showSkipOptionsDialog() {
        if (currentWaypoint == null) return;
        final long deliveryId = currentWaypoint.getDeliveryId();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skip_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        View cardPostponeQueue = dialogView.findViewById(R.id.cardPostponeQueue);
        View cardCancelDelivery = dialogView.findViewById(R.id.cardCancelDelivery);
        View btnSkipDismiss = dialogView.findViewById(R.id.btnSkipDismiss);

        cardPostponeQueue.setOnClickListener(v -> {
            dialog.dismiss();
            repository.postponeDelivery(NavigationMapActivity.this, routeId, deliveryId, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Toast.makeText(NavigationMapActivity.this, R.string.stop_postponed, Toast.LENGTH_SHORT).show();
                    clearRoutePolyline();
                    currentRoutePoints = null;
                    lastRouteOrigin = null;
                    checkNextOrFinish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(NavigationMapActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        cardCancelDelivery.setOnClickListener(v -> {
            dialog.dismiss();
            showCancelJustificationDialog(deliveryId);
        });

        btnSkipDismiss.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCancelJustificationDialog(long deliveryId) {
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

        tvDialogTitle.setText(R.string.dialog_cancel_title);
        tvDialogSubtitle.setText(R.string.dialog_cancel_subtitle);
        rbReason1.setText(R.string.reason_customer_cancelled);
        rbReason2.setText(R.string.reason_address_not_found);
        rbReason3.setText(R.string.reason_damaged_cargo);
        rbReason4.setText(R.string.reason_other);
        btnDialogConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_cancelled));

        rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
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

            repository.cancelDelivery(NavigationMapActivity.this, routeId, deliveryId, justification, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Toast.makeText(NavigationMapActivity.this, R.string.stop_cancelled, Toast.LENGTH_SHORT).show();
                    checkNextOrFinish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(NavigationMapActivity.this, R.string.stop_cancelled_local, Toast.LENGTH_SHORT).show();
                    checkNextOrFinish();
                }
            });
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
            lastRouteOrigin = null;
            loadCurrentNavigationTarget();
        } else {
            handleAllStopsHandledOrNextRoute();
        }
    }

    private void handleAllStopsHandledOrNextRoute() {
        repository.completeRoute(routeId);

        // Procura se há uma próxima rota ativa (pulando rotas já finalizadas)
        Route nextRoute = repository.getNextActiveRoute(routeId);
        if (nextRoute != null) {
            Toast.makeText(this, "Rota concluída! Iniciando próxima rota: " + nextRoute.getCode(), Toast.LENGTH_LONG).show();
            routeId = nextRoute.getId();
            currentRoute = nextRoute;
            repository.startRoute(routeId);
            clearRoutePolyline();
            currentRoutePoints = null;
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
