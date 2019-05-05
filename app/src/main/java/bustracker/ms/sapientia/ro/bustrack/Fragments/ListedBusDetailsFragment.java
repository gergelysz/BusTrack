package bustracker.ms.sapientia.ro.bustrack.Fragments;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import bustracker.ms.sapientia.ro.bustrack.Data.ListedBusData;
import bustracker.ms.sapientia.ro.bustrack.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static bustracker.ms.sapientia.ro.bustrack.Fragments.SettingsFragment.DARK_MAP_THEME;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListedBusDetailsFragment extends DialogFragment {

    private static final String TAG = "LBDFragment";
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;
    private MapView mapViewListedRouteForUser;
    private double distanceToClosestStation = 2000000;

    public ListedBusDetailsFragment() {
        // Required empty public constructor
    }

    @SuppressLint({"SetTextI18n", "LogNotTimber"})
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_listed_bus_details, container, false);
        Objects.requireNonNull(getDialog().getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView textViewInfoForBus = view.findViewById(R.id.textView_result_buses_clicked_info_for_bus);
        TextView textViewArrivesIn = view.findViewById(R.id.textView_result_buses_clicked_arrives_in);

        assert getArguments() != null;

        ListedBusData listedBusData = (ListedBusData) getArguments().getSerializable("listedBusAdapter");
        assert listedBusData != null;

        Map<String, LatLng> stations = (Map<String, LatLng>) getArguments().getSerializable("stations");
        assert stations != null;

        String latitude = getArguments().getString("latitude");
        String longitude = getArguments().getString("longitude");
        assert latitude != null;
        assert longitude != null;

        String selectedStation = getArguments().getString("selectedStation");
        assert selectedStation != null;

        textViewInfoForBus.setText("Info for bus: " + listedBusData.getBus().getNumber());

        LatLng closest = new LatLng();
        Location locationStation = new Location("");
        Location locationHere = new Location("");
        Location closestStation = new Location("");
        locationHere.setLatitude(Double.parseDouble(latitude));
        locationHere.setLongitude(Double.parseDouble(longitude));
        float distance = 20000;
        String closestStationName = "";

        // To calculate arrival
        int numberOfStationsBetween;
        float comesInAround;

        // From first station to last
        if (listedBusData.getDirection() == 0) {
            for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                // If bus goes through that station
                if (listedBusData.getBus().getStationsFromFirstStation().contains(entry.getKey())) {
                    locationStation.setLatitude(entry.getValue().getLatitude());
                    locationStation.setLongitude(entry.getValue().getLongitude());
                    if (locationHere.distanceTo(locationStation) < distance) {
                        closestStationName = entry.getKey();
                        distance = locationHere.distanceTo(locationStation);
                        closest.setLatitude(entry.getValue().getLatitude());
                        closest.setLongitude(entry.getValue().getLongitude());
                    }
                }
            }

            int start = 0, end = listedBusData.getBus().getStationsFromFirstStation().size();
            for (int i = 0; i < listedBusData.getBus().getStationsFromFirstStation().size(); ++i) {
                if (listedBusData.getBus().getStationsFromFirstStation().get(i).equals(closestStationName)) {
                    end = i + 1;
                } else if (listedBusData.getBus().getStationsFromFirstStation().get(i).equals(selectedStation)) {
                    start = i + 1;
                }
            }

            numberOfStationsBetween = start - end + 2;

            Location busLocation = new Location("");
            busLocation.setLatitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getFirstStationName())).getLatitude());
            busLocation.setLongitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getFirstStationName())).getLongitude());

            closestStation.setLatitude(closest.getLatitude());
            closestStation.setLongitude(closest.getLongitude());

            comesInAround = closestStation.distanceTo(busLocation) / 500; // speedIsXMetersPerMinute  500m/min = 30km/h
            comesInAround = Math.round(comesInAround);

            // Bus already left
            if (listedBusData.getComesInMin() > 0) {
                if (numberOfStationsBetween + comesInAround - listedBusData.getComesInMin() > 0) {
                    textViewArrivesIn.setText("Arrives in " + Math.round(numberOfStationsBetween + comesInAround - listedBusData.getComesInMin()) + " minutes.");
                } else {
                    textViewArrivesIn.setText("Probably left.");
                }
            }
            // Bus will leave sometime
            else if (listedBusData.getComesInMin() < 0) {
                textViewArrivesIn.setText("Arrives in " + Math.round(Math.abs(listedBusData.getComesInMin()) + numberOfStationsBetween + comesInAround) + " minutes.");
            }
        }
        // From last station to first
        else {
            for (Map.Entry<String, LatLng> entry : stations.entrySet()) {
                // If bus goes through that station
                if (listedBusData.getBus().getStationsFromLastStation().contains(entry.getKey())) {
                    locationStation.setLatitude(entry.getValue().getLatitude());
                    locationStation.setLongitude(entry.getValue().getLongitude());
                    if (locationHere.distanceTo(locationStation) < distance) {
                        closestStationName = entry.getKey();
                        distance = locationHere.distanceTo(locationStation);
                        closest.setLatitude(entry.getValue().getLatitude());
                        closest.setLongitude(entry.getValue().getLongitude());
                    }
                }
            }

            int start = 0, end = listedBusData.getBus().getStationsFromLastStation().size();
            for (int i = 0; i < listedBusData.getBus().getStationsFromLastStation().size(); ++i) {
                if (listedBusData.getBus().getStationsFromLastStation().get(i).equals(closestStationName)) {
                    end = i + 1;
                } else if (listedBusData.getBus().getStationsFromLastStation().get(i).equals(selectedStation)) {
                    start = i + 1;
                }
            }

            numberOfStationsBetween = end - start + 2;

            Location busLocation = new Location("");
            busLocation.setLatitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getLastStationName())).getLatitude());
            busLocation.setLongitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getLastStationName())).getLongitude());

            closestStation.setLatitude(closest.getLatitude());
            closestStation.setLongitude(closest.getLongitude());

            comesInAround = closestStation.distanceTo(busLocation) / 500; // speedIsXMetersPerMinute  500m/min = 30km/h
            comesInAround = Math.round(comesInAround);

            // Bus already left
            if (listedBusData.getComesInMin() > 0) {
                if (numberOfStationsBetween + comesInAround - listedBusData.getComesInMin() > 0) {
                    textViewArrivesIn.setText("Arrives in " + Math.round(numberOfStationsBetween + comesInAround - listedBusData.getComesInMin()) + " minutes.");
                } else {
                    textViewArrivesIn.setText("Probably left.");
                }
            }
            // Bus will leave sometime
            else if (listedBusData.getComesInMin() < 0) {
                textViewArrivesIn.setText("Arrives in " + Math.round(Math.abs(listedBusData.getComesInMin()) + numberOfStationsBetween + comesInAround) + " minutes.");
            }
        }

        mapViewListedRouteForUser = view.findViewById(R.id.mapView2);
        mapViewListedRouteForUser.onCreate(savedInstanceState);

        mapViewListedRouteForUser.getMapAsync(mapboxMap -> {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (sharedPreferences.getBoolean(DARK_MAP_THEME, true)) {
                mapboxMap.setStyle(Style.DARK);
            } else {
                mapboxMap.setStyle(Style.LIGHT);
            }

            // Map is set up and the style has loaded.
            // Now you can add data or make other map adjustments

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(Float.valueOf(latitude), Float.valueOf(longitude)))
                            .zoom(16)
                            .build()),
                    1000);

            assert Mapbox.getAccessToken() != null;
            NavigationRoute.Builder builder = NavigationRoute.builder(getActivity())
                    .accessToken(Mapbox.getAccessToken())
                    .origin(Point.fromLngLat(Float.valueOf(longitude), Float.valueOf(latitude)))
                    .destination(Point.fromLngLat(closest.getLongitude(), closest.getLatitude()))
                    .profile(DirectionsCriteria.PROFILE_WALKING);

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
                                navigationMapRoute = new NavigationMapRoute(null, mapViewListedRouteForUser, mapboxMap, R.style.NavigationMapRoute);
                            }
                            navigationMapRoute.addRoute(currentRoute);
                        }

                        @Override
                        public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                            Log.e(TAG, "Error: " + throwable.getMessage());
                        }
                    });
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapViewListedRouteForUser.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapViewListedRouteForUser.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapViewListedRouteForUser.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapViewListedRouteForUser.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapViewListedRouteForUser != null) {
            mapViewListedRouteForUser.onLowMemory();
        }
    }

    @Override
    public void dismiss() {
        if (mapViewListedRouteForUser != null && !mapViewListedRouteForUser.isDestroyed()) {
            mapViewListedRouteForUser.onPause();
            mapViewListedRouteForUser.onStop();
            mapViewListedRouteForUser.onDestroy();
            mapViewListedRouteForUser = null;
        }
        super.dismiss();
    }

    @Override
    public void onDestroyView() {
        if (mapViewListedRouteForUser != null) {
            mapViewListedRouteForUser.onDestroy();
        }
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapViewListedRouteForUser != null) {
            mapViewListedRouteForUser.onSaveInstanceState(outState);
        }
    }
}