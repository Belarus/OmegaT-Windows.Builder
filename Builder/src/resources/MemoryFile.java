package resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MemoryFile {
    static final byte[] PADDING;
    static {
        try {
            PADDING = "PADDINGXXPADDING".getBytes("UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte buf[];
    private int count;
    private int pos;

    public MemoryFile() {
        buf = new byte[256];
        count = 0;
        pos = 0;
    }

    public MemoryFile(byte[] data) {
        buf = Arrays.copyOf(data, data.length);
        count = buf.length;
        pos = 0;
    }

    protected void expandBuffer(int minNewSize) {
        if (minNewSize > buf.length) {
            int newSize = ((minNewSize + 255) / 256) * 256;
            buf = Arrays.copyOf(buf, newSize);
        }
    }

    public byte[] getBytes() {
        byte[] r = new byte[count];
        System.arraycopy(buf, 0, r, 0, count);
        return r;
    }

    public int length() {
        return count;
    }

    public int pos() {
        return pos;
    }

    public void seek(int pos) {
        this.pos = pos;
    }

    public long readLong() {
        int i2 = readInt();
        int i1 = readInt();
        return ((long) (i1) << 32) + (i2 & 0xFFFFFFFFL);
    }

    public int read() {
        int r = buf[pos++];
        if (r < 0) {
            r = 256 + r;
        }
        return r;
    }

    public int readInt() {
        int ch4 = read();
        int ch3 = read();
        int ch2 = read();
        int ch1 = read();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public short readShort() {
        int ch2 = read();
        int ch1 = read();
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public int readWord() {
        int ch2 = read();
        int ch1 = read();
        return ((ch1 << 8) + (ch2 << 0));
    }

    public long readDWord() {
        long ch4 = read();
        long ch3 = read();
        long ch2 = read();
        long ch1 = read();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public byte readByte() {
        return (byte) read();
    }

    public void readFully(byte b[]) {
        System.arraycopy(buf, pos, b, 0, b.length);
        pos += b.length;
    }

    public void writeToFile(File f) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        out.write(buf, 0, count);
        out.close();
    }

    public void writeFully(byte[] a) {
        expandBuffer(pos + a.length);
        System.arraycopy(a, 0, buf, pos, a.length);
        pos = pos + a.length;
        if (count < pos) {
            count = pos;
        }
    }

    public final void writeLong(long v) {
        write((byte) (v >>> 0));
        write((byte) (v >>> 8));
        write((byte) (v >>> 16));
        write((byte) (v >>> 24));
        write((byte) (v >>> 32));
        write((byte) (v >>> 40));
        write((byte) (v >>> 48));
        write((byte) (v >>> 56));
    }

    public final void writeDWord(long v) {
        write((byte) (v >>> 0));
        write((byte) (v >>> 8));
        write((byte) (v >>> 16));
        write((byte) (v >>> 24));
    }

    public void writeInt(int v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 24) & 0xFF);
    }

    public void writeInt(long v) {
        write((byte) (v >>> 0));
        write((byte) (v >>> 8));
        write((byte) (v >>> 16));
        write((byte) (v >>> 24));
    }

    public void writeWord(int v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
    }

    public void writeShort(short v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
    }

    public void writeShort(int v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
    }

    public void writeByte(byte v) {
        write(v);
    }

    public synchronized void write(int b) {
        expandBuffer(pos + 1);
        buf[pos] = (byte) b;
        pos++;
        if (count < pos) {
            count = pos;
        }
    }

    public void read32bitPadding() {
        while (pos % 4 != 0) {
            pos++;
        }
    }

    public void write32bitFillPadding() {
        int c = ResUtils.ceil(pos, 4) - pos;
        byte[] p = new byte[c];
        System.arraycopy(PADDING, 0, p, 0, c);
        writeFully(p);
    }

    public void write32bitZeroPadding() {
        while (pos % 4 != 0) {
            write(0);
        }
    }

    public void write512bytesFillPadding() {
        writeFully(PADDING);
        int c = ResUtils.ceil(pos, 512) - pos;
        byte[] p = new byte[c];
        for (int pp = 0; pp < c; pp += PADDING.length) {
            System.arraycopy(PADDING, 0, p, pp, Math.min(PADDING.length, c - pp));
        }

        writeFully(p);
    }
}
