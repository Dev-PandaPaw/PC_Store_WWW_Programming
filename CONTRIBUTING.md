# Quy ước cộng tác

## Nhánh và review

- `main` phải luôn ở trạng thái build được sau khi bước bootstrap kỹ thuật bắt đầu.
- Mỗi ticket dùng một feature branch ngắn và một pull request có phạm vi rõ ràng.
- Pull request cần ít nhất một người review và CI pass trước khi merge.
- Migration đã merge không được sửa lại; thay đổi schema phải dùng migration kế tiếp.
- Commit phải tuân theo [quy ước commit](docs/project/COMMIT_CONVENTION.md).

## Ranh giới module

- Module chỉ gọi public service hoặc port công khai của module khác.
- Không truy cập repository, entity hay adapter nội bộ của module khác.
- `sales` điều phối checkout; không đưa nghiệp vụ dùng chung vào một package `common`.
- Tích hợp ngoài đi qua port và adapter, để adapter giả lập và adapter thật dùng cùng contract.
- Chỉ tạo class hoặc interface khi có trách nhiệm thực tế; không tạo lớp rỗng chỉ để lấp cấu trúc.

## Hoàn thành một thay đổi

Một thay đổi chỉ hoàn thành khi đáp ứng acceptance criteria, có validation và phân quyền phù hợp, cập nhật migration/tài liệu liên quan, có test tương xứng, không chứa secret và chạy được trên nhánh chung.

Mẫu chi tiết cho ticket và Definition of Done nằm trong [PLAN.md](docs/PLAN.md).
