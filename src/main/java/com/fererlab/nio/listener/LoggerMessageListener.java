package com.fererlab.nio.listener;

import java.util.logging.Logger;

/**
 * Logger implementation of the MessageListener interface
 */
public class LoggerMessageListener implements MessageListener {

    private final String name;

    private Logger logger = Logger.getLogger(LoggerMessageListener.class.getName());

    /**
     * name set to empty string
     */
    public LoggerMessageListener() {
        this.name = "";
    }

    /**
     * @param name will be used at logging
     */
    public LoggerMessageListener(String name) {
        this.name = name;
    }

    @Override
    public void receive(byte[] bytes) {
        this.logger.info(name + " MessageListener receive, bytes length = " + bytes.length + " message: " + new String(bytes));
    }

    @Override
    public void receive(byte[] bytes, String ownerUniqueId) {
        this.logger.info(name + "MessageListener receive, ownerUniqueId: " + ownerUniqueId + " bytes length = " + bytes.length + " message: " + new String(bytes));
    }

    @Override
    public String toString() {
        return " LoggerMessageListener:" + name + " ";
    }

}
