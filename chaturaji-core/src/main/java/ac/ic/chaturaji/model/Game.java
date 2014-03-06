package ac.ic.chaturaji.model;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * @author samirarabbanian
 */
public class Game extends EqualsHashCodeToString {
    private String id;
    private LocalDateTime createdDate;
    private List<Player> players = new ArrayList<>();
    private List<Move> moves = new ArrayList<>();
    @JsonIgnore
    private long[] bitboards;
    private Colour currentPlayer = Colour.YELLOW;
    private int stalemateCount;

    // Dummy constructor needed to map JSON string back to Java object
    public Game() {
        LocalDateTime localDateTime = new LocalDateTime();
        createdDate = new LocalDateTime(localDateTime.getYear(), localDateTime.getMonthOfYear(), localDateTime.getDayOfMonth(), localDateTime.getHourOfDay(), localDateTime.getMinuteOfHour());
    }

    public Game(String id, Player player) {
        this();
        this.id = id;
        players.add(player);
    }

    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        // TODO - REMOVE THIS EXCLUSION ONCE PLAYERS ARE SAVED IN THE DB CORRECTLY
        return new String[]{"players"};
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    /**
     * Needed by AI but not for client to use
     */
    public long[] getBitboards() {
        return bitboards;
    }

    public void setBitboards(long[] bitboards) {
        this.bitboards = bitboards;
    }

    public Colour getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Colour currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getStalemateCount() { return stalemateCount; }

    public void incrementStalemateCount() { stalemateCount++; }
    public void resetStalemateCount() { stalemateCount = 0; }

    public Player getPlayer(int index) {
        return players.get(index);
    }

    public void addPlayer(Player player) {
        if (players.size() < 4) {
            players.add(player);
        } else {
            throw new RuntimeException("Game already has four players");
        }
    }
}