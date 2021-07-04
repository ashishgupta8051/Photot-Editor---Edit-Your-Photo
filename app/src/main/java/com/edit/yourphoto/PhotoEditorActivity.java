package com.edit.yourphoto;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.ByteArrayOutputStream;

import static android.content.ContentValues.TAG;

public class PhotoEditorActivity extends AppCompatActivity {
    private ImageView cameraImage,galleryImage;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> resultGalleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> resultCameraLauncher;
    private static final int PERMISSION = 123;
    private Uri finalUri;
    private Uri cameraImageUri;
    private Bitmap cameraImageMap;
    private InterstitialAd mInterstitialAd;
    private static final String AD_UNIT_ID = "ca-app-pub-7419751706380309/2809326434";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);

        //this.getWindow().setStatusBarColor(Color.parseColor("#6495ED"));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getSupportActionBar().hide();

        cameraImage = findViewById(R.id.camera);
        galleryImage = findViewById(R.id.gallery);

        //Initialize MobileAds
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        galleryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(PhotoEditorActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    galleryLauncher.launch(intent);
                }else {
                    requestPermission();
                }
            }
        });

        cameraImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                        //permission not enabled, request it
                        String[] permission = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        //show popup to request permission
                        requestPermissions(permission,1);
                    }else {
                        openCamera();
                    }
                }else {
                    openCamera();
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            Uri uri = intent.getData();
                            Intent dsPhotoEditorIntent = new Intent(PhotoEditorActivity.this, DsPhotoEditorActivity.class);
                            dsPhotoEditorIntent.setData(uri);
                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_TOOL_BAR_BACKGROUND_COLOR, Color.parseColor("#6495ED"));
                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_MAIN_BACKGROUND_COLOR, Color.parseColor("#FFFFFF"));

                            //Save Photo
                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "/Photo Editor");
                            resultGalleryLauncher.launch(dsPhotoEditorIntent);
                        }else {
                            Log.e("Result","Error in Pick Image");
                        }
                    }
        });

        resultGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            assert intent != null;
                            finalUri = intent.getData();
                            Log.e("Url",String.valueOf(finalUri));
                        }else {
                            Log.e("Result","Error in DsPhotoEditor Intent");
                        }
                    }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Intent dsPhotoEditorIntent = new Intent(PhotoEditorActivity.this, DsPhotoEditorActivity.class);
                            dsPhotoEditorIntent.setData(cameraImageUri);
                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_TOOL_BAR_BACKGROUND_COLOR, Color.parseColor("#6495ED"));
                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_MAIN_BACKGROUND_COLOR, Color.parseColor("#FFFFFF"));

                            dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "/Photo Editor");
                            resultCameraLauncher.launch(dsPhotoEditorIntent);
                        }else {
                            Log.e("Error","Camera Error");
                        }
                    }
        });

        resultCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK){
                            Intent intent = result.getData();
                            assert intent != null;
                            finalUri = intent.getData();
                            Log.e("Url",String.valueOf(finalUri));
                        }else {
                            Log.e("Result","Error in DsPhotoEditor Intent");
                        }
                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        mInterstitialAd = null;
                                        Log.d("TAG", "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        mInterstitialAd = null;
                                        Log.d("TAG", "The ad failed to show.");
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        Log.d("TAG", "The ad was shown.");
                                    }
                                });

                        if (interstitialAd != null) {
                            interstitialAd.show(PhotoEditorActivity.this);
                        } else {
                            Toast.makeText(getApplicationContext(), "Ad did not load", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;

                        @SuppressLint("DefaultLocale") String error =
                                String.format(
                                        "domain: %s, code: %d, message: %s",
                                        loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                        Toast.makeText(
                                PhotoEditorActivity.this, "onAdFailedToLoad() with error: " + error, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION,"From the camera");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Camera Intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(PhotoEditorActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoEditorActivity.this);
            builder.setTitle("Permission needed");
            builder.setMessage("This permission is needed because of this and that");
            builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(PhotoEditorActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION);
                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(PhotoEditorActivity.this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    galleryLauncher.launch(intent);
                    Toast.makeText(this, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}