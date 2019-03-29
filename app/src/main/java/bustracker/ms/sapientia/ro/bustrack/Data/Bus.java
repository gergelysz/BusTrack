package bustracker.ms.sapientia.ro.bustrack.Data;

import java.util.ArrayList;
import java.util.List;

public class Bus  {

    private String number;
    private String firstStationName;
    private String lastStationName;
    private List<String> stations = new ArrayList<>();
    private List<String> firstStationLeavingTime = new ArrayList<>();
    private List<String> lastStationLeavingTime = new ArrayList<>();
    private List<String> stationsFromFirstStation = new ArrayList<>();
    private List<String> stationsFromLastStation = new ArrayList<>();

    Bus() {
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getFirstStationName() {
        return firstStationName;
    }

    public void setFirstStationName(String firstStationName) {
        this.firstStationName = firstStationName;
    }

    public String getLastStationName() {
        return lastStationName;
    }

    public void setLastStationName(String lastStationName) {
        this.lastStationName = lastStationName;
    }

    public List<String> getStations() {
        return stations;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
    }

    public List<String> getFirstStationLeavingTime() {
        return firstStationLeavingTime;
    }

    public void setFirstStationLeavingTime(List<String> firstStationLeavingTime) {
        this.firstStationLeavingTime = firstStationLeavingTime;
    }

    public List<String> getLastStationLeavingTime() {
        return lastStationLeavingTime;
    }

    public void setLastStationLeavingTime(List<String> lastStationLeavingTime) {
        this.lastStationLeavingTime = lastStationLeavingTime;
    }

    public List<String> getStationsFromFirstStation() {
        return stationsFromFirstStation;
    }

    public void setStationsFromFirstStation(List<String> stationsFromFirstStation) {
        this.stationsFromFirstStation = stationsFromFirstStation;
    }

    public List<String> getStationsFromLastStation() {
        return stationsFromLastStation;
    }

    public void setStationsFromLastStation(List<String> stationsFromLastStation) {
        this.stationsFromLastStation = stationsFromLastStation;
    }


}
