package com.fererlab.nio.listener;

import java.util.logging.Logger;

/**
 * Logger implementation of the NIOStateListener interface
 */
public class LoggerNIOStateListener implements NIOStateListener {

    private final String name;

    private Logger logger = Logger.getLogger(LoggerNIOStateListener.class.getName());

    /**
     * name set to empty string
     */
    public LoggerNIOStateListener() {
        this.name = "";
    }

    public LoggerNIOStateListener(String name) {
        this.name = name;
    }

    @Override
    public void created() {
        this.logger.info(name + " LoggerNIOStateListener created, NIO created");
    }

    @Override
    public void willStart() {
        this.logger.info(name + " LoggerNIOStateListener created, NIO will start");
    }

    @Override
    public void started() {
        this.logger.info(name + " LoggerNIOStateListener created, NIO started");
    }

    @Override
    public void willStop() {
        this.logger.info(name + " LoggerNIOStateListener created, NIO will stop");
    }

    @Override
    public void stopped() {
        this.logger.info(name + " LoggerNIOStateListener created, NIO stopped");
    }

}
