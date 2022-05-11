package com.efbiay.ubeyid.VPN;

import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class Proxy extends Thread {
    private int portKey;
    private VPNService service;
    public short port;
    public Proxy(VPNService service, int port){
        this.service = service;
        this.port = (short) port;
    }

    @Override
    public void run(){
        try{
            Socket socket;
            ServerSocket serverSocket = new ServerSocket(port);

            port = (short) (serverSocket.getLocalPort() & 0xFFFF);
            Log.e("info", "VPNtoSocket VPN started on port: "+serverSocket.getLocalPort());

            while((socket = serverSocket.accept()) != null && !isInterrupted()){
                (new Tunnel(socket)).start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public class Tunnel extends Thread {

        private Socket socket, server;
        private InputStream clientIn, serverIn;
        private OutputStream clientOut, serverOut;


        /////IF YOU WANT TO USE PROXY SO ROUTE YOUR ALL TRAFFIC TO PROXY YOU MUST SET isProxyEnabled=true
        //// AND YOU MUST SET proxyHost="YOUR PROXY HOST" AND proxyPort=YOUR PROXY PORT
        //// AND IF YOUR PROXY REQUIRES PROXY AUTHENTICATION
        //// THEN YOU MUST SET proxyUserName="YOUR PROXY USERNAME" AND proxyPassword="YOUR PROXY PASSWORD"
        private final boolean isProxyEnabled=false;
        private final String proxyHost="192.168.123.152";//FOR EXAMPLE 192.168.123.152
        private final int proxyPort=8888;//8888
        private final String proxyUserName="ubeyid";
        private final String proxyPassword="12345";
        private final String proxyAuth="Basic "+ Base64.encodeToString((proxyUserName + ":" + proxyPassword).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        public Tunnel(Socket socket){

            this.socket = socket;

        }


        @Override
        public void run(){
            try{
                portKey=(short) socket.getPort();
                NatSessionManager.NatSession session = NatSessionManager.getSession((short) socket.getPort());

                if(session != null){

                    clientIn = socket.getInputStream();
                    clientOut = socket.getOutputStream();

                    //InetSocketAddress destination = new InetSocketAddress(socket.getInetAddress(), session.remotePort & 0xFFFF);
                    InetSocketAddress destination = new InetSocketAddress(socket.getInetAddress(), session.remotePort & 0xFFFF);
                    String host=socket.getInetAddress().getHostAddress();
                    int port=session.remotePort & 0xFFFF;

                    Log.e("info", "CONNECTION:  "+socket.getInetAddress()+":"+session.remotePort+"   -HOST-   "+session.remoteHost);

                    String connectToProxy="CONNECT "+host+":"+port+" HTTP/1.1\r\n"+
                                           "Host: "+host+":"+port+"\r\n"+
                                           "Proxy-Connection: Keep-Alive\r\n"+
                                           "Proxy-Authorization: "+proxyAuth+"\r\n"+
                                           "Connection: Keep-Alive\r\n"+
                                           "User-Agent: Diyarbakir/2121\r\n"+
                                           "\r\n";

                    //EVERYTHING PAST THIS WILL BE YOUR PROXY OR WHAT EVER YOU WISH TO DO...
                    if(isProxyEnabled){
                        Thread.sleep(100);
                        server = new Socket();
                        server.bind(new InetSocketAddress(0));
                        service.protect(server);
                        server.setSoTimeout(5000);
                        server.connect(new InetSocketAddress(proxyHost,proxyPort), 5000);
                        serverIn = server.getInputStream();
                        serverOut = server.getOutputStream();
                        serverOut.write(connectToProxy.getBytes(StandardCharsets.UTF_8));
                        byte[] responseArrayFromProxy=new byte[1024];
                        int responseSize=serverIn.read(responseArrayFromProxy);
                        if(responseSize > 0){
                            String responseStringFromProxy=new String(responseArrayFromProxy,0,responseSize);
                            //CHECK RESPONSE FROM PROXY IF IT CONTAINS 200
                            // THAT MEANS CONNECTION ESTABLISHED SO ROUTE TRAFFIC TO PROXY
                            if(responseStringFromProxy.contains("200")){
                                relay();
                            }
                        }

                    }else {
                        //IN HERE WE DONT USE PROXY SEND ALL TRAFFIC TO THEIR REAL DESTINATION ADRESS
                        server = new Socket();
                        server.bind(new InetSocketAddress(0));
                        service.protect(server);
                        server.setSoTimeout(5000);
                        server.connect(destination, 5000);
                        serverIn = server.getInputStream();
                        serverOut = server.getOutputStream();

                        relay();
                    }

                }
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                quickClose(socket);
                quickClose(server);
                NatSessionManager.removeSession(portKey);
            }
        }

        public void relay(){

            Thread thread = new Thread(new Runnable(){

                @Override
                public void run(){
                    byte[] buffer = new byte[4096];
                    int length;
                    try{
                        while((length = clientIn.read(buffer)) > 0){

                            try{
                                serverOut.write(buffer, 0, length);
                                serverOut.flush();
                                Log.d("Request",new String(buffer,0,length));
                            }catch(Exception e){
                                e.printStackTrace();

                            }

                        }
                    }catch(Exception e){
                        e.printStackTrace();

                    }
                }
            });

            thread.start();

            try{
                byte[] buffer = new byte[4096];
                int length;

                while((length = serverIn.read(buffer)) > 0){
                    try{
                        clientOut.write(buffer, 0, length);
                        clientOut.flush();
                        Log.d("Response",new String(buffer,0,length));
                    }catch(Exception e){
                        e.printStackTrace();

                    }

                }

                thread.interrupt();
            }catch(Exception e){
            }
        }

        public void quickClose(Socket socket){
            try{
                if(!socket.isOutputShutdown()){
                    socket.shutdownOutput();
                }
                if(!socket.isInputShutdown()){
                    socket.shutdownInput();
                }

                socket.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
