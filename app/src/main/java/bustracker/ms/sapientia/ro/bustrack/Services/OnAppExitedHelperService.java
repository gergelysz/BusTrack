package bustracker.ms.sapientia.ro.bustrack.Services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import bustracker.ms.sapientia.ro.bustrack.Activities.MainActivity;

public class OnAppExitedHelperService extends Service {

    private static final String TAG = "OAEHService";

    private final MainActivity mainActivity = new MainActivity();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("LogNotTimber")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
    }

    @SuppressLint("LogNotTimber")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        return START_NOT_STICKY;
    }

    @SuppressLint("LogNotTimber")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
    }

    @SuppressLint("LogNotTimber")
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "Service Ends userId: " + mainActivity.currentUserId);
        if (mainActivity.currentUserId != null) {
            Log.e(TAG, "Service end - currentUser still in");
            mainActivity.firestoreDb.collection("users").document(mainActivity.currentUserId).delete();
        }
        stopSelf();
    }
}
