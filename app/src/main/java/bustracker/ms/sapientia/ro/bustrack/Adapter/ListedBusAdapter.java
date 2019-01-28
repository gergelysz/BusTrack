package bustracker.ms.sapientia.ro.bustrack.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import bustracker.ms.sapientia.ro.bustrack.Data.ListedBusData;
import bustracker.ms.sapientia.ro.bustrack.R;

public class ListedBusAdapter extends ArrayAdapter<ListedBusData> {

    public ListedBusAdapter(Context context, ArrayList<ListedBusData> listedBuses) {
        super(context, 0, listedBuses);
    }

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
        textViewBusNumber.setText(listedBusData.getBusNumber());
        textViewRealTimeBusData.setText(listedBusData.getRealTimeBusData());
        textViewBusComesIn.setText(listedBusData.getComesInMinutes());
        // Return the completed view to render on screen
        return convertView;
    }
}
