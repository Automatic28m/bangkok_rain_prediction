# TMD API Reference

สรุป URL และค่าที่ได้จาก TMD Weather Forecast API (กรมอุตุนิยมวิทยา) — **ทดสอบกับ API จริงเมื่อ 2026-10-15**

> **สำหรับคน B:** ไม่ต้องเรียก URL เหล่านี้เอง `TmdWeatherProvider` จัดการให้แล้ว ดูหัวข้อ [6. สิ่งที่ฝั่ง Logic + CLI ใช้จริง](#6-สิ่งที่ฝั่ง-logic--cli-ใช้จริง)
> เอกสารอื่น: [CONTRACT.md](CONTRACT.md) · [SCHEDULE.md](SCHEDULE.md) · [GIT_FLOW.md](GIT_FLOW.md)

---

## 1. ข้อมูลทั่วไป

| รายการ | ค่า |
|--------|-----|
| Base URL | `https://data.tmd.go.th/nwpapi/v1/forecast/location/` |
| Auth | Header `authorization: Bearer <TMD_API_TOKEN>` |
| Format | JSON |
| เวลา | เวลาไทย (`+07:00`) |
| พื้นที่ที่รองรับ | lat 5.61–20.47, lon 97.34–105.64 (ประเทศไทย) |

---

## 2. URL ที่เรียกได้ (8 รูปแบบ)

| # | URL | ระบุพื้นที่ด้วย | ผลทดสอบ |
|---|-----|-----------------|---------|
| 1 | `hourly` | — (ข้อมูลช่วงเวลา) | ✅ 200 |
| 2 | `hourly/at` | `lat`, `lon` | ✅ 200 **← โปรเจกต์ใช้ตัวนี้** |
| 3 | `hourly/place` | `province`, `amphoe`, `tambon` (ชื่อไทย) | ❌ 404 ทุกครั้ง |
| 4 | `hourly/region` | `region` = `C`, `N`, `NE`, `E`, `S`, `W` | ✅ 200 |
| 5 | `daily` | — (ข้อมูลช่วงเวลา) | ✅ 200 |
| 6 | `daily/at` | `lat`, `lon` | ✅ 200 |
| 7 | `daily/place` | `province`, `amphoe`, `tambon` | ❌ 404 ทุกครั้ง (ลองถึงระดับตำบลแล้ว) |
| 8 | `daily/region` | `region` | ✅ 200 |

### Parameter ที่ใช้ร่วมกัน

| Parameter | ใช้กับ | ความหมาย |
|-----------|--------|----------|
| `fields` | ทุกตัวยกเว้น 1, 5 | field ที่ต้องการ คั่นด้วย `,` เช่น `rain,cond,rh` |
| `date` | ทุกตัวยกเว้น 1, 5 | วันเริ่มต้น `yyyy-MM-dd` |
| `hour` | `hourly/*` | ชั่วโมงเริ่มต้น 0–23 |
| `duration` | ทุกตัวยกเว้น 1, 5 | จำนวนชั่วโมง (สูงสุด 48) หรือจำนวนวัน (สูงสุด 126) |

ค่า default ของ `fields`: รายชั่วโมง `tc,rh,cond` / รายวัน `tc_min,tc_max,rh,cond`

---

## 3. ค่าที่แต่ละ URL คืน

### 3.1 `hourly`, `daily` — ช่วงเวลาที่มีข้อมูล

คืนแค่ช่วงเวลาที่มีพยากรณ์ ไม่มีข้อมูลอากาศ

```json
{"hourly_data":{"min":"2026-10-15T02:00:00+07:00","max":"2026-10-18T01:00:00+07:00"}}
```

```json
{"daily_data":{"min":"2026-10-15","max":"2026-10-25"}}
```

ณ วันทดสอบ: รายชั่วโมงพยากรณ์ล่วงหน้าประมาณ 3 วัน, รายวันประมาณ 11 วัน

### 3.2 `hourly/at`, `daily/at` — พยากรณ์ของจุดพิกัดเดียว

```json
{"WeatherForecasts":[{
  "location":{"lat":13.7696,"lon":100.4965},
  "forecasts":[
    {"time":"2026-10-15T15:00:00+07:00",
     "data":{"cond":3,"rain":0,"rh":49.39,"tc":32.93,"ws10m":4.71}}
  ]}]}
```

- `location` คือจุดกริดที่ใกล้ที่สุด ไม่ใช่พิกัดที่ส่งไปตรงๆ
- `forecasts` มี 1 รายการต่อ 1 ชั่วโมง / 1 วัน ตาม `duration`
- ถ้าขอเวลาก่อนช่วงที่มีข้อมูล จะเริ่มจากเวลาแรกที่มี (เช่น ขอ `hour=0` ได้เริ่มที่ 02:00)

ตัวอย่างรายวัน (`daily/at`, `duration=2`):

```json
{"forecasts":[
  {"time":"2026-10-15T00:00:00+07:00","data":{"cond":3,"rain":16.8,"rh":74.4,"tc_max":32.93,"tc_min":24.85}},
  {"time":"2026-10-16T00:00:00+07:00","data":{"cond":3,"rain":11.6,"rh":73.11,"tc_max":33.44,"tc_min":26.84}}
]}
```

### 3.3 `hourly/region`, `daily/region` — ทุกจังหวัดในภาค

คืน 1 รายการต่อ 1 จังหวัด พร้อมข้อมูลพื้นที่ครบ

```json
{"WeatherForecasts":[
  {"location":{"province":"กรุงเทพมหานคร","name":"กรุงเทพมหานคร","areatype":"province",
               "region":"C","geocode":"10","amphoe":null,"tambon":null,
               "lat":13.753958,"lon":100.501746},
   "forecasts":[{"time":"2026-10-15T00:00:00+07:00","data":{"rain":49.9}}]},
  ...
]}
```

ชื่อภาษาไทยใน response จริงมาเป็น Unicode escape (`ก...`) Jackson แปลงให้อัตโนมัติ

### 3.4 `hourly/place`, `daily/place` — ใช้ไม่ได้

```json
{"WeatherForecasts":[]}
```

ตอบ HTTP 404 ทุกครั้ง ทั้งชื่อไทย ชื่ออังกฤษ และระบุถึงระดับอำเภอ/ตำบล

---

## 4. Field ใน `data`

ได้เฉพาะ field ที่ขอใน `fields`

| field | ความหมาย | หน่วย | รายชั่วโมง | รายวัน |
|-------|----------|-------|:---:|:---:|
| `rain` | ปริมาณฝน | มม. | ต่อชั่วโมง | รวม 24 ชม. |
| `tc` | อุณหภูมิ | °C | ✅ | — |
| `tc_max`, `tc_min` | อุณหภูมิสูงสุด / ต่ำสุด | °C | — | ✅ |
| `rh` | ความชื้นสัมพัทธ์ | % | ✅ | ✅ (เฉลี่ย) |
| `cond` | รหัสสภาพอากาศ (ตารางด้านล่าง) | 1–12 | ✅ | ✅ |
| `ws10m`, `wd10m` | ความเร็ว / ทิศทางลมที่ 10 ม. | m/s / องศา | ✅ | ✅ (สูงสุด) |
| `slp` | ความกดอากาศระดับน้ำทะเล | hPa | ✅ | ✅ (เฉลี่ย) |
| `cloudlow`, `cloudmed`, `cloudhigh` | ปริมาณเมฆชั้นต่ำ / กลาง / สูง | % | ✅ | ✅ (เฉลี่ย) |
| `psfc`, `swdown` | ความกดอากาศพื้นผิว / รังสีดวงอาทิตย์ | Pa / W m⁻² | — | ✅ |

**รหัส `cond`:** 1 ท้องฟ้าแจ่มใส, 2 มีเมฆบางส่วน, 3 เมฆเป็นส่วนมาก, 4 มีเมฆมาก, 5 ฝนตกเล็กน้อย, 6 ฝนปานกลาง, 7 ฝนตกหนัก, 8 ฝนฟ้าคะนอง, 9 อากาศหนาวจัด, 10 อากาศหนาว, 11 อากาศเย็น, 12 อากาศร้อนจัด

> **ไม่มี field "โอกาสฝนตก %"** ในทุก endpoint — โปรเจกต์จึงคำนวณเองจากข้อมูลรายชั่วโมง (ดู [CONTRACT.md](CONTRACT.md) หัวข้อ 3.2)

---

## 5. Error ที่เจอจริง

| กรณี | HTTP | body |
|------|------|------|
| token ผิด / ไม่ส่ง | 401 | `{"error":"Unauthenticated."}` |
| พิกัดนอกประเทศไทย | 422 | `{"lat":["The lat must be between 5.61 and 20.47."],"lon":["The lon must be between 97.34 and 105.64."]}` |
| `place` หาไม่พบ | 404 | `{"WeatherForecasts":[]}` |

---

## 6. สิ่งที่ฝั่ง Logic + CLI ใช้จริง

คน B ใช้ผ่าน interface `WeatherProvider` เท่านั้น:

```java
WeatherProvider provider = useMock
        ? new MockWeatherProvider()
        : new TmdWeatherProvider(System.getenv("TMD_API_TOKEN"));

WeatherData data = provider.fetch("Bangkok");
data.getRainMm();             // ฝนรวม 24 ชม. ข้างหน้า (มม.)
data.getRainChancePercent();  // % ของชั่วโมงที่มีฝน >= 0.1 มม. (ค่าจริงมักอยู่ที่ 0–20)
```

`TmdWeatherProvider` เรียก `hourly/at` แบบ `fields=rain`, `duration=24` เริ่มจากชั่วโมงปัจจุบัน แล้วสรุปเป็น `WeatherData`

ถ้าดึงข้อมูลไม่ได้ `fetch` โยน `WeatherException`:

| ข้อความ | สาเหตุ |
|---------|--------|
| `Location not found: <location>` | ชื่อไม่อยู่ในตาราง หรือพิกัดนอกไทย (422) |
| `Invalid or missing API token` | 401 / 403 |
| `Cannot reach weather service (timeout)` | เกิน 10 วินาที |
| `Cannot reach weather service` | เครือข่ายมีปัญหา |
| `Weather service returned HTTP <code>` | HTTP อื่นที่ไม่ใช่ 200 |
| `Unexpected response from weather service` | JSON ผิดรูป / ว่าง / ไม่มี `rain` |

---

## 7. ข้อสังเกต

- **`region` ใช้แทน `place` ได้:** คืนชื่อจังหวัดภาษาไทยพร้อมพิกัด ถ้าอนาคตอยากรับชื่อจังหวัดได้ครบ 77 จังหวัด (แทนตาราง 17 เมืองใน `TmdWeatherProvider`) ใช้ endpoint นี้หาพิกัดได้ — ยังไม่จำเป็นสำหรับงาน 1 วัน
- **ค่าฝนต่างกันตามจุดอ้างอิง:** กรุงเทพฯ จาก `daily/region` ได้ 49.9 มม. แต่ `daily/at` ที่พิกัดในโปรเจกต์ได้ 16.8 มม. ในวันเดียวกัน เพราะใช้คนละจุดกริด
- **ผลรวมรายชั่วโมง ≈ ค่ารายวัน:** ฝนรายชั่วโมงรวม 16.7 มม. เทียบกับ `daily/at` 16.8 มม. ที่จุดเดียวกัน
