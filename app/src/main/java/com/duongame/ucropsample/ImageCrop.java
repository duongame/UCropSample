package com.duongame.ucropsample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by namjungsoo on 16. 3. 9..
 */
public class ImageCrop {
    private static final String TAG = "ImageCrop";

    private static final boolean REMOVE_TEMP_FILE = true;
    private static final boolean DEBUG = false;

    public static final int PICK_FROM_CAMERA = 0;
    public static final int PICK_FROM_ALBUM = 1;
    public static final int PERMISSION_FROM_CAMERA = 3;

    private static final int PHOTO_SIZE = 640;
    private static final String ACTIVITY_NAME_PHOTOS = "com.google.android.apps.photos";
    private static final String ACTIVITY_NAME_PLUS = "com.google.android.apps.plus";

    private static boolean mUseActivityPhoto = false;
    private static boolean mUseActivityPlus = false;

    private static Uri mImageCaptureUri;
    private static Bitmap mCropBitmap;
    private static String mTempImagePath;

    private static int mLastAction = PICK_FROM_CAMERA;

    private static final String CAMERA_TEMP_PREFIX = "camera_";
    private static final String CROP_TEMP_PREFIX = "crop_";
    private static final String IMAGE_EXT = ".png";

    public static void checkPackages(Activity context, Intent intentPhoto) {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        mUseActivityPhoto = false;
        mUseActivityPlus = false;

        final List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        // photos는 있지만 해당 인텐트를 photos가 아니라 plus가 가지고 있는 경우가 있어서 정확하게 intent를 query하여 확인함
        for (ResolveInfo info : infos) {
            if(info.activityInfo.packageName.equals(ACTIVITY_NAME_PHOTOS)) {

                final List<ResolveInfo> photoInfos = pm.queryIntentActivities(intentPhoto, PackageManager.MATCH_ALL);
                for (ResolveInfo photoInfo : photoInfos) {
                    if(photoInfo.activityInfo.packageName.equals(ACTIVITY_NAME_PHOTOS)) {
                        Log.d(TAG,"mUseActivityPhoto TRUE");
                        mUseActivityPhoto = true;
                        break;
                    }
                }

            }
            else if(info.activityInfo.packageName.equals(ACTIVITY_NAME_PLUS)) {

                final List<ResolveInfo> photoInfos = pm.queryIntentActivities(intentPhoto, PackageManager.MATCH_ALL);
                for (ResolveInfo photoInfo : photoInfos) {
                    if(photoInfo.activityInfo.packageName.equals(ACTIVITY_NAME_PLUS)) {
                        Log.d(TAG,"mUseActivityPlus TRUE");
                        mUseActivityPlus = true;
                        break;
                    }
                }
            }
        }
    }

    public static void takeCameraAction(Activity context) {
        if(DEBUG)
            Log.d(TAG, "takeCameraAction");
        if (ImageCrop.checkPermissions(context)) {
            ImageCrop.doTakeCameraAction(context);
        } else {
            mLastAction = ImageCrop.PICK_FROM_CAMERA;
        }

    }
    public static void takeAlbumAction(Activity context) {
        if(DEBUG)
            Log.d(TAG, "takeAlbumAction");
        if(ImageCrop.checkPermissions(context)) {
            ImageCrop.doTakeAlbumAction(context);
        }
        else {
            mLastAction = ImageCrop.PICK_FROM_ALBUM;
        }
    }
    /**
     * 카메라에서 이미지 가져오기
     */
    private static void doTakeCameraAction(Activity context) {
        if(DEBUG)
            Log.d(TAG, "doTakeCameraAction");

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // 임시로 사용할 파일의 경로를 생성
        final String url = CAMERA_TEMP_PREFIX + String.valueOf(System.currentTimeMillis()) + IMAGE_EXT;
        final File file = new File(Environment.getExternalStorageDirectory(), url);
        mTempImagePath = file.getAbsolutePath();
        mImageCaptureUri = Uri.fromFile(file);

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

        // 사진은 아무거나로 찍을수 있으므로 패키지 설정하지 않음
//        intent.setPackage("com.google.android.GoogleCamera");

        // 특정기기에서 사진을 저장못하는 문제가 있어 다음을 주석처리 합니다.
        //intent.putExtra("return-data", true);
        context.startActivityForResult(intent, PICK_FROM_CAMERA);
    }
    /**
     * 앨범에서 이미지 가져오기
     */
    private static void doTakeAlbumAction(Activity context) {
        if(DEBUG)
            Log.d(TAG, "doTakeAlbumAction");

        // 앨범 호출해서 이미지 파일을 선택한다.
        final Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);

        checkPackages(context, intent);

        if(mUseActivityPhoto) {
            if(DEBUG)
                Log.d(TAG, "doTakeAlbumAction setPackage ACTIVITY_NAME_PHOTOS");
            intent.setPackage(ACTIVITY_NAME_PHOTOS);
        }
        else if(mUseActivityPlus) {
            if(DEBUG)
                Log.d(TAG, "doTakeAlbumAction setPackage ACTIVITY_NAME_PLUS");
            intent.setPackage(ACTIVITY_NAME_PLUS);
        }
        context.startActivityForResult(intent, ImageCrop.PICK_FROM_ALBUM);
    }

    private static void removeTempFile() {
        // 캡쳐 파일 삭제
        if(mImageCaptureUri != null) {
            final String capturePath = mImageCaptureUri.getPath();
            if(capturePath != null) {
                Log.w(TAG, "removeTempFile capturePath=" + capturePath);

                final File captureFile = new File(capturePath);
                if(captureFile != null) {
                    if (captureFile.getAbsoluteFile().exists()) {
                        captureFile.delete();
                    }
                }
            }
        }

        // 임시 파일 삭제
        if(mTempImagePath != null) {
            Log.w(TAG, "removeTempFile mTempImagePath=" + mTempImagePath);

            final File tempFile = new File(mTempImagePath);
            if(tempFile != null) {
                if(tempFile.getAbsoluteFile().exists()) {
                    tempFile.delete();
                }
            }
        }
    }
    private static void removeDataFile(Intent data) {
        if(data == null) {
            Log.w(TAG, "removeDataFile data == null");
            return;
        }
        if(data.getData() == null) {
            Log.w(TAG, "removeDataFile data.getData() == null");
            return;
        }

        final String dataPath = data.getData().getPath();
        if(dataPath == null) {
            Log.w(TAG, "removeDataFile dataPath == null");
            return;
        }
        Log.w(TAG, "removeDataFile dataPath=" + dataPath);

        final File dataFile = new File(dataPath);
        if(dataFile == null) {
            Log.w(TAG, "removeDataFile dataFile == null");
            return;
        }

        if(dataFile.getAbsoluteFile().exists()) {
            dataFile.delete();
        }
    }

    // 1. data를 체크
    private static File cropFileFromPhotoData(Activity context, Intent data) {
        if(DEBUG)
            Log.d(TAG, "cropFileFromPhotoData");

        if(data.getData() == null) {
            Log.e(TAG, "cropFileFromPhotoData data.getData() == null");
            return null;
        }

        final String dataPath = data.getData().getPath();
        if (dataPath == null) {
            Log.e(TAG, "cropFileFromPhotoData dataPath == null");
            return null;
        }

        File dataFile = null;

        // 파일이 아니라 Uri이다(안드로이드 6.0)
        if(dataPath.startsWith("/external")) {
            final Uri dataUri = Uri.parse("content://media"+dataPath);
            final String dataFilePath = getRealPathFromURI(context, dataUri);
            dataFile = new File(dataFilePath);
            boolean exist = dataFile.exists();
            long length = dataFile.length();
            if(DEBUG)
                Log.d(TAG, "cropFileFromPhotoData dataFilePath=" + dataFilePath + " exist="+exist + " length=" +length);
        }
        else {
            dataFile = new File(dataPath);
            boolean exist = dataFile.exists();
            long length = dataFile.length();
            if(DEBUG)
                Log.d(TAG, "cropFileFromPhotoData dataPath=" + dataPath + " exist="+exist + " length=" +length);
        }


        return dataFile;
    }
    // 2. extra를 체크(저화질)
    private static File cropFileFromPhotoExtra(Activity context, Intent data) {
        if(DEBUG)
            Log.d(TAG, "cropFileFromPhotoExtra");

        final Bundle extras = data.getExtras();
        if (extras == null) {
            if(DEBUG)
                Log.d(TAG, "cropFileFromPhotoExtra extra == null");
            return null;
        }

        mCropBitmap = extras.getParcelable("data");

        File dataFile = null;
        final String dataBitmapPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CROP_TEMP_PREFIX + String.valueOf(System.currentTimeMillis()) + IMAGE_EXT;
        if(dataBitmapPath != null) {
            dataFile = new File(dataBitmapPath);
        }
        try {
            final FileOutputStream out = new FileOutputStream(dataFile);
            mCropBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return dataFile;
    }
    public static File cropFileFromPhoto(Activity context, Intent data) {

        if(data == null) {
            Log.e(TAG, "cropFileFromPhoto data == null");
            removeTempFile();
            return null;
        }

        // 리턴할 파일
        File dataFile = null;

        dataFile = cropFileFromPhotoData(context, data);
        if(dataFile == null) {
            dataFile = cropFileFromPhotoExtra(context, data);
        }

        if(REMOVE_TEMP_FILE)
            removeTempFile();
        return dataFile;
    }

    // 아만다에서는 사용하지 않음
    public static Bitmap cropBitmapFromPhoto(Activity context, Intent data) {
        // 크롭이 된 이후의 이미지를 넘겨 받습니다.
        // 이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에
        // 임시 파일을 삭제합니다.
        if(data == null) {
            Log.e(TAG, "cropBitmapFromPhoto data == null");
            return null;
        }

        final Bundle extras = data.getExtras();
        if (extras != null) {
            if(DEBUG)
                Log.d(TAG, "cropBitmapFromPhoto extra");
            mCropBitmap = extras.getParcelable("data");
        }
        else {
            if(DEBUG)
                Log.d(TAG, "cropBitmapFromPhoto extra == null");
            mCropBitmap = BitmapFactory.decodeFile(data.getData().getPath());
        }

        if(REMOVE_TEMP_FILE)
            removeTempFile();
        removeDataFile(data);
        return mCropBitmap;
    }
    public static void pickFromCamera(Activity context, Intent data) {
        if(DEBUG)
            Log.d(TAG, "pickFromCamera => launchCropActivity");

        if(mTempImagePath == null || mTempImagePath.isEmpty()) {
            Log.e(TAG, "pickFromCamera mTempImagePath error");
            return;
        }

        launchCropActivity(context, mTempImagePath);
    }
    public static void pickFromAlbum(Activity context, Intent data) {
        if(data == null) {
            Log.e(TAG, "pickFromAlbum data == null");
            return;
        }

        mImageCaptureUri = data.getData();

        launchCropActivity(context, mImageCaptureUri);
    }
    private static void launchCropActivity(Activity context, Uri srcUri) {
        if(DEBUG)
            Log.d(TAG, "launchCropActivity Uri");

        final File out = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        final Uri destUri = Uri.fromFile(out);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);

        UCrop.of(srcUri, destUri)
                .withAspectRatio(1,1)
                .withMaxResultSize(PHOTO_SIZE,PHOTO_SIZE)
                .withOptions(options)
                .start(context);
    }
    private static void launchCropActivity(Activity context, File in) {
        if(DEBUG)
            Log.d(TAG, "launchCropActivity File");

        if(!in.exists()) {
            Log.e(TAG, "launchCropActivity !in.exists()");
            return;
        }

        final Uri srcUri = Uri.fromFile(in);
        launchCropActivity(context, srcUri);
    }
    private static void launchCropActivity(Activity context, String path) {
        if(DEBUG)
            Log.d(TAG, "launchCropActivity String");

        final File in = new File(path);
        launchCropActivity(context, in);
    }

    // 파일 패스만 해당한다.
    public static String getRealPathFromURI(Activity context, Uri contentUri) {
        Cursor cursor = null;
        final String[] proj = { MediaStore.Images.Media.DATA };
        String ret = null;

        try {
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            ret = cursor.getString(column_index);
        }
        catch(Exception e) {
            Log.e(TAG, "getRealPathFromURI exception");
            return null;
        }

        if (cursor != null) {
            cursor.close();
        }
        return ret;
    }

    private static boolean checkPermissions(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ) {
                if(DEBUG)
                    Log.d(TAG, "checkPermissions");
                context.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, PERMISSION_FROM_CAMERA);
                return false;// 퍼미션을 체크해야하는 상황에서는 activity result를 받은후에 움직이자.
            }
        }
        return true;
    }

    public static void onRequestPermissionsResult(Activity context, int requestCode, String[] permissions, int[] grantResults) {
        if(DEBUG)
            Log.d(TAG, "onRequestPermissionsResult requestCode="+requestCode);

        final String read = Manifest.permission.READ_EXTERNAL_STORAGE;
        final String write = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        final String camera = Manifest.permission.CAMERA;

        boolean readEnable = false;
        boolean writeEnable = false;
        boolean cameraEnable = false;

        for(int i=0; i<permissions.length; i++) {
            if(DEBUG)
                Log.d(TAG, "onRequestPermissionsResult permissions="+permissions[i] + " grantResults="+grantResults[i]);

            if(read.equals(permissions[i]) && grantResults[i] == 0)
                readEnable = true;
            if(write.equals(permissions[i]) && grantResults[i] == 0)
                writeEnable = true;
            if(camera.equals(permissions[i]) && grantResults[i] == 0)
                cameraEnable = true;
        }

        // 모든 권한이 확보되었을 때
        if(readEnable && writeEnable && cameraEnable) {
            switch(mLastAction) {
                case ImageCrop.PICK_FROM_CAMERA:
                    if(DEBUG)
                        Log.d(TAG, "doTakeCameraAction");
                    ImageCrop.doTakeCameraAction(context);
                    break;
                case ImageCrop.PICK_FROM_ALBUM:
                    if(DEBUG)
                        Log.d(TAG, "doTakeAlbumAction");
                    ImageCrop.doTakeAlbumAction(context);
                    break;
            }
        }
        else {
            if(DEBUG) {
                if(!readEnable)
                    Log.e(TAG, "READ_EXTERNAL_STORAGE not found");
                if(!writeEnable)
                    Log.e(TAG, "WRITE_EXTERNAL_STORAGE not found");
                if(!cameraEnable)
                    Log.e(TAG, "CAMERA not found");
            }
        }
    }
}
