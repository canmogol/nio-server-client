package com.fererlab.nio.server;

/**
 * client connection listener
 */
public interface ConnectionListener {

    /**
     * this method called at client connection accepted
     *
     * @param clientUniqueId is the UUID of the connected client
     */
    void connected(String clientUniqueId);

    /**
     * this method called at the client disconnect
     *
     * @param clientUniqueId is the UUID of the connected client
     */
    void disconnected(String clientUniqueId);

}
