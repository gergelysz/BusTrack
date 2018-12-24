package bustracker.ms.sapientia.ro.bustrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
//import com.google.type.LatLng;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
//import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import bustracker.ms.sapientia.ro.bustrack.Data.Station;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PermissionsListener {

    private static final String TAG = "MainActivity";
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;

    private FirebaseFirestore mFirestore;

    private LocationEngine locationEngine = null;

    private LocationEngineListener locationEngineListener;

    private ArrayList<Station> busStations = new ArrayList<>();

    LocationComponent locationComponent = null;
//    LocationLayerPlugin locationLayerPlugin = null;

    Location originLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         *   Mapbox access token is configured here. This needs to be called either in your application
         *   object or in the same activity which contains the mapview.
         */

        Mapbox.getInstance(this, getString(R.string.access_token));


        Log.d(TAG, "Mapbox set...");

        /**
         *   This contains the MapView in XML and needs to be called after the access token is configured.
         */

        setContentView(R.layout.activity_main);

        /**
         *   Connecting to Firestore database
         */

        mFirestore = FirebaseFirestore.getInstance();


        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {

            MainActivity.this.mapboxMap = mapboxMap;

            enableLocationComponent();

            /**
             *   Get stations from database
             *   coordinates and name
             */

            setStations();
        });


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
    public boolean onNavigationItemSelected(MenuItem item) {
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
            locationComponent = mapboxMap.getLocationComponent();
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

        locationEngineListener = new LocationEngineListener() {
            @Override
            public void onConnected() {
                locationEngine.requestLocationUpdates();
            }

            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "LocationChanged InitializeLocationEngine");
                setCameraPosition(location);
            }
        };

        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.setFastestInterval(5000);
        locationEngine.setSmallestDisplacement(0);
        locationEngine.addLocationEngineListener(locationEngineListener);

        locationEngine.activate();

    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
//        mapboxMap.addMarker(new MarkerOptions()
//                .position(new LatLng(location.getLatitude(), location.getLongitude()))
//                .title("Current location")
//                .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
//        );
        Log.d(TAG, "asdasdasd");
    }

    private void setStations() {

        /**
         *   Setting stations from database
         *   and drawing them with markers on map
         */

        Log.d(TAG, "setStations function called");

        mFirestore.collection("stations").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                LatLng coordinates;
                String latitude;
                String longitude;
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                    Station station = new Station(
//                            documentSnapshot.getString("latitude"),
//                            documentSnapshot.getString("longitude"),
//                            documentSnapshot.getString("name")
//                    );
                    latitude = documentSnapshot.getString("latitude");
                    longitude = documentSnapshot.getString("longitude");

                    coordinates = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

//                    Station station = new Station(coordinates, documentSnapshot.getString("name"));
                    Station station = new Station(coordinates, documentSnapshot.getId());

                    Log.d(TAG, "reading bus station data: " + station.getName() + " coordinates: " + station.getCoordinates());

//                    mapboxMap.addMarker(new MarkerOptions()
//                            .position(station.getCoordinates())
////                            .title("Current location")
//                    );

//                    double stationLat = Double.parseDouble(documentSnapshot.getString("latitude"));
//                    double stationLng = Double.parseDouble(documentSnapshot.getString("longitude"));
//                    LatLng stationLatLng = new LatLng(stationLat, stationLng);

//                    station.setCoordinates(stationLatLng);

                    busStations.add(station);
                }

                Log.d(TAG, "asdLOL");

                if (busStations.size() != 0) {

                    for (Station station : busStations) {

                        Log.d(TAG, "creating bus stations markers");

                        mapboxMap.addMarker(new MarkerOptions()
                                        .position(station.getCoordinates())
                                        .title(station.getName())
                                        .icon(IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.ic_bus_station))
                        );

                        Log.d(TAG, "created bus stations marker");
                    }
                }
            }
        });
    }
}



