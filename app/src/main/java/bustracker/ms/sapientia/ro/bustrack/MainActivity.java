package bustracker.ms.sapientia.ro.bustrack;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
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
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PermissionsListener {

    private static final String TAG = "MainActivity";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private PermissionsManager permissionsManager;

    private FirebaseFirestore mFirestore;

    private LocationEngine locationEngine = null;

    private User currentUser = null;

    private Map<String, Bus> buses = new HashMap<>();

    private Map<String, Marker> usersMarkers = new HashMap<>();

    private Map<String, LatLng> stations = null;
    private Map<String, String> stations2 = null;

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

    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
                Mapbox access token is configured here. This needs to be called either in your application
                object or in the same activity which contains the mapview.
         */

        Mapbox.getInstance(this, getString(R.string.access_token));


        Log.d(TAG, "Mapbox set...");

        /*
                This contains the MapView in XML and needs to be called after the access token is configured.
         */

        setContentView(R.layout.activity_main);

        /*
                Connecting to Firestore database
         */

        mFirestore = FirebaseFirestore.getInstance();

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

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

        mapView.getMapAsync((MapboxMap mapboxMap) -> {

            MainActivity.this.mapboxMap = mapboxMap;

            enableLocationComponent();

            /*
                    Get stations from database
                    coordinates and name
             */

            stations2 = loadStationsOffline();

            if (stations2 != null) {
                stations = new HashMap<>();
//                Toast.makeText(this, "stations data found in file", Toast.LENGTH_SHORT).show();
                for (Map.Entry<String, String> entry : stations2.entrySet()) {
                    String key = entry.getKey();
                    String[] value = entry.getValue().split(",");
                    String latitude = value[0];
                    String longitude = value[1];
                    LatLng coordinates = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                    stations.put(key, coordinates);
                    Log.d(TAG, "stationsData: " + key + " " + value[0] + " " + value[1]);
                    mapboxMap.addMarker(new MarkerOptions()
                            .position(coordinates)
                            .title(key)
                            .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                    );
                }
            } else {
                stations2 = new HashMap<>();
//                Toast.makeText(this, "stations data not found in file", Toast.LENGTH_SHORT).show();
                setStations();
            }

            getUsersData();
            getBusesDataFromDatabase();
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("LogNotTimber")
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_select_station) {
            selectedStationRouting();
        } else if (id == R.id.nav_offline_bus_data) {
            saveStationsOffline();
        } else if (id == R.id.nav_change_statusAndBus) {
            statusAndBusSelectorLoader();
        } else if (id == R.id.nav_setup_route) {
//            getRouteForBus("26");
//            Log.d(TAG, "Route drawn");

            Dialog dialog = new Dialog(MainActivity.this);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
            dialog.setContentView(R.layout.bus_route);
            dialog.setCancelable(true);

            Button buttonSelectedBusRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);

            final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectBus);

            List<String> stationsList = new ArrayList<>(stations.keySet());
            Collections.sort(stationsList);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                    R.layout.spinner_item, getResources().getStringArray(R.array.bus_numbers));
//                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.bus_numbers));
//        spinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            buttonSelectedBusRouteApply.setOnClickListener(v -> {
                getRouteForBus(spinner.getSelectedItem().toString());
                dialog.cancel();
            });

            dialog.show();

        } else if (id == R.id.nav_update_busStations) {
            setStations();
            saveStationsOffline();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {

        locationEngine.removeLocationUpdates();
        locationEngine = null;

        /*
                Delete user's data from database
                when the user closes the app.
         */

        if (currentUserId != null) mFirestore.collection("users").document(currentUserId).delete();

        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            initializeLocationEngine();

            LocationComponentOptions options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(this, R.color.red))
                    .build();

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.setLocationEngine(locationEngine);

            // Activate with options
            locationComponent.activateLocationComponent(this, options);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, getString(R.string.location_permission_needed_warning), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(this, getString(R.string.user_location_permission_not_granted), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressLint("LogNotTimber")
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {

        Log.d(TAG, "initializing LocationEngine");

        LocationEngineListener locationEngineListener = new LocationEngineListener() {
            @Override
            public void onConnected() {
                locationEngine.requestLocationUpdates();
            }

            boolean first = true;

            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location changed(?), new location data: " + String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));

                setCameraPosition(location);

                latitude = String.valueOf(location.getLatitude());
                longitude = String.valueOf(location.getLongitude());


                Log.d(TAG, "Current location: " + latitude + " " + longitude);

                if (latitude != null && longitude != null && first) {
                    uploadCurrentUserData();
                    first = false;
                }

                if (currentUserId != null) {

                    /*
                            Update current user's data in database
                     */


                    currentUser.setBus(currentBus);
                    currentUser.setStatus(currentStatus);
                    currentUser.setLatitude(latitude);
                    currentUser.setLongitude(longitude);
                    currentUser.setTimestamp(Timestamp.now());
                    currentUser.setId(currentUserId);

                    Log.d(TAG, "Current user updated data: " + currentUser.getId() + " " + currentUser.getBus() + " " + currentUser.getStatus() + " " + currentUser.getLatitude() + " " + currentUser.getLongitude());

                    mFirestore.collection("users")
                            .document(currentUser.getId())
                            .set(currentUser)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Current user's (" + currentUser.getId() + ") location data updated")
                            );

                    userIdTextView.setText(getResources().getString(R.string.user_id) + " " + currentUserId);
                }

                /*
                        Get all bus data and calculate
                        the closest distance to current location.
                 */

                for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                    Location locationStation = new Location("calcClosest");

                    locationStation.setLatitude(entry.getValue().getLatitude());
                    locationStation.setLongitude(entry.getValue().getLongitude());


                    if (distanceToClosestStation > location.distanceTo(locationStation)) {
                        distanceToClosestStation = location.distanceTo(locationStation);
                        closestStationName = entry.getKey();
                    }
                }

                Log.d(TAG, "closest station: " + closestStationName + " distance: " + distanceToClosestStation);

                closestStationTextView.setText(getResources().getString(R.string.closest_station) + " " + closestStationName);
                speedTextView.setText(getResources().getString(R.string.current_speed) + " " + location.getSpeed());

            }
        };

        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
//        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.setPriority(LocationEnginePriority.BALANCED_POWER_ACCURACY);
        locationEngine.setFastestInterval(1000);
        locationEngine.setInterval(1000);
        locationEngine.setSmallestDisplacement(0);
        locationEngine.addLocationEngineListener(locationEngineListener);

        locationEngine.activate();

    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    @SuppressLint("LogNotTimber")
    private void setStations() {

        /*
                Setting stations from database
                and drawing them with markers on map
         */

        Log.d(TAG, "setStations function called");

        mFirestore.collection("stations").get().addOnSuccessListener(queryDocumentSnapshots -> {
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

                mapboxMap.addMarker(new MarkerOptions()
                        .position(station.getCoordinates())
                        .title(station.getName())
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                );

                stations.put(documentSnapshot.getId(), coordinates);
                stations2.put(documentSnapshot.getId(), loc);


//                busStations.add(station);

                Log.d(TAG, "station: " + station.getName() + " " + station.getCoordinates());

            }
        }).addOnFailureListener(e -> Log.d(TAG, "Couldn't get stations data from database!"));
    }

    @SuppressLint({"LogNotTimber", "SetTextI18n"})
    private void uploadCurrentUserData() {

        /*
                Upload first user data
         */

        currentUser = new User(currentBus, currentStatus, Timestamp.now(), latitude, longitude);

        userStatusTextView.setText(getString(R.string.current_status) + " " + currentStatus);

        mFirestore.collection("users").add(currentUser).addOnSuccessListener(documentReference -> {
            Log.d(TAG, getString(R.string.user_data_upload_success) + documentReference.getId());
            currentUserId = documentReference.getId();
        }).addOnFailureListener(e -> Log.d(TAG, getString(R.string.user_data_upload_fail_details) + e.getMessage()));
    }

    @SuppressLint("LogNotTimber")
    private void getUsersData() {

        /*
                Getting data from database
                and drawing user on map with marker
                if he/she is on bus.
         */

        Log.d(TAG, "Getting users...");

        mFirestore.collection("users").addSnapshotListener((queryDocumentSnapshots, e) -> {
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
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
                    usersMarkers.put(documentSnapshot.getId(), mapboxMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(newUser.getLatitude()), Double.parseDouble(newUser.getLongitude())))));
                } else {
                    LatLng newPosition = new LatLng(
                            Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("latitude"))),
                            Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString("longitude"))));
                    Objects.requireNonNull(usersMarkers.get(documentSnapshot.getId())).setPosition(newPosition);
                }
            }
        });
    }

    @SuppressLint("LogNotTimber")
    private void getRouteForBus(String busNumber) {

        String stationOrigin = Objects.requireNonNull(buses.get(busNumber)).getFirstStationName();
        String stationDest = Objects.requireNonNull(buses.get(busNumber)).getLastStationName();

        assert Mapbox.getAccessToken() != null;
        NavigationRoute.Builder builder = NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(Point.fromLngLat(Objects.requireNonNull(stations.get(stationOrigin)).getLongitude(), Objects.requireNonNull(stations.get(stationOrigin)).getLatitude()))
                .destination(Point.fromLngLat(Objects.requireNonNull(stations.get(stationDest)).getLongitude(), Objects.requireNonNull(stations.get(stationDest)).getLatitude()))
                .profile(DirectionsCriteria.PROFILE_DRIVING);

        for (String stationName : Objects.requireNonNull(buses.get(busNumber)).getStations().subList(1, 10)) {
            if (stations.keySet().contains(stationName)) {
                Point waypoint = Point.fromLngLat(Objects.requireNonNull(stations.get(stationName)).getLongitude(), Objects.requireNonNull(stations.get(stationName)).getLatitude());
                builder.addWaypoint(waypoint);
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
//                            navigationMapRoute.removeRoute();
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

    /*
               Dialog to change the current user's
               status and bus, if he/she is on bus
     */

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void statusAndBusSelectorLoader() {

        final Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.status_and_bus_set);
        dialog.setCancelable(true);

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
//                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.bus_numbers));
//        spinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
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

    @SuppressLint("LogNotTimber")
    private void getBusesDataFromDatabase() {

        mFirestore.collection("busesData").get().addOnSuccessListener(queryDocumentSnapshots -> {
            assert queryDocumentSnapshots != null;
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                /*
                            Converting the data from Firestore to Bus object
                 */

                Bus bus = documentSnapshot.toObject(Bus.class);

                assert bus != null;
                Log.d(TAG, "bus number read: " + bus.getNumber() + " " + bus.getFirstStationName() + " " + bus.getLastStationName());

                for (String stationName : bus.getStations()) {
                    Log.d(TAG, "stationName: " + stationName);
                }

                for (String stationName : bus.getFirstStationLeavingTime()) {
                    Log.d(TAG, "firstStationLeavingTime: " + stationName);
                }

                for (String stationName : bus.getLastStationLeavingTime()) {
                    Log.d(TAG, "lastStationLeavingTime: " + stationName);
                }

                buses.put(bus.getNumber(), bus);
            }
        });
    }

    @SuppressLint("LogNotTimber")
    private void saveStationsOffline() {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(openFileOutput("stationsData2.dat", MODE_PRIVATE));
            objectOutputStream.writeObject(stations2);
            Log.d(TAG, "asdasdasdasd" + objectOutputStream.toString());
            Log.d(TAG, "asdasdasdasd" + stations2);
            objectOutputStream.close();
//            Toast.makeText(this, getString(R.string.stations_data_saved), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
//            Toast.makeText(this, getString(R.string.stations_data_save_failed), Toast.LENGTH_SHORT).show();
            Toast.makeText(this, getString(R.string.couldnt_save_data_error) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> loadStationsOffline() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(openFileInput("stationsData2.dat"));
            return (HashMap<String, String>) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint({"LogNotTimber", "SetTextI18n"})
    private void selectedStationRouting() {
        Dialog dialog = new Dialog(MainActivity.this);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.argb(100, 0, 0, 0)));
        dialog.setContentView(R.layout.draw_route_options);
        dialog.setCancelable(true);

        Button buttonSelectedRouteApply = dialog.findViewById(R.id.button_drawRoute_apply);

        final Spinner spinner = dialog.findViewById(R.id.spinner_drawRoute_selectStation);

        List<String> stationsList = new ArrayList<>(stations.keySet());
        Collections.sort(stationsList);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MainActivity.this,
                R.layout.spinner_item, stationsList);
//                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.bus_numbers));
//        spinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        buttonSelectedRouteApply.setOnClickListener(v -> {
            selectedStation = spinner.getSelectedItem().toString();

            // NEW

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
                ListedBusData listedBusData1 = new ListedBusData(bus.getNumber(), realTimeBusData, comesIn);
                listedBusData.add(listedBusData1);
            }

            ListView listView;

            listView = dialog.findViewById(R.id.listView_result_buses);

            ListedBusAdapter listedBusAdapter = new ListedBusAdapter(this, listedBusData);
            listView.setAdapter(listedBusAdapter);
        });

        dialog.show();
    }
}