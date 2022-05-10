package com.efbiay.ubeyid.VPN;

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
        // |||||||||||||||
        // |||||||||||||||
        // READ THIS PART
        // |||||||||||||||
        // |||||||||||||||


        //if you want to use this app as a proxy
        //You must specify proyPort and proxyHost
        //After that set isProxyEnabled to true;

        private boolean isProxyEnabled=false;
        private final String proxyHost="";//FOR EXAMPLE "77.39.38.31"
        private final int proxyPort=0; //FOR EXAMPLE 8080


        //This host and port number is request host and port
        //So they are the adress that packet wants to go
        private String host;
        private int port;

        private Socket socket, server;
        private InputStream clientIn, serverIn;
        private OutputStream clientOut, serverOut;

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

                    //this is for when proxy is not enabled we are getting destination host and port
                    //InetSocketAddress destination = new InetSocketAddress(socket.getInetAddress(), session.remotePort & 0xFFFF);
                    InetSocketAddress destination = new InetSocketAddress(socket.getInetAddress(), session.remotePort & 0xFFFF);

                    //Setting up for proxy server
                    host=socket.getInetAddress().getHostAddress();
                    port=session.remotePort & 0xFFFF;
                    String connectRequestToProxyServer=
                            "CONNECT "+host+":"+port+" HTTP/1.1\r\n"+
                            "Host: "+host+":"+port+"\r\n"+
                            "Proxy-Connection: Keep-Alive\r\n"+
                            "Connection: Keep-Alive\r\n"+
                            "User-Agent: Diyarbakir/2121\r\n"+
                            "\r\n";

                    Log.e("info", "CONNECTION:  "+socket.getInetAddress()+":"+session.remotePort+"   -HOST-   "+session.remoteHost);


                    //EVERYTHING PAST THIS WILL BE YOUR PROXY OR WHAT EVER YOU WISH TO DO...

                    //Check that if proxy mode is enabled so we will route all traffic to specified proxy address
                    if(isProxyEnabled){
                        server = new Socket();
                        server.bind(new InetSocketAddress(0));
                        service.protect(server);
                        server.setSoTimeout(5000);
                        server.connect(new InetSocketAddress(proxyHost,proxyPort), 5000);
                        serverIn = server.getInputStream();
                        serverOut = server.getOutputStream();
                        serverOut.write(connectRequestToProxyServer.getBytes(StandardCharsets.UTF_8));
                        byte[] responseFromProxy=new byte[1024];
                        int sizeOfProxyResponse;
                        String responseString;
                        sizeOfProxyResponse=serverIn.read(responseFromProxy);
                        if(sizeOfProxyResponse > 0){
                            responseString=new String(responseFromProxy,0,sizeOfProxyResponse);
                            if(responseString.contains("200")){
                                relay();
                            }
                        }//if size not bigger than 0 so it directly close server and client connection

                    }else{
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
                Log.d("ERROR",e.getMessage());
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
