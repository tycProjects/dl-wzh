package com.filemax.manager;

import android.os.ParcelFileDescriptor;

interface IPrivService {
    void destroy() = 16777114;
    String exec(String cmd) = 1;
    ParcelFileDescriptor openRead(String path) = 2;
    ParcelFileDescriptor openWrite(String path) = 3;
}
