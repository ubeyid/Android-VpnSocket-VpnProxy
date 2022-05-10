package com.efbiay.ubeyid.VPN;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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


                    //EVERYTHING PAST THIS WILL BE YOUR PROXY OR WHAT EVER YOU WISH TO DO...

                    server = new Socket();
                    server.bind(new InetSocketAddress(0));
                    service.protect(server);
                    server.setSoTimeout(5000);
                    server.connect(destination, 5000);
                    serverIn = server.getInputStream();
                    serverOut = server.getOutputStream();

                    relay();
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
