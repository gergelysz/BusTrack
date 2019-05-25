package bustracker.ms.sapientia.ro.bustrack.Data;

public class User {

    private String id;
    private String bus;
    private String status;
    private String latitude;
    private String longitude;
    private String direction;
    private String speed;

    public User(String bus, String status, String latitude, String longitude, String direction, String speed) {
        this.bus = bus;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.speed = speed;
    }

    public User(String id, String bus, String status, String latitude, String longitude, String direction, String speed) {
        this.id = id;
        this.bus = bus;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.speed = speed;
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

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }
}
