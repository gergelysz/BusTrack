package bustracker.ms.sapientia.ro.bustrack.Data;

import java.util.ArrayList;
import java.util.List;

public class Bus {

    private String number;
    private String firstStationName;
    private String lastStationName;
    private final List<String> firstStationLeavingTime = new ArrayList<>();
    private final List<String> lastStationLeavingTime = new ArrayList<>();
    private final List<String> firstStationLeavingTimeWeekend = new ArrayList<>();
    private final List<String> lastStationLeavingTimeWeekend = new ArrayList<>();
    private final List<String> stationsFromFirstStation = new ArrayList<>();
    private final List<String> stationsFromLastStation = new ArrayList<>();

    Bus() {
    }

    public String getNumber() {
        return number;
    }

    public String getFirstStationName() {
        return firstStationName;
    }

    public String getLastStationName() {
        return lastStationName;
    }

    public List<String> getFirstStationLeavingTime() {
        return firstStationLeavingTime;
    }

    public List<String> getLastStationLeavingTime() {
        return lastStationLeavingTime;
    }

    public List<String> getFirstStationLeavingTimeWeekend() {
        return firstStationLeavingTimeWeekend;
    }

    public List<String> getLastStationLeavingTimeWeekend() {
        return lastStationLeavingTimeWeekend;
    }

    public List<String> getStationsFromFirstStation() {
        return stationsFromFirstStation;
    }

    public List<String> getStationsFromLastStation() {
        return stationsFromLastStation;
    }

}