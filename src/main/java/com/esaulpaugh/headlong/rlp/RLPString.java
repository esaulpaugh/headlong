package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;

/**
 * Created by Evo on 1/19/2017.
 */
public class RLPString extends RLPItem {

    RLPString(byte lead, DataType type, byte[] buffer, int index, int containerEnd, boolean lenient) throws DecodeException {
        super(lead, type, buffer, index, containerEnd, lenient);
    }

    @Override
    public boolean isList() {
        return false;
    }
}
