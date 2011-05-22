import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import resources.MemoryFile;
import resources.ParserVersion;
import win7.Utils;

public class Find {

    static File zipDir;
    static File outDir;

    static Map<String, String> files;

    public static void main(String[] args) throws Exception {
        Assert.assertEquals("Execute: Find <fromdir> <todir>", 2, args.length);

        zipDir = new File(args[0]);
        outDir = new File(args[1]);
        FileUtils.deleteDirectory(outDir);

        loadNeed();

        File[] zips = zipDir.listFiles(new FileFilter() {
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

                String outSubDir = null;
                for (String r : files.keySet()) {
                    if (ze.getName().endsWith("/" + r)) {
                        outSubDir = files.get(r);
                        break;
                    }
                }
                if (outSubDir != null) {
                    byte[] data = Utils.readZip(zip, ze);
                    processFile(ze.getName(), outSubDir, data);
                }
            }
        }
    }

    protected static void loadNeed() throws Exception {
        File need = new File(zipDir, "need-mui.txt");
        files = new HashMap<String, String>();
        for (String f : (List<String>) FileUtils.readLines(need, "UTF-8")) {
            if (f.endsWith("###")) {
                continue;
            }
            int pos = f.lastIndexOf('/');
            String dir = f.substring(0, pos + 1);
            String file = f.substring(pos + 1);
            Assert.assertTrue(pos > 0);
            Assert.assertNull(f, files.get(file));
            files.put(file, dir);
        }
    }

    protected static void processFile(String zipPath, String outSubDir, byte[] data) throws Exception {
        ReaderWriterMUI mui = new ReaderWriterMUI(data);
        String version = getVersion(data) + '_' + mui.readArch();
        System.out.println("  " + zipPath + ": " + version);

        String fn = zipPath.replaceAll(".+/", "");
        fn = fn.replaceAll("(\\.mui)$", '.' + version + "$1");
        File out = new File(outDir, outSubDir + fn);
        if (out.exists()) {
            byte[] exist = FileUtils.readFileToByteArray(out);
            Assert.assertArrayEquals(exist, data);
        } else {
            FileUtils.writeByteArrayToFile(out, data);
        }
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
