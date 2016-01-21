package com.fererlab.nio.server;

/**
 * Receiving end's Message listener, receives messages sent by the client
 */
public interface MessageListener {

    /**
     * this method is for those who are not interested in replying back, but interested only listening to receiving messages
     *
     * @param bytes sent by the client which is unknown there see {@link #receive(byte[], String)} method
     */
    void receive(byte[] bytes);

    /**
     * this method is here for those who are interested in replying back to this client/owner of this message
     *
     * @param bytes         sent by the client
     * @param ownerUniqueId this message's owner's unique id
     */
    void receive(byte[] bytes, String ownerUniqueId);

}
