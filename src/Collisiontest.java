import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;

/**
 * 5. Kolizijos.
 *
 * Kiekvienam fiksuotam ilgiui L is {10, 100, 500, 1000} (baitu):
 *     sugeneruojama 100 000 poru (t.y. 200 000 atskiru ivesciu);
 *     patikranamos pacios poros (hash(a_i) == hash(b_i)?);
 *     patikrinama visas to ilgio rinkinys (visos 200 000 ivestys sudetos
 *     i viena HashMap pagal maisos reiksme - bet kurios dvi is skirtingu
 *     poru taip pat gali susikirsti, ne tik poros viduje);
 *     papildomai isbandomi strukturuoti (ne atsitiktiniai) atvejai;
 *     kolizija uzskaitoma tik jei du skirtingi baitu masyvai duoda ta
 *     pacia maisa (jei atsitiktinai sugeneruojami du vienodi masyvai,
 *     tai ne kolizija, o tiesiog dublikatas).
 */
public class Collisiontest {

    static final int PAIRS_PER_LENGTH = 100_000;
    static final int[] LENGTHS = { 10, 100, 500, 1000 };

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        for (int len : LENGTHS) {
            runForLength(len);
        }
    }

    static void runForLength(int len) {
        System.out.println("\n Ilgis = " + len + " baitu ");
        Random rnd = new Random(1000 + len);

        byte[][] a = new byte[PAIRS_PER_LENGTH][];
        byte[][] b = new byte[PAIRS_PER_LENGTH][];
        String[] ha = new String[PAIRS_PER_LENGTH];
        String[] hb = new String[PAIRS_PER_LENGTH];

        long genStart = System.nanoTime();
        for (int i = 0; i < PAIRS_PER_LENGTH; i++) {
            a[i] = randomBytes(rnd, len);
            b[i] = randomBytes(rnd, len);
            ha[i] = Hash.hash(a[i]);
            hb[i] = Hash.hash(b[i]);
        }
        long genMs = (System.nanoTime() - genStart) / 1_000_000;
        System.out.println("Sugeneruota ir sumaisyta " + (2 * PAIRS_PER_LENGTH) + " ivesciu per " + genMs + " ms");

        //  1) Poru patikra: a_i prieš b_i
        int pairCollisions = 0;
        int pairDuplicates = 0;
        for (int i = 0; i < PAIRS_PER_LENGTH; i++) {
            boolean sameBytes = java.util.Arrays.equals(a[i], b[i]);
            boolean sameHash = ha[i].equals(hb[i]);
            if (sameHash && !sameBytes) {
                pairCollisions++;
                System.out.println("  !!! kolizija poroje #" + i + ": " + ha[i]);
            } else if (sameHash && sameBytes) {
                pairDuplicates++; // atsitiktinai sugeneruotas identiskas baitu masyvas - ne kolizija
            }
        }
        System.out.println("Poru patikra: kolizju rasta = " + pairCollisions
                + ", atsitiktiniu dublikatu (ne kolizija) = " + pairDuplicates);

        //  2) Viso rinkinio patikra (visos 200000 ivestys kartu)
        HashMap<String, byte[]> seen = new HashMap<>(PAIRS_PER_LENGTH * 3);
        int setCollisions = 0;
        int setDuplicates = 0;
        for (int i = 0; i < PAIRS_PER_LENGTH; i++) {
            setCollisions += checkAndInsert(seen, ha[i], a[i]);
            setCollisions += checkAndInsert(seen, hb[i], b[i]);
        }
        System.out.println("Viso rinkinio (" + (2 * PAIRS_PER_LENGTH) + " ivesciu) patikra: kolizju rasta = "
                + setCollisions + " (unikaliu maisu rinkinyje: " + seen.size() + ")");

        //  3) Struktūruoti atvejai
        System.out.println("Struktūruoti atvejai (ilgiui " + len + "):");
        structuredCase(len, "visi nuliai vs visi 0xFF", allBytes(len, (byte) 0x00), allBytes(len, (byte) 0xFF));
        structuredCase(len, "paskutinis baitas +1", incrementLast(allBytes(len, (byte) 0x00)), allBytes(len, (byte) 0x00));
        byte[] base = randomBytes(new Random(42), len);
        byte[] oneBitFlipped = base.clone();
        oneBitFlipped[0] ^= 0x01;
        structuredCase(len, "baze vs 1 apverstas bitas pirmame baite", base, oneBitFlipped);
        if (len >= 2) {
            byte[] swapped = base.clone();
            byte tmp = swapped[0];
            swapped[0] = swapped[len - 1];
            swapped[len - 1] = tmp;
            structuredCase(len, "baze vs sukeisti pirmas/paskutinis baitas", base, swapped);
        }
        byte[] reversed = reverse(base);
        structuredCase(len, "baze vs apverstos baitu tvarkos (reverse)", base, reversed);
    }

    static int checkAndInsert(HashMap<String, byte[]> map, String hash, byte[] data) {
        byte[] prev = map.get(hash);
        if (prev == null) {
            map.put(hash, data);
            return 0;
        }
        if (!java.util.Arrays.equals(prev, data)) {
            System.out.println("  kolizija viso rinkinio patikroje: " + hash);
            return 1;
        }
        return 0; // tas pats masyvas (dublikatas), ne kolizija
    }

    static void structuredCase(int len, String label, byte[] x, byte[] y) {
        String hx = Hash.hash(x);
        String hy = Hash.hash(y);
        boolean sameBytes = java.util.Arrays.equals(x, y);
        boolean collision = hx.equals(hy) && !sameBytes;
        System.out.println("  [" + label + "] " + (collision ? "kolizija" : "skirtingos maisos (kaip tikėtasi)"));
    }

    static byte[] randomBytes(Random rnd, int len) {
        byte[] b = new byte[len];
        rnd.nextBytes(b);
        return b;
    }

    static byte[] allBytes(int len, byte value) {
        byte[] b = new byte[len];
        java.util.Arrays.fill(b, value);
        return b;
    }

    static byte[] incrementLast(byte[] src) {
        byte[] b = src.clone();
        b[b.length - 1] += 1;
        return b;
    }

    static byte[] reverse(byte[] src) {
        byte[] b = new byte[src.length];
        for (int i = 0; i < src.length; i++) b[i] = src[src.length - 1 - i];
        return b;
    }
}