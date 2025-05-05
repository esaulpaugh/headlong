package com.esaulpaugh.headlong.abi;

final class Slice implements CharSequence {

    private final CharSequence source;
    private final int start;
    private final int end;

    Slice(CharSequence source) {
        this(source, 0, source.length());
    }

    private Slice(CharSequence source, int start, int end) { // no bounds checking!!
        this.source = source;
        this.start = start;
        this.end = end;
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        return source.charAt(start + index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return slice(start, end);
    }

    public int lastIndexOf(int ch, int fromIdx) {
        for (int i = Math.min(length() - 1, fromIdx); i >= 0; i--) {
            if (ch == charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public Slice slice(int start, int end) {
        if (start > end || start < 0 || end > length()) {
            throw new StringIndexOutOfBoundsException();
        }
        return new Slice(source, this.start + start, this.start + end);
    }

//    public String substring(int start, int end) {
//        return source.subSequence(this.start + start, this.start + end).toString();
//    }

    public void append(StringBuilder sb, int start) {
        sb.append(this, start, length());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Slice)) return false;
        final Slice o = (Slice) obj;
        final int len = length();
        if (len != o.length()) return false;
        for (int i = 0; i < len; i++) {
            if (source.charAt(start + i) != o.source.charAt(o.start + i)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = start; i < end; i++) {
            h = 31 * h + source.charAt(i);
        }
        return h;
    }

    @Override
    public String toString() {
        return source.subSequence(start, end).toString();
    }
}
