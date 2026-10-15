# Git Flow — Real-time Flood Risk Alerter

Git Flow แบบย่อสำหรับทีม 2 คน ทำงานภายใน 1 วัน: branch สั้น, PR เล็ก, review ข้ามกัน

Repository: https://github.com/Automatic28m/bangkok_rain_prediction
เอกสารอื่น: [SCHEDULE.md](SCHEDULE.md) · [CONTRACT.md](CONTRACT.md)

---

## 1. โครงสร้าง Branch

```
main        ●──────────────────────────────────────●   ← พร้อมเดโม/ส่งเท่านั้น
             \                                    /
develop       ●──●────●────●────●────●────●──────●     ← รวมงานทุก feature
                  \  /      \  /      \  /
feature/*          ●●        ●●        ●●              ← branch สั้นๆ ของแต่ละคน
```

| Branch | หน้าที่ | วิธีเข้า | ลบหลัง merge? |
|--------|---------|----------|---------------|
| `main` | โค้ดที่ build ผ่านและพร้อมเดโม | PR จาก `develop` ตอนท้ายวัน | ไม่ลบ |
| `develop` | แหล่งรวมงานทุก feature | PR เท่านั้น | ไม่ลบ |
| `feature/<ชื่องาน>` | งาน 1 ชิ้นต่อ 1 branch | แตกจาก `develop` | ลบ |
| `fix/<ชื่อบั๊ก>` | แก้บั๊กหลังรวมระบบ | แตกจาก `develop` | ลบ |
| `docs/<ชื่อ>` | แก้เอกสารอย่างเดียว | แตกจาก `develop` | ลบ |

> **ข้อยกเว้นครั้งเดียว:** repo ตอนนี้ว่าง (ยังไม่มี commit) ให้ commit แรกของ `main` เป็นโครง Maven + `docs/` + `.gitignore`
> จากนั้นสร้าง `develop` ทันที แล้วใช้ PR ทุกครั้งหลังจากนั้น

---

## 2. การตั้งชื่อ Branch

รูปแบบ: `<type>/<owner>-<short-description>` (ตัวพิมพ์เล็ก คั่นด้วย `-`)

| งาน | ชื่อ branch |
|-----|-------------|
| A: model + `WeatherProvider` + Mock | `feature/a-provider-interface-and-mock` |
| A: TMD client | `feature/a-tmd-weather-provider` |
| A: จัดการ error | `feature/a-provider-error-handling` |
| B: `RiskAnalyzer` + `RiskResult` | `feature/b-risk-analyzer` |
| B: `AlertPrinter` + `Main` | `feature/b-cli-and-alert-printer` |
| B: เทสต์ `RiskAnalyzer` | `feature/b-risk-analyzer-tests` |
| ร่วม: ต่อระบบ | `feature/ab-integration` |

---

## 3. Commit Message (Conventional Commits)

```
<type>(<scope>): <สรุปสั้นๆ ไม่เกิน 72 ตัวอักษร>
```

| type | ใช้เมื่อ |
|------|---------|
| `feat` | เพิ่มความสามารถใหม่ |
| `fix` | แก้บั๊ก |
| `refactor` | ปรับโครงสร้างโดยพฤติกรรมเท่าเดิม |
| `test` | เพิ่ม/แก้ test |
| `docs` | แก้เอกสาร |
| `chore` | ตั้งค่า build, dependency |

ตัวอย่าง:

```
feat(provider): add MockWeatherProvider with default sample data
feat(provider): add TmdWeatherProvider JSON parsing
feat(risk): implement RiskAnalyzer thresholds
test(risk): cover WARNING boundary at 35 mm
fix(cli): exit with code 1 when location is missing
docs(contract): confirm risk thresholds
```

กติกา: 1 commit = 1 เรื่อง, commit บ่อยๆ, เขียนเป็นภาษาอังกฤษให้สม่ำเสมอ

---

## 4. ขั้นตอนประจำ

```bash
# 1) เริ่มงานใหม่จาก develop ล่าสุด
git checkout develop
git pull origin develop
git checkout -b feature/b-risk-analyzer

# 2) ทำงาน + commit
git add <ไฟล์ที่เกี่ยวข้อง>
git commit -m "feat(risk): implement RiskAnalyzer thresholds"

# 3) ก่อน push: รวม develop ล่าสุดเข้ามา
git fetch origin
git rebase origin/develop      # หรือ git merge origin/develop ถ้าไม่ถนัด rebase

# 4) push แล้วเปิด Pull Request ไปที่ develop
git push -u origin feature/b-risk-analyzer
```

### Pull Request

- **base:** `develop` ← **compare:** `feature/...`
- ชื่อ PR ใช้รูปแบบเดียวกับ commit message
- ขนาดเล็ก (ควรไม่เกิน ~300 บรรทัด) ให้ review เสร็จใน 15 นาที
- **Reviewer:** อีกคนในทีมเสมอ (A ↔ B)
- **Merge:** Squash and merge แล้วลบ branch

### PR Checklist

- [ ] `mvn test` ผ่านในเครื่อง
- [ ] ไม่มีไฟล์ที่ไม่เกี่ยวข้อง (`target/`, `.idea/`, `*.iml`)
- [ ] ไม่มี secret (API token, `.env`) ใน commit
- [ ] public signature ตรงกับ [CONTRACT.md](CONTRACT.md)
- [ ] แก้เฉพาะไฟล์ในความรับผิดชอบ (ถ้าไม่ใช่ ให้บอกอีกคน)

---

## 5. Release (ท้ายวัน)

```bash
git checkout develop && git pull origin develop
mvn clean test          # ต้องผ่านทั้งหมด
# เปิด PR: develop → main (อีกคน review) แล้ว merge
```

หลัง merge ให้ซ้อมเดโมจาก `main` อีกครั้งทั้ง API จริงและ `--mock`

---

## 6. การจัดการ Conflict

1. **ลดโอกาสเกิด:** แต่ละคนแตะเฉพาะไฟล์ของตัวเอง (ตามตารางใน [SCHEDULE.md](SCHEDULE.md))
2. **ไฟล์ร่วม** (`pom.xml`, `README.md`, `docs/`): แก้ใน PR สั้นๆ merge ทันที แล้วแจ้งอีกคนให้ pull
3. **ถ้าเกิด conflict:** เจ้าของ branch แก้เอง ถ้าไม่แน่ใจให้คุยกันก่อนเลือกว่าจะเก็บฝั่งไหน
4. **หลังแก้:** รัน `mvn test` ซ้ำก่อน push

---

## 7. ไฟล์ที่ต้องอยู่ใน `.gitignore`

```
target/
.idea/
*.iml
.vscode/
.env
*.log
```

API token ของ TMD ให้อ่านจาก environment variable `TMD_API_TOKEN` เท่านั้น ห้าม hardcode หรือ commit

---

## 8. กฎสรุป

1. ไม่ push ตรงเข้า `main` / `develop` (ยกเว้น commit แรกของ repo)
2. 1 branch = 1 งาน อายุไม่เกินครึ่งวัน
3. ทุก PR ต้องมีอีกคน approve
4. `develop` ต้อง build ผ่านเสมอ — ถ้าพัง ให้แก้ก่อนทำงานอื่น
5. ไม่ commit secret
6. Commit message ตาม Conventional Commits
