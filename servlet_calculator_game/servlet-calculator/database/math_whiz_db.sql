-- SHIPON - MATH WHIZ database (MySQL)
-- Run this in MySQL Workbench (File > Open SQL Script, then the lightning button).
-- You do NOT have to run it by hand: the app creates the database and tables
-- automatically on the first run. It is here so you can see/create them in Workbench.

CREATE DATABASE IF NOT EXISTS math_whiz_db;
USE math_whiz_db;

-- Math Whiz scores (leaderboard)
CREATE TABLE IF NOT EXISTS game_scores (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(100) NOT NULL,
    score       INT          NOT NULL,
    difficulty  VARCHAR(20)  NOT NULL,
    played_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Calculator history
CREATE TABLE IF NOT EXISTS history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    expression  VARCHAR(255) NOT NULL,
    result      DOUBLE       NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Example: save a game
-- INSERT INTO game_scores (player_name, score, difficulty, played_at)
-- VALUES ('Shipon', 10, 'NORMAL', NOW());

-- Top 10 leaderboard (higher score first, earlier time wins a tie)
SELECT * FROM game_scores ORDER BY score DESC, played_at ASC LIMIT 10;

-- Calculator history, newest first
SELECT * FROM history ORDER BY id DESC;
