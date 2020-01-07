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
package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;

import java.math.BigDecimal;
import java.util.Iterator;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class RLPStudentAdapter implements RLPAdapter<RLPStudent> {

    @Override
    public RLPStudent decode(byte[] rlp, int index) throws DecodeException {

        Iterator<RLPItem> iter = RLP_STRICT.listIterator(rlp, index);

        return new RLPStudent(iter.next().asString(UTF_8),
                iter.next().asFloat(),
                iter.next().asBytes(),
                new BigDecimal(iter.next().asBigInt(), iter.next().asInt())
        );

//        RLPList rlpList = RLP_STRICT.wrapList(rlp, index);
//        List<RLPItem> elements = rlpList.elements(RLP_STRICT);
//        return new RLPStudent(
//                elements.get(0).asString(UTF_8),
//                elements.get(1).asFloat(),
//                elements.get(2).asBytes(),
//                new BigDecimal(elements.get(3).asBigInt(), elements.get(4).asInt())
//        );
    }

    @Override
    public byte[] encode(RLPStudent student) {
        return RLPEncoder.encodeAsList(student.toObjectArray());
    }
}
