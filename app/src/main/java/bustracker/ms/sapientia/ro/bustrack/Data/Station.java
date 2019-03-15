package bustracker.ms.sapientia.ro.bustrack.Data;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class Station {

    private LatLng coordinates;
    private String location;
    private String latitude;
    private String longitude;
    private String name;

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
