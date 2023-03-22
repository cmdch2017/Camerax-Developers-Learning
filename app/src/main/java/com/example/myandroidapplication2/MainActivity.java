package com.example.myandroidapplication2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.myandroidapplication2.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用 Preview 用例显示相机取景器。
 * 使用 ImageCapture 用例实现了照片拍摄并将图片保存到存储空间。
 * 使用 ImageAnalysis 用例对相机中的帧进行实时分析。
 * 使用 VideoCapture 用例实现了视频拍摄。
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding viewBinding;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static String[] REQUIRED_PERMISSIONS;
    private String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    static {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        }
    }


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());

        viewBinding.videoCaptureButton.setOnClickListener(v -> captureVideo());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        if (imageCapture == null) {
            return;
        }

        // Create time stamped name and MediaStore entry.
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }

                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                    }
                }
        );
    }

    private void captureVideo() {
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
                    // Used to bind the lifecycle of cameras to the lifecycle owner
                    /**
                     * Android 11（API级别 30）中引入的
                     * 通过使用cameraProvider，您可以轻松地实现以下功能：
                     *
                     * 访问可用的相机设备列表和设备属性（例如支持的分辨率、曝光模式和白平衡等）。
                     * 打开相机设备并配置相机参数。
                     * 创建预览、捕获图像和捕获视频的用例。
                     * 控制相机设备的状态（例如打开/关闭闪光灯、切换前/后置摄像头等）。
                     * 使用cameraProvider可以更轻松地管理相机功能，并且能够适应各种不同的设备和相机API。它还提供了与其他CameraX组件（如用例和分析器）集成的便捷方式，以便更好地管理相机应用程序的整个生命周期。
                     */
                    ProcessCameraProvider cameraProvider = null;
                    try {
                        cameraProvider = cameraProviderFuture.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //CameraX Preview 类实现取景器,用户预览他们拍摄的照片
                    // Preview 继承了useCase，也就是说是provider的其中一个功能
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                    //实现了拍照功能
                    // imageCapture 继承了useCase，也就是说是provider的其中一个功能
                    imageCapture = new ImageCapture.Builder().build();

                    //实现了每一帧图像分析功能
                    ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder().build();
                    imageAnalyzer.setAnalyzer(cameraExecutor, new LuminosityAnalyzer(luma -> {
                        Log.d(TAG, "Average luminosity: " + luma);
                    }));

                    // Select back camera as a default
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll();

                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture,imageAnalyzer);

                    } catch (Exception exc) {
                        Log.e(TAG, "Use case binding failed", exc);
                    }
                },
                /**
                 * ContextCompat.getMainExecutor()是一个AndroidX库中的静态方法，它返回一个执行器（Executor）对象，该对象将任务投递到应用程序主线程（UI线程）中执行。
                 *
                 * 在Android应用程序中，由于主线程上运行着应用程序的UI，因此在主线程上执行的任务应该是轻量级的，耗时较短的操作。如果在主线程上执行了耗时较长的操作，将会导致应用程序变得不响应，并且可能会导致ANR（应用程序无响应）错误。
                 *
                 * 为了避免这种情况，Android提供了ContextCompat.getMainExecutor()方法，该方法返回一个执行器对象，该对象可以将任务安排在应用程序的主线程上执行。这个执行器对象可以用于在后台线程上执行异步任务后更新UI或执行其他需要在主线程上执行的任务。
                 *
                 * 使用ContextCompat.getMainExecutor()方法，可以确保任务在主线程上执行，从而避免应用程序的UI变得不响应，并提高应用程序的性能。
                 */
                ContextCompat.getMainExecutor(this));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
