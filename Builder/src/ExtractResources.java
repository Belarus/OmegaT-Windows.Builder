import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import resources.ParserRES;
import resources.ResUtils;
import resources.io.WriterRC;
import util.ListMUI;
import win7.SkipResources;

public class ExtractResources {
    static String tPath = "../../Windows.OmegaT/Windows7/source/";

    static int errors = 0;

    public static void main(String[] args) throws Exception {
        Assert.assertEquals("Execute: ExtractResources <fromdir>", 1, args.length);

        Map<String, File> filesExist = ResUtils.listFiles(new File(tPath), null);
        filesExist.remove("list.txt");

        // тэкст rc,сьпіс файлаў у якіх такі rc
        Map<FileContent, Set<String>> contents = new HashMap<FileContent, Set<String>>();

        List<ListMUI.MUInfo> muis = ListMUI.listMUI(new File(args[0], "mui"));

        for (ListMUI.MUInfo mui : muis) {
            try {
                if (SkipResources.isSkippedFile(mui.resourceFileName)) {
                    continue;
                }
                System.out.println("Processing: " + mui.resourceFileName);
                byte[] data32 = mui.file32 != null ? FileUtils.readFileToByteArray(mui.file32) : null;
                byte[] data64 = mui.file64 != null ? FileUtils.readFileToByteArray(mui.file64) : null;

                String rcText32 = data32 != null ? parseMUI(mui.resourceFileName, data32) : null;
                String rcText64 = data64 != null ? parseMUI(mui.resourceFileName, data64) : null;
                if (rcText32 != null && rcText64 != null) {
                    Assert.assertEquals(rcText32, rcText64);
                }
                FileUtils.writeStringToFile(new File(tPath, "mui/" + mui.resourceFileName),
                        rcText32 != null ? rcText32 : rcText64, "UTF-8");
            } catch (Exception ex) {
                System.err.println("Error in " + mui.resourceFileName);
                ex.printStackTrace();
                errors++;
            }
        }

        Map<String, File> otherFiles = ResUtils.listFiles(new File(args[0], "gadget"), null);
        for (String f : otherFiles.keySet()) {
            System.out.println("Processing: " + f);
            FileUtils.copyFile(otherFiles.get(f), new File(tPath, "gadget/" + f));
        }

        System.out.println("Error count: " + errors);
    }

    protected static String parseMUI(String fspath, byte[] data) throws Exception {
        ReaderWriterMUI mui = new ReaderWriterMUI(data);
        mui.read();

        Map<Object, Map<Object, byte[]>> minused = SkipResources.minus(fspath, mui.getCompiledResources(),
                SkipResources.SKIP_EXTRACT);

        ParserRES res = new ParserRES(minused);

        return new WriterRC().write(res.getParsedResources());
    }

    protected static final Pattern RE_FILENAME = Pattern.compile("([a-zA-Z0-9]+?)/(.+)(\\.[a-z]+)");

    protected static String fileToName(String f) throws Exception {
        Matcher m = RE_FILENAME.matcher(f);
        if (!m.matches()) {
            throw new Exception("Invalid file name: " + f);
        }
        String fn = m.group(2) + "_" + m.group(1) + m.group(3);
        if (m.group(3).equals(".mui")) {
            fn += ".rc";
        }
        return fn;
    }

    protected static class FileContent {
        final byte[] byteContent;
        int hash;

        public FileContent(byte[] c) {
            this.byteContent = c;
            hash = Arrays.hashCode(this.byteContent);
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            return Arrays.equals(byteContent, ((FileContent) obj).byteContent);
        }
    }
}
