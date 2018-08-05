package com.esaulpaugh.headlong.rlp.codec;

/**
 * Created by Evo on 1/19/2017.
 */
public class RLPString extends RLPItem {

    RLPString(byte[] buffer, int index, int containerLimit) {
        super(buffer, index, containerLimit);
    }

    @Override
    public boolean isList() {
        return false;
    }
}
