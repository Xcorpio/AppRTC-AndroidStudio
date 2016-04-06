package org.xcorpio.myapp;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SignalingEventListenerImpl implements RoomClient.SignalingEventListener {

    private RoomClient client;

    @Override
    public void onPeers(JSONObject data) {
        try {
            JSONArray connections = data.getJSONArray("connections");
            for (int i = 0; i < connections.length(); i++) {
                client.getConnections().add(connections.get(i).toString());
            }
            String id = data.getString("you");
            client.setId(id);
            client.createPeerConnection();
            client.sendOffers();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidate(JSONObject data) {
        try {
            Object socketId = data.get("socketId");
            PeerConnection peerConnection = client.getPeerConnections().get(socketId);
            if (null != peerConnection) {
                IceCandidate iceCandidate = new IceCandidate("", data.getInt("label"), data.getString("candidate"));
                peerConnection.addIceCandidate(iceCandidate);
            } else {
                Log.i(TAG.TAG_INFO, "SignalingEventListenerImpl onIceCandidate: can't get peerConnection.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewPeer(JSONObject data) {
        String socketId = null;
        try {
            socketId = data.getString("socketId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        client.getConnections().add(socketId);
    }

    @Override
    public void onRemovePeer(JSONObject data) {
        try {
            String socketId = data.getString("socketId");
            client.getPeerConnections().remove(socketId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOffer(JSONObject data) {
        try {
            JSONObject sdp = data.getJSONObject("sdp");
            String socketId = data.getString("socketId");
            PeerConnection peerConnection = client.getPeerConnections().get(socketId);
            SessionDescription remoteSDP = new SessionDescription(SessionDescription.Type.OFFER, sdp.getString("sdp"));
            peerConnection.setLocalDescription(new SDPObserver(), remoteSDP);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnswer(JSONObject data) {
        try {
            JSONArray sdp = data.getJSONArray("sdp");
            String socketId = data.getString("socketId");
            PeerConnection peerConnection = client.getPeerConnections().get(socketId);
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp.toString());
            peerConnection.setRemoteDescription(new SDPObserver(), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setClient(RoomClient client) {
        this.client = client;
    }

    private class SDPObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i(TAG.TAG_INFO, "~~~SDPObserver onCreateSuccess:" + sessionDescription);
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG.TAG_INFO, "~~~SDPObserver onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.i(TAG.TAG_INFO, "~~~SDPObserver onCreateFailure:" + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.i(TAG.TAG_INFO, "~~~SDPObserver onSetFailure:" + s);
        }
    }
}
