package bustracker.ms.sapientia.ro.bustrack.Fragments;


import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
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

/**
 * A simple {@link Fragment} subclass.
 */
public class ListedBusDetailsFragment extends DialogFragment {

    private static final String TAG = "LBDFragment";
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;
    private MapView mapViewListedRouteForUser;

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

        Map<String, LatLng> stations = (Map<String, LatLng>) getArguments().getSerializable("stations");

        assert stations != null;

        String latitude = getArguments().getString("latitude");
        String longitude = getArguments().getString("longitude");
        assert latitude != null;
        assert longitude != null;

        String closestStationName = getArguments().getString("closestStationName");
        String closestStationName2 = getArguments().getString("closestStationName2");
        assert closestStationName != null;
        assert closestStationName2 != null;

        String selectedStation = getArguments().getString("selectedStation");
        assert selectedStation != null;
        assert listedBusData != null;

        textViewInfoForBus.setText("Info for bus: " + listedBusData.getBus().getNumber());

        // Calculate arrival

        int startIndex = 0;
        int endIndex = 0;
        int numberOfStationsBetween;
        float comesInAround;

        if (listedBusData.getDirection() == 0) {

            for (String string : listedBusData.getBus().getStationsFromFirstStation()) {
                if (string.equals(closestStationName) || string.equals(closestStationName2)) {
                    startIndex = listedBusData.getBus().getStationsFromFirstStation().indexOf(string);
                }
                if (string.equals(selectedStation)) {
                    endIndex = listedBusData.getBus().getStationsFromFirstStation().indexOf(string);
                }
            }

            if (endIndex == 0) {
                endIndex = listedBusData.getBus().getStationsFromFirstStation().size() - 1;
            }

            numberOfStationsBetween = endIndex - startIndex;

            Location busLocation = new Location("");
            busLocation.setLatitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getFirstStationName())).getLatitude());
            busLocation.setLongitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getFirstStationName())).getLongitude());

            Location currentLocation = new Location("");
            currentLocation.setLatitude(Double.parseDouble(latitude));
            currentLocation.setLongitude(Double.parseDouble(longitude));
            comesInAround = currentLocation.distanceTo(busLocation) / 500; // speedIsXPerMinute

            // Bus already left
            if (listedBusData.getComesInMin() > 0) {
                if (listedBusData.getComesInMin() - numberOfStationsBetween - Math.round(comesInAround) <= 0) {
                    textViewArrivesIn.setText("Arrives in " + Math.abs(Math.abs(listedBusData.getComesInMin()) - numberOfStationsBetween - Math.round(comesInAround)) + " minutes.");
                } else {
                    textViewArrivesIn.setText("Probably left.");
                }
            }
            // Bus will leave sometime
            else if (listedBusData.getComesInMin() < 0) {
                textViewArrivesIn.setText("Arrives in " + (Math.abs(listedBusData.getComesInMin()) + numberOfStationsBetween + Math.round(comesInAround)) + " minutes.");
            }


        } else {

            for (String string : listedBusData.getBus().getStationsFromLastStation()) {
                if (string.equals(closestStationName) || string.equals(closestStationName2)) {
                    startIndex = listedBusData.getBus().getStationsFromLastStation().indexOf(string);
                }
                if (string.equals(selectedStation)) {
                    endIndex = listedBusData.getBus().getStationsFromLastStation().indexOf(string);
                }
            }

            if (endIndex == 0) {
                endIndex = listedBusData.getBus().getStationsFromLastStation().size() - 1;
            }

            numberOfStationsBetween = endIndex - startIndex;

            Location busLocation = new Location("asd");
            busLocation.setLatitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getLastStationName())).getLatitude());
            busLocation.setLongitude(Objects.requireNonNull(stations.get(listedBusData.getBus().getLastStationName())).getLongitude());

            Location currentLocation = new Location("asd");
            currentLocation.setLatitude(Double.parseDouble(latitude));
            currentLocation.setLongitude(Double.parseDouble(longitude));
            comesInAround = currentLocation.distanceTo(busLocation) / 500; // speedIsXMetersPerMinute

            // Bus already left
            if (listedBusData.getComesInMin() > 0) {
                if (listedBusData.getComesInMin() - numberOfStationsBetween - Math.round(comesInAround) <= 0) {
                    textViewArrivesIn.setText("Arrives in " + Math.abs(Math.abs(listedBusData.getComesInMin()) - numberOfStationsBetween - Math.round(comesInAround)) + " minutes.");
                } else {
                    textViewArrivesIn.setText("Probably left.");
                }
            }
            // Bus will leave sometime
            else if (listedBusData.getComesInMin() < 0) {
                textViewArrivesIn.setText("Arrives in " + (Math.abs(listedBusData.getComesInMin()) + numberOfStationsBetween + Math.round(comesInAround)) + " minutes.");
            }

            mapViewListedRouteForUser = view.findViewById(R.id.mapView2);
            mapViewListedRouteForUser.onCreate(savedInstanceState);

            mapViewListedRouteForUser.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.DARK, style -> {

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
                        .destination(Point.fromLngLat(Objects.requireNonNull(stations.get(closestStationName)).getLongitude(), Objects.requireNonNull(stations.get(closestStationName)).getLatitude()))
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
            }));
        }
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