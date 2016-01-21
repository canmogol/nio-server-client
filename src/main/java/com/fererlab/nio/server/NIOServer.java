package com.fererlab.nio.server;

import com.fererlab.nio.listener.ConnectionListener;
import com.fererlab.nio.listener.MessageListener;
import com.fererlab.nio.listener.NIOStateListener;
import com.fererlab.nio.service.NIOService;
import com.fererlab.nio.service.NIOServiceLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class NIOServer implements NIOService, NIOServiceLifecycle {

    // server's hostname address
    private String hostname;

    // port to listen
    private Integer port;

    // Set of messageListeners for client messages
    private Set<MessageListener> messageListeners = new HashSet<>();

    // Set of connectionListeners for client connections
    private Set<ConnectionListener> connectionListeners = new HashSet<>();

    // Set of nioStateListeners for NIOServer state
    private Set<NIOStateListener> nioStateListeners = new HashSet<>();

    // the selector we will be monitoring
    private Selector selector;

    // flag to stop server
    private boolean running = true;

    // size of the buffer
    private static final int BUFFER_SIZE = 8192;

    // byte buffer for messages
    private ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    // client and current state map
    // state can be either 1,4,8,16
    // check SelectionKey.OP_... for details
    private final Map<String, Integer> clientStateMap = new HashMap<>();

    // client unique id and its messages, a mail box for each client
    private final Map<String, List<byte[]>> pendingBytesToSend = new HashMap<>();

    // every socket channel is associated with a UUID, this UUID is the client unique id
    private final Map<String, SocketChannel> uuidSocketChannelMap = new HashMap<>();

    // logger for NIOServer
    private NIOServerLogger logger = new NIOServerLogger();

    /**
     * This constructor may be used in an DI Container,
     * remember to set hostname, port and MessageListener via setter methods
     */
    public NIOServer() {
        this.logger.emptyConstructorCalledNoParametersAvailable();
        // call lifecycle method create to handle listeners and logging
        this.onCreate();
    }

    /**
     * hostname and port should be available for this constructor
     *
     * @param hostname listening hostname address or domain name
     * @param port     listening port number
     */
    public NIOServer(String hostname, Integer port, Set<MessageListener> messageListeners, Set<ConnectionListener> connectionListeners) {
        super();// implicitly added super(); constructor call, check empty/default constructor, it will notify state listeners
        this.hostname = hostname;
        this.port = port;
        this.messageListeners = messageListeners;
        this.connectionListeners = connectionListeners;
    }

    /*
     * below are the NIOService interface methods
     */

    /**
     * server should open socket and start listening, should throw IOException if it could not open connection
     *
     * @throws IOException
     */
    @Override
    public void start() throws IOException {
        // call lifecycle pre-start method, handle listeners and internal logging
        this.preStart();
        // a selector provider will create a selector, DevPollSelectorProvider for SunOS, EPollSelectorProvider for linux etc.
        selector = Selector.open();
        // this will open a server socket, check Net::socket0 native method if needed
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // this will raise an IOException(ClosedChannelException) if the channel is closed
        serverSocketChannel.configureBlocking(false);
        // IP Socket Address (host and port number)
        InetSocketAddress inetSocketAddress = new InetSocketAddress(hostname, port);
        // this will create a ServerSocket (actually a ServerSocketAdaptor)
        // calling socket() method will always return the same server socket
        ServerSocket serverSocket = serverSocketChannel.socket();
        // this will bind the server socket to ip/port
        serverSocket.bind(inetSocketAddress);
        // register channel with selector
        // returning selection key which will be ignored here
        /*SelectionKey key = */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT/*, if needed, you can attach an object here*/);
        this.logger.selectorRegisteredToAccept();

        // call lifecycle method to log and notify listeners
        this.postStart();

        // loop until running flag is false
        while (running) {
            try {
                this.logger.blockOnSelectorSelect();

                // select() will block until an event occurs
                this.selector.select();
                this.logger.selectorSelectNotified();

                // check if there are any pending bytes scheduled to send to client(s)
                if (this.pendingBytesToSend.size() > 0) {
                    for (String clientUniqueId : this.pendingBytesToSend.keySet()) {
                        if (this.uuidSocketChannelMap.containsKey(clientUniqueId)) {
                            if (this.pendingBytesToSend.get(clientUniqueId).size() > 0) {
                                SocketChannel currentClientSocketChannel = this.uuidSocketChannelMap.get(clientUniqueId);
                                SelectionKey selectionKey = currentClientSocketChannel.keyFor(this.selector);
                                if (selectionKey != null && !selectionKey.isWritable()) {
                                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                                    //set current state for this client
                                    this.clientStateMap.put(clientUniqueId, SelectionKey.OP_WRITE);
                                    this.logger.clientHasMessages(clientUniqueId, this.pendingBytesToSend.get(clientUniqueId).size());
                                }
                            }
                        } else {
                            List<byte[]> messages = this.pendingBytesToSend.remove(clientUniqueId);
                            this.logger.triedToSendMessageToDisconnectedClient(clientUniqueId, messages.size());
                        }
                    }
                }

                // will iterate through selector's selected-key set
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    // current selection key
                    SelectionKey selectionKey = (SelectionKey) selectedKeys.next();
                    // remove this key from collection
                    selectedKeys.remove();
                    // selection key may not be valid,
                    // selection keys may be set as invalid and they will be removed,
                    // do nothing for this key
                    if (!selectionKey.isValid()) {
                        this.logger.selectionKeyInvalid();
                        continue;
                    }
                    // handle each event accordingly
                    if (selectionKey.isAcceptable()) {
                        this.accept(selectionKey);
                    } else if (selectionKey.isReadable()) {
                        this.read(selectionKey);
                    } else if (selectionKey.isWritable()) {
                        this.write(selectionKey);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // call lifecycle method to log and notify listeners
        this.postStop();

    }

    @Override
    public void stop() {

        // call lifecycle method pre-stop to log and notify listeners
        this.preStop();

        // set running to false
        running = false;
        // wake up selector if it was blocked at selector.select()
        this.selector.wakeup();
    }

    /**
     * this method will broadcast bytes to all available clients
     *
     * @param bytesToSend byte array of the message
     */
    @Override
    public void send(byte[] bytesToSend) {
        this.send(bytesToSend, null);
    }

    /**
     * this method will schedule bytes to be sent to indicated client
     *
     * @param bytesToSend    byte array of the message
     * @param clientUniqueId client's unique id, obtained via {@link MessageListener#receive(byte[])} method
     */
    @Override
    public void send(byte[] bytesToSend, String clientUniqueId) {
        this.logger.sendCalled(bytesToSend.length, clientUniqueId);
        synchronized (this.pendingBytesToSend) {
            // check if this is a broadcast
            if (clientUniqueId == null) {
                this.logger.willBroadcastMessage(bytesToSend.length);
                for (String currentClientUniqueId : this.uuidSocketChannelMap.keySet()) {
                    this.logger.nextBroadcastClient(currentClientUniqueId);
                    // check if this client has ever got any message
                    if (!this.pendingBytesToSend.containsKey(currentClientUniqueId)) {
                        // if not, create a list for client in pendingBytesToSend Map
                        this.pendingBytesToSend.put(currentClientUniqueId, new LinkedList<byte[]>());
                    }
                    // add these bytes to every client's list
                    this.pendingBytesToSend.get(currentClientUniqueId).add(bytesToSend);
                }
            } else {
                // check if this client has ever got any message
                if (!this.pendingBytesToSend.containsKey(clientUniqueId)) {
                    // if not, create a list for client in pendingBytesToSend Map
                    this.pendingBytesToSend.put(clientUniqueId, new LinkedList<byte[]>());
                }
                this.logger.willSendMessage(clientUniqueId);
                // add bytesToSend array to pendingBytesToSend
                this.pendingBytesToSend.get(clientUniqueId).add(bytesToSend);
            }
        }
        this.logger.willCallWakeUp();
        // wake up selector if it was blocked at selector.select()
        this.selector.wakeup();
    }

    /**
     * will return true if client is connected and available for reading and writing
     *
     * @return boolean
     */
    @Override
    public boolean isConnected(String clientId) {
        return this.clientStateMap.containsKey(clientId);
    }

    /**
     * this will return this client's state, either 0 or a SelectionKey value like SelectionKey.OP_READ
     *
     * @return SelectionKey's OP or 0 for not connected
     */
    @Override
    public int getCurrentState(String clientId) {
        return !this.clientStateMap.containsKey(clientId) ? 0 : this.clientStateMap.get(clientId);
    }

    @Override
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public void addMessageListener(MessageListener listener) {
        this.messageListeners.add(listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        this.connectionListeners.add(listener);
    }

    @Override
    public void addNIOStateListener(NIOStateListener listener) {
        this.nioStateListeners.add(listener);
    }

    /*
     * above are the NIOService interface methods
     */

    /*
     * below are the NIOServer internal methods
     */

    /**
     * this method will handle the SelectionKey.OP_ACCEPT
     *
     * @param key selection key, a ServerSocketChannel expected
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException {
        this.logger.acceptCalled();
        // accept only occurs for server sockets, it is safe to cast SelectableChannel to ServerSocketChannel
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // accept the connection request and set it as non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        // create a unique UUID for this socket channel
        String uuid = UUID.randomUUID().toString();
        while (uuidSocketChannelMap.keySet().contains(uuid)) {
            uuid = UUID.randomUUID().toString();
        }
        // add this socket channel to map with its UUID
        uuidSocketChannelMap.put(uuid, socketChannel);
        this.logger.acceptClientId(uuid);

        // it is assumed that the connected client would want to write to channel
        // so we register selector to OP_READ
        socketChannel.register(this.selector, SelectionKey.OP_READ);
        this.logger.registeredToRead(uuid);
        for (ConnectionListener connectionListener : connectionListeners) {
            this.logger.willNotifyListenerWithClientConnected(connectionListener.toString(), uuid);
            connectionListener.connected(uuid);
        }
        //set current state for this client
        this.clientStateMap.put(uuid, SelectionKey.OP_READ);
    }

    /**
     * this method will handle the SelectionKey.OP_READ
     *
     * @param key selection key, a SocketChannel expected
     * @throws IOException
     */
    private void read(SelectionKey key) throws IOException {
        this.logger.readCalled();
        // accepts SocketChannel since this method handles the SelectionKey.OP_READ
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // we will read a new message, so clear the byte buffer
        this.byteBuffer.clear();

        // although we should read the channel in a "while (this.byteBuffer.hasRemaining())" loop since there may be more bytes in the channel

        // number of read bytes
        // this will read the bytes to buffer
        int numberOfBytes = socketChannel.read(this.byteBuffer);
        this.logger.numberOfBytesRead(numberOfBytes);

        // zero bytes indicates an empty read
        if (numberOfBytes == 0) {
            // empty read? currently doing nothing
            this.logger.emptyRead();
        } else if (numberOfBytes < 0) {
            this.logger.clientClosedConnection();

            // client cleanly closed the connection at its side
            // will cancel the key and close the channel (these two are different things)
            key.cancel();
            socketChannel.close();
            String clientUniqueId = null;
            for (Map.Entry<String, SocketChannel> entry : this.uuidSocketChannelMap.entrySet()) {
                if (socketChannel.equals(entry.getValue())) {
                    clientUniqueId = entry.getKey();
                }
            }
            if (clientUniqueId != null) {
                if (this.pendingBytesToSend.containsKey(clientUniqueId)) {
                    List<byte[]> messages = this.pendingBytesToSend.remove(clientUniqueId);
                    this.logger.numberOfMessagesDiscardedForClient(clientUniqueId, messages.size());
                }
                if (this.uuidSocketChannelMap.containsKey(clientUniqueId)) {
                    /*SocketChannel removedSocketChannel = */
                    this.uuidSocketChannelMap.remove(clientUniqueId);
                    this.logger.clientSocketChannelRemoved(clientUniqueId);
                }
            }
            for (ConnectionListener connectionListener : connectionListeners) {
                this.logger.willNotifyListenerWithClientDisconnected(connectionListener.toString(), clientUniqueId);
                connectionListener.disconnected(clientUniqueId);
            }
            // remove client from map
            this.clientStateMap.remove(clientUniqueId);

        } else {
            this.logger.clientSentMessage();

            // bytes to be send to client
            byte[] clientSentBytes;

            // all available data from client read into byteBuffer, so prepare and set bytes to clientSentBytes
            if (this.byteBuffer.position() < this.byteBuffer.limit()) {
                // bytes are in reverse order (check JSR-51 / ReadableByteChannel::read() method),
                // flip the buffer first
                byteBuffer.flip();

                // copy available bytes to clientSentBytes from current byte buffer
                clientSentBytes = new byte[numberOfBytes];
                System.arraycopy(byteBuffer.array(), 0, clientSentBytes, 0, numberOfBytes);
                this.logger.singleBufferedBytesReadFromChannel(numberOfBytes);
            }

            // there are bytes left in the channel and we should read the rest of it
            else {
                this.logger.willReadAllBytes(BUFFER_SIZE);
                // this will collect all the bytes in channel
                List<Byte> byteCollection = new ArrayList<>();

                // bytes are in reverse order (check JSR-51 / ReadableByteChannel::read() method),
                // flip the buffer first
                byteBuffer.flip();

                // first add already read bytes to collection
                for (byte b : byteBuffer.array()) {
                    byteCollection.add(b);
                }

                // loop until there is no available byte in the channel
                while (true) {
                    // clear byte buffer for this loop
                    byteBuffer.clear();
                    // read remaining bytes to byte buffer
                    numberOfBytes = socketChannel.read(this.byteBuffer);
                    // check if there is any number of bytes read
                    if (numberOfBytes > 0) {
                        // bytes are in reverse order (check JSR-51 / ReadableByteChannel::read() method),
                        // flip the buffer first
                        byteBuffer.flip();
                        // add read bytes to collection
                        byte[] readBytes = new byte[numberOfBytes];
                        System.arraycopy(byteBuffer.array(), 0, readBytes, 0, numberOfBytes);
                        for (byte b : readBytes) {
                            byteCollection.add(b);
                        }
                    } else {
                        // got all the bytes in the channel
                        this.logger.multipleBufferedBytesReadFromChannel(byteCollection.size());
                        break;
                    }
                }

                // copy Byte from collection to clientSentBytes array
                clientSentBytes = new byte[byteCollection.size()];
                for (int i = 0; i < byteCollection.size(); i++) {
                    clientSentBytes[i] = byteCollection.get(i);
                }
            }
            this.logger.readAllClientSentBytes(clientSentBytes.length);

            // send bytes received/collected from client to listener
            // call receive without the client/owner unique id, for those who are interested only in listening
            this.logger.willNotifyListenerWithoutClientId(messageListeners.toString());
            for (MessageListener messageListener : messageListeners) {
                messageListener.receive(clientSentBytes);
            }


            // find client's unique id
            String uuid = null;
            for (Map.Entry<String, SocketChannel> entry : this.uuidSocketChannelMap.entrySet()) {
                if (socketChannel.equals(entry.getValue())) {
                    uuid = entry.getKey();
                }
            }

            // call receive if the client/owner of this message is available,
            // this method is here for those who are interested in replying back to this client
            // (although uuid should never be null)
            if (uuid != null) {
                for (MessageListener messageListener : messageListeners) {
                    this.logger.willNotifyListenerWithClientId(messageListener.toString(), uuid);
                    messageListener.receive(clientSentBytes, uuid);
                }
            }

        }
    }

    /**
     * this method will handle the SelectionKey.OP_WRITE
     *
     * @param key selection key, a SocketChannel expected
     * @throws IOException
     */
    private void write(SelectionKey key) throws IOException {
        this.logger.writeCalled();
        // accepts SocketChannel since this method handles the SelectionKey.OP_WRITE
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingBytesToSend) {
            // client unique id set at connection ACCEPT
            String clientUniqueId = null;
            // find current SocketChannel's uniqueId
            for (Map.Entry<String, SocketChannel> entry : uuidSocketChannelMap.entrySet()) {
                if (socketChannel.equals(entry.getValue())) {
                    clientUniqueId = entry.getKey();
                    break;
                }
            }
            // there should be a uniqueId for this SocketChannel,
            // otherwise it is removed somehow and someone still trying to send messages with to this client
            if (clientUniqueId != null) {
                List<byte[]> bytesToSendToClientList = this.pendingBytesToSend.get(clientUniqueId);
                if (bytesToSendToClientList.size() > 0) {
                    Iterator<byte[]> iterator = bytesToSendToClientList.iterator();
                    while (iterator.hasNext()) {
                        byte[] bytesToSendToClient = iterator.next();
                        this.logger.willWriteMessageToClient(clientUniqueId, bytesToSendToClient.length);
                        socketChannel.write(ByteBuffer.wrap(bytesToSendToClient));
                        iterator.remove();
                    }
                }
                // all messages written to client, register to READ
                key.interestOps(SelectionKey.OP_READ);
                //set current state for this client
                this.clientStateMap.put(clientUniqueId, SelectionKey.OP_READ);
            } else {
                // there is no client for this socket channel
                // between send method and write method a lot has happened,
                // throwing an exception here may not be the best idea,
                // may be there should be a method at listener's side to be notified?
                this.logger.noClientForSocket();
            }

        }
    }

    /*
     * above are the NIOServer internal methods
     */

    /*
     * NIO NIOServiceLifecycle methods below
     */

    @Override
    public void onCreate() {
        // notify all state listeners that NIOServer created
        for (NIOStateListener nioStateListener : this.nioStateListeners) {
            nioStateListener.created();
        }
    }

    @Override
    public void preStart() {
        this.logger.nioServerWillStart(hostname, port, messageListeners.toString(), connectionListeners.toString(), nioStateListeners.toString());
        // notify state listeners that NIO started
        for (NIOStateListener nioStateListener : this.nioStateListeners) {
            nioStateListener.willStart();
        }
    }

    @Override
    public void postStart() {
        this.logger.nioServerStarted(hostname, port, messageListeners.toString(), connectionListeners.toString(), nioStateListeners.toString());
        // notify state listeners that NIO started
        for (NIOStateListener nioStateListener : this.nioStateListeners) {
            nioStateListener.started();
        }
    }

    @Override
    public void preStop() {
        this.logger.willStop();
        // notify state listeners that NIO will stop
        for (NIOStateListener nioStateListener : this.nioStateListeners) {
            nioStateListener.willStop();
        }
    }

    @Override
    public void postStop() {
        this.logger.stopped();
        // notify state listeners that NIO stopped
        for (NIOStateListener nioStateListener : this.nioStateListeners) {
            nioStateListener.stopped();
        }
    }

    /*
     * NIO NIOServiceLifecycle methods above
     */

}
