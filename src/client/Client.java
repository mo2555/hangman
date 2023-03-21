package client;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Client {

    private Socket socket;

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;
    private String clientName;
    private String clientPassword;

    public Client(Socket socket, String clientUserName, String clientName, String clientPassword) {
        try {
            this.socket = socket;
            this.clientName = clientName;
            this.clientPassword = clientPassword;
            this.clientUserName = clientUserName;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (Exception e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
            e.printStackTrace();
        }

    }


    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (Exception e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
            e.printStackTrace();
        }
    }

    public void listenForMessage() {

        new Thread(new Runnable() {


            @Override
            public void run() {
                String messageFromServer;

                try {
                    while (socket.isConnected()) {
                        messageFromServer = bufferedReader.readLine();
                        if (messageFromServer != null && messageFromServer.compareTo("null") != 0) {
                            String[] data = messageFromServer.split(",");
                            if (clientUserName.compareTo(data[1]) == 0 || data[1].compareTo("toAll") == 0) {

                                switch (data[0]) {
                                    case "singleOrMulti": {
                                        System.out.println(data[2]);
                                        int number = scanner.nextInt();
                                        sendMessage("singleOrMulti," + clientUserName + "," + number);
                                        break;
                                    }
                                    case "startGame":
                                    case "loseGame":
                                    case "winGame":
                                    case "gamePrint": {
                                        System.out.println(data[2]);
                                        break;
                                    }

                                    case "gameInput": {
                                        System.out.println(data[2]);
                                        String word = scanner.next();
                                        sendMessage("gameInput," + clientUserName + "," + word);
                                        break;
                                    }
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    closeEveryThing(socket, bufferedReader, bufferedWriter);
                }
            }
        }).start();

    }


    public void closeEveryThing(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {

            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public static Scanner scanner = new Scanner(System.in);
    public static String clientUsernameInfo;
    public static String clientNameInfo;
    public static String clientPasswordInfo;
    public static int code;

    public static void main(String[] args) {

        execute();

    }

    public static void execute() {
        try {
            Boolean authDone = false;
            System.out.println("Hello , welcome to the game");
            System.out.println("Enter the number of option : ");
            System.out.println(" 1) login ");
            System.out.println(" 2) register ");

            switch (scanner.nextInt()) {
                case 1: {
                    authDone = login();
                    break;
                }
                case 2: {
                    authDone = register();
                    break;
                }
            }

            if (authDone) {
                Socket socket = new Socket("localhost", 2001);
                Client client = new Client(socket, clientUsernameInfo, clientNameInfo, clientPasswordInfo);
                client.listenForMessage();
                client.sendMessage(clientUsernameInfo);
            } else {

                System.out.println(code);

            }

        } catch (Exception e) {

        }
    }

    public static Boolean login() {
        String username = null;
        String password = null;

        takeAuthInfo(true);

        try {
            Scanner loginInfoScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\login_info.txt"));
            while (loginInfoScanner.hasNext()) {
                String line = loginInfoScanner.nextLine();
                String[] loginData = line.split(",");

                if (loginData[0].compareTo(clientUsernameInfo) == 0) {
                    username = loginData[0];
                    password = loginData[1];
                    break;
                }

            }

            if (username == null) {
                code = 404;
                return false;
            }
            if (password.compareTo(clientPasswordInfo) != 0) {
                code = 401;
                return false;
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            code = 400;
            return false;
        }

    }

    public static Boolean register() {

        takeAuthInfo(false);

        try {
            Scanner loginInfoScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\login_info.txt"));

            while (loginInfoScanner.hasNext()) {
                String line = loginInfoScanner.nextLine();
                String[] loginData = line.split(",");

                if (loginData[0].compareTo(clientUsernameInfo) == 0) {
                    code = 440;
                    return false;
                }

            }

            OutputStream outputStream = new FileOutputStream("D:\\Intellij Projects\\hangman_project\\src\\database\\login_info.txt",true);
String data =clientUsernameInfo + "," + clientPasswordInfo + "," + clientNameInfo;
            outputStream.write(data.getBytes(), 0, data.length());
            outputStream.flush();
            outputStream.close();

            return true;

        } catch (Exception e) {
            code = 400;
            return false;
        }
    }

    public static void takeAuthInfo(Boolean isLogin) {

        if (!isLogin) {
            System.out.println("Enter your name : ");
            clientNameInfo = scanner.next();

        }

        System.out.println("Enter your username : ");
        clientUsernameInfo = scanner.next();

        System.out.println("Enter your password : ");
        clientPasswordInfo = scanner.next();


    }

}
