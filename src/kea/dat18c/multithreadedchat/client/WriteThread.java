package kea.dat18c.multithreadedchat.client;

import kea.dat18c.multithreadedchat.server.ChatServer;

import java.io.*;
import java.net.Socket;

public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatClient client;

    public WriteThread(Socket socket, ChatClient client) {
        this.socket = socket;
        this.client = client;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    public void run() {
        try {
            Console console = System.console();

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("\nEnter your name: ");
            //String userName = null;
            String userName = reader.readLine();

            client.setUserName(userName);
            writer.println(userName);

            String text;

            do {
                //System.out.print("["+userName+"]2: ");
                text = reader.readLine();
                writer.println(text);

            } while (!text.equals(ChatServer.serverQuit));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException ex) {

            System.out.println("Error writing to server: " + ex.getMessage());
        }
    }
}