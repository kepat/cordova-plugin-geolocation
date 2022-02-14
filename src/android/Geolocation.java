/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at
         http://www.apache.org/licenses/LICENSE-2.0
       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */
package org.apache.cordova.geolocation;

import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.Build;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.callback.Callback;

public class Geolocation extends CordovaPlugin {

	public static final Integer REQUEST_CODE_GET_LOCATION = 1;

    String TAG = "GeolocationPlugin";
    CallbackContext context;

    String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    private boolean mEnableHighAccuracy; // Enable High Accuracy

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "We are entering execute");
        context = callbackContext;
        if(action.equals("getPermission")) {
            if(hasPermisssion()) {
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                context.sendPluginResult(result);
                return true;
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
            return true;
        } else if(action.equals("getLocation")) {
            // Store the passed parameter
    	    this.mEnableHighAccuracy = args.getBoolean(0);

            if(hasPermisssion()) {
                getLocation();
                return true;
            } else {
                PermissionHelper.requestPermissions(this, REQUEST_CODE_GET_LOCATION, permissions);
            }
            return true;
        }
        return false;
    }


    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if(context != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    LOG.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    context.sendPluginResult(result);
                    return;
                }

            }

            // Check if it is from get location action
            if (requestCode == REQUEST_CODE_GET_LOCATION) {
                getLocation();
                return;
            }

            result = new PluginResult(PluginResult.Status.OK);
            context.sendPluginResult(result);
        }
    }

    public boolean hasPermisssion() {
        for(String p : permissions) {
            if(!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    /*
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     */

    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }

    /**
     * Get the current location
     */
    public void getLocation() {
        // Prepare the fused location provider from google
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.cordova.getActivity());

        // Typically use one cancellation source per lifecycle
        CancellationTokenSource cancellationSource = new CancellationTokenSource();

        // Prepare the accuracy settings
        Integer priority = (this.mEnableHighAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Get the current location
        fusedLocationClient.getCurrentLocation(priority, cancellationSource.getToken())
            .addOnSuccessListener(this.cordova.getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Prepare the result
                    PluginResult result;

                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Prepare the result
                        JSONObject resultLocation = new JSONObject();
                        appendJSONObjectData(resultLocation, "latitude", Double.toString(location.getLatitude()));
                        appendJSONObjectData(resultLocation, "longitude", Double.toString(location.getLongitude()));
                        appendJSONObjectData(resultLocation, "altitude", Double.toString(location.getAltitude()));
                        appendJSONObjectData(resultLocation, "accuracy", Float.toString(location.getAccuracy()));
                        
                        // Send the result back
                        result = new PluginResult(PluginResult.Status.OK, resultLocation);
                        context.sendPluginResult(result);
                    } else {
                        result = new PluginResult(PluginResult.Status.ERROR);
                        context.sendPluginResult(result);
                    }
                }
            });
    }

    /**
     * Add the data to a json object
     */
    private JSONObject appendJSONObjectData(JSONObject object, String name, String data) {
		try {
			object.put(name, data);
		} catch (JSONException e) {
            LOG.d(TAG, e.getMessage());
        } catch (Exception e) {
            LOG.e(TAG, e.getMessage());
        }

		return object;
	}

}
