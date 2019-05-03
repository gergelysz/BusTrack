package bustracker.ms.sapientia.ro.bustrack.Services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity;

import static bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity.currentUserId;
import static bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity.firestoreDb;

public class LocationUpdatesService extends Service implements LocationListener {

    private LocationManager locationManager;
    private Location location = null;
    private final Handler mHandler = new Handler();
    private Timer mTimer = null;
    private Intent intentService;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        locationManager.removeUpdates(this);
        if (currentUserId != null) {
            firestoreDb.collection("users").document(currentUserId).delete();
        }
        mTimer.cancel();
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 0, MainActivity.UPDATE_INTERVAL);
        intentService = new Intent("service.to.activity.send.data");
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @SuppressLint("MissingPermission")
    private void getLocationUpdates() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        boolean isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        if (!isGPSEnable && !isNetworkEnable) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            if (isGPSEnable) {
                location = null;
                if (locationManager != null) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }
            if (isNetworkEnable && location == null) {
                if (locationManager != null) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
            if (location != null) {
                sendUpdatesToMainActivity(location);
            }
        }
    }

    private class TimerTaskToGetLocation extends TimerTask {
        @Override
        public void run() {
            mHandler.post(LocationUpdatesService.this::getLocationUpdates);
        }
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