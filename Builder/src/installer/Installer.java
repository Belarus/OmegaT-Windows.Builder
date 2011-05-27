/**************************************************************************
 Converter for Widnows MUI files.

 Copyright (C) 2010 Alex Buloichik <alex73mail@gmail.com>

 This program is free software; you can redistribute it and/or modify 
 it under the terms of the GNU General Public License as published by 
 the Free Software Foundation; either version 3 of the License, or 
 (at your option) any later version.

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License 
 along with this program; if not, see http://www.gnu.org/licenses.
 **************************************************************************/
package installer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import resources.MemoryFile;
import resources.ParserVersion;
import resources.ResUtils;

/**
 * Ствараем файл усталёўкі для NSIS.
 */
public class Installer {
    static File OUT_DIR = new File("../out/");

    /** Трэба падаваць свае назвы, бо у JDK 1.6 назвы месяцаў няправільныя. */
    protected static final String[] MONTHS = new String[] { "студзеня", "лютага", "сакавіка", "красавіка",
            "траўня", "чэрвеня", "ліпеня", "жніўня", "верасня", "кастрычніка", "лістапада", "снежня" };

    public static void main(String[] a) throws Exception {
        make();
    }

    public static void make() throws Exception {

        Process p32 = new Process();
        p32.listFiles(true);
        Process p64 = new Process();
        p64.listFiles(false);

        Calendar c = Calendar.getInstance();
        String dateText = c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)] + " "
                + c.get(Calendar.YEAR);
        String dateTextMark = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, String> templateVars = new TreeMap<String, String>();
        templateVars.put("##DATE##", dateText);
        templateVars.put("##DATEMARK##", dateTextMark);
        templateVars.put("##FILEVERSIONS32##", p32.getFileVersions());
        templateVars.put("##FILEUNPACK32##", p32.getFileUnpack());
        templateVars.put("##FILEINSTALL32##", p32.getFileInstall());
        templateVars.put("##DIR_INSTALL32##", p32.getDirInstall());
        templateVars.put("##DIR_UNINSTALL32##", p32.getDirUninstall());
        templateVars.put("##FILEVERSIONS64##", p64.getFileVersions());
        templateVars.put("##FILEUNPACK64##", p64.getFileUnpack());
        templateVars.put("##FILEINSTALL64##", p64.getFileInstall());
        templateVars.put("##DIR_INSTALL64##", p64.getDirInstall());
        templateVars.put("##DIR_UNINSTALL64##", p64.getDirUninstall());

        patchFile("readme.txt", templateVars);
        patchFile("win7bel.nsi", templateVars);
        patchFile("Belarusian.nlf", templateVars);
        patchFile("Belarusian.nsh", templateVars);
        copy("win7i18n.ico");
        FileUtils.writeStringToFile(new File(OUT_DIR, "i18n-bel-win7.version.txt"), dateTextMark);
    }

    protected static void patchFile(String fn, Map<String, String> templateVars) throws Exception {
        String template = IOUtils.toString(Installer.class.getResourceAsStream("/installer/" + fn), "UTF-8");

        String out = template;
        for (Map.Entry<String, String> e : templateVars.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }

        FileUtils.writeStringToFile(new File(OUT_DIR, fn), out, "Cp1251");
    }

    protected static String getVersion(byte[] file) throws Exception {
        ReaderWriterMUI mui = new ReaderWriterMUI(file);
        mui.read();
        byte[] ver = mui.getCompiledResources().get(ResUtils.TYPE_VERSION).get(1);
        ParserVersion pv = new ParserVersion();
        pv.parse(1, new MemoryFile(ver), new PrintWriter(new StringWriter()));

        return (pv.vi.dwFileVersionMS >> 16) + "." + (pv.vi.dwFileVersionMS & 0xFFFF) + "."
                + (pv.vi.dwFileVersionLS >> 16) + "." + (pv.vi.dwFileVersionLS & 0xFFFF);
    }

    protected static void copy(String file) throws Exception {
        byte[] data = IOUtils.toByteArray(Installer.class.getResourceAsStream("/installer/" + file));
        FileUtils.writeByteArrayToFile(new File(OUT_DIR, file), data);
    }

    protected static class Process {
        Map<String, Map<String, String>> versions = new TreeMap<String, Map<String, String>>();

        Map<String, Boolean> optionals = new TreeMap<String, Boolean>();

        public void listFiles(boolean x32) throws Exception {
            if (x32) {
                addDir("$PROGRAMFILES/", "Program Files/", new File(OUT_DIR, "Program Files"));
                addDir("$WINDIR/", "Windows/", new File(OUT_DIR, "Windows"));
            } else {
                addDir("$PROGRAMFILES64/", "Program Files/", new File(OUT_DIR, "Program Files"));
                addDir("$PROGRAMFILES32/", "Program Files/", new File(OUT_DIR, "Program Files"));
                addDir("$WINDIR/", "Windows/", new File(OUT_DIR, "Windows"));
                addDir("$WINDIR/SysWOW64/", "Windows/System32/", new File(OUT_DIR, "Windows/System32"));
            }
        }

        protected void addDir(String winPrefix, String localPrefix, File dir) {
            Map<String, File> files = ResUtils.listFiles(dir, "mui");
            for (Map.Entry<String, File> f : files.entrySet()) {
                addFile(winPrefix + f.getKey(), localPrefix + f.getKey());
            }
        }

        static final Pattern RE_FILE_VERSION = Pattern.compile("^(.+)_(\\d+\\.\\d+\\.\\d+\\.\\d+_x\\d+).mui");

        private void addFile(String winPath, String localPath) {
            Matcher m = RE_FILE_VERSION.matcher(winPath);
            Assert.assertTrue(m.matches());
            String fn = m.group(1) + ".mui";
            String ver = m.group(2);
            Map<String, String> fv = versions.get(fn);
            if (fv == null) {
                fv = new TreeMap<String, String>();
                versions.put(fn, fv);
            }
            fv.put(ver, localPath);
        }

        // public void listFilesold(File dirFind, String dirSrc) throws Exception {
        // Map<String, File> files = ResUtils.listFiles(dirFind, null);
        // for (Map.Entry<String, File> f : files.entrySet()) {
        // processFile(sysDirs, f.getKey(), dirSrc, f.getValue());
        // }
        //
        // for (String d : dirs) {
        // dirInstall.append("\tCreateDirectory  '" + d + "'\n");
        // dirUninstall.append("\tRmDir /r /REBOOTOK '" + d + "'\n");
        // }
        // }

        private String getFileVersions() {
            StringBuilder o = new StringBuilder();

            // for (String fw : versions.keySet()) {
            // if (fw.toLowerCase().endsWith(".mui")) {
            // // boolean optional = optionals.get(fw);
            // boolean optional = false;
            // o.append("\tPush \"" + fw + "\"\n");
            // if (optional) {
            // o.append("\tCall FindMuiFileOptional\n");
            // } else {
            // o.append("\tCall FindMuiFile\n");
            // }
            // o.append("\t!insertmacro GetMuiVersion \"$outFile\"\n");
            // // o.append("\t!insertmacro GetFileVersion \"$outFile\"\n");
            // for (String v : versions.get(fw).keySet()) {
            // o.append("\tPush \"" + v + "\"\n");
            // }
            // o.append("\tCall VersionsCheckFunc\n");
            // o.append("\tnxs::Update /NOUNLOAD \"Спраўджваем усталяваныя версіі...\" /pos $9 /end\n");
            // o.append("\tIntOp $9 $9 + 1\n");
            // o.append("\n");
            // }
            // }

            return o.toString();
        }

        private String getFileUnpack() {
            StringBuilder o = new StringBuilder();
            int fileIndex = 1;
            for (String fw : versions.keySet()) {
                String fwWin = fw.replace('/', '\\');
                o.append(";  File " + fileIndex + "\n");
                o.append("\t!insertmacro GetMuiVersion '" + fw + "'\n");
                int versionIndex = 1;
                for (String v : versions.get(fw).keySet()) {
                    o.append("\tStrCmp $0 '" + v + "' 0 versionEnd_" + fileIndex + "_" + versionIndex + "\n");
                    o.append("\t\tFile '/oname=" + fwWin + ".new' '"
                            + versions.get(fw).get(v).replace('/', '\\') + "'\n");
                    o.append("\t\tDelete /REBOOTOK '" + fwWin + "'\n");
                    o.append("\t\tRename /REBOOTOK '" + fwWin + ".new' '" + fwWin + "'\n");
                    o.append("\t\tGoto fileEnd" + fileIndex + "\n");
                    o.append("versionEnd_" + fileIndex + "_" + versionIndex + ":\n");
                    versionIndex++;
                }
                o.append("\tStrCpy $wrongVersionsText \"$wrongVersionsTextНемагчыма ўсталяваць увесь пакунак, бо файл "
                        + fw + " мае версію $0$\\r$\\n$\\r$\\n\"\n");
                o.append("fileEnd" + fileIndex + ":\n\n");
                fileIndex++;
            }
            return o.toString();
        }

        private String getFileInstall() {
            StringBuilder o = new StringBuilder();
            // for (String fw : versions.keySet()) {
            // o.append("\tDelete /REBOOTOK '" + fw + "'\n");
            // o.append("\tRename /REBOOTOK '" + fw + ".new' '" + fw + "'\n");
            // o.append("\n");
            // }
            return o.toString();
        }

        protected String getDirInstall() {
            StringBuilder o = new StringBuilder();
            for (String d : getUniqueDirs()) {
                o.append("\tCreateDirectory  '" + d.replace('/', '\\') + "'\n");
            }
            return o.toString();
        }

        protected String getDirUninstall() {
            StringBuilder o = new StringBuilder();
            for (String d : getUniqueDirs()) {
                o.append("\tRmDir /r /REBOOTOK '" + d.replace('/', '\\') + "'\n");
            }
            return o.toString();
        }

        private Set<String> getUniqueDirs() {
            Set<String> result = new TreeSet<String>();
            for (String fw : versions.keySet()) {
                int pos = fw.lastIndexOf('/');
                String d = fw.substring(0, pos);
                result.add(d);
            }
            return result;
        }
    }

    protected static class FileInfo {
        String winPath, localDir;

    }
}
