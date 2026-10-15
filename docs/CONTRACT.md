# Contract — Real-time Flood Risk Alerter

ข้อตกลงกลางระหว่าง **คน A (Data Layer)** และ **คน B (Logic + CLI)** ว่าแต่ละฝ่ายต้องส่งอะไรให้กัน
เพื่อให้ทำงานขนานกันได้โดยไม่ต้องรอ — ทุกอย่างผูกกันที่ interface `WeatherProvider` ตัวเดียว

> **ห้ามเปลี่ยน signature ในเอกสารนี้เองโดยไม่แจ้งอีกฝ่าย** ถ้าต้องแก้ ให้แก้เอกสารนี้ผ่าน PR และให้อีกคน approve ก่อน
> เอกสารอื่น: [SCHEDULE.md](SCHEDULE.md) · [GIT_FLOW.md](GIT_FLOW.md)

---

## 1. ขอบเขตที่ตกลงกัน (ลดทอนแล้วให้ทำเสร็จภายใน 1 วัน)

| ไม่ทำ | ทำแทน |
|-------|-------|
| provider ตัวที่ 2 (ThaiWater / BMA) | ทำแค่ `interface WeatherProvider` ให้ต่อยอดได้ — "ขยายได้" แต่ยังไม่ implement |
| ไฟล์ config, logging framework | อ่าน token จาก environment variable `TMD_API_TOKEN` |
| unit test ครบทุก class | เขียน test เฉพาะ `RiskAnalyzer` |
| loop / scheduler | "เรียลไทม์" = ดึงข้อมูลสดทุกครั้งที่รัน แล้วจบ |
| ประเมินจากหลายค่า | ใช้แค่ 2 ค่า: **ปริมาณฝน (มม.)** และ **โอกาสฝนตก (%)** |

**มี `MockWeatherProvider`** ไว้ใช้ตอนเดโมและตอนที่ API มีปัญหา ช่วยให้ทั้งสองคนทำงานขนานกันได้

ระบบเป็น Stateless และไม่มีฐานข้อมูล

---

## 2. ภาพรวมสถาปัตยกรรม (OOP)

```
 args ──► Main ──► WeatherProvider.fetch(location) ──► WeatherData
                         ▲                                   │
              ┌──────────┴──────────┐                        ▼
       TmdWeatherProvider   MockWeatherProvider        RiskAnalyzer.analyze()
          (คน A)                (คน A)                       │
                                                             ▼
                                                        RiskResult ──► AlertPrinter ──► Terminal
                                                          (คน B)           (คน B)
```

| หลักการ | จุดที่ใช้ |
|---------|----------|
| **Abstraction / Polymorphism** | `WeatherProvider` — สลับ Tmd ↔ Mock (และ provider อื่นในอนาคต) โดยไม่แก้ส่วนอื่น |
| **Encapsulation** | `WeatherData`, `RiskResult` เป็นข้อมูลที่สร้างแล้วไม่เปลี่ยน (private final + getter, validate ใน constructor) |
| **Single Responsibility** | ดึงข้อมูล (A) / ประเมิน (`RiskAnalyzer`) / แสดงผล (`AlertPrinter`) แยก class กัน |
| **Dependency Injection** | `Main` เลือก provider แล้วส่งให้ส่วนที่ใช้ ไม่ `new` ภายในกันเอง |

---

## 3. Java Contract

Java 17, Maven, JUnit 5 — package เดียว `floodalert` (ไฟล์ทั้งหมดอยู่ใน `src/main/java/floodalert/`)

### 3.1 ส่วนกลาง (ตกลงกัน — คน A เป็นคน commit ก่อนภายใน 15 นาทีแรก)

```java
public enum RiskLevel { SAFE, WARNING, DANGER }

public class WeatherData {
    public WeatherData(String location, double rainMm, int rainChancePercent);
    public String getLocation();
    public double getRainMm();            // ปริมาณฝนคาดการณ์ (มม.), >= 0
    public int    getRainChancePercent(); // โอกาสฝนตก, 0..100
}

public interface WeatherProvider {
    WeatherData fetch(String location) throws WeatherException;
}

public class WeatherException extends Exception {
    public WeatherException(String message);
    public WeatherException(String message, Throwable cause);
}
```

- `WeatherData` ค่านอกช่วงให้ throw `IllegalArgumentException` ตั้งแต่ constructor
- `fetch` **ต้องไม่คืน `null`** — ถ้าได้ข้อมูลไม่ครบ ให้ throw `WeatherException`

### 3.2 คน A — Data Layer

| Class | หน้าที่ |
|-------|---------|
| `MockWeatherProvider` | คืนข้อมูลจำลองที่กำหนดไว้ ใช้ในเดโมและเมื่อ API ล่ม — **ทำเป็นอย่างแรก (15 นาที)** |
| `TmdWeatherProvider` | เรียก TMD API, แปลง JSON เป็น `WeatherData` |
| `WeatherException` | exception ของชั้นข้อมูล |

```java
public MockWeatherProvider();                                   // ใช้ชุดข้อมูลจำลองมาตรฐาน
public MockWeatherProvider(Map<String, WeatherData> data);      // กำหนดเองสำหรับ test
public TmdWeatherProvider(String apiToken);
```

**ข้อมูลจำลองมาตรฐานของ `MockWeatherProvider`** (B ใช้ทดสอบ CLI ได้ทั้ง 3 ระดับ โดยไม่ต้องรอ API):

| location (ไม่สนตัวพิมพ์) | rainMm | rainChancePercent | ผลที่คาดไว้ |
|--------------------------|--------|-------------------|-------------|
| `Bangkok` | 42.0 | 80 | WARNING |
| `Chiangmai` | 5.0 | 20 | SAFE |
| `Phuket` | 95.0 | 90 | DANGER |
| อื่นๆ | — | — | throw `WeatherException("Location not found: ...")` |

**ข้อผิดพลาดที่ `TmdWeatherProvider` ต้องแปลงเป็น `WeatherException`** (ช่วงบ่าย)

| กรณี | ข้อความ |
|------|---------|
| timeout / เครือข่ายล่ม | `Cannot reach weather service (timeout)` |
| token ผิด / หมดอายุ (401/403) | `Invalid or missing API token` |
| ไม่พบสถานที่ (ไม่อยู่ในตาราง หรือพิกัดนอกประเทศไทย → HTTP 422) | `Location not found: <location>` |
| response ผิดรูป / ขาด field | `Unexpected response from weather service` |

#### TMD API ที่ใช้ (ทดสอบกับ API จริงแล้ว 2026-10-15)

| รายการ | ค่า |
|--------|-----|
| Endpoint | `GET https://data.tmd.go.th/nwpapi/v1/forecast/location/hourly/at` |
| Auth | Header `authorization: Bearer <TMD_API_TOKEN>` |
| Query | `lat`, `lon`, `fields=rain`, `date=<วันนี้>`, `hour=<ชั่วโมงปัจจุบัน>`, `duration=24` (เวลาไทย) |

**ผลการทดสอบที่ทำให้เลือกแบบนี้**

- endpoint แบบชื่อสถานที่ (`/place?province=...`) ตอบ **404 ทุกครั้ง** ทั้งชื่อไทยและอังกฤษ → ใช้ **lat/lon (`/at`)** แทน
- TMD **ไม่มี field "โอกาสฝนตก %"** (ทั้งในเอกสารและ response จริง) มีแค่ `rain` (มม.) และ `cond` (รหัสสภาพอากาศ)
- ช่วงพิกัดที่รับ: lat 5.61–20.47, lon 97.34–105.64 (นอกช่วงตอบ 422)

**วิธีคำนวณค่าใน `WeatherData`** (จากข้อมูลรายชั่วโมงล่วงหน้า 24 ชั่วโมง)

| field | วิธีคำนวณ |
|-------|-----------|
| `rainMm` | ผลรวม `rain` ของทุกชั่วโมง (ปัดเป็นทศนิยม 1 ตำแหน่ง) |
| `rainChancePercent` | **สัดส่วนชั่วโมงที่มีฝน** = จำนวนชั่วโมงที่ `rain >= 0.1` มม. × 100 ÷ จำนวนชั่วโมงทั้งหมด (ปัดเป็นจำนวนเต็ม) |

ตัวอย่างจริง กรุงเทพฯ 2026-10-15: 22 ชั่วโมง มีฝน 2 ชั่วโมง (3.4 + 13.3 มม.) → `rainMm = 16.7`, `rainChancePercent = 9`

**การแปลงชื่อสถานที่เป็นพิกัด:** `TmdWeatherProvider` ใช้ `ThaiLocations` (ตารางชื่อจังหวัดภาษาอังกฤษครบ 77 จังหวัด → lat/lon ตามพิกัดอ้างอิงของ TMD) (ไม่สนตัวพิมพ์/ช่องว่าง เช่น `Bangkok`, `Chiang Mai`, `chiangmai`)
และรับรูปแบบ `"<lat>,<lon>"` ได้โดยตรง เช่น `13.75,100.50` ชื่อที่ไม่อยู่ในตารางจะได้ `Location not found`

ผลลัพธ์ต่อ B ยังเป็น `WeatherData` เหมือนเดิม — **signature ไม่เปลี่ยน**

### 3.3 คน B — Logic + CLI

| Class | หน้าที่ |
|-------|---------|
| `RiskAnalyzer` | รับ `WeatherData` คืน `RiskResult` (ไม่มี I/O, ไม่แตะ network) |
| `RiskResult` | ผลประเมิน: ข้อมูลต้นทาง + `RiskLevel` + เหตุผล |
| `AlertPrinter` | พิมพ์ผลให้อ่านง่าย (สี/ไอคอนตามระดับ) |
| `Main` | อ่าน args, เลือก provider, เรียกตามลำดับ, จัดการ error |

```java
public class RiskResult {
    public RiskResult(WeatherData data, RiskLevel level, String reason);
    public WeatherData getData();
    public RiskLevel   getLevel();
    public String      getReason();   // เช่น "Rain 42.0 mm >= 35.0 mm"
}

public class RiskAnalyzer {
    public RiskResult analyze(WeatherData data);
}

public class AlertPrinter {
    public AlertPrinter(PrintStream out);     // รับ stream เพื่อให้ test จับ output ได้
    public void print(RiskResult result);
}
```

**เกณฑ์ประเมิน (ข้อเสนอเริ่มต้น — ทั้งคู่ยืนยันที่ช่องท้ายเอกสาร)**

ตรวจจากบนลงล่าง ข้อแรกที่ตรงเป็นผล

| ลำดับ | เงื่อนไข | ระดับ |
|-------|----------|-------|
| 1 | `rainMm >= 90` **หรือ** (`rainMm >= 50` **และ** `rainChance >= 80`) | `DANGER` |
| 2 | `rainMm >= 35` **หรือ** (`rainMm >= 20` **และ** `rainChance >= 70`) | `WARNING` |
| 3 | นอกเหนือจากนั้น | `SAFE` |

ค่า threshold ให้เป็นค่าคงที่ (`static final`) ใน `RiskAnalyzer` เพื่อแก้จุดเดียวได้

> **B ช่วยพิจารณา:** `rainChancePercent` จาก TMD คือ "สัดส่วนชั่วโมงที่มีฝนใน 24 ชั่วโมง" (ดูหัวข้อ 3.2) ไม่ใช่ความน่าจะเป็นแบบพยากรณ์ทั่วไป
> ฝนในไทยมักตกเป็นช่วงสั้นๆ ค่านี้จึงมักต่ำ (เช่น ฝนหนัก 2–3 ชั่วโมง = 8–13%) เงื่อนไข `rainChance >= 70` / `>= 80` อาจแทบไม่เกิดกับข้อมูลจริง
> ถ้าจะปรับเกณฑ์ ให้แก้ตารางนี้ใน PR เดียวกับ `RiskAnalyzer`

**รูปแบบ output** (ป้ายระดับอยู่ต้นบรรทัดแรกเสมอ)

```
[DANGER] Phuket
  Rain        : 95.0 mm
  Rain chance : 90 %
  Reason      : Rain 95.0 mm >= 90.0 mm
```

**Test ที่ต้องมี (4–5 เคสของ `RiskAnalyzer`)**

| เคส | rainMm | rainChance | คาดว่า |
|-----|--------|-----------|--------|
| ฝนน้อย | 5.0 | 20 | SAFE |
| ขอบเขต WARNING | 35.0 | 0 | WARNING |
| ฝนปานกลาง + โอกาสสูง | 25.0 | 80 | WARNING |
| ฝนมาก | 95.0 | 30 | DANGER |
| ฝนหนัก + โอกาสสูงมาก | 55.0 | 90 | DANGER |

---

## 4. CLI Contract (คน B)

```
java -jar app.jar <location> [--mock]
```

| Argument | บังคับ | ความหมาย |
|----------|--------|----------|
| `<location>` | ใช่ | ชื่อสถานที่ เช่น `Bangkok` |
| `--mock` | ไม่ | ใช้ `MockWeatherProvider` แทน API จริง (สำหรับเดโม / API มีปัญหา) |

ถ้าไม่ใส่ `--mock` ต้องมี environment variable `TMD_API_TOKEN`

| Exit code | ความหมาย | ส่งออกทาง |
|-----------|----------|-----------|
| `0` | สำเร็จ | stdout |
| `1` | args ผิดรูปแบบ (ไม่ใส่ location / args เกิน / ขาด token) พร้อมข้อความวิธีใช้ | stderr |
| `2` | `WeatherException` | stderr |

---

## 5. มาตรฐานโค้ด

| หัวข้อ | ข้อตกลง |
|--------|---------|
| เครื่องมือ | Java 17, Maven, JUnit 5 |
| JSON | Jackson (`jackson-databind`) |
| HTTP | `java.net.http.HttpClient` (ไม่เพิ่ม dependency) |
| ชื่อ | class `PascalCase`, method/field `camelCase`, constant `UPPER_SNAKE_CASE` |
| ภาษาใน code / comment | อังกฤษ |
| ห้าม | `null` จาก method สาธารณะ, hardcode API token, `System.exit` นอก `Main` |

---

## 6. ข้อตกลงการทำงาน

1. **เจ้าของไฟล์** ตัดสินใจรายละเอียดภายในได้เอง แต่ public signature ต้องตรงกับเอกสารนี้
2. **ต้องการเปลี่ยน contract** → แจ้งอีกฝ่ายก่อน → แก้เอกสารนี้ผ่าน PR → อีกฝ่าย approve → ค่อย merge
3. **ติดปัญหา > 20 นาที** → บอกอีกฝ่ายทันที
4. **Review PR ของกันและกัน** ภายใน 15 นาที (ดู [GIT_FLOW.md](GIT_FLOW.md))
5. **Sync สั้นๆ 5 นาที** 3 ครั้ง: 12:30, 15:00, 17:00 — ทำอะไรเสร็จ / ติดอะไร / ต้องการอะไรจากอีกฝ่าย
6. **ถ้าเวลาไม่พอ** ตัดความสวยงามของ output ก่อน; **ห้ามตัด test ของ `RiskAnalyzer`**

---

## 7. ช่องยืนยัน

| รายการ | ผู้ยืนยัน | สถานะ |
|--------|-----------|-------|
| Signature ในหัวข้อ 3 ตกลงกันแล้ว | A + B | ☐ |
| เกณฑ์ SAFE / WARNING / DANGER (หัวข้อ 3.3) ตกลงกันแล้ว | A + B | ☐ |
| ได้ TMD API token แล้ว | A | ✅ |
| เลือก endpoint ของ TMD และรู้ว่า field "โอกาสฝน %" มาจากไหน | A | ✅ (หัวข้อ 3.2) |
| รับทราบวิธีคำนวณ `rainChancePercent` (หัวข้อ 3.2) | B | ☐ |
