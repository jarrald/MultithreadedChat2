package kea.dat18c.multithreadedchat.client;

import java.io.BufferedReader;
import java.io.InputStream;
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
                while (true) {
                    try {
                        String response = reader.readLine();
                        if(response.equals(ChatClient.SERVER_DISCONNECTED)||response.isEmpty())
                        {
                            socket.close();
                            client.setDisconnected(true);
                            break;
                        }
                        else if(response.equals(ChatClient.SERVER_QUIT_REPLY)) {
                            socket.close();
                            break;
                        }
                        System.out.println("\n" + response);

                        // prints the username after displaying the server's message
                        if (client.getUserName() != null) {
                            System.out.print("[" + client.getUserName() + "]: ");
                        }
                    }
                    catch (Exception ex) {
                        System.out.println("Error reading from server: " + ex.getMessage());
                        //ex.printStackTrace();
                        break;
                    }
                }
    }
}