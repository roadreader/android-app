package app.roadreader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends AppCompatActivity {

    protected Camera mCamera;
    protected Camera.CameraInfo mCameraInfo;
    protected CameraPreview mPreview;
    protected FrameLayout preview;
    protected ImageView redDot;
    protected Animation redDotAnimation;
    protected Button record;
    protected Picturetask takePictures;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    protected int btnState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        redDot = (ImageView)findViewById(R.id.redDot);
        redDot.setVisibility(View.INVISIBLE);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }else{
                if(checkCameraHardware(this)){
                    mCamera = getCameraInstance();
                }

                findBackFacingCamera();
                mCamera.setDisplayOrientation(getCorrectCameraOrientation(mCameraInfo,mCamera));

                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(getCorrectCameraOrientation(mCameraInfo, mCamera));
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(parameters);

                mPreview = new CameraPreview(this, mCamera);
                preview = (FrameLayout) findViewById(R.id.camera_preview);
                preview.addView(mPreview);

                //milliseconds -- 5fps
                takePictures = new Picturetask(200);

                record = (Button)findViewById(R.id.button_capture);
                record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(btnState == 0) {
                            startRedDotAnimation();
                            btnState = 1;
                            record.setText("Stop");
                            takePictures.execute();
                        }
                        else if(btnState == 1){
                            takePictures.cancelPicture();
                            cancelRedDotAnimation();
                            btnState = 0;
                            record.setText("Record");
                        }
                    }
                });
            }
        }
    }

    private void startRedDotAnimation() {
        redDot.setVisibility(View.VISIBLE);
        redDotAnimation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        redDotAnimation.setDuration(1000); // duration - half a second
        redDotAnimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        redDotAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        redDotAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        redDot.startAnimation(redDotAnimation);
    }

    private void cancelRedDotAnimation() {
        redDotAnimation.cancel();
        redDot.setVisibility(View.INVISIBLE);
    }
    private void writeData(int flag){

        final File directory = this.getFilesDir();

        if(flag == 1){

            String filename = "myfile";
            String fileContents = "Hello world!";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(fileContents.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else{
            this.deleteFile("myfile");
        }

        Toast toast = Toast.makeText(getApplicationContext(),directory.getAbsolutePath(),Toast.LENGTH_SHORT);
        toast.show();

        File [] files = directory.listFiles();
        for(int i = 0; i < files.length; i++){
            String name = files[i].getName();

        }


    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream outputStream;
            String filename = "testimage.jpg";

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(data);
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
            ImageView iv = new ImageView(MainActivity.this);
            iv.setImageBitmap(mutableBitmap);
            preview.addView(iv);
            */

        }
    };


    private int findBackFacingCamera()
    {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++)
        {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            mCameraInfo = info;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                Log.d(TAG, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    public int getCorrectCameraOrientation(Camera.CameraInfo info, Camera camera) {

        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;
        System.out.println(info);
        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }else{
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    public LatLng getRandomLocation() {
        LatLng point = new LatLng(32.715736,-117.161087);
        int radius = 310;

        List<LatLng> randomPoints = new ArrayList<>();
        List<Float> randomDistances = new ArrayList<>();
        Location myLocation = new Location("");
        myLocation.setLatitude(point.latitude);
        myLocation.setLongitude(point.longitude);

        //This is to generate 10 random points
        for(int i = 0; i<10; i++) {
            double x0 = point.latitude;
            double y0 = point.longitude;

            Random random = new Random();

            // Convert radius from meters to degrees
            double radiusInDegrees = radius / 111000f;

            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Adjust the x-coordinate for the shrinking of the east-west distances
            double new_x = x / Math.cos(y0);

            double foundLatitude = new_x + x0;
            double foundLongitude = y + y0;
            LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
            randomPoints.add(randomLatLng);
            Location l1 = new Location("");
            l1.setLatitude(randomLatLng.latitude);
            l1.setLongitude(randomLatLng.longitude);
            randomDistances.add(l1.distanceTo(myLocation));
        }
        //Get nearest point to the centre
        int indexOfNearestPointToCentre = randomDistances.indexOf(Collections.min(randomDistances));
        return randomPoints.get(indexOfNearestPointToCentre);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Camera Access Must be Granted");
                    builder.setMessage("App depends on camera");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs camera access");
                builder.setMessage("Please grant camera access to this app");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CAMERA_PERMISSION);
                    }
                });
                AlertDialog dialog = builder.show();
            }else{

            }
        }
    }

    private class Picturetask extends AsyncTask<Void,Void,Void> {

        private long timeout;
        private volatile boolean wait = true;

        public Picturetask(long timeout){
            this.timeout = timeout;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            takePicture();
            return null;
        }

        private void takePicture(){

            while(wait){
                mCamera.takePicture(null, null, mPicture);
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void cancelPicture(){
            this.wait = false;
        }
    }


}


