package com.fererlab.nio.listener;

/**
 * nio state listener, this interface is useful to monitor the state of the NIO implementation,
 * NIO may be implemented as server or client, regardless of the implementation this interface will notify the listener
 */
public interface NIOStateListener {

    /**
     * this method will be called just after the creation of NIO implementation
     */
    void created();

    /**
     * will be called just before the accept for server, connect for client
     */
    void willStart();

    /**
     * will be called just after the accept for server, connect for client
     */
    void started();

    /**
     * will be called just before the close clear all connections and listeners
     */
    void willStop();

    /**
     * from NIO implementation's point of view, this method will be called after everything come to an end
     */
    void stopped();

}
