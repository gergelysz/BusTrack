package bustracker.ms.sapientia.ro.bustrack.Activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.TelemetryDefinition;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import bustracker.ms.sapientia.ro.bustrack.Adapter.ListedBusAdapter;
import bustracker.ms.sapientia.ro.bustrack.Data.Bus;
import bustracker.ms.sapientia.ro.bustrack.Data.ListedBusData;
import bustracker.ms.sapientia.ro.bustrack.Data.User;
import bustracker.ms.sapientia.ro.bustrack.Fragments.ListedBusDetailsFragment;
import bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment;
import bustracker.ms.sapientia.ro.bustrack.R;
import bustracker.ms.sapientia.ro.bustrack.Services.LocationUpdatesService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.CURRENT_LOCATION_FOCUS;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.DARK_MAP_THEME;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.THEME_ACCOMMODATION;
import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.UPDATE_FREQUENCY;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, NavigationView.OnNavigationItemSelectedListener {

    @SuppressLint("StaticFieldLeak")
    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    private boolean loaded = false;
    private static final String TAG = "MainActivity";

    public static User currentUser = null;

    private final Map<String, Bus> buses = new HashMap<>();
    private final Map<String, Marker> usersMarkers = new HashMap<>();
    private final List<Marker> stationsMarkers = new ArrayList<>();

    private final List<User> users = new ArrayList<>();

    private final List<String> userIds = new ArrayList<>();
    private Map<String, LatLng> stations = new HashMap<>();

    public static int UPDATE_INTERVAL;
    public static int UPDATE_PRIORITY;

    private boolean skipReadSettings = true;

    private Marker currentUserMarker;
    private Location currentLocation;
    private String selectedStation = "";

    private TextView userIdTextView;
    private TextView speedTextView;
    private TextView closestStationTextView;
    private TextView userStatusTextView;
    private ImageView userStatusImageView;

    private float distanceToClosestStation;
    private String closestStationName = "";

    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;

    private boolean focusOnCurrentLocation;

    private MapboxMap mapboxMap;
    private MapView mapView;

    private Dialog dialog;

    private BroadcastReceiver broadcastReceiver;

    @SuppressLint("StaticFieldLeak")
    public static FirebaseFirestore firestoreDb;
    private PermissionsManager permissionsManager;

    private SymbolManager symbolManager;

    public MainActivity() {
    }

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
        userStatusImageView = headerView.findViewById(R.id.status_nav_image);

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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.nav_select_station:
                selectedStationRouting();
                break;
            case R.id.nav_offline_bus_data:
                getBusesProgram();
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

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "No location permission", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            startService(new Intent(MainActivity.this, LocationUpdatesService.class));
        } else {
            Toast.makeText(this, "Location permission not granted!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        // Read saved settings
        readSettings();

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            startService(new Intent(MainActivity.this, LocationUpdatesService.class));
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }

        currentLocation = new Location("");

        currentUser = new User("0", "waiting for bus", null, null, "0", "0");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                distanceToClosestStation = 20000;
                Location locationStation = new Location("calcClosest");
                if (intent != null) {
                    if (!mapView.isDestroyed()) {
                        currentUser.setLatitude(intent.getStringExtra("latitude"));
                        currentUser.setLongitude(intent.getStringExtra("longitude"));
                        currentUser.setSpeed(intent.getStringExtra("speed"));

                        uploadCurrentUserData();

                        currentLocation.setLatitude(Float.valueOf(currentUser.getLatitude()));
                        currentLocation.setLongitude(Float.valueOf(currentUser.getLongitude()));

                        if (focusOnCurrentLocation) {
                            setCameraPosition(new LatLng(Float.valueOf(currentUser.getLatitude()), Float.valueOf(currentUser.getLongitude())));
                        }

                        // Get closest station
                        for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                            locationStation.setLatitude(entry.getValue().getLatitude());
                            locationStation.setLongitude(entry.getValue().getLongitude());

                            if (distanceToClosestStation > currentLocation.distanceTo(locationStation)) {
                                distanceToClosestStation = currentLocation.distanceTo(locationStation);
                                closestStationName = entry.getKey();
                            }
                        }
                        closestStationTextView.setText(getString(R.string.closest_station, closestStationName));
                        speedTextView.setText(getString(R.string.current_speed, currentUser.getSpeed()));
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("service.to.activity.send.data"));

        /*
                    Get stations from database
                    coordinates and name
        */
        stations = loadStationsOffline();
        if (stations != null) {
            for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                stationsMarkers.add(mapboxMap.addMarker(new MarkerOptions()
                        .position(entry.getValue())
                        .title(entry.getKey())
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                ));
            }
        } else {
            stations = new HashMap<>();
            setStations();
        }

        // Get the users data from database
        getUsersData();
        // Get the buses data from database
        getBusesDataFromDatabase();

    }

    /**
     * Function to move the camera
     * to the given location.
     *
     * @param location - the given location
     */
    private void setCameraPosition(LatLng location) {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
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
        if (dialog != null) {
            dialog.dismiss();
        }
        mapView.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (currentUser.getId() != null) {
            firestoreDb.collection("users").document(currentUser.getId()).delete();
        }
        stopService(new Intent(MainActivity.this, LocationUpdatesService.class));
        super.onDestroy();
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
    private void setStations() {
        firestoreDb.collection("stations").get().addOnSuccessListener(queryDocumentSnapshots -> {
            LatLng coordinates;
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                coordinates = new LatLng(Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("latitude"))),
                        Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("longitude"))));

                stationsMarkers.add(mapboxMap.addMarker(new MarkerOptions()
                        .position(coordinates)
                        .title(documentSnapshot.getId())
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                ));

                stations.put(documentSnapshot.getId(), coordinates);
            }
        }).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Couldn't get stations data from database!", Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> saveStationsOffline());
    }

    /**
     * Function to create a new user and upload
     * and then update his/her
     * data in the Firestore database.
     */
    private void uploadCurrentUserData() {
        if (currentUser.getId() == null && !loaded) {
            userStatusTextView.setText(getString(R.string.current_status, currentUser.getStatus()));
            firestoreDb.collection("users").add(currentUser)
                    .addOnSuccessListener(documentReference -> currentUser.setId(documentReference.getId()))
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, (getString(R.string.user_data_upload_fail_details) + e.getMessage()), Toast.LENGTH_SHORT).show());
            loaded = true;
            currentUserMarker = mapboxMap.addMarker(new MarkerOptions()
                    .position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                    .title("You")
                    .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_user))
            );
        } else if (currentUser.getId() != null && loaded) {
            currentUserMarker.setPosition(
                    new LatLng(
                            Float.valueOf(currentUser.getLatitude()),
                            Float.valueOf(currentUser.getLongitude())
                    )
            );

            firestoreDb.collection("users")
                    .document(currentUser.getId())
                    .set(currentUser);

            userIdTextView.setText(getResources().getString(R.string.user_id, currentUser.getId()));
        }
    }

    /**
     * Getting data from database
     * and drawing user on map with marker
     * if he/she is on bus.
     */
    private void getUsersData() {
        firestoreDb.collection("users").addSnapshotListener(MainActivity.this, (queryDocumentSnapshots, e) -> {
            userIds.clear();
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                if (!mapView.isDestroyed()) {
                    userIds.add(documentSnapshot.getId());
                    // if user isn't yet in the local users list
                    if (!documentSnapshot.getId().equals(currentUser.getId()) && !usersMarkers.keySet().contains(documentSnapshot.getId())) {
                        User newUser = new User(
                                documentSnapshot.getId(),
                                documentSnapshot.getString("bus"),
                                documentSnapshot.getString("status"),
                                documentSnapshot.getString("latitude"),
                                documentSnapshot.getString("longitude"),
                                documentSnapshot.getString("direction"),
                                documentSnapshot.getString("speed")
                        );
                        users.add(newUser);
                        if (newUser.getStatus().equals("on bus")) {
                            usersMarkers.put(documentSnapshot.getId(), mapboxMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(newUser.getLatitude()), Double.parseDouble(newUser.getLongitude())))
                                    .title(newUser.getBus())
                                    .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_user_bus))
                            ));
                        }
                    }
                    // user is already in the local users list
                    else if (!documentSnapshot.getId().equals(currentUser.getId()) && usersMarkers.keySet().contains(documentSnapshot.getId())) {
                        if (Objects.equals(documentSnapshot.getString("status"), "on bus")) {
                            LatLng newPosition = new LatLng(
                                    Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("latitude"))),
                                    Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("longitude"))));
                            Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())).setPosition(newPosition);
                            Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())).setIcon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_user_bus));
                        } else {
                            mapboxMap.removeMarker(Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())));
                            usersMarkers.remove(documentSnapshot.getId());
                        }
                    }
                }
            }

            for (Map.Entry<String, Marker> entry : usersMarkers.entrySet()) {
                if (!mapView.isDestroyed()) {
                    if (!userIds.contains(entry.getKey())) {
                        for (User user : users) {
                            if (user.getId().equals(entry.getKey())) {
                                users.remove(user);
                                break;
                            }
                        }
                        mapboxMap.removeMarker(Objects.requireNonNull(usersMarkers.get(entry.getKey())));
                        usersMarkers.remove(entry.getKey());
                    }
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
                        if (response.body() == null) {
                            Toast.makeText(MainActivity.this, "No route access token!", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Toast.makeText(MainActivity.this, "No routes found!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.updateRouteVisibilityTo(false);
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                        Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Dialog to change the current user's
     * status and bus, if he/she is on bus.
     */
    private void statusAndBusSelectorLoader() {

        final Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.status_and_bus_set);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ImageView imageWaitingForBus, imageOnBus;

        imageWaitingForBus = dialog.findViewById(R.id.image_status_waiting_for_bus);
        imageOnBus = dialog.findViewById(R.id.image_status_on_bus);

        final TextView textView = dialog.findViewById(R.id.editText_sab_selectBus);

        final Spinner spinner = dialog.findViewById(R.id.spinner_statAndBus);

        textView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);

        Button applyButton = dialog.findViewById(R.id.button_apply_status_and_bus);
        applyButton.setVisibility(View.INVISIBLE);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
        spinner.setAdapter(spinnerAdapter);

        imageWaitingForBus.setOnClickListener(v -> {
            currentUser.setStatus("waiting for bus");
            userStatusImageView.setImageResource(R.drawable.ic_waiting_for_bus);
            userStatusTextView.setText(getString(R.string.current_status, currentUser.getStatus()));
            currentUser.setBus("0");
            Snackbar.make(findViewById(R.id.mapView), R.string.successfully_updated, Snackbar.LENGTH_SHORT).show();
            dialog.cancel();
        });

        imageOnBus.setOnClickListener(v -> {
            currentUser.setStatus("on bus");
            userStatusImageView.setImageResource(R.drawable.ic_bus);
            userStatusTextView.setText(getString(R.string.current_status, currentUser.getStatus()));
            textView.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
            applyButton.setVisibility(View.VISIBLE);
        });

        applyButton.setOnClickListener(v -> {
            currentUser.setBus(spinner.getSelectedItem().toString());
            Snackbar.make(findViewById(R.id.mapView), R.string.successfully_updated, Snackbar.LENGTH_SHORT).show();
            dialog.cancel();
        });

        dialog.show();
    }

    /**
     * Function to read data about all of the buses
     * from the 'busesData' collection.
     */
    private void getBusesDataFromDatabase() {
        firestoreDb.collection("busesData").get().addOnSuccessListener(queryDocumentSnapshots -> {
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                Bus bus = documentSnapshot.toObject(Bus.class);
                assert bus != null;
                buses.put(bus.getNumber(), bus);
            }
        });
    }

    /**
     * Function to save stations data to
     * sharedPreferences so the app
     * won't need to reload data from the
     * database.
     */
    private void saveStationsOffline() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor collection = sharedPreferences.edit();
        Gson gson = new Gson();
        String hashMapStations = gson.toJson(stations);
        collection.putString("stationsListOffline", hashMapStations);
        collection.apply();
    }

    /**
     * Function to load the saved stations data
     * from the sharedPreferences.
     *
     * @return - HashMap<String, LatLng> stations
     */
    private HashMap<String, LatLng> loadStationsOffline() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String hashMapStationsString = sharedPreferences.getString("stationsListOffline", null);
        if (hashMapStationsString != null) {
            Type type = new TypeToken<HashMap<String, LatLng>>() {
            }.getType();
            return gson.fromJson(hashMapStationsString, type);
        }
        return null;
    }

    /**
     * Function to display available buses
     * for the given station.
     */
    private void selectedStationRouting() {
        // Current hour (in 24 hour format) and minute
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int currentMin = Calendar.getInstance().get(Calendar.MINUTE);

        ArrayList<Integer> closestHours = new ArrayList<>();
        ArrayList<Integer> closestMinutes = new ArrayList<>();

        dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.draw_route_options);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        Button buttonSelectedRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectStation);
        AutoCompleteTextView autoCompleteTextViewSearch = dialog.findViewById(R.id.autoCompleteTextView_search_station);

        // Sets adapter for search EditText
        String[] searchStationsList = stations.keySet().toArray(new String[0]);
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, searchStationsList);
        autoCompleteTextViewSearch.setAdapter(searchAdapter);

        /*
            If the user used the search-box,
            then the spinner will be disabled.
            If he/she selected the station from
            the spinner, that value will be used.
         */
        autoCompleteTextViewSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                spinner.setEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchInput = autoCompleteTextViewSearch.getText().toString();
                spinner.setEnabled(searchInput.isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        List<String> stationsList = new ArrayList<>(stations.keySet());
        Collections.sort(stationsList);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, stationsList);
        spinner.setAdapter(spinnerAdapter);

        buttonSelectedRouteApply.setOnClickListener(v -> {
            if (spinner.isEnabled()) {
                selectedStation = spinner.getSelectedItem().toString();
            } else {
                selectedStation = autoCompleteTextViewSearch.getText().toString();
            }

            CheckBox checkBoxShowClosestBuses = dialog.findViewById(R.id.checkbox_drawRoute_selectStationClosest);

            List<String> closestStations = null;

            if (checkBoxShowClosestBuses.isChecked()) {
                closestStations = new ArrayList<>();
                Location locationStation = new Location("");
                int distance = 1500;
                for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                    locationStation.setLatitude(entry.getValue().getLatitude());
                    locationStation.setLongitude(entry.getValue().getLongitude());
                    if (distance > currentLocation.distanceTo(locationStation)) {
                        closestStations.add(entry.getKey());
                    }
                }
            }

            HashMap<Bus, Integer> resultBuses = new HashMap<>();
            ArrayList<ListedBusData> listedBusData = new ArrayList<>();

            if (!selectedStation.isEmpty()) {
                for (Bus bus : buses.values()) {
                    if (closestStations != null) {
                        if ((bus.getStationsFromFirstStation().contains(selectedStation) || bus.getLastStationName().equals(selectedStation))
                                && !Collections.disjoint(bus.getStationsFromFirstStation(), closestStations)) {
                            resultBuses.put(bus, 0);
                            currentUser.setDirection("0");
                        } else if ((bus.getStationsFromLastStation().contains(selectedStation) || bus.getFirstStationName().equals(selectedStation))
                                && !Collections.disjoint(bus.getStationsFromLastStation(), closestStations)) {
                            resultBuses.put(bus, 1);
                            currentUser.setDirection("1");
                        }
                    } else {
                        if (bus.getStationsFromFirstStation().contains(selectedStation) || bus.getLastStationName().equals(selectedStation)) {
                            resultBuses.put(bus, 0);
                            currentUser.setDirection("0");
                        } else if (bus.getStationsFromLastStation().contains(selectedStation) || bus.getFirstStationName().equals(selectedStation)) {
                            resultBuses.put(bus, 1);
                            currentUser.setDirection("1");
                        }
                    }

                }
            }

            dialog.setContentView(R.layout.result_buses);
            dialog.setCancelable(true);

            TextView textViewHeadingTo = dialog.findViewById(R.id.editText_result_title);
            textViewHeadingTo.setText(getString(R.string.list_of_all_buses_heading_to, selectedStation));

            // Get the location of the selected station
            Location selectedStationLocation = new Location("");
            selectedStationLocation.setLatitude(Objects.requireNonNull(stations.get(selectedStation)).getLatitude());
            selectedStationLocation.setLongitude(Objects.requireNonNull(stations.get(selectedStation)).getLongitude());

            /*
                Searches for real-time bus
                in the list of users.
            */
            for (User user : users) {
                Location busLocation = new Location("");
                if (resultBuses.containsKey(buses.get(user.getBus()))
                        && user.getStatus().equals("on bus")
                        && user.getDirection().equals(currentUser.getDirection())) {
                    busLocation.setLatitude(Float.parseFloat(user.getLatitude()));
                    busLocation.setLongitude(Float.parseFloat(user.getLongitude()));
                    listedBusData.add(new ListedBusData(buses.get(user.getBus()), true, Integer.valueOf(user.getDirection()), 2, user));
                }
            }

            /*
                Approximately which bus should
                come based on the program and check if
                it's weekend or not
             */
            for (Map.Entry<Bus, Integer> entry : resultBuses.entrySet()) {
                Calendar calendar = Calendar.getInstance();
                // If it's weekend
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    String[] splitTime;
                    // From last station to first
                    if (entry.getValue().equals(1)) {
                        for (String time : entry.getKey().getLastStationLeavingTimeWeekend()) {
                            splitTime = time.split(":");
                            int comesInMin = (currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1]));
                            int minuteDifference = Math.abs((currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1])));

                            if (minuteDifference <= 40) {
                                listedBusData.add(new ListedBusData(entry.getKey(), false, 1, comesInMin));
                            }
                        }
                    }
                    // From first station to last
                    else if (entry.getValue().equals(0)) {
                        for (String time : entry.getKey().getFirstStationLeavingTimeWeekend()) {
                            splitTime = time.split(":");
                            int comesInMin = (currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1]));
                            int minuteDifference = Math.abs((currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1])));

                            if (minuteDifference <= 40) {
                                listedBusData.add(new ListedBusData(entry.getKey(), false, 0, comesInMin));
                            }
                        }
                    }
                }
                // If it's weekday
                else {
                    String[] splitTime;
                    // From last station to first
                    if (entry.getValue().equals(1)) {
                        for (String time : entry.getKey().getLastStationLeavingTime()) {
                            splitTime = time.split(":");
                            int comesInMin = (currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1]));
                            int minuteDifference = Math.abs((currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1])));

                            if (minuteDifference <= 20) {
                                listedBusData.add(new ListedBusData(entry.getKey(), false, 1, comesInMin));
                            }
                        }
                    }
                    // From first station to last
                    else if (entry.getValue().equals(0)) {
                        for (String time : entry.getKey().getFirstStationLeavingTime()) {
                            splitTime = time.split(":");
                            int comesInMin = (currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1]));
                            int minuteDifference = Math.abs((currentHour - Integer.valueOf(splitTime[0])) * 60 + (currentMin - Integer.valueOf(splitTime[1])));

                            if (minuteDifference <= 20) {
                                listedBusData.add(new ListedBusData(entry.getKey(), false, 0, comesInMin));
                            }
                        }
                    }
                }
            }

            dialog.getWindow().setLayout(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
            ListView listView;
            listView = dialog.findViewById(R.id.listView_result_buses);

            if (listedBusData.isEmpty()) {
                dialog.cancel();
                Toast.makeText(MainActivity.this, "No buses found! Check time", Toast.LENGTH_LONG).show();
            } else {
                ListedBusAdapter listedBusAdapter = new ListedBusAdapter(this, listedBusData);
                listView.setAdapter(listedBusAdapter);

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ListedBusDetailsFragment listedBusDetailsFragment = new ListedBusDetailsFragment();
                    // If the user clicked on the real-time bus
                    if (Objects.requireNonNull(listedBusAdapter.getItem(position)).getUser() != null) {
                        LatLng userLoc = new LatLng();
                        userLoc.setLatitude(Float.parseFloat(Objects.requireNonNull(listedBusAdapter.getItem(position)).getUser().getLatitude()));
                        userLoc.setLongitude(Float.parseFloat(Objects.requireNonNull(listedBusAdapter.getItem(position)).getUser().getLongitude()));
                        setCameraPosition(userLoc);
                        dialog.cancel();
                    }
                    // Else display DialogFragment with estimated information about the bus
                    else {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("listedBusAdapter", listedBusAdapter.getItem(position));
                        bundle.putString("latitude", currentUser.getLatitude());
                        bundle.putString("longitude", currentUser.getLongitude());
                        bundle.putString("selectedStation", selectedStation);
                        bundle.putSerializable("stations", (Serializable) stations);
                        listedBusDetailsFragment.setArguments(bundle);
                        listedBusDetailsFragment.show(getSupportFragmentManager(), "ListedBusDetailsFragment");
                    }
                });
            }
        });
        dialog.show();
    }

    /**
     * Function to show a dialog where the user
     * can select a bus and display it's route
     * based on the selected stations.
     */
    private void drawRouteForSelectedBus() {

        dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.bus_route);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        RadioButton firstStation, lastStation;
        firstStation = dialog.findViewById(R.id.radioButtonRouting);
        lastStation = dialog.findViewById(R.id.radioButtonRouting2);

        firstStation.setVisibility(View.GONE);
        lastStation.setVisibility(View.GONE);

        Button buttonSelectedBusRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);
        buttonSelectedBusRouteApply.setClickable(false);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectBus);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selection = spinner.getSelectedItem().toString();

                for (Map.Entry<String, Bus> bus : buses.entrySet()) {
                    if (selection.equals(bus.getKey())) {
                        firstStation.setVisibility(View.VISIBLE);
                        lastStation.setVisibility(View.VISIBLE);
                        firstStation.setText(getString(R.string.from_where_to_where, bus.getValue().getFirstStationName(), bus.getValue().getLastStationName()));
                        lastStation.setText(getString(R.string.from_where_to_where, bus.getValue().getLastStationName(), bus.getValue().getFirstStationName()));
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
        // Dark mode
        if (!sharedPreferences.getBoolean(THEME_ACCOMMODATION, false)) {
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
        }
        // Part of the day accommodation
        if (sharedPreferences.getBoolean(THEME_ACCOMMODATION, false)) {
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (currentHour > 18) {
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
        }
        focusOnCurrentLocation = sharedPreferences.getBoolean(CURRENT_LOCATION_FOCUS, true);
        UPDATE_INTERVAL = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(UPDATE_FREQUENCY, "5000")));
        UPDATE_PRIORITY = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(SettingsFragment.UPDATE_PRIORITY, "102")));
    }

    /**
     * Function to display departure time
     * of selected bus, based on if it's
     * weekend or not.
     */
    private void getBusesProgram() {
        dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.bus_route);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView text;
        text = dialog.findViewById(R.id.textView_drawRoute_select_bus);
        text.setText(getString(R.string.select_bus_to_show_its_program));
        RadioButton firstStation, lastStation;
        firstStation = dialog.findViewById(R.id.radioButtonRouting);
        lastStation = dialog.findViewById(R.id.radioButtonRouting2);

        firstStation.setVisibility(View.GONE);
        lastStation.setVisibility(View.GONE);

        Button buttonSelectedBusRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);
        buttonSelectedBusRouteApply.setClickable(false);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectBus);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selection = spinner.getSelectedItem().toString();

                for (Map.Entry<String, Bus> bus : buses.entrySet()) {
                    if (selection.equals(bus.getKey())) {
                        firstStation.setVisibility(View.VISIBLE);
                        lastStation.setVisibility(View.VISIBLE);
                        firstStation.setText(getString(R.string.from_where_to_where, bus.getValue().getFirstStationName(), bus.getValue().getLastStationName()));
                        lastStation.setText(getString(R.string.from_where_to_where, bus.getValue().getLastStationName(), bus.getValue().getFirstStationName()));
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

            Dialog dialog2;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Time of departure");

            ListView modeList = new ListView(this);
            ArrayAdapter<String> modeAdapter;
            Calendar calendar = Calendar.getInstance();

            if (firstStation.isSelected()) {
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getFirstStationLeavingTimeWeekend());
                } else {
                    modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getFirstStationLeavingTime());
                }
            } else {
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getLastStationLeavingTimeWeekend());
                } else {
                    modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, Objects.requireNonNull(buses.get(spinner.getSelectedItem().toString())).getLastStationLeavingTime());
                }
            }

            modeList.setAdapter(modeAdapter);

            builder.setView(modeList);
            dialog2 = builder.create();
            dialog2.show();
        });
        dialog.show();
    }
}