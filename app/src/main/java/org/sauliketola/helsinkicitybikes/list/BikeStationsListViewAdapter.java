package org.sauliketola.helsinkicitybikes.list;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sauliketola.helsinkicitybikes.R;
import org.sauliketola.helsinkicitybikes.domain.BikeStation;

import java.util.Comparator;
import java.util.List;

public class BikeStationsListViewAdapter extends ArrayAdapter<BikeStation> {
    private final Context context;
    private final List<BikeStation> values;

    private Location mLocation;

    public BikeStationsListViewAdapter(Context context, List<BikeStation> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item, parent, false);

        TextView textViewName = (TextView) rowView.findViewById(R.id.name);
        textViewName.setText(values.get(position).getName());

        TextView textViewBikes = (TextView) rowView.findViewById(R.id.bikesAvailable);
        textViewBikes.setText("Bikes: " + values.get(position).getBikesAvailable()  + " / " + (values.get(position).getBikesAvailable() + values.get(position).getSpacesAvailable()));

        TextView textViewDistance = (TextView) rowView.findViewById(R.id.distance);
        Float distance = values.get(position).getDistance();
        textViewDistance.setText("Distance: " + (distance != null ? Math.round(values.get(position).getDistance()) : "?") + " m");

        return rowView;
    }

    public void setLocation(Location mLocation){
        this.mLocation = mLocation;

        // update distances
        for(BikeStation bikeStation : values){

            float[] distanceResult = new float[3];
            Location.distanceBetween(mLocation.getLatitude(), mLocation.getLongitude(), bikeStation.getLatitude(), bikeStation.getLongitude(), distanceResult);

            bikeStation.setDistance(distanceResult[0]);
        }

        this.notifyDataSetChanged();
        this.sort(new Comparator<BikeStation>() {
            @Override
            public int compare(BikeStation lhs, BikeStation rhs) {
                return lhs.getDistance().compareTo(rhs.getDistance());
            }
        });
    }


}
