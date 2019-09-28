package kea.dat18c.multithreadedchat.client;

import kea.dat18c.multithreadedchat.server.ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReadThread extends Thread {
    private BufferedReader reader;
    private Socket socket;
    private ChatClient client;
    private InputStream input;

    public ReadThread(Socket socket, ChatClient client, InputStream input, BufferedReader reader) {
        this.socket = socket;
        this.client = client;
        this.input = input;
        this.reader = reader;

    }

    public void run() {
        try {
            String connected = reader.readLine();

            if(connected.equals(ChatServer.serverOk)){
                while (true) {
                    try {
                        String response = reader.readLine();
                        System.out.println("\n" + response);

                        // prints the username after displaying the server's message
                        if (client.getUserName() != null) {
                            System.out.print("[" + client.getUserName() + "]: ");
                        }
                    } catch (IOException ex) {
                        System.out.println("Error reading from server: " + ex.getMessage());
                        ex.printStackTrace();
                        break;
                    }
                }
            }
            else{
                System.out.println(connected);
                System.exit(1);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}