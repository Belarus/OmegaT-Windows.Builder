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

package resources;

import java.util.Map;

/**
 * Дэкампілятар рэсурсаў дыялогаў.
 * 
 * http://msdn.microsoft.com/en-us/library/ms645398(v=VS.85).aspx
 */
public class ParserDialog {

    private final MemoryFile data;
    private ResourceDialog dialog;

    public ParserDialog(MemoryFile data) {
        this.data = data;
    }

    public ResourceDialog parse() throws Exception {
        dialog = new ResourceDialog();
        readDlgTemplateEx();

        for (int i = 0; i < dialog.cDlgItems; i++) {
            ResourceDialog.DlgItemTemplateEx item = readDlgItemTemplateEx(data);
            if (item.extraCount != 0) {
                throw new Exception("extraCount!=0");
            }
            dialog.items.add(item);
        }

        return dialog;
    }

    public void write(ResourceDialog dialog) throws Exception {
        this.dialog = dialog;

        writeDlgTemplateEx();

        for (ResourceDialog.DlgItemTemplateEx item : dialog.items) {
            writeDlgItemTemplateEx(item);
        }
    }

    protected String getFlags(Map<Long, String> allFlags, long value) {
        StringBuilder r = new StringBuilder();
        for (Map.Entry<Long, String> f : allFlags.entrySet()) {
            if ((f.getKey() & value) != 0) {
                r.append(f.getValue()).append(" | ");
            }
        }
        if (r.length() > 0) {
            r.setLength(r.length() - 3);
        }
        return r.toString();
    }

    public void readDlgTemplateEx() throws Exception {
        dialog.dlgVer = data.readWord();
        dialog.signature = data.readWord();
        dialog.helpID = data.readDWord();
        dialog.exStyle = data.readDWord();
        dialog.style = data.readDWord();
        dialog.cDlgItems = data.readWord();
        dialog.x = data.readShort();
        dialog.y = data.readShort();
        dialog.cx = data.readShort();
        dialog.cy = data.readShort();
        dialog.menu = (Integer) ResUtils.sz_Or_Ord(data);
        dialog.windowClass = ResUtils.sz_Or_Ord(data);
        dialog.title = ResUtils.readUnicodeString0(data);
        dialog.pointsize = data.readWord();
        dialog.weight = data.readWord();
        dialog.italic = data.readByte();
        dialog.charset = data.readByte();
        dialog.typeface = ResUtils.readUnicodeString0(data);
    }

    private void writeDlgTemplateEx() {
        data.writeWord(dialog.dlgVer);
        data.writeWord(dialog.signature);
        data.writeDWord(dialog.helpID);
        data.writeDWord(dialog.exStyle);
        data.writeDWord(dialog.style);
        data.writeWord(dialog.cDlgItems);
        data.writeShort(dialog.x);
        data.writeShort(dialog.y);
        data.writeShort(dialog.cx);
        data.writeShort(dialog.cy);
        ResUtils.write_sz_Or_Ord(dialog.menu, data);
        ResUtils.write_sz_Or_Ord(dialog.windowClass, data);
        ResUtils.writeUnicodeString0(data, dialog.title);
        data.writeWord(dialog.pointsize);
        data.writeWord(dialog.weight);
        data.writeByte(dialog.italic);
        data.writeByte(dialog.charset);
        ResUtils.writeUnicodeString0(data, dialog.typeface);
    }

    public ResourceDialog.DlgItemTemplateEx readDlgItemTemplateEx(MemoryFile data) throws Exception {
        data.read32bitPadding();

        ResourceDialog.DlgItemTemplateEx r = new ResourceDialog.DlgItemTemplateEx();

        r.helpID = data.readDWord();
        r.exStyle = data.readDWord();
        r.style = data.readDWord();
        r.x = data.readShort();
        r.y = data.readShort();
        r.cx = data.readShort();
        r.cy = data.readShort();
        r.id = data.readDWord();
        r.windowClass = ResUtils.sz_Or_Ord(data);
        r.title = ResUtils.sz_Or_Ord(data);
        r.extraCount = data.readWord();

        return r;
    }

    public void writeDlgItemTemplateEx(ResourceDialog.DlgItemTemplateEx r) throws Exception {
        data.write32bitZeroPadding();

        data.writeDWord(r.helpID);
        data.writeDWord(r.exStyle);
        data.writeDWord(r.style);
        data.writeShort(r.x);
        data.writeShort(r.y);
        data.writeShort(r.cx);
        data.writeShort(r.cy);
        data.writeDWord(r.id);
        ResUtils.write_sz_Or_Ord(r.windowClass, data);
        ResUtils.write_sz_Or_Ord(r.title, data);
        data.writeWord(r.extraCount);
    }
}
