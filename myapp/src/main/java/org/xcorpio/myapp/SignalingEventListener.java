package org.xcorpio.myapp;

import org.json.JSONObject;

public interface SignalingEventListener {
    void onPeers(JSONObject data);

    void onIceCandidate(JSONObject data);

    void onNewPeer(JSONObject data);

    void onRemovePeer(JSONObject data);

    void onOffer(JSONObject data);

    void onAnswer(JSONObject data);
}