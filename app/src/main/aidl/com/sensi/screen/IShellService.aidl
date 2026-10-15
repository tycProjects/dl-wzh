package com.sensi.screen;

interface IShellService {
    // Bắt buộc với Shizuku UserService
    void destroy() = 16777114;

    // Chạy 1 lệnh shell với quyền shell (uid 2000) và trả về kết quả
    String exec(String command) = 1;
}
