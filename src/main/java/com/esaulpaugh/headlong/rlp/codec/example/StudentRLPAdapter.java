package com.esaulpaugh.headlong.rlp.codec.example;

import com.esaulpaugh.headlong.rlp.codec.RLPAdapter;
import com.esaulpaugh.headlong.rlp.codec.RLPCodec;
import com.esaulpaugh.headlong.rlp.codec.RLPItem;
import com.esaulpaugh.headlong.rlp.codec.RLPList;
import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.codec.util.Integers;

import java.util.List;

import static com.esaulpaugh.headlong.rlp.codec.util.Strings.UTF_8;
import static com.esaulpaugh.headlong.rlp.codec.util.Strings.fromUtf8;

/**
 *
 */
public class StudentRLPAdapter implements RLPAdapter<Student> {

    @Override
    public Student fromRLP(byte[] rlp) throws DecodeException {
        RLPList rlpList = (RLPList) RLPCodec.wrap(rlp);
//        System.out.println(rlpList.toString());
        List<RLPItem> fields = rlpList.elements();
        return new Student(
                fields.get(0).data(UTF_8),
                Float.intBitsToFloat(fields.get(1).asInt())
        );
    }

    @Override
    public byte[] toRLP(Student student) {
        return RLPCodec.encodeAsList(
                fromUtf8(student.getName()),
                Integers.toBytes(Float.floatToIntBits(student.getGpa()))
        );
    }
}
