package org.xcorpio.androidrtc;


import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class WsRoomClient extends WebSocketClient {

    private String TAG = "INFO";

    public WsRoomClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "websocket onOpen");
        String data = "{\"eventName\":\"__join\",\"data\":{\"room\":\"__default\"}}";
        send(data);
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG, "onMessage:" + message);
        try {
            JSONObject jsonObject = new JSONObject(message);
            String eventName = jsonObject.getString("eventName");
            JSONObject data = jsonObject.getJSONObject("data");
            switch (eventName) {
                case "_peers":
                    break;
                case "_ice_candidate":
                    break;
                case "_new_peer":
                    break;
                case "_remove_peer":
                    break;
                case "_offer":
                    break;
                case "_answer":
                    break;
                default:
                    Log.i(TAG, "unknown event");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}