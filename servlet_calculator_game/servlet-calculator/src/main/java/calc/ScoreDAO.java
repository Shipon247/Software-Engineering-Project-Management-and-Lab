package calc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Talks to the Math Whiz database (MySQL database math_whiz_db) using JDBC. */
public class ScoreDAO {

    /** Creates the game_scores table if it does not exist yet. */
    public void init() throws SQLException {
        System.out.println("Math Whiz database: " + DbConfig.describe());
        try (Connection c = DbConfig.connect();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS game_scores ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "player_name VARCHAR(100) NOT NULL,"
                    + "score INT NOT NULL,"
                    + "difficulty VARCHAR(20) NOT NULL,"
                    + "played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    /** Saves one finished game. */
    public void save(String playerName, int score, String difficulty) throws SQLException {
        String sql = "INSERT INTO game_scores(player_name, score, difficulty, played_at) "
                + "VALUES (?, ?, ?, NOW())";
        try (Connection c = DbConfig.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setInt(2, score);
            ps.setString(3, difficulty);
            ps.executeUpdate();
        }
    }

    /** The ten best scores. Equal scores: the earlier one ranks higher. */
    public List<GameScore> topTen() throws SQLException {
        String sql = "SELECT id, player_name, score, difficulty, "
                + "DATE_FORMAT(played_at, '%Y-%m-%d %H:%i:%s') AS played_at FROM game_scores "
                + "ORDER BY score DESC, played_at ASC LIMIT 10";
        List<GameScore> list = new ArrayList<>();
        try (Connection c = DbConfig.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new GameScore(rs.getInt("id"), rs.getString("player_name"),
                        rs.getInt("score"), rs.getString("difficulty"), rs.getString("played_at")));
            }
        }
        return list;
    }
}
