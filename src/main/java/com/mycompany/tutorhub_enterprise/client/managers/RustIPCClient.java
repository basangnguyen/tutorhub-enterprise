package com.mycompany.tutorhub_enterprise.client.managers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class RustIPCClient {
    private FileChannel channel;
    private String pipeName;
    private String sessionId;
    
    private Thread listenerThread;
    private volatile boolean isRunning = false;
    
    private Consumer<String> alertListener;
    private Consumer<String> genericResponseListener;
    private Runnable disconnectListener;

    public void setAlertListener(Consumer<String> listener) {
        this.alertListener = listener;
    }

    public void setGenericResponseListener(Consumer<String> listener) {
        this.genericResponseListener = listener;
    }
    
    public void setDisconnectListener(Runnable listener) {
        this.disconnectListener = listener;
    }

    public boolean connect(String sessionId) {
        this.sessionId = sessionId;
        this.pipeName = "\\\\.\\pipe\\TutorHubExam_" + sessionId;
        try {
            Path pipePath = Paths.get(pipeName);
            this.channel = FileChannel.open(pipePath, StandardOpenOption.READ, StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to pipe: " + e.getMessage());
            return false;
        }
    }
    
    public synchronized boolean reconnect() {
        if (pipeName == null) return false;
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            // Ignore close error during reconnect
        }
        
        try {
            Path pipePath = Paths.get(pipeName);
            this.channel = FileChannel.open(pipePath, StandardOpenOption.READ, StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            System.err.println("Reconnect failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized void sendCommand(String command) throws IOException {
        if (channel != null) {
            System.out.println("Sending raw command: " + command);
            String payload = command.endsWith("\n") ? command : command + "\n";
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            System.out.println("Command bytes length: " + bytes.length);
            
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int bytesWritten = channel.write(buffer);
            System.out.println("Command written bytes: " + bytesWritten);
        } else {
            throw new IOException("Pipe is not connected");
        }
    }

    public String readResponse() throws IOException {
        if (channel == null) {
            throw new IOException("Pipe is not connected");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ByteBuffer singleByte = ByteBuffer.allocate(1);
        int bytesRead;
        while ((bytesRead = channel.read(singleByte)) != -1) {
            if (bytesRead > 0) {
                singleByte.flip();
                byte b = singleByte.get();
                singleByte.clear();
                
                if (b == '\n') {
                    break;
                }
                if (b != '\r') {
                    buffer.write(b);
                }
            }
        }
        if (buffer.size() == 0 && bytesRead == -1) {
            return null; // EOF indicating disconnect
        }
        String response = buffer.toString("UTF-8");
        System.out.println("Read response: " + response);
        return response;
    }

    public void sendLockCommand() throws IOException {
        sendCommand("LOCK");
    }

    public void sendPing() throws IOException {
        System.out.println("Sending PING");
        sendCommand("PING");
    }

    public void sendUnlockCommand() throws IOException {
        sendCommand("UNLOCK");
    }

    public void startListener() {
        if (channel == null || isRunning) return;
        isRunning = true;
        listenerThread = new Thread(() -> {
            while (isRunning) {
                try {
                    String response = readResponse();
                    if (response == null) {
                        // EOF - pipe disconnected
                        if (isRunning && disconnectListener != null) {
                            disconnectListener.run();
                        }
                        break; 
                    }
                    
                    if (response.startsWith("PROCESS_ALERT:")) {
                        String processName = response.substring("PROCESS_ALERT:".length()).trim();
                        if (alertListener != null) {
                            alertListener.accept(processName);
                        }
                    } else {
                        if (response.trim().equals("PONG")) {
                            System.out.println("Received PONG");
                        } else if (response.trim().equals("UNLOCKED")) {
                            System.out.println("Received UNLOCK response: UNLOCKED");
                        }
                        if (genericResponseListener != null) {
                            genericResponseListener.accept(response.trim());
                        }
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error reading from pipe: " + e.getMessage());
                        if (disconnectListener != null) {
                            disconnectListener.run();
                        }
                    }
                    break;
                }
            }
            isRunning = false;
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void close() {
        isRunning = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel = null;
        }
    }
}
