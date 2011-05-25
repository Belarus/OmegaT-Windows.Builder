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

package win7;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import resources.ResUtils;
import resources.ResourceID;

/**
 * Тут пазначаныя рэсурсы, якія мы не апрацоўваем.
 */
public class SkipResources {

    private static final Pattern[] NULEOL_FILES = new Pattern[] {
            Pattern.compile("Windows/System32/en-US/rasdlg.dll_6.1.7600.16385_x32.rc"),
            Pattern.compile("Windows/System32/en-US/VAN.dll_6.1.7600.16385_x32.rc"),
            Pattern.compile("Windows/System32/en-US/rasmm.dll_6.1.7600.16385_x32.rc"),
            Pattern.compile("Windows/System32/en-US/netshell.dll_6.1.7600.16385_x32.rc"),
            Pattern.compile("Windows/System32/en-US/tcpipcfg.dll_6.1.7600.16385_x32.rc"),
            Pattern.compile("Windows/System32/en-US/wlanpref.dll_6.1.7600.16385_x32.rc"), };

    public static boolean isNulEolFile(String fn) {
        return isRegexListContains(NULEOL_FILES, fn);
    }

    private static boolean isRegexListContains(Pattern[] regexs, String str) {
        for (Pattern p : regexs) {
            if (p.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    public static final Set<String> SKIP_FILES = new HashSet<String>(
            Arrays.asList("Windows/System32/en-US/wuaueng.dll_7.4.7600.226_x32.rc"));

    public static boolean isSkippedFile(String fn) {
        return SKIP_FILES.contains(fn);
    }

    public static final ResourceID[] SKIP_EXTRACT = new ResourceID[] {
            /* Блёкі вэрсій перакладаць ня мае сэнсу, і часам яны невалідныя. */
            new ResourceID(null, ResUtils.TYPE_VERSION),
            /* HTML пакуль не перакладаем */
            new ResourceID(null, ResUtils.TYPE_HTML),
            /* MUI */
            new ResourceID(null, "MUI"),
            /* дзіўны тып */
            new ResourceID(null, 240),
            /* дзіўны тып */
            new ResourceID(null, 2110),
            /* Невалідны блёк */
            new ResourceID("Windows/System32/en\\-US/mspaint.exe_6.1.7600.16385_x32.rc",
                    ResUtils.TYPE_DIALOG, 134),
            /* Невалідны блёк */
            new ResourceID("Windows/System32/en\\-US/winsrv.dll_6.1.7600.16385_x32.rc", ResUtils.TYPE_DIALOG,
                    21, 22),
            /* Невалідны блёк(extraCount у нестандартавым кантроде ў dialog item) */
            new ResourceID("Windows/System32/en\\-US/wmploc.DLL_12.0.7600.16385_x32.rc",
                    ResUtils.TYPE_DIALOG, 1150, 1170, 1360, 1368, 2009, 2043, 2049, 2070) };

    public static final ResourceID[] SKIP_COMPARE = new ResourceID[] {
            /* Немагчыма стварыць такі самы dialog style. */
            new ResourceID("Program Files/Internet Explorer/en-US/jsdbgui.dll_9.0.8112.16421_x32.rc",
                    ResUtils.TYPE_DIALOG, 632),
            /* Немагчыма стварыць такі самы dialog style. */
            new ResourceID("Windows/System32/en-US/sndvol.exe_6.1.7600.16385_x32.rc", ResUtils.TYPE_DIALOG,
                    204),
            /* Няправільны парадак радкоў */
            new ResourceID("Windows/System32/en-US/winsrv.dll_6.1.7600.16385_x32.rc",
                    ResUtils.TYPE_MESSAGETABLE, 1),
            /* Няправільны парадак радкоў */
            new ResourceID("Windows/System32/en-US/ntshrui.dll_6.1.7600.16385_x32.rc",
                    ResUtils.TYPE_MESSAGETABLE, 1) };

    private static final Pattern[] SKIP_COMPARE_FILES = new Pattern[] {
    /* Padding напрыканцы файла занадта доўгі. */
    Pattern.compile("Windows/System32/en-US/pnidui.dll_6.1.7600.16385_x32.rc"),
    /* Padding напрыканцы файла занадта доўгі. */
    Pattern.compile("Windows/System32/en-US/msutb.dll_6.1.7600.16385_x32.rc"),
    /* Padding напрыканцы файла занадта доўгі. */
    Pattern.compile("Windows/System32/en-US/werui.dll_6.1.7600.16385_x32.rc"),
    /* Padding у загалоўках рэсурсаў */
    Pattern.compile("Windows/System32/en-US/calc.exe_6.1.7600.16385_x32.rc"),
    /* Загалоўкі рэсурсаў */
    Pattern.compile("Windows/System32/en-US/ieframe.dll_8.0.7600.16385_x32.rc"),
    /* Загалоўкі рэсурсаў */
    Pattern.compile("Windows/System32/en-US/ieframe.dll_9.0.8112.16421_x32.rc"),
    /** */
    Pattern.compile("Windows/System32/en-US/wmploc.DLL_12.0.7600.16385_x32.rc"),
    /** */
    Pattern.compile("Windows/System32/en-US/searchfolder.dll_6.1.7600.16385_x32.rc"),
    /** padding */
    Pattern.compile("Windows/System32/en-US/ipconfig.exe_6.1.7600.16385_x32.rc"),
    /** padding */
    Pattern.compile("Windows/System32/en-US/inetcpl.cpl_8.0.7600.16385_x32.rc"),
    /* Padding у загалоўках рэсурсаў */
    Pattern.compile("x(32|64)sp(0|1)/Windows/ehome/en\\-US/ehres.dll.mui"),
    /** */
    Pattern.compile("Program Files/Windows Photo Viewer/en-US/PhotoAcq.dll_6.1.7600.16385_x32.rc") };

    public static boolean isSkipCompareFiles(String fn) {
        return isRegexListContains(SKIP_COMPARE_FILES, fn);
    }

    public static <T> Map<Object, Map<Object, T>> minus(String filename, Map<Object, Map<Object, T>> source,
            ResourceID[] skips) {
        Map<Object, Map<Object, T>> result = new TreeMap<Object, Map<Object, T>>(
                ResUtils.stringIntegerComparator);
        for (Map.Entry<Object, Map<Object, T>> rt : source.entrySet()) {
            Map<Object, T> rest = null;

            for (Map.Entry<Object, T> ri : rt.getValue().entrySet()) {
                boolean add = true;
                for (ResourceID s : skips) {
                    if (s.contains(filename, rt.getKey(), ri.getKey())) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    if (rest == null) {
                        rest = new TreeMap<Object, T>(ResUtils.stringIntegerComparator);
                        result.put(rt.getKey(), rest);
                    }
                    rest.put(ri.getKey(), ri.getValue());
                }
            }
        }
        return result;
    }
}
