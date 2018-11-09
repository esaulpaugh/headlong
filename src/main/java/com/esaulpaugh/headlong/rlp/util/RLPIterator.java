package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPItem;

public interface RLPIterator {

    boolean hasNext();

    RLPItem next() throws DecodeException;

}
