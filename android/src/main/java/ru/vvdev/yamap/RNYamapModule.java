package ru.vvdev.yamap;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.i18n.LocaleListener;
import com.yandex.runtime.i18n.LocaleResetListener;
import com.yandex.runtime.i18n.LocaleUpdateListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.SuggestItem;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;

public class RNYamapModule extends ReactContextBaseJavaModule {
    private static final String REACT_CLASS = "yamap";

    private ReactApplicationContext getContext() {
        return reactContext;
    }

    private static ReactApplicationContext reactContext = null;


    private final double BOX_SIZE = 0.2;
    private final SearchOptions SEARCH_OPTIONS =  new SearchOptions().setSearchTypes(
            SearchType.GEO.value);
    private SearchManager searchManager;
    private YamapViewManager yamapViewManager;

    RNYamapModule(ReactApplicationContext context, YamapViewManager yamapViewManager) {
        super(context);
        reactContext = context;
        this.yamapViewManager = yamapViewManager;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public Map<String, Object> getConstants() {
        return new HashMap<>();
    }

    @ReactMethod
    public void init(final String apiKey) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                MapKitFactory.setApiKey(apiKey);
                MapKitFactory.initialize(reactContext);
                SearchFactory.initialize(reactContext);
                TransportFactory.initialize(reactContext);
                MapKitFactory.getInstance().onStart();
                searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
            }
        }));
    }

    @ReactMethod
    public void setLocale(final String locale, final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                I18nManagerFactory.setLocale(locale, new LocaleUpdateListener() {
                    @Override
                    public void onLocaleUpdated() {
                        successCb.invoke();
                    }

                    @Override
                    public void onLocaleUpdateError(@NonNull Error error) {
                        errorCb.invoke(error.toString());
                    }
                });
            }
        }));
    }

    @ReactMethod
    public void getLocale(final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                I18nManagerFactory.getLocale(new LocaleListener() {
                    @Override
                    public void onLocaleReceived(@androidx.annotation.Nullable String s) {
                        successCb.invoke(s);
                    }

                    @Override
                    public void onLocaleError(@NonNull Error error) {
                        errorCb.invoke(error.toString());
                    }
                });
            }
        }));
    }

    @ReactMethod
    public void resetLocale(final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                I18nManagerFactory.resetLocale(new LocaleResetListener() {
                    @Override
                    public void onLocaleReset() {
                        successCb.invoke();
                    }

                    @Override
                    public void onLocaleResetError(@NonNull Error error) {
                        errorCb.invoke(error.toString());
                    }
                });
            }
        }));
    }

    @ReactMethod
    public void search(final String query, final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                requestSuggest(query, successCb, errorCb);
            }
        }));
    }
    // Yamap.search({query, point: {latitude, longitude}})
    public void requestSuggest(String query, final Callback successCb, final Callback errorCb) {
        Point CENTER_COORD = this.yamapViewManager.yamapView.getMap().getCameraPosition().getTarget();
        BoundingBox BOUNDING_BOX_LAYER = new BoundingBox(
                new Point(CENTER_COORD.getLatitude() - BOX_SIZE, CENTER_COORD.getLongitude() - BOX_SIZE),
                new Point(CENTER_COORD.getLatitude() + BOX_SIZE, CENTER_COORD.getLongitude() + BOX_SIZE));
        searchManager.suggest(query, BOUNDING_BOX_LAYER, SEARCH_OPTIONS, new SearchManager.SuggestListener() {

            @Override
            public void onSuggestResponse(List<SuggestItem> suggest) {
                WritableArray results = Arguments.createArray();
                for (int i = 0; i < Math.min(50, suggest.size()); i++) {
                    WritableMap suggestMap = Arguments.createMap();
                    suggestMap.putString("displayName", suggest.get(i).getDisplayText());
                    suggestMap.putString("fullName", suggest.get(i).getTitle().getText());
                    results.pushMap(suggestMap);
                }
                // ReactContext reactContext = (ReactContext)getContext();
                WritableMap suggestions = Arguments.createMap();
                suggestions.putArray("suggestions", results);
                successCb.invoke(suggestions);
            }

            @Override
            public void onSuggestError(Error error) {
                Log.d("SuggestError", "error");
            }
        });
    }

    private static void emitDeviceEvent(String eventName, @Nullable WritableMap eventData) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, eventData);
    }
}
