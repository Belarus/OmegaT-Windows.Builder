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

/**
 * Дэкампілятар рэсурсаў String Table.
 */
public class ParserString {

    public static ResourceStringBlock parse(MemoryFile data) throws Exception {
        ResourceStringBlock result = new ResourceStringBlock();
        result.strings = new String[16];
        for (int i = 0; i < 16; i++) {
            int charCount = data.readShort();
            if (charCount > 0) {
                String s = ResUtils.readUnicodeString(data, charCount * 2);
                if (s.charAt(s.length() - 1) == '\0') {
                    s = s.substring(0, s.length() - 1);
                }
                result.strings[i] = s;
            }
        }

        return result;
    }
}
