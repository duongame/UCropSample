package com.duongame.ucropsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.yalantis.ucrop.UCrop;

import java.util.List;

import kr.co.amanda.www.cameracrop.R;

public class MainActivity extends AppCompatActivity {
    private ImageView mPhotoImageView;

    private static final String TAG = "MainActivity";

    private void checkPackages() {
        final PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        for (ResolveInfo info : list) {
            String appActivity = info.activityInfo.name;
            String appPackageName = info.activityInfo.packageName;
            String appName = info.loadLabel(pm).toString();

            Drawable drawable = info.activityInfo.loadIcon(pm);

            Log.e(TAG, "appName : " + appName + ", appActivity : " + appActivity
                    + ", appPackageName : " + appPackageName);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // test packages
        checkPackages();

        final Button camera = (Button) findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageCrop.takeCameraAction(MainActivity.this);
                    }
                });

            }
        });

        final Button gallary = (Button) findViewById(R.id.gallery);
        gallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageCrop.takeAlbumAction(MainActivity.this);
                    }
                });
            }
        });

        mPhotoImageView = (ImageView) findViewById(R.id.image);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case UCrop.REQUEST_CROP: {
                Log.d(TAG, "onActivityResult REQUEST_CROP");
                final Uri resultUri = UCrop.getOutput(data);
                Log.d(TAG, "onActivityResult REQUEST_CROP resultUri=" + resultUri.getPath());

                String resultPath = ImageCrop.getRealPathFromURI(this, resultUri);
                if (resultPath == null) {
                    resultPath = resultUri.getPath();
                }

                Log.d(TAG, "onActivityResult REQUEST_CROP resultPath=" + resultPath);
                final Bitmap bitmap = BitmapFactory.decodeFile(resultPath);
                if (bitmap != null) {
                    mPhotoImageView.setImageBitmap(bitmap);
                }
                break;
            }

            case ImageCrop.PICK_FROM_ALBUM: {
                ImageCrop.pickFromAlbum(this, data);
                break;
            }

            case ImageCrop.PICK_FROM_CAMERA: {
                ImageCrop.pickFromCamera(this, data);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        ImageCrop.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
