package com.et.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.utils.HandlerThreadHandler;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity extends AppCompatActivity {
    private final Object mSync = new Object();

    private static final String TAG = "MainActivity";
    //    private Button mBtnOpen;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    //    private TextureView mTvCamera;
//    private CardView mCvCamera;
    private USBMonitor monitor;
    private Surface mPreviewSurface;
    private Handler mWorkerHandler;
    private long mWorkerThreadID = -1;
    private List<UsbDevice> list;
    private boolean haveCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mUVCCameraView = findViewById(R.id.simple_camera);
    }

    private void initData() {

        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        Log.d(TAG, "initData: " + screenWidth);
        Log.d(TAG, "initData: " + screenHeight);
//        if (ScreenVoiceUtil.getInstance().getScreenDirection().equals("0")) { // 横屏
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200,100);
//        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(200,100);

//            mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        }
        mWorkerHandler = HandlerThreadHandler.createHandler(TAG);
        mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();
        monitor = new USBMonitor(this, onDeviceConnectListener);
        List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(getApplicationContext(), R.xml.device_filter);
        monitor.setDeviceFilter(filters);
    }

    private void initEvent() {
        mUVCCameraView.setOnClickListener(v -> {
            synchronized (mSync) {
                if (mUVCCamera == null) {
                    if (haveCamera) {
                        monitor.requestPermission(list.get(0));
                        mUVCCameraView.setVisibility(View.VISIBLE);
                    } else {
                        mUVCCameraView.setVisibility(View.GONE);
                    }
//                mBtnOpen.setText("关闭");
                } else {
                    releaseCamera();
//                mBtnOpen.setText("开启");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        monitor.register();
        list = monitor.getDeviceList();
        haveCamera = list.size() != 0;
        for (UsbDevice usbDevice : list) {
            Log.d(TAG, "usb设备: " + usbDevice);
        }
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.startPreview();
            }
        }
    }

    @Override
    protected void onStop() {
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
        }
        if (monitor != null) {
            monitor.unregister();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        synchronized (mSync) {
            releaseCamera();
            if (monitor != null) {
                monitor.destroy();
                monitor = null;
            }
            mUVCCameraView = null;
//        mBtnOpen = null;
            if (mWorkerHandler != null) {
                try {
                    mWorkerHandler.getLooper().quit();
                } catch (final Exception e) {
                    //
                }
                mWorkerHandler = null;
            }
        }
        super.onDestroy();
    }


    private final USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Log.d(TAG, "onAttach: ");
        }

        @Override
        public void onDettach(UsbDevice device) {
            Log.d(TAG, "onDetach: ");
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            releaseCamera();
            Log.d(TAG, "onConnect: ");
            queueEvent(() -> {
                final UVCCamera camera = new UVCCamera();
                camera.open(ctrlBlock);
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
                try {
                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                } catch (final IllegalArgumentException e) {
                    // fallback to YUV mode
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                    } catch (final IllegalArgumentException e1) {
                        camera.destroy();
                        return;
                    }
                }
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                if (st != null) {
                    mPreviewSurface = new Surface(st);
                    camera.setPreviewDisplay(mPreviewSurface);
                    camera.startPreview();
                }
                synchronized (mSync) {
                    mUVCCamera = camera;
                }

            }, 0);
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            releaseCamera();
            Log.d(TAG, "onDisconnect: ");
        }

        @Override
        public void onCancel(UsbDevice device) {
            Log.d(TAG, "onCancel: ");
        }
    };

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
            }
        }
    }

    protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
        if ((task == null) || (mWorkerHandler == null)) return;
        try {
            mWorkerHandler.removeCallbacks(task);
            if (delayMillis > 0) {
                mWorkerHandler.postDelayed(task, delayMillis);
            } else if (mWorkerThreadID == Thread.currentThread().getId()) {
                task.run();
            } else {
                mWorkerHandler.post(task);
            }
        } catch (final Exception e) {
            // ignore
        }
    }
}