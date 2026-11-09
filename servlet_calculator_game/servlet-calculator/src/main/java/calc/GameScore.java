package calc;

/** One row of the game_scores table. */
public class GameScore {
    private final int id;
    private final String playerName;
    private final int score;
    private final String difficulty;
    private final String playedAt;

    public GameScore(int id, String playerName, int score, String difficulty, String playedAt) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.difficulty = difficulty;
        this.playedAt = playedAt;
    }

    public int getId() { return id; }
    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public String getDifficulty() { return difficulty; }
    public String getPlayedAt() { return playedAt; }
}
