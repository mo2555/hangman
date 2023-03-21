package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

    private ServerSocket serverSocket;
    private ArrayList<String> phrases = new ArrayList<>();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        Scanner phrasesScanner = null;
        try {
            phrasesScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\phrases.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (phrasesScanner.hasNext()) {
            phrases.add(phrasesScanner.nextLine());
        }
    }

    public void startServer() {

        try {



            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();

                System.out.println("A new client has connected");

                ClientHandler clientHandler = new ClientHandler(socket,phrases);

                Thread thread = new Thread(clientHandler);
                thread.start();

            }

        } catch (Exception e) {
            //closeServerSocket();
            e.printStackTrace();
        }

    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(2001);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
