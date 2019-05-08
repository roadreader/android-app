package app.roadreader;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;


public class SensorListener implements SensorEventListener {

    protected SensorManager sensorManager;
    protected Sensor accelerometer;
    protected Sensor gyroscope;
    protected float ax, ay, az, gx, gy, gz;
    FileWriter accel_writer, gyro_writer;

    public SensorListener(Context context) {

        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];
            Log.d("accelerometer", ax + " " + ay + " " + az + "\n");
            try {
                accel_writer.write(ax + " " + ay + " " + az + "\n");
            } catch(Exception e) {}
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
            Log.d("gyroscope", gx + " " + gy + " " + gz + "\n");
            try {
                gyro_writer.write(gx + " " + gy + " " + gz + "\n");
            } catch(Exception e) {}
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Safe not to implement
    }

    protected void resume() {
        try {
            //File accel = new File()
            accel_writer = new FileWriter("accel.txt", true);
            gyro_writer = new FileWriter("gyro.txt", true);
        } catch(Exception e) {}

    }

    protected void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void stop() {
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
    }

    protected void pause() {
        if(accel_writer != null) {
            try {
                accel_writer.close();
            } catch (Exception e) {}
        }

        if (gyro_writer != null) {
            try {
                gyro_writer.close();
            } catch (Exception e) {}
        }
    }



}
