package bustracker.ms.sapientia.ro.bustrack.Data;

public class ListedBusData {

    private String busNumber;
    private String realTimeBusData;
    private String comesInMinutes;

    public ListedBusData(String busNumber, String realTimeBusData, String comesInMinutes) {
        this.busNumber = busNumber;
        this.realTimeBusData = realTimeBusData;
        this.comesInMinutes = comesInMinutes;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
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
