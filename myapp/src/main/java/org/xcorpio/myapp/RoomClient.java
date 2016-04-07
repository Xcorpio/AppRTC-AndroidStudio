package org.xcorpio.myapp;


import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RoomClient extends WebSocketClient {

    private String id;

    // 其他连接的id
    private ArrayList<String> connections = new ArrayList<>();
    // 其他PeerConnection, 键为 id
    private Map<String, PeerConnection> peerConnections = new HashMap<>();
    private SignalingEventListener signalingEventListener;

    public RoomClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG.TAG_INFO, "websocket onOpen");
        String data = "{\"eventName\":\"__join\",\"data\":{\"room\":\"__default\"}}";
        send(data);
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG.TAG_INFO, "onMessage:" + message);
        try {
            JSONObject jsonObject = new JSONObject(message);
            String eventName = jsonObject.getString("eventName");
            JSONObject data = jsonObject.getJSONObject("data");
            switch (eventName) {
                case "_peers":
                    signalingEventListener.onPeers(data);
                    break;
                case "_ice_candidate":
                    signalingEventListener.onIceCandidate(data);
                    break;
                case "_new_peer":
                    signalingEventListener.onNewPeer(data);
                    break;
                case "_remove_peer":
                    signalingEventListener.onRemovePeer(data);
                    break;
                case "_offer":
                    signalingEventListener.onOffer(data);
                    break;
                case "_answer":
                    signalingEventListener.onAnswer(data);
                    break;
                default:
                    Log.i(TAG.TAG_INFO, "unknown event");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(TAG.TAG_INFO, "RoomClient onClose:" + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.i(TAG.TAG_INFO, "RoomClient onError:" + ex);
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getConnections() {
        return connections;
    }

    public Map<String, PeerConnection> getPeerConnections() {
        return peerConnections;
    }


    public void setSignalingEventListener(SignalingEventListener signalingEventListener) {
        this.signalingEventListener = signalingEventListener;
    }
}
