import java.io.DataOutputStream;
import java.io.IOException;

/** SNBT bem simples (compostos, textos entre aspas, inteiros e bytes com b) para o NBT dos blocos. */
public class Snbt {
    private final String s;
    private int i;

    private Snbt(String s) { this.s = s; }

    /** Escreve o conteúdo de um composto (sem o tipo e o nome, que quem chama já escreveu). */
    static void escrever(DataOutputStream o, String snbt) throws IOException {
        Snbt p = new Snbt(snbt.trim());
        p.composto(o);
    }

    private void esp() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private void composto(DataOutputStream o) throws IOException {
        esp();
        if (s.charAt(i) != '{') throw new IOException("esperava { em " + i + ": " + s);
        i++;
        esp();
        while (s.charAt(i) != '}') {
            int a = i;
            while (s.charAt(i) != ':') i++;
            String k = s.substring(a, i).trim();
            i++;
            esp();
            valor(o, k);
            esp();
            if (s.charAt(i) == ',') { i++; esp(); }
        }
        i++;
        o.writeByte(0);
    }

    private void valor(DataOutputStream o, String k) throws IOException {
        char c = s.charAt(i);
        if (c == '"') {
            int a = ++i;
            while (s.charAt(i) != '"') i++;
            o.writeByte(8); o.writeUTF(k); o.writeUTF(s.substring(a, i));
            i++;
        } else if (c == '{') {
            o.writeByte(10); o.writeUTF(k);
            composto(o);
        } else {
            int a = i;
            while (i < s.length() && ",}".indexOf(s.charAt(i)) < 0) i++;
            String v = s.substring(a, i).trim();
            if (v.endsWith("b")) { o.writeByte(1); o.writeUTF(k); o.writeByte(Byte.parseByte(v.substring(0, v.length() - 1))); }
            else if (v.endsWith("f")) { o.writeByte(5); o.writeUTF(k); o.writeFloat(Float.parseFloat(v.substring(0, v.length() - 1))); }
            else { o.writeByte(3); o.writeUTF(k); o.writeInt(Integer.parseInt(v)); }
        }
    }
}
