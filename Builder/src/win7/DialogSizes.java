package win7;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import resources.MemoryFile;
import resources.ParserDialog;
import resources.ResUtils;
import resources.ResourceDialog;

public class DialogSizes {
    static Pattern RE_FILE = Pattern.compile("(.+)\\s*:");
    static Pattern RE_DIALOG = Pattern.compile("DIALOG/(.+)");
    static Pattern RE_CONTROL = Pattern.compile("\"(.*)\"\\s*,\\s+(\\-?[0-9]+)");
    static Pattern RE_PLACE = Pattern
            .compile("([0-9]+)\\s*,\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*\\-\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*,\\s*([0-9]+)");

    // Map<file,Map<dialogID,List>>
    final Map<String, Map<Object, List<SizeChange>>> changes = new TreeMap<String, Map<Object, List<SizeChange>>>();

    public int errors;

    public DialogSizes(File szFile) throws Exception {
        BufferedReader rd = new BOMBufferedReader(new InputStreamReader(new FileInputStream(szFile), "UTF-8"));
        String s;
        int line = 0;

        String file = null;
        Object dialogID = null;
        SizeChange ch = null;

        while ((s = rd.readLine()) != null) {
            line++;

            s = s.trim();
            if (s.length() == 0) {
                continue;
            }

            Matcher m;
            if ((m = RE_FILE.matcher(s)).matches()) {
                file = m.group(1);
                if (!changes.containsKey(file)) {
                    changes.put(file, new HashMap<Object, List<SizeChange>>());
                }
            } else if ((m = RE_DIALOG.matcher(s)).matches()) {
                try {
                    dialogID = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ex) {
                    dialogID = m.group(1);
                }
                if (!changes.get(file).containsKey(dialogID)) {
                    changes.get(file).put(dialogID, new ArrayList<SizeChange>());
                }
            } else if ((m = RE_CONTROL.matcher(s)).matches()) {
                ch = new SizeChange();
                ch.controlTitle = m.group(1);
                ch.controlID = Long.parseLong(m.group(2));
            } else if ("<DIALOG>".equals(s)) {
                // dialog box
                ch = new SizeChange();
                ch.controlTitle = null;
                ch.controlID = 0;
            } else if ((m = RE_PLACE.matcher(s)).matches()) {
                ch.oldPlace = getRect(m, 1);
                ch.newPlace = getRect(m, 5);
                changes.get(file).get(dialogID).add(ch);
                ch = null;
            } else {
                throw new Exception("Unknown line in DialogSizes: " + s);
            }
        }
    }

    public void incCounts(String filename) {
        Map<Object, List<SizeChange>> changesForFile = changes.get(filename);
        if (changesForFile == null) {
            return;
        }
        for (Map.Entry<Object, List<SizeChange>> d : changesForFile.entrySet()) {
            for (SizeChange sc : d.getValue()) {
                sc.appliedLeaved++;
            }
        }
    }

    /**
     * Fix mui for change dialog sizes
     */
    public void fix(String file, Map<Object, Map<Object, byte[]>> resources, boolean dryRun) throws Exception {
        Map<Object, List<SizeChange>> changesForFile = changes.get(file);
        if (changesForFile == null) {
            return;
        }
        Map<Object, byte[]> dialogs = resources.get(ResUtils.TYPE_DIALOG);
        if (dialogs != null) {
            for (Object k : dialogs.keySet()) {
                List<SizeChange> dialogChanges = changesForFile.get(k);
                if (dialogChanges == null) {
                    continue;
                }
                byte[] dialogResource = dialogs.get(k);
                // now - dialogChanges vs dialogResource
                ResourceDialog dlg = new ParserDialog(new MemoryFile(dialogResource)).parse();
                applyChanges(dlg, dialogChanges, dryRun);

                for (ResourceDialog.DlgItemTemplateEx dlgItem : dlg.items) {
                    applyChanges(k, dlgItem, dialogChanges, dryRun);
                }

                MemoryFile outDialog = new MemoryFile();
                new ParserDialog(outDialog).write(dlg);
                dialogs.put(k, outDialog.getBytes());
                /*
                 * boolean needUpdate = false; for (ResourceDialog.DlgItemTemplateEx item : dlg.items) {
                 * needUpdate |= applyChangesToItem(item, dialogChanges); } if (needUpdate) {
                 * 
                 * }
                 */
            }
        }
    }

    public void report() {
        for (Map.Entry<String, Map<Object, List<SizeChange>>> f : changes.entrySet()) {
            for (Map.Entry<Object, List<SizeChange>> d : f.getValue().entrySet()) {
                for (SizeChange sc : d.getValue()) {
                    if (sc.appliedLeaved > 0) {
                        System.err.println("Not fixed size for DIALOG #" + d.getKey() + " control#"
                                + sc.controlID + " in file " + f.getKey());
                    }
                }
            }
        }
    }

    /**
     * Change pos for dialog.
     */
    private void applyChanges(ResourceDialog dialog, List<SizeChange> dialogChanges, boolean dryRun)
            throws Exception {
        for (SizeChange ch : dialogChanges) {
            if (ch.controlID == 0 && ch.controlTitle == null && ch.oldPlace.x == dialog.x
                    && ch.oldPlace.y == dialog.y && ch.oldPlace.width == dialog.cx
                    && ch.oldPlace.height == dialog.cy) {
                if (!dryRun) {
                    dialog.x = (short) ch.newPlace.x;
                    dialog.y = (short) ch.newPlace.y;
                    dialog.cx = (short) ch.newPlace.width;
                    dialog.cy = (short) ch.newPlace.height;
                    ch.appliedLeaved--;
                }
                break;
            }
        }
    }

    /**
     * Change pos for dialog's control.
     */
    private void applyChanges(Object dialogID, ResourceDialog.DlgItemTemplateEx dialogItem,
            List<SizeChange> dialogChanges, boolean dryRun) throws Exception {
        for (SizeChange ch : dialogChanges) {
            if (ch.controlID == dialogItem.id && ch.oldPlace.x == dialogItem.x
                    && ch.oldPlace.y == dialogItem.y && ch.oldPlace.width == dialogItem.cx
                    && ch.oldPlace.height == dialogItem.cy) {
                if (!dryRun) {
                    if (!eqEmpty(ch.controlTitle, (String) dialogItem.title)) {
                        errors++;
                        System.err.println("Wrong text in control #" + dialogID + "/" + dialogItem.id
                                + ": compiled: [" + dialogItem.title + "] in changes: [" + ch.controlTitle
                                + "]");
                    }
                    dialogItem.x = (short) ch.newPlace.x;
                    dialogItem.y = (short) ch.newPlace.y;
                    dialogItem.cx = (short) ch.newPlace.width;
                    dialogItem.cy = (short) ch.newPlace.height;
                    ch.appliedLeaved--;
                }
                break;
            }
        }
    }

    protected static boolean eqEmpty(String s1, String s2) {
        if (s1 == null)
            s1 = "";
        if (s2 == null)
            s2 = "";
        return s1.equals(s2);
    }

    protected static Rectangle getRect(Matcher m, int base) {
        Rectangle r = new Rectangle();
        r.x = Integer.parseInt(m.group(base + 0));
        r.y = Integer.parseInt(m.group(base + 1));
        r.width = Integer.parseInt(m.group(base + 2));
        r.height = Integer.parseInt(m.group(base + 3));
        return r;
    }

    protected static class SizeChange {
        long controlID;
        String controlTitle;
        Rectangle oldPlace, newPlace;
        int appliedLeaved; // колькі разоў трэба аплаіць
    }

    public class BOMBufferedReader extends BufferedReader {
        public BOMBufferedReader(Reader rd) throws IOException {
            super(rd);
            mark(4);

            int char1 = read();
            if (char1 != 65279) { // BOM: EF BB BF
                reset();
            }
        }
    }
}
