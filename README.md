# Bangkok Rain Prediction — Real-time Flood Risk Alerter

ระบบแจ้งเตือนความเสี่ยงน้ำท่วมแบบเรียลไทม์ — แอปพลิเคชัน Java ที่ทำงานผ่าน Command-line ดึงข้อมูลพยากรณ์ฝนสดจากกรมอุตุนิยมวิทยา (TMD)
แล้วประเมินและแจ้งระดับความเสี่ยงเป็น **SAFE**, **WARNING** หรือ **DANGER**

> Mini Project — Object Oriented Software Engineering Laboratory (ทำงาน 2 คน ภายใน 1 วัน)

---

## คุณสมบัติ

- **Java CLI** — รวดเร็ว น้ำหนักเบา รันคำสั่งเดียวจบ
- **ออกแบบด้วย OOP** — แยกชั้นข้อมูล / การประเมิน / การแสดงผล ต่อยอดได้ง่าย
- **Stateless, ไม่มีฐานข้อมูล** — ดึงข้อมูลสดทุกครั้งที่รัน ("เรียลไทม์" = ข้อมูลล่าสุด ณ ตอนรัน)
- **ประเมินจาก 2 ค่า** — ปริมาณฝน (มม.) และโอกาสฝนตก (%)
- **Mock mode** — ใช้ข้อมูลจำลองสำหรับเดโมหรือตอน API มีปัญหา
- **ขยายได้** — เพิ่มแหล่งข้อมูลอื่น (เช่น ThaiWater, BMA) ได้โดย implement `WeatherProvider` ตัวใหม่

---

## ระดับความเสี่ยง

ตรวจจากบนลงล่าง เงื่อนไขแรกที่ตรงคือผลลัพธ์

| ระดับ | เงื่อนไข |
|-------|----------|
| 🔴 **DANGER** | ฝน ≥ 90 มม. **หรือ** (ฝน ≥ 50 มม. **และ** โอกาสฝน ≥ 80%) |
| 🟡 **WARNING** | ฝน ≥ 35 มม. **หรือ** (ฝน ≥ 20 มม. **และ** โอกาสฝน ≥ 70%) |
| 🟢 **SAFE** | นอกเหนือจากนั้น |

เกณฑ์อ้างอิงจากการแบ่งระดับฝนของ TMD (ฝนหนัก ≥ 35 มม., ฝนหนักมาก ≥ 90 มม.) — รายละเอียดใน [docs/CONTRACT.md](docs/CONTRACT.md)

---

## สถาปัตยกรรม

```
 args ──► Main ──► WeatherProvider.fetch(location) ──► WeatherData
                         ▲                                   │
              ┌──────────┴──────────┐                        ▼
       TmdWeatherProvider   MockWeatherProvider        RiskAnalyzer.analyze()
                                                             │
                                                             ▼
                                                        RiskResult ──► AlertPrinter ──► Terminal
```

| Class | หน้าที่ |
|-------|---------|
| `WeatherProvider` | interface สำหรับแหล่งข้อมูลสภาพอากาศ |
| `TmdWeatherProvider` | เรียก TMD API แล้วแปลง JSON เป็น `WeatherData` |
| `MockWeatherProvider` | ข้อมูลจำลองสำหรับเดโม / ทดสอบ |
| `WeatherData` | ข้อมูลสภาพอากาศ: สถานที่, ปริมาณฝน, โอกาสฝนตก |
| `WeatherException` | ข้อผิดพลาดจากการดึงข้อมูล |
| `RiskLevel` | `SAFE`, `WARNING`, `DANGER` |
| `RiskAnalyzer` | ประเมินระดับความเสี่ยงจาก `WeatherData` |
| `RiskResult` | ผลการประเมิน + เหตุผล |
| `AlertPrinter` | แสดงผลแจ้งเตือนแบบอ่านง่าย |
| `Main` | จุดเริ่มโปรแกรม อ่าน arguments |

---

## ความต้องการของระบบ

- Java 17 ขึ้นไป
- Maven 3.8 ขึ้นไป
- TMD API token (ไม่ต้องใช้ถ้ารันแบบ `--mock`) — สมัครได้ที่ https://data.tmd.go.th

---

## การติดตั้งและรัน

```bash
git clone https://github.com/Automatic28m/bangkok_rain_prediction.git
cd bangkok_rain_prediction
mvn clean package
```

### ตั้งค่า API token

โปรแกรมอ่าน token จาก environment variable `TMD_API_TOKEN` เท่านั้น (ไม่อ่านไฟล์ `.env`) — **ห้ามใส่ token ในโค้ดหรือ commit**

**แบบที่ 1 — ใช้เฉพาะ terminal ที่เปิดอยู่** (หายเมื่อปิด terminal)

```bash
# macOS / Linux / Git Bash
export TMD_API_TOKEN="<your-token>"
```

```powershell
# Windows PowerShell
$env:TMD_API_TOKEN = "<your-token>"
```

**แบบที่ 2 — บันทึกถาวร** (ตั้งครั้งเดียว ใช้ได้ทุก terminal ที่เปิดใหม่)

```powershell
# Windows (Command Prompt หรือ PowerShell) — ต้องปิดแล้วเปิด terminal / IDE ใหม่หลังรัน
setx TMD_API_TOKEN "<your-token>"
```

```bash
# macOS / Linux — เพิ่มลงไฟล์ shell profile แล้วเปิด terminal ใหม่
echo 'export TMD_API_TOKEN="<your-token>"' >> ~/.bashrc   # หรือ ~/.zshrc
```

**แบบที่ 3 — ตั้งใน IDE** (รันจากปุ่ม Run ของ IDE)

- **IntelliJ IDEA:** Run → Edit Configurations… → เลือก configuration ของ `Main` → ช่อง **Environment variables** ใส่ `TMD_API_TOKEN=<your-token>`
- **VS Code:** ใน `.vscode/launch.json` เพิ่ม `"env": { "TMD_API_TOKEN": "<your-token>" }` (โฟลเดอร์ `.vscode/` อยู่ใน `.gitignore` แล้ว)

ตรวจว่าตั้งค่าแล้ว (แสดงแค่จำนวนตัวอักษร ไม่แสดง token):

```powershell
# Windows PowerShell
$env:TMD_API_TOKEN.Length
```

```bash
# macOS / Linux / Git Bash
echo ${#TMD_API_TOKEN}
```

ถ้ายังไม่มี token ใช้ `--mock` แทนได้

### รันด้วยคำสั่งเดียว (แนะนำ)

สคริปต์ `run` จะ build โปรเจกต์ (ข้ามเทสต์) แล้วรันโปรแกรมให้ทันที — ไม่ต้องรัน `mvn package` เอง และได้โค้ดล่าสุดทุกครั้ง

```powershell
# Windows (PowerShell หรือ Command Prompt)
.\run Bangkok
.\run "Chiang Mai"
.\run Phuket --mock
```

```bash
# macOS / Linux / Git Bash
./run.sh Bangkok
./run.sh Phuket --mock
```

ข้อความระหว่าง build จะแสดงเฉพาะตอน build ไม่ผ่าน และสคริปต์คืน exit code เดียวกับโปรแกรม (ดูตารางด้านล่าง)

### การใช้งานแบบรัน jar โดยตรง

```
java -jar target/app.jar <location> [--mock]
```

| Argument | ความหมาย |
|----------|----------|
| `<location>` | ชื่อสถานที่ เช่น `Bangkok` (บังคับ) |
| `--mock` | ใช้ข้อมูลจำลองแทน API จริง |

ตัวอย่าง:

```bash
java -jar target/app.jar Bangkok
java -jar target/app.jar Phuket --mock
```

ผลลัพธ์:

```
[DANGER] Phuket
  Rain        : 95.0 mm
  Rain chance : 90 %
  Reason      : Rain 95.0 mm >= 90.0 mm
```

ข้อมูลจำลองใน `--mock` ครอบคลุมทั้ง 3 ระดับ: `Chiangmai` (SAFE), `Bangkok` (WARNING), `Phuket` (DANGER)

| Exit code | ความหมาย |
|-----------|----------|
| `0` | สำเร็จ |
| `1` | arguments ผิดรูปแบบ หรือไม่ได้ตั้ง `TMD_API_TOKEN` |
| `2` | ดึงข้อมูลไม่สำเร็จ (timeout, token ผิด, ไม่พบสถานที่) |

### รันเทสต์

```bash
mvn test
```

---

## โครงสร้างโปรเจกต์

```
bangkok_rain_prediction/
├── docs/
│   ├── SCHEDULE.md      แผนงานและการแบ่งงาน 1 วัน
│   ├── GIT_FLOW.md      ขั้นตอนการใช้ Git ของทีม
│   └── CONTRACT.md      ข้อตกลง interface / API / เกณฑ์ประเมิน
├── src/
│   ├── main/java/floodalert/
│   └── test/java/floodalert/
├── pom.xml
├── run.cmd              build + รันในคำสั่งเดียว (Windows)
├── run.sh               build + รันในคำสั่งเดียว (macOS / Linux)
└── README.md
```

---

## ทีมและการแบ่งงาน

| สมาชิก | ส่วนที่รับผิดชอบ |
|--------|------------------|
| คน A — Data Layer | `TmdWeatherProvider`, `MockWeatherProvider`, `WeatherException` |
| คน B — Logic + CLI | `RiskAnalyzer`, `RiskResult`, `AlertPrinter`, `Main` |

รายละเอียดเพิ่มเติม: [Schedule](docs/SCHEDULE.md) · [Git Flow](docs/GIT_FLOW.md) · [Contract](docs/CONTRACT.md)

---

## สิ่งที่ยังไม่อยู่ในขอบเขต

- แหล่งข้อมูลอื่น (ThaiWater, BMA) — มี interface รองรับไว้แล้ว
- โหมดรันต่อเนื่อง / scheduler
- ไฟล์ config และ logging framework
- ฐานข้อมูลหรือการเก็บประวัติ
