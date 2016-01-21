package com.fererlab.nio.server;

import java.util.logging.Logger;

public class NIOServerLogger {

    private Logger logger = Logger.getLogger(NIOServer.class.getName());

    public void numberOfMessagesDiscardedForClient(String clientUniqueId, int size) {
        logger.warning("there are " + size + " messages discarded for client: " + clientUniqueId);
    }

    public void emptyConstructorCalledNoParametersAvailable() {
        logger.warning("there is no hostname, port or listeners available, please use setter methods to set them before calling the start method");
    }

    public void nioServerWillStart(String hostname, Integer port, String messageListeners, String connectionListeners, String nioStateListeners) {
        logger.info("NIO Server will start with hostname: " + hostname + " port: " + port + " messageListeners: " + messageListeners + " connectionListeners: " + connectionListeners + " nioStateListeners: " + nioStateListeners);
    }

    public void selectorRegisteredToAccept() {
        logger.info("NIO Server registered to ACCEPT, now it will accept connections");
    }

    public void blockOnSelectorSelect() {
        logger.info("NIO Server blocks on its selector's select() method");
    }

    public void selectorSelectNotified() {
        logger.info("Selector woke up, ");
    }

    public void clientHasMessages(String clientUniqueId, int size) {
        logger.info("client with unique id: " + clientUniqueId + " has " + size + " messages, its interest set to WRITE");
    }

    public void selectionKeyInvalid() {
        logger.info("selection key is invalid, will skip this one and continue on other selectedKeys");
    }

    public void acceptCalled() {
        logger.warning("accept from channel called");
    }

    public void acceptClientId(String clientUniqueId) {
        logger.info("a client created with a unique id: " + clientUniqueId);
    }

    public void registeredToRead(String clientUniqueId) {
        logger.info("a new client with id: " + clientUniqueId + " connected and it is registered to READ");
    }

    public void readCalled() {
        logger.warning("read from channel called");
    }

    public void numberOfBytesRead(int numberOfBytes) {
        logger.info("number of bytes read from channel: " + numberOfBytes);
    }

    public void emptyRead() {
        logger.warning("an empty read occured, client send an empty message");
    }

    public void clientClosedConnection() {
        logger.warning("client cleanly closed the connection at its side");
    }

    public void clientSocketChannelRemoved(String clientUniqueId) {
        logger.warning("socket channel removed from map for client: " + clientUniqueId);
    }

    public void clientSentMessage() {
        logger.info("client sent a message");
    }

    public void singleBufferedBytesReadFromChannel(int length) {
        logger.info("all bytes sent by client read from channel, message length: " + length);
    }

    public void willReadAllBytes(int bufferSize) {
        logger.warning("current buffer size: " + bufferSize + " was not enough for message, will read the rest of it");
    }

    public void readAllClientSentBytes(int length) {
        logger.warning("read all bytes from client, message length: " + length);
    }

    public void willNotifyListenerWithoutClientId(String listener) {
        logger.warning("will call receive method of the MessageListener: " + listener);
    }

    public void willNotifyListenerWithClientId(String listener, String clientUniqueId) {
        logger.warning("will call receive method of the MessageListener: " + listener + " with clientId: " + clientUniqueId);
    }

    public void sendCalled(int length, String clientUniqueId) {
        logger.warning("send method called for clientId: " + clientUniqueId + " with message length: " + length);
    }

    public void willBroadcastMessage(int length) {
        logger.warning("will broadcast message to all clients, message length: " + length);
    }

    public void willSendMessage(String clientUniqueId) {
        logger.warning("will send message to client: " + clientUniqueId);
    }

    public void willCallWakeUp() {
        logger.warning("will call wakeUp on selector");
    }

    public void writeCalled() {
        logger.warning("write from channel called");
    }

    public void willWriteMessageToClient(String clientUniqueId, int length) {
        logger.info("will write message to client: " + clientUniqueId + " with length: " + length);
    }

    public void noClientForSocket() {
        logger.warning("there is no client for this socket channel!");
    }

    public void multipleBufferedBytesReadFromChannel(int size) {
        logger.info("all bytes sent by client read from channel, total message length: " + size);
    }

    public void willNotifyListenerWithClientConnected(String listener, String clientUniqueId) {
        logger.warning("will notify listener: " + listener + " a client connected, clientId: " + clientUniqueId);
    }

    public void willNotifyListenerWithClientDisconnected(String listener, String clientUniqueId) {
        logger.warning("will notify listener: " + listener + " a client disconnected, clientId: " + clientUniqueId);
    }

    public void nextBroadcastClient(String currentClientUniqueId) {
        logger.info("next broadcast client: " + currentClientUniqueId);
    }

    public void nioServerStarted(String hostname, Integer port, String messageListeners, String connectionListeners, String nioStateListeners) {
        logger.info("NIO Server started with hostname: " + hostname + " port: " + port + " messageListeners: " + messageListeners + " connectionListeners: " + connectionListeners + " nioStateListeners: " + nioStateListeners);
    }

    public void stopped() {
        logger.info("NIO Server stopped, this is its last log");
    }

    public void willStop() {
        logger.info("NIO Server will stop");
    }
}
