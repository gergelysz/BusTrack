package bustracker.ms.sapientia.ro.bustrack.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;

import bustracker.ms.sapientia.ro.bustrack.Data.ListedBusData;
import bustracker.ms.sapientia.ro.bustrack.R;

public class ListedBusAdapter extends ArrayAdapter<ListedBusData> {

    public ListedBusAdapter(Context context, ArrayList<ListedBusData> listedBuses) {
        super(context, 0, listedBuses);
    }

    @NotNull
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {

        // Get the data item for this position
        ListedBusData listedBusData = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.result_buses_row_item, parent, false);
        }

        // Lookup view for data population
        TextView textViewBusNumber = convertView.findViewById(R.id.textView_result_bus_number);
        TextView textViewBusComesIn = convertView.findViewById(R.id.textView_result_bus_comesIn);

        // Populate the data into the template view using the data object
        assert listedBusData != null;
        Calendar calendar = Calendar.getInstance();

        // Draw the bus number with white on weekdays and blue on weekends
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            textViewBusNumber.setTextColor(Color.BLUE);
        } else {
            textViewBusNumber.setTextColor(Color.WHITE);
        }

        textViewBusNumber.setText(listedBusData.getBus().getNumber());

        if (listedBusData.isRealTime()) {
            textViewBusComesIn.setTextColor(Color.GREEN);
            textViewBusComesIn.setText(getContext().getString(R.string.real_time_bus_found));
        } else {

            int comesInAbs = Math.abs(listedBusData.getComesInMin());

            if (listedBusData.getComesInMin() < 0) {
                if (listedBusData.getDirection() == 0) {
                    if(comesInAbs == 1) {
                        textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.leaves_station_in_minutes, 1, comesInAbs, listedBusData.getBus().getFirstStationName()));
                    } else {
                        textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.leaves_station_in_minutes, 2, comesInAbs,listedBusData.getBus().getFirstStationName()));
                    }
                } else {
                    if(comesInAbs == 1) {
                        textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.leaves_station_in_minutes, 1, comesInAbs, listedBusData.getBus().getLastStationName()));
                    } else {
                        textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.leaves_station_in_minutes, 2, comesInAbs,listedBusData.getBus().getLastStationName()));
                    }
                }
            } else {
                if (listedBusData.getComesInMin() == 0) {
                    if (listedBusData.getDirection() == 0) {
                        textViewBusComesIn.setText(getContext().getString(R.string.leaves_station_right_now, listedBusData.getBus().getFirstStationName()));
                    } else {
                        textViewBusComesIn.setText(getContext().getString(R.string.leaves_station_right_now, listedBusData.getBus().getLastStationName()));
                    }
                } else {
                    if (listedBusData.getDirection() == 0) {
                        if(comesInAbs == 1) {
                            textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.left_station_minutes_ago, 1, comesInAbs, listedBusData.getBus().getFirstStationName()));
                        } else {
                            textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.left_station_minutes_ago, 2, comesInAbs, listedBusData.getBus().getFirstStationName()));
                        }
                    } else {
                        if(comesInAbs == 1) {
                            textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.left_station_minutes_ago, 1, comesInAbs, listedBusData.getBus().getLastStationName()));
                        } else {
                            textViewBusComesIn.setText(getContext().getResources().getQuantityString(R.plurals.left_station_minutes_ago, 2, comesInAbs, listedBusData.getBus().getLastStationName()));
                        }
                    }
                }
            }
        }
        return convertView;
    }
}
