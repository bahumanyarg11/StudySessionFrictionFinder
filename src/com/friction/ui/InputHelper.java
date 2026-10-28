package com.friction.ui;

import com.friction.model.Priority;
import com.friction.util.DateUtil;

import java.time.LocalDate;
import java.util.Scanner;

/** Reads and validates keyboard input; keeps asking until the input is valid. */
public class InputHelper {
    private final Scanner scanner = new Scanner(System.in);

    public String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("Value cannot be empty.");
        }
    }

    /** Like readText, but re-prompts when the value is longer than the database column allows. */
    public String readText(String prompt, int maxLength) {
        while (true) {
            String line = readText(prompt);
            if (line.length() <= maxLength) return line;
            System.out.println("Please use at most " + maxLength + " characters.");
        }
    }

    public String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Like readOptional, but re-prompts when the value is longer than the database column allows. */
    public String readOptional(String prompt, int maxLength) {
        while (true) {
            String line = readOptional(prompt);
            if (line.length() <= maxLength) return line;
            System.out.println("Please use at most " + maxLength + " characters (or leave it blank).");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    /** Reads a DD-MM-YYYY date. Pressing Enter returns the default date. */
    public LocalDate readDate(String prompt, LocalDate defaultDate) {
        while (true) {
            System.out.print(prompt + " [" + DateUtil.format(defaultDate) + "]: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defaultDate;
            LocalDate date = DateUtil.parse(line);
            if (date != null) return date;
            System.out.println("Invalid date. Use DD-MM-YYYY.");
        }
    }

    public Priority readPriority(String prompt) {
        while (true) {
            try {
                return Priority.fromText(readText(prompt));
            } catch (IllegalArgumentException e) {
                System.out.println("Enter High, Medium or Low.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String line = readText(prompt).toUpperCase();
            if (line.equals("Y") || line.equals("YES")) return true;
            if (line.equals("N") || line.equals("NO")) return false;
            System.out.println("Enter Y or N.");
        }
    }
}
