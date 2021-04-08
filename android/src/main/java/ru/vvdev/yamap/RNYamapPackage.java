package ru.vvdev.yamap;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.List;

public class RNYamapPackage implements ReactPackage {
    private YamapViewManager yamapViewManager;

    public RNYamapPackage() {
        yamapViewManager = new YamapViewManager();
    }

    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(new RNYamapModule(reactContext, yamapViewManager));
    }

    @NonNull
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                yamapViewManager,
                new YamapPolygonManager(),
                new YamapPolylineManager(),
                new YamapMarkerManager(),
                new YamapCircleManager()
        );
    }
}
