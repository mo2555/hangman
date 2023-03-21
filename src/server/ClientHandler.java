package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    private Socket socket;

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;

    private ArrayList<String> phrases;

    public ClientHandler(Socket socket, ArrayList<String> phrases) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.phrases = phrases;
            this.clientUserName = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMessages("singleOrMulti," + clientUserName + ",Enter the number of option : 1) play as a single player 2) play with multiplayers");

        } catch (IOException e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        String messageFromClient;


        try {
            messageFromClient = bufferedReader.readLine();
            if (messageFromClient != null && messageFromClient.compareTo("null") != 0) {
                System.out.println(messageFromClient);
                handleMessageFromClient(messageFromClient);
            }
            //broadcastMessages(messageFromClient);

        } catch (IOException e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
        }


    }

    public void broadcastMessages(String message) {

        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write(message);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();

            } catch (IOException e) {
                closeEveryThing(socket, bufferedReader, bufferedWriter);
                e.printStackTrace();
            }

        }

    }

    public String handleMessageFromClient(String message) {
        System.out.println(message);
        String[] data = message.split(",");
        switch (data[0]) {

            case "singleOrMulti": {

                if (data[2].compareTo("1") == 0) {
                    playAsSinglePlayer(data[1]);
                } else {
                    playWithMultiPlayers(data[1]);
                }

                break;
            }
            case "gameInput": {

                return data[2];
            }

        }

        return "";

    }

    public void playAsSinglePlayer(String username) {

        String word;

        try {
            Random rand = new Random();
            word = phrases.get(rand.nextInt(phrases.size()));
            List<Character> playerGuesses = new ArrayList<>();

            Integer wrongCount = 0;

            while (true) {
                if (wrongCount >= 6) {
                    broadcastMessages("loseGame," + username + ",You lose!");
                    broadcastMessages("loseGame," + username + ",The word are : " + word);
                    break;
                }
                if (printWordState(word, playerGuesses)) {
                    broadcastMessages("winGame," + username + ",You win!");
                    break;
                }
                if (!getPlayerGuess(word, playerGuesses)) {
                    wrongCount++;
                    broadcastMessages("gamePrint," + username + ",Letter miss");
                    broadcastMessages("gamePrint," + username + ",try again");
                    broadcastMessages("gamePrint," + username + ",Misses : " + wrongCount);
                }



            }
            //broadcastMessages(messageFromClient);

        } catch (Exception e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
            e.printStackTrace();
        }


    }

    private boolean getPlayerGuess(String word, List<Character> playerGuesses) {

        String letterGuess = null;
        try {
            String message = bufferedReader.readLine();
            if (message != null && message.compareTo("null") != 0) {
                letterGuess = handleMessageFromClient(message);
            }
        } catch (IOException e) {
        }
        playerGuesses.add(letterGuess.charAt(0));

        return word.contains(letterGuess);
    }

    private boolean printWordState(String word, List<Character> playerGuesses) {
        String wordToSend = "";
        int dLength =0;
        int correctCount = 0;
        for (int i = 0; i < word.length(); i++) {
            if (playerGuesses.contains(word.charAt(i))) {
                wordToSend += word.charAt(i);
                correctCount++;
            } else if (word.charAt(i) == ' ') {
                wordToSend += " ";
                dLength++;
            } else {
                wordToSend += "_";
            }
        }


        if(!(word.length()-dLength == correctCount)){
            broadcastMessages("gamePrint," + clientUserName + "," + wordToSend);
            broadcastMessages("gameInput," + clientUserName + ",Please enter a letter:");
        }
        return (word.length()-dLength == correctCount);
    }

    public void playWithMultiPlayers(String username) {

    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessages("SERVER : " + clientUserName + " has left the game");
    }

    public void closeEveryThing(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
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

}
