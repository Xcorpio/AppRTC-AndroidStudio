package org.xcorpio.myapp;

import org.webrtc.MediaStream;

public interface RtcListener {
    void onCallReady(String callId);
    void onStatusChanged(String newStatus);
    void onLocalStream(MediaStream localStream);
    void onAddRemoteStream(MediaStream remoteStream, int endPoint);
    void onRemoveRemoteStream(int endPoint);
}
