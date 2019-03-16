package com.esaulpaugh.headlong.abi;

public interface ABIObject {

    int FUNCTION = 1;
    int EVENT = 2;

    int objectType();
}
