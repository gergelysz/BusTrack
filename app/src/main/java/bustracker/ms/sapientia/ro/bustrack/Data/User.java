package bustracker.ms.sapientia.ro.bustrack.Data;

import com.google.firebase.Timestamp;

public class User  {

    private String id;
    private String bus;
    private String status;
    private Timestamp timestamp;
    private String latitude;
    private String longitude;
    private String direction;

    User() {
    }

    public User(String bus, String status, Timestamp timestamp) {
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
    }

    public User(String id, String bus, String status, Timestamp timestamp, String latitude, String longitude) {
        this.id = id;
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public User(String bus, String status, Timestamp timestamp, String latitude, String longitude) {
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public User(String bus, String status, Timestamp timestamp, String latitude, String longitude, String direction) {
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
    }

    public User(String id, String bus, String status, Timestamp timestamp, String latitude, String longitude, String direction) {
        this.id = id;
        this.bus = bus;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
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

    public String getDirection() {

        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
