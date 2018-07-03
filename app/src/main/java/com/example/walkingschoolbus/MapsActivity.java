package com.example.walkingschoolbus;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.location.LocationProvider;

import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.walkingschoolbus.model.Group;
import com.example.walkingschoolbus.model.User;
import com.example.walkingschoolbus.proxy.ProxyBuilder;
import com.example.walkingschoolbus.proxy.WGServerProxy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import java.util.List;
import java.util.ListIterator;

import retrofit2.Call;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/**
 * Simple test app to show a Google Map.
 * - If using the emulator, Create an Emulator from the API 26 image.
 *   (API27's doesn't/didn't support maps; nor will 24 or before I believe).
 * - Accessing Google Maps requires an API key: You can request one for free (and should!)
 *   see /res/values/google_maps_api.xml
 * - More notes at the end of this file.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Boolean mLocationPermissionsGranted = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private static final float DEFAULT_ZOOM = 14f;
    WGServerProxy proxy;
    private User user = User.getInstance();
    private FusedLocationProviderClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        proxy = ProxyBuilder.getProxy(getString(R.string.api_key),null);
        getUserLocationPermission();

    }

    /**
     * This method will send a request to the server to get all the groups and then
     * the callback response will display all the group's locations and meeting places on the map.
     *
     * This method is different from displayUserGroups in that it will display all groups from the server
     * rather than just the groups that the user is a member of.
     *
     * Only call one of the two methods upon initiating the map!
     */
    private void displayAllGroups(){
        Call<List<Group>> groupListCaller = proxy.getGroups();
        ProxyBuilder.callProxy(MapsActivity.this, groupListCaller, returnedGroups -> response(returnedGroups));
    }
    /*
     * Server response is to return server groups and then display all of their current locations and
     * meeting locations on the map.
     */
    private void response(List<Group> returnedGroupList){

        for(int i = 0; i < returnedGroupList.size(); i++){
            Group group = returnedGroupList.get(i);
            LatLng groupMeetingLocation = new LatLng(group.getMeetingPlace().getLatitude(),group.getMeetingPlace().getLongitude());
            MarkerOptions groupMeetingLocationMarker = new MarkerOptions()
                    .position(groupMeetingLocation)
                    .title("Group: "+group.getName())
                    //Differentiate group meeting location markers from current location markers with a different marker opacity (alpha).
                    .alpha(2);
            mMap.addMarker(groupMeetingLocationMarker);

            LatLng groupCurrentLocation = new LatLng(group.getLocation().getLatitude(),group.getLocation().getLongitude());
            MarkerOptions groupLocationMarker = new MarkerOptions()
                    .position(groupMeetingLocation)
                    .title("Group: "+group.getName())
                    .alpha(3);
            mMap.addMarker(groupLocationMarker);
        }
    }

    /**
     * Retrieves the groups that the user is a member of and displays their meeting locations on the map as well as their current locations.
     */
    private void displayUserGroups(){
        List<Group> userGroupList = user.getMemberOfGroups();
        for(int i = 0; i < userGroupList.size(); i++){
            //Unsure of whether to use singleton here.
            Group group = userGroupList.get(i);
            LatLng groupMeetingLocation = new LatLng(group.getMeetingPlace().getLatitude(),group.getMeetingPlace().getLongitude());
            MarkerOptions groupMeetingLocationMarker = new MarkerOptions()
                    .position(groupMeetingLocation)
                    .title("Group: "+group.getName())
                    //Differentiate group meeting location markers from current location markers with a different marker opacity (alpha).
                    .alpha(2);
            mMap.addMarker(groupMeetingLocationMarker);

            LatLng groupCurrentLocation = new LatLng(group.getLocation().getLatitude(),group.getLocation().getLongitude());
            MarkerOptions groupLocationMarker = new MarkerOptions()
                    .position(groupMeetingLocation)
                    .title("Group: "+group.getName())
                    .alpha(3);
            mMap.addMarker(groupLocationMarker);
        }
    }


    private void initMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        displayAllGroups();

        //If user enables the app to access their location, get user location.
        if(mLocationPermissionsGranted){
            getUserLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }

        //Hardcoded destination for now.
        //Need to change destinationLatLng to get the walking destination of the user's group.
        //Also need to mark locations of other groups
        LatLng destinationLatLng = new LatLng(49.278059,-122.919926);
        MarkerOptions destinationMarker = new MarkerOptions()
                .position(destinationLatLng)
                .title("My Meeting Place");
        mMap.addMarker(destinationMarker);

    }

    /**
     * Retrieves the user's location and marks it on the map.
     * The map will automatically centre on to the user's location upon initiation of the map.
     * There is a button on the top right corner of the map that will centre on to the user's location if clicked.
     */
    private void getUserLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Location currentLocation = (Location) task.getResult();
                        moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),DEFAULT_ZOOM);
                    }
                }
            });
        }catch (SecurityException exception){
            Log.e("MapsActivity","Security Exception: "+exception.getMessage());
        }
    }

    /**
     * Centers the screen to the specified position.
     * @param latLng Position coordinates of the location
     * @param zoom Zooms on the location by a specified amount.
     */
    private void moveCamera(LatLng latLng,float zoom){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    /**
     * Requests permission from the user to allow location services for the map.
     */
    private void getUserLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};

        //If both ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions are granted,
        //device location permissions will be granted.
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionsGranted = true;
            initMap();
        }else{
        //Else, request the user to enable location services for the map.
        //LOCATION_PERMISSION_REQUEST_CODE will be passed to onRequestPermissionsResult method
        //to verify the results of user's selection.
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }


    }

    /**
     *
     * @param requestCode The code passed by getUserLocationMethod providing the results of whether user denied or allowed location services.
     * @param permissions The array of requested permissions.
     * @param grantResults Grant results for the requested permissions.  Either PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       mLocationPermissionsGranted = false;
       switch(requestCode){
           case LOCATION_PERMISSION_REQUEST_CODE:
               if(grantResults.length > 0){
                   for(int i = 0; i < grantResults.length; i++){
                       if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                           mLocationPermissionsGranted = false;
                           return;
                       }
                   }
                   mLocationPermissionsGranted = true;
               }
       }
    }


    public static Intent makeIntent(Context context){
        Intent intent = new Intent(context, MapsActivity.class);
        return intent;
    }
}

