package kea.dat18c.multithreadedchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private int port;
    private int capacity;
    private Set<String> userNames = Collections.synchronizedSet( new HashSet<>());
    private Set<UserThread> userThreads = Collections.synchronizedSet( new HashSet<>());

    //Server responses, commands and default values below

    public static final String serverOk = "J_OK";
    public static final String serverFull = "J_ER 1: Server is at max capacity.";
    public static final String serverUserExists = "J_ER 2: Username taken.";
    public static final String serverDisconnected = "J_ER 3: Disconnected due to inactivity";
    public static final String serverQuitReply = "Quit successfully";
    public static final String serverQuit = "QUIT";
    public static final String serverList = "LIST";
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
            (new Thread(new keepClientsAlive(TimeUnit.SECONDS, 10, this))).start();
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
    public Set<UserThread> getUserThreads(){
        return this.userThreads;
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
     * When a client is disconnected, removes the associated username and UserThread
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
    private class keepClientsAlive implements Runnable{
        TimeUnit timeUnit;
        long keepAlive;
        ChatServer server;
        public keepClientsAlive(TimeUnit timeUnit, long keepAlive, ChatServer server){
            this.timeUnit = timeUnit;
            this.keepAlive = keepAlive;
            this.server = server;
        }
        @Override
        public void run() {
            try {
                while (true){
                    synchronized (this){
                    for(UserThread userThread : server.userThreads){
                        LocalTime timeNow = LocalTime.now();
                        if(userThread.getLastAlive().isBefore(timeNow.minus(this.keepAlive, this.timeUnit.toChronoUnit()))){

                            userThread.killThread();
                            //System.out.println("User: "+userThread.getUsername()+" was disconnected due to inactivity");
                            //server.removeUser(userThread.getUsername(),userThread);
                        }
                    }
                    }
                    timeUnit.sleep(keepAlive);
                }
            } catch (InterruptedException e) {
                System.out.println("Server heartbeat thread stopped, currently no limit for connection time\n"
                +"Please reset the server to fix it.");
                e.printStackTrace();
            }
        }
    }
}