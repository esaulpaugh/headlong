package com.esaulpaugh.headlong.rlp.codec.example;

import com.esaulpaugh.headlong.rlp.codec.RLPAdapter;
import com.esaulpaugh.headlong.rlp.codec.RLPCodec;
import com.esaulpaugh.headlong.rlp.codec.RLPItem;
import com.esaulpaugh.headlong.rlp.codec.RLPList;
import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.codec.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.codec.util.Integers;
import com.esaulpaugh.headlong.rlp.codec.util.Strings;

import java.math.BigDecimal;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.codec.util.Strings.UTF_8;

public class StudentRLPAdapter implements RLPAdapter<Student> {

    @Override
    public Student fromRLP(byte[] rlp) throws DecodeException {
        RLPList rlpList = (RLPList) RLPCodec.wrap(rlp);
//        System.out.println(rlpList.toString());
        List<RLPItem> fields = rlpList.elements();
        return new Student(
                fields.get(0).asString(UTF_8),
                fields.get(1).asFloat(),
                fields.get(2).asBigInt(),
                new BigDecimal(fields.get(3).asBigInt(), fields.get(4).asInt())
        );
    }

    @Override
    public byte[] toRLP(Student student) {
        return RLPCodec.encodeAsList(
                Strings.decode(student.getName(), UTF_8),
                FloatingPoint.toBytes(student.getGpa()),
                student.getPublicKey().toByteArray(),
                student.getBalance().unscaledValue().toByteArray(),
                Integers.toBytes(student.getBalance().scale())
        );
    }
}
