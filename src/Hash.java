public class Hash {
    private static final int ROUNDS = 8;               // NAUJA: buvo 4
    private static final int STEPS = ROUNDS * 8;       // 64 žingsniai

    private static final int[] INIT = {
            0x17859D00,
            0x2468ACE0,
            0x1A2B3C4D,
            0x77777777,
            0x89ABCDEF,
            0x11203040,
            0xBEEFCA80,
            0x12578950
    };

    // NAUJA: raundų konstantos (pirmų 64 pirminių skaičių kubinių šaknų trupmeninės dalys)
    private static final int[] K = new int[STEPS];
    static {
        int n = 0;
        for (int c = 2; n < STEPS; c++) {
            boolean prime = true;
            for (int d = 2; d * d <= c; d++) {
                if (c % d == 0) { prime = false; break; }
            }
            if (!prime) continue;
            K[n++] = (int) (long) ((Math.cbrt(c) % 1.0) * 4294967296.0);
        }
    }

    public static String hash(byte[] data) {
        byte[] padded = pad(data);
        int[] h = INIT.clone();
        int[] saved = new int[8];
        int[] w = new int[STEPS];                      // NAUJA: message schedule, naudojamas iš naujo

        for (int block = 0; block < padded.length; block += 32) {

            System.arraycopy(h, 0, saved, 0, 8);

            // NAUJA: message schedule (8 žodžiai -> 64)
            for (int i = 0; i < 8; i++) {
                w[i] = readWord(padded, block + i * 4);
            }
            for (int t = 8; t < STEPS; t++) {
                w[t] = w[t - 8] + smallSigma0(w[t - 7]) + w[t - 3] + smallSigma1(w[t - 2]);
            }

            // pradinio "h ^= words" nebėra: žinutė dabar įterpiama kiekviename žingsnyje per w[t]
            for (int round = 0; round < ROUNDS; round++) {
                for (int i = 0; i < 8; i++) {
                    int t = round * 8 + i;
                    int next = h[(i + 1) & 7];
                    int previous = h[(i + 7) & 7];
                    int opposite = h[(i + 4) & 7];     // NAUJA: trečias žodis Ch/Maj funkcijoms

                    int x = h[i];
                    x += next;
                    x ^= bigSigma1(previous);
                    x ^= smallSigma0(x);
                    x += smallSigma1(next);
                    x ^= bigSigma0(previous);

                    // NAUJA: netiesiškumas + raundo konstanta + žinutės žodis
                    x += ch(previous, next, opposite);
                    x ^= maj(previous, next, opposite);
                    x += K[t] + w[t];

                    h[i] = x;
                }
            }

            for (int i = 0; i < 8; i++) {
                h[i] += saved[i];
            }
        }
        return toHex(h);
    }

    // NAUJA: netiesinės funkcijos
    static int ch(int x, int y, int z) {
        return (x & y) ^ (~x & z);
    }

    static int maj(int x, int y, int z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    static int bigSigma0(int x) {
        return Integer.rotateRight(x, 2) ^ Integer.rotateRight(x, 13) ^ Integer.rotateRight(x, 22);
    }

    static int bigSigma1(int x) {
        return Integer.rotateRight(x, 6) ^ Integer.rotateRight(x, 11) ^ Integer.rotateRight(x, 25);
    }

    static int smallSigma0(int x) {
        return Integer.rotateRight(x, 7) ^ Integer.rotateRight(x, 18) ^ (x >>> 3);
    }

    static int smallSigma1(int x) {
        return Integer.rotateRight(x, 17) ^ Integer.rotateRight(x, 19) ^ (x >>> 10);
    }

    static byte[] pad(byte[] data) {
        int newLength = ((data.length + 1 + 8 + 31) / 32) * 32;

        byte[] padded = new byte[newLength];
        System.arraycopy(data, 0, padded, 0, data.length);
        padded[data.length] = (byte) 0x80;

        long bitLength = (long) data.length << 3;
        for (int i = 0; i < 8; i++) {
            padded[newLength - 1 - i] = (byte) (bitLength >>> (8 * i));
        }
        return padded;
    }

    static int readWord(byte[] p, int off) {
        return ((p[off] & 0xFF) << 24)
                | ((p[off + 1] & 0xFF) << 16)
                | ((p[off + 2] & 0xFF) << 8)
                | (p[off + 3] & 0xFF);
    }

    static String toHex(int[] h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.length; i++) {
            sb.append(String.format("%08x", h[i]));
        }
        return sb.toString();
    }
}