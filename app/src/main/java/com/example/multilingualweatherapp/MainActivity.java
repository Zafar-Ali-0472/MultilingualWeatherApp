package com.example.multilingualweatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText editTextCity;
    Button buttonFetchWeather;
    TextView textViewTemperature, textViewMinTemp, textViewMaxTemp, textViewPressure,
            textViewHumidity, textViewVisibility, editTextLocation, textViewWindSpeed;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextCity = findViewById(R.id.editTextText);
        buttonFetchWeather = findViewById(R.id.button);
        textViewTemperature = findViewById(R.id.textView);
        textViewMinTemp = findViewById(R.id.textView2);
        textViewMaxTemp = findViewById(R.id.textView3);
        textViewPressure = findViewById(R.id.textView4);
        textViewHumidity = findViewById(R.id.textView5);
        textViewVisibility = findViewById(R.id.textView6);
        textViewWindSpeed = findViewById(R.id.textView7);
        editTextLocation = findViewById(R.id.editTextText2); // Add this line

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        buttonFetchWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextCity.getText().toString().isEmpty()) {
                    // City name is empty
                    Toast.makeText(MainActivity.this, R.string.enter_city_name, Toast.LENGTH_SHORT).show();
                } else {
                    // Fetch weather based on city name
                    new FetchWeatherTask().execute(editTextCity.getText().toString());
                }
            }
        });

        ImageButton imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fetch weather based on current location
                getCurrentLocation();
            }
        });
    }

    private void getCurrentLocation() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
                new FetchWeatherTask().execute(String.valueOf(latitude), String.valueOf(longitude));
                locationManager.removeUpdates(locationListener);
                // Update the EditText with the city name using reverse geocoding
                updateCityNameFromCoordinates(latitude, longitude);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        // Check if GPS is enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(MainActivity.this, "Please enable GPS to fetch location", Toast.LENGTH_SHORT).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
    private void updateCityNameFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                String cityName = addresses.get(0).getLocality();
                if (cityName != null) {
                    editTextLocation.setText(cityName);
                } else {
                    editTextLocation.setText("City not found");
                }
            } else {
                editTextLocation.setText("City not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            editTextLocation.setText("City not found");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class FetchWeatherTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            double latitude, longitude;
            String apiKey = "e37075dfc08a0a10a39d8f8ff66b4c47";
            String apiUrl;

            if (params.length == 2) {
                // Fetch weather based on coordinates
                latitude = Double.parseDouble(params[0]);
                longitude = Double.parseDouble(params[1]);
                apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey;
            } else {
                // Fetch weather based on city name
                String cityName = params[0];
                apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + apiKey;
            }

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                return new JSONObject(response.toString());

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            super.onPostExecute(response);
            if (response != null) {
                try {
                    // Handle JSON parsing and update UI
                    double temperature = response.getJSONObject("main").getDouble("temp") - 273.15; // Convert from Kelvin to Celsius
                    double minTemp = response.getJSONObject("main").getDouble("temp_min") - 273.15;
                    double maxTemp = response.getJSONObject("main").getDouble("temp_max") - 273.15;
                    double pressure = response.getJSONObject("main").getDouble("pressure");
                    double humidity = response.getJSONObject("main").getDouble("humidity");
                    double visibility = response.getDouble("visibility");
                    double windSpeed = response.getJSONObject("wind").getDouble("speed");

                    textViewTemperature.setText(R.string.Temperature+": " + String.format("%.2f", temperature) + "°C");
                    textViewMinTemp.setText(R.string.MinTemperature+": " + String.format("%.2f", minTemp) + "°C");
                    textViewMaxTemp.setText(R.string.MaxTemerature+": " + String.format("%.2f", maxTemp) + "°C");
                    textViewPressure.setText(R.string.Pressure+": " + pressure);
                    textViewHumidity.setText(R.string.Humidity+": " + humidity);
                    textViewVisibility.setText(R.string.Visibility+": " + visibility);
                    textViewWindSpeed.setText(R.string.WindSpeed+": " + windSpeed);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed to parse weather data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}