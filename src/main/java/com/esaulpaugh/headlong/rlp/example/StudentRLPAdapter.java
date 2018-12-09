package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;

import java.math.BigDecimal;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class StudentRLPAdapter implements RLPAdapter<Student> {

    @Override
    public Student decode(byte[] rlp, int index) throws DecodeException {

        RLPList.Iterator iter = RLP_STRICT.listIterator(rlp, index);

        return new Student(iter.next().asString(UTF_8),
                iter.next().asFloat(),
                iter.next().asBigInt(),
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
