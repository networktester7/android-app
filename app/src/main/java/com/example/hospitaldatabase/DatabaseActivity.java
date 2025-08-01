package com.example.hospitaldatabase; // Make sure this matches your package name

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DatabaseActivity extends AppCompatActivity {

    // UI Components
    private EditText etItemName, etQuantityChange, etSearch;
    private Button btnAddQuantity, btnSubtractQuantity;
    private TextView tvStatus;
    private ListView lvInventoryItems;

    // Data and Adapter for ListView
    private ArrayAdapter<InventoryItem> inventoryAdapter;
    private ArrayList<InventoryItem> inventoryList;       // This holds the CURRENTLY displayed (filtered) items
    private ArrayList<InventoryItem> fullInventoryList;   // This holds the full, unfiltered list from the server

    // Volley for Network Requests
    private RequestQueue requestQueue;

    // Selected Item for Update (from ListView click)
    private int selectedItemId = -1; // Stores the item_id of the currently selected item

    // Hospital Location Data (received from LocationSelectionActivity)
    private int currentHospitalLocationId = -1;
    private String currentHospitalLocationName = "Unknown Location";

    // IMPORTANT: Update these URLs to your actual server endpoints!
    private static final String INVENTORY_FETCH_URL = "http://172.206.33.45/get_inventory.php";
    private static final String INVENTORY_UPDATE_URL = "http://172.206.33.45/update_inventory.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        // 1. Initialize UI Components
        etItemName = findViewById(R.id.etItemName);
        etQuantityChange = findViewById(R.id.etQuantityChange);
        etSearch = findViewById(R.id.etSearch); // Initialize search EditText
        btnAddQuantity = findViewById(R.id.btnAddQuantity);
        btnSubtractQuantity = findViewById(R.id.btnSubtractQuantity);
        tvStatus = findViewById(R.id.tvStatus); // Initialize status TextView
        lvInventoryItems = findViewById(R.id.lvInventoryItems);

        // Make etItemName non-editable by direct typing, only by selecting from list
        etItemName.setFocusable(false);
        etItemName.setClickable(false);

        // 2. Initialize Data Lists and Adapter
        inventoryList = new ArrayList<>();
        fullInventoryList = new ArrayList<>(); // Initialize the full list
        inventoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, // Default layout for list items
                inventoryList);
        lvInventoryItems.setAdapter(inventoryAdapter);

        // 3. Initialize Volley Request Queue
        requestQueue = Volley.newRequestQueue(this);

        // 4. Retrieve Location Data from Intent
        // This activity should only be launched with a valid location ID
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("LOCATION_ID")) {
            currentHospitalLocationId = intent.getIntExtra("LOCATION_ID", -1);
            currentHospitalLocationName = intent.getStringExtra("LOCATION_NAME");
            // Update the title TextView in your layout to show the current location
            TextView tvTitle = findViewById(R.id.tvTitle);
            if (tvTitle != null) {
                tvTitle.setText("Inventory: " + currentHospitalLocationName);
            }
        } else {
            // If no location ID is passed, this is an error state.
            Toast.makeText(this, "Error: No hospital location selected!", Toast.LENGTH_LONG).show();
            finish(); // Close this activity
            return; // Stop further execution of onCreate
        }

        // 5. Set up Listeners

        // Item click listener for ListView
        lvInventoryItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InventoryItem selectedItem = inventoryList.get(position);
                etItemName.setText(selectedItem.getName());
                selectedItemId = selectedItem.getId();
                etQuantityChange.setText(""); // Clear quantity change field
                tvStatus.setText("Selected: " + selectedItem.getName() + " (Current: " + selectedItem.getQuantity() + ")");
            }
        });

        // TextWatcher for search EditText
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int intBefore, int count) {
                // Filter the list as the user types
                filterInventoryList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Button Listeners for quantity update
        btnAddQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItemId != -1) {
                    try {
                        int quantityChange = Integer.parseInt(etQuantityChange.getText().toString());
                        if (quantityChange > 0) {
                            updateInventoryQuantity(selectedItemId, quantityChange, "add");
                        } else {
                            Toast.makeText(DatabaseActivity.this, "Quantity to add must be positive.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(DatabaseActivity.this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DatabaseActivity.this, "Please select an item first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSubtractQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItemId != -1) {
                    try {
                        int quantityChange = Integer.parseInt(etQuantityChange.getText().toString());
                        if (quantityChange > 0) {
                            updateInventoryQuantity(selectedItemId, quantityChange, "subtract");
                        } else {
                            Toast.makeText(DatabaseActivity.this, "Quantity to subtract must be positive.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(DatabaseActivity.this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DatabaseActivity.this, "Please select an item first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear search text and re-fetch the list when returning to this activity
        etSearch.setText("");
        fetchInventoryList(); // This will also apply the empty search filter
    }

    /**
     * Fetches the inventory list from the server for the current hospital location.
     */
    private void fetchInventoryList() {
        if (currentHospitalLocationId == -1) {
            tvStatus.setText("Error: Location not set. Cannot fetch inventory.");
            Toast.makeText(this, "A hospital location must be selected.", Toast.LENGTH_LONG).show();
            return;
        }

        tvStatus.setText("Loading inventory for " + currentHospitalLocationName + "...");

        // Using POST method to send location_id securely
        StringRequest stringRequest = new StringRequest(Request.Method.POST, INVENTORY_FETCH_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");

                            if (status.equals("success")) {
                                JSONArray jsonArray = jsonResponse.getJSONArray("data");
                                fullInventoryList.clear(); // Clear the full list before populating
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    int itemId = item.getInt("item_id");
                                    String itemName = item.getString("item_name");
                                    int quantity = item.getInt("quantity");
                                    fullInventoryList.add(new InventoryItem(itemId, itemName, quantity));
                                }
                                // After fetching, filter based on current search query (empty string if no query)
                                filterInventoryList(etSearch.getText().toString());
                                tvStatus.setText("Inventory loaded for " + currentHospitalLocationName + ".");

                                if (fullInventoryList.isEmpty()) {
                                    Toast.makeText(DatabaseActivity.this, "No inventory items found for this location.", Toast.LENGTH_LONG).show();
                                }

                            } else {
                                String message = jsonResponse.optString("message", "Unknown error loading inventory.");
                                tvStatus.setText("Failed to load inventory: " + message);
                                Toast.makeText(DatabaseActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                                Log.e("DatabaseActivity", "Server response status: " + status + ", message: " + message);
                            }
                        } catch (JSONException e) {
                            tvStatus.setText("Error parsing inventory data.");
                            Toast.makeText(DatabaseActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("DatabaseActivity", "JSON parsing error (fetch): " + e.getMessage(), e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvStatus.setText("Network error loading inventory.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(DatabaseActivity.this, "Network error: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("DatabaseActivity", "Volley error fetching inventory: " + errorMessage, error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // Send the selected hospital location ID with the request
                params.put("location_id", String.valueOf(currentHospitalLocationId));
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    /**
     * Filters the currently displayed inventory list based on the provided search query.
     * This operates on the fullInventoryList and updates the inventoryList (bound to adapter).
     * @param query The search string.
     */
    private void filterInventoryList(String query) {
        inventoryList.clear(); // Clear the currently displayed (filtered) list

        if (query.isEmpty()) {
            // If the query is empty, show all items from the full list
            inventoryList.addAll(fullInventoryList);
        } else {
            // Convert query to lowercase for a case-insensitive search
            query = query.toLowerCase(Locale.getDefault());
            for (InventoryItem item : fullInventoryList) {
                // Check if the item name contains the search query (case-insensitive)
                if (item.getName().toLowerCase(Locale.getDefault()).contains(query)) {
                    inventoryList.add(item); // Add matching item to the filtered list
                }
            }
        }
        inventoryAdapter.notifyDataSetChanged(); // Tell the adapter to refresh the ListView

        // IMPORTANT: Reset selected item when the list is filtered or changed
        // This prevents updating an item that's no longer visible or intended
        selectedItemId = -1;
        etItemName.setText(""); // Clear the selected item name display
        etQuantityChange.setText(""); // Also clear quantity change
    }


    /**
     * Sends a request to the server to update the quantity of an inventory item.
     * Includes location_id in the request to ensure the correct item at the correct location is updated.
     * @param itemId The ID of the item to update.
     * @param quantityChange The amount to add or subtract.
     * @param operation "add" or "subtract".
     */
    private void updateInventoryQuantity(int itemId, int quantityChange, String operation) {
        if (currentHospitalLocationId == -1) {
            tvStatus.setText("Error: Location not set. Cannot update inventory.");
            Toast.makeText(this, "A hospital location must be selected to update.", Toast.LENGTH_LONG).show();
            return;
        }

        tvStatus.setText("Updating item " + etItemName.getText().toString() + "...");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, INVENTORY_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.optString("message", "No message provided.");

                            if (status.equals("success")) {
                                tvStatus.setText("Inventory updated successfully! " + message);
                                Toast.makeText(DatabaseActivity.this, "Update OK: " + message, Toast.LENGTH_SHORT).show();
                                etItemName.setText("");
                                etQuantityChange.setText("");
                                selectedItemId = -1; // Reset selected item ID
                                fetchInventoryList(); // Re-fetch the entire list to show updated quantities and re-apply filter
                            } else if (status.equals("fail")) {
                                tvStatus.setText("Update failed: " + message);
                                Toast.makeText(DatabaseActivity.this, "Update failed: " + message, Toast.LENGTH_LONG).show();
                            } else if (status.equals("error")) {
                                tvStatus.setText("Server error: " + message);
                                Toast.makeText(DatabaseActivity.this, "Server error: " + message, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            tvStatus.setText("Error parsing server response.");
                            Toast.makeText(DatabaseActivity.this, "Response parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("DatabaseActivity", "JSON parsing error for update response: " + e.getMessage(), e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvStatus.setText("Network error during update.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(DatabaseActivity.this, "Network Error updating inventory: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("DatabaseActivity", "Volley error updating inventory: " + errorMessage, error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("item_id", String.valueOf(itemId));
                params.put("quantity_change", String.valueOf(quantityChange));
                params.put("operation", operation);
                params.put("location_id", String.valueOf(currentHospitalLocationId)); // Pass location_id
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
}






