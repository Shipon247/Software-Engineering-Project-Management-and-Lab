package calc;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

/** Starts an embedded Tomcat on port 8080 with the calculator and the Math Whiz game. */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Path baseDir = Files.createTempDirectory("tomcat-calc");
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir.toString());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", baseDir.toString());

        Wrapper calc = Tomcat.addServlet(ctx, "calculator", new CalculatorServlet());
        calc.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/", "calculator");

        Wrapper game = Tomcat.addServlet(ctx, "game", new GameServlet());
        game.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/game", "game");

        tomcat.start();
        System.out.println("=====================================================");
        System.out.println(" Calculator : http://localhost:" + port + "/");
        System.out.println(" Math Whiz  : http://localhost:" + port + "/game");
        System.out.println(" Press Ctrl+C (or the red Stop button) to quit.");
        System.out.println("=====================================================");
        tomcat.getServer().await();
    }
}
