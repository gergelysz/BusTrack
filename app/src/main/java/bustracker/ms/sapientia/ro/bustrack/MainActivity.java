package bustracker.ms.sapientia.ro.bustrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bustracker.ms.sapientia.ro.bustrack.Data.Station;
import bustracker.ms.sapientia.ro.bustrack.Data.User;

//import com.google.type.LatLng;
//import com.mapbox.mapboxsdk.annotations.MarkerOptions;
//import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
//import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PermissionsListener {

    private static final String TAG = "MainActivity";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private PermissionsManager permissionsManager;

    private FirebaseFirestore mFirestore;

    private LocationEngine locationEngine = null;

    private User currentUser = null;

    private ArrayList<User> usersList = new ArrayList<>();
    private ArrayList<Station> busStations = new ArrayList<>();

    private Map<User, Marker> users = new HashMap<>();

    private static final int REQUEST_LOCATION = 1234;

    private boolean mLocationPermissionGranted = false;

//    private Map<String, Object> currentUserData = new HashMap<String, Object>() {
//        {
//            put("bus", currentBus);
//            put("status", currentStatus);
//            put("timestamp", Timestamp.now());
////            put("coordinates", currentCoordinates);
//            put("latitude", currentCoordinates.getLatitude());
//            put("longitude", currentCoordinates.getLongitude());
//        }
//    };

//    private Map<String, String> currentUserData = new HashMap<>();

    //    private LatLng currentCoordinates;
    private String latitude;
    private String longitude;
    private String currentBus = "0";
    private String currentStatus = "waiting for bus";
    private String currentUserId = null;

    private boolean coordinatesFound;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

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

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync((MapboxMap mapboxMap) -> {

            MainActivity.this.mapboxMap = mapboxMap;

            enableLocationComponent();

            /*
                Get stations from database
                coordinates and name
             */


            setStations();
            getUsersData();
        });



        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Log.d(TAG, "doShit2: " + String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));
                }
            }
        };

        doShit2();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

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
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Location permission is needed for the app", Toast.LENGTH_LONG).show();
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

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {

        Log.d(TAG, "initializing LocationEngine");

        LocationEngineListener locationEngineListener = new LocationEngineListener() {
            @Override
            public void onConnected() {
                locationEngine.requestLocationUpdates();
//                uploadCurrentUserData();
            }

            boolean first = true;

            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "LocationChanged InitializeLocationEngine");
                doShit();
                setCameraPosition(location);
//                currentCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
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
                }

            }
        };

        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
//        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
//        locationEngine.setPriority(LocationEnginePriority.BALANCED_POWER_ACCURACY);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.setFastestInterval(5000);
        locationEngine.setInterval(2000);
        locationEngine.setSmallestDisplacement(0);
//        locationEngine.addLocationEngineListener(locationEngineListener);
        locationEngine.addLocationEngineListener(locationEngineListener);

        //getLastLocation

        locationEngine.activate();

    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
    }

    private void setStations() {

        /*
                Setting stations from database
                and drawing them with markers on map
         */

        Log.d(TAG, "setStations function called");

        mFirestore.collection("stations").addSnapshotListener((queryDocumentSnapshots, e) -> {

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

                Log.d(TAG, "reading bus station data: " + station.getName() + " coordinates: " + station.getCoordinates());

                mapboxMap.addMarker(new MarkerOptions()
                        .position(station.getCoordinates())
                        .title(station.getName())
                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                );

                busStations.add(station);

            }
        });
    }

    private void uploadCurrentUserData() {

        /*
                Upload first user data
         */

        currentUser = new User(currentBus, currentStatus, Timestamp.now(), latitude, longitude);

        mFirestore.collection("users").add(currentUser).addOnSuccessListener(documentReference -> {
            Log.d(TAG, getString(R.string.user_data_upload_success) + documentReference.getId());
            currentUserId = documentReference.getId();
        }).addOnFailureListener(e -> Log.d(TAG, getString(R.string.user_data_upload_fail_details) + e.getMessage()));
    }

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
                User newUser = new User(
                        documentSnapshot.getId(),
                        documentSnapshot.getString("bus"),
                        documentSnapshot.getString("status"),
                        documentSnapshot.getTimestamp("timestamp"),
                        documentSnapshot.getString("latitude"),
                        documentSnapshot.getString("longitude")
                );

                Log.d(TAG, "User data from database: " + newUser.getId() + " " + newUser.getBus() + " " + newUser.getStatus());

                users.put(newUser, mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(Double.parseDouble(newUser.getLatitude()), Double.parseDouble(newUser.getLongitude()))))
                );
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void doShit() {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        Log.d(TAG, "doShit: " + String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));
                    }
                });
    }


    @SuppressLint("MissingPermission")
    protected void doShit2() {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }
}