package com.fererlab.nio.listener;

import java.util.logging.Logger;

/**
 * Logger implementation of the ConnectionListener interface
 */
public class LoggerConnectionListener implements ConnectionListener {

    private final String name;

    private Logger logger = Logger.getLogger(LoggerConnectionListener.class.getName());

    /**
     * name set to empty string
     */
    public LoggerConnectionListener() {
        this.name = "";
    }

    /**
     * @param name will be used at logging
     */
    public LoggerConnectionListener(String name) {
        this.name = name;
    }

    @Override
    public void connected(String clientUniqueId) {
        this.logger.info(name + " ConnectionListener connected, client with id: " + clientUniqueId + " connected");
    }

    @Override
    public void disconnected(String clientUniqueId) {
        this.logger.info(name + " ConnectionListener disconnected, client with id: " + clientUniqueId + " disconnected");
    }

    @Override
    public String toString() {
        return " LoggerConnectionListener:" + name + " ";
    }

}
