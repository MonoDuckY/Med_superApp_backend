# ADSUS AI Backend

Backend FastAPI hỗ trợ phát hiện tổn thương trên ảnh bằng model YOLO lưu trên Hugging Face. API nhận ảnh, chạy model AI và trả về bounding box cùng gợi ý tọa độ caliper.

## Requirements

- Python 3.10 trở lên
- Kết nối Internet để tải model từ Hugging Face lần đầu
- Windows PowerShell, Linux hoặc macOS

## Project structure

```text
ADSUS_AIBE/
├── backend/
│   ├── main.py
│   ├── geometry.py
│   ├── requirements.txt
│   └── .env
└── README.md
```

## Model configuration

Backend sử dụng model mặc định:

```text
Repository: tranqui247/adsus
Filename: YOLO26_EffNetV2S_512_40H1_nbl.pt
```

Hai giá trị này đã được khai báo mặc định trong `backend/main.py`. Khi gọi `/api/detect`, có thể bỏ trống `repo_id` và `filename`.

## Environment variables

Tạo file `backend/.env`:

```env
HUGGINGFACE_TOKEN=your-huggingface-token
WEBHOOK_TOKEN=your-internal-webhook-token
CONF_THRESHOLD=0.15
IOU_THRESHOLD=0.1
```

`HUGGINGFACE_TOKEN` dùng để tải model từ Hugging Face. `WEBHOOK_TOKEN` dùng cho endpoint `/api/reload-model`.

Không commit `.env` lên GitHub.

## Windows installation

Mở PowerShell tại thư mục project:

```powershell
cd "D:\doAn2026\khoanh\ADSUS_AIBE\backend"
```

Tạo virtual environment:

```powershell
python -m venv .venv
```

Nếu PowerShell chặn việc chạy script:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

Kích hoạt virtual environment:

```powershell
.\.venv\Scripts\Activate.ps1
```

Cài dependency:

```powershell
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

## Run locally

Từ thư mục `backend`, chạy:

```powershell
python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Chạy development mode với tự động reload:

```powershell
python -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

Backend chạy tại:

```text
http://127.0.0.1:8000
```

Swagger UI:

```text
http://127.0.0.1:8000/docs
```

## Health check

PowerShell:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/health
```

Response mẫu:

```json
{
  "status": "ok",
  "model_loaded": false
}
```

`model_loaded` sẽ chuyển thành `true` sau request detect đầu tiên.

## Detect an image

Endpoint:

```http
POST /api/detect
```

PowerShell:

```powershell
curl.exe -X POST "http://127.0.0.1:8000/api/detect" `
  -F "file=@C:\path\to\image.jpg;type=image/jpeg"
```

Các field `repo_id` và `filename` là tùy chọn. Nếu không gửi, backend dùng model mặc định.

Response mẫu:

```json
{
  "session_id": "uuid",
  "image_width": 1028,
  "image_height": 796,
  "detections": [
    {
      "confidence": 0.91,
      "class_id": 0,
      "bbox": {
        "xmin": 0.2,
        "ymin": 0.3,
        "xmax": 0.6,
        "ymax": 0.7
      },
      "suggested_calipers": {
        "pair_a": [[100, 200], [300, 200]],
        "pair_b": [[200, 100], [200, 300]]
      }
    }
  ]
}
```

## Reload model

Endpoint này cho phép đổi model đang chạy:

```http
POST /api/reload-model
```

Header:

```http
Authorization: Bearer <WEBHOOK_TOKEN>
Content-Type: application/json
```

PowerShell:

```powershell
$token = "your-webhook-token"
$headers = @{
  Authorization = "Bearer $token"
  "Content-Type" = "application/json"
}
$body = @{
  repo_id = "tranqui247/adsus"
  filename = "YOLO26_EffNetV2S_512_40H1_nbl.pt"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://127.0.0.1:8000/api/reload-model" `
  -Method Post `
  -Headers $headers `
  -Body $body
```

## Integration with Spring Boot backend

Khi chạy cùng EC2, Spring Boot gọi AI backend bằng địa chỉ nội bộ:

```text
http://127.0.0.1:8000/api/detect
```

Frontend không gọi trực tiếp AI backend. Frontend gọi Spring Boot endpoint:

```http
POST /api/researcher/detect
```

Spring Boot kiểm tra role `RESEARCHER`, chuyển ảnh sang AI backend và trả kết quả về frontend.

## Common issues

### `No module named uvicorn`

Virtual environment chưa được kích hoạt hoặc dependency chưa được cài:

```powershell
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

### Model tải chậm

Request đầu tiên cần tải model từ Hugging Face. Các request sau sẽ dùng model đã được giữ trong bộ nhớ.

### `401` khi gọi `/api/reload-model`

Kiểm tra header có đúng format không:

```text
Authorization: Bearer <WEBHOOK_TOKEN>
```

### Thiếu RAM

Model AI có thể sử dụng nhiều RAM. Không nên chạy nhiều worker Uvicorn trên máy có bộ nhớ thấp:

```powershell
python -m uvicorn main:app --host 127.0.0.1 --port 8000 --workers 1
```

## Security notes

- Không commit `backend/.env`.
- Không public port `8000` nếu AI backend chỉ được gọi bởi Spring Boot.
- Chỉ expose endpoint `/api/researcher/detect` của Spring Boot cho frontend.
- Giới hạn kích thước và loại file ảnh khi triển khai production.
