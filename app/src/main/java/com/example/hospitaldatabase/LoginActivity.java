package com.example.hospitaldatabase; // IMPORTANT: Make sure this matches your app's actual package name

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // For logging errors, useful for debugging
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvLoginStatus; // This TextView displays login messages to the user

    // Volley for Network Requests
    private RequestQueue requestQueue;

    // IMPORTANT: Update this URL to your actual login.php script on your server!
    private static final String LOGIN_URL = "http://172.206.33.45/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Link to your activity_login.xml layout file

        // 1. Initialize UI Components
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvLoginStatus = findViewById(R.id.tvLoginStatus); // Initialize the TextView for status messages

        // 2. Initialize Volley Request Queue
        requestQueue = Volley.newRequestQueue(this);

        // 3. Set OnClickListener for the Login Button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(); // Call the login method when the button is clicked
            }
        });
    }

    /**
     * Handles the user login process:
     * 1. Validates input fields.
     * 2. Sends a POST request to the login PHP script.
     * 3. Parses the server response and handles success/failure.
     * 4. Redirects to LocationSelectionActivity on successful login.
     */
    private void loginUser() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        // Basic input validation
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
            tvLoginStatus.setText("Username and password required.");
            return; // Stop execution if validation fails
        }

        tvLoginStatus.setText("Logging in..."); // Provide immediate feedback to the user

        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGIN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.optString("message", "No message provided."); // Get optional message

                            if (status.equals("success")) {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                tvLoginStatus.setText("Login successful!");

                                // *** IMPORTANT CHANGE HERE: Redirect to LocationSelectionActivity ***
                                Intent intent = new Intent(LoginActivity.this, LocationSelectionActivity.class);
                                startActivity(intent);
                                finish(); // Finish LoginActivity so the user cannot go back to it with the back button

                            } else {
                                tvLoginStatus.setText("Login failed: " + message);
                                Toast.makeText(LoginActivity.this, "Login failed: " + message, Toast.LENGTH_LONG).show();
                                Log.w("LoginActivity", "Login failed: " + message + " Response: " + response); // Log warning
                            }
                        } catch (JSONException e) {
                            // Error parsing JSON response from the server
                            tvLoginStatus.setText("Error parsing server response.");
                            Toast.makeText(LoginActivity.this, "Error parsing login response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("LoginActivity", "JSON parsing error: " + e.getMessage(), e); // Log the exception
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Network error (e.g., no internet, server unreachable)
                        tvLoginStatus.setText("Network error during login.");
                        String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown network error.";
                        Toast.makeText(LoginActivity.this, "Network error: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("LoginActivity", "Volley error during login: " + errorMessage, error); // Log the Volley error
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Parameters to be sent to the PHP script via POST
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        // Add the request to the RequestQueue to execute it
        requestQueue.add(stringRequest);
    }
}






