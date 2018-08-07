package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPCodec;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.RLPAdapter;

import java.math.BigDecimal;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.util.Strings.UTF_8;

public class StudentRLPAdapter implements RLPAdapter<Student> {

    @Override
    public byte[] encode(Student student) {
        return RLPCodec.encodeAsList(student.toObjectArray());
    }

    @Override
    public Student decode(byte[] rlp, int index) throws DecodeException {
        RLPList rlpList = (RLPList) RLPCodec.wrap(rlp, index);
//        System.out.println(rlpList.toString());
        List<RLPItem> elements = rlpList.elements();
        return new Student(
                elements.get(0).asString(UTF_8),
                elements.get(1).asFloat(),
                elements.get(2).asBigInt(),
                new BigDecimal(elements.get(3).asBigInt(), elements.get(4).asInt())
        );
    }
}
