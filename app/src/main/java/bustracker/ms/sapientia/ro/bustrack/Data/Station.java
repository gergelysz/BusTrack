package bustracker.ms.sapientia.ro.bustrack.Data;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class Station {

    private LatLng coordinates;
    private String name;

    public Station() {
    }

    public Station(LatLng coordinates, String name) {
        this.coordinates = coordinates;
        this.name = name;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
