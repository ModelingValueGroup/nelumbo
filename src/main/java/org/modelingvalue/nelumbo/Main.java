package org.modelingvalue.nelumbo;

import java.util.Scanner;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;

public final class Main {

    private final static String READ  = "nelumbo: ";
    private final static String WRITE = " result: ";
    private final static String ERROR = "  error: ";

    private Main() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");
        KnowledgeBase.run(() -> {
            try {
                Parser.parse(Integer.class);
            } catch (ParseException e) {
                System.err.println(ERROR + e.getMessage());
            }
            System.out.print(READ);
            String line = scanner.next();
            while (line != null) {
                try {
                    for (Node root : Parser.parse(line)) {
                        if (root.type() == Type.RESULT) {
                            System.out.println(WRITE + root.toString(2));
                        }
                    }
                } catch (ParseException e) {
                    System.err.println(ERROR + e.getMessage());
                }
                System.out.print(READ);
                line = scanner.next();
            }
        });
    }

}
