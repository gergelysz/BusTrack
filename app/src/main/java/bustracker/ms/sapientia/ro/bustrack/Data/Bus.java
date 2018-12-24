package bustracker.ms.sapientia.ro.bustrack.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bus extends User {

    private String firstStationName;
    private String lastStationName;
    private List<String> stations = new ArrayList<>();
    private List<String> firstStationLeavingTime = new ArrayList<>();
    private List<String> lastStationLeavingTime = new ArrayList<>();
    private Map<String, String> stationData = new HashMap<>();
}
