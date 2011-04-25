import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import resources.ResUtils;

public class PatchSizes {
    static String tPath = "../i18n-bel/Windows7/target/";

    public static void main(String args[]) throws Exception {
        Map<String, File> filesExist = ResUtils.listFiles(new File(tPath), null);
        for (File f : filesExist.values()) {
            if (f.getName().endsWith(".rc")) {
                String src = FileUtils.readFileToString(f);
                StringWriter wr = new StringWriter();
                process(new BufferedReader(new StringReader(src)), new BufferedWriter(wr));
                FileUtils.writeStringToFile(f, wr.toString());
            }
        }
    }

    enum PART {
        DIALOG, MENU, MESSAGETABLE, STRINGTABLE, OTHER, UNKNOWN
    };

    protected static String blockId;
    protected static int b, e;

    static void process(BufferedReader inFile, BufferedWriter outFile) throws Exception {
        StringBuilder r = new StringBuilder();

        PART cPart = PART.UNKNOWN;
        int cLevel = 0;

        blockId = null;
        String s;
        while ((s = inFile.readLine()) != null) {
            b = -1;
            e = -1;
            String id = null;
            String strim = s.trim();

            if (strim.startsWith("//") || strim.startsWith("#")) {
                outFile.write(s);
                outFile.newLine();
                continue;
            }

            if (strim.length() == 0) {
                if (cLevel == 0) {
                    cPart = PART.UNKNOWN;
                }
            } else if (cPart == PART.UNKNOWN) {
                cPart = parseFirstLineInBlock(strim);
            } else if ("{".equals(strim) || "BEGIN".equalsIgnoreCase(strim)) {
                cLevel++;
            } else if ("}".equals(strim) || "END".equalsIgnoreCase(strim)) {
                cLevel--;
                if (cLevel == 0) {
                    cPart = PART.UNKNOWN;
                }
            } else if (cLevel > 0 && cPart != PART.OTHER && cPart != PART.UNKNOWN) {
                markForTranslation(s);
                if (b >= 0 && e >= 0 && b < e && e > 0) {
                    id = parseId(cPart, s, b, e);
                }
            } else if (cLevel == 0 && cPart == PART.DIALOG) {
                if (RE_DIALOG_CAPTION.matcher(strim).matches()) {
                    markForTranslation(s);
                    id = "__CAPTION__";
                }
            }

            outFile.write(s);
            outFile.newLine();
        }

        outFile.flush();
    }

    static String parseId(PART cPart, String line, int b, int e) {
        String[] w;
        switch (cPart) {
        case DIALOG:
        case MENU:
            w = line.substring(e).split(",");
            return w.length > 1 ? w[1].trim() : null;
        case MESSAGETABLE:
        case STRINGTABLE:
            w = line.split(",");
            return w[0].trim();
        }
        return null;
    }

    static PART parseFirstLineInBlock(String line) {
        Matcher m;
        if ((m = RE_DIALOG.matcher(line)).matches()) {
            blockId = m.group(1);
            return PART.DIALOG;
        }
        if ((m = RE_MENU.matcher(line)).matches()) {
            blockId = m.group(1);
            return PART.MENU;
        }
        if ((m = RE_MESSAGETABLE.matcher(line)).matches()) {
            blockId = m.group(1);
            return PART.MESSAGETABLE;
        }
        if (RE_STRINGTABLE.matcher(line).matches()) {
            blockId = "";
            return PART.STRINGTABLE;
        }
        return PART.OTHER;
    }

    static void markForTranslation(String s) {
        b = s.indexOf('"');
        if (b < 0) {
            return;
        }
        e = b;
        while (true) {
            e = s.indexOf('"', e + 1);
            if (e < 0) {
                break;
            }
            if (s.charAt(e - 1) == '\\') {
                // skip escaped quote
                continue;
            }
            if (e < s.length() - 1) {
                if (s.charAt(e + 1) == '"') {
                    // skip double quote
                    e++;
                    continue;
                }
            }
            break;
        }
    }

    protected static final Pattern RE_DIALOG = Pattern.compile("(\\S+)\\s+DIALOG(EX)?\\s+.+");
    protected static final Pattern RE_DIALOG_CAPTION = Pattern.compile("CAPTION\\s+.+");
    protected static final Pattern RE_MENU = Pattern.compile("(\\S+)\\s+MENU(EX)?\\s*.*");
    protected static final Pattern RE_MESSAGETABLE = Pattern.compile("(\\S+)\\s+MESSAGETABLE\\s*.*");
    protected static final Pattern RE_STRINGTABLE = Pattern.compile("STRINGTABLE\\s*.*");
}
