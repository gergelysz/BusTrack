package bustracker.ms.sapientia.ro.bustrack.Data;

public class ListedBusData extends Bus {

    private Bus bus;
    //    private String realTimeBusData;
//    private String comesInMinutes;
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

    public void setBus(Bus bus) {
        this.bus = bus;
    }

//    public String getRealTimeBusData() {
//        return realTimeBusData;
//    }
//
//    public void setRealTimeBusData(String realTimeBusData) {
//        this.realTimeBusData = realTimeBusData;
//    }
//
//    public String getComesInMinutes() {
//        return comesInMinutes;
//    }
//
//    public void setComesInMinutes(String comesInMinutes) {
//        this.comesInMinutes = comesInMinutes;
//    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getComesInMin() {
        return comesInMin;
    }

    public void setComesInMin(int comesInMin) {
        this.comesInMin = comesInMin;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isRealTime() {
        return realTime;
    }

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }
}
