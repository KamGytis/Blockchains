import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 4. Sparta.
 *
 * Dydziai: 1, 2, 4, 8, ... failo eiluciu ir visas failas (dvejetaine
 * progresija eiluciu skaiciumi, plius papildomas taskas - visas failas,
 * jei jo eiluciu skaicius nera tiksli dvejeto laipsnio reiksme).
 *
 * Kiekvienam dydziui: apsilimas (warm-up, neivertintas i statistika),
 * po to BENT 5 laiko matavimai, skaiciuojami TIK aplink Hash.hash() -
 * duomenys is failo nuskaitomi IS ANKSTO (be I/O laiko matavimo viduje).
 */
public class Benchmark {

    static final Path FILE = Path.of("speed_test_data.txt");
    static final int TOTAL_LINES = 200_000; // ne dvejeto laipsnis -> "visas failas" yra atskiras taskas
    static final int LINE_LEN = 80; // simboliu viena eiluteje (be \n)

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        generateFileIfMissing();

        List<byte[]> allLines = readAllLinesAsBytes();
        System.out.println("Sugeneruotas testo failas: " + FILE.toAbsolutePath()
                + " (" + allLines.size() + " eiluciu)");

        List<Integer> lineCounts = new ArrayList<>();
        int n = 1;
        while (n < allLines.size()) {
            lineCounts.add(n);
            n *= 2;
        }
        lineCounts.add(allLines.size()); // "ir visas failas"

        System.out.println("\n%-12s %-10s %-14s %-14s".formatted("Eiluciu", "Baitu", "Vidurkis(ms)", "Sklaida(ms)"));

        List<long[]> chartData = new ArrayList<>(); // [bytes, meanNanos]

        for (int lines : lineCounts) {
            byte[] data = concatFirstNLines(allLines, lines); // I/O ir sujungimas VISADA pries matavima

            // apsilimas - keli neivertinti kvietimai (JIT, klases ikelimas ir pan.)
            for (int w = 0; w < 5; w++) {
                Hash.hash(data);
            }

            int repeats = 7; // >= 5 reikalaujama
            long[] samplesNanos = new long[repeats];
            for (int r = 0; r < repeats; r++) {
                long start = System.nanoTime();
                Hash.hash(data);              // MATUOJAMA TIK SITA EILUTE
                long end = System.nanoTime();
                samplesNanos[r] = end - start;
            }

            double meanNanos = mean(samplesNanos);
            double stddevNanos = stddev(samplesNanos, meanNanos);

            System.out.printf("%-12d %-10d %-14.3f %-14.3f%n",
                    lines, data.length, meanNanos / 1_000_000.0, stddevNanos / 1_000_000.0);

            chartData.add(new long[] { data.length, Math.round(meanNanos) });
        }

        // CSV, kad butu patogu daryti grafika kitur / importuoti
        StringBuilder csv = new StringBuilder("baitai,vidurkis_ms,sklaida_ms\n");
        for (long[] row : chartData) {
            csv.append(row[0]).append(",").append(row[1] / 1_000_000.0).append("\n");
        }
        Files.writeString(Path.of("speed_results.csv"), csv.toString(), StandardCharsets.UTF_8);
        System.out.println("\nCSV rezultatai issaugoti: speed_results.csv");
    }

    static void generateFileIfMissing() throws Exception {
        if (Files.exists(FILE)) return;
        Random rnd = new Random(123); // fiksuota seka - determinuotas testo failas
        StringBuilder sb = new StringBuilder();
        String alphabet = "abcdefghijklmnopqrstuvwxyzACDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
        for (int i = 0; i < TOTAL_LINES; i++) {
            for (int c = 0; c < LINE_LEN; c++) {
                sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            sb.append('\n');
        }
        Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
    }

    static List<byte[]> readAllLinesAsBytes() throws Exception {
        List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
        List<byte[]> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add((line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return result;
    }

    static byte[] concatFirstNLines(List<byte[]> lines, int n) {
        int total = 0;
        for (int i = 0; i < n; i++) total += lines.get(i).length;
        byte[] out = new byte[total];
        int pos = 0;
        for (int i = 0; i < n; i++) {
            byte[] l = lines.get(i);
            System.arraycopy(l, 0, out, pos, l.length);
            pos += l.length;
        }
        return out;
    }

    static double mean(long[] xs) {
        long sum = 0;
        for (long x : xs) sum += x;
        return sum / (double) xs.length;
    }

    static double stddev(long[] xs, double mean) {
        double sumSq = 0;
        for (long x : xs) sumSq += (x - mean) * (x - mean);
        return Math.sqrt(sumSq / xs.length);
    }
}