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
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import resources.MemoryFile;
import resources.ParserVersion;
import resources.ResUtils;
import win7.Utils;

/**
 * Ствараем файл усталёўкі для NSIS.
 */
public class Installer {
    static File SOURCE_DIR = new File("../Files/");
    static File OUT_DIR = new File("../out/");

    /** Трэба падаваць свае назвы, бо у JDK 1.6 назвы месяцаў няправільныя. */
    protected static final String[] MONTHS = new String[] { "студзеня", "лютага", "сакавіка", "красавіка",
            "траўня", "чэрвеня", "ліпеня", "жніўня", "верасня", "кастрычніка", "лістапада", "снежня" };

    public static void main(String[] a) throws Exception {
        make();
    }

    public static void make() throws Exception {
        Map<String, String> SYSDIR32 = new TreeMap<String, String>();
        Map<String, String> SYSDIR64 = new TreeMap<String, String>();

        SYSDIR32.put("Program Files", "$PROGRAMFILES");
        SYSDIR32.put("Windows", "$WINDIR");

        SYSDIR64.put("Program Files (x86)", "$PROGRAMFILES32");
        SYSDIR64.put("Program Files", "$PROGRAMFILES64");
        SYSDIR64.put("Windows", "$WINDIR");

        Process p32 = new Process(SYSDIR32);
        p32.listFiles("x32sprtm", false);
        p32.listFiles("x32sp0", false);
        p32.listFiles("x32sp1", false);
        p32.listFiles("ie8x32sp0", true);
        p32.listFiles("ie8x32sp1", true);
        p32.listFiles("ie9x32sp0", true);
        Process p64 = new Process(SYSDIR64);
        p64.listFiles("x64sp0", false);
        p64.listFiles("x64sp1", false);
        p64.listFiles("ie8x64sp0", true);
        p64.listFiles("ie8x64sp1", true);
        p64.listFiles("ie9x64sp0", true);

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
        templateVars.put("##FILESCOUNT32##", Integer.toString(p32.filesCount()));
        templateVars.put("##FILESCOUNT64##", Integer.toString(p64.filesCount()));

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

    protected static String getSha1(String fn) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        FileInputStream fis = new FileInputStream(new File(SOURCE_DIR, fn));
        byte[] dataBytes = new byte[1024];

        int nread = 0;

        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }

        byte[] mdbytes = md.digest();
        // convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    protected static void copy(String file) throws Exception {
        byte[] data = IOUtils.toByteArray(Installer.class.getResourceAsStream("/installer/" + file));
        FileUtils.writeByteArrayToFile(new File(OUT_DIR, file), data);
    }

    protected static class Process {
        final Map<String, String> sysDirs;

        // Map<OutputWindowsPath, Map<version,localPath>>
        Map<String, Map<String, String>> versions = new TreeMap<String, Map<String, String>>();

        Map<String, Boolean> optionals = new TreeMap<String, Boolean>();

        Set<String> dirs = new TreeSet<String>();

        public Process(Map<String, String> sysDirs) throws Exception {
            this.sysDirs = sysDirs;
        }

        public void listFiles(String dirSrc, boolean optional) throws Exception {
            Map<String, File> files = ResUtils.listFiles(new File(OUT_DIR, dirSrc), null);
            for (Map.Entry<String, File> f : files.entrySet()) {
                try {
                    String winFile = getSysDirFile(f.getKey());
                    String diskFile = dirSrc + '/' + f.getKey();
                    String version;

                    ZipFile zip = new ZipFile(new File(SOURCE_DIR, dirSrc+".zip"));
                    byte[] originalFile = Utils.readZip(zip,
                            new ZipEntry(f.getKey().replace("/be-BY/", "/en-US/")));
                    zip.close();

                    if (winFile.endsWith(".mui")) {
                        version = getVersion(originalFile);
                    } else {
                        version = "z";// getSha1(dirSrc + f.getKey().replace("/be-BY/", "/en-US/"));
                    }
                    Map<String, String> verFiles = versions.get(winFile);
                    if (verFiles == null) {
                        verFiles = new TreeMap<String, String>();
                        versions.put(winFile, verFiles);
                        optionals.put(winFile, optional);
                    }
                    verFiles.put(version, diskFile);

                } catch (Exception ex) {
                    System.err.println("File: " + f.getKey());
                    throw ex;
                }
            }
        }

        private String getSysDirFile(String fn) {
            int pos = fn.indexOf('/');
            String prefix = fn.substring(0, pos);
            String var = sysDirs.get(prefix);
            String destFile = (var + fn.substring(pos)).replace('/', '\\').replace("\\en-US\\", "\\be-BY\\");
            return destFile;
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

        protected String getFileVersions() {
            StringBuilder o = new StringBuilder();

            for (String fw : versions.keySet()) {
                if (fw.toLowerCase().endsWith(".mui")) {
                    boolean optional = optionals.get(fw);
                    o.append("\tPush \"" + fw + "\"\n");
                    if (optional) {
                        o.append("\tCall FindMuiFileOptional\n");
                    } else {
                        o.append("\tCall FindMuiFile\n");
                    }
                    o.append("\t!insertmacro GetMuiVersion \"$outFile\"\n");
                    // o.append("\t!insertmacro GetFileVersion \"$outFile\"\n");
                    for (String v : versions.get(fw).keySet()) {
                        o.append("\tPush \"" + v + "\"\n");
                    }
                    o.append("\tCall VersionsCheckFunc\n");
                    o.append("\tnxs::Update /NOUNLOAD \"Спраўджваем усталяваныя версіі...\" /pos $9 /end\n");
                    o.append("\tIntOp $9 $9 + 1\n");
                    o.append("\n");
                }
            }

            return o.toString();
        }

        int filesCount() {
            return versions.size();
        }

        protected String getFileUnpack() {
            StringBuilder o = new StringBuilder();
            for (String fw : versions.keySet()) {
                for (String v : versions.get(fw).keySet()) {
                    if (fw.endsWith(".mui")) {
                        o.append("\t!insertmacro InstallMuiVersion \"" + fw + "\" \"" + v + "\" \""
                                + versions.get(fw).get(v).replace('/', '\\') + "\"\n");
                    } else {
                        o.append("\t!insertmacro InstallFileVersion \"" + fw + "\" \"" + v + "\" \""
                                + versions.get(fw).get(v).replace('/', '\\') + "\"\n");
                    }
                }
            }
            return o.toString();
        }

        protected String getFileInstall() {
            StringBuilder o = new StringBuilder();
            for (String fw : versions.keySet()) {
                o.append("\tDelete /REBOOTOK '" + fw + "'\n");
                o.append("\tRename /REBOOTOK '" + fw + ".new' '" + fw + "'\n");
                o.append("\n");
            }
            return o.toString();
        }

        protected String getDirInstall() {
            StringBuilder o = new StringBuilder();
            for (String d : getUniqueDirs()) {
                o.append("\tCreateDirectory  '" + d + "'\n");
            }
            return o.toString();
        }

        protected String getDirUninstall() {
            StringBuilder o = new StringBuilder();
            for (String d : getUniqueDirs()) {
                o.append("\tRmDir /r /REBOOTOK '" + d + "'\n");
            }
            return o.toString();
        }

        private Set<String> getUniqueDirs() {
            Set<String> result = new TreeSet<String>();
            for (String fw : versions.keySet()) {
                int pos = fw.lastIndexOf('\\');
                String d = fw.substring(0, pos);
                result.add(d);
            }
            return result;
        }
    }
}
