package starmgsio.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.starmicronics.starmgsio.ConnectionInfo;
import com.starmicronics.starmgsio.Scale;
import com.starmicronics.starmgsio.ScaleCallback;
import com.starmicronics.starmgsio.ScaleData;
import com.starmicronics.starmgsio.ScaleOutputConditionSetting;
import com.starmicronics.starmgsio.ScaleSetting;
import com.starmicronics.starmgsio.ScaleType;
import com.starmicronics.starmgsio.StarDeviceManager;
import com.starmicronics.starmgsio.StarDeviceManagerCallback;

import androidx.annotation.NonNull;

public class StarMgsIO extends CordovaPlugin {

    private static final String LOG_TAG = "StarMgsIO";

    private StarDeviceManager mStarDeviceManager;
    private Scale mScale;
    private CallbackContext callback = null;
    private CallbackContext discoveryCallback = null;
    private double lastWeight = 0;
    private String lastUnit = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("startDiscovery".equals(action)) {
            discover(callbackContext);
            return true;
        } else if ("stopDiscovery".equals(action)) {
            stopDiscovery(callbackContext);
            return true;
        } else if ("connect".equals(action)) {
            String id = args.getString(0);
            connect(id, callbackContext);
            return true;
        } else if ("disconnect".equals(action)) {
            disconnect(callbackContext);
            return true;
        } else if ("isConnected".equals(action)) {
            boolean isConnected = mScale != null;
            callbackContext.success(isConnected ? 1 : 0);
            return true;
        }
        return false;
    }

    private void discover(CallbackContext callbackContext) {
        LOG.d(LOG_TAG, "discover");
        stopDiscovery();
        this.discoveryCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                LOG.d(LOG_TAG, "discover got thread");
                mStarDeviceManager = new StarDeviceManager(cordova.getActivity(), StarDeviceManager.InterfaceType.BluetoothLowEnergy);
                mStarDeviceManager.scanForScales(new StarDeviceManagerCallback() {
                    @Override
                    public void onDiscoverScale(@NonNull ConnectionInfo connectionInfo) {
                        JSONObject jsonInfo = getDeviceInfo(connectionInfo);
                        try {
                            jsonInfo.put("update_type", "discovery_update");
                        } catch (JSONException e) {
                            LOG.e(LOG_TAG, e.getMessage(), e);
                        }
                        sendDiscoveryUpdate(jsonInfo, true);
                    }
                });
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    private void stopDiscovery() {
        LOG.d(LOG_TAG, "stopDiscovery wihtout callback");
        this.discoveryCallback = null;
        if(mStarDeviceManager != null){
            mStarDeviceManager.stopScan();
        }
    }

    private void stopDiscovery(CallbackContext callbackContext) {
        LOG.d(LOG_TAG, "stopDiscovery");
        this.discoveryCallback = null;
        if(mStarDeviceManager != null){
            mStarDeviceManager.stopScan();
        }
        callbackContext.success();
    }

    private void connect(String id, final CallbackContext callbackContext) {
        LOG.d(LOG_TAG, "connect");
        this.callback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                LOG.d(LOG_TAG, "connect got thread");
                if(mScale != null){
                    LOG.d(LOG_TAG, "mScale already has value. exiting");
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                } else {
                    LOG.d(LOG_TAG, "starting device connection");
                    ConnectionInfo connectionInfo = new ConnectionInfo.Builder()
                        .setBleInfo(id)
                        .build();

                    StarDeviceManager mStarDeviceManager = new StarDeviceManager(cordova.getActivity());

                    LOG.d(LOG_TAG, "createScale");
                    mScale = mStarDeviceManager.createScale(connectionInfo);
                    LOG.d(LOG_TAG, "connect");
                    mScale.connect(new ScaleCallback() {
                        @Override
                        public void onConnect(Scale scale, int status) {
                            LOG.d(LOG_TAG, "ScaleCallback onConnect");
                            stopDiscovery();
                            sendConnectionUpdate(scale, status);
                        }
                        @Override
                        public void onReadScaleData(Scale scale, ScaleData scaleData) {
                            LOG.d(LOG_TAG, "ScaleCallback onReadScaleData");
                            JSONObject readScaleDataJson = getWeightInfo(scaleData);
                            try {
                                readScaleDataJson.put("update_type", "weight_update");
                                if(readScaleDataJson.has("weight") && readScaleDataJson.has("unit")){
                                    double newWeight = readScaleDataJson.getDouble("weight");
                                    String newUnit = readScaleDataJson.getString("unit");
                                    if(lastWeight != newWeight || newUnit != lastUnit){
                                        lastWeight = readScaleDataJson.getDouble("weight");
                                        lastUnit = readScaleDataJson.getString("unit");
                                        sendWeightUpdate(readScaleDataJson, true);
                                    }
                                }
                            } catch (JSONException e) {
                                LOG.e(LOG_TAG, e.getMessage(), e);
                            }
                        }
                        @Override
                        public void onDisconnect(Scale scale, int status) {
                            LOG.d(LOG_TAG, "ScaleCallback onDisconnect");
                            mScale = null;
                            sendDisconnectionUpdate(scale, status);
                        }
                    });
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            }
        });
    }

    private void disconnect(CallbackContext callbackContext) {
        LOG.d(LOG_TAG, "disconnect");
        if(mScale != null){
            mScale.disconnect();
        }
        callbackContext.success();
    }

    private void sendDiscoveryUpdate(JSONObject connectionJson, boolean keepCallback) {
        LOG.d(LOG_TAG, "sendDiscoveryUpdate");
        if (this.discoveryCallback != null) {
            LOG.d(LOG_TAG, "sendDiscoveryUpdate sending");
            PluginResult result = new PluginResult(PluginResult.Status.OK, connectionJson);
            result.setKeepCallback(keepCallback);
            this.discoveryCallback.sendPluginResult(result);
        }
    }

    private JSONObject getDeviceInfo(@NonNull ConnectionInfo connectionInfo) {
        LOG.d(LOG_TAG, "getDeviceInfo");
        JSONObject obj = new JSONObject();
        try {
            obj.put("interface", connectionInfo.getInterfaceType().name());
            obj.put("name", connectionInfo.getDeviceName());
            obj.put("id", connectionInfo.getIdentifier());
            obj.put("type", connectionInfo.getScaleType().name());
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }

    private void sendConnectionUpdate(Scale scale, int status) {
        LOG.d(LOG_TAG, "sendConnectionUpdate");
        boolean connectSuccess = false;
        JSONObject result = new JSONObject();
        try {
            result.put("update_type", "connection_update");
            switch (status) {
                case Scale.CONNECT_SUCCESS:
                    connectSuccess = true;
                    result.put("status", "success");
                    break;

                case Scale.CONNECT_NOT_AVAILABLE:
                    result.put("status", "not_available");
                    break;

                case Scale.CONNECT_ALREADY_CONNECTED:
                    result.put("status", "already_connected");
                    break;

                case Scale.CONNECT_TIMEOUT:
                    result.put("status", "timeout");
                    break;

                case Scale.CONNECT_READ_WRITE_ERROR:
                    result.put("status", "read_write_error");
                    break;

                case Scale.CONNECT_NOT_SUPPORTED:
                    result.put("status", "not_supported");
                    break;

                case Scale.CONNECT_NOT_GRANTED_PERMISSION:
                    result.put("status", "not_granted_permission");
                    break;

                default:
                case Scale.CONNECT_UNEXPECTED_ERROR:
                    result.put("status", "unexpected_error");
                    break;
            }

            if (!connectSuccess) {
                mScale = null;
            }
        } catch (JSONException e) {
            try {
                result.put("error", "Error occurred while handling callback: " + e.getMessage());
            } catch (JSONException jsonException) {
                LOG.e(LOG_TAG, "JSONException occurred: " + jsonException.getMessage());
            }
        }
        PluginResult resp = new PluginResult(PluginResult.Status.OK, result);
        resp.setKeepCallback(true);
        this.callback.sendPluginResult(resp);
    }

    private void sendDisconnectionUpdate(Scale scale, int status) {
        LOG.d(LOG_TAG, "sendDisconnectionUpdate");
        boolean connectSuccess = false;
        JSONObject result = new JSONObject();
        try {
            result.put("update_type", "disconnection_update");
            switch (status) {
                case Scale.DISCONNECT_SUCCESS:
                    connectSuccess = true;
                    result.put("status", "success");
                    break;

                case Scale.DISCONNECT_NOT_CONNECTED:
                    result.put("status", "not_connected");
                    break;

                case Scale.DISCONNECT_TIMEOUT:
                    result.put("status", "timeout");
                    break;

                case Scale.DISCONNECT_READ_WRITE_ERROR:
                    result.put("status", "read_write_error");
                    break;

                case Scale.DISCONNECT_UNEXPECTED_ERROR:
                    result.put("status", "unexpected_error");
                    break;

                case Scale.DISCONNECT_UNEXPECTED_DISCONNECTION:
                    result.put("status", "unexpected_disconnection");
                    break;
            }

            if (!connectSuccess) {
                mScale = null;
            }
        } catch (JSONException e) {
            try {
                result.put("error", "Error occurred while handling callback: " + e.getMessage());
            } catch (JSONException jsonException) {
                LOG.e(LOG_TAG, "JSONException occurred: " + jsonException.getMessage());
            }
        }
        PluginResult resp = new PluginResult(PluginResult.Status.OK, result);
        resp.setKeepCallback(true);
        this.callback.sendPluginResult(resp);
    }

    private JSONObject getWeightInfo(ScaleData scaleData) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("weight", scaleData.getWeight());
            obj.put("unit", scaleData.getUnit().toString());
            obj.put("comparatorResult", scaleData.getComparatorResult());
            obj.put("dataType", scaleData.getDataType());
            obj.put("status", scaleData.getStatus());
            obj.put("numberOfDecimalPlaces", scaleData.getNumberOfDecimalPlaces());
            obj.put("rawString", scaleData.getRawString());
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }

    private void sendWeightUpdate(JSONObject weightJson, boolean keepCallback) {
        if (this.callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, weightJson);
            result.setKeepCallback(keepCallback);
            this.callback.sendPluginResult(result);
        }
    }
}
