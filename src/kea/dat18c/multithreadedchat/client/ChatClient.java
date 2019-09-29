package kea.dat18c.multithreadedchat.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatClient {
    private String hostname;
    private int port;
    private String userName;

    public static final String clientQuit = "QUIT";
    public static final String clientOk = "J_OK";

    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void execute() {
        try {
            Socket socket = new Socket(hostname, port);

            System.out.println("Connected to the chat server");
            try {
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                String connected = reader.readLine();
                System.out.println(connected);
                if(connected.equals(ChatClient.clientOk)){
                    new ReadThread(socket, this, input, reader).start();
                    new WriteThread(socket, this, writer).start();
                }
            } catch (IOException ex) {
            System.out.println("Error getting stream: " + ex.getMessage());
            ex.printStackTrace();
            }



        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }

    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }


    public static void main(String[] args) throws IOException {
        System.out.println("Protocol between Chat server and client:\n" +
                "List of allowed messages (and their meaning):\n\n" +

                "JOIN <<user_name>>, <<server_ip>>:<<server_port>>\n" +
                "From client to server.\n" +
                "The user name is given by the user. Username is max 12 chars long, only\n" +
                "letters, digits, ‘-‘ and ‘_’ allowed.\n\n" +

                "J_OK\n" +
                "From server to client.\n" +
                "Client is accepted.\n\n" +

                "J_ER <<err_code>>: <<err_msg>>\n" +
                "From server to client.\n" +
                "Client not accepted. Duplicate username, unknown command, bad command or\n" +
                "any other errors.\n\n" +

                "DATA <<user_name>>: <<free text…>>\n" +
                "From client to server.\n" +
                "From server to all clients.\n" +
                "First part of message indicates from which user it is, the colon(:)\n" +
                "indicates where the user message begins. Max 250 user characters.\n\n" +

                "IMAV\n" +
                "From client to server.\n" +
                "Client sends this heartbeat alive every 1 minute.\n\n" +
                "QUIT\n" +
                "From client to server.\n" +
                "Client is closing down and leaving the group.\n\n" +

                "LIST <<name1 name2 name3 …>>\n" +
                "From server to client.\n" +
                "A list of all active user names is sent to all clients, each time the\n" +
                "list at the server changes.\n\n" +

                "Note:\n" +
                "This notation <<info>> indicates a placeholder, and they need to be\n" +
                "replaced with appropriate content.\n" +
                "E.g.:\n" +
                "JOIN <<user_name>>, <<server_ip>>:<<server_port>>\n" +
                "Might look like this:\n" +
                "JOIN alice_92, 172.168.168.12:4578");
        boolean correctSyntax = false;
        do {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String command = bufferedReader.readLine();

            //Checks if the syntax input by the user is correct
            if (command.matches("^JOIN [\\w\\d-_]{1,12}, (([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+")) {
                //Splits the command into string array with , splitting the 2
                //Takes first element of array with [0] and splits it again with join (Effectively removing "JOIN "
                String hostname = (command.split(","))[0].split("JOIN ")[0];
                //Parses port number
                int port = Integer.parseInt(command.split(":")[1]);
                correctSyntax = true;
                ChatClient client = new ChatClient(hostname, port);
                client.execute();
            }
            else {
                System.out.println("Wrong syntax please try again\n");
            }
        }while (!correctSyntax);
    }

}