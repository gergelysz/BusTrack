package bustracker.ms.sapientia.ro.bustrack.Services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity;
import bustracker.ms.sapientia.ro.bustrack.BuildConfig;
import bustracker.ms.sapientia.ro.bustrack.R;

import static bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity.currentUserId;
import static bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity.firestoreDb;

public class LocationUpdatesService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = LocationUpdatesService.class.getSimpleName();
    private static final long LOCATION_REQUEST_INTERVAL = 10000;
    private static final float LOCATION_REQUEST_DISPLACEMENT = 5.0f;
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Intent intentService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intentService = new Intent("service.to.activity.send.data");

        buildGoogleApiClient();
        showNotificationAndStartForegroundService();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //here you get the continues location updated based on the interval defined in
                //location request
                sendUpdatesToMainActivity(locationResult.getLastLocation());
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        return START_NOT_STICKY;
    }

    /**
     * Method used for building GoogleApiClient and add connection callback
     */
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Method used for creating location request
     * After successfully connection of the GoogleClient ,
     * This method used for to request continues location
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(MainActivity.UPDATE_PRIORITY);
        mLocationRequest.setInterval(MainActivity.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(MainActivity.UPDATE_INTERVAL);
        mLocationRequest.setMaxWaitTime(MainActivity.UPDATE_INTERVAL + 10);
        mLocationRequest.setSmallestDisplacement(0);

        requestLocationUpdate();
    }

    /**
     * Method used for the request new location using Google FusedLocation Api
     */
    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this::sendUpdatesToMainActivity);
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void removeLocationUpdate() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * This Method shows notification for ForegroundService
     * Start Foreground Service and Show Notification to user for android all version
     */
    private void showNotificationAndStartForegroundService() {

        final String CHANNEL_ID = BuildConfig.APPLICATION_ID.concat("_notification_id");
        final String CHANNEL_NAME = BuildConfig.APPLICATION_ID.concat("_notification_name");
        final int NOTIFICATION_ID = 100;

        NotificationCompat.Builder builder;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_NONE;
            assert notificationManager != null;
            NotificationChannel mChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                notificationManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.drawable.ic_bus_station)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.app_is_running_in_the_background));
            startForeground(NOTIFICATION_ID, builder.build());
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.drawable.ic_bus_station)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.app_is_running_in_the_background));
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        removeLocationUpdate();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (currentUserId != null) {
            firestoreDb.collection("users").document(currentUserId).delete();
            currentUserId = null;
        }
        stopForeground(true);
        stopSelf();
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    /**
     * Function to send location data
     * back to the MainActivity.
     *
     * @param location - latitude, longitude and speed
     */
    private void sendUpdatesToMainActivity(Location location) {
        Bundle bundle = new Bundle();
        bundle.putString("latitude", String.valueOf(location.getLatitude()));
        bundle.putString("longitude", String.valueOf(location.getLongitude()));
        bundle.putString("speed", String.valueOf(location.getSpeed()));
        intentService.putExtras(bundle);
        sendBroadcast(intentService);
    }
}