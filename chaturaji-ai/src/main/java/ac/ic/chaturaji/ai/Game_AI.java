package ac.ic.chaturaji.ai;

import ac.ic.chaturaji.model.*;

import java.util.List;
import java.util.Scanner;

public class Game_AI {
    AI api;
    Scanner read;
    char input[];
    Game game;

    public Game_AI () {
        game = new Game();
        api = new AI();
        input = new char[20];
        read = new Scanner(System.in);

        boolean move_complete = false;
        int source, dest, colour;
        char ch;
        int[] kings = new int[] {0, 0, 0, 0};
        Move move = new Move();
        Result result;

        List<Player> players = game.getPlayers();

        for (int i = 0; i < 4; i++) {
            System.out.println("What type of player will " + GameConstants.PlayerStrings[i] + " be? [H]uman or [C]omputer?");
            ch = read.next().charAt(0);
            Player player = new Player();

            if (ch == 'H' || ch == 'h') {
                player.setType(PlayerType.HUMAN);
                game.addPlayer(player);
            }
            else {
                player.setType(PlayerType.AI);
                game.addPlayer(player);
            }
            player.setKingsCaptured(kings);
        }

        // Initialise the game:
        api.startGame(game);

        do{
            //System.out.println("Press 'c' to continue");
            //ch = read.next().charAt(0);


            // Select the player about to make a move.
            colour = game.getCurrentPlayer().ordinal();
            System.out.println(GameConstants.PlayerStrings[colour] + " to move next.");

            if (players.get(colour).getType() == PlayerType.HUMAN)
                System.out.print("Please enter your move:" );
            else {
                move.setColour(game.getCurrentPlayer());
                result = api.submitMove(game, move);
                if (result.getGameStatus() == GameStatus.GAME_OVER)
                    move_complete = true;
                continue;
            }

            String moves = read.nextLine();
            input = moves.toCharArray();

            if (input.length == 0) {
                move_complete = true;
                continue;
            }

            source = ((int)(Character.toLowerCase(input[0])) - 'a') + (8 - ((int)(input[1]) - '0')) * 8;
            dest = ((int)(Character.toLowerCase(input[3])) - 'a') + (8 - ((int)(input[4]) - '0')) * 8;

            if ((source < 0 ) || (source > 63 )){
                System.out.println( "Illegal source square " + source);
                continue;
            }
            if ((dest < 0 ) || (dest > 63)){
                System.out.println( "Illegal destination square " + dest);
                continue;
            }

            move.setColour(game.getCurrentPlayer());
            move.setSource(source);
            move.setDestination(dest);

            result = api.submitMove(game, move);

            if (result.getGameStatus() == GameStatus.GAME_OVER)
                move_complete = true;

        } while (!move_complete);

        System.out.println("Game over! Thank you for playing");
    }
}