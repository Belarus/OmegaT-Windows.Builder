package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import resources.ResUtils;

public class ListMUI {
    /**
     * Вяртае усе MUI-файлы, але 64-бітавыя дублікаты запісваюцца асобна.
     */
    public static List<MUInfo> listMUI(File dir) throws Exception {
        List<MUInfo> result = new ArrayList<MUInfo>();
        Map<String, File> muis = ResUtils.listFiles(dir, "mui");
        for (String m : muis.keySet()) {
            String m32 = null;
            String m64 = null;
            if (m.endsWith("_x32.mui")) {
                m32 = m;
                m64 = m32.replaceAll("_x32.mui$", "_x64.mui");
            } else if (m.endsWith("_x64.mui")) {
                m64 = m;
                m32 = m64.replaceAll("_x64.mui$", "_x32.mui");
            } else {
                Assert.fail();
            }
            File f32 = muis.get(m32);
            File f64 = muis.get(m64);

            if (f32 != null && m64.equals(m)) {
                continue;
            }
            MUInfo mui = new MUInfo();
            if (f32 != null) {
                mui.resourceFileName = m32.replaceAll("\\.mui$", ".rc");
                mui.file32 = f32;
                mui.file32path = m32;
                mui.file64 = f64;
                if (mui.file64 != null) {
                    mui.file64path = m64;
                }
            } else {
                mui.resourceFileName = m64.replaceAll("\\.mui$", ".rc");
                mui.file32 = null;
                mui.file64 = f64;
                mui.file64path = m64;
            }
            result.add(mui);
        }

        return result;
    }

    public static class MUInfo {
        public String resourceFileName;
        public String file32path, file64path;
        public File file32, file64;
    }
}
