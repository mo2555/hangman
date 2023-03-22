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
    private ArrayList<String> teams;
    private String myTeam;

    public ClientHandler(Socket socket, ArrayList<String> phrases, ArrayList<String> teams) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.phrases = phrases;
            this.teams = teams;
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

        if (data[2].compareTo("-") == 0) {
            removeClientHandler();
            return "";
        }

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
        int dLength = 0;
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


        if (!(word.length() - dLength == correctCount)) {
            broadcastMessages("gamePrint," + clientUserName + "," + wordToSend);
            broadcastMessages("gameInput," + clientUserName + ",Please enter a letter:");
        }
        return (word.length() - dLength == correctCount);
    }

    public void playWithMultiPlayers(String username) {
        try {
            broadcastMessages("gamePrint," + username + ",Enter the number of option : ");
            broadcastMessages("gamePrint," + username + ",1) Create team");
            broadcastMessages("gameInput," + username + ",2) Join team");

            String number = handleMessageFromClient(bufferedReader.readLine());
            System.out.println(number.compareTo("1") == 0);
            if (number.compareTo("1") == 0) {
                createTeam();
            } else {
                joinTeam();
            }

        } catch (IOException e) {
        }

    }

    public void createTeam() throws IOException {

        while (true) {
            broadcastMessages("gameInput," + clientUserName + ",Enter the team name : ");
            String teamName = handleMessageFromClient(bufferedReader.readLine());
            if (checkTeamName(teamName)) {
                broadcastMessages("gamePrint," + clientUserName + ",Team name must be unique");
                broadcastMessages("gamePrint," + clientUserName + ",Try again");
            } else {
                OutputStream outputStream = new FileOutputStream("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt", true);
                String data = teamName + "," + clientUserName + "\n";
                outputStream.write(data.getBytes(), 0, data.length());
                outputStream.flush();
                outputStream.close();
                teams.add(teamName);
                myTeam = teamName + "," + clientUserName;
                broadcastMessages("gamePrint," + clientUserName + ",Team created successfully");
                broadcastMessages("gamePrint," + clientUserName + ",Waiting for players for joining the team...");
                broadcastMessages("gamePrint," + clientUserName + ",Note : ");
                broadcastMessages("gamePrint," + clientUserName + ",Minimum number of players per team are 2 ");
                broadcastMessages("gamePrint," + clientUserName + ",Maximum number of players per team are 5 ");
                break;
            }
        }

    }

    public void joinTeam() throws IOException {

        while (true) {
            broadcastMessages("gameInput," + clientUserName + ",Enter the team name : ");
            String teamName = handleMessageFromClient(bufferedReader.readLine());
            if (checkTeamName(teamName)) {
                String adminUserName = getAdminTeamUseName(teamName);
                myTeam = teamName;
                if (adminUserName.compareTo(clientUserName) == 0) {
                    broadcastMessages("gamePrint," + clientUserName + ",Joined successfully");
                    broadcastMessages("gamePrint," + clientUserName + ",Waiting for players for joining the team...");
                    broadcastMessages("gamePrint," + clientUserName + ",Note : ");
                    broadcastMessages("gamePrint," + clientUserName + ",Minimum number of players per team are 2 ");
                    broadcastMessages("gamePrint," + clientUserName + ",Maximum number of players per team are 5 ");
                } else {
                    broadcastMessages("gamePrint," + clientUserName + ",Joined successfully");
                    broadcastMessages("gamePrint," + clientUserName + ",Waiting for admin to start a game");
                    broadcastMessages("gamePrint," + adminUserName + "," + clientUserName + " Has joined the team");
                    updateTeamUsers();
                    //broadcastMessages("gameInput," + adminUserName + "," + clientUserName + " Has joined the team");
                }


                break;
            } else {
                broadcastMessages("gamePrint," + clientUserName + ",Team name must be unique");
                broadcastMessages("gamePrint," + clientUserName + ",Try again");
            }
        }
    }

    public boolean checkTeamName(String teamName) {
        Scanner phrasesScanner = null;
        teams = new ArrayList<>();
        try {
            phrasesScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (phrasesScanner.hasNext()) {
            teams.add(phrasesScanner.nextLine());
        }
        for (String team : teams) {
            if (team.split(",")[0].compareTo(teamName) == 0) {
                return true;
            }
        }
        return false;
    }

    public void updateTeamUsers() {

        Scanner phrasesScanner = null;
        ArrayList<String> oldTeam = new ArrayList<>();
        try {
            phrasesScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (phrasesScanner.hasNext()) {
            oldTeam.add(phrasesScanner.nextLine());
        }

        String p = "";
        String n = "";

        for (int index = 0; index < oldTeam.size(); index++) {
            if (oldTeam.get(index).split(",")[0].compareTo(myTeam) == 0) {
                p = oldTeam.get(index);
                n = p + "," + clientUserName;
            }
        }

        oldTeam.remove(p);
        oldTeam.add(n);

        String data = "";

        for (String team : oldTeam) {
            data += team + "\n";
        }


        try {
            BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(new PrintStream("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt")));

            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
        }

    }

    public String getAdminTeamUseName(String teamName) {
        Scanner phrasesScanner = null;
        teams = new ArrayList<>();
        try {
            phrasesScanner = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (phrasesScanner.hasNext()) {
            teams.add(phrasesScanner.nextLine());
        }
        for (String team : teams) {
            if (team.split(",")[0].compareTo(teamName) == 0) {
                return team.split(",")[1];
            }
        }
        return "";
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
