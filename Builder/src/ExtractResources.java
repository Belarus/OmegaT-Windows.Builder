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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;

import resources.ParserRES;
import resources.ResUtils;
import resources.io.WriterRC;
import win7.SkipResources;

/**
 * Распакоўка рэсурсаў з .mui файлаў у .rc.
 */
public class ExtractResources {

    static String sPath = "mui-bin/";
    static String tPath = "e:/sf.net/i18n-bel/Windows7/source/";

    static int errors = 0;

    public static void main(String[] args) throws Exception {
        Map<String, File> filesExist = ResUtils.listFiles(new File(tPath), null);
        Map<String, File> filesMUI = ResUtils.listFiles(new File(sPath), null);
        filesMUI.remove("list.txt");
        filesExist.remove("list.txt");

        // тэкст rc,сьпіс файлаў у якіх такі rc
        Map<FileContent, List<String>> contents = new HashMap<FileContent, List<String>>();
        for (Map.Entry<String, File> f : filesMUI.entrySet()) {
            String fspath = f.getKey();
            try {
                System.out.println("Processing: " + fspath);

                FileContent content;
                if (fspath.endsWith(".mui")) {
                    String rcText = parseMUI(fspath, f.getValue());
                    content = new FileContent(rcText.getBytes(Charset.forName("UTF-8")));
                } else {
                    content = new FileContent(parseOther(f.getValue()));
                }
                List<String> fnames = contents.get(content);
                if (fnames == null) {
                    fnames = new ArrayList<String>();
                    contents.put(content, fnames);
                }
                fnames.add(fspath);
            } catch (Exception ex) {
                System.err.println("Error in " + fspath);
                ex.printStackTrace();
                errors++;
            }
        }

        Map<String, FileContent> contentByNames = new TreeMap<String, ExtractResources.FileContent>();
        for (Map.Entry<FileContent, List<String>> c : contents.entrySet()) {
            String fo = fileToName(c.getValue().get(0));
            contentByNames.put(fo, c.getKey());
        }

        Writer wr = new OutputStreamWriter(new FileOutputStream(sPath + "/list.txt"), "UTF-8");
        for (Map.Entry<String, FileContent> fc : contentByNames.entrySet()) {
            FileContent cKey = fc.getValue();
            List<String> cValue = contents.get(cKey);
            String fo = fileToName(cValue.get(0));
            wr.write(fo + "\n");
            for (String f : cValue) {
                wr.write("+" + f + "\n");
            }
            wr.write("\n");
            File f = new File(tPath + fo);
            f.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(f, cKey.byteContent);

            filesExist.remove(fo);
        }
        wr.close();

        for (String f : filesExist.keySet()) {
            if (!f.contains("/.svn/") && !f.startsWith(".svn/")) {
                System.out.println("Exist, but not converted: " + f);
            }
        }
        System.out.println("Error count: " + errors);
    }

    protected static String parseMUI(String fspath, File f) throws Exception {
        ReaderWriterMUI mui = new ReaderWriterMUI(FileUtils.readFileToByteArray(f));

        Map<Object, Map<Object, byte[]>> minused = SkipResources.minus(fspath, mui.getCompiledResources(),
                SkipResources.SKIP_EXTRACT);

        ParserRES res = new ParserRES(minused);

        return new WriterRC().write(res.getParsedResources());
    }

    protected static byte[] parseOther(File f) throws Exception {
        return FileUtils.readFileToByteArray(f);
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
