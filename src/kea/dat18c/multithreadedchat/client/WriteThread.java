package kea.dat18c.multithreadedchat.client;

import java.io.*;
import java.net.Socket;

public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatClient client;

    public WriteThread(Socket socket, ChatClient client, PrintWriter writer) {
        this.socket = socket;
        this.client = client;
        this.writer = writer;
        /*
        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }*/
    }
    public void run() {
        try {
            //Console console = System.console();

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            //System.out.print("\nEnter your name: ");
            //String userName = null;
            //String userName = reader.readLine();

            //client.setUserName(userName);
            //writer.println(userName);

            String text;

            do {
                //System.out.print("["+userName+"]2: ");
                text = reader.readLine();
                if(!client.hasDisconnected()){
                    if(text.equals(ChatClient.clientQuit)) {
                        writer.println(text);
                        break;
                    }
                    else if(text.equals("LIST") || text.equals("IMAV"))
                        writer.println(text);
                    else{
                        String message = "DATA "+client.getUserName()+": "+text;
                        writer.println(message);
                    }
                }
                else{
                    System.out.println("Disconnected, shutting down");
                    break;
                }

            } while (!client.hasDisconnected() && !socket.isClosed());
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