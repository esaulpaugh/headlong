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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes RLP object notation as defined by the {@link Notation} class.
 */
public class NotationParser {

    private static final int LIST = 0;
    private static final int STRING = 1;

    private static final int LIST_PREFIX_LEN = Notation.BEGIN_LIST.length();
    private static final int LIST_SUFFIX_LEN = Notation.END_LIST.length();
    private static final int STRING_PREFIX_LEN = Notation.BEGIN_STRING.length();
    private static final int STRING_SUFFIX_LEN = Notation.END_STRING.length();

    /**
     * Returns the object hierarchy represented by the notation.
     *
     * @param notation  the notation to be parsed
     * @return  the hierarchy of objects
     */
    public static List<Object> parse(String notation) {
        List<Object> topLevelObjects = new ArrayList<>(); // a sequence (as in encodeSequentially)
        parse(notation, 0, notation.length(), topLevelObjects, new int[2]);
        return topLevelObjects;
    }

    private static int parse(String notation, int i, final int end, List<Object> parent, int[] resultHolder) {

        int nextArrayEnd = -1;

        while (i < end) {
            if(!findNextObject(notation, i, resultHolder)) {
                return Integer.MAX_VALUE;
            }

            if(i > nextArrayEnd) { // only update nextArrayEnd when i has passed it
                nextArrayEnd = notation.indexOf(Notation.END_LIST, i);
                if(nextArrayEnd < 0) {
                    nextArrayEnd = Integer.MAX_VALUE;
                }
            }

            int nextObjectIndex = resultHolder[0];

            if(nextArrayEnd < nextObjectIndex) {
                return nextArrayEnd + LIST_SUFFIX_LEN;
            }

            switch (/* nextObjectType */ resultHolder[1]) {
            case STRING:
                int datumStart = nextObjectIndex + STRING_PREFIX_LEN;
                int datumEnd = notation.indexOf(Notation.END_STRING, datumStart);
                parent.add(Strings.decode(notation.substring(datumStart, datumEnd)));
                i = datumEnd + STRING_SUFFIX_LEN;
                continue;
            case LIST:
                List<Object> childList = new ArrayList<>();
                i = parse(notation, nextObjectIndex + LIST_PREFIX_LEN, end, childList, resultHolder);
                parent.add(childList);
                continue;
            default:
            }
        }

        return end + LIST_SUFFIX_LEN;
    }

    private static boolean findNextObject(String notation, int startIndex, int[] resultHolder) {
        final int indexString = notation.indexOf(Notation.BEGIN_STRING, startIndex);
        final int indexList = notation.indexOf(Notation.BEGIN_LIST, startIndex);
        if(indexString == -1) {
            if(indexList == -1) {
                return false;
            }
        } else if(indexString < indexList || indexList == -1) {
            resultHolder[0] = indexString;
            resultHolder[1] = STRING;
            return true;
        }
        // indexString == -1 || indexList <= indexString
        resultHolder[0] = indexList;
        resultHolder[1] = LIST;
        return true;
    }
}
