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
| `ThaiLocations` | ตารางพิกัด 77 จังหวัด แปลงชื่อสถานที่เป็น lat/lon |
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

## เริ่มต้นใช้งานครั้งแรก (Quick Start)

ทำตามลำดับนี้บนเครื่องใหม่ — คำสั่งฝั่ง Windows ใช้ **PowerShell** (ไม่ใช่ Command Prompt)

### ขั้นที่ 1 — ติดตั้ง Java 17 ขึ้นไป

ตรวจก่อนว่ามีอยู่แล้วหรือยัง:

```bash
java -version
```

ถ้าขึ้น `not recognized` / `command not found` หรือเวอร์ชันต่ำกว่า 17 ให้ติดตั้ง:

| OS | วิธีติดตั้ง |
|----|-------------|
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` หรือดาวน์โหลดตัวติดตั้งจาก https://adoptium.net |
| macOS | `brew install openjdk@21` |
| Ubuntu / Debian | `sudo apt install openjdk-21-jdk` |

### ขั้นที่ 2 — ติดตั้ง Maven 3.8 ขึ้นไป

ตรวจก่อน:

```bash
mvn -v
```

ถ้ายังไม่มี:

| OS | วิธีติดตั้ง |
|----|-------------|
| macOS | `brew install maven` |
| Ubuntu / Debian | `sudo apt install maven` |
| Windows | ทำตามขั้นด้านล่าง |

**Windows:**

1. ดาวน์โหลด **Binary zip archive** (`apache-maven-3.x.x-bin.zip`) จาก https://maven.apache.org/download.cgi
2. แตกไฟล์ไว้ในโฟลเดอร์ที่ไม่มีเว้นวรรค เช่น `C:\tools\apache-maven-3.9.9`
3. เพิ่มโฟลเดอร์ `bin` เข้า PATH ของ user (แก้ path ให้ตรงกับที่แตกไฟล์):

   ```powershell
   [Environment]::SetEnvironmentVariable('Path', [Environment]::GetEnvironmentVariable('Path', 'User') + ';C:\tools\apache-maven-3.9.9\bin', 'User')
   ```

4. **ปิดแล้วเปิด PowerShell ใหม่** แล้วรัน `mvn -v` อีกครั้ง

### ขั้นที่ 3 — ดาวน์โหลดโปรเจกต์

```bash
git clone https://github.com/Automatic28m/bangkok_rain_prediction.git
cd bangkok_rain_prediction
```

### ขั้นที่ 4 — ขอและตั้งค่า TMD API token

1. สมัครสมาชิก / เข้าสู่ระบบที่ https://data.tmd.go.th/nwpapi/
2. คัดลอก API token ของบัญชี — **เฉพาะตัว token ที่ขึ้นต้นด้วย `eyJ`**
3. บันทึก token แบบถาวร ตาม[ตั้งค่า API token](#ตั้งค่า-api-token) แบบที่ 2 (Windows ใช้คำสั่งด้านล่าง **ห้ามใช้ `setx`**):

   ```powershell
   [Environment]::SetEnvironmentVariable('TMD_API_TOKEN', '<your-token>', 'User')
   ```

4. **ปิดแล้วเปิด terminal ใหม่**

ข้ามขั้นนี้ได้ถ้าจะใช้แค่ `--mock`

### ขั้นที่ 5 — ลองรัน

ในโฟลเดอร์โปรเจกต์:

```powershell
# Windows
.\run Bangkok --mock      # ข้อมูลจำลอง — ใช้ได้แม้ยังไม่มี token
.\run Bangkok             # ข้อมูลจริงจาก TMD
```

```bash
# macOS / Linux / Git Bash
./run.sh Bangkok --mock
./run.sh Bangkok
```

ครั้งแรกจะช้ากว่าปกติ (Maven ดาวน์โหลด dependency) ถ้าเห็นผลขึ้นต้นด้วย `[SAFE]`, `[WARNING]` หรือ `[DANGER]` แสดงว่าพร้อมใช้งาน

### ปัญหาที่เจอบ่อย

| อาการ | วิธีแก้ |
|-------|---------|
| `java` / `mvn` is not recognized | ยังไม่ได้ติดตั้ง หรือยังไม่ได้เปิด terminal ใหม่หลังแก้ PATH (ถ้าใช้ terminal ใน VS Code / IntelliJ ให้ปิดโปรแกรมแล้วเปิดใหม่) |
| `TMD_API_TOKEN is not set` | ยังไม่ได้ตั้ง token หรือยังไม่ได้เปิด terminal ใหม่ — หรือใช้ `--mock` |
| `Invalid or missing API token` | token ผิด / ถูกตัด — ตรวจตาม[ตั้งค่า API token](#ตั้งค่า-api-token) (ความยาวต้องมากกว่า 1000 และขึ้นต้นด้วย `eyJ`) |
| `Too many requests to weather service` | TMD จำกัดประมาณ 60 ครั้งต่อนาที — รอประมาณ 1 นาทีแล้วรันใหม่ |
| `The filename, directory name, or volume label syntax is incorrect` | รันคำสั่ง `[Environment]::...` ใน Command Prompt — ให้พิมพ์ `powershell` ก่อนแล้วรันใหม่ |
| `.\run` is not recognized | ไม่ได้อยู่ในโฟลเดอร์โปรเจกต์ — `cd` ไปที่โฟลเดอร์ที่ clone มา |
| `Location not found` | ชื่อสะกดไม่ตรงกับ[สถานที่ที่รองรับ](#สถานที่ที่รองรับ) — ตรวจการสะกด หรือใช้พิกัดแทน เช่น `.\run 13.75,100.50` |
| `Error: build failed` | โค้ดมี compile error — แก้ตามข้อความ `[ERROR]` ที่แสดง |

---

## การติดตั้งและรัน

```bash
git clone https://github.com/Automatic28m/bangkok_rain_prediction.git
cd bangkok_rain_prediction
mvn clean package
```

### ตั้งค่า API token

โปรแกรมอ่าน token จาก environment variable `TMD_API_TOKEN` เท่านั้น (ไม่อ่านไฟล์ `.env`) — **ห้ามใส่ token ในโค้ดหรือ commit**

> token ของ TMD ขึ้นต้นด้วย `eyJ` และยาวกว่า 1,000 ตัวอักษร — คัดลอก**เฉพาะตัว token** อย่าให้มีข้อความอื่นติดมา เช่น ชื่อ "Thai Meteorological Department (TMD)" ที่หน้าเว็บแสดงไว้ด้านหน้า

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
# Windows PowerShell (ไม่ใช่ Command Prompt) — ต้องปิดแล้วเปิด terminal / IDE ใหม่หลังรัน
[Environment]::SetEnvironmentVariable('TMD_API_TOKEN', '<your-token>', 'User')
```

> **ห้ามใช้ `setx`** — `setx` เก็บค่าได้ไม่เกิน 1,024 ตัวอักษร token ของ TMD ยาวกว่านั้นจึงถูกตัดท้ายทิ้ง แล้ว API จะตอบ `Invalid or missing API token`

```bash
# macOS / Linux — เพิ่มลงไฟล์ shell profile แล้วเปิด terminal ใหม่
echo 'export TMD_API_TOKEN="<your-token>"' >> ~/.bashrc   # หรือ ~/.zshrc
```

**แบบที่ 3 — ตั้งใน IDE** (รันจากปุ่ม Run ของ IDE)

- **IntelliJ IDEA:** Run → Edit Configurations… → เลือก configuration ของ `Main` → ช่อง **Environment variables** ใส่ `TMD_API_TOKEN=<your-token>`
- **VS Code:** ใน `.vscode/launch.json` เพิ่ม `"env": { "TMD_API_TOKEN": "<your-token>" }` (โฟลเดอร์ `.vscode/` อยู่ใน `.gitignore` แล้ว)

ตรวจว่าตั้งค่าแล้ว (แสดงแค่จำนวนตัวอักษรและ 3 ตัวแรก ไม่แสดง token ทั้งหมด):

```powershell
# Windows PowerShell — ควรได้ตัวเลขมากกว่า 1000 และ eyJ
$env:TMD_API_TOKEN.Length
$env:TMD_API_TOKEN.Substring(0, 3)
```

```bash
# macOS / Linux / Git Bash — ควรได้ตัวเลขมากกว่า 1000 และ eyJ
echo ${#TMD_API_TOKEN}
echo "${TMD_API_TOKEN:0:3}"
```

| อาการ | สาเหตุ |
|-------|--------|
| ความยาวเป็น `0` / ขึ้น error ว่า null | ยังไม่ได้ตั้ง หรือยังไม่ได้เปิด terminal ใหม่หลังตั้งแบบที่ 2 |
| ความยาวเป็น `1024` พอดี | ใช้ `setx` → token ถูกตัด ให้ตั้งใหม่ด้วยคำสั่งในแบบที่ 2 |
| 3 ตัวแรกไม่ใช่ `eyJ` | มีข้อความอื่นติดมาด้านหน้า token |

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

### สถานที่ที่รองรับ

ใช้กับข้อมูลจริง (ไม่ใช่ `--mock`) ระบุได้ 2 แบบ:

**1. ชื่อจังหวัดภาษาอังกฤษ — ครบ 77 จังหวัด** ไม่สนตัวพิมพ์ เว้นวรรค `-` หรือ `_` (เช่น `"Chiang Mai"`, `chiang-mai`, `CHIANGMAI` ได้ผลเหมือนกัน) ชื่อที่มีเว้นวรรคต้องใส่ `"..."`

| ภาค | จังหวัด |
|-----|---------|
| กลาง | Bangkok, Kamphaeng Phet, Chai Nat, Nakhon Nayok, Nakhon Pathom, Nakhon Sawan, Nonthaburi, Pathum Thani, Phra Nakhon Si Ayutthaya, Phichit, Phitsanulok, Lop Buri, Samut Prakan, Samut Songkhram, Samut Sakhon, Saraburi, Sing Buri, Suphan Buri, Sukhothai, Uthai Thani, Ang Thong, Phetchabun |
| เหนือ | Nan, Phayao, Lampang, Lamphun, Uttaradit, Chiang Rai, Chiang Mai, Phrae, Mae Hong Son |
| ตะวันออกเฉียงเหนือ | Kalasin, Khon Kaen, Chaiyaphum, Nakhon Phanom, Nakhon Ratchasima, Bueng Kan, Buri Ram, Maha Sarakham, Mukdahan, Yasothon, Roi Et, Si Sa Ket, Sakon Nakhon, Surin, Nong Khai, Nong Bua Lam Phu, Amnat Charoen, Udon Thani, Ubon Ratchathani, Loei |
| ตะวันออก | Chanthaburi, Chachoengsao, Chon Buri, Trat, Prachin Buri, Rayong, Sa Kaeo |
| ใต้ | Krabi, Chumphon, Trang, Nakhon Si Thammarat, Narathiwat, Pattani, Phang Nga, Phatthalung, Phuket, Yala, Ranong, Songkhla, Satun, Surat Thani |
| ตะวันตก | Kanchanaburi, Tak, Prachuap Khiri Khan, Ratchaburi, Phetchaburi |

ชื่อเพิ่มเติม: `Ayutthaya`, `Korat` (นครราชสีมา) และเมืองที่ไม่ใช่จังหวัด `Pattaya`, `Hat Yai` — การสะกดติดกันก็ใช้ได้ เช่น `Sisaket`, `Chonburi`, `Lopburi`
การจัดภาคและพิกัดของแต่ละจังหวัดใช้ตาม TMD (`forecast/location/daily/region`)

**2. พิกัด `lat,lon`** — ใช้ได้ทุกจุดในประเทศไทย (lat 5.61–20.47, lon 97.34–105.64) เช่น `.\run 13.75,100.50`

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
