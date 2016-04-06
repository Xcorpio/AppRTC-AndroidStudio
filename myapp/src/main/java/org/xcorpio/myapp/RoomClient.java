package org.xcorpio.myapp;


import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomClient extends WebSocketClient {

    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private String id;

    // 其他连接的id
    private ArrayList<String> connections = new ArrayList<>();
    // 其他PeerConnection, 键为 id
    private Map<String, PeerConnection> peerConnections = new HashMap<>();
    private SignalingEventListener signalingEventListener;

    private PCObserver pcObserver = new PCObserver();
    private MediaConstraints sdpMediaConstraints;

    private MediaStream localMediaStream;

    public RoomClient(URI serverURI, SignalingEventListener signalingEventListener, PeerConnectionFactory peerConnectionFactory, MediaStream localMediaStream) {
        super(serverURI);
        this.signalingEventListener = signalingEventListener;
        this.peerConnectionFactory = peerConnectionFactory;
        this.localMediaStream = localMediaStream;

        PeerConnection.IceServer iceServer = new PeerConnection.IceServer("stun:stun.l.google.com:19302");
        iceServers.add(iceServer);
        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "false"));
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

    public void createPeerConnection() {
        if (connections.size() > 0) {
            String remoteClienId = connections.get(0);
            PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            rtcConfiguration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
            rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfiguration.keyType = PeerConnection.KeyType.ECDSA;
            MediaConstraints mediaConstraints = new MediaConstraints();
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToRecieveAudio", "true"));
            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToRecieveVideo", "true"));
            PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, mediaConstraints, pcObserver);
            peerConnections.put(remoteClienId, peerConnection);
            peerConnection.addStream(localMediaStream);
        }
    }

    public void sendOffers() {
        for (PeerConnection peerConnection : peerConnections.values()) {
            peerConnection.createOffer(new SDPObserver(connections.get(0)), sdpMediaConstraints);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(TAG.TAG_INFO, "RoomClient onClose:" + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.i(TAG.TAG_INFO, "RoomClient onError:" + ex.getMessage());
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

    public Map<String, PeerConnection> getPeerConnections() {
        return peerConnections;
    }

    public interface SignalingEventListener {
        void onPeers(JSONObject data);

        void onIceCandidate(JSONObject data);

        void onNewPeer(JSONObject data);

        void onRemovePeer(JSONObject data);

        void onOffer(JSONObject data);

        void onAnswer(JSONObject data);
    }

    private class PCObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~~~~~~PCObserver onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~~~~~~PCObserver onIceConnectionChange:" + iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~~~PCObserver onIceConnectionReceivingChange:" + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~PCObserver onIceGatheringChange:" + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~onIceCandidate:" + iceCandidate);
            int label = iceCandidate.sdpMLineIndex;
            String sdp = iceCandidate.sdp;
            StringBuilder sb = new StringBuilder("{\"eventName\":\"__ice_candidate\",\"data\":{\"label\":");
            sb.append(label).append(",\"candidate\":\"").append(sdp).append("\",\"socketId\":\"").append(connections.get(0)).append("\"}}");
            String message = sb.toString();
            Log.i(TAG.TAG_INFO, "PCObserver send to signaling server iceCandidate:" + message);
            send(message);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.i(TAG.TAG_INFO, "###############PCObserver onAddStream:" + mediaStream);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~PCObserver onRemoveStream:" + mediaStream);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~PCObserver onDataChannel:" + dataChannel);
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.i(TAG.TAG_INFO, "~~~~~~~~~~~~~~~~~PCObserver onRenegotiationNeeded");
        }
    }

    private class SDPObserver implements SdpObserver {
        private String targetId;

        public SDPObserver(String targetId) {
            this.targetId = targetId;
        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            PeerConnection peerConnection = peerConnections.get(targetId);
            peerConnection.setLocalDescription(this, sessionDescription);
            StringBuilder sb = new StringBuilder("{\"eventName\":\"__offer\",\"data\":{\"sdp\":{\"type\":\"");
            sb.append(sessionDescription.type.toString().toLowerCase()).append("\",\"sdp\":\"").append(sessionDescription.description).append("\"},\"socketId\":\"").append(targetId).append("\"}}");
            String message = sb.toString();
            Log.i(TAG.TAG_INFO, "sendOffers:" + message);
            send(message);
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG.TAG_INFO, "sdbObserver onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.i(TAG.TAG_INFO, "sdbObserver onCreateFailure:" + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.i(TAG.TAG_INFO, "sdbObserver onCreateFailure:" + s);
        }
    }
}
