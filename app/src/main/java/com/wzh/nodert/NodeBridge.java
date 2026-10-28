package com.wzh.nodert;

public final class NodeBridge {
    private NodeBridge() {}

    // Blocks for the lifetime of the Node process; implemented in wzh-node.cpp.
    public static native int start(String[] args);
}
