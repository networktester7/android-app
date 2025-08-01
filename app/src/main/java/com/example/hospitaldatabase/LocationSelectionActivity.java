package com.example.hospitaldatabase; // Your package name

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap; // Required if you use HashMap for data (not for this example)
import java.util.Map; // Required if you use Map for data (not for this example)

public class LocationSelectionActivity extends AppCompatActivity {

    private Spinner spinnerLocations;
    private Button btnLoadInventory;
    private TextView tvLocationStatus;

    private RequestQueue requestQueue;
    private ArrayList<LocationItem> locationList;
    private ArrayAdapter<LocationItem> spinnerAdapter;

    private static final String GET_LOCATIONS_URL = "http://172.206.33.45/get_locations.php"; // *** UPDATE THIS URL ***

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selection);

        spinnerLocations = findViewById(R.id.spinnerLocations);
        btnLoadInventory = findViewById(R.id.btnLoadInventory);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);

        requestQueue = Volley.newRequestQueue(this);

        locationList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, // Default layout for spinner items
                locationList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Layout for dropdown list
        spinnerLocations.setAdapter(spinnerAdapter);

        fetchLocations();

        btnLoadInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinnerLocations.getSelectedItem() != null) {
                    LocationItem selectedLocation = (LocationItem) spinnerLocations.getSelectedItem();
                    if (selectedLocation != null) {
                        Intent intent = new Intent(LocationSelectionActivity.this, DatabaseActivity.class);
                        intent.putExtra("LOCATION_ID", selectedLocation.getId());
                        intent.putExtra("LOCATION_NAME", selectedLocation.getName()); // Optional: pass name for display
                        startActivity(intent);
                        finish(); // Finish this activity so user can't go back to it via back button
                    } else {
                        Toast.makeText(LocationSelectionActivity.this, "Please select a valid location.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LocationSelectionActivity.this, "No locations available.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchLocations() {
        tvLocationStatus.setText("Loading locations...");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, GET_LOCATIONS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");

                            if (status.equals("success")) {
                                JSONArray jsonArray = jsonResponse.getJSONArray("data");
                                locationList.clear();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject location = jsonArray.getJSONObject(i);
                                    int id = location.getInt("location_id");
                                    String name = location.getString("location_name");
                                    locationList.add(new LocationItem(id, name));
                                }
                                spinnerAdapter.notifyDataSetChanged();
                                tvLocationStatus.setText("Locations loaded.");
                                if (locationList.isEmpty()) {
                                    Toast.makeText(LocationSelectionActivity.this, "No locations found.", Toast.LENGTH_LONG).show();
                                    btnLoadInventory.setEnabled(false); // Disable button if no locations
                                } else {
                                    btnLoadInventory.setEnabled(true);
                                }
                            } else {
                                String message = jsonResponse.optString("message", "Unknown error loading locations.");
                                tvLocationStatus.setText("Failed to load locations: " + message);
                                Toast.makeText(LocationSelectionActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            tvLocationStatus.setText("Error parsing location data.");
                            Toast.makeText(LocationSelectionActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("LocationActivity", "JSON parsing error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvLocationStatus.setText("Network error loading locations.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(LocationSelectionActivity.this, "Network error: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("LocationActivity", "Volley error: " + errorMessage);
                        btnLoadInventory.setEnabled(false); // Disable button on network error
                    }
                });
        requestQueue.add(stringRequest);
    }
}