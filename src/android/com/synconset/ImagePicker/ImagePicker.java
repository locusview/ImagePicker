/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext callbackContext;

    private Intent imagePickerIntent;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
            final Intent imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
            int max = 20;
            int desiredWidth = 0;
            int desiredHeight = 0;
            int quality = 100;
            int outputType = 0;
            if (params.has("maximumImagesCount")) {
                max = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                outputType = params.getInt("outputType");
            }

            imagePickerIntent.putExtra("MAX_IMAGES", max);
            imagePickerIntent.putExtra("WIDTH", desiredWidth);
            imagePickerIntent.putExtra("HEIGHT", desiredHeight);
            imagePickerIntent.putExtra("QUALITY", quality);
            imagePickerIntent.putExtra("OUTPUT_TYPE", outputType);

            if (hasReadPermission()) {
                cordova.startActivityForResult(this, imagePickerIntent, 0);
            } else {
                this.imagePickerIntent = imagePickerIntent;
                requestReadPermission();
            }
            return true;
        }
        return false;
    }


    private boolean hasReadPermission() {
        String[] permissions = getPermissionsPerApiLevel();
        for (String permission : permissions) {
            if (!cordova.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    private void requestReadPermission() {
        if (!hasReadPermission()) {
            cordova.requestPermissions(this, PERMISSION_REQUEST_CODE, getPermissionsPerApiLevel());
        } else {
            callbackContext.success();
        }
    }

    @SuppressLint("InlinedApi")
    private String[] getPermissionsPerApiLevel() {
        ArrayList<String> permissions = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            int sync = data.getIntExtra("bigdata:synccode", -1);
            final Bundle bigData = ResultIPC.get().getLargeData(sync);

            ArrayList<String> fileNames = bigData.getStringArrayList("MULTIPLEFILENAMES");

            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            callbackContext.error(error);

        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("No images selected");
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        boolean allGranted = true;
        for (int result : grantResults) {
          if (result != PackageManager.PERMISSION_GRANTED) {
            allGranted = false;
            break;
          }
        }
        if (allGranted) {
            if (imagePickerIntent != null) {
                cordova.startActivityForResult(this, imagePickerIntent, 0);
            } else {
                callbackContext.success();
            }
        } else {
          callbackContext.error("Permission denied");
        }
    }
}
