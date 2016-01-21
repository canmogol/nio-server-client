package com.fererlab.nio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class BlockingClient {

    private String hostname = "localhost";
    private int port = 8992;

    public static void main(String[] args) {
        new BlockingClient();
    }

    public BlockingClient() {
        for (int i = 0; i < 10; i++) {
            final int currentClientIndex = i;
            new Thread() {
                @Override
                public void run() {
                    doRequest(currentClientIndex);
                }
            }.start();
        }
    }

    private void doRequest(int currentClientIndex) {
        try {
            Socket socket = new Socket(InetAddress.getByName(hostname), port); // open connection
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            osw.write("this is client " + currentClientIndex);
            osw.flush();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
            InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream);
            StringBuilder stringBuilder = new StringBuilder();
            int read;
            while ((read = inputStreamReader.read()) != 13) {
                char readChar = (char) read;
                if (read == -1) {
                    break;
                }
                stringBuilder.append(readChar);
            }
            System.out.println(currentClientIndex + ": response: " + stringBuilder.toString().trim());
            socket.close();
        } catch (Exception e) {
            System.out.println("----- [" + currentClientIndex + "] exception e: " + e.getMessage());
        }
    }
}
