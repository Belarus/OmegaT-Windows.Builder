import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import resources.MemoryFile;
import resources.ParserVersion;
import win7.Utils;

/**
 * Extract:
 * 
 * 7z x '-ir!*en-US*' ../*.wim
 * 
 * 7z x '-ir!*en-us*' ../*.wim
 */
public class ExtractMUIs {
    static XMLInputFactory factory;
    static File ZIP_DIR;
    static File OUT_DIR;

    static Map<String, Integer> files;

    static Map<String, String> pathFromManifests = new HashMap<String, String>();

    public static void main(String[] args) throws Exception {
        Assert.assertEquals("Execute: ExtractMUIs <fromdir> <todir>", 2, args.length);

        factory = XMLInputFactory.newFactory();

        ZIP_DIR = new File(args[0]);
        OUT_DIR = new File(args[1]);
        FileUtils.deleteDirectory(OUT_DIR);

        loadNeed();

        File[] zips = ZIP_DIR.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".zip");
            }
        });
        List<String> updates = new ArrayList<String>();
        for (File z : zips) {
            System.out.println(z);

            ZipFile zip = new ZipFile(z);
            // load manifests
            pathFromManifests.clear();
            for (Enumeration<? extends ZipEntry> zit = zip.entries(); zit.hasMoreElements();) {
                ZipEntry ze = zit.nextElement();
                if (ze.getName().toUpperCase().endsWith(".MANIFEST")) {
                    loadManifest(ze.getName(), Utils.readZip(zip, ze));
                }
            }
            // load mui
            int extracted = 0;
            for (Enumeration<? extends ZipEntry> zit = zip.entries(); zit.hasMoreElements();) {
                ZipEntry ze = zit.nextElement();
                if (ze.getName().toUpperCase().endsWith(".MUI")) {
                    String zipPath = ze.getName();
                    String normPath = normalizePath(zipPath);
                    String requiredPath = isRequired(normPath);
                    if (requiredPath != null) {
                        byte[] data = Utils.readZip(zip, ze);
                        int c = processFile(zipPath, normPath, data);
                        files.put(requiredPath, files.get(requiredPath) + c);
                        extracted += c;
                    }
                }
            }
            updates.add("Extracted from " + z.getName() + ": " + extracted);
        }
        int errors = 0;
        for (String f : files.keySet()) {
            int count = files.get(f);
            String suffix = "";
            if (count == 0) {
                suffix = " ***************************************";
                errors++;
            }
            System.out.println(f + ": " + count + suffix);
        }
        System.out.println("===========================");
        System.out.println("Errors: " + errors);
    }

    protected static void loadNeed() throws Exception {
        File need = new File("need-mui.txt");
        files = new TreeMap<String, Integer>();
        for (String f : (List<String>) FileUtils.readLines(need, "UTF-8")) {
            if (f.startsWith("###")) {
                continue;
            }
            files.put(f.trim(), 0);
        }
    }

    protected static String isRequired(String path) {
        for (String f : files.keySet()) {
            if (path.equalsIgnoreCase(f)) {
                return f;
            }
        }
        return null;
    }

    protected static final String PREFIX_MANIFEST = "Windows/winsxs/Manifests/";

    protected static void loadManifest(String manifestName, byte[] data) throws Exception {
        String path = manifestName.replaceAll("^(.+/)?(.+?)\\.manifest$", "$2") + "/";

        XMLEventReader rd = factory.createXMLEventReader(new ByteArrayInputStream(data));
        while (rd.hasNext()) {
            XMLEvent e;
            try {
                e = rd.nextEvent();
            } catch (XMLStreamException ex) {
                return;
            }
            switch (e.getEventType()) {
            case XMLEvent.START_ELEMENT:
                StartElement eStart = (StartElement) e;
                if ("file".equals(eStart.getName().getLocalPart())) {
                    String name = eStart.getAttributeByName(new QName("name")).getValue();
                    if (name.endsWith(".mui")) {
                        Attribute da = eStart.getAttributeByName(new QName("destinationPath"));
                        String destinationPath = da != null ? da.getValue() : "Windows/System32/";
                        destinationPath = destinationPath.replace('\\', '/');
                        destinationPath = destinationPath.replace("$(runtime.windows)", "Windows");
                        destinationPath = destinationPath.replace("$(runtime.system32)", "Windows/System32");
                        destinationPath = destinationPath.replace("$(runtime.System32)", "Windows/System32");
                        destinationPath = destinationPath.replace("$(runtime.programFiles)", "Program Files");
                        destinationPath = destinationPath.replace("$(runtime.commonFiles)",
                                "Program Files/Common Files");
                        pathFromManifests.put(path + name, destinationPath + name);
                    }
                }
            }
        }
    }

    protected static int processFile(String zipPath, String normPath, byte[] data) throws Exception {
        ReaderWriterMUI mui = new ReaderWriterMUI(data);

        String version = getVersion(data);
        String arch = mui.readArch();

        String outPath = normPath.replaceAll("\\.mui$", "_") + version + '_' + arch + ".mui";

        System.out.println("  " + zipPath + " -> " + outPath);

        File out = new File(OUT_DIR, outPath);
        if (out.exists()) {
            byte[] exist = FileUtils.readFileToByteArray(out);
            Assert.assertArrayEquals(exist, data);
            return 0;
        } else {
            FileUtils.writeByteArrayToFile(out, data);
            return 1;
        }
    }

    protected static final String PREFIX_PF86 = "Program Files (x86)/";
    protected static final String PREFIX_PF86_NORM = "Program Files/";
    protected static final String PREFIX_W86 = "Windows/SysWOW64/";
    protected static final String PREFIX_W86_NORM = "Windows/System32/";

    protected static String normalizePath(String p) {
        String lastDirAndName = p.replaceAll("^(.+/)(.+?/.+?)", "$2");
        String r = pathFromManifests.get(lastDirAndName);
        if (r != null) {
            return r;
        }
        if (p.startsWith(PREFIX_PF86)) {
            p = PREFIX_PF86_NORM + p.substring(PREFIX_PF86.length());
        } else if (p.startsWith(PREFIX_W86)) {
            p = PREFIX_W86_NORM + p.substring(PREFIX_W86.length());
        }
        return p;
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
