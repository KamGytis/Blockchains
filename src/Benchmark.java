public class Benchmark {
    public static void main(String[] args) {
        int[] sizes = {1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000};

        for (int size : sizes) {
            byte[] data = new byte[size];
            new java.util.Random(42).nextBytes(data);

            for (int i = 0; i < 3; i++) {
                Hash.hash(data);
            }

            int iterations = 5;
            long total = 0;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                Hash.hash(data);
                long end = System.nanoTime();
                total += (end - start);
            }
            long avgMillis = (total / iterations) / 1_000_000;

            System.out.println("Dydis: " + size + " baitu, vidutinis laikas: " + avgMillis + " ms");
        }
    }
}