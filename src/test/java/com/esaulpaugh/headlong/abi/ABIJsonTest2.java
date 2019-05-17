package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.util.Integers;
import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;

public class ABIJsonTest2 {

    private static final String RESOURCE = "tests/ethers-io/tests/tests/contract-interface-abi2.json";

    private static final JsonArray TEST_CASES;

    static {
        try {
            TEST_CASES = JsonUtils.parseArray(TestUtils.readResourceAsString(ABIJsonTest.class, RESOURCE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestCase {

        private static final Integers.UintType ADDRESS = new Integers.UintType(160);

        private final String name;

        private final TupleType types;
        private final Tuple values;
        private final byte[] result;

        public TestCase(JsonObject object) throws ParseException {
            this.name = object.get("name").getAsString();
            String typesStr = object.get("types").getAsString();
            String valuesStr = object.get("values").getAsString();
            String resultStr = object.get("result").getAsString();

            JsonArray typesArray = JsonUtils.parseArray(typesStr);
            JsonArray valuesArray = JsonUtils.parseArray(valuesStr);

            StringBuilder tupleTypeString = new StringBuilder("(");
            for (JsonElement e : typesArray) {
                tupleTypeString.append(e.getAsString().replace("tuple", "")).append(',');
            }

            String tts = completeTupleTypeString(tupleTypeString);
            this.types = TupleType.parse(tts);
            this.values = parseTuple(this.types, valuesArray);
            this.result = FastHex.decode(resultStr, 2, resultStr.length() - 2);
        }

        private boolean test() {
            byte[] encoding = types.encode(values).array();
            try {
                Assert.assertArrayEquals(result, encoding);
                return true;
            } catch (AssertionError ae) {
                String[] resultTokens = format(result).split("[\n]");
                String[] encodingTokens = format(encoding).split("[\n]");
                System.out.println(types.canonicalType);
                int i = 0;
                for ( ; i < resultTokens.length; i++) {
                    String r = resultTokens[i];
                    String e = encodingTokens[i];
                    System.out.println(r + " " + e + " " + (r.equals(e) ? "" : "**************"));
                }
                for ( ; i < encodingTokens.length; i++) {
                    System.out.println("----------------------------------------------------------------" + " " + encodingTokens[i]);
                }
                throw ae;
            }
        }

        private static String format(byte[] abi) {
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            while(idx < abi.length) {
                sb.append(encode(Arrays.copyOfRange(abi, idx, idx + UNIT_LENGTH_BYTES), HEX)).append('\n');
                idx += UNIT_LENGTH_BYTES;
            }
            return sb.toString();
        }

        private static Object parseValue(ABIType<?> type, JsonElement value) {
            switch (type.typeCode()) {
            case ABIType.TYPE_CODE_BOOLEAN: return value.getAsJsonPrimitive().getAsBoolean();
            case ABIType.TYPE_CODE_BYTE: return (byte) value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_INT: return value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_LONG: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueValue = valueObj.get("value").getAsString();
                return Long.parseLong(valueValue);
            }
            case ABIType.TYPE_CODE_BIG_INTEGER: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueValue = valueObj.get("value").getAsString();
                if("address".equals(type.canonicalType)) {
                    BigInteger val = new BigInteger(FastHex.decode(valueValue, 2, valueValue.length() - 2));
                    return Integers.toUnsigned(val, ADDRESS);
                } else {
                    return new BigInteger(valueValue);
                }
            }
            case ABIType.TYPE_CODE_BIG_DECIMAL: {
                String valueValue = value.getAsJsonObject().get("value").getAsString();
                return new BigDecimal(new BigInteger(valueValue), 18);
            }
            case ABIType.TYPE_CODE_ARRAY: return parseArray((ArrayType<?, ?>) type, value);
            case ABIType.TYPE_CODE_TUPLE: return parseTuple((TupleType) type, value.getAsJsonObject().get("value").getAsJsonArray());
            default: throw new Error();
            }
        }

        private static Object parseArray(ArrayType<?, ?> arrayType, JsonElement value) {
            if (arrayType.isString) {
                return value.getAsJsonObject().get("value").getAsString();
            } else if (value.isJsonArray()) {
                JsonArray valueValue = value.getAsJsonArray();
                final int len = valueValue.size();
                final ABIType<?> elementType = arrayType.elementType;
                int i = 0;
                if(Boolean.class == elementType.clazz) {
                    boolean[] array = new boolean[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Boolean) parseValue(elementType, iter.next());
                    }
                    return array;
                } else if(Byte.class == elementType.clazz) {
                    byte[] array = new byte[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Byte) parseValue(elementType, iter.next());
                    }
                    return array;
                } if (Integer.class == elementType.clazz) {
                    int[] array = new int[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Integer) parseValue(elementType, iter.next());
                    }
                    return array;
                } else if (Long.class == elementType.clazz) {
                    long[] array = new long[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Long) parseValue(elementType, iter.next());
                    }
                    return array;
                } else {
                    Object[] array = (Object[]) Array.newInstance(elementType.clazz, len);
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = parseValue(elementType, iter.next());
                    }
                    return array;
                }
            } else {
                JsonObject valueObj = value.getAsJsonObject();
                String valueType = valueObj.get("type").getAsString();
                String valueValue = valueObj.get("value").getAsString();
                if ("buffer".equals(valueType)) {
                    return FastHex.decode(valueValue, 2, valueValue.length() - 2);
                } else {
                    throw new RuntimeException("????");
                }
            }
        }

        private static Tuple parseTuple(TupleType tupleType, JsonArray values) {
            ABIType[] abiTypes = tupleType.elementTypes;
            final int len = abiTypes.length;
            Object[] elements = new Object[len];
            int i = 0;
            for (Iterator<JsonElement> iter = values.iterator(); i < len; i++) {
                elements[i] = parseValue(abiTypes[i], iter.next());
            }
            return new Tuple(elements);
        }

        private static String completeTupleTypeString(StringBuilder canonicalTupleType) {
            final int len = canonicalTupleType.length();
            if(len == 1) {
                return "()";
            }
            return canonicalTupleType.replace(len - 1, len, ")").toString(); // replace trailing comma
        }
    }

    @Test
    public void testMegaJson() throws ParseException {
        for (JsonElement e : TEST_CASES) {
            new TestCase(e.getAsJsonObject()).test();
        }
    }
}
