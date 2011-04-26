package win7;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

public class Utils {

    public static byte[] readZip(ZipFile zip, ZipEntry en) throws Exception {
        InputStream in = zip.getInputStream(en);
        try {
            return IOUtils.toByteArray(in);
        } finally {
            in.close();
        }
    }

}
