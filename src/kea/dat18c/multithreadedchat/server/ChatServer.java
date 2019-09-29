package kea.dat18c.multithreadedchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private int port;
    private int capacity;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();

    //Server responses, commands and default values below

    public static final String serverOk = "J_OK";
    public static final String serverFull = "Error: Server is at max capacity.";
    public static final String serverUserExists = "Error: username taken.";
    public static final String serverQuit = "QUIT";
    public static final String serverList = "LISTUSERS";
    public static final int defaultCapacity = 5;
    public static final int defaultPort = 6000;

    public ChatServer(int port, int capacity) {
        this.port = port;
        this.capacity = capacity;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Chat Server is listening on port " + port);
            ExecutorService executor = Executors.newFixedThreadPool(capacity);
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String userName = reader.readLine();
                if(userThreads.size()<capacity) {
                    if(!userNames.contains(userName)){
                        System.out.println("New user connected "+userName);
                        UserThread newUser = new UserThread(socket, this, reader, userName);
                        userThreads.add(newUser);
                        executor.execute(newUser);
                    }
                    else{
                        UserThread sendError = new UserThread(socket, this, serverUserExists);
                        (new Thread(sendError)).start();
                    }
                }
                else{
                    UserThread sendError = new UserThread(socket, this, serverFull);
                    (new Thread(sendError)).start();
                }
            }

        } catch (IOException ex) {
            System.out.println("Error in the server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void denyAccess(String errmsg){

    }

    public static void main(String[] args) {
        int port;
        if (args.length < 1) {
            port = defaultPort;
        }
        else {
            port = Integer.parseInt(args[0]);
        }
        ChatServer server = new ChatServer(port, ChatServer.defaultCapacity);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    public void broadcast(String message, UserThread excludeUser) {
        for (UserThread aUser : userThreads) {
            if (aUser != excludeUser) {
                aUser.sendMessage(message);
            }
        }
    }

    /**
     * Stores username of the newly connected client.
     */
    public void addUserName(String userName) {
        userNames.add(userName);
    }

    /**
     * When a client is disconneted, removes the associated username and UserThread
     */
    public void removeUser(String userName, UserThread aUser) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(aUser);
            System.out.println("The user " + userName + " quitted");
        }
    }

    public Set<String> getUserNames() {
        return this.userNames;
    }

    /**
     * Returns true if there are other users connected (not count the currently connected user)
     */
    public boolean hasUsers() {
        return !this.userNames.isEmpty();
    }
}