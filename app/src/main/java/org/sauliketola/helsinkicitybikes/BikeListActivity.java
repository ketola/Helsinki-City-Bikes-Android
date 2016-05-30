package org.sauliketola.helsinkicitybikes;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.sauliketola.helsinkicitybikes.domain.BikeStation;
import org.sauliketola.helsinkicitybikes.list.BikeStationsListViewAdapter;
import org.sauliketola.helsinkicitybikes.reader.CityBikeStationsReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BikeListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private ArrayList<BikeStation> stations = new ArrayList<BikeStation>();
    private Map<String, BikeStation> idToStationsMap = new HashMap<String, BikeStation>();

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private Runnable listRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_list);

        ListAdapter adapter = new BikeStationsListViewAdapter(
                this, stations);
        ((ListView) findViewById(R.id.listView)).setAdapter(adapter);
        ((ListView) findViewById(R.id.listView)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent mapIntent = new Intent(BikeListActivity.this, MapActivity.class);
                mapIntent.putExtra("latitude", stations.get(position).getLatitude());
                mapIntent.putExtra("longitude", stations.get(position).getLongitude());
                mapIntent.putExtra("name", stations.get(position).getName());
                mapIntent.putExtra("stations", stations);
                BikeListActivity.this.startActivity(mapIntent);
            }
        });

        Timer stationDataTimer = new Timer();
        stationDataTimer.scheduleAtFixedRate( new TimerTask() {
            public void run() {
                try{
                    new ReadStations().execute();
                }
                catch (Exception e) {}
            }
        }, 0, 30000);

        createGoogleApiClient();
        createLocationRequest();

        new GetLocation().execute();

        listRefresh = new Runnable() {
            public void run() {
                ((ListView) findViewById(R.id.listView)).invalidateViews();
                ((ListView) findViewById(R.id.listView)).refreshDrawableState();
            }
        };
    }

    private void createGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Location", "Permission denied");
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if(mGoogleApiClient.isConnected())
            startLocationUpdates();
        super.onResume();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        readLastLocation();
        startLocationUpdates();
    }

    private void readLastLocation() {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Location", "Permission denied");
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        ((BikeStationsListViewAdapter)((ListView) findViewById(R.id.listView)).getAdapter()).updateDistances(mLastLocation);
    }

    private class ReadStations extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            Log.i("Stations", "Start reading station data");
            CityBikeStationsReader cityBikeStationsReader = new CityBikeStationsReader();

            try {
                List<BikeStation> bikeStations = cityBikeStationsReader.readBikeStations();
                if(BikeListActivity.this.stations.isEmpty()){
                    BikeListActivity.this.stations.addAll(bikeStations);
                    for(BikeStation bikeStation : bikeStations){
                        idToStationsMap.put(bikeStation.getId(), bikeStation);
                    }
                } else {
                    for(BikeStation bikeStation : bikeStations){
                        BikeStation stationToUpdate = idToStationsMap.get(bikeStation.getId());
                        if(stationToUpdate != null){
                            stationToUpdate.setBikesAvailable(bikeStation.getBikesAvailable());
                            stationToUpdate.setSpacesAvailable(bikeStation.getSpacesAvailable());
                        }
                    }
                }
                runOnUiThread(listRefresh);
                Log.i("Stations", "Station data updated");
            } catch (IOException e){
                Log.e("BikeListActivity", "Couldn't read bike stations " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //((ListView) findViewById(R.id.listView)).getAdapter().;
        }
    }

    private class GetLocation extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            Log.i("Location", "Wait for location..");


            while (mLastLocation == null){
                Log.i("Location", "Waiting..");
                try {
                    Thread.sleep(100);
                } catch (Exception e){

                }
            }

            //runOnUiThread(listRefresh);
            Log.i("Location", "Location found " + mLastLocation.getAltitude() + " " + mLastLocation.getLongitude());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ((BikeStationsListViewAdapter)((ListView) findViewById(R.id.listView)).getAdapter()).updateDistances(mLastLocation);
        }
    }
}
