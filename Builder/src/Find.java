import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;

import resources.MemoryFile;
import resources.ParserVersion;

import win7.Utils;

public class Find {

    static final String[] FILES_MUI = new String[] { "ieframe.dll.mui", "inetcpl.cpl.mui", "aclui.dll.mui",
            "shell32.dll.mui" };

    public static void main(String[] args) throws Exception {
        File[] zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".zip");
            }
        });
        for (File z : zips) {
            System.out.println(z);
            ZipFile zip = new ZipFile(z);
            for (Enumeration<? extends ZipEntry> zit = zip.entries(); zit.hasMoreElements();) {
                ZipEntry ze = zit.nextElement();
                if (ze.isDirectory()) {
                    continue;
                }

                boolean need = false;
                for (String r : FILES_MUI) {
                    if (ze.getName().endsWith("/" + r)) {
                        need = true;
                        break;
                    }
                }
                if (need) {
                    byte[] data = Utils.readZip(zip, ze);
                    processFile(ze.getName(), data);
                }
            }
        }
    }

    protected static void processFile(String path, byte[] data) {
        System.out.println("  " + path);
        System.out.println("      " + getVersion(data));
    }

    static final Charset UTF16 = Charset.forName("UTF-16LE");
    static final byte[] VER_SIGN = "ProductVersion".getBytes(UTF16);

    protected static String getVersion(byte[] d) {
        int pos = pos(d, ParserVersion.MARK, 0);

        Assert.assertTrue(pos > 0);

        Assert.assertEquals(-1, pos(d, ParserVersion.MARK, pos + 1));

        MemoryFile m = new MemoryFile(d);
        m.seek(pos + ParserVersion.MARK.length);
        Assert.assertEquals(0xFEEF04BDL, m.readDWord());
        Assert.assertEquals(0x10000L, m.readDWord());

        long dwFileVersionMS = m.readDWord();
        long dwFileVersionLS = m.readDWord();

        String ver = (dwFileVersionMS >> 16) + "." + (dwFileVersionMS & 0xFFFF) + "."
                + (dwFileVersionLS >> 16) + "." + (dwFileVersionLS & 0xFFFF);

        return ver;
    }

    protected static int pos(byte[] d, byte[] find, int minPos) {
        for (int i = minPos; i < d.length - find.length; i++) {
            boolean found = true;
            for (int j = 0; j < find.length; j++) {
                if (d[i + j] != find[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

}
