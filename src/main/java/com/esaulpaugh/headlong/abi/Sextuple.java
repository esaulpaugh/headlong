/*
   Copyright 2024-2026 Evan Saulpaugh

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

import java.nio.ByteBuffer;

/** A tuple with six values. */
public final class Sextuple<A, B, C, D, E, F> extends Tuple {

    Sextuple(Object[] values) {
        super(values);
    }

    /** @return the element at index 0, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 0 was omitted. */
    @SuppressWarnings("unchecked")
    public A get0() {
        return (A) elements[0];
    }

    /** @return the element at index 1, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 1 was omitted. */
    @SuppressWarnings("unchecked")
    public B get1() {
        return (B) elements[1];
    }

    /** @return the element at index 2, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 2 was omitted. */
    @SuppressWarnings("unchecked")
    public C get2() {
        return (C) elements[2];
    }

    /** @return the element at index 3, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 3 was omitted. */
    @SuppressWarnings("unchecked")
    public D get3() {
        return (D) elements[3];
    }

    /** @return the element at index 4, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 4 was omitted. */
    @SuppressWarnings("unchecked")
    public E get4() {
        return (E) elements[4];
    }

    /** @return the element at index 5, or {@code null} if this tuple resulted from {@link TupleType#decode(ByteBuffer, int...)} and index 5 was omitted. */
    @SuppressWarnings("unchecked")
    public F get5() {
        return (F) elements[5];
    }
}
