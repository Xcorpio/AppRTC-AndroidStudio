package org.xcorpio.myapp;


import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

public class RoomClient extends WebSocketClient {

    private String id;
    private WebSocket socket;
    // 其他连接的id
    private ArrayList<String> connections;
    // 其他PeerConnection, 键为 id
    private Map<String, WebSocket> peerConnections;
    private SignalingEventListener signalingEventListener;
    private RoomClientEventListener roomClientEventListener;

    public RoomClient(URI serverURI, SignalingEventListener signalingEventListener, RoomClientEventListener roomClientEventListener) {
        super(serverURI);
        this.signalingEventListener = signalingEventListener;
        this.roomClientEventListener = roomClientEventListener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG.TAG_INFO, "websocket onOpen");
        String data = "{\"eventName\":\"__join\",\"data\":{\"room\":\"__default\"}}";
        send(data);
        roomClientEventListener.onConnected();
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
                    signalingEventListener.onRemotePeer(data);
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
        Log.i(TAG.TAG_INFO, "onClose:" + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.i(TAG.TAG_INFO, "onError:" + ex.getMessage());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getConnections() {
        return connections;
    }

    public void setConnections(ArrayList<String> connections) {
        this.connections = connections;
    }

    public Map<String, WebSocket> getPeerConnections() {
        return peerConnections;
    }

    public void setPeerConnections(Map<String, WebSocket> peerConnections) {
        this.peerConnections = peerConnections;
    }

    public interface SignalingEventListener {
        void onPeers(JSONObject data);

        void onIceCandidate(JSONObject data);

        void onNewPeer(JSONObject data);

        void onRemotePeer(JSONObject data);

        void onOffer(JSONObject data);

        void onAnswer(JSONObject data);
    }

    public interface RoomClientEventListener {
        void onConnected();
        void onIceCandidate(IceCandidate candidate);
    }
}
