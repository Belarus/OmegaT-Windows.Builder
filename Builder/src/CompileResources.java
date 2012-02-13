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

import installer.Installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.omegat.core.Core;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.RealProject;
import org.omegat.filters2.master.PluginUtils;
import org.omegat.util.ProjectFileStorage;
import org.omegat.util.RuntimePreferences;

import resources.CompilerMessageTable;
import resources.MemoryFile;
import resources.ParserRES;
import resources.ResUtils;
import resources.ResourceMessageTable;
import resources.io.WriterRC;
import resources.res.RESFile;
import util.ListMUI;
import win7.DialogSizes;
import win7.SkipResources;

/**
 * Гэты клясс абыходзіць .mui файлы ў каталёзе mui-bin/ і накладае на іх новыя перакладзеныя рэсурсы.
 */
public class CompileResources {

    static String projectPath = "../../Windows.OmegaT/Windows/";
    static String configPath = "../../Windows.OmegaT/Windows.settings/";
    static String outPath = "../out/";
    static String fromPath;
    static String ver;

    static Map<String, List<JSENtry>> JS_TRANS = new TreeMap<String, List<JSENtry>>();

    static DialogSizes places;

    static Map<String, String> list = new HashMap<String, String>();

    public static void main(String[] args) throws Exception {
        Assert.assertEquals("Execute: CompileResources <fromdir> <version(7 or 8)>", 2, args.length);

        fromPath = args[0];
        ver = args[1];

        // execute OmegaT for translate
        translate();

        // remote output dir
        FileUtils.deleteDirectory(new File(outPath));

        places = new DialogSizes(new File(projectPath + "/dialog-sizes-"+ver+".txt"));

        // readList();

        String rcPath = projectPath + "/target/"+ver+"/mui/";

        readJStrans(new File(projectPath + "/js-trans-"+ver+".txt"));

        List<ListMUI.MUInfo> muis = ListMUI.listMUI(new File(args[0], "mui"));

        int errors = 0;
        for (ListMUI.MUInfo mui : muis) {
            if (SkipResources.isSkippedFile(mui.resourceFileName)) {
                if (mui.file32 != null) {
                    touch(new File(outPath, "mui/" + mui.file32path));
                }
                if (mui.file64 != null) {
                    touch(new File(outPath, "mui/" + mui.file64path));
                }
                continue;
            }
            try {
                if (mui.file32 != null) {
                    System.err.println(mui.file32path);
                    goMUI(mui.file32, new File(outPath, "mui/" + mui.file32path), new File(rcPath,
                            mui.resourceFileName), mui.resourceFileName);
                }
                if (mui.file64 != null) {
                    System.err.println(mui.file64path);
                    goMUI(mui.file64, new File(outPath, "mui/" + mui.file64path), new File(rcPath,
                            mui.resourceFileName), mui.resourceFileName);
                }
            } catch (Exception ex) {
                errors++;
                ex.printStackTrace();
            }
        }

        Map<String, File> gadgets = ResUtils.listFiles(new File(args[0], "gadget"), null);
        for (String f : gadgets.keySet()) {
            File o = new File(outPath, "gadget/" + f);
            o.getParentFile().mkdirs();
            if (f.endsWith(".js")) {
                byte[] data = FileUtils.readFileToByteArray(gadgets.get(f));
                goJS(f, data, o);
            } else {
                FileUtils.copyFile(gadgets.get(f), o);
            }
        }

        // for (File z : zips) {
        // ZipFile zip = new ZipFile(z);
        // for (Enumeration<? extends ZipEntry> zit = zip.entries(); zit.hasMoreElements();) {
        // ZipEntry ze = zit.nextElement();
        // if (ze.isDirectory()) {
        // continue;
        // }
        //
        // String fspath = z.getName().replace(".zip", "") + "/" + ze.getName();
        // byte[] data = Utils.readZip(zip, ze);
        //
        // System.err.println(fspath);
        // try {
        // String trFile = list.get(fspath);
        //
        // File out = new File(outPath + fspath.replace("/en-US/", "/be-BY/"));
        // out.getParentFile().mkdirs();
        // if (fspath.toLowerCase().endsWith(".mui")) {
        // goMUI(data, out, new File(rcPath + trFile), fspath, trFile);
        // } else if (fspath.toLowerCase().endsWith(".js")) {
        // goJS(trFile, data, out);
        // } else {
        // FileUtils.copyFile(new File(rcPath, trFile), out);
        // }
        // } catch (Exception ex) {
        // errors++;
        // ex.printStackTrace();
        // }
        // }
        // }
        System.err.println("-------------------------------------------------------------------------");
        System.err.println("Errors: " + (errors + places.errors));
        reportJS();

        places.report();

        System.err.print("Installer... ");
        Installer.make();
        System.err.println("OK");
    }

    protected static void touch(File f) throws Exception {
        FileOutputStream out = new FileOutputStream(f);
        out.close();
    }

    protected static void translate() throws Exception {
        // remove translations
        File targetDir = new File(projectPath, "target");
        FileUtils.deleteDirectory(targetDir);
        targetDir.mkdir();

        System.out.println("Initializing OmegaT");
        Map<String, String> pa = new TreeMap<String, String>();
        pa.put("ITokenizer", "org.omegat.plugins.tokenizer.SnowballEnglishTokenizer");
        pa.put("alternate-filename-from", "_\\d+.\\d+.\\d+.\\d+_x\\d+.rc$");
        pa.put("alternate-filename-to", "_VERSION.rc");

        RuntimePreferences.setConfigDir(configPath);

        PluginUtils.loadPlugins(pa);

        Core.initializeConsole(pa);

        ProjectProperties projectProperties = ProjectFileStorage.loadProjectProperties(new File(projectPath));
        if (!projectProperties.verifyProject()) {
            throw new Exception("The project cannot be verified");
        }

        RealProject p = new RealProject(projectProperties);
        p.loadProject();
        Core.setProject(p);

        p.compileProject(".*");

        p.closeProject();
        System.out.println("Translation finished");
    }

    // protected static void readList() throws Exception {
    // BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(fromPath
    // + "list.txt")));
    //
    // String rcFileName = null;
    // String s;
    // while ((s = rd.readLine()) != null) {
    // s = s.trim();
    // if (s.length() == 0) {
    // continue;
    // }
    // if (s.startsWith("+")) {
    // list.put(s.substring(1), rcFileName);
    // places.incCounts(rcFileName);
    // } else {
    // rcFileName = s;
    // }
    // }
    // }

    protected static void readJStrans(File in) throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(in), "UTF-8"));
        String s;
        String input = null;
        List<JSENtry> trans1 = null;
        while ((s = rd.readLine()) != null) {
            if (s.trim().length() == 0) {
                continue;
            }
            if (s.startsWith("I:")) {
                input = s.substring(2);
            } else if (s.startsWith("O:")) {
                if (input == null) {
                    throw new Exception("Invalid format of js-trans");
                }
                trans1.add(new JSENtry(input, s.substring(2)));
                input = null;
            } else {
                trans1 = new ArrayList<JSENtry>();
                JS_TRANS.put(s, trans1);
            }
        }
    }

    protected static void goJS(String file, byte[] data, File outFile) throws Exception {
        List<JSENtry> trans = JS_TRANS.get(file);
        BufferedReader rd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data),
                "UTF-16LE"));
        Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-16LE"));
        String s;
        while ((s = rd.readLine()) != null) {
            if (trans != null) {
                for (JSENtry e : trans) {
                    if (s.equals(e.source)) {
                        s = e.target;
                        e.processed = true;
                        break;
                    }
                }
            }
            wr.write(s + "\r\n");
        }
        wr.close();
    }

    protected static void reportJS() {
        for (Map.Entry<String, List<JSENtry>> ens : JS_TRANS.entrySet()) {
            boolean all = true;
            for (JSENtry e : ens.getValue()) {
                if (!e.processed) {
                    all = false;
                }
            }
            if (!all) {
                System.err.println("Not all js translation used for " + ens.getKey());
            }
        }
    }

    static final byte[] LANG_ORIGINAL = "en-US".getBytes(ResUtils.UNICODE);
    static final byte[] LANG_LOCALIZED = "be-BY".getBytes(ResUtils.UNICODE);
    static final Pattern RE_MESSAGETABLE_HEADER = Pattern.compile("\\s*(\\d+)\\s+MESSAGETABLE\\s*");
    static final Pattern RE_MESSAGETABLE_LINE = Pattern.compile("\\s*([0-9\\-]+)\\s*,\\s*\"(.+)\"\\s*");

    static File tempResFile = new File("temp.res");
    static File tempRcFile = new File("temp.rc");
    static String tempMtPrefix = "tempmt";

    protected static void goMUI(File inFile, File outFile, File rcFile, String rcFileName) throws Exception {
        places.startProcessing(rcFileName);

        byte[] data = FileUtils.readFileToByteArray(inFile);
        tryRecreate(data, rcFileName);

        ReaderWriterMUI mui = new ReaderWriterMUI(data);
        mui.read();
        ResUtils.removeEmptyStrings(mui.getCompiledResources());

        // compile localized resources
        extractMT(rcFile, tempRcFile);
        boolean nul = SkipResources.isNulEolFile(rcFileName);
        execRC("/iRC", nul ? "/n" : "", "/d", "_UNICODE", "/d", "UNICODE", "/fo", tempResFile.getPath(),
                tempRcFile.getPath());
        /* "/g1","/fm", "/media/0082/temp.muires","/q","RC/config.rcconfig", */

        // read compiled resources
        RESFile localizedRes = new RESFile(tempResFile);

        // copy and fix MUI description from original file
        localizedRes.getResources().put("MUI", new TreeMap<Object, byte[]>());
        byte[] muiDesc = mui.getCompiledResources().get("MUI").get(1);
        muiDesc = fixMUI(muiDesc, LANG_ORIGINAL, LANG_LOCALIZED);
        localizedRes.getResources().get("MUI").put(1, muiDesc);

        // fix dialog sizes
        places.fix(rcFileName, localizedRes.getResources(), false);

        // replace resource in MUI
        mui.replaceResources(1059, localizedRes.getResources());

        // save MUI
        MemoryFile out = new MemoryFile();
        mui.write(out);

        outFile.getParentFile().mkdirs();

        out.writeToFile(outFile);
    }

    /**
     * Try to recreate original file. Steps:
     * 
     * 1. Try to recreate DLL using binary resources from original file
     * 
     * 2. Extract original resources in text file
     * 
     * 3. Compile this text file
     * 
     * 4. Compare each-by-each binary resources from original file and from compiled file
     * 
     * If original and created files are equals, we able to recreate file without errors, so, we can create
     * translated MUI.
     */
    protected static void tryRecreate(byte[] originData, String rcFileName) throws Exception {
        // parse binary DLL to separate binary resources
        ReaderWriterMUI mui = new ReaderWriterMUI(originData);
        mui.read();

        // recreate DLL
        MemoryFile muiOut = new MemoryFile();
        mui.write(muiOut);

        // compare original and created DLLs
        byte[] dstFile = muiOut.getBytes();
        if (!SkipResources.isSkipCompareFiles(rcFileName)) {
            compare(originData, dstFile, "Files not equals !");
        }

        Map<Object, Map<Object, byte[]>> allBinRes = mui.getCompiledResources();
        ResUtils.removeEmptyStrings(allBinRes);

        Map<Object, Map<Object, byte[]>> forParseBinRes = SkipResources.minus(rcFileName, allBinRes,
                SkipResources.SKIP_EXTRACT);
        forParseBinRes = SkipResources.minus(rcFileName, forParseBinRes, SkipResources.SKIP_COMPARE);
        ParserRES res = new ParserRES(forParseBinRes);

        // dump resources to text file
        String r = new WriterRC().write(res.getParsedResources());
        FileUtils.writeStringToFile(tempRcFile, r, "UTF-8");

        extractMT(tempRcFile, tempRcFile);
        // compile original text resources
        boolean nul = SkipResources.isNulEolFile(rcFileName);
        execRC("/iRC", nul ? "/n" : "", "/d", "_UNICODE", "/d", "UNICODE", "/fo", tempResFile.getPath(),
                tempRcFile.getPath());

        // read compiled resources
        RESFile originalRes = new RESFile(tempResFile);

        /*
         * // copy MUI description from original file originalRes.getResources().put("MUI", new
         * TreeMap<Object, byte[]>()); byte[] muiDesc = mui.getCompiledResources().get("MUI").get(1);
         * originalRes.getResources().get("MUI").put(1, muiDesc);
         */

        /*
         * // copy MESSAGETABLE from original file TODO if
         * (mui.getCompiledResources().containsKey(ResUtils.TYPE_MESSAGETABLE)) {
         * originalRes.getResources().put(ResUtils.TYPE_MESSAGETABLE,
         * mui.getCompiledResources().get(ResUtils.TYPE_MESSAGETABLE)); }
         */

        places.fix(rcFileName, originalRes.getResources(), true);

        checkExistAndCompare(forParseBinRes, originalRes.getResources());
    }

    /**
     * Extract and compile message tables from rc file.
     */
    protected static void extractMT(File rcFile, File outRcFile) throws Exception {
        byte[] rc = FileUtils.readFileToByteArray(rcFile);
        BufferedReader rd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rc), "UTF-8"));
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outRcFile),
                "UTF-8"));
        String s;
        while ((s = rd.readLine()) != null) {
            Matcher m = RE_MESSAGETABLE_HEADER.matcher(s);
            if (m.matches()) {
                int mtid = Integer.parseInt(m.group(1));
                ResourceMessageTable messages = new ResourceMessageTable();

                while (!(s = rd.readLine()).trim().equals("{"))
                    ;
                while ((s = rd.readLine()) != null) {
                    if (s.trim().equals("}")) {
                        break;
                    }
                    m = RE_MESSAGETABLE_LINE.matcher(s);
                    if (!m.matches()) {
                        throw new Exception("Invalid messagetable line: " + s);
                    }
                    long id = Long.parseLong(m.group(1));
                    String text = ResUtils.unescape(m.group(2));
                    messages.messages.put(id, text);
                }
                byte[] mt = CompilerMessageTable.compile(messages);
                File f = new File(tempMtPrefix + mtid);
                FileUtils.writeByteArrayToFile(f, mt);
                wr.write(mtid + " MESSAGETABLE " + f.getPath() + "\n");
            } else {
                wr.write(s + "\n");
            }
        }
        wr.close();
    }

    /**
     * check if all original resources exist in the localized file, and compare they
     */
    protected static void checkExistAndCompare(Map<Object, Map<Object, byte[]>> r1,
            Map<Object, Map<Object, byte[]>> r2) throws Exception {
        for (Object e : r2.keySet()) {
            if (!r1.containsKey(e)) {
                throw new Exception("There is no object type " + ResUtils.getObjectType(e) + " in original");
            }
        }
        for (Object e : r1.keySet()) {
            if (!r2.containsKey(e)) {
                throw new Exception("There is no object type " + ResUtils.getObjectType(e) + " in compiled");
            }
        }

        for (Object e : r1.keySet()) {
            Map<Object, byte[]> enBin = r1.get(e);
            Map<Object, byte[]> enTxt = r2.get(e);
            if (enTxt != null) {
                for (Object in : enBin.keySet()) {
                    if (!enTxt.containsKey(in)) {
                        throw new Exception("There is no object " + ResUtils.getObjectType(e) + "/" + in
                                + " in compiled");
                    } else {
                        compare(enBin.get(in), enTxt.get(in),
                                "Different objects " + ResUtils.getObjectType(e) + "/" + in + ": ");
                    }
                }
                for (Object in : enTxt.keySet()) {
                    if (!enBin.containsKey(in)) {
                        throw new Exception("There is no object " + ResUtils.getObjectType(e) + "/" + in
                                + " in original");
                    }
                }
            }
        }
    }

    protected static void compare(byte[] d1, byte[] d2, String error) throws Exception {
        if (!Arrays.equals(d1, d2)) {
            FileUtils.writeByteArrayToFile(new File("d1"), d1);
            FileUtils.writeByteArrayToFile(new File("d2"), d2);
            throw new Exception(error);
            // Runtime.getRuntime().exec("sh ./hd.sh d1");
            // Runtime.getRuntime().exec("sh ./hd.sh d2");
        }
    }

    /**
     * Replace "en-US" to "be-BY" in the "MUI" resource.
     */
    protected static byte[] fixMUI(byte[] in, byte[] find, byte[] replace) throws Exception {
        for (int i = 0; i < in.length - find.length; i++) {
            boolean found = true;
            for (int j = 0; j < find.length; j++) {
                if (in[i + j] != find[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                byte[] r = new byte[in.length];
                System.arraycopy(in, 0, r, 0, in.length);
                System.arraycopy(replace, 0, r, i, replace.length);
                return r;
            }
        }
        throw new Exception("Can't fix MUI");
    }

    protected static void execRC(String... cmd) throws Exception {
        List<String> rc = new ArrayList<String>(Arrays.asList(cmd));

        boolean isWin = System.getProperty("os.name").startsWith("Windows");
        if (isWin) {
            rc.add(0, "RC\\rc.exe");
        } else {
            rc.add(0, "wine");
            rc.add(1, "RC/rc.exe");
        }
        String c = "";
        for (String r : rc) {
            c += r + ' ';
        }
        ByteArrayOutputStream pout = new ByteArrayOutputStream();
        ByteArrayOutputStream perr = new ByteArrayOutputStream();
        Process rcp = Runtime.getRuntime().exec(rc.toArray(new String[rc.size()]));
        IOUtils.copy(rcp.getInputStream(), pout);
        IOUtils.copy(rcp.getErrorStream(), perr);
        int result = rcp.waitFor();
        rcp.destroy();
        if (result != 0) {
            System.err.println("===========================================");
            System.err.println("Error execute external command: " + c);
            System.err.println("===========================================");
            IOUtils.copy(new ByteArrayInputStream(pout.toByteArray()), System.err);
            IOUtils.copy(new ByteArrayInputStream(perr.toByteArray()), System.err);
            throw new Exception("Error execute external command");
        }
    }

    protected static class JSENtry {
        String source, target;
        boolean processed;

        public JSENtry(String s, String t) {
            source = s;
            target = t;
        }
    }
}
