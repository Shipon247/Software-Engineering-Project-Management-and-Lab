package calc;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Calculator servlet. It draws the calculator UI (display + keypad),
 * evaluates the expression on the server, saves it to MySQL,
 * and shows the saved history table.
 */
public class CalculatorServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        try {
            System.out.println("Calculator database: " + DbConfig.describe());
            try (Connection c = DbConfig.connect();
                 Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS history ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "expression VARCHAR(255) NOT NULL,"
                        + "result DOUBLE NOT NULL,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            throw new ServletException("Could not initialise database", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        render(resp, req.getParameter("e"), req.getParameter("r"), null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("clear".equals(req.getParameter("action"))) {
            try (Connection c = DbConfig.connect();
                 Statement s = c.createStatement()) {
                s.executeUpdate("DELETE FROM history");
            } catch (SQLException e) {
                render(resp, null, null, "Database error: " + e.getMessage());
                return;
            }
            resp.sendRedirect("/");
            return;
        }

        String expr = req.getParameter("expr");
        expr = expr == null ? "" : expr.replace(" ", "");
        if (expr.isEmpty() || expr.length() > 100) {
            render(resp, null, null, "Please enter a calculation.");
            return;
        }

        double result;
        try {
            result = new Parser(expr).parse();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                throw new ArithmeticException("Result is not a finite number");
            }
        } catch (RuntimeException e) {
            render(resp, expr, null, e.getMessage());
            return;
        }

        try (Connection c = DbConfig.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO history(expression, result) VALUES (?,?)")) {
            ps.setString(1, expr);
            ps.setDouble(2, result);
            ps.executeUpdate();
        } catch (SQLException e) {
            render(resp, expr, null, "Database error: " + e.getMessage());
            return;
        }

        // Post/Redirect/Get so a browser refresh doesn't save the calculation twice
        resp.sendRedirect("/?e=" + URLEncoder.encode(expr, "UTF-8")
                + "&r=" + URLEncoder.encode(fmt(result), "UTF-8"));
    }

    // ------------------------------------------------------------------ page

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
        ".calc{width:100%;max-width:350px;margin:0 auto;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.18);backdrop-filter:blur(14px);border-radius:34px;padding:20px;box-shadow:0 20px 50px rgba(0,0,0,.45)}",
        ".screen{padding:14px 18px;margin-bottom:18px;text-align:right;min-height:104px}",
        ".small{font-size:15px;min-height:20px;overflow-x:auto;white-space:nowrap}",
        ".main{font-size:40px;line-height:1.3;overflow-x:auto;white-space:nowrap}",
        ".err{font-size:14px;min-height:18px;margin-top:2px}",
        ".keys{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}",
        ".keys button{font-size:21px;font-weight:600;padding:17px 0;border:1px solid rgba(255,255,255,.14);border-radius:20px;background:rgba(255,255,255,.1);color:#f1ecfb;cursor:pointer;transition:transform .1s,filter .2s}",
        ".keys button:hover{filter:brightness(1.35)}",
        ".keys button:active{transform:scale(.93)}",
        ".keys .op{background:linear-gradient(145deg,#ff5fa2,#ff8a3d);border-color:transparent;color:#fff}",
        ".keys .fn{background:rgba(167,139,250,.35);color:#e9deff}",
        ".keys .eq{background:linear-gradient(145deg,#22d3ee,#34d399);border-color:transparent;color:#06222b}",
        ".keys .wide{grid-column:span 2}",
        ".hist{max-width:640px;margin:36px auto 0;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.16);backdrop-filter:blur(12px);border-radius:24px;padding:20px;box-shadow:0 10px 30px rgba(0,0,0,.35)}",
        ".hist h2{margin:0 0 4px;font-size:18px;color:#ffd3e6}",
        ".hint{color:#b3a4d6;font-size:13px;margin:0 0 12px;word-break:break-all}",
        ".hist p[style]{color:#ffb4c6!important}",
        ".clear{margin-top:14px;background:linear-gradient(145deg,#ff5fa2,#e11d48);color:#fff;border:0;border-radius:999px;padding:10px 20px;font-size:14px;font-weight:600;cursor:pointer}",
        ".clear:hover{filter:brightness(1.15)}");

    private static final String JS = String.join("\n",
        "var ops='+-*/%';",
        "var cur='', fresh=false;",
        "var app=document.getElementById('app');",
        "var disp=document.getElementById('main'), small=document.getElementById('small');",
        "var form=document.getElementById('f'), hid=document.getElementById('expr');",
        "function pretty(s){return s.split('*').join(String.fromCharCode(215)).split('/').join(String.fromCharCode(247)).split('-').join(String.fromCharCode(8722));}",
        "function show(){disp.textContent = cur==='' ? '0' : pretty(cur);}",
        "function press(k){",
        "  if(k==='C'){cur='';small.textContent='';fresh=false;}",
        "  else if(k==='B'){ if(fresh){cur='';fresh=false;} else {cur=cur.slice(0,-1);} }",
        "  else if(k==='='){ if(cur===''){return;} hid.value=cur; form.submit(); return; }",
        "  else if(ops.indexOf(k)>=0){",
        "    if(cur===''){ if(k==='-'){cur='-';} }",
        "    else { var last=cur.slice(-1);",
        "      if(ops.indexOf(last)>=0){ if(cur.length>1){cur=cur.slice(0,-1)+k;} }",
        "      else {cur+=k;} }",
        "    fresh=false;",
        "  } else {",
        "    if(fresh){cur='';fresh=false;}",
        "    if(k==='.'){",
        "      var i=cur.length-1;",
        "      while(i>=0 && ops.indexOf(cur.charAt(i))<0){i--;}",
        "      var num=cur.slice(i+1);",
        "      if(num.indexOf('.')>=0){return;}",
        "      if(num===''){cur+='0';}",
        "    }",
        "    if(cur.length<60){cur+=k;}",
        "  }",
        "  show();",
        "}",
        "var btns=document.querySelectorAll('button[data-k]');",
        "for(var j=0;j<btns.length;j++){ btns[j].onclick=function(){press(this.getAttribute('data-k'));}; }",
        "document.addEventListener('keydown',function(e){",
        "  var k=e.key;",
        "  if(k.length===1 && '0123456789.+-*/%'.indexOf(k)>=0){press(k);e.preventDefault();}",
        "  else if(k==='Enter'||k==='='){press('=');e.preventDefault();}",
        "  else if(k==='Backspace'){press('B');}",
        "  else if(k==='Escape'||k==='c'||k==='C'){press('C');}",
        "});",
        "var E=app.getAttribute('data-e'), R=app.getAttribute('data-r');",
        "if(R){cur=R;fresh=true;small.textContent=pretty(E)+' =';}",
        "else if(E){cur=E;}",
        "show();");

    private void render(HttpServletResponse resp, String expr, String result, String error)
            throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>Calculator</title><style>" + CSS + "</style></head><body>");
        out.println("<h1>Calculator</h1>");
        out.println("<div class='nav'><a href='/game'>Play SHIPON \u2013 MATH WHIZ</a>"
                + "<a href='/game?p=leaderboard'>Leaderboard</a></div>");

        // Calculator
        out.println("<div class='calc' id='app' data-e='" + esc(expr) + "' data-r='" + esc(result) + "'>");
        out.println("<div class='screen'><div class='small' id='small'></div>"
                + "<div class='main' id='main'>0</div>"
                + "<div class='err'>" + esc(error) + "</div></div>");
        out.println("<div class='keys'>");
        out.println(key("C", "C", "fn") + key("B", "&#9003;", "fn") + key("%", "%", "fn") + key("/", "&divide;", "op"));
        out.println(key("7", "7", "") + key("8", "8", "") + key("9", "9", "") + key("*", "&times;", "op"));
        out.println(key("4", "4", "") + key("5", "5", "") + key("6", "6", "") + key("-", "&minus;", "op"));
        out.println(key("1", "1", "") + key("2", "2", "") + key("3", "3", "") + key("+", "+", "op"));
        out.println(key("0", "0", "wide") + key(".", ".", "") + key("=", "=", "eq"));
        out.println("</div>");
        out.println("<form id='f' method='post' action='/'><input type='hidden' name='expr' id='expr'></form>");
        out.println("</div>");

        // History table from the database
        out.println("<div class='hist'><h2>Database: history</h2>");
        out.println("<p class='hint'>Saved in " + esc(DbConfig.describe()) + "</p>");
        try (Connection c = DbConfig.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id, expression, result, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at FROM history ORDER BY id DESC")) {
            out.println("<table><tr><th>ID</th><th>Expression</th><th>Result</th><th>Time</th></tr>");
            int rows = 0;
            while (rs.next()) {
                rows++;
                String e = rs.getString("expression").replace("*", "\u00d7").replace("/", "\u00f7");
                out.println("<tr><td>" + rs.getInt("id") + "</td><td>" + esc(e) + "</td><td>"
                        + esc(fmt(rs.getDouble("result"))) + "</td><td>"
                        + esc(rs.getString("created_at")) + "</td></tr>");
            }
            if (rows == 0) out.println("<tr><td colspan='4'>No calculations yet.</td></tr>");
            out.println("</table>");
        } catch (SQLException e) {
            out.println("<p style='color:#b91c1c'>Could not read database: " + esc(e.getMessage()) + "</p>");
        }
        out.println("<form method='post' action='/'><input type='hidden' name='action' value='clear'>"
                + "<button class='clear' type='submit'>Clear history</button></form></div>");

        out.println("<script>" + JS + "</script></body></html>");
    }

    private static String key(String value, String label, String cls) {
        return "<button type='button' class='" + cls + "' data-k='" + value + "'>" + label + "</button>";
    }

    /** 5.0 -> "5", 0.1+0.2 -> "0.3" (rounded to 10 decimals). */
    private static String fmt(double d) {
        BigDecimal bd = new BigDecimal(d).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.signum() == 0 ? "0" : bd.toPlainString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ------------------------------------------------------- expression parser

    /** Evaluates + - * / % ( ) and unary minus with correct precedence. */
    private static class Parser {
        private final String s;
        private int p = 0;

        Parser(String s) { this.s = s; }

        double parse() {
            double v = expr();
            if (p < s.length()) throw new IllegalArgumentException("Unexpected '" + s.charAt(p) + "'");
            return v;
        }

        private double expr() {
            double v = term();
            while (p < s.length()) {
                char c = s.charAt(p);
                if (c == '+') { p++; v += term(); }
                else if (c == '-') { p++; v -= term(); }
                else break;
            }
            return v;
        }

        private double term() {
            double v = factor();
            while (p < s.length()) {
                char c = s.charAt(p);
                if (c == '*') { p++; v *= factor(); }
                else if (c == '/') {
                    p++;
                    double d = factor();
                    if (d == 0) throw new ArithmeticException("Cannot divide by zero");
                    v /= d;
                } else if (c == '%') {
                    p++;
                    double d = factor();
                    if (d == 0) throw new ArithmeticException("Cannot take modulo by zero");
                    v %= d;
                } else break;
            }
            return v;
        }

        private double factor() {
            if (p >= s.length()) throw new IllegalArgumentException("Incomplete expression");
            char c = s.charAt(p);
            if (c == '-') { p++; return -factor(); }
            if (c == '+') { p++; return factor(); }
            if (c == '(') {
                p++;
                double v = expr();
                if (p >= s.length() || s.charAt(p) != ')') throw new IllegalArgumentException("Missing )");
                p++;
                return v;
            }
            int start = p;
            while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;
            if (start == p) throw new IllegalArgumentException("Unexpected '" + c + "'");
            try {
                return Double.parseDouble(s.substring(start, p));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number");
            }
        }
    }
}
