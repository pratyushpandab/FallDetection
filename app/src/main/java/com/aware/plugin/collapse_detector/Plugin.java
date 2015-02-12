package com.aware.plugin.collapse_detector;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;


public class Plugin extends Aware_Plugin implements SensorEventListener {

    private static SensorManager mSensorManager = null;
    private static Sensor mAccelerometer = null;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "collapse_detector";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        if( DEBUG ) Log.d(TAG, "collapse_detector plugin running");

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if( DEBUG ) Log.d(TAG, "collapse_detector plugin terminated");
        mSensorManager.unregisterListener(this, mAccelerometer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        /*System.out.println("x: "+ x);
        System.out.println("y: "+ y);
        System.out.println("z: "+ z);
        */
        double vector_sum = Math.sqrt(x*x + y*y + z*z );
        System.out.println("Vector sum: "+ vector_sum);
        //the acceleration is around 0.3 as its lowest when in free fall. This may depend on the phone used.
        if (vector_sum < 0.3){
            Toast.makeText(getApplicationContext(),"I fell down!", Toast.LENGTH_SHORT).show();
        }

    }

}
