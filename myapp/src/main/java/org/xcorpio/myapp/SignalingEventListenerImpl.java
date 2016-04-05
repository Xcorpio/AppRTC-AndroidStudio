package org.xcorpio.myapp;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;

public class SignalingEventListenerImpl implements RoomClient.SignalingEventListener {

    private RoomClient client;

    private RoomClient.RoomClientEventListener roomClientEventListener;

    @Override
    public void onPeers(JSONObject data) {
        try {
            JSONArray connections = data.getJSONArray("connections");
            for (int i = 0; i < connections.length(); i++) {
                client.getConnections().add(connections.get(i).toString());
            }
            String id = data.getString("you");
            client.setId(id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidate(JSONObject data) {
        try {
            IceCandidate iceCandidate = new IceCandidate("", data.getInt("label"), data.getString("candidate"));
            roomClientEventListener.onIceCandidate(iceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewPeer(JSONObject data) {

    }

    @Override
    public void onRemotePeer(JSONObject data) {

    }

    @Override
    public void onOffer(JSONObject data) {

    }

    @Override
    public void onAnswer(JSONObject data) {

    }

    public void setClient(RoomClient client) {
        this.client = client;
    }

    public RoomClient.RoomClientEventListener getRoomClientEventListener() {
        return roomClientEventListener;
    }

    public void setRoomClientEventListener(RoomClient.RoomClientEventListener roomClientEventListener) {
        this.roomClientEventListener = roomClientEventListener;
    }
}
