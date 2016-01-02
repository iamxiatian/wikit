package ruc.irm.wikit.util;

import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
            //scanner.close();
        } catch (NoSuchElementException e) {
            //End
        }
        System.out.println("Bye.");
    }

    /**
     * support multiple handler， for example:
     *  <pre>
     * loop(new Triple("user", "View User Info", handler1),
     *      new Triple("article", "View Article Info", handler2));
     * </pre>
     * @param commands
     */
    public static void loop(Triple<String, String, Handler> ...commands) {
        String prompt = ">>>";
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append("Type command to switch corresponded action, " +
                "or type exit to quit, possible commands are listed below:")
                .append("\n");

        Map<String, Handler> handlers = new HashMap<>();
        for (Triple<String, String, Handler> cmd : commands) {
            handlers.put(cmd.getLeft().toLowerCase(), cmd.getRight());
            helpMsg.append("\t").append(cmd.getLeft()).append("\t").append
                    (cmd.getMiddle()).append("\n");
        }

        //append help action to print the help information
        handlers.put("help", new Handler() {
            @Override
            public void handle(String input) throws IOException {
                System.out.println(helpMsg);
            }
        });
        helpMsg.append("\thelp\tprint this help information").append("\n");

        System.out.println(helpMsg);

        try {
            Scanner scanner = new Scanner(System.in);
            String input = null;
            System.out.print(prompt);
            while ((input = scanner.nextLine()) != null) {
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                if (handlers.containsKey(input.toLowerCase())) {
                    System.out.println("switch action to " + input);
                    loop("$", handlers.get(input.toLowerCase()));
                } else {
                    System.out.println(helpMsg);
                }
                System.out.print(prompt);
            }
            scanner.close();
        } catch (NoSuchElementException e) {
            //End
        }
        System.out.println("Bye Bye!");
    }

    public interface Handler {
        public void handle(String input) throws IOException;
    }

    public static String readLine() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}
