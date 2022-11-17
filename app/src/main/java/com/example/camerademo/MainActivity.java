package com.example.camerademo;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
//References
// https://iotes.lk/2020/12/13/camerax-android-in-java/#google_vignette
public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private ImageCapture imageCapture;
    private ImageView imageView;
    Button btnGallery;
    public static final int GALLERY_REQUEST_CODE = 105;
    private TextView itemLabel;
    private int currentIndex = -1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        Button buttonTakePhoto = findViewById(R.id.buttonTakePhoto);
        Button btnNext = findViewById(R.id.buttonNext);
        Button btnPrevious = findViewById(R.id.buttonPrevious);
        imageView = findViewById(R.id.imageView);
        itemLabel = findViewById(R.id.itemLabel);
        btnGallery = findViewById(R.id.btnGallery);

        //Open Gallery
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            int REQUEST_CODE_PERMISSIONS = 101;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        buttonTakePhoto.setOnClickListener(v -> takePhoto());

        btnNext.setOnClickListener(v -> {
            setAnimationLeftToRight();
            setImageFromStorage();
        });
        btnPrevious.setOnClickListener(view -> {
            setAnimationLeftToLeft();
            setImageFromStorage();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                // Image capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle((this), cameraSelector, preview, imageCapture);
            } catch (InterruptedException | ExecutionException e) {
                // handle InterruptedException.
                Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private List<String> getImageAbsolutePaths() {
        final List<String> paths = new ArrayList();
        final Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = { MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        final Cursor cursor = this.getContentResolver().query(uri, projection, null,
                null, orderBy);

        final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            final String absolutePathOfImage = cursor.getString(column_index_data);
            paths.add(absolutePathOfImage);
        }
        cursor.close();
        return paths;
    }
    //Capture Photo
    private void takePhoto() {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();

        imageCapture.takePicture(outputFileOptions, getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {

                        List<String> imageAbsolutePaths = getImageAbsolutePaths();
                        // display recent captured photo
                        Glide.with(MainActivity.this).load(imageAbsolutePaths.get(0))
                                .centerCrop()
                                .into(imageView);
                        itemLabel.setText(outputFileResults.getSavedUri().getPath());
                        Toast.makeText(MainActivity.this, "Photo has been saved successfully. "+imageAbsolutePaths.size()+"@"+ outputFileResults.getSavedUri().getPath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    //Save Image
    private void setImageFromStorage(){
        List<String> imageAbsolutePaths = getImageAbsolutePaths();
        final int count = imageAbsolutePaths.size();

        if (count ==0 ) {
            Toast.makeText(MainActivity.this, "No photo found", Toast.LENGTH_SHORT).show();
        }
        else {
            currentIndex++;
            if (currentIndex == count) {
                currentIndex = 0;
            }
            final String imagePath = imageAbsolutePaths.get(currentIndex);
            itemLabel.setText(imagePath);
            Glide.with(this).load(imagePath)
                    .centerCrop()
                    .into(imageView);
        }
    }
    //Next
    private void setAnimationLeftToRight() {
        Animation in_left = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        imageView.setAnimation(in_left);
    }
    //Previous
    private void setAnimationLeftToLeft() {
        Animation in_right = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        imageView.setAnimation(in_right);
    }
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }
}