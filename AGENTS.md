# THÔNG TIN DỰ ÁN (Dùng chung cho cả 3 Repos)
- **Dự án:** Ứng dụng chẩn đoán hình ảnh y tế.
- **Nền tảng Client:** Web & Mobile.
- **Quy mô nhóm:** 5 thành viên.
- **Quy tắc cơ sở dữ liệu:** KHÔNG lưu trực tiếp file ảnh vào Database (chỉ lưu URL/Metadata). Giới hạn Document 16MB.

---

# QUY CHUẨN GIAO TIẾP API (BẮT BUỘC)
Mọi API trả về từ Spring Boot và nhận tại Flutter/Python đều phải tuân thủ nghiêm ngặt định dạng JSON bọc (Wrapper) sau:
{
  "success": boolean,
  "message": string,
  "data": object | array | null,
  "errorCode": string | null
}

---

# QUY CHUẨN KỸ THUẬT RIÊNG (Tùy biến theo từng Repo)

## Nếu đây là Frontend Repo (Flutter):
- **Kiến trúc:** MVVM (Model - View - ViewModel). Tách biệt hoàn toàn giao diện và logic.
- **State Management:** Sử dụng `Provider`.
- **Network Client:** Sử dụng `dio` thay cho `http`.
- **Cấu trúc thư mục:** `lib/core`, `lib/models`, `lib/services`, `lib/viewmodels`, `lib/views`.

## Nếu đây là Backend Repo (Java Spring Boot):
- **Công cụ Build:** Gradle (Groovy DSL).
- **Kiến trúc:** 3-Tier Architecture (Controllers - Services - Repositories).
- **Ngôn ngữ:** Java 17 hoặc 21. 
- **Cấu trúc thư mục:** Nhóm theo tính năng (Package by Feature) hoặc theo lớp (Package by Layer).
- **Lưu ý:** Bắt buộc sử dụng Generic `ApiResponse<T>` cho mọi Controller.

## Nếu đây là AI Service Repo (Python):
- **Framework:** FastAPI.
- **Môi trường:** Bắt buộc sử dụng môi trường ảo (`venv`). Không push thư mục `venv` lên Git.
- **Xử lý Request:** Nhận file ảnh qua `UploadFile`, trả kết quả nhanh chóng, không thực hiện các tác vụ block luồng chính.
- **Lưu ý trọng số AI:** Các file `.h5`, `.pt`, `.onnx` phải được khai báo trong `.gitignore`.

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes_tool` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context_tool` | Need source snippets for review — token-efficient |
| `get_impact_radius_tool` | Understanding blast radius of a change |
| `get_affected_flows_tool` | Finding which execution paths are impacted |
| `query_graph_tool` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool` | Finding functions/classes by name or keyword |
| `get_architecture_overview_tool` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.

---

# QUY CHUẨN VIẾT TEST (BACKEND)

## Nguyên tắc bắt buộc
1. **Mọi test class mới PHẢI có Test Design trước** — thêm sheet tương ứng vào file Excel trong thư mục `test design/` trước khi viết code.
2. **Tên phương thức test** phải khớp với cột `Notes` (tên method) trong Excel (ví dụ: `login_issuesAccessAndRefreshTokensForActiveAccountWithCorrectPassword()`).
3. **Test ID** phải được ghi vào comment của test method để đảm bảo traceability (ví dụ: `// TC-UNIT-AuthServiceImpl-001`).
4. **File Excel design** nằm tại: `test design/Report 5.1_UnitTests_L1_BE.xlsx` (Unit) và `test design/Report 5.2_IntegrationTests_L2_BE.xlsx` (Integration).

## Cấu trúc thư mục test
```
src/test/java/com/yourproject/backend/
├── config/          # Unit test cho @Configuration classes
├── integration/     # Integration test (yêu cầu MongoDB container)
├── seeder/          # Test cho data seeder
├── services/        # Unit test cho service layer
│   └── impl/
└── utils/           # Unit test cho utility classes
```
Tất cả integration test phải **extends `MongoIntegrationTestBase`**.

## Quy chuẩn định dạng file Excel (BẮT BUỘC)

Khi tạo sheet mới trong file Excel test design, **bắt buộc phải thêm Data Validation (dropdown list)** vào các cột sau. Giá trị phải khớp chính xác (có thể dùng script Python + openpyxl):

### File L1 — `Report 5.1_UnitTests_L1_BE.xlsx`
| Cột | Tên cột | Giá trị dropdown hợp lệ |
|---|---|---|
| **E** | Test Type | `Functional`, `Boundary & Negative`, `Input Validation`, `Security (GBR)` |
| **F** | Coverage Technique | `Equivalence Partitioning (EP)`, `Boundary Value Analysis (BVA)`, `Decision Table Testing`, `State Transition Testing`, `Use Case Testing`, `Error Guessing`, `Branch / Condition Coverage` |
| **K** | Priority | `Critical`, `High`, `Medium`, `Low` |
| **L** | Status | `Not Run`, `Pass`, `Fail`, `Blocked`, `Skip` |

### File L2 — `Report 5.2_IntegrationTests_L2_BE.xlsx`
| Cột | Tên cột | Giá trị dropdown hợp lệ |
|---|---|---|
| **E** | Test Type | `Integration/API`, `UI Testing`, `Input Validation`, `Security`, `Functional` |
| **F** | Coverage Technique | *(same as L1)* |
| **H** | Validation Direction | `N/A`, `Client-visible (UI)`, `Server-side (bypass UI)`, `Both` |
| **L** | Priority | `Critical`, `High`, `Medium`, `Low` |
| **M** | Status | `Not Run`, `Pass`, `Fail`, `Blocked`, `Skip` |

> **Lưu ý:** Cột `Validation Direction` chỉ điền giá trị khác `N/A` khi `Test Type = Input Validation`.

---

## Trạng thái Coverage hiện tại (cập nhật: 2026-08-08)

### ✅ Đã có cả Test Design VÀ Test Code
| Test Class | File Excel (Sheet) | # TCs |
|---|---|---|
| `AuthServiceImplTest` | 5.1 – ` AuthServiceImpl` | 10 |
| `UserServiceImplTest` | 5.1 – `UserServiceImpl` | 26 |
| `PhoneNumberNormalizerTest` | 5.1 – `PhoneNumberNormalizer` | 4 |
| `PasswordPolicyTest` | 5.1 – `PasswordPolicy` | 7 |
| `SecurityConfigTest` | 5.1 – `SecurityConfig` | 2 |
| `PatientDataProtectionServiceTest` | 5.1 – `PatientDataProtectionService` | 8 |
| `AuthLoginIntegrationTest` | 5.2 – `AuthController` | 10+ |
| `PatientOtpIntegrationTest` | 5.2 – `AuthController` | 20+ |
| `AuthTokenLifecycleIntegrationTest` | 5.2 – `AuthController` | 12+ |
| `AuthPasswordChangeIntegrationTest` | 5.2 – `AuthController` | 10+ |
| `AuthJwtSecurityIntegrationTest` | 5.2 – `AuthSecurity` | 10+ |
| `UserProfileIntegrationTest` | 5.2 – `AuthController` | 3 |
| `AppointmentServiceImplTest` | 5.1 – `AppointmentServiceImpl` | 18 |
| `DoctorDirectoryServiceImplTest` | 5.1 – `DoctorDirectoryServiceImpl` | 1 |
| `StaffPatientSearchServiceImplTest` | 5.1 – `StaffPatientSearchServiceImpl` | 5 |
| `WorkScheduleServiceImplTest` | 5.1 – `WorkScheduleServiceImpl` | 14 |
| `MedicineScheduleStatusJobTest` | 5.1 – `MedicineScheduleStatusJob` | 2 |
| `WorkSlotBootstrapperTest` | 5.1 – `WorkSlotBootstrapper` | 1 |
| `DataSeederTest` | 5.1 – `DataSeeder` | 1 |
| `AuthForgotPasswordIntegrationTest` | 5.2 – `AuthForgotPassword` | 3 |
| `ClinicalMedicationIntegrationTest` | 5.2 – `ClinicalMedication` | 2 |
| `ManageAccountSystemTest` | 5.2 – `ManageAccount` | 6 |
| `SchedulingIntegrationTest` | 5.2 – `Scheduling` | 4 |
| `StaffPatientSearchIntegrationTest` | 5.2 – `StaffPatientSearch` | 2 |

### ⚠️ Có Test Code nhưng CHƯA CÓ trong Test Design
> ✅ **Không còn class nào trong trạng thái này** — tất cả đã được bổ sung design vào Excel ngày 2026-08-08.

### 🔴 Chưa có cả Test Design lẫn Test Code (ưu tiên viết tiếp)
**P0 – Core feature, viết ngay:**
- `ClinicalMedicationServiceImpl`, `DiagnosticServiceImpl`, `SchedulingCatalogServiceImpl`
- `DiagnosticController`, `PatientAppointmentController`

**P1 – Quan trọng về bảo mật:**
- `JwtUtils`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`

**P2 – Các Controllers còn lại:**
- `AdminUserController`, `AdminSmsGatewayController`, `DoctorExaminationController`
- `DoctorWorkScheduleController`, `PatientDoctorController`, `PatientMedicineScheduleController`
- `StaffDoctorController`, `StaffPatientController`, `StaffSchedulingController`, `SmsGatewayController`

**P3 – Utils & Config:**
- `WorkSlotTimeUtils`, `GlobalExceptionHandler`
- Các `*Migration.java` (đánh giá lại sự cần thiết)