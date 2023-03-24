package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public ArrayList<String> myTeamMembers = new ArrayList<>();
    public ArrayList<String> enemyTeamMembers = new ArrayList<>();
    public ArrayList<String> allTeamsMembers = new ArrayList<>();

    private Socket socket;

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;

    private ArrayList<String> phrases;
    private ArrayList<String> teams;
    private String myTeam;
    private String enemy;

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
            e.printStackTrace();
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
            case "gameInput":
            case "gameStartedInput": {
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
            e.printStackTrace();
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
                if (clientUserName.compareTo(getAdminTeamUseName(myTeam)) != 0) {
                    askForStartGame();
                    listenForMessage();
                }
            }
            if (clientUserName.compareTo(getAdminTeamUseName(myTeam)) == 0) {
                if (handleMessageFromClient(bufferedReader.readLine()).compareTo("1") == 0) {
                    System.out.println("yes");
                    startGame();
                } else {
                    System.out.println("not");
                    String adminUserName = getAdminTeamUseName(myTeam);
                    broadcastMessages("gamePrint," + adminUserName + ",Waiting players for joining the team...");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            if (checkTeamName(teamName) == true) {
                String adminUserName = getAdminTeamUseName(teamName);
                myTeam = teamName;
                if (adminUserName.compareTo(clientUserName) == 0) {
                    broadcastMessages("gamePrint," + clientUserName + ",Joined successfully");
                    broadcastMessages("gamePrint," + clientUserName + ",Waiting players for joining the team...");
                    broadcastMessages("gamePrint," + clientUserName + ",Note : ");
                    broadcastMessages("gamePrint," + clientUserName + ",Minimum number of players per team are 2 ");
                    broadcastMessages("gamePrint," + clientUserName + ",Maximum number of players per team are 5 ");
                } else {
                    broadcastMessages("gamePrint," + clientUserName + ",Joined successfully");
                    broadcastMessages("gamePrint," + clientUserName + ",Waiting for admin to start a game");
                    broadcastMessages("gamePrint," + adminUserName + "," + clientUserName + " Has joined the team");
                    updateTeamUsers();

                }


                break;
            } else if (checkTeamName(teamName) == null) {
                broadcastMessages("gamePrint," + clientUserName + ",Team completed");
                broadcastMessages("gamePrint," + clientUserName + ",Try again");
            } else {
                broadcastMessages("gamePrint," + clientUserName + ",No team have this name");
                broadcastMessages("gamePrint," + clientUserName + ",Try again");
            }
        }
    }

    public void askForStartGame() throws IOException {
        String adminUserName = getAdminTeamUseName(myTeam);
        broadcastMessages("gamePrint," + adminUserName + ",Enter the number of option : ");
        broadcastMessages("gamePrint," + adminUserName + ",1) Start game");
        broadcastMessages("gameInput," + adminUserName + ",2) Waiting another member to join");
//        String number = handleMessageFromClient(bufferedReader.readLine());
//        if (number.compareTo("1") == 0) {
//            return true;
//        } else {
//            return false;
//        }
    }

    public void startGame() {
        try {
            String team = null;
            Scanner teams = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\teams.txt"));
            while (teams.hasNext()) {
                team = teams.nextLine();
                if (team.split(",")[0].compareTo(myTeam) == 0) {
                    break;
                }
            }
            OutputStream outputStream = new FileOutputStream("D:\\Intellij Projects\\hangman_project\\src\\database\\score_history.txt", true);
            String data = "searchingForTeam" + "," + team + "\n";
            outputStream.write(data.getBytes(), 0, data.length());
            outputStream.flush();
            outputStream.close();
            broadcastMessages("gamePrint," + clientUserName + ",Waiting another team");
            searchingForTeam();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void searchingForTeam() throws Exception {
        String team = null;
        String enemyTeam = null;
        String myTeam = null;
        ArrayList<String> oldTeam = new ArrayList<>();
        Scanner teams = new Scanner(new File("D:\\Intellij Projects\\hangman_project\\src\\database\\score_history.txt"));
        while (teams.hasNext()) {
            team = teams.nextLine();
            if (team.split(",")[0].compareTo("searchingForTeam") == 0 && team.split(",")[1].compareTo(this.myTeam) != 0) {
                enemyTeam = team;
                enemy=team.split(",")[1];
            }
            if (team.split(",")[1].compareTo(this.myTeam) == 0) {
                myTeam = team;
                this.myTeam=team.split(",")[1];
            }
            oldTeam.add(team);
        }
        if (enemyTeam == null) {
            listenForMessage();
            return;
        }
        String pMyTeam = "";
        String nMyTeam = "";
        String pEnemyTeam = "";
        String nEnemyTeam = "";

        for (int index = 0; index < oldTeam.size(); index++) {
            if (oldTeam.get(index).compareTo(myTeam) == 0) {
                pMyTeam = oldTeam.get(index);
                nMyTeam = pMyTeam.replaceAll("searchingForTeam", "inGame");
            }
            if (oldTeam.get(index).compareTo(enemyTeam) == 0) {
                pEnemyTeam = oldTeam.get(index);
                nEnemyTeam = pEnemyTeam.replaceAll("searchingForTeam", "inGame");
            }
        }

        for (int index = 0; index < nMyTeam.split(",").length; index++) {
            if (index == 0 || index == 1) {
                continue;
            }
            myTeamMembers.add(nMyTeam.split(",")[index]);
            broadcastMessages("gamePrint," + nMyTeam.split(",")[index] + ",Game Started");

        }
        for (int index = 0; index < nEnemyTeam.split(",").length; index++) {
            if (index == 0 || index == 1) {
                continue;
            }
            myTeamMembers.add(nEnemyTeam.split(",")[index]);
            broadcastMessages("gamePrint," + nEnemyTeam.split(",")[index] + ",Game Started");
        }
        gameStarted();
        oldTeam.remove(pMyTeam);
        oldTeam.add(nMyTeam);
        oldTeam.remove(pEnemyTeam);
        oldTeam.add(nEnemyTeam);

        String data = "";

        for (String reNewTeam : oldTeam) {
            data += reNewTeam + "\n";
        }


        BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(new PrintStream("D:\\Intellij Projects\\hangman_project\\src\\database\\score_history.txt")));

        outputStream.write(data);
        outputStream.flush();
        outputStream.close();
    }

    public void listenForMessage() {

        new Thread(new Runnable() {


            @Override
            public void run() {
                String messageFromToServer;

                try {
                    while (socket.isConnected()) {
                        messageFromToServer = bufferedReader.readLine();
                        if (messageFromToServer != null && messageFromToServer.compareTo("null") != 0) {
                            String[] data = messageFromToServer.split(",");
                            if (clientUserName.compareTo(data[1]) != 0) {

                                switch (data[0]) {

                                    case "gameStartedInput": {
                                        System.out.println(data[2]);
                                        broadcastMessages("gameStartedInputFromClient," + data[1] + "," + data[2]);
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

    public void gameStarted() {
        allTeamsMembers = new ArrayList<>();
        for (String team : myTeamMembers) {
            allTeamsMembers.add(team);
        }
        for (String team : enemyTeamMembers) {
            allTeamsMembers.add(team);
        }
        String word;

        try {
            Random rand = new Random();
            word = phrases.get(rand.nextInt(phrases.size()));
            List<Character> playerGuesses = new ArrayList<>();

            Integer myTeamWrongCount = 0;
            Integer enemyTeamWrongCount = 0;
            loop:
            while (true) {
                boolean turn = true;
                loop2:
                for (int index = 0; index < allTeamsMembers.size(); index++) {

                    if (index == ((allTeamsMembers.size() / 2))) {
                        turn = false;
                    }
                    System.out.println("myTeamWrongCount===>" + myTeamWrongCount);
                    System.out.println("turn===>" + turn);
                    System.out.println("enemyTeamWrongCount===>" + enemyTeamWrongCount);
                    if (myTeamWrongCount >= 6) {
                        for (String team : myTeamMembers) {
                            broadcastMessages("loseGame," + team + "," + enemy + " won the game");
                            broadcastMessages("loseGame," + team + ",The word are : " + word);
                        }
                        for (String team : enemyTeamMembers) {
                            broadcastMessages("winGame," + team + "," + enemy + " won the game");
                            broadcastMessages("winGame," + team + ",The word are : " + word);
                        }
                        break loop;
                    } else if (enemyTeamWrongCount >= 6) {
                        for (String team : myTeamMembers) {
                            broadcastMessages("loseGame," + team + "," + myTeam + " won the game");
                            broadcastMessages("loseGame," + team + ",The word are : " + word);
                        }
                        for (String team : enemyTeamMembers) {
                            broadcastMessages("winGame," + team + "," + myTeam + " won the game");
                            broadcastMessages("winGame," + team + ",The word are : " + word);
                        }
                        break loop;
                    }
                    if (printWordStateMulti(word, playerGuesses, allTeamsMembers.get(index))) {
                        if (turn) {
                            for (String team : enemyTeamMembers) {
                                broadcastMessages("loseGame," + team + "," + myTeam + " won the game");
                                broadcastMessages("loseGame," + team + ",The word are : " + word);
                            }
                            for (String team : myTeamMembers) {
                                broadcastMessages("winGame," + team + "," + myTeam + " won the game");
                                broadcastMessages("winGame," + team + ",The word are : " + word);
                            }
                            break loop;
                        } else {
                            for (String team : myTeamMembers) {
                                broadcastMessages("loseGame," + team + "," + enemy + " won the game");
                                broadcastMessages("loseGame," + team + ",The word are : " + word);
                            }
                            for (String team : enemyTeamMembers) {
                                broadcastMessages("winGame," + team + "," + enemy + " won the game");
                                broadcastMessages("winGame," + team + ",The word are : " + word);
                            }
                            break loop;
                        }

                    }
                    if (!getPlayerGuessMulti(word, playerGuesses)) {
                        if (turn) {
                            myTeamWrongCount++;
                        } else {
                            enemyTeamWrongCount++;
                        }
                        for (String team : myTeamMembers) {
                            broadcastMessages("gamePrint," + team + ",Letter miss");
                            broadcastMessages("gamePrint," + team + ",Misses : " + (turn ? myTeamWrongCount : enemyTeamWrongCount));
                        }
                        for (String team : enemyTeamMembers) {
                            broadcastMessages("gamePrint," + team + ",Letter miss");
                            broadcastMessages("gamePrint," + team + ",Misses : " + (turn ? myTeamWrongCount : enemyTeamWrongCount));
                        }

                    }
                }
            }
            //broadcastMessages(messageFromClient);

        } catch (Exception e) {
            closeEveryThing(socket, bufferedReader, bufferedWriter);
            e.printStackTrace();
        }


    }

    private boolean getPlayerGuessMulti(String word, List<Character> playerGuesses) {

        String letterGuess = null;
        try {
            String message = bufferedReader.readLine();
            if (message != null && message.compareTo("null") != 0) {
                letterGuess = handleMessageFromClient(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        playerGuesses.add(letterGuess.toLowerCase().charAt(0));

        return word.toLowerCase().contains(letterGuess.toLowerCase());
    }

    private boolean printWordStateMulti(String word, List<Character> playerGuesses, String member) {
        String wordToSend = "";
        int dLength = 0;
        int correctCount = 0;
        for (int i = 0; i < word.length(); i++) {
            if (playerGuesses.contains(word.toLowerCase().charAt(i))) {
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
            for (String team : myTeamMembers) {
                broadcastMessages("gamePrint," + team + "," + wordToSend);
                broadcastMessages("gamePrint," + team + "," + member + " turn");
            }
            for (String team : enemyTeamMembers) {
                broadcastMessages("gamePrint," + team + "," + wordToSend);
                broadcastMessages("gamePrint," + team + "," + member + " turn");
            }
            broadcastMessages("gameStartedInput," + member + "," + clientUserName + ",Please enter a letter:");
        }
        return (word.length() - dLength == correctCount);
    }

    public Boolean checkTeamName(String teamName) {
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
                if (team.split(",").length >= 6) {
                    return null;
                }
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
                break;
            }
        }

        boolean isInTeam = false;

        for (String m : p.split(",")) {
            if (m.compareTo(clientUserName) == 0) {
                isInTeam = true;
            }
        }

        if (isInTeam) {
            return;
        } else {
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
                e.printStackTrace();
            }
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
