package BusyTasks;
import java.io.*;
import java.lang.reflect.*;
public class DetailsFilterTest {
    static String next(String input, int mode, int pid) throws Exception {
        Method m=BusyTasks.class.getDeclaredMethod("naechsteDetailzeile", BufferedReader.class, int.class, int.class);
        m.setAccessible(true);
        return (String)m.invoke(null,new BufferedReader(new StringReader(input)),mode,pid);
    }
    static void eq(String a, String b) { if (a==null ? b!=null : !a.equals(b)) throw new AssertionError("expected '"+b+"', got '"+a+"'"); }
    public static void main(String[] args) throws Exception {
        eq(next("0000000-1111 r--p 0 /usr/bin/a\n1111-2222 rw-p 0 [heap]\n",1,42),"1111-2222 rw-p 0 [heap]");
        eq(next("0000-1111 rw-p 0 /usr/bin/a\n",10,42),"/usr/bin/a");
        eq(next("rchar: 5\nread_bytes: 10\nwrite_bytes: 20\n",2,42),"read_bytes: 10");
        eq(next("lrwxrwxrwx 1 u g 2 -> /tmp/a\n",20,42),"/tmp/a");
        eq(next("a b c d e pid=4,\na b c d e pid=42,\n",3,42),"a b c d e pid=42,");
        System.out.println("PASS Detail filters: maps, IO, FD, ss");
    }
}
