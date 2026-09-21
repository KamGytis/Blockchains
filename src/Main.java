import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    private static final boolean INCLUDE_NEWLINE = false;

    public static void main(String[] args) {
        byte[] data;

        if (args.length > 0) {
            System.out.println("Rezimas: FAILAS (" + args[0] + ")");
            data = readFile(args[0]);
        } else {
            System.out.println("Rezimas: RANKINIS IVEDIMAS");
            data = readManual();
        }

        System.out.println("Baitu skaicius: " + data.length);
        System.out.println("Maisa: " + computeHash(data));
    }

    private static byte[] readFile(String pathText) {
        try {
            return Files.readAllBytes(Path.of(pathText));
        } catch (InvalidPathException e) {
            System.err.println("Klaida: netinkamas kelias: " + pathText);
        } catch (IOException e) {
            System.err.println("Klaida: nepavyko perskaityti failo '" + pathText + "': " + e);
        }
        System.exit(1);
        return null; // nepasiekiama, bet kompiliatoriui reikia
    }

    private static byte[] readManual() {
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("Iveskite teksta: ");

        String text = "";
        if (in.hasNextLine()) {
            text = in.nextLine();
        } else {
            System.err.println("Pastaba: ivestis tuscia (nera eilutes).");
        }

        if (INCLUDE_NEWLINE) {
            text += "\n";
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String computeHash(byte[] data) {
        return "TODO (gauta " + data.length + " baitu)";
    }
}