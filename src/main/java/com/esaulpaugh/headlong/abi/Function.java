package com.esaulpaugh.headlong.abi;

import com.joemelsha.crypto.hash.Keccak;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.text.ParseException;
import java.util.List;

/**
 * Represents a function in an Ethereum contract. Can encode function calls matching the function's signature.
 */
public class Function {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];
    public static final int FUNCTION_ID_LEN = 4;

    private final String canonicalSignature;
    private boolean requiredCanonicalization;
    transient final byte[] selector = new byte[FUNCTION_ID_LEN];
    transient final TupleType paramTypes;

    public Function(String signature) throws ParseException {
        StringBuilder canonicalSignature = new StringBuilder();
        List<Type> types = SignatureParser.parseFunctionSignature(signature, canonicalSignature);
        this.canonicalSignature = canonicalSignature.toString();
        this.requiredCanonicalization = !signature.equals(this.canonicalSignature);

        try {
            Keccak keccak256 = new Keccak(256);
            keccak256.update(this.canonicalSignature.getBytes(ASCII));
            keccak256.digest(selector, 0, selector.length);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }

        this.paramTypes = TupleType.create(types.toArray(EMPTY_TYPE_ARRAY));
    }

    public ByteBuffer encodeCall(Object... args) {
        return Encoder.encodeFunctionCall(this, args);
    }

    public String getCanonicalSignature() {
        return canonicalSignature;
    }

    public boolean requiredCanonicalization() {
        return requiredCanonicalization;
    }

    public byte[] getSelector() {
        byte[] out = new byte[selector.length];
        System.arraycopy(selector, 0, out, 0, out.length);
        return out;
    }

    public String getSelectorHex() {
        return Hex.toHexString(selector);
    }

    public static ByteBuffer encodeFunctionCall(String signature, Object... arguments) throws ParseException {
        return Encoder.encodeFunctionCall(new Function(signature), arguments);
    }
}
