package ac.ic.chaturaji.web.controller;

import ac.ic.chaturaji.ai.AI;
import ac.ic.chaturaji.dao.GameDAO;
import ac.ic.chaturaji.dao.MoveDAO;
import ac.ic.chaturaji.dao.PlayerDAO;
import ac.ic.chaturaji.model.*;
import ac.ic.chaturaji.security.SpringSecurityUserContext;
import ac.ic.chaturaji.uuid.UUIDFactory;
import ac.ic.chaturaji.web.websockets.NotifyPlayer;
import ac.ic.chaturaji.web.websockets.ReplayGameMoveSender;
import ac.ic.chaturaji.web.websockets.WebSocketServletContextListener;
import ac.ic.chaturaji.websockets.ClientRegistrationListener;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ac.ic.chaturaji.web.controller.InMemoryGamesContextListener.getInMemoryGames;

/**
 * @author samirarabbanian
 */
@Controller
public class GameController {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameDAO gameDAO;
    @Resource
    private MoveDAO moveDAO;
    @Resource
    private PlayerDAO playerDAO;
    @Resource
    private UUIDFactory uuidFactory;
    @Resource
    private AI ai;
    @Resource
    private SpringSecurityUserContext springSecurityUserContext;
    @Resource
    private ServletContext servletContext;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @ResponseBody
    @RequestMapping(value = "/games", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    public List<Game> games() throws IOException {
        User currentUser = springSecurityUserContext.getCurrentUser();
        List<Game> notYourGames = new ArrayList<>();
        for (Game game : gameDAO.getAllWaitingForPlayers()) {
            if (!alreadyJoinedGame(game, currentUser) && getInMemoryGames(servletContext).get(game.getId()) != null) {
                notYourGames.add(game);
            }
        }
        return notYourGames;
    }

    @ResponseBody
    @RequestMapping(value = "/gameHistory", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    public Collection<Game> gameHistory() throws IOException {
        User currentUser = springSecurityUserContext.getCurrentUser();
        return gameDAO.getFinishedGames(currentUser.getId());
    }

    @ResponseBody
    @RequestMapping(value = "/createGame", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public ResponseEntity createGame(@RequestParam("numberOfAIPlayers") int numberOfAIPlayers, @RequestParam(value = "aiLevel", required = false, defaultValue = "8") int aiLevel) throws IOException {
        logger.info("User " + springSecurityUserContext.getCurrentUser() + " creating game with " + numberOfAIPlayers + " number of AI players");
        if (numberOfAIPlayers < 0 || numberOfAIPlayers > 3) {
            return new ResponseEntity<>("Invalid numberOfAIPlayers: " + numberOfAIPlayers + " is not between 0 and 3 inclusive", HttpStatus.BAD_REQUEST);
        }
        if (aiLevel > 10 || aiLevel < 2) {
            return new ResponseEntity<>("Invalid aiLevel: " + aiLevel + " is not between 2 and 10 inclusive", HttpStatus.BAD_REQUEST);
        }

        User currentUser = springSecurityUserContext.getCurrentUser();
        // create new player
        Player player = new Player(uuidFactory.generateUUID(), currentUser, Colour.values()[0], PlayerType.HUMAN);
        // create game
        Game game = new Game(uuidFactory.generateUUID(), player);
        game.setAILevel(aiLevel);
        // add AI players
        for (int i = 1; i <= numberOfAIPlayers; i++) {
            game.addPlayer(new Player(uuidFactory.generateUUID(), currentUser, Colour.values()[i], PlayerType.AI));
        }
        ai.createGame(game);
        try {
            // save game
            gameDAO.save(game);
            getInMemoryGames(servletContext).put(game.getId(), game);
            // register web socket listener
            registerMoveListener(game.getId(), player);
        } catch (Exception e) {
            logger.warn("Exception while saving game", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(player, HttpStatus.CREATED);
    }

    @ResponseBody
    @RequestMapping(value = "/joinGame", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public ResponseEntity joinGame(@RequestParam("gameId") String gameId) throws IOException {
        logger.info("User " + springSecurityUserContext.getCurrentUser() + " is joining game " + gameId);
        Game game = getInMemoryGames(servletContext).get(gameId);
        if (game == null) {
            game = gameDAO.get(gameId);
            if (game == null) {
                logger.info("No game found with gameId: " + gameId);
                return new ResponseEntity<>("No game found with gameId: " + gameId, HttpStatus.BAD_REQUEST);
            } else {
                getInMemoryGames(servletContext).put(gameId, game);
            }
        }
        if (game.getPlayers().size() >= 4) {
            logger.info("Game already has four players");
            return new ResponseEntity<>("Game already has four players", HttpStatus.BAD_REQUEST);
        }
        User currentUser = springSecurityUserContext.getCurrentUser();
        if (alreadyJoinedGame(game, currentUser)) {
            logger.info("You have already joined this game");
            return new ResponseEntity<>("You have already joined this game", HttpStatus.BAD_REQUEST);
        }

        // create new player
        Player player = new Player(uuidFactory.generateUUID(), currentUser, Colour.values()[game.getPlayerCount()], PlayerType.HUMAN);
        try {
            // update game and save
            game.addPlayer(player);
            playerDAO.save(game.getId(), player);
            // register web socket listener
            registerMoveListener(gameId, player);
        } catch (Exception e) {
            logger.warn("Exception while saving game", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(player, HttpStatus.CREATED);
    }

    // Note: request must have a Content-Type: application/json; charset=UTF-8
    @ResponseBody
    @RequestMapping(value = "/submitMove", method = RequestMethod.POST, produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> submitMove(@RequestBody final Move move) {
        try {
            logger.debug("User " + springSecurityUserContext.getCurrentUser() + " is submitting move " + move);
            final Game game = getInMemoryGames(servletContext).get(move.getGameId());
            if (game != null) {
                if (game.getPlayers().size() == 4) {
                    Result result = ai.submitMove(game, move);
                    if (result.getType() == ResultType.NOT_VALID) {
                        logger.info("Move " + move + " is not valid");
                        return new ResponseEntity<>("That move is not valid", HttpStatus.BAD_REQUEST);
                    } else {
                        for (Player player : result.getGame().getPlayers()) {
                            playerDAO.save(result.getGame().getId(), player);
                        }
                        moveDAO.save(result.getGame().getId(), result.getMove());
                        gameDAO.save(game);
                        // allow for a human player to have no pieces left
                        while (game.currentPlayerCanNotMoveAnyPiece()) {
                            game.setCurrentPlayerColour(game.getNextPlayerColour());
                        }
                        // now schedule AI move if next player is AI
                        if (game.getCurrentPlayerType() == PlayerType.AI && result.getGameStatus() == GameStatus.IN_PLAY) {
                            taskExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    submitMove(new Move(uuidFactory.generateUUID(), move.getGameId(), game.getCurrentPlayerColour(), -1, -1));
                                }
                            });
                        }
                        return new ResponseEntity<>("", HttpStatus.ACCEPTED);
                    }
                } else {
                    return new ResponseEntity<>("No enough players, please wait for more players to join", HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>("No game found with gameId: " + move.getGameId(), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.warn("Exception while submitting move", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ResponseBody
    @RequestMapping(value = "/replayGame", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public ResponseEntity replayGame(@RequestParam("gameId") String gameId) throws IOException {
        logger.info("User " + springSecurityUserContext.getCurrentUser() + " is replaying game " + gameId);
        Game game = gameDAO.get(gameId);
        if (game == null) {
            logger.info("No game found with gameId: " + gameId);
            return new ResponseEntity<>("No game found with gameId: " + gameId, HttpStatus.BAD_REQUEST);
        }
        // find player
        Player player = findPlayer(game, springSecurityUserContext.getCurrentUser());
        if (player == null) {
            logger.info("You can only replay games for which you were a player");
            return new ResponseEntity<>("You can only replay games for which you were a player", HttpStatus.BAD_REQUEST);
        }

        // add web socket registration listener
        Map<String, ClientRegistrationListener> clientRegistrationListeners = (Map<String, ClientRegistrationListener>) servletContext.getAttribute(WebSocketServletContextListener.WEB_SOCKET_CLIENT_REGISTRATION_LISTENERS_ATTRIBUTE_NAME);
        clientRegistrationListeners.put("ID_" + player.getId(), new ReplayGameMoveSender(gameId, moveDAO, gameDAO));

        return new ResponseEntity<>(player, HttpStatus.ACCEPTED);
    }

    public Player findPlayer(Game game, User user) {
        if (user != null) {
            for (Player player : game.getPlayers()) {
                if (player.getUser().equals(user)) {
                    return player;
                }
            }
        }
        return null;
    }

    public boolean alreadyJoinedGame(Game game, User user) {
        if (user != null) {
            for (Player player : game.getPlayers()) {
                if (player.getUser().equals(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void registerMoveListener(String gameId, Player player) {
        ai.registerListener(gameId, new NotifyPlayer(player.getId(), (Map<String, Channel>) servletContext.getAttribute(WebSocketServletContextListener.WEB_SOCKET_CLIENT_ATTRIBUTE_NAME)));
    }

}
