import java.io.File;

public class RemoveNonEnUS {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new Exception("Arg should be dir");
        }
    }

    static void processDir(File dir) throws Exception {
        System.out.println("Process dir " + dir);
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                processDir(f);
            } else {
                processFile(f);
            }
        }
        files = dir.listFiles();
        if (files.length == 0) {
            dir.delete();
        }
    }

    static void processFile(File f) throws Exception {
        if (!f.getPath().toUpperCase().contains("EN-US")) {
            f.delete();
        }
    }
}
