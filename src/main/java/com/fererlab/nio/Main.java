package com.fererlab.nio;

import com.fererlab.nio.listener.ConnectionListener;
import com.fererlab.nio.listener.LoggerConnectionListener;
import com.fererlab.nio.listener.LoggerMessageListener;
import com.fererlab.nio.listener.MessageListener;
import com.fererlab.nio.server.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    private void start() {
        // prepare at least one message listener
        Set<MessageListener> messageListeners = new HashSet<>();
        messageListeners.add(new LoggerMessageListener("First"));
        messageListeners.add(new LoggerMessageListener("Second"));

        // prepare at least one connection listener
        Set<ConnectionListener> connectionListeners = new HashSet<>();
        connectionListeners.add(new LoggerConnectionListener("First"));
        connectionListeners.add(new LoggerConnectionListener("Second"));

        // testing hostname and port
        String hostname = "localhost";
        Integer port = 8992;
        // create an NIOServer
        final NIOServer nioServer = new NIOServer(hostname, port, messageListeners, connectionListeners);
        // NIOServer could also be created as follows
//        final NIOServer nioServer = new NIOServer();
//        nioServer.setHostname(hostname);
//        nioServer.setPort(port);

        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("client message sender started, usage:");
                    System.out.println("\t ClientUniqueId|message");
                    System.out.println("\t or free format string, this will be broadcasted to all clients");
                    while (true) {
                        try {
                            System.out.println("type something: ");
                            Scanner keyboard = new Scanner(System.in);
                            String line = keyboard.nextLine();
                            if ("quit".equals(line)) {
                                break;
                            } else if (line.lastIndexOf("|") != -1) {
                                String[] split = line.split("\\|", 2);
                                nioServer.send((split[1] + "\r").getBytes(), split[0]);
                            } else {
                                nioServer.send((line + "\r").getBytes());
                            }
                        } catch (Exception e) {
                            System.out.println("got exception will break loop, exception: " + e.getMessage());
                            break;
                        }
                    }
                    System.out.println("client message sender quited");
                    nioServer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        try {
            // start NIOServer, start method will block here
            // you may want to run server in another thread
            nioServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
