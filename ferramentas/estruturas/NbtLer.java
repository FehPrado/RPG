import java.io.*;
import java.util.zip.GZIPInputStream;

/** Mostra um .nbt (gzip) como texto, para entender o formato dos moldes do jogo. */
public class NbtLer {
    public static void main(String[] a) throws Exception {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(a[0]))))) {
            int t = in.readByte(); in.readUTF();
            StringBuilder sb = new StringBuilder();
            ler(in, t, sb, 0);
            String s = sb.toString();
            int lim = a.length > 1 ? Integer.parseInt(a[1]) : 4000;
            System.out.println(s.length() > lim ? s.substring(0, lim) + "..." : s);
        }
    }
    static void ler(DataInputStream in, int t, StringBuilder sb, int d) throws IOException {
        switch (t) {
            case 1 -> sb.append(in.readByte()).append('b');
            case 2 -> sb.append(in.readShort()).append('s');
            case 3 -> sb.append(in.readInt());
            case 4 -> sb.append(in.readLong()).append('L');
            case 5 -> sb.append(in.readFloat()).append('f');
            case 6 -> sb.append(in.readDouble()).append('d');
            case 7 -> { int n = in.readInt(); in.skipBytes(n); sb.append("[B;" + n + "]"); }
            case 8 -> sb.append('"').append(in.readUTF()).append('"');
            case 9 -> { int et = in.readByte(); int n = in.readInt(); sb.append('['); for (int i = 0; i < n; i++) { if (i > 0) sb.append(','); ler(in, et, sb, d + 1); } sb.append(']'); }
            case 10 -> { sb.append('{'); boolean f = true; while (true) { int et = in.readByte(); if (et == 0) break; if (!f) sb.append(','); f = false; sb.append(in.readUTF()).append(':'); ler(in, et, sb, d + 1); } sb.append('}'); }
            case 11 -> { int n = in.readInt(); sb.append("[I;"); for (int i = 0; i < n; i++) { if (i > 0) sb.append(','); sb.append(in.readInt()); } sb.append(']'); }
            case 12 -> { int n = in.readInt(); sb.append("[L;"); for (int i = 0; i < n; i++) { if (i > 0) sb.append(','); sb.append(in.readLong()); } sb.append(']'); }
            default -> throw new IOException("tag " + t);
        }
    }
}
