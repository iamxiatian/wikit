package ruc.irm.wikit.util;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * A console loop toolkit to make loop simply.
 *
 * @author Tian Xia
 * @date Aug 12, 2015 12:55 PM
 */
public class ConsoleLoop {
    public static void loop(Handler handler) {
        loop(">>>", handler);
    }

    public static void loop(String prompt, Handler handler) {
        try {
            Scanner scanner = new Scanner(System.in);
            String input = null;
            System.out.print(prompt);
            while ((input = scanner.nextLine()) != null) {
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                try {
                    handler.handle(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.print("\n" + prompt);
            }
            scanner.close();
        } catch (NoSuchElementException e) {
            //End
        }
        System.out.println("Bye.");
    }

    public interface Handler {
        public void handle(String input) throws IOException;
    }

    public static String readLine() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}
