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

/** A tuple with three values. */
public final class Triple<A, B, C> extends Tuple {

    Triple(Object[] values) {
        super(values);
    }

    /** @return the element at index 0, or {@code null} if this tuple resulted from {@link TupleType#decode(java.nio.ByteBuffer, int...)} and index 0 was omitted. */
    @SuppressWarnings("unchecked")
    public A get0() {
        return (A) elements[0];
    }

    /** @return the element at index 1, or {@code null} if this tuple resulted from {@link TupleType#decode(java.nio.ByteBuffer, int...)} and index 1 was omitted. */
    @SuppressWarnings("unchecked")
    public B get1() {
        return (B) elements[1];
    }

    /** @return the element at index 2, or {@code null} if this tuple resulted from {@link TupleType#decode(java.nio.ByteBuffer, int...)} and index 2 was omitted. */
    @SuppressWarnings("unchecked")
    public C get2() {
        return (C) elements[2];
    }
}
