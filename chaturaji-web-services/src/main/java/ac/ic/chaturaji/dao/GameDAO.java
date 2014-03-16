package ac.ic.chaturaji.dao;

import ac.ic.chaturaji.model.*;
import ac.ic.chaturaji.uuid.UUIDFactory;
import org.joda.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author samirarabbanian
 */
@Component
public class GameDAO {

    @Resource
    private DataSource dataSource;
    @Resource
    private UUIDFactory uuidFactory;
    @Resource
    private PlayerDAO playerDAO;
    @Resource
    private MoveDAO moveDAO;
    @Resource
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void setupDefaultData() {
        List<Game> games = Arrays.asList(
                new Game(uuidFactory.generateUUID(), new Player(uuidFactory.generateUUID(), new User(uuidFactory.generateUUID(), "as@df.com", passwordEncoder.encode("qazqaz"), "user_one"), Colour.YELLOW, PlayerType.HUMAN)),
                new Game(uuidFactory.generateUUID(), new Player(uuidFactory.generateUUID(), new User(uuidFactory.generateUUID(), "fd@sa.com", passwordEncoder.encode("qazqaz"), "user_two"), Colour.YELLOW, PlayerType.HUMAN)),
                new Game(uuidFactory.generateUUID(), new Player(uuidFactory.generateUUID(), new User(uuidFactory.generateUUID(), "qa@qa.com", passwordEncoder.encode("qazqaz"), "user_three"), Colour.YELLOW, PlayerType.HUMAN)),
                new Game(uuidFactory.generateUUID(), new Player(uuidFactory.generateUUID(), new User(uuidFactory.generateUUID(), "qa@az.com", passwordEncoder.encode("qazqaz"), "user_four"), Colour.YELLOW, PlayerType.HUMAN))
        );
        for (Game game : games) {
            save(game);
        }
    }

    public Collection<Game> getAll() {
        String sql = "SELECT GAME_ID, CREATED_DATE, CURRENT_PLAYER, GAME_STATUS FROM GAME";
        List<Game> games = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                games.add(getGame(result));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return games;
    }

    public Game get(String id) {
        String sql = "SELECT GAME_ID, CREATED_DATE, CURRENT_PLAYER, GAME_STATUS FROM GAME WHERE GAME_ID=?";

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                return getGame(result);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Game getGame(ResultSet result) throws SQLException {
        Game game = new Game();
        game.setId(result.getString("GAME_ID"));
        game.setCreatedDate(new LocalDateTime(result.getTimestamp("CREATED_DATE").getTime()));
        game.setCurrentPlayer(Colour.values()[result.getInt("CURRENT_PLAYER")]);
        game.setGameStatus(GameStatus.values()[result.getInt("GAME_STATUS")]);
        game.setPlayers(playerDAO.getAll(game.getId()));
        game.setMoves(moveDAO.getAll(game.getId()));
        return game;
    }

    public void save(Game game) {
        String sql = "INSERT INTO GAME(GAME_ID, CREATED_DATE, CURRENT_PLAYER, GAME_STATUS) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, game.getId());
            ps.setDate(2, new java.sql.Date(game.getCreatedDate().toDate().getTime()));
            ps.setInt(3, game.getCurrentPlayer().ordinal());
            ps.setInt(4, game.getGameStatus().ordinal());
            if (ps.executeUpdate() != 1) {
                throw new RuntimeException();
            }
            for (Player player : game.getPlayers()) {
                playerDAO.save(game.getId(), player);
            }
            for (Move move : game.getMoves()) {
                moveDAO.save(game.getId(), move);
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
