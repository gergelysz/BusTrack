package bustracker.ms.sapientia.ro.bustrack.Data;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.sql.Timestamp;

public class User {

    private String id;
    private String bus;
    private String status;
    private Timestamp timestamp;
    private LatLng coordinates;

    public User() {
    }

    public User(String id, String bus, String status, Timestamp timestamp, LatLng coordinates) {
        this.id = id;
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
        this.coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBus() {
        return bus;
    }

    public void setBus(String bus) {
        this.bus = bus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }
}
