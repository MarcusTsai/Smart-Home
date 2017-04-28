package com.mobiot.cmu.smarthome.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.CountDownTimer;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.CameraSource.*;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.facetracker.CameraSourcePreview;
import com.mobiot.cmu.smarthome.facetracker.FaceTrackerFactory;
import com.mobiot.cmu.smarthome.facetracker.GraphicOverlay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


public class MultiTrackerActivity extends AppCompatActivity {
    private static final String TAG = "MultiTracker";
    private final double sec = 1.5;
    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private String instanceID = null;
    private String deviceID = null;
    public static int OffsetThread = 0;
    private Button backButton, confirmButton;
    private static TextView faceCount;
    public static List<Integer> idList;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference sensors;

    public static Handler handler;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_tracker);
            backButton = (Button) findViewById(R.id.cancelButton);
            confirmButton = (Button) findViewById(R.id.confirmButton);
            faceCount = (TextView) findViewById(R.id.faceCount);
            mPreview = (CameraSourcePreview) findViewById(R.id.preview);
            mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
            idList = new ArrayList<Integer>();
            System.out.println("Face Detection");

            // Check for the camera permission before accessing the camera.  If the
            // permission is not granted yet, request permission.
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
            } else {
                requestCameraPermission();
            }


            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    faceCount.setText(String.valueOf(idList.size()));
                    System.out.println("value:" + idList.size());
                }
            };

            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // close handler
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                    //deleteNodeInFirebase();

                    Intent intent = new Intent(MultiTrackerActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    photoshot();
                }

            });

//            CountDownTimer timer = new CountDownTimer(sec * 1000, 1000) {
//                public void onTick(long millisUntilFinished) {
//                    if(millisUntilFinished == 0) {
//                        takePicture();
//                    }
//                }
//                public void onFinish(){}
//            };

            faceCount.addTextChangedListener(new TextWatcher() {
                private Timer timer = new Timer();
                private final int DELAY = (int)(sec * 1000);

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(Integer.parseInt(faceCount.getText().toString()) > 0) {
                        timer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        photoshot();
                                    }
                                }, DELAY
                        );
                    }
                    //photoshot();
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void photoshot() {
        if(mCameraSource != null) {
            mCameraSource.takePicture(null, new PictureCallback() {
                private File imageFile = null;

                @Override
                public void onPictureTaken(byte[] bytes) {

                    try {
                        File imageFile = null;
                        Bitmap loadedImage = null;
                        Bitmap rotatedBitmap = null;
                        loadedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        Matrix rotateMatrix = new Matrix();
                        rotateMatrix.postRotate(-90);
                        //rotateMatrix.postRotate(ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
                        rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                                loadedImage.getWidth(), loadedImage.getHeight(), rotateMatrix, false);

                        File dir = initFilePath();

                        java.util.Date date = new java.util.Date();
                        imageFile = File.createTempFile("image", ".jpg", dir);

                        ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                        // save image into gallery
                        rotatedBitmap.compress(CompressFormat.JPEG, 100, ostream);

                        FileOutputStream fout = new FileOutputStream(imageFile);
                        fout.write(ostream.toByteArray());
                        fout.close();
                        ContentValues values = new ContentValues();

                        values.put(Images.Media.DATE_TAKEN,
                                System.currentTimeMillis());
                        values.put(Images.Media.MIME_TYPE, "image/jpeg");
                        values.put(MediaColumns.DATA,
                                imageFile.getAbsolutePath());
                        //System.out.println("imagepath:" + imageFile.getAbsolutePath());
                        MultiTrackerActivity.this.getContentResolver().insert(
                                Images.Media.EXTERNAL_CONTENT_URI, values);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                ;
            });
        }
    }


    private File initFilePath() {
        String path;
        if (isSDCardValid()) {
            path = Environment.getExternalStorageDirectory().toString()
                    + File.separator + "recordImage";
            System.out.println(path);
        } else {
            path = Environment.getRootDirectory().toString()
                    + File.separator + "recordImage";
        }
        File imagePath = new File(path);
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        return imagePath;
    }

    private boolean isSDCardValid() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            Toast.makeText(getBaseContext(), "No SD card", Toast.LENGTH_LONG).show();
        }
        return false;
    }

//    private static File getOutputMediaFile() {
//        String state = Environment.getExternalStorageState();
//        if (!state.equals(Environment.MEDIA_MOUNTED)) {
//            return null;
//        }
//        else {
//            File folder_gui = new File(Environment.getExternalStorageDirectory() + File.separator + "GUI");
//            if (!folder_gui.exists()) {
//                Log.v(TAG, "Creating folder: " + folder_gui.getAbsolutePath());
//                folder_gui.mkdirs();
//            }
//            File outFile = new File(folder_gui, "temp.jpg");
//            Log.v(TAG, "Returnng file: " + outFile.getAbsolutePath());
//            return outFile;
//        }
//    }

//    private void deleteNodeInFirebase() {
//        //DatabaseReference applicationInfo;
//        //reference for the application
//        database.getReference("/install_sensors").child(instanceID).child("camera").removeValue();
//    }

//    private void checkNodeInFirebase() {
//        confirmButton.setEnabled(false);
//        DatabaseReference applicationInfo = database.getReference("/install_sensors");
//        final DatabaseReference instanceInfo = applicationInfo.child(instanceID);
//        instanceInfo.addListenerForSingleValueEvent(new ValueEventListener() {
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                try {
//                    boolean enable = (dataSnapshot.child("camera").child(deviceID).child("value").getValue().equals("0"));
//                    System.out.println("enable:" + enable);
//                    System.out.println("cvalue:" + dataSnapshot.child("camera").child(deviceID).child("value"));
//                    confirmButton.setEnabled(enable);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }



    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {


        Context context = getApplicationContext();

        // A face detector is created to track faces.  An associated multi-processor instance
        // is set to receive the face detection results, track the faces, and maintain graphics for
        // each face on screen.  The factory is used by the multi-processor to create a separate
        // activity_tracker instance for each face.
        FaceDetector faceDetector = new FaceDetector.Builder(context).build();
        FaceTrackerFactory faceFactory = new FaceTrackerFactory(mGraphicOverlay);
        faceDetector.setProcessor(
                new MultiProcessor.Builder<>(faceFactory).build());

//        // A barcode detector is created to track barcodes.  An associated multi-processor instance
//        // is set to receive the barcode detection results, track the barcodes, and maintain
//        // graphics for each barcode on screen.  The factory is used by the multi-processor to
//        // create a separate activity_tracker instance for each barcode.
//        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
//        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
//        barcodeDetector.setProcessor(
//                new MultiProcessor.Builder<>(barcodeFactory).build());

        // A multi-detector groups the two detectors together as one detector.  All images received
        // by this detector from the camera will be sent to each of the underlying detectors, which
        // will each do face and barcode detection, respectively.  The detection results from each
        // are then sent to associated activity_tracker instances which maintain per-item graphics on the
        // screen.
        OffsetThread = Thread.activeCount();
        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(faceDetector)
                .build();
//                .add(barcodeDetector)
//                .build();
//        System.out.println("Face Count:" + multiDetector. );
        if (!multiDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        // set CAMERA_FACING_FRONT or CAMERA_FACING_BACK
        mCameraSource = new CameraSource.Builder(getApplicationContext(), multiDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(60.0f)
                .build();


    }

    private void takePicture() {

    }


    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }


    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }
    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

}
