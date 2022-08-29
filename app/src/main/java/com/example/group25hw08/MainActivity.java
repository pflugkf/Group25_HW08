package com.example.group25hw08;

/**
 * Assignment #: HW08
 * File Name: Group25_HW08 MainActivity.java
 * Full Name: Kristin Pflug
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final OkHttpClient client = new OkHttpClient();

    private GoogleMap map;

    private static int AUTOCOMPLETE_REQUEST_CODE = 1;
    TextView startingAddressTextView, endingAddressTextView;
    Button getStart, getEnd, getDirections, findRestaurants, findGas;
    String buttonPressed = "";
    Place startPlace;
    Place endPlace;
    ArrayList<LatLng> points;
    LatLngBounds bounds;
    int totalDistance = 0;
    int totalDistanceInMeters = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        // Initialize the SDK
        Places.initialize(getApplicationContext(), "AIzaSyCWgx5hYKbV9gh9sw3ZVltuRyAUtnIozh0");

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        startingAddressTextView = findViewById(R.id.startingAddress);
        endingAddressTextView = findViewById(R.id.endingAddress);
        getStart = findViewById(R.id.enterStartAddress);
        getEnd = findViewById(R.id.enterEndAddress);
        getDirections = findViewById(R.id.button_getDirections);

        findRestaurants = findViewById(R.id.findRestaurants);
        findGas = findViewById(R.id.findGasStations);

        findRestaurants.setClickable(false);
        findRestaurants.setVisibility(View.INVISIBLE);
        findGas.setClickable(false);
        findGas.setVisibility(View.INVISIBLE);

        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);

        getStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the autocomplete intent.
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(view.getContext());
                buttonPressed = "start";
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        getEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the autocomplete intent.
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(view.getContext());
                buttonPressed = "end";
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        getDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startPlace != null && endPlace != null) {
                    HttpUrl dirURL = HttpUrl.parse("https://maps.googleapis.com/maps/api/directions/json").newBuilder()
                            .addQueryParameter("destination", "place_id:" + endPlace.getId())
                            .addQueryParameter("origin", "place_id:" + startPlace.getId())
                            .addQueryParameter("key", "AIzaSyCWgx5hYKbV9gh9sw3ZVltuRyAUtnIozh0")
                            .build();

                    Request directionsRequest = new Request.Builder()
                            .url(dirURL)
                            .build();

                    client.newCall(directionsRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if(response.isSuccessful()) {
                                try {
                                    JSONObject directionJSONObject = new JSONObject(response.body().string());
                                    JSONArray routeInfoArray = directionJSONObject.getJSONArray("routes");
                                    JSONObject routeJSONObject = routeInfoArray.getJSONObject(0);

                                    JSONArray legInfoArray = routeJSONObject.getJSONArray("legs");

                                    for(int i = 0; i < legInfoArray.length(); i++) {
                                        JSONObject legsArrayObj = legInfoArray.getJSONObject(i);
                                        JSONObject distanceObj = legsArrayObj.getJSONObject("distance");
                                        totalDistanceInMeters += distanceObj.getInt("value");
                                    }

                                    totalDistance = totalDistanceInMeters / 1609;

                                    JSONObject overviewPolylineObject = routeJSONObject.getJSONObject("overview_polyline");
                                    String encodedPolyline = overviewPolylineObject.getString("points");
                                    points = (ArrayList<LatLng>) PolyUtil.decode(encodedPolyline);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Polyline polyline1 = map.addPolyline(new PolylineOptions()
                                                    .startCap(new RoundCap())
                                                    .endCap(new RoundCap())
                                                    .addAll(points));

                                            for(LatLng point : points) {
                                                builder.include(point);
                                            }
                                            bounds = builder.build();

                                            LatLng startPoint = points.get(0);
                                            LatLng endPoint = points.get(points.size() -1);

                                            map.addMarker(new MarkerOptions()
                                                    .position(startPoint)
                                                    .title("Starting Point"));

                                            map.addMarker(new MarkerOptions()
                                                    .position(endPoint)
                                                    .title("Ending Point"));

                                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                                        }
                                    });

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    findRestaurants.setClickable(true);
                    findRestaurants.setVisibility(View.VISIBLE);
                    findGas.setClickable(true);
                    findGas.setVisibility(View.VISIBLE);
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                    b.setTitle("Error")
                            .setMessage("Please fill in both textboxes!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    b.create().show();
                }
            }
        });

        findRestaurants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                find("restaurant");
            }
        });

        findGas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                find("gas_station");
            }
        });
    }

    public void find(String queryKeyword) {
        LatLng center = bounds.getCenter();

        HttpUrl queryURL = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/textsearch/json").newBuilder()
                .addQueryParameter("query", queryKeyword)
                .addQueryParameter("location", center.latitude + "," + center.longitude)
                .addQueryParameter("radius", String.valueOf(totalDistanceInMeters/2))
                .addQueryParameter("key", "AIzaSyCWgx5hYKbV9gh9sw3ZVltuRyAUtnIozh0")
                .build();

        Request searchRequest = new Request.Builder()
                .url(queryURL)
                .build();

        client.newCall(searchRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    try {
                        JSONObject resultJSONObject = new JSONObject(response.body().string());
                        JSONArray routeInfoArray = resultJSONObject.getJSONArray("results");

                        for(int i = 0; i < routeInfoArray.length(); i++) {
                            JSONObject resultsArrayObject = routeInfoArray.getJSONObject(i);
                            JSONObject geometryResultsObj = resultsArrayObject.getJSONObject("geometry");

                            String restaurantName = resultsArrayObject.getString("name");

                            JSONObject latLongObject = geometryResultsObj.getJSONObject("location");
                            double latitude = latLongObject.getDouble("lat");
                            double longitude = latLongObject.getDouble("lng");

                            LatLng location = new LatLng(latitude, longitude);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(queryKeyword.equals("restaurant")) {
                                        map.addMarker(new MarkerOptions()
                                                .position(location)
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                                .title(restaurantName));
                                    } else if(queryKeyword.equals("gas_station")){
                                        map.addMarker(new MarkerOptions()
                                                .position(location)
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                .title(restaurantName));
                                    }

                                }
                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Get a handle to the GoogleMap object and display marker.
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                if(buttonPressed.equals("start")){
                    startingAddressTextView.setText(place.getAddress());
                    startPlace = place;
                } else if(buttonPressed.equals("end")){
                    endingAddressTextView.setText(place.getAddress());
                    endPlace = place;
                }

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i("q/test", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}