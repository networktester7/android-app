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
    private ArrayAdapter<String> inventoryAdapter;
    private ArrayList<String> inventoryList;

    private RequestQueue requestQueue;

    // IMPORTANT: Replace with the actual URL of your PHP scripts
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
        inventoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                inventoryList);
        lvInventoryItems.setAdapter(inventoryAdapter);

        // Set item click listener for the ListView
        lvInventoryItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedItemDisplay = inventoryList.get(position);
                // Extract only the item name (before the parenthesis for quantity)
                String selectedItemName = selectedItemDisplay.split(" \\(")[0];
                etItemName.setText(selectedItemName); // Populate the EditText
                Toast.makeText(DatabaseActivity.this, "Selected: " + selectedItemName, Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch inventory list when the activity is created
        fetchInventoryList();

        // Set OnClickListener for the "Add Quantity" button
        btnAddQuantity.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            String quantityChangeStr = etQuantityChange.getText().toString().trim();

            if (itemName.isEmpty() || quantityChangeStr.isEmpty()) {
                Toast.makeText(DatabaseActivity.this, "Please select an item and enter quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantityChange = Integer.parseInt(quantityChangeStr);
                updateInventoryQuantity(itemName, quantityChange, "add");
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set OnClickListener for the "Subtract Quantity" button
        btnSubtractQuantity.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            String quantityChangeStr = etQuantityChange.getText().toString().trim();

            if (itemName.isEmpty() || quantityChangeStr.isEmpty()) {
                Toast.makeText(DatabaseActivity.this, "Please select an item and enter quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantityChange = Integer.parseInt(quantityChangeStr);
                updateInventoryQuantity(itemName, quantityChange, "subtract");
            } catch (NumberFormatException e) {
                Toast.makeText(DatabaseActivity.this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // It's good practice to refresh the list when returning to the activity
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
                                JSONArray jsonArray = jsonResponse.getJSONArray("data"); // Get the data array
                                inventoryList.clear(); // Clear existing data
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    String itemName = item.getString("item_name");
                                    int quantity = item.getInt("quantity");
                                    inventoryList.add(itemName + " (" + quantity + ")"); // Add formatted string
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
     * Sends a request to the server to update the quantity of an inventory item.
     * @param itemName The name of the item to update.
     * @param quantityChange The amount by which to change the quantity.
     * @param operation The operation to perform: "add" or "subtract".
     */
    private void updateInventoryQuantity(String itemName, int quantityChange, String operation) {
        tvStatus.setText("Updating...");

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
                params.put("item_name", itemName);
                params.put("quantity_change", String.valueOf(quantityChange));
                params.put("operation", operation); // "add" or "subtract"
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
}