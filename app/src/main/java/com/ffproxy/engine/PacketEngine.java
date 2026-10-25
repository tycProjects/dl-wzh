package com.ffproxy.engine;

public class PacketEngine {
    public static byte[] process(byte[] data, int len) {
        if (len < 28) return data;
        if (((data[0] >> 4) & 0xF) != 4) return data;
        if ((data[9] & 0xFF) != 17) return data; // UDP
        if (FeatureConfig.antiban) return data;
        return data;
    }
}
