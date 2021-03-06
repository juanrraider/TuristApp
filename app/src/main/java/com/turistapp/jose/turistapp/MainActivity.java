package com.turistapp.jose.turistapp;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.turistapp.jose.turistapp.Async.UrlUtils;
import com.turistapp.jose.turistapp.Fragments.Chatbot;
import com.turistapp.jose.turistapp.Fragments.Places;
import com.turistapp.jose.turistapp.Fragments.Route;
import com.turistapp.jose.turistapp.Fragments.SavedRoutes;
import com.turistapp.jose.turistapp.Model.Place;
import com.turistapp.jose.turistapp.Model.RouteSegment;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity
implements Chatbot.OnFragmentInteractionListener, Route.OnFragmentInteractionListener{

    private static final String TAG = "MainActivity: ";

    private static final String GETPLACESBYINDEXURL = "https://us-central1-trip-planner-pucp.cloudfunctions.net/getPlacesByIndex/";

    private String directionsResponse = "";

    final Fragment chatbotFragment = new Chatbot();
    final Fragment placesFragment = new Places();
    final Fragment routeFragment = new Route();
    final Fragment savedFragment = new SavedRoutes();
    final FragmentManager fm = getSupportFragmentManager();
    Fragment active = chatbotFragment;

    public String datap = "";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case R.id.navigation_chat:
                    fm.beginTransaction().hide(active).show(chatbotFragment).commit();
                    active = chatbotFragment;
                    return true;

                case R.id.navigation_places:
                    fm.beginTransaction().hide(active).show(placesFragment).commit();
                    active = placesFragment;
                    return true;

                case R.id.navigation_routes:
                    fm.beginTransaction().hide(active).show(routeFragment).commit();
                    active = routeFragment;
                    return true;

                case R.id.navigation_saved:
                    fm.beginTransaction().hide(active).show(savedFragment).commit();
                    active = routeFragment;
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Check if device is online and add saved routes fragment

        if(isNetworkAvailable()){

            navigation.getMenu()
                    .findItem(R.id.navigation_saved)
                    .setVisible(false);

            navigation.invalidate();

            fm.beginTransaction().add(R.id.mainfragment, routeFragment, "3").hide(routeFragment).commit();
            fm.beginTransaction().add(R.id.mainfragment, placesFragment, "2").hide(placesFragment).commit();
            fm.beginTransaction().add(R.id.mainfragment, chatbotFragment, "1").commit();

        } else {

            navigation.getMenu()
                    .findItem(R.id.navigation_places)
                    .setVisible(false);

            navigation.invalidate();

            navigation.getMenu()
                    .findItem(R.id.navigation_routes)
                    .setVisible(false);

            navigation.invalidate();

            fm.beginTransaction().add(R.id.mainfragment, savedFragment, "2").hide(savedFragment).commit();
            fm.beginTransaction().add(R.id.mainfragment, chatbotFragment, "1").commit();

        }



    }

    public String getDirectionsResponse(){
        return this.directionsResponse;
    }

    public void setDirectionsResponse(String r){
        this.directionsResponse = r;
    }

    public void routesCallback(ResponseBody responseBody, Place origin, ArrayList<Place> waypoints){


        JSONArray legs = null;
        JSONArray waypoint_order = null;

        try {
            JSONObject routes = new JSONObject(responseBody.string());
            JSONObject route =  routes.getJSONArray("routes").getJSONObject(0);

            legs = route.getJSONArray("legs");
            waypoint_order = route.getJSONArray("waypoint_order");

        } catch (JSONException | IOException e) {
            Log.e(TAG, e.getMessage());
        }

        //solo necesito legs y waypoint order

        if(legs.length() <= 0 || legs == null){
            Toast.makeText(this, "No hay ruta disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<RouteSegment> routeSegments = new ArrayList<>();

        try {



            for (int i = 0; i < legs.length() - 1; i++) {
                JSONObject aux = legs.getJSONObject(i);
                JSONArray steps = aux.getJSONArray("steps");
                JSONObject distance = aux.getJSONObject("distance");
                JSONObject duration = aux.getJSONObject("duration");

                ArrayList<String> segment_steps = new ArrayList<>();
                int segment_distance = distance.getInt("value");
                int segment_duration = duration.getInt("value");

                for(int j = 0; j < steps.length(); j++){
                    JSONObject step = steps.getJSONObject(j);
                    JSONObject polyline = step.getJSONObject("polyline");
                    String points = polyline.getString("points");
                    segment_steps.add(points);
                }

                routeSegments.add(new RouteSegment(segment_steps,segment_distance,segment_duration));

            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        fm.beginTransaction().hide(active).show(routeFragment).commit();
        active = routeFragment;

        int[] numbers = new int[waypoint_order.length()];

        for (int i = 0; i < waypoint_order.length(); ++i) {
            numbers[i] = waypoint_order.optInt(i);
        }


        ((Activity) this).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Places) placesFragment).switchcontrols(0,1);
                ((Route) routeFragment).processRoute(routeSegments, origin, waypoints, numbers);
            }
        });




    }

    public void placesCallback(List<Integer> places) {
        String placesstring = "";

        for(int i : places){
            placesstring = placesstring + "," + i;
        }

        placesstring = placesstring.substring(1);

        String requesturl = GETPLACESBYINDEXURL + placesstring;

        Log.i(TAG, requesturl);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(requesturl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ArrayList<Place> plist = new ArrayList<>();

                try {

                    //Log.i(TAG, response.body().string());

                    String rr = response.body().string();
                    JSONArray pjson = new JSONArray(rr);

                    for(int i = 0; i<pjson.length(); i++){
                        JSONObject o = pjson.getJSONObject(i);

                        String name = o.getString("name");
                        String desc = o.getString("description");
                        int id = o.getInt("id");
                        String image = o.getString("image");
                        JSONArray hours = o.getJSONArray("hours");
                        int h1 = hours.getInt(0);
                        int h2 = hours.getInt(1);
                        JSONObject location = o.getJSONObject("location");
                        double lat = location.getDouble("_latitude");
                        double lng = location.getDouble("_longitude");

                        LatLng loc = new LatLng(lat,lng);

                        Place p = new Place(id, name, loc, image, h1, h2, desc);

                        plist.add(p);

                    }

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((Places) placesFragment).setAdapter(plist);
                        }
                    });

                } catch(JSONException e) {
                    Log.e(TAG, e.getMessage());
                }




            }
        });

    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onFragmentInteraction(Uri uri){

    }

}
