package org.swampsoft.gpstimesync;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class GPSTimeSyncService extends AccessibilityService {

    //AccessibilityServiceInfo info;

    boolean suEnabled = false;

    Process suProcess;
    //private DataOutputStream dataOutputStream;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    LocationManager locationManager;
    LocationProvider locationProvider;
    LocationListener locationListener;

    long GPStime;
    String gpsConverted;

    @Override
    public void onServiceConnected() {

        // todo change all of this info crap, or not? don't think it hurts to leave it out
        //info = new AccessibilityServiceInfo();

        // stuff from the Google tutorial:
        //info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED | AccessibilityEvent.TYPE_VIEW_FOCUSED;
        //info.packageNames = new String[]{"org.swampsoft.odroidc1spitest"};
        //info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        //info.notificationTimeout = 100;
        //this.setServiceInfo(info);

        // try to get root access
        startSu();

        // check for ROOT access, then start reading GPS time and change it if needed
        if (suEnabled){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                setupGPS();
                //setTime();
                System.out.println("** GPS Time Sync Service Started");
            } else {
                Toast.makeText(getApplicationContext(), "GPS Time Sync Service NOT Started\nCheck permission settings!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Root access not found!\n\nThis app cannot change time\nwithout root access, sorry.", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onRebind(Intent intent){
        // try to get root access
        startSu();

        // check for ROOT access, then start reading GPS time and change it if needed
        if (suEnabled){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                setupGPS();
                //setTime();
                System.out.println("** GPS Time Sync Service Started");
            } else {
                Toast.makeText(getApplicationContext(), "GPS Time Sync Service NOT Started\nCheck settings!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Root access not found!\n\nThis app cannot change time\nwithout root access, sorry.", Toast.LENGTH_LONG).show();
        }

        System.out.println("** GPS Time Sync Service Rebound");
    }

    @Override
    public boolean onUnbind(Intent intent){
        // stop gps updates loop
        locationManager.removeUpdates(locationListener);

        try {
            bufferedWriter.flush();
            bufferedReader.close();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        suProcess.destroy();

        System.out.println("** GPS Time Sync Service Unbound");

        return true;
    }

    @SuppressLint("MissingPermission")
    void setupGPS(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            // todo not enabled
            System.out.println("** GPS Time Sync Service NOT Started");
            Toast.makeText(getApplicationContext(), "GPS Time Sync Service NOT Started", Toast.LENGTH_LONG).show();
        } else {
            // GPS enabled, setup listener and setup update interval
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    GPStime = location.getTime();

                    // Android CMD Date Format change
                    if ((Integer.valueOf(android.os.Build.VERSION.SDK_INT)) < 23) {
                        gpsConverted = new java.text.SimpleDateFormat("yyyyMMdd.HHmmss").format(new java.util.Date(GPStime));
                    }
                    else {
                        gpsConverted = new java.text.SimpleDateFormat("MMddHHmmyyyy.ss").format(new java.util.Date(GPStime));
                    }

                    //Toast.makeText(getApplicationContext(), "GPS Time: " + GPStime + "\n\nConverted: " + gpsConverted, Toast.LENGTH_LONG).show();
                    System.out.println("** GPS Time: " + GPStime + " Converted: " + gpsConverted);

                    setTime();
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                    //
                }

                @Override
                public void onProviderEnabled(String s) {
                    //
                }

                @Override
                public void onProviderDisabled(String s) {
                    //
                }
            };

            int updateTime = 3600000;
            //int updateTime = 1000;
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, updateTime, 10000, locationListener);
            //System.out.println("GPS Time Sync Service Started");
            //Toast.makeText(getApplicationContext(), "GPS Time Sync Service Started", Toast.LENGTH_SHORT).show();



        }
    }

    private void startSu(){
        // Don't touch this! it starts SU and initializes the XPT2046
        try {
            suProcess = Runtime.getRuntime().exec("su");
            //dataOutputStream = new DataOutputStream(suProcess.getOutputStream());
            suEnabled = true;
            System.out.println("** GPS Time Sync: SU enabled");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(suProcess.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTime(){
        // todo set time here
        try {
            System.out.println("** GPS Time Sync Service Setting Time...");
            String cmd = "date -s " + gpsConverted + "\n";
            bufferedWriter.write(cmd); // YYYYMMDD.HHmmss
            bufferedWriter.flush();

            if (bufferedReader.ready()){
                String line = bufferedReader.readLine();
                if (line != null){
                    System.out.println(line);
                    Toast.makeText(getApplicationContext(), line, Toast.LENGTH_LONG).show();
                }
            }

            /*
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                Toast.makeText(getApplicationContext(), line, Toast.LENGTH_SHORT).show();
            }
            bufferedReader.close();
            */
            Toast.makeText(getApplicationContext(), "GPS Time Sync Service Time Set: " + gpsConverted, Toast.LENGTH_SHORT).show();
            System.out.println("** GPS Time Sync Service Time Set: " + gpsConverted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
