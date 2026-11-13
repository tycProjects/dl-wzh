package com.example.chatzipai;

/**
 * Quy trình cố định cho AI trong ChatZipAI. Đây là luật điều phối, không phải prompt tùy hứng.
 */
final class AiWorkflow {
    private AiWorkflow() { }

    static final String PROTOCOL =
            "QUY TRÌNH BẮT BUỘC: 1) Đọc mục tiêu và cấu trúc project trước khi sửa. "
            + "2) Không tự ý đổi API/public signature, package, tên file hoặc dependency nếu đề bài không yêu cầu. "
            + "3) Mỗi worker chỉ sửa file được giao và phải trả toàn bộ file. "
            + "4) Không bịa file, class, method, dependency hay API. "
            + "5) Sau khi 4 worker xong, AI Trung tâm kiểm tra từng file, dependency liên file và toàn project. "
            + "6) Nếu REJECT, phải nêu lỗi cụ thể theo file; không được coi REJECT là hoàn thành. "
            + "7) Chỉ APPROVED mới được tạo Final ZIP. "
            + "8) Final ZIP phải khớp 100% với snapshot APPROVED theo path + size + SHA-256. "
            + "9) Final Judge phải kiểm tra nội dung source thực tế, không chỉ manifest. "
            + "10) Khi thiếu dữ liệu để kết luận, phải REJECT/ yêu cầu kiểm tra thêm thay vì đoán.";

    static final String PLANNER = PROTOCOL +
            " LẬP KẾ HOẠCH: phân công toàn bộ file editable đúng một worker; nhóm file có quan hệ chặt chẽ; "
            + "không bỏ sót file; ghi rõ contract/API và dependency quan trọng cần giữ.";

    static final String WORKER = PROTOCOL +
            " WORKER: chỉ sửa mục tiêu được giao. Trước khi thay đổi phải hiểu file hiện tại. "
            + "Nếu thay đổi một public API là bắt buộc thì ghi rõ lý do trong kết quả review; nếu không, giữ nguyên.";

    static final String REVIEW = PROTOCOL +
            " REVIEW: đối chiếu BEFORE/AFTER với yêu cầu. Kiểm tra package/import/API, class/method được gọi, "
            + "không làm mất logic ngoài phạm vi, và các thay đổi giữa worker phải tương thích.";

    static final String FINAL = PROTOCOL +
            " FINAL REVIEW: đọc source thật trong Final ZIP đối với các file code/config quan trọng; "
            + "đối chiếu với snapshot APPROVED và phát hiện nội dung không đúng dù hash/manifest có vẻ hợp lệ.";

    static final String REPAIR = PROTOCOL +
            " REPAIR: chỉ sửa các lỗi mà AI Trung tâm đã xác định. Không mở rộng phạm vi sửa. "
            + "Sau repair phải review lại từ đầu; tối đa số vòng được hệ thống quy định.";
}
