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
package com.esaulpaugh.headlong.example;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPListIterator;

import java.math.BigDecimal;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class StudentRLPAdapter implements RLPAdapter<Student> {

    @Override
    public Student decode(byte[] rlp, int index) throws DecodeException {

        RLPListIterator iter = RLP_STRICT.listIterator(rlp, index);

        return new Student(iter.next().asString(UTF_8),
                iter.next().asFloat(),
                iter.next().data(),
                new BigDecimal(iter.next().asBigInt(), iter.next().asInt())
        );

//        RLPList rlpList = RLP_STRICT.wrapList(rlp, index);
//        List<RLPItem> elements = rlpList.elements(RLP_STRICT);
//        return new Student(
//                elements.get(0).asString(UTF_8),
//                elements.get(1).asFloat(),
//                elements.get(2).asBigInt(),
//                new BigDecimal(elements.get(3).asBigInt(), elements.get(4).asInt())
//        );
    }

    @Override
    public byte[] encode(Student student) {
        return RLPEncoder.encodeAsList(student.toObjectArray());
    }
}
