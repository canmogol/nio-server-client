package com.fererlab.nio.service;

import com.fererlab.nio.listener.ConnectionListener;
import com.fererlab.nio.listener.MessageListener;
import com.fererlab.nio.listener.NIOStateListener;

import java.io.IOException;

/**
 * this service has at least two implementations, NIO as server and NIO as client
 */
public interface NIOService {

    /**
     * hostname for server implementation indicates the domain name or ip address to listen,
     * for client implementation this is the host's name or ip address to connect
     *
     * @param hostname domain name or ip address
     */
    void setHostname(String hostname);

    /**
     * port is the listening port for server implementation, and it is the connection port for client implementation
     *
     * @param port connection port
     */
    void setPort(Integer port);

    /**
     * without this method call NIO will not start
     *
     * @throws IOException
     */
    void start() throws IOException;

    /**
     * should close connection(s) and notify listeners
     */
    void stop();

    /**
     * server implementation will broadcast message to all clients, client implementation will send the message to server
     *
     * @param bytesToSend message to be send
     */
    void send(byte[] bytesToSend);

    /**
     * this method is useful for NIO server implementation, it can be used to send message to specific client instead of broadcasting
     *
     * @param bytesToSend    message to send
     * @param clientUniqueId UUID of the client
     */
    void send(byte[] bytesToSend, String clientUniqueId);

    /**
     * useful for NIO server implementation check if the client with id is connected
     * NIO client implementation will return its connection state
     *
     * @param clientId client UUID
     * @return will return true if client is connected and available for reading and writing
     */
    boolean isConnected(String clientId);

    /**
     * useful for NIO server implementation, get the status of the client
     * NIO client implementation will always return its state
     *
     * @param clientId client UUID
     * @return will return this client's state, either 0 or a SelectionKey value like SelectionKey.OP_READ
     */
    int getCurrentState(String clientId);

    /**
     * adds a message listener, they will be called upon new message
     *
     * @param listener
     */
    void addMessageListener(MessageListener listener);

    /**
     * adds a connection listener, they will be called upon connect disconnect
     *
     * @param listener
     */
    void addConnectionListener(ConnectionListener listener);

    /**
     * adds an NIOStateListener, they will be called at NIOServiceLifecycle points
     *
     * @param listener
     */
    void addNIOStateListener(NIOStateListener listener);

}
