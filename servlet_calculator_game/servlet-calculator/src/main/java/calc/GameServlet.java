package calc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * SHIPON - MATH WHIZ game.
 * The system gives the numbers, the player guesses, and the answers are then
 * calculated by the MathService methods and shown on calculator-style screens.
 *
 *   GET  /game                  welcome page
 *   GET  /game?p=play           the four questions
 *   GET  /game?p=result         the calculated answers + score
 *   GET  /game?p=leaderboard    top 10 scores
 *   POST /game (action=start)   create a new game
 *   POST /game (action=submit)  evaluate guesses and save the score
 */
public class GameServlet extends HttpServlet {

    private static final String DIFFICULTY = "NORMAL";
    private static final String TITLE = "SHIPON \u2013 MATH WHIZ";
    private static final int[] ANGLES = {0, 30, 45, 60, 90, 120, 135, 150, 180,
            210, 225, 240, 270, 300, 315, 330, 360};
    private static final int[] MARKS = {MathService.PRIME_MARKS, MathService.MOD_MARKS,
            MathService.TRIG_MARKS, MathService.TRIG_MARKS};
    private static final String[] AREAS = {"Prime Number", "Modulus", "Trigonometry", "Trigonometry"};

    private final ScoreDAO dao = new ScoreDAO();
    private final Random rnd = new Random();

    /** Everything about one game. Kept in the player's session. */
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        String player;
        int primeNumber, modA, modB, angle3, angle4;
        String fn3, fn4;
        boolean finished;
        String[] guess = new String[4];
        String[] answer = new String[4];
        boolean[] correct = new boolean[4];
        int total;
    }

    @Override
    public void init() throws ServletException {
        try {
            dao.init();
        } catch (SQLException e) {
            throw new ServletException("Could not initialise Math Whiz database", e);
        }
    }

    // ------------------------------------------------------------ requests

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String p = req.getParameter("p");
        GameState g = (GameState) req.getSession().getAttribute("game");

        if ("play".equals(p)) {
            if (g == null || g.finished) { resp.sendRedirect("/game"); return; }
            showPlay(resp, g, null);
        } else if ("result".equals(p)) {
            if (g == null || !g.finished) { resp.sendRedirect("/game"); return; }
            showResult(resp, g);
        } else if ("leaderboard".equals(p)) {
            showLeaderboard(resp);
        } else {
            showWelcome(resp, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        HttpSession session = req.getSession();

        if ("start".equals(action)) {
            String name = req.getParameter("player");
            name = name == null ? "" : name.trim();
            if (name.isEmpty()) { showWelcome(resp, "Please enter your name."); return; }
            if (name.length() > 50) name = name.substring(0, 50);
            session.setAttribute("game", newGame(name));
            resp.sendRedirect("/game?p=play");

        } else if ("submit".equals(action)) {
            GameState g = (GameState) session.getAttribute("game");
            if (g == null) { resp.sendRedirect("/game"); return; }
            if (!g.finished) {
                evaluate(g, req);
                try {
                    dao.save(g.player, g.total, DIFFICULTY);
                } catch (SQLException e) {
                    showPlay(resp, g, "Database error: " + e.getMessage());
                    return;
                }
                g.finished = true;
            }
            // Redirect so refreshing the page can't save the score twice
            resp.sendRedirect("/game?p=result");

        } else {
            resp.sendRedirect("/game");
        }
    }

    // ---------------------------------------------------------- game logic

    /** The system picks the numbers. */
    private GameState newGame(String player) {
        GameState g = new GameState();
        g.player = player;

        // Q1: about half the time a prime, half the time not
        boolean wantPrime = rnd.nextBoolean();
        do {
            g.primeNumber = rnd.nextInt(100) + 1;
        } while ((MathService.isPrime(g.primeNumber) == 1) != wantPrime);

        // Q2: a % b with b never zero
        g.modA = rnd.nextInt(90) + 10;
        g.modB = rnd.nextInt(11) + 2;

        // Q3 and Q4: trigonometry (never an undefined tangent, never the same question twice)
        g.fn3 = rnd.nextBoolean() ? "sin" : "cos";
        g.angle3 = ANGLES[rnd.nextInt(ANGLES.length)];
        String[] fns = {"sin", "cos", "tan"};
        do {
            g.fn4 = fns[rnd.nextInt(3)];
            g.angle4 = ANGLES[rnd.nextInt(ANGLES.length)];
        } while ((g.fn4.equals("tan") && !MathService.isTanDefined(g.angle4))
                || (g.fn4.equals(g.fn3) && g.angle4 == g.angle3));
        return g;
    }

    /** The real answers come from the MathService methods, then the guesses are checked. */
    private void evaluate(GameState g, HttpServletRequest req) {
        int primeAns = MathService.isPrime(g.primeNumber);
        int modAns = MathService.calculateMod(g.modA, g.modB);
        double trig3 = MathService.calculateTrig(g.fn3, g.angle3);
        double trig4 = MathService.calculateTrig(g.fn4, g.angle4);

        g.answer[0] = String.valueOf(primeAns);
        g.answer[1] = String.valueOf(modAns);
        g.answer[2] = fmt(trig3);
        g.answer[3] = fmt(trig4);

        for (int i = 0; i < 4; i++) {
            String raw = req.getParameter("q" + (i + 1));
            g.guess[i] = raw == null ? "" : raw.trim();
        }

        Integer guessPrime = parseInt(g.guess[0]);
        Integer guessMod = parseInt(g.guess[1]);
        Double guessTrig3 = parseDouble(g.guess[2]);
        Double guessTrig4 = parseDouble(g.guess[3]);

        g.correct[0] = guessPrime != null && guessPrime == primeAns;
        g.correct[1] = guessMod != null && guessMod == modAns;
        g.correct[2] = guessTrig3 != null && MathService.isClose(guessTrig3, trig3);
        g.correct[3] = guessTrig4 != null && MathService.isClose(guessTrig4, trig4);

        g.total = MathService.totalScore(g.correct[0], g.correct[1], g.correct[2], g.correct[3]);
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String[] expressions(GameState g) {
        return new String[] {
            "isPrime(" + g.primeNumber + ")",
            g.modA + " % " + g.modB,
            g.fn3 + "(" + g.angle3 + "\u00b0)",
            g.fn4 + "(" + g.angle4 + "\u00b0)"
        };
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // --------------------------------------------------------------- pages

    private static final String CSS = String.join("\n",
        "*{box-sizing:border-box}",
        "body{font-family:'Trebuchet MS',system-ui,'Segoe UI',sans-serif;background:#1b1038 linear-gradient(135deg,#1b1038 0%,#3a1c5e 50%,#0e4a5a 100%) fixed;min-height:100vh;margin:0;padding:28px 12px;color:#f1ecfb}",
        "h1{text-align:center;font-size:26px;letter-spacing:3px;text-transform:uppercase;margin:0 0 16px;background:linear-gradient(90deg,#7cf9e6,#ff7ab8);-webkit-background-clip:text;background-clip:text;color:transparent}",
        ".nav{text-align:center;margin:0 0 22px}",
        ".nav a{display:inline-block;color:#f1ecfb;margin:4px 5px;padding:7px 16px;border-radius:999px;background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.2);text-decoration:none;font-weight:600;font-size:14px;transition:background .2s}",
        ".nav a:hover{background:rgba(124,249,230,.25)}",
        ".screen{background:rgba(0,0,0,.38);border:1px solid rgba(255,255,255,.12);border-radius:18px}",
        ".small{color:#c3b5e8;font-family:ui-monospace,Consolas,monospace}",
        ".main{color:#7cf9e6;font-family:ui-monospace,Consolas,monospace;font-weight:400;text-shadow:0 0 14px rgba(124,249,230,.45)}",
        ".err{color:#ffb4c6}",
        "table{width:100%;border-collapse:collapse}",
        "th,td{border-bottom:1px solid rgba(255,255,255,.14);padding:9px 8px;text-align:left;font-size:14px}",
        "th{background:rgba(255,255,255,.1);color:#7cf9e6;letter-spacing:.5px}",
        "::placeholder{color:#a595cc}",
        ".panel{max-width:520px;margin:0 auto 20px;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.18);backdrop-filter:blur(12px);border-radius:26px;padding:22px;box-shadow:0 10px 30px rgba(0,0,0,.35)}",
        ".panel h2{margin:0 0 8px;font-size:19px;color:#ffd3e6}",
        ".panel ul{padding-left:20px;line-height:1.6}",
        ".muted{color:#c3b5e8;font-size:14px}",
        ".q{max-width:520px;margin:0 auto 20px;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.18);border-left:5px solid #ff7ab8;backdrop-filter:blur(12px);border-radius:24px;padding:18px;color:#f1ecfb;box-shadow:0 10px 28px rgba(0,0,0,.35)}",
        ".qh{font-size:13px;letter-spacing:.5px;text-transform:uppercase;color:#ffb3d4;margin-bottom:10px;display:flex;justify-content:space-between}",
        ".screen{padding:12px 16px;text-align:right}",
        ".small{font-size:17px;min-height:22px}",
        ".main{font-size:38px;line-height:1.3;overflow-x:auto}",
        ".hint{color:#b3a4d6;font-size:13px;margin:10px 0 8px}",
        ".q input{width:100%;font-size:20px;padding:12px 14px;border-radius:14px;border:1px solid rgba(124,249,230,.35);text-align:right;background:rgba(0,0,0,.3);color:#fff;outline:none}",
        ".q input:focus,input.name:focus{border-color:#7cf9e6;box-shadow:0 0 0 3px rgba(124,249,230,.25)}",
        "input.name{width:100%;font-size:18px;padding:12px 14px;border:1px solid rgba(124,249,230,.35);border-radius:14px;margin:12px 0;background:rgba(0,0,0,.3);color:#fff;outline:none}",
        "button,.btn{display:inline-block;font-size:16px;font-weight:600;padding:12px 26px;border:0;border-radius:999px;background:linear-gradient(135deg,#8b5cf6,#ec4899);color:#fff;cursor:pointer;text-decoration:none;margin:4px 4px 0 0;box-shadow:0 6px 18px rgba(236,72,153,.35);transition:transform .1s,filter .2s}",
        "button:hover,.btn:hover{filter:brightness(1.15)}",
        "button:active,.btn:active{transform:scale(.96)}",
        ".btn.alt{background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.25);box-shadow:none}",
        ".ok{color:#5eead4;font-weight:700}",
        ".bad{color:#fda4af;font-weight:700}",
        ".score{text-align:center;font-size:56px;font-weight:800;margin:4px 0;background:linear-gradient(90deg,#7cf9e6,#ff7ab8);-webkit-background-clip:text;background-clip:text;color:transparent}",
        ".err{max-width:520px;margin:0 auto 16px;background:rgba(225,29,72,.22);border:1px solid rgba(255,122,150,.5);color:#ffd0db;border-radius:14px;padding:11px 16px}",
        ".center{text-align:center}");

    private static PrintWriter begin(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        return resp.getWriter();
    }

    private static void head(PrintWriter out) {
        out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>" + esc(TITLE) + "</title><style>" + CSS + "</style></head><body>");
        out.println("<div class='nav'><a href='/'>Calculator</a><a href='/game'>Math Whiz</a>"
                + "<a href='/game?p=leaderboard'>Leaderboard</a></div>");
        out.println("<h1>" + esc(TITLE) + "</h1>");
    }

    private static void foot(PrintWriter out) {
        out.println("</body></html>");
    }

    /** Screen 1: welcome page. */
    private void showWelcome(HttpServletResponse resp, String error) throws IOException {
        PrintWriter out = begin(resp);
        head(out);
        if (error != null) out.println("<div class='err'>" + esc(error) + "</div>");
        out.println("<div class='panel'><h2>Welcome!</h2>"
                + "<p class='muted'>You get four challenges. The system gives you the numbers and "
                + "you guess the answers. Then the calculator works out the real answers and "
                + "scores you out of 10.</p>"
                + "<form method='post' action='/game'>"
                + "<input type='hidden' name='action' value='start'>"
                + "<input class='name' type='text' name='player' placeholder='Enter your name' "
                + "maxlength='50' required autofocus>"
                + "<button type='submit'>Start Game</button></form></div>");
        out.println("<div class='panel'><h2>Rules</h2><ul class='muted'>"
                + "<li><b>Prime number</b> (2 marks): type 1 if prime, 0 if not.</li>"
                + "<li><b>Modulus</b> (2 marks): the remainder of the division, whole numbers only.</li>"
                + "<li><b>Trigonometry</b> (3 marks each): sine, cosine or tangent of an angle in "
                + "degrees, to 2 decimal places.</li>"
                + "<li>Wrong answers earn nothing. Maximum score: 10. Difficulty: " + DIFFICULTY + ".</li>"
                + "</ul></div>");
        foot(out);
    }

    /** Screen 2: the four questions. */
    private void showPlay(HttpServletResponse resp, GameState g, String error) throws IOException {
        PrintWriter out = begin(resp);
        head(out);
        out.println("<p class='nav muted'>Player: <b>" + esc(g.player) + "</b> &middot; Difficulty: "
                + DIFFICULTY + "</p>");
        if (error != null) out.println("<div class='err'>" + esc(error) + "</div>");

        String[] ex = expressions(g);
        String[] hints = {
            "Is " + g.primeNumber + " a prime number? Type 1 for prime, 0 for not prime.",
            "What is the remainder? (whole number)",
            "Answer to 2 decimal places, for example 0.87",
            "Answer to 2 decimal places, for example 0.87"};
        String[] attrs = {"min='0' max='1' step='1'", "step='1'", "step='any'", "step='any'"};

        out.println("<form method='post' action='/game'>"
                + "<input type='hidden' name='action' value='submit'>");
        for (int i = 0; i < 4; i++) {
            out.println("<div class='q'><div class='qh'><span>Question " + (i + 1) + " &middot; "
                    + AREAS[i] + "</span><span>" + MARKS[i] + " marks</span></div>"
                    + "<div class='screen'><div class='small'>" + esc(ex[i]) + " =</div>"
                    + "<div class='main'>?</div></div>"
                    + "<div class='hint'>" + esc(hints[i]) + "</div>"
                    + "<input type='number' name='q" + (i + 1) + "' " + attrs[i]
                    + " placeholder='Your guess' required></div>");
        }
        out.println("<div class='center'><button type='submit'>Submit Answers</button></div></form>");
        foot(out);
    }

    /** Screen 3: calculator screens showing the real answers, plus the score. */
    private void showResult(HttpServletResponse resp, GameState g) throws IOException {
        PrintWriter out = begin(resp);
        head(out);
        out.println("<div class='panel'><h2 class='center'>Result for " + esc(g.player) + "</h2>"
                + "<div class='score'>" + g.total + " / 10</div>"
                + "<p class='muted center'>Difficulty: " + DIFFICULTY
                + " &middot; Your score has been saved.</p></div>");

        String[] ex = expressions(g);
        String[] methods = {"isPrime()", "calculateMod()",
            "calculate" + cap(g.fn3) + "()", "calculate" + cap(g.fn4) + "()"};

        for (int i = 0; i < 4; i++) {
            int earned = g.correct[i] ? MARKS[i] : 0;
            String guess = g.guess[i].isEmpty() ? "(no answer)" : g.guess[i];
            String verdict = g.correct[i]
                    ? "<span class='ok'>&#10004; Correct +" + earned + "</span>"
                    : "<span class='bad'>&#10008; Not correct +0</span>";
            out.println("<div class='q'><div class='qh'><span>Question " + (i + 1) + " &middot; "
                    + AREAS[i] + "</span><span>" + earned + " / " + MARKS[i] + " marks</span></div>"
                    + "<div class='screen'><div class='small'>" + esc(ex[i]) + " =</div>"
                    + "<div class='main'>" + esc(g.answer[i]) + "</div></div>"
                    + "<div class='hint'>Calculated by MathService." + esc(methods[i]) + "</div>"
                    + "<div>Your guess: <b>" + esc(guess) + "</b> &nbsp; " + verdict + "</div></div>");
        }

        out.println("<div class='center'>"
                + "<form method='post' action='/game' style='display:inline'>"
                + "<input type='hidden' name='action' value='start'>"
                + "<input type='hidden' name='player' value='" + esc(g.player) + "'>"
                + "<button type='submit'>Play Again</button></form>"
                + "<a class='btn alt' href='/game?p=leaderboard'>Leaderboard</a>"
                + "<a class='btn alt' href='/'>Calculator</a></div>");
        foot(out);
    }

    /** Screen 4: top 10 from the database. */
    private void showLeaderboard(HttpServletResponse resp) throws IOException {
        PrintWriter out = begin(resp);
        head(out);
        out.println("<div class='panel' style='max-width:720px'><h2>Top 10 Scores</h2>");
        try {
            List<GameScore> list = dao.topTen();
            out.println("<table><tr><th>#</th><th>Player</th><th>Score</th><th>Difficulty</th>"
                    + "<th>Date / Time</th></tr>");
            int rank = 1;
            for (GameScore s : list) {
                out.println("<tr><td>" + rank++ + "</td><td>" + esc(s.getPlayerName()) + "</td><td>"
                        + s.getScore() + "</td><td>" + esc(s.getDifficulty()) + "</td><td>"
                        + esc(s.getPlayedAt()) + "</td></tr>");
            }
            if (list.isEmpty()) out.println("<tr><td colspan='5'>No games played yet.</td></tr>");
            out.println("</table>");
        } catch (SQLException e) {
            out.println("<p class='bad'>Could not read database: " + esc(e.getMessage()) + "</p>");
        }
        out.println("<p><a class='btn' href='/game'>Play</a></p></div>");
        foot(out);
    }

    // ------------------------------------------------------------- helpers

    /** 0.8660254 -> "0.866", 1.0000000000000002 -> "1", -1.8e-16 -> "0". */
    private static String fmt(double d) {
        BigDecimal bd = new BigDecimal(d).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.signum() == 0 ? "0" : bd.toPlainString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
