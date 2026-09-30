import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 6. Lavinos efektas.
 *
 * 100 000 poru is viso, po LYGIAI 25 000 keturiems ilgiams
 * {10, 100, 500, 1000} (baitu; naudojami spausdinami ASCII simboliai
 * 0x20-0x7E, todel "vienas simbolis" == "vienas baitas" - sis pasirinkimas
 * pasirinktas samoningai, kad rezultatas butu nedviprasmiskas: keiciant
 * daugiabaicius UTF-8 simbolius pasikeistu ne tik reiksme, bet ir baitu
 * skaicius, o tai iskraipytu "vieno simbolio pakeitimo" rezultata).
 *
 * Kiekvienai porai keiciamas lygiai vienas simbolis atsitiktineje
 * pozicijoje. Skaiciuojami tiek bitu, tiek hex simboliu skirtumai.
 * Bitai lyginami dekodavus hex i baitus (ne lyginant pacius hex simbolius
 * kaip tekstą).
 */
public class Avalanchetest {

    static final int[] LENGTHS = { 10, 100, 500, 1000 };
    static final int PAIRS_PER_LENGTH = 25_000;
    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,-!?";

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // bendra (visu ilgiu) statistika
        long overallSumBits = 0;
        int overallMinBits = Integer.MAX_VALUE, overallMaxBits = Integer.MIN_VALUE;
        long overallSumHex = 0;
        int overallMinHex = Integer.MAX_VALUE, overallMaxHex = Integer.MIN_VALUE;
        int[] overallHistogram = new int[257];
        int totalPairs = 0;

        System.out.println("%-8s %-8s %-8s %-10s | %-8s %-8s %-8s".formatted(
                "Ilgis", "MinBit", "MaxBit", "VidBit", "MinHex", "MaxHex", "VidHex"));

        for (int len : LENGTHS) {
            Random rnd = new Random(500 + len);
            long sumBits = 0;
            int minBits = Integer.MAX_VALUE, maxBits = Integer.MIN_VALUE;
            long sumHex = 0;
            int minHex = Integer.MAX_VALUE, maxHex = Integer.MIN_VALUE;

            for (int i = 0; i < PAIRS_PER_LENGTH; i++) {
                char[] chars = randomChars(rnd, len);
                int pos = rnd.nextInt(len);
                char original = chars[pos];
                char replacement;
                do {
                    replacement = ALPHABET.charAt(rnd.nextInt(ALPHABET.length()));
                } while (replacement == original);

                byte[] a = new String(chars).getBytes(StandardCharsets.US_ASCII);
                chars[pos] = replacement;
                byte[] b = new String(chars).getBytes(StandardCharsets.US_ASCII);

                String ha = Hash.hash(a);
                String hb = Hash.hash(b);

                int bitDiff = hammingDistanceBits(ha, hb);
                int hexDiff = hammingDistanceHexChars(ha, hb);

                sumBits += bitDiff;
                if (bitDiff < minBits) minBits = bitDiff;
                if (bitDiff > maxBits) maxBits = bitDiff;

                sumHex += hexDiff;
                if (hexDiff < minHex) minHex = hexDiff;
                if (hexDiff > maxHex) maxHex = hexDiff;

                overallHistogram[bitDiff]++;
            }

            double meanBits = sumBits / (double) PAIRS_PER_LENGTH;
            double meanHex = sumHex / (double) PAIRS_PER_LENGTH;

            System.out.println("%-8d %-8d %-8d %-10.3f | %-8d %-8d %-8.3f".formatted(
                    len, minBits, maxBits, meanBits, minHex, maxHex, meanHex));

            overallSumBits += sumBits;
            if (minBits < overallMinBits) overallMinBits = minBits;
            if (maxBits > overallMaxBits) overallMaxBits = maxBits;
            overallSumHex += sumHex;
            if (minHex < overallMinHex) overallMinHex = minHex;
            if (maxHex > overallMaxHex) overallMaxHex = maxHex;
            totalPairs += PAIRS_PER_LENGTH;
        }

        double overallMeanBits = overallSumBits / (double) totalPairs;
        double overallMeanHex = overallSumHex / (double) totalPairs;

        System.out.println("\n BENDRAI (visi " + totalPairs + " poru) ");
        System.out.println("Bitu skirtumas: min=" + overallMinBits + " max=" + overallMaxBits
                + " vidurkis=" + String.format("%.3f", overallMeanBits) + " (idealu 128.0 is 256)");
        System.out.println("Hex simboliu skirtumas: min=" + overallMinHex + " max=" + overallMaxHex
                + " vidurkis=" + String.format("%.3f", overallMeanHex) + " (idealu ~60 is 64, nes hex simbolis"
                + " turi 16 reiksmiu ir tikimybe sutapti su originaliu ~1/16)");

        System.out.println("\nBitu skirtumo histograma (visi ilgiai kartu, grupuota po 8 bitus):");
        for (int bucketStart = 88; bucketStart <= 168; bucketStart += 8) {
            int count = 0;
            for (int d = bucketStart; d < bucketStart + 8 && d <= 256; d++) count += overallHistogram[d];
            System.out.println("  [" + bucketStart + "-" + (bucketStart + 7) + "]: " + bar(count, totalPairs));
        }
    }

    static String bar(int count, int total) {
        int width = (int) Math.round(50.0 * count / total * 8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(width, 60); i++) sb.append('#');
        return sb + " (" + count + ")";
    }

    static char[] randomChars(Random rnd, int len) {
        char[] c = new char[len];
        for (int i = 0; i < len; i++) c[i] = ALPHABET.charAt(rnd.nextInt(ALPHABET.length()));
        return c;
    }

    // bitu skirtumas - dekoduojame hex i baitus ir lyginame bitus
    static int hammingDistanceBits(String hexA, String hexB) {
        byte[] ba = hexToBytes(hexA);
        byte[] bb = hexToBytes(hexB);
        int dist = 0;
        for (int i = 0; i < ba.length; i++) {
            dist += Integer.bitCount((ba[i] ^ bb[i]) & 0xFF);
        }
        return dist;
    }

    // hex simboliu skirtumas - kiek is 64 hex simboliu poziciju skiriasi
    static int hammingDistanceHexChars(String hexA, String hexB) {
        int dist = 0;
        for (int i = 0; i < hexA.length(); i++) {
            if (hexA.charAt(i) != hexB.charAt(i)) dist++;
        }
        return dist;
    }

    static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}