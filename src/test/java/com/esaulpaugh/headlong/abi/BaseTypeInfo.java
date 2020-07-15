/*
   Copyright 2019 Evan Saulpaugh

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An object to hold metadata about a base type. Metadata objects are stored in {@link #TYPE_INFO_MAP}.
 * <p>
 * fixed/ufixed types, which number in the thousands, are not included in the map (except for fixed, ufixed,
 * fixed128x18, and ufixed128x18).
 */
final class BaseTypeInfo {

    private static final int ADDRESS_BIT_LEN = 160;

    static final int DECIMAL_BIT_LEN = 128;
    static final int DECIMAL_SCALE = 10;

    static final int FIXED_BIT_LEN = 128;
    static final int FIXED_SCALE = 18;

    private static final int FUNCTION_BYTE_LEN = 24;

    private static final int N_A = -1;

    private static final Map<String, BaseTypeInfo> TYPE_INFO_MAP;

    static {
        final Map<String, BaseTypeInfo> map = new HashMap<>(256);

        for(int n = 8; n <= 256; n += 8) {
            BaseTypeInfo info = new BaseTypeInfo(n);
            map.put("int" + n, info);
            map.put("uint" + n, info);
        }
        map.put("int", map.get("int256"));
        map.put("uint", map.get("uint256"));

        for (int n = 1; n <= 32; n++) {
            map.put("bytes" + n, new BaseTypeInfo(N_A, n));
        }
        map.put("function", map.get("bytes" + FUNCTION_BYTE_LEN));

        final BaseTypeInfo present = new BaseTypeInfo(N_A, N_A);
        map.put("bytes", present);
        map.put("string", present);
        map.put("address", new BaseTypeInfo(ADDRESS_BIT_LEN));
        map.put("decimal", new BaseTypeInfo(DECIMAL_BIT_LEN));
        map.put("bool", present);

        BaseTypeInfo fixedType = new BaseTypeInfo(FIXED_BIT_LEN);
        map.put("fixed", fixedType);
        map.put("ufixed", fixedType);
        map.put("fixed128x18", fixedType);
        map.put("ufixed128x18", fixedType);

        TYPE_INFO_MAP = Collections.unmodifiableMap(map);
    }

    final int bitLen;
    final int arrayLen;

    private BaseTypeInfo(int bitLen) {
        this(bitLen, N_A);
    }

    private BaseTypeInfo(int bitLen, int arrayLen) {
        this.bitLen = bitLen;
        this.arrayLen = arrayLen;
    }

    /**
     * Returns the canonical base type's metadata object if it exists.
     *
     * @param canonical the canonical type string for the base type
     * @return the metadata object, or {@code null} if it does not exist
     */
    static BaseTypeInfo get(String canonical) {
        return TYPE_INFO_MAP.get(canonical);
    }

    static Map<String, BaseTypeInfo> getBaseTypeInfoMap() {
        return TYPE_INFO_MAP;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitLen, arrayLen);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseTypeInfo that = (BaseTypeInfo) o;
        return bitLen == that.bitLen && arrayLen == that.arrayLen;
    }
}
