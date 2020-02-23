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
package com.esaulpaugh.headlong.abi.util;

import org.bouncycastle.jcajce.provider.digest.Keccak.DigestKeccak;

import java.security.MessageDigest;

/** A wrapper class for the bouncycastle implementation of Keccak. */
public final class WrappedKeccak extends MessageDigest {

    private final DigestKeccak impl;

    public WrappedKeccak(int digestSizeBits) {
        super("Keccak-" + digestSizeBits);
        impl = new DigestKeccak(digestSizeBits);
    }

    @Override
    protected void engineUpdate(byte input) {
        impl.update(input);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        impl.update(input, offset, len);
    }

    @Override
    protected byte[] engineDigest() {
        return impl.digest();
    }

    @Override
    protected void engineReset() {
        impl.reset();
    }

    @Override
    public int engineDigest(byte[] buf, int offset, int len) {
        byte[] digest = engineDigest();
        System.arraycopy(digest, 0, buf, offset, len);
        return len;
    }

    @Override
    protected int engineGetDigestLength() {
        return impl.getDigestLength();
    }
}
