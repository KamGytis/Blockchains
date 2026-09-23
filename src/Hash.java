public class Hash {
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

    public static String hash(byte[] data) {
        byte[] padded = pad(data);
        int[] h = INIT.clone();

        for (int block = 0; block < padded.length; block += 32) {

            int [] words = new int[8];

            for (int i = 0; i < 8; i++) {
                words[i] = readWord(padded, block + i *4);
            }

            for (int i = 0; i < 8; i++) {
                h[i] ^= words[i];
            }

            for (int round = 0; round < 4; round++){
                for (int i = 0; i < 8; i++){
                    int next = h[(i + 1) % 8];
                    int previous = h[(i +7) % 8];

                    h[i] += next;
                    h[i] ^= Integer.rotateLeft(previous, 7);
                    h[i] ^= Integer.rotateLeft(h[i], 2);
                    h[i] += Integer.rotateLeft(next, 2);
                    h[i] ^= Integer.rotateRight(previous, 15);
                }
            }
        }
        return toHex(h);
    }

    static byte[] pad(byte[] data) {
        int newLength = ((data.length + 1 + 31) / 32) * 32;

        byte[] padded = new byte[newLength];

        System.arraycopy(data, 0, padded, 0, data.length);

        padded[data.length] = (byte) 0x80;

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

    public static void main(String[] args) { //testing function
        System.out.println(toHex(new int[8]));

        System.out.println(hash("hello".getBytes()));
        System.out.println(hash("hello hello".getBytes()));
    }
}