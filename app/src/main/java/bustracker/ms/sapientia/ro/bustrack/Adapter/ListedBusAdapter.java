package bustracker.ms.sapientia.ro.bustrack.Adapter;

import android.annotation.SuppressLint;
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

    @SuppressLint("SetTextI18n")
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
        TextView textViewRealTimeBusData = convertView.findViewById(R.id.textView_result_bus_info);
        TextView textViewBusComesIn = convertView.findViewById(R.id.textView_result_bus_comesIn);

        // Populate the data into the template view using the data object
        assert listedBusData != null;
        Calendar calendar = Calendar.getInstance();

        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            textViewBusNumber.setTextColor(Color.BLUE);
        } else {
            textViewBusNumber.setTextColor(Color.WHITE);
        }
        textViewBusNumber.setText(listedBusData.getBus().getNumber());
//        if(listedBusData.getRealTimeBusData().equals("Found!")) {
        if (listedBusData.isRealTime()) {
            textViewRealTimeBusData.setTextColor(Color.GREEN);
            textViewRealTimeBusData.setText(getContext().getString(R.string.real_time_bus_found));
        } else {
            textViewRealTimeBusData.setTextColor(Color.WHITE);
            textViewRealTimeBusData.setText(getContext().getString(R.string.no_real_time_bus_found));

            int comesInAbs = Math.abs(listedBusData.getComesInMin());

            if (listedBusData.getComesInMin() < 0) {
                if(listedBusData.getDirection() == 0) {
                    textViewBusComesIn.setText("Leaves " + listedBusData.getBus().getFirstStationName() + "\nstation in " + comesInAbs + " minutes.");
                } else {
                    textViewBusComesIn.setText("Leaves " + listedBusData.getBus().getLastStationName() + "\nstation in " + comesInAbs + " minutes.");
                }
            } else {
                if(listedBusData.getComesInMin() == 0) {
                    if(listedBusData.getDirection() == 0) {
                        textViewBusComesIn.setText("Leaves " + listedBusData.getBus().getFirstStationName() + "\nstation right now.");
                    } else {
                        textViewBusComesIn.setText("Leaves " + listedBusData.getBus().getLastStationName() + "\nstation right now.");
                    }
                } else {
                    if(listedBusData.getDirection() == 0) {
                        textViewBusComesIn.setText("Left " + listedBusData.getBus().getFirstStationName() + "\nstation " + comesInAbs + " minutes ago.");
                    } else {
                        textViewBusComesIn.setText("Left " + listedBusData.getBus().getLastStationName() + "\nstation " + comesInAbs + " minutes ago.");
                    }
                }
            }
        }


        // Return the completed view to render on screen
        return convertView;
    }
}
