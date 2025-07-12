package com.example.hospitaldatabase;

import android.os.Bundle;
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
import java.util.Map;

public class DatabaseActivity extends AppCompatActivity {

    private EditText etItemName, etQuantityChange;
    private Button btnAddQuantity, btnSubtractQuantity;
    private TextView tvStatus;
    private ListView lvInventoryItems;
    private ArrayAdapter<InventoryItem> inventoryAdapter; // Adapter now uses InventoryItem
    private ArrayList<InventoryItem> inventoryList; // List now holds InventoryItem objects

    private RequestQueue requestQueue;

    // To hold the ID of the currently selected item for updates
    private int selectedItemId = -1; // Initialize with an invalid ID

    private static final String INVENTORY_UPDATE_URL = "http://172.206.33.45/update_inventory.php";
    private static final String INVENTORY_FETCH_URL = "http://172.206.33.45/get_inventory.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        // Initialize UI components
        etItemName = findViewById(R.id.etItemName);
        etQuantityChange = findViewById(R.id.etQuantityChange);
        btnAddQuantity = findViewById(R.id.btnAddQuantity);
        btnSubtractQuantity = findViewById(R.id.btnSubtractQuantity);
        tvStatus = findViewById(R.id.tvStatus);
        lvInventoryItems = findViewById(R.id.lvInventoryItems);

        // Initialize data structures for ListView
        inventoryList = new ArrayList<>();
        // ArrayAdapter will use InventoryItem and call its toString() method
        inventoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                inventoryList);
        lvInventoryItems.setAdapter(inventoryAdapter);

        // Set item click listener for the ListView
        lvInventoryItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                InventoryItem selectedItem = inventoryList.get(position);
                selectedItemId = selectedItem.getId(); // Store the selected item's ID
                etItemName.setText(selectedItem.getName()); // Populate the EditText with the name
                Toast.makeText(DatabaseActivity.this, "Selected: " + selectedItem.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch inventory list when the activity is created
        fetchInventoryList();

        // Set OnClickListener for the "Add Quantity" button
        btnAddQuantity.setOnClickListener(v -> {
            // Check if an item is selected
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
                updateInventoryQuantity(selectedItemId, quantityChange, "add"); // Pass item ID
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set OnClickListener for the "Subtract Quantity" button
        btnSubtractQuantity.setOnClickListener(v -> {
            // Check if an item is selected
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
                updateInventoryQuantity(selectedItemId, quantityChange, "subtract"); // Pass item ID
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchInventoryList(); // Refresh the list when returning to the activity
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
                                inventoryList.clear(); // Clear existing data
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    int itemId = item.getInt("item_id"); // Get item_id
                                    String itemName = item.getString("item_name");
                                    int quantity = item.getInt("quantity");
                                    inventoryList.add(new InventoryItem(itemId, itemName, quantity)); // Add InventoryItem object
                                }
                                inventoryAdapter.notifyDataSetChanged(); // Update ListView
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
     * Sends a request to the server to update the quantity of an inventory item by ID.
     * @param itemId The ID of the item to update.
     * @param quantityChange The amount by which to change the quantity.
     * @param operation The operation to perform: "add" or "subtract".
     */
    private void updateInventoryQuantity(int itemId, int quantityChange, String operation) { // Now accepts itemId
        tvStatus.setText("Updating...");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, INVENTORY_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.optString("message", "No message provided.");
                            // Optional: get updated item details from response
                            String updatedItemName = jsonResponse.optString("item_name", "");
                            int newQuantity = jsonResponse.optInt("new_quantity", -1);

                            if (status.equals("success")) {
                                tvStatus.setText("Inventory updated successfully! " + message);
                                Toast.makeText(DatabaseActivity.this, "Update OK: " + message, Toast.LENGTH_SHORT).show();
                                etItemName.setText(""); // Clear selected item name
                                etQuantityChange.setText("");
                                selectedItemId = -1; // Reset selected item ID
                                fetchInventoryList(); // Refresh the list to show updated quantities

                            } else if (status.equals("fail")) { // For item not found or no rows affected
                                tvStatus.setText("Update failed: " + message);
                                Toast.makeText(DatabaseActivity.this, "Update failed: " + message, Toast.LENGTH_LONG).show();
                            } else if (status.equals("error")) { // For server-side internal errors
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
                params.put("item_id", String.valueOf(itemId)); // Send item_id instead of item_name
                // Optional: You could still send item_name for server-side logging or more descriptive responses
                // params.put("item_name_client", etItemName.getText().toString().trim());
                params.put("quantity_change", String.valueOf(quantityChange));
                params.put("operation", operation);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
}