package kea.dat18c.multithreadedchat.server;

import java.io.*;
import java.net.Socket;
public class UserThread extends Thread {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private String errorMsg;
    private String username;
    private BufferedReader reader;

    public UserThread(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.errorMsg = ChatServer.serverOk;
    }
    public UserThread(Socket socket, ChatServer server, BufferedReader reader, String username) {
        this.socket = socket;
        this.server = server;
        this.errorMsg = ChatServer.serverOk;
        this.reader = reader;
        this.username = username;
    }
    public UserThread(Socket socket, ChatServer server, String errorMsg) {
        this.socket = socket;
        this.server = server;
        this.errorMsg = errorMsg;
    }

    public void run() {
        if(errorMsg.equals(ChatServer.serverOk)){
            try {


                OutputStream output = socket.getOutputStream();
                writer = new PrintWriter(output, true);
                sendMessage(ChatServer.serverOk);
                printUsers();

                String userName = reader.readLine();
                server.addUserName(userName);

                String serverMessage = "New user connected: " + userName;
                server.broadcast(serverMessage, this);

                String clientMessage;

                do {
                    clientMessage = reader.readLine();
                    serverMessage = "[" + userName + "]: " + clientMessage;
                    server.broadcast(serverMessage, this);

                } while (!clientMessage.equals(ChatServer.serverQuit));

                server.removeUser(userName, this);
                socket.close();

                serverMessage = userName + " has quitted.";
                server.broadcast(serverMessage, this);

            } catch (IOException ex) {
                System.out.println("Error in UserThread: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        else{
            OutputStream output = null;
            try {
                output = socket.getOutputStream();
                writer = new PrintWriter(output, true);
                sendMessage(errorMsg);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a list of online users to the newly connected user.
     */
    public void printUsers() {
        if (server.hasUsers()) {
            writer.println("Connected users: " + server.getUserNames());
        } else {
            writer.println("No other users connected");
        }
    }

    /**
     * Sends a message to the client.
     */
    public void sendMessage(String message) {
        writer.println(message);
    }
}