package com.example.hospitaldatabase;

import android.os.Bundle;
import android.text.Editable; // NEW
import android.text.TextWatcher; // NEW
import android.util.Log;
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
import java.util.Locale; // NEW for toLowerCase()
import java.util.Map;

public class DatabaseActivity extends AppCompatActivity {

    private EditText etItemName, etQuantityChange, etSearch; // Added etSearch
    private Button btnAddQuantity, btnSubtractQuantity;
    private TextView tvStatus;
    private ListView lvInventoryItems;
    private ArrayAdapter<InventoryItem> inventoryAdapter;
    private ArrayList<InventoryItem> inventoryList; // This will hold the CURRENTLY displayed (filtered) list
    private ArrayList<InventoryItem> fullInventoryList; // NEW: Holds the full, unfiltered list

    private RequestQueue requestQueue;

    private int selectedItemId = -1;

    private static final String INVENTORY_UPDATE_URL = "http://172.206.33.45/update_inventory.php";
    private static final String INVENTORY_FETCH_URL = "http://172.206.33.45/get_inventory.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        // Initialize UI components
        etItemName = findViewById(R.id.etItemName);
        etQuantityChange = findViewById(R.id.etQuantityChange);
        etSearch = findViewById(R.id.etSearch); // Initialize etSearch
        btnAddQuantity = findViewById(R.id.btnAddQuantity);
        btnSubtractQuantity = findViewById(R.id.btnSubtractQuantity);
        tvStatus = findViewById(R.id.tvStatus);
        lvInventoryItems = findViewById(R.id.lvInventoryItems);

        // Initialize data structures for ListView
        inventoryList = new ArrayList<>();
        fullInventoryList = new ArrayList<>(); // Initialize full list
        inventoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                inventoryList);
        lvInventoryItems.setAdapter(inventoryAdapter);

        // Set item click listener for the ListView
        lvInventoryItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                InventoryItem selectedItem = inventoryList.get(position);
                selectedItemId = selectedItem.getId();
                etItemName.setText(selectedItem.getName());
                Toast.makeText(DatabaseActivity.this, "Selected: " + selectedItem.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // NEW: Add TextWatcher for search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This is called as the user types
                filterInventoryList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });


        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch inventory list when the activity is created
        fetchInventoryList();

        // Set OnClickListener for the "Add Quantity" button
        btnAddQuantity.setOnClickListener(v -> {
            if (selectedItemId == -1) {
                Toast.makeText(DatabaseActivity.this, "Please select an item from the list.", Toast.LENGTH_SHORT).show();
                return;
            }

            String quantityChangeStr = etQuantityChange.getText().toString().trim();
            if (quantityChangeStr.isEmpty()) {
                Toast.makeText(DatabaseActivity.this, "Please enter quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantityChange = Integer.parseInt(quantityChangeStr);
                updateInventoryQuantity(selectedItemId, quantityChange, "add");
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set OnClickListener for the "Subtract Quantity" button
        btnSubtractQuantity.setOnClickListener(v -> {
            if (selectedItemId == -1) {
                Toast.makeText(DatabaseActivity.this, "Please select an item from the list.", Toast.LENGTH_SHORT).show();
                return;
            }

            String quantityChangeStr = etQuantityChange.getText().toString().trim();
            if (quantityChangeStr.isEmpty()) {
                Toast.makeText(DatabaseActivity.this, "Please enter quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantityChange = Integer.parseInt(quantityChangeStr);
                updateInventoryQuantity(selectedItemId, quantityChange, "subtract");
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear search and reset list whenever resuming activity
        etSearch.setText(""); // Clears the search text
        fetchInventoryList();
    }

    /**
     * Fetches the entire inventory list from the server and updates the ListView.
     */
    private void fetchInventoryList() {
        tvStatus.setText("Loading inventory...");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, INVENTORY_FETCH_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");

                            if (status.equals("success")) {
                                JSONArray jsonArray = jsonResponse.getJSONArray("data");
                                fullInventoryList.clear(); // Clear the full list first
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    int itemId = item.getInt("item_id");
                                    String itemName = item.getString("item_name");
                                    int quantity = item.getInt("quantity");
                                    fullInventoryList.add(new InventoryItem(itemId, itemName, quantity)); // Add to full list
                                }
                                // After fetching, apply any current search filter or display full list
                                filterInventoryList(etSearch.getText().toString());
                                tvStatus.setText("Inventory loaded.");
                            } else {
                                String message = jsonResponse.optString("message", "Unknown error loading inventory.");
                                tvStatus.setText("Failed to load inventory: " + message);
                                Toast.makeText(DatabaseActivity.this, "Error loading inventory: " + message, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            tvStatus.setText("Error parsing inventory data.");
                            Toast.makeText(DatabaseActivity.this, "Data parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("DatabaseActivity", "JSON parsing error (fetch): " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvStatus.setText("Failed to load inventory.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(DatabaseActivity.this, "Network error fetching inventory: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("DatabaseActivity", "Volley error fetching inventory: " + errorMessage);
                    }
                });

        requestQueue.add(stringRequest);
    }

    /**
     * Filters the inventory list based on the provided search query.
     * @param query The search string.
     */
    private void filterInventoryList(String query) {
        inventoryList.clear(); // Clear the currently displayed list
        if (query.isEmpty()) {
            inventoryList.addAll(fullInventoryList); // If query is empty, show all
        } else {
            query = query.toLowerCase(Locale.getDefault()); // Convert query to lowercase for case-insensitive search
            for (InventoryItem item : fullInventoryList) {
                // Check if item name contains the query (case-insensitive)
                if (item.getName().toLowerCase(Locale.getDefault()).contains(query)) {
                    inventoryList.add(item);
                }
            }
        }
        inventoryAdapter.notifyDataSetChanged(); // Notify adapter that data has changed
        // Reset selection when filter changes
        selectedItemId = -1;
        etItemName.setText("");
    }


    /**
     * Sends a request to the server to update the quantity of an inventory item by ID.
     * @param itemId The ID of the item to update.
     * @param quantityChange The amount by which to change the quantity.
     * @param operation The operation to perform: "add" or "subtract".
     */
    private void updateInventoryQuantity(int itemId, int quantityChange, String operation) {
        tvStatus.setText("Updating...");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, INVENTORY_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.optString("message", "No message provided.");
                            String updatedItemName = jsonResponse.optString("item_name", "");
                            int newQuantity = jsonResponse.optInt("new_quantity", -1);

                            if (status.equals("success")) {
                                tvStatus.setText("Inventory updated successfully! " + message);
                                Toast.makeText(DatabaseActivity.this, "Update OK: " + message, Toast.LENGTH_SHORT).show();
                                etItemName.setText("");
                                etQuantityChange.setText("");
                                selectedItemId = -1; // Reset selected item ID
                                fetchInventoryList(); // Re-fetch and re-filter the list to show updated quantities

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
                            Log.e("DatabaseActivity", "JSON parsing error for update response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvStatus.setText("Network error during update.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(DatabaseActivity.this, "Network Error updating inventory: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("DatabaseActivity", "Volley error updating inventory: " + errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("item_id", String.valueOf(itemId));
                params.put("quantity_change", String.valueOf(quantityChange));
                params.put("operation", operation);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
}