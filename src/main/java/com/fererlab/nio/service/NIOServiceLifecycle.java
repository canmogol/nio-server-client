package com.fererlab.nio.service;

/**
 * NIO lifecycle interface, can be used for logging and to notify listeners
 */
public interface NIOServiceLifecycle {

    /**
     * should call at creation of NIO implementation
     */
    void onCreate();

    /**
     * should call before the accept for server, connect for client
     */
    void preStart();

    /**
     * should call after the accept for server, connect for client
     */
    void postStart();

    /**
     * should call before the close clear all connections and listeners
     */
    void preStop();

    /**
     * should call to the end
     */
    void postStop();

}
