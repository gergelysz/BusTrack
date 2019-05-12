package bustracker.ms.sapientia.ro.bustrack.Data;

import java.io.Serializable;

public class ListedBusData extends Bus implements Serializable {

    private Bus bus;
    private boolean realTime;
    private int direction;
    private int comesInMin;
    private User user;

    public ListedBusData(Bus bus, boolean realTime, int direction, int comesInMin) {
        this.bus = bus;
        this.realTime = realTime;
        this.direction = direction;
        this.comesInMin = comesInMin;
    }

    public ListedBusData(Bus bus, boolean realTime, int direction, int comesInMin, User user) {
        this.bus = bus;
        this.realTime = realTime;
        this.direction = direction;
        this.comesInMin = comesInMin;
        this.user = user;
    }

    public Bus getBus() {
        return bus;
    }

    public int getDirection() {
        return direction;
    }

    public int getComesInMin() {
        return comesInMin;
    }

    public User getUser() {
        return user;
    }

    public boolean isRealTime() {
        return realTime;
    }

}
