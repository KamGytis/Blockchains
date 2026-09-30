import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 3. Determinizmas.
 *
 *  a) Kartotiniai kvietimai tos pacios ivesties siame paleidime.
 *  b) Seka A, B, A - trecias rezultatas turi sutapti su pirmu (patikrina,
 *     kad tarp kvietimu nelieka jokios tarpines busenos, kuri veiktu
 *     veliau apskaiciuojamas maisas).
 *  c) Atskiri paleidimai - rezultatas issaugomas faile; kito paleidimo metu
 *     palyginamas su issaugotu. Paleiskite sia programa bent du kartis is
 *     eiles, kad pamatytumete "b) atskiri paleidimai" patikrinima praeinanti.
 */
public class Determinismtest {

    static final Path STATE_FILE = Path.of("determinism_state.txt");

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        byte[] A = "Determinizmo testas - ivestis A".getBytes(StandardCharsets.UTF_8);
        byte[] B = "Visiskai kitokia ivestis B (skirtingo ilgio)".getBytes(StandardCharsets.UTF_8);

        //  a) Kartotiniai kvietimai siame paleidime
        System.out.println(" a) Kartotiniai kvietimai (10x tai paciai ivesciai A) ");
        String first = Hash.hash(A);
        boolean allSame = true;
        for (int i = 0; i < 10; i++) {
            String h = Hash.hash(A);
            if (!h.equals(first)) allSame = false;
        }
        System.out.println("  Rezultatas: " + first);
        System.out.println("  Visi 10 kartotiniu kvietimu sutapo: " + allSame);

        //  b) Seka A, B, A
        System.out.println("\n b) Seka A, B, A ");
        String r1 = Hash.hash(A);
        String r2 = Hash.hash(B);
        String r3 = Hash.hash(A);
        System.out.println("  hash(A) #1: " + r1);
        System.out.println("  hash(B):    " + r2);
        System.out.println("  hash(A) #2: " + r3);
        System.out.println("  hash(A) #1 == hash(A) #2: " + r1.equals(r3)
                + " (B apskaiciavimas neturejo jokios itakos A rezultatui)");

        //  c) Atskiri paleidimai - palyginimas su ankstesniu paleidimu
        System.out.println("\n c) Atskiri JVM paleidimai ");
        String currentRun = r1; // hash(A) sio paleidimo metu
        if (Files.exists(STATE_FILE)) {
            String previousRun = Files.readString(STATE_FILE, StandardCharsets.UTF_8).trim();
            boolean matches = previousRun.equals(currentRun);
            System.out.println("  Ankstesnio paleidimo hash(A): " + previousRun);
            System.out.println("  Sio paleidimo hash(A):        " + currentRun);
            System.out.println("  Sutampa su ankstesniu paleidimu: " + matches);
            if (!matches) {
                System.out.println("   neatitikimas - determinizmas paziestas tarp atskiru paleidimu ");
            }
        } else {
            System.out.println("  (Pirmas paleidimas - issaugomas rezultatas palyginimui su kitu paleidimu.)");
        }
        Files.writeString(STATE_FILE, currentRun, StandardCharsets.UTF_8);
        System.out.println("  Issaugota i: " + STATE_FILE.toAbsolutePath());
    }
}