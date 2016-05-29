package org.sauliketola.helsinkicitybikes.reader;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sauliketola.helsinkicitybikes.domain.BikeStation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CityBikeStationsReader {

    public List<BikeStation> readBikeStations() throws IOException {
        URL bikeXml = new URL("http://api.digitransit.fi/routing/v1/routers/hsl/bike_rental");

        InputStream inputStream = bikeXml.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        Log.i("Stations", "Station data: " + result.toString());

        List<BikeStation> stations = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(result.toString());
            JSONArray array =  jsonObject.getJSONArray("stations");

            for (int i=0; i < array.length(); i++) {

                JSONObject jo = array.getJSONObject(i);
                BikeStation bikeStation = new BikeStation();
                bikeStation.setId(jo.getString("id"));
                bikeStation.setName(jo.getString("name"));
                bikeStation.setBikesAvailable(Integer.parseInt(jo.getString("bikesAvailable")));
                bikeStation.setSpacesAvailable(Integer.parseInt(jo.getString("spacesAvailable")));
                bikeStation.setLongitude(Double.parseDouble(jo.getString("x")));
                bikeStation.setLatitude(Double.parseDouble(jo.getString("y")));

                stations.add(bikeStation);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return stations;
    }
}
