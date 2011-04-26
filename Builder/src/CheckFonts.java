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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import muifile.ReaderWriterMUI;

import org.apache.commons.io.FileUtils;

import resources.MemoryFile;
import resources.ParserDialog;
import resources.ResUtils;
import resources.ResourceDialog;
import win7.SkipResources;

/**
 * Спраўджваем ці хапае месца для перакладу ў дыялогах, бо беларускі тэкст можа патрабаваць больш месца за
 * ангельскі.
 */
public class CheckFonts {
    static File OUT_DIR = new File("../out/");

    public static void main(String[] a) throws Exception {
        Map<String, File> filesMUI = ResUtils.listFiles(OUT_DIR, "mui");

        for (Map.Entry<String, File> e : filesMUI.entrySet()) {
            System.out.println(e.getKey());
            ReaderWriterMUI mui = new ReaderWriterMUI(FileUtils.readFileToByteArray(e.getValue()));
            Map<Object, Map<Object, byte[]>> res = mui.getCompiledResources();

            res = SkipResources.minus(e.getKey().replace("/be-BY/", "/en-US/"), res,
                    SkipResources.SKIP_EXTRACT);

            Map<Object, byte[]> dialogs = res.get(ResUtils.TYPE_DIALOG);
            if (dialogs == null) {
                continue;
            }
            for (Map.Entry<Object, byte[]> en : dialogs.entrySet()) {
                ResourceDialog dialog = new ParserDialog(new MemoryFile(en.getValue())).parse();
                for (ResourceDialog.DlgItemTemplateEx it : dialog.items) {
                    if (!(it.title instanceof String)) {
                        continue;
                    }
                    // int linesCount = 1;
                    // try {
                    // int h = it.cy;
                    // if (h == dialog.pointsize - 1) {
                    // h++;
                    // }
                    // linesCount = h / dialog.pointsize;
                    // } catch (ArithmeticException ex) {
                    // ex.printStackTrace();
                    // System.out.println(en.getKey() + "/" + it.id + " - zero height");
                    // }
                    // if (linesCount == 0) {
                    // System.out.println(en.getKey() + "/" + it.id + " - zero height");
                    // continue;
                    // }
                    int w = getDimension(dialog, (String) it.title).width;
                    if (w > it.cx) {
                        System.out.println(en.getKey() + "/" + it.id + " - \"" + it.title + "\" need width: "
                                + w + ", but width: " + it.cx + " [" + it.x + "," + it.y + "," + it.cx + ","
                                + it.cy + "]");
                    }
                }
            }
        }
    }

    static Map<String, Font> FONTS = new TreeMap<String, Font>();

    protected static Font getDialogFont(ResourceDialog dialog) throws Exception {
        Font r = FONTS.get(dialog.typeface);
        if (r == null) {
            r = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/" + dialog.typeface.replace(' ', '_')
                    + ".ttf"));
            FONTS.put(dialog.typeface, r);
        }
        return r;
    }

    /**
     * Вяртае памер тэксту ў dialog base unit'ах.
     * 
     * Адзін юніт: па гарызанталі - чвэрць ад сярэдняй шырыні літары, па вертыкалі - восьмая ад сярэдняй
     * вышыні літары.
     */
    protected static Dimension getDimension(ResourceDialog dialog, String text) throws Exception {
        int fontPixelsSize = dialog.pointsize * 96 / 72;

        Font f = getDialogFont(dialog).deriveFont(dialog.italic != 0 ? Font.ITALIC : 0, fontPixelsSize);

        // BufferedImage img = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
        // Graphics gr = img.getGraphics();
        // FontMetrics fm = gr.getFontMetrics(f);
        // r = fm.stringWidth(text);

        FontRenderContext frc = new FontRenderContext(null, false, false);
        Rectangle2D strDialogUnits = f.getStringBounds(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", frc);
        // Колькасьць піксэляў у юніце
        double dbuX = strDialogUnits.getWidth() / 52 / 4;
        double dbuY = strDialogUnits.getHeight() / 8;

        Rectangle2D strRect = f.getStringBounds(text.replace("&", ""), frc);
        int w = (int) Math.ceil(strRect.getWidth() / dbuX);

        int h = (int) Math.ceil(strRect.getHeight() / dbuY);

        return new Dimension(w, h);
    }
}
