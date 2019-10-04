package kea.dat18c.multithreadedchat.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;

public class UserThread implements Runnable {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private String errorMsg;
    private boolean disconnected;

    public String getUsername() {
        return username;
    }

    private String username;
    private BufferedReader reader;
    private LocalTime lastAlive;

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

    public LocalTime getLastAlive() {
        return lastAlive;
    }

    public void run() {
        if(errorMsg.equals(ChatServer.serverOk)){
            try {


                OutputStream output = socket.getOutputStream();
                writer = new PrintWriter(output, true);
                sendMessage(ChatServer.serverOk);
                printUsers();

                //String userName = reader.readLine();
                server.addUserName(username);
                Thread.currentThread().setName("UserThread-"+username);
                this.lastAlive = LocalTime.now();
                this.disconnected = false;

                String serverMessage = "New user connected: " + username;
                server.broadcast(serverMessage, this);

                String clientCommand;

                do {
                    if(disconnected)
                        break;
                    clientCommand = reader.readLine();
                    if(clientCommand.startsWith("DATA "+username+": ")){
                        String message = (clientCommand.split(": ")[1]);
                        //Cuts message to 250 characters
                        if(message.length()>250) {
                            message = message.substring(0,249);
                        }
                        serverMessage = "[" + username + "]: " +message;
                        server.broadcast(serverMessage, this);
                    }
                    else if(clientCommand.equals(ChatServer.serverList)){
                        printUsers();
                    }
                    else if(clientCommand.equals("IMAV")){
                        resetLastAlive();
                    }

                    else if(clientCommand.strip().equals(ChatServer.serverQuit)){
                        writer.println(ChatServer.serverQuitReply);
                    }
                    else{
                        writer.println("J_ER 9: Unknown command");
                    }


                } while (!clientCommand.equals(ChatServer.serverQuit));

                server.removeUser(username, this);
                socket.close();

                serverMessage = username + " has quitted.";
                server.broadcast(serverMessage, this);

            }
            catch (SocketException ex){
                System.out.println(this.username+" disconnected.");
            }
            catch (IOException ex) {
                System.out.println("Error in "+Thread.currentThread().getName()+": " + ex.getMessage());
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
    public void killThread(){
        try{
            synchronized (server.getUserThreads()){
            sendMessage(ChatServer.serverDisconnected);
            this.disconnected = true;
            socket.close();
            server.removeUser(username, this);
            //To get out of the loop
            throw new IOException(username + " was disconnected cause of inactivity");
            }
        }catch (IOException e){
            System.out.println( e.getMessage());
        }
    }
    public void resetLastAlive(){
        this.lastAlive = LocalTime.now();
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