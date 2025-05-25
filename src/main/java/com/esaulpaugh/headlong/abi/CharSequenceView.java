/*
   Copyright 2025 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

/** An immutable view into a {@link CharSequence}, for use as a HashMap key when a substring is too slow. */
final class CharSequenceView implements CharSequence {

    private final CharSequence source;
    private final int start;
    private final int end;

    CharSequenceView(CharSequence source) {
        this(source, 0, source.length());
    }

    private CharSequenceView(CharSequence source, int start, int end) { // no bounds checking!!
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
    public CharSequenceView subSequence(int start, int end) {
//        assert end >= start;
//        assert start >= 0;
//        assert length() >= end;
        return new CharSequenceView(source, this.start + start, this.start + end);
    }

    int lastArrayOpen(int fromIdx) {
        for (int i = start + fromIdx;  ; i--) {
            if (source.charAt(i) == '[') {
                return i - start;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
//        if (!(obj instanceof CharSequenceView)) return false;
        final CharSequenceView o = (CharSequenceView) obj;
        if (length() != o.length()) return false;
        for (int i = start, oi = o.start; i < end; i++, oi++) {
            if (source.charAt(i) != o.source.charAt(oi)) return false;
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

    public long getFourCharLong(int index) {
        return fourCharLong(source, start + index);
    }

    public static long fourCharLong(CharSequence cs) {
        return fourCharLong(cs, 0);
    }

    private static long fourCharLong(CharSequence cs, int offset) {
        return (long)cs.charAt(offset) << 48
                | (long)cs.charAt(offset + 1) << 32
                | (long)cs.charAt(offset + 2) << 16
                | cs.charAt(offset + 3);
    }
}
