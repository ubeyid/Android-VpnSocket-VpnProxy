package com.efbiay.ubeyid.VPN;

import android.util.SparseArray;

import com.efbiay.ubeyid.VPN.tcpip.CommonMethods;

public class NatSessionManager {


    private static SparseArray<NatSession> sessions = new SparseArray<>();

    public static NatSession getSession(int portKey){
        NatSession session = sessions.get(portKey);
        if(session != null){
            session.lastNanoTime = System.nanoTime();
        }
        return sessions.get(portKey);
    }

    public static void removeSession(int portKey){
        NatSession session = sessions.get(portKey);
        if(session != null){
            sessions.remove(portKey);
        }
    }

    public static NatSession createSession(int portKey, int remoteIP, short remotePort){

        NatSession session = new NatSession();
        session.lastNanoTime = System.nanoTime();
        session.remoteAddress = remoteIP;
        session.remotePort = remotePort;

        if(session.remoteHost == null){
            session.remoteHost = CommonMethods.ipIntToString(remoteIP);
        }
        sessions.put(portKey, session);
        return session;
    }

    public static class NatSession {

        public int remoteAddress;
        public short remotePort;
        public String remoteHost;
        public int bytesSent, packetsSent;
        public long lastNanoTime;
    }
}
