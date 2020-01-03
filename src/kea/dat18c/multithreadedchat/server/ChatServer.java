package kea.dat18c.multithreadedchat.server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private int port;
    private int capacity;
    private Set<String> userNames = Collections.synchronizedSet( new HashSet<>());
    private Set<UserThread> userThreads = Collections.synchronizedSet( new HashSet<>());

    public Path getLogPath() {
        return logPath;
    }

    public void setLogPath(Path logPath) {
        this.logPath = logPath;
    }

    private Path logPath;
    

    //Server responses, commands and default values below

    public static final String SERVER_OK = "J_OK";
    public static final String SERVER_FULL = "J_ER 1: Server is at max capacity.";
    public static final String USERNAME_TAKEN = "J_ER 2: Username taken.";
    public static final String SERVER_DISCONNECTED = "J_ER 3: Disconnected due to inactivity";
    public static final String SERVER_QUIT_REPLY = "Quit successfully";
    public static final String SERVER_QUIT = "QUIT";
    public static final String SERVER_LIST = "LIST";
    public static final int DEFAULT_CAPACITY = 5;
    public static final int DEFAULT_PORT = 6000;

    public ChatServer(int port, int capacity, Path logPath) {
        this.port = port;
        this.capacity = capacity;
        this.logPath = logPath;
    }

    public void logString(String text) throws IOException {
        String logText = new Date().toLocaleString() +" "+ text;
        Files.write(logPath, Collections.singleton(logText), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            String startStatus =  new Date().toLocaleString() + " Chat Server is listening on port " + port;
            Files.write(logPath, Collections.singleton(startStatus), StandardCharsets.UTF_8);
            System.out.println(startStatus);

            ExecutorService executor = Executors.newFixedThreadPool(capacity);
            Thread heartBeat = new Thread(new ActivityMonitor(TimeUnit.SECONDS, 10, this));
            heartBeat.start();
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
                        UserThread sendError = new UserThread(socket, this, USERNAME_TAKEN);
                        (new Thread(sendError)).start();
                    }
                }
                else{
                    UserThread sendError = new UserThread(socket, this, SERVER_FULL);
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
            port = DEFAULT_PORT;
        }
        else {
            port = Integer.parseInt(args[0]);
        }
        //Creates directory if there isn't one for logs, won't overwrite.
        new File("logs").mkdirs();
        //Chooses a random filename with logFile_xxxx.txt where the x's are numbers, if it exists it tries again
        boolean availableFileName = false;
        String logName ="logs/"+ ThreadLocalRandom.current().nextInt(1,9999)+".txt";
        while(!availableFileName){
            if(Files.exists(Paths.get(logName)))
                logName ="logs/"+ ThreadLocalRandom.current().nextInt(1,9999)+".txt";
            else
                availableFileName = true;
        }

        System.out.println("Logging file to: "+logName);
        Path logPath = Paths.get(logName);
        ChatServer server = new ChatServer(port, ChatServer.DEFAULT_CAPACITY, logPath);
        server.execute();
    }

    /**
     * Delivers a message from one user to others (broadcasting)
     */
    public void broadcast(String message, UserThread excludeUser) {
        try {
            logString(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    private class ActivityMonitor implements Runnable{
        TimeUnit timeUnit;
        long keepAlive;
        ChatServer server;
        public ActivityMonitor(TimeUnit timeUnit, long keepAlive, ChatServer server){
            this.timeUnit = timeUnit;
            this.keepAlive = keepAlive;
            this.server = server;
        }
        @Override
        public void run() {
                while (true){
                    try {
                        List<UserThread> usersToKick = new ArrayList<>(5);
                        for(UserThread userThread : server.userThreads){
                            LocalTime timeNow = LocalTime.now();
                            if(userThread.getLastAlive().isBefore(timeNow.minus(this.keepAlive, this.timeUnit.toChronoUnit()))){
                                usersToKick.add(userThread);
                            }
                        }
                        for (UserThread userThread : usersToKick){
                            userThread.disconnect();
                            server.removeUser(userThread.getUsername(), userThread);
                            server.logString(userThread.getUsername() + " was disconnected cause of inactivity");
                        }
                        timeUnit.sleep(keepAlive);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
    }
}