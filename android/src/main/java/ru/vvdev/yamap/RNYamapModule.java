package ru.vvdev.yamap;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.SuggestOptions;
import com.yandex.mapkit.search.SuggestSession;
import com.yandex.mapkit.search.SuggestType;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.i18n.LocaleListener;

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
    private final SuggestOptions SEARCH_OPTIONS = new SuggestOptions().setSuggestTypes(
            SuggestType.GEO.value);
//     |
//    SuggestType.BIZ.value |
//    SuggestType.TRANSIT.value
    private SearchManager searchManager;
    private SuggestSession suggestSession;
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
                suggestSession = searchManager.createSuggestSession();
            }
        }));
    }

    @ReactMethod
    public void setLocale(final String locale, final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                I18nManagerFactory.setLocale(locale);
                successCb.invoke();
//                , new LocaleUpdateListener() {
//                    @Override
//                    public void onLocaleUpdated() {
//                        successCb.invoke();
//                    }
//
//                    @Override
//                    public void onLocaleUpdateError(@NonNull Error error) {
//                        errorCb.invoke(error.toString());
//                    }
//                }
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
                });
            }
        }));
    }

    @ReactMethod
    public void resetLocale(final Callback successCb, final Callback errorCb) {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                I18nManagerFactory.setLocale(reactContext.getResources().getConfiguration().locale.getLanguage());
                successCb.invoke();
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
        suggestSession.suggest(query, BOUNDING_BOX_LAYER, SEARCH_OPTIONS, new SuggestSession.SuggestListener() {

            @Override
            public void onResponse(@NonNull List<SuggestItem> suggest) {
                WritableArray results = Arguments.createArray();
                for (int i = 0; i < Math.min(50, suggest.size()); i++) {
                    WritableMap suggestMap = Arguments.createMap();
                    suggestMap.putString("displayName", suggest.get(i).getDisplayText());
                    suggestMap.putString("fullName", suggest.get(i).getSearchText());
                    results.pushMap(suggestMap);
                }

                WritableMap suggestions = Arguments.createMap();
                suggestions.putArray("suggestions", results);
                successCb.invoke(suggestions);
            }

            @Override
            public void onError(@NonNull Error error) {
                Log.d("Suggest Error", "OOPS");
            }
        });
    }

    private static void emitDeviceEvent(String eventName, @Nullable WritableMap eventData) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, eventData);
    }
}
