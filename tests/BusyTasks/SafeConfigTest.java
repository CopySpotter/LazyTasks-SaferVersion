package BusyTasks;
import java.nio.file.*;
import java.util.*;
import java.nio.file.attribute.PosixFilePermissions;

public class SafeConfigTest {
    static void verify(boolean result, String message) { if (!result) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory("bt-safe-test-");
        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", home.toString());
            Properties input = new Properties();
            input.setProperty("length", "390");
            input.setProperty("layout", "2");
            input.setProperty("enablePolicyKit", "false");
            SafeConfig.save(input);
            verify(Files.exists(SafeConfig.path()), "config not saved");
            verify(PosixFilePermissions.toString(Files.getPosixFilePermissions(SafeConfig.path())).equals("rw-------"), "config permissions");
            verify(PosixFilePermissions.toString(Files.getPosixFilePermissions(SafeConfig.path().getParent())).equals("rwx------"), "folder permissions");
            Properties loaded = SafeConfig.load();
            verify(SafeConfig.integer(loaded,"length", 1, 10,1500)==390,"config roundtrip");
            verify(SafeConfig.integer(loaded,"length", 1, 400,1500)==1,"range validation");
            verify(!loaded.containsKey("password"),"password leaked");
            verify(Files.readString(SafeConfig.path()).contains("enablePolicyKit=false"),"no opt-in default");
            Path previous = SafeConfig.path();
            Files.delete(previous);
            Path outside = home.resolve("outside"); Files.writeString(outside,"not a config");
            Files.createSymbolicLink(previous, outside);
            boolean rejected=false;
            try { SafeConfig.load(); } catch (java.io.IOException expected) { rejected=true; }
            verify(rejected,"symlink must be refused on read");
            rejected=false;
            try { SafeConfig.save(input); } catch (java.io.IOException expected) { rejected=true; }
            verify(rejected,"symlink must be refused on write");
            System.out.println("PASS SafeConfig: permissions, roundtrip, bounds, symlink refusal, no password");
        } finally {
            System.setProperty("user.home",oldHome);
            // Disposable test temp directory; does not touch actual home.
        }
    }
}
