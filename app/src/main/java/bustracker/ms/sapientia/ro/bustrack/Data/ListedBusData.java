package bustracker.ms.sapientia.ro.bustrack.Data;

public class ListedBusData {

    private Bus bus;
    private String realTimeBusData;
    private String comesInMinutes;

    public ListedBusData(Bus bus, String realTimeBusData, String comesInMinutes) {
        this.bus = bus;
        this.realTimeBusData = realTimeBusData;
        this.comesInMinutes = comesInMinutes;
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
}
