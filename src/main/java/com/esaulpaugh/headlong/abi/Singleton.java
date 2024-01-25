/*
   Copyright 2024 Evan Saulpaugh

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

public final class Singleton<A> extends Tuple {

    Singleton(Object[] elements) {
        super(elements);
    }

    public static <A> Singleton<A> of(A a) {
        return new Singleton<>(new Object[] { Tuple.requireNotNull(a, 0) });
    }

    @SuppressWarnings("unchecked")
    public A get0() {
        return (A) elements[0];
    }
}
