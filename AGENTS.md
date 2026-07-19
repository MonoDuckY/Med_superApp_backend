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
