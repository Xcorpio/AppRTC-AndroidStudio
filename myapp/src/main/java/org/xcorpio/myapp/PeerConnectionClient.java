package org.xcorpio.myapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.util.ArrayList;
import java.util.List;

public class PeerConnectionClient implements SignalingEventListener, PeerConnection.Observer, SdpObserver {

    private RoomClient roomClient;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMediaStream;
    private VideoSource videoSource;
    private RtcListener rtcListener;
    private PeerConnection peerConnection;

    public PeerConnectionClient(RtcListener rtcListener) {
        this.rtcListener = rtcListener;

        boolean initializeAudio = true;
        boolean initializeVideo = true;
        boolean videoCodecHwAcceleration = true;
        boolean ret = PeerConnectionFactory.initializeAndroidGlobals(rtcListener, initializeAudio, initializeVideo, videoCodecHwAcceleration);
        Log.i(TAG.TAG_INFO, "PeerConnectionFactory initialize result:" + ret);
        peerConnectionFactory = new PeerConnectionFactory();

        // iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }

    @Override
    public void onPeers(JSONObject data) {
        try {
            JSONArray connections = data.getJSONArray("connections");
            for (int i = 0; i < connections.length(); i++) {
                roomClient.getConnections().add(connections.get(i).toString());
            }
            String id = data.getString("you");
            roomClient.setId(id);
            createPeerConnection();
            sendOffers();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidate(JSONObject data) {
        try {
            if(peerConnection == null) {
                createPeerConnection();
            }
            IceCandidate iceCandidate = new IceCandidate("", data.getInt("label"), data.getString("candidate"));
            peerConnection.addIceCandidate(iceCandidate);
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
        roomClient.getConnections().add(socketId);
    }

    @Override
    public void onRemovePeer(JSONObject data) {
        try {
            String socketId = data.getString("socketId");
            roomClient.getPeerConnections().remove(socketId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOffer(JSONObject data) {
        try {
            JSONObject sdp = data.getJSONObject("sdp");
            SessionDescription remoteSDP = new SessionDescription(SessionDescription.Type.OFFER, sdp.getString("sdp"));
            peerConnection.setLocalDescription(this, remoteSDP);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnswer(JSONObject data) {
        try {
            JSONArray sdp = data.getJSONArray("sdp");
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp.toString());
            peerConnection.setRemoteDescription(this, sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        setCamera();
    }

    private void setCamera() {
        localMediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS");
        Log.i(TAG.TAG_INFO, "get localMediaStream:" + localMediaStream);
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(120)));
        videoSource = peerConnectionFactory.createVideoSource(getVideoCapturer(), videoConstraints);
        localMediaStream.addTrack(peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource));
        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localMediaStream.addTrack(peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource));
        rtcListener.onLocalStream(localMediaStream);
    }

    private VideoCapturer getVideoCapturer() {
        String nameOfFrontFacingDevice = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        return VideoCapturerAndroid.create(nameOfFrontFacingDevice);
    }

    public void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfiguration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfiguration.keyType = PeerConnection.KeyType.ECDSA;
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, mediaConstraints, this);
        Log.e(TAG.TAG_INFO, "create peerConnection:" + peerConnection);
        peerConnection.addStream(localMediaStream);
    }

    public void sendOffers() {
        for (PeerConnection peerConnection : roomClient.getPeerConnections().values()) {
            peerConnection.createOffer(this, pcConstraints);
        }
    }

    public void setRoomClient(RoomClient roomClient) {
        this.roomClient = roomClient;
    }

    // PeerConnection.Observer
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
        sb.append(label).append(",\"candidate\":\"").append(sdp).append("\",\"socketId\":\"").append(roomClient.getConnections().get(0)).append("\"}}");
        String message = sb.toString();
        Log.i(TAG.TAG_INFO, "PCObserver send to signaling server iceCandidate:" + message);
        roomClient.send(message);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.e(TAG.TAG_INFO, "###############PCObserver onAddStream:" + mediaStream);
        rtcListener.onAddRemoteStream(mediaStream, 0);
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

    // SdpObserver
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        PeerConnection peerConnection = roomClient.getPeerConnections().get(roomClient.getConnections().get(0));
        String targetId = roomClient.getConnections().get(0);
        peerConnection.setLocalDescription(this, sessionDescription);
        StringBuilder sb = new StringBuilder("{\"eventName\":\"__offer\",\"data\":{\"sdp\":{\"type\":\"");
        sb.append(sessionDescription.type.toString().toLowerCase()).append("\",\"sdp\":\"").append(sessionDescription.description).append("\"},\"socketId\":\"").append(targetId).append("\"}}");
        String message = sb.toString();
        Log.i(TAG.TAG_INFO, "sendOffers:" + message);
        roomClient.send(message);
    }

    @Override
    public void onSetSuccess() {
        Log.i(TAG.TAG_INFO, "SdpObserver onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.i(TAG.TAG_INFO, "SdpObserver onCreateFailure:" + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.i(TAG.TAG_INFO, "SdpObserver onCreateFailure:" + s);
    }
}
