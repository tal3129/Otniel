package com.otniel;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


/**
 * Created by amxha on 21/07/2016.
 */
public class AskForPermissions {

    public static final int contactsIndx =  0;
    public static final String contacts =  Manifest.permission.WRITE_CONTACTS;

    public static boolean checkPermission(Activity activity, String permission){
        int permissionCheck = ContextCompat.checkSelfPermission(activity,permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity, String permission, int index) {

        ActivityCompat.requestPermissions(activity,
                new String[]{permission},
                index);
    }

}
