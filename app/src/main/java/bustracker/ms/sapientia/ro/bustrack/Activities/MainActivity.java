package bustracker.ms.sapientia.ro.bustrack.Activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.TelemetryDefinition;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import bustracker.ms.sapientia.ro.bustrack.Adapter.ListedBusAdapter;
import bustracker.ms.sapientia.ro.bustrack.Data.Bus;
import bustracker.ms.sapientia.ro.bustrack.Data.ListedBusData;
import bustracker.ms.sapientia.ro.bustrack.Data.Station;
import bustracker.ms.sapientia.ro.bustrack.Data.User;
import bustracker.ms.sapientia.ro.bustrack.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.CURRENT_LOCATION_FOCUS;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.DARK_MAP_THEME;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.UPDATE_FREQUENCY;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.UPDATE_PRIORITY;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        PermissionsListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private User currentUser = null;

    private boolean firstLocationData = true;

    private final Map<String, Bus> buses = new HashMap<>();

    private final Map<String, Marker> usersMarkers = new HashMap<>();

    private List<Marker> stationsMarkers = new ArrayList<>();

    private final List<String> userIds = new ArrayList<>();
    private Map<String, LatLng> stations = new HashMap<>();
    private Map<String, String> stations2 = null;

    private int UPDATE_INTERVAL;
    private int UPDATE_BATTERY;

    private boolean skipReadSettings = true;

    private String latitude;
    private String longitude;
    private String currentBus = "0";
    private String currentStatus = "waiting for bus";
    private String currentUserId = null;
    private String selectedStation = "";

    private TextView userIdTextView;
    private TextView speedTextView;
    private TextView closestStationTextView;
    private TextView userStatusTextView;

    private double distanceToClosestStation = 2000000;
    private String closestStationName = "";

    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;

    private boolean focusOnCurrentLocation;

    private MapboxMap mapboxMap;
    private MapView mapView;

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private final MainActivityLocationCallback callback = new MainActivityLocationCallback(this);

    private FirebaseFirestore firestoreDb;

    public MainActivity() {
    }

    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        firestoreDb = FirebaseFirestore.getInstance();

        mapView.getMapAsync(this);

        TelemetryDefinition telemetry = Mapbox.getTelemetry();
        assert telemetry != null;
        telemetry.setUserTelemetryRequestState(false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> statusAndBusSelectorLoader());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);

        /*
                headerView to get the TextViews from
                the navigation bar to use setText()
                at showing current status, ID, speed and closest station.
         */

        View headerView = navigationView.getHeaderView(0);

        userStatusTextView = headerView.findViewById(R.id.status_nav_header);
        closestStationTextView = headerView.findViewById(R.id.closest_station_nav_header);
        userIdTextView = headerView.findViewById(R.id.user_id_nav_header);
        speedTextView = headerView.findViewById(R.id.user_speed_nav_header);

        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("LogNotTimber")
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.nav_select_station:
                selectedStationRouting();
                break;
            case R.id.nav_offline_bus_data:
                break;
            case R.id.nav_change_statusAndBus:
                statusAndBusSelectorLoader();
                break;
            case R.id.nav_setup_route:
                drawRouteForSelectedBus();
                break;
            case R.id.nav_update_busStations:
                setStations();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("LogNotTimber")
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(Style.DARK, this::enableLocationComponent);

        readSettings();

        /*
                    Get stations from database
                    coordinates and name
        */

        stations2 = loadStationsOffline();

        if (stations2 != null) {
            Toast.makeText(this, "stations data found in file", Toast.LENGTH_LONG).show();
            for (Map.Entry<String, String> entry : stations2.entrySet()) {
                String key = entry.getKey();
                String[] value = entry.getValue().split(",");
                // value[0] is the latitude and value[1] is the longitude
                stations.put(key, new LatLng(Double.parseDouble(value[0]), Double.parseDouble(value[1])));
                Log.d(TAG, "stationsData: " + key + " " + value[0] + " " + value[1]);
                stationsMarkers.add(mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(Double.parseDouble(value[0]), Double.parseDouble(value[1])))
                        .title(key)
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                ));
            }
        } else {
            stations2 = new HashMap<>();
            Toast.makeText(this, "stations data not found in file", Toast.LENGTH_LONG).show();
            setStations();
        }

        // Get the users data from database
        getUsersData();
        // Get the buses data from database
        getBusesDataFromDatabase();
    }

    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initLocationEngine();
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle, false);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(1000)
                .setFastestInterval(UPDATE_INTERVAL)
                .setPriority(UPDATE_BATTERY)
                .setDisplacement(0)
                .setMaxWaitTime(UPDATE_INTERVAL)
                .build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "No location permission", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            if (mapboxMap.getStyle() != null) {
                enableLocationComponent(mapboxMap.getStyle());
            }
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Function to move the camera
     * to the given location.
     *
     * @param location - the given location
     */
    private void setCameraPosition(Location location) {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
    }


    private static class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainActivity> activityWeakReference;

        MainActivityLocationCallback(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @SuppressLint({"LogNotTimber", "SetTextI18n"})
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainActivity activity = activityWeakReference.get();

            if (activity != null) {

                Location location = result.getLastLocation();
                assert location != null;

                Log.d(TAG, "getLastLocation: " + location.getLatitude() + " " + location.getLongitude());

                if (!activity.firstLocationData) {
                    // Create a Toast which displays the new location's coordinates
                    Toast.makeText(activity, "New location: " + result.getLastLocation().getLatitude() + " " + result.getLastLocation().getLongitude(),
                            Toast.LENGTH_SHORT).show();
                }

                activity.latitude = String.valueOf(location.getLatitude());
                activity.longitude = String.valueOf(location.getLongitude());

                if (activity.focusOnCurrentLocation) {
                    activity.setCameraPosition(location);
                }

                if (activity.firstLocationData) {
                    activity.uploadCurrentUserData();
                    activity.firstLocationData = false;
                }

                if (activity.currentUserId != null) {

                    activity.currentUser.setBus(activity.currentBus);
                    activity.currentUser.setStatus(activity.currentStatus);
                    activity.currentUser.setLatitude(activity.latitude);
                    activity.currentUser.setLongitude(activity.longitude);
                    activity.currentUser.setTimestamp(Timestamp.now());
                    activity.currentUser.setId(activity.currentUserId);

                    Log.d(TAG, "Current user updated data: " + activity.currentUser.getId() + " " + activity.currentUser.getBus() + " " + activity.currentUser.getStatus() + " " + activity.currentUser.getLatitude() + " " + activity.currentUser.getLongitude());

                    activity.firestoreDb.collection("users")
                            .document(activity.currentUser.getId())
                            .set(activity.currentUser)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Current user's (" + activity.currentUser.getId() + ") location data updated")
                            );

                    activity.userIdTextView.setText(activity.getResources().getString(R.string.user_id) + " " + activity.currentUserId);
                }

                /*
                        Get all bus data and calculate
                        the closest distance to current location.
                 */

                for (Map.Entry<String, LatLng> entry : activity.stations.entrySet()) {
                    Location locationStation = new Location("calcClosest");

                    locationStation.setLatitude(entry.getValue().getLatitude());
                    locationStation.setLongitude(entry.getValue().getLongitude());


                    if (activity.distanceToClosestStation > location.distanceTo(locationStation)) {
                        activity.distanceToClosestStation = location.distanceTo(locationStation);
                        activity.closestStationName = entry.getKey();
                    }
                }

                Log.d(TAG, "closest station: " + activity.closestStationName + " distance: " + activity.distanceToClosestStation);

                activity.closestStationTextView.setText(activity.getResources().getString(R.string.closest_station) + " " + activity.closestStationName);
                activity.speedTextView.setText(activity.getResources().getString(R.string.current_speed) + " " + location.getSpeed());
            }


            // Pass the new location to the Maps SDK's LocationComponent
            assert activity != null;
            if (activity.mapboxMap != null && result.getLastLocation() != null) {
                activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
            }

        }


        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        @SuppressLint("LogNotTimber")
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (!skipReadSettings) {
            readSettings();
        }
        skipReadSettings = false;

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * When this function is called, the app
     * deletes it's user's data from the database
     * and stops getting location updates.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }

        if (currentUserId != null) firestoreDb.collection("users").document(currentUserId).delete();

        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Setting stations from database
     * and drawing them with markers on map
     */
    @SuppressLint("LogNotTimber")
    private void setStations() {

        Log.d(TAG, "setStations function called");

        firestoreDb.collection("stations").get().addOnSuccessListener(queryDocumentSnapshots -> {

            LatLng coordinates;
            String latitude;
            String longitude;

            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                latitude = documentSnapshot.getString("latitude");
                longitude = documentSnapshot.getString("longitude");

                assert latitude != null;
                assert longitude != null;
                coordinates = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                Station station = new Station(coordinates, documentSnapshot.getId());

                station.setLocation(coordinates.toString());
                station.setLatitude(String.valueOf(coordinates.getLatitude()));
                station.setLongitude(String.valueOf(coordinates.getLongitude()));

                String loc = station.getLatitude() + "," + station.getLongitude();

                Log.d(TAG, "reading bus station data: " + station.getName() + " coordinates: " + station.getCoordinates());


                stationsMarkers.add(mapboxMap.addMarker(new MarkerOptions()
                        .position(station.getCoordinates())
                        .title(station.getName())
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                ));

                stations.put(documentSnapshot.getId(), coordinates);
                stations2.put(documentSnapshot.getId(), loc);

                Log.d(TAG, "station: " + station.getName() + " " + station.getCoordinates());

            }
        }).addOnFailureListener(e -> Log.d(TAG, "Couldn't get stations data from database!"));

        saveStationsOffline();
    }

    /**
     * Function to create a new user and upload his/her
     * data to the Firestore database.
     */
    @SuppressLint({"LogNotTimber", "SetTextI18n"})
    private void uploadCurrentUserData() {

        /*
                Upload first user data
         */

        currentUser = new User(currentBus, currentStatus, Timestamp.now(), latitude, longitude);

        userStatusTextView.setText(getString(R.string.current_status) + " " + currentStatus);

        firestoreDb.collection("users").add(currentUser).addOnSuccessListener(documentReference -> {
            Log.d(TAG, getString(R.string.user_data_upload_success) + documentReference.getId());
            currentUserId = documentReference.getId();
        }).addOnFailureListener(e -> Log.d(TAG, getString(R.string.user_data_upload_fail_details) + e.getMessage()));
    }

    /**
     * Getting data from database
     * and drawing user on map with marker
     * if he/she is on bus.
     */

    @SuppressLint("LogNotTimber")
    private void getUsersData() {

        Log.d(TAG, "Getting users...");

        firestoreDb.collection("users").addSnapshotListener((queryDocumentSnapshots, e) -> {
            userIds.clear();
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                userIds.add(documentSnapshot.getId());
                Log.d(TAG, "userIds size: " + userIds.size());
                // if user isn't yet in the local users list
                if (!documentSnapshot.getId().equals(currentUserId) && !usersMarkers.keySet().contains(documentSnapshot.getId())) {
                    User newUser = new User(
                            documentSnapshot.getId(),
                            documentSnapshot.getString("bus"),
                            documentSnapshot.getString("status"),
                            documentSnapshot.getTimestamp("timestamp"),
                            documentSnapshot.getString("latitude"),
                            documentSnapshot.getString("longitude")
                    );
                    Log.d(TAG, "New user data from database: " + newUser.getId() + " " + newUser.getBus() + " " + newUser.getStatus());
                    if (newUser.getStatus().equals("on bus")) {
                        usersMarkers.put(documentSnapshot.getId(), mapboxMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(newUser.getLatitude()), Double.parseDouble(newUser.getLongitude())))
                                .setTitle(newUser.getBus())));
                    }

                }
                // user is already in the local users list
                else if (!documentSnapshot.getId().equals(currentUserId) && usersMarkers.keySet().contains(documentSnapshot.getId())) {
                    if (Objects.equals(documentSnapshot.getString("status"), "on bus")) {
                        LatLng newPosition = new LatLng(
                                Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("latitude"))),
                                Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("longitude"))));
                        Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())).setPosition(newPosition);
                    } else {
                        mapboxMap.removeMarker(Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())));
                        usersMarkers.remove(documentSnapshot.getId());
                    }

                }
            }

            for (Map.Entry<String, Marker> entry : usersMarkers.entrySet()) {
                if (!userIds.contains(entry.getKey())) {
                    mapboxMap.removeMarker(Objects.requireNonNull(usersMarkers.get(entry.getKey())));
                    usersMarkers.remove(entry.getKey());
                }
            }
        });
    }

    /**
     * Function to draw a route by the given coordinates.
     *
     * @param stationsWaypointList - waypoints between the stationOrigin and stationDest
     * @param stationOrigin        - starting point coordinates
     * @param stationDest          - destination point coordinates
     */
    @SuppressLint("LogNotTimber")
    private void getRouteForBus(List<String> stationsWaypointList, String stationOrigin, String stationDest) {

        assert Mapbox.getAccessToken() != null;
        NavigationRoute.Builder builder = NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(Point.fromLngLat(Objects.requireNonNull(stations.get(stationOrigin)).getLongitude(), Objects.requireNonNull(stations.get(stationOrigin)).getLatitude()))
                .destination(Point.fromLngLat(Objects.requireNonNull(stations.get(stationDest)).getLongitude(), Objects.requireNonNull(stations.get(stationDest)).getLatitude()))
                .profile(DirectionsCriteria.PROFILE_DRIVING);

        for (String stationName : stationsWaypointList) {
            if (stations.keySet().contains(stationName)) {
                builder.addWaypoint(Point.fromLngLat(Objects.requireNonNull(stations.get(stationName)).getLongitude(), Objects.requireNonNull(stations.get(stationName)).getLatitude()));
            }
        }

        builder.build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        // 200 = SUCCESS
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, getString(R.string.no_routes_access_token));
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, getString(R.string.no_routes_found));
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.updateRouteVisibilityTo(false);
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            Log.d(TAG, getString(R.string.route_configured));
                        }
                        navigationMapRoute.addRoute(currentRoute);
                        Log.d(TAG, getString(R.string.route_added));
                    }

                    @Override
                    public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    /**
     * Dialog to change the current user's
     * status and bus, if he/she is on bus.
     */
    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void statusAndBusSelectorLoader() {

        final Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.status_and_bus_set);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        final RadioButton onBus, waitingForBus;
        onBus = dialog.findViewById(R.id.radioButton);
        waitingForBus = dialog.findViewById(R.id.radioButton2);

        final TextView textView = dialog.findViewById(R.id.editText_sab_selectBus);

        final Spinner spinner = dialog.findViewById(R.id.spinner_statAndBus);

        textView.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);

        Button applyButton = dialog.findViewById(R.id.button_apply_status_and_bus);

        onBus.setOnClickListener(v -> {
            spinner.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
        });

        waitingForBus.setOnClickListener(v -> {
            spinner.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
        });

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
        spinner.setAdapter(spinnerAdapter);

        applyButton.setOnClickListener(v -> {
            if (onBus.isChecked()) {
                currentStatus = "on bus";
                userStatusTextView.setText(getString(R.string.current_status) + currentStatus);
                currentBus = spinner.getSelectedItem().toString();
                dialog.cancel();
            } else if (waitingForBus.isChecked()) {
                currentStatus = "waiting for bus";
                userStatusTextView.setText(getString(R.string.current_status) + currentStatus);
                currentBus = "0";
                dialog.cancel();
            }
            Snackbar.make(findViewById(R.id.mapView), R.string.successfully_updated, Snackbar.LENGTH_SHORT).show();

        });

        dialog.show();
    }

    /**
     * Function to read data about all of the buses
     * from the 'busesData' collection.
     */
    @SuppressLint("LogNotTimber")
    private void getBusesDataFromDatabase() {
        firestoreDb.collection("busesData").get().addOnSuccessListener(queryDocumentSnapshots -> {
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                Bus bus = documentSnapshot.toObject(Bus.class);
                assert bus != null;
                Log.d(TAG, "bus number read: " + bus.getNumber() + " " + bus.getFirstStationName() + " " + bus.getLastStationName());
                buses.put(bus.getNumber(), bus);
            }
        });
    }

    /**
     * Function to save the stations in a .dat file
     * with the purpose of not having to read all the stations
     * from the database every time when the application is launched.
     */
    @SuppressLint("LogNotTimber")
    private void saveStationsOffline() {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(openFileOutput("stationsData.dat", MODE_PRIVATE));
            objectOutputStream.writeObject(stations2);
            Log.d(TAG, "asdasdasdasd" + stations2);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.couldnt_save_data_error) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Function to load the stations from the .dat file
     * which is saved in the 'saveStationsOffline' function.
     *
     * @return - HashMap with the station names and their coordinates
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, String> loadStationsOffline() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("stationsData.dat"));
            return (HashMap<String, String>) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Function to display available buses
     * for the given station.
     */
    @SuppressLint({"LogNotTimber", "SetTextI18n"})
    private void selectedStationRouting() {

        Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.draw_route_options);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        Button buttonSelectedRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectStation);

        List<String> stationsList = new ArrayList<>(stations.keySet());
        Collections.sort(stationsList);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, stationsList);
        spinner.setAdapter(spinnerAdapter);

        buttonSelectedRouteApply.setOnClickListener(v -> {
            selectedStation = spinner.getSelectedItem().toString();

            ArrayList<Bus> resultBuses = new ArrayList<>();

            if (!selectedStation.isEmpty()) {
                for (Bus bus : buses.values()) {
                    if (bus.getStations().contains(selectedStation)) {
                        resultBuses.add(bus);
                    }
                }
                Log.d(TAG, "number of resultBuses: " + resultBuses.size());
            }

            dialog.setContentView(R.layout.result_buses);
            dialog.setCancelable(true);
            ArrayList<ListedBusData> listedBusData = new ArrayList<>();

            TextView textViewHeadingTo = dialog.findViewById(R.id.editText_result_title);
            textViewHeadingTo.setText(getString(R.string.list_of_all_buses_heading_to) + " " + selectedStation);

            // TODO
            String comesIn = "10";
            String realTimeBusData = "Realtime bus not found! xd";

            listedBusData.clear();

            for (Bus bus : resultBuses) {
                listedBusData.add(new ListedBusData(bus.getNumber(), realTimeBusData, comesIn));
            }

            ListView listView;

            listView = dialog.findViewById(R.id.listView_result_buses);

            ListedBusAdapter listedBusAdapter = new ListedBusAdapter(this, listedBusData);
            listView.setAdapter(listedBusAdapter);
        });

        dialog.show();
    }

    /**
     * Function to show a dialog where the user
     * can select a bus and display it's route
     * based on the selected stations.
     */
    private void drawRouteForSelectedBus() {

        Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.bus_route);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        RadioButton firstStation, lastStation;
        firstStation = dialog.findViewById(R.id.radioButtonRouting);
        lastStation = dialog.findViewById(R.id.radioButtonRouting2);

        firstStation.setVisibility(View.INVISIBLE);
        lastStation.setVisibility(View.INVISIBLE);

        Button buttonSelectedBusRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);

        buttonSelectedBusRouteApply.setClickable(false);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectBus);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selection = spinner.getSelectedItem().toString();

                for (Map.Entry<String, Bus> bus : buses.entrySet()) {
                    if (selection.equals(bus.getKey())) {

                        firstStation.setVisibility(View.VISIBLE);
                        lastStation.setVisibility(View.VISIBLE);

                        firstStation.setText(bus.getValue().getFirstStationName() + " -> " + bus.getValue().getLastStationName());
                        lastStation.setText(bus.getValue().getLastStationName() + " -> " + bus.getValue().getFirstStationName());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        firstStation.setOnClickListener(v -> buttonSelectedBusRouteApply.setClickable(true));
        lastStation.setOnClickListener(v -> buttonSelectedBusRouteApply.setClickable(true));

        buttonSelectedBusRouteApply.setOnClickListener(v -> {

            if (firstStation.isSelected()) {
                getRouteForBus(Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getStationsFromFirstStation(), Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getFirstStationName(), Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getLastStationName());
            } else {
                getRouteForBus(Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getStationsFromLastStation(), Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getLastStationName(), Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getFirstStationName());
            }

            dialog.cancel();
        });

        dialog.show();
    }


    /**
     * Function to read settings for dark mode map theme and
     * other settings, when activity is resumed from SettingsActivity
     */
    private void readSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(DARK_MAP_THEME, true)) {
            mapboxMap.setStyle(Style.DARK);
            for (Marker marker : stationsMarkers) {
                marker.setIcon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station));
            }
        } else {
            mapboxMap.setStyle(Style.LIGHT);
            for (Marker marker : stationsMarkers) {
                marker.setIcon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station_light));
            }
        }

        focusOnCurrentLocation = sharedPreferences.getBoolean(CURRENT_LOCATION_FOCUS, true);

        UPDATE_INTERVAL = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(UPDATE_FREQUENCY, "5000")));

        UPDATE_BATTERY = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(UPDATE_PRIORITY, "1")));
    }
}