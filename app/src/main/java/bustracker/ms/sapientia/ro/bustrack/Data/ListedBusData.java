package bustracker.ms.sapientia.ro.bustrack.Data;

public class ListedBusData extends Bus {

    private Bus bus;
    private String realTimeBusData;
    private String comesInMinutes;
    private int direction;
    private int comesInMin;

    public ListedBusData(Bus bus, String realTimeBusData, String comesInMinutes, int direction, int comesInMin) {
        this.bus = bus;
        this.realTimeBusData = realTimeBusData;
        this.comesInMinutes = comesInMinutes;
        this.direction = direction;
        this.comesInMin = comesInMin;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public String getRealTimeBusData() {
        return realTimeBusData;
    }

    public void setRealTimeBusData(String realTimeBusData) {
        this.realTimeBusData = realTimeBusData;
    }

    public String getComesInMinutes() {
        return comesInMinutes;
    }

    public void setComesInMinutes(String comesInMinutes) {
        this.comesInMinutes = comesInMinutes;
    }

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
}
