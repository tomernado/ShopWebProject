# Spec — שלב 5: דוחות ו-I/O (Logs, JSON, Word)

תאריך: 2026-08-05
תת-פרויקט מתוך: [2026-08-05-store-chain-system-overview-design.md](./2026-08-05-store-chain-system-overview-design.md)
תלוי ב-: שלבים 1-4 — כולם ממוזגים ב-`master`.

## 1. מטרת השלב

שלושה דברים: (א) לוגים מקיפים למערכת — עובדים, לקוחות, מכירות, פרטי צ'אט; (ב) הפקת דוחות סטטיסטיים (לפי סניף/מוצר/קטגוריה) וייצואם ל-JSON; (ג) הצגת אותם דוחות במסמך Word אמיתי (`.docx`) — **דרישה רשמית מפורשת של המרצה (סעיף 7ד), לא אופציה קוסמטית.**

## 2. פער שהתגלה: אין עדיין "רכישה" אמיתית במערכת

הדרישות המקוריות מבקשות לוגים ל"עובדים, לקוחות, קניות/מכירות ופרטי שיחות". עד עכשיו נבנו: עובדים מתחברים (שלב 2), יצירת חשבונות (שלב 3), צ'אט (שלב 4) — אבל **אף פעולת רכישה לא קיימת בשרת**. מחלקות ה-`Customer`/`NewCustomer`/`ReturningCustomer`/`VipCustomer` משלב 1 קיימות ונבדקות ביחידה, אבל מעולם לא הופעלו מזרימת מערכת אמיתית; אותו דבר לגבי `Inventory` — קיימת ונבדקת, אבל בלי נתוני seed אמיתיים ובלי דרך לצרוך אותה מהשרת.

**החלטה (אושרה עם המשתמש):** שלב 5 מוסיף פעולת רכישה מינימלית (`server.SaleService`) שמפעילה בפועל את הפולימורפיזם של שלב 1 (`customer.purchase(...)`) ואת `Inventory.reduceStock(...)`. זה לא "הרחבת היקף" מלאכותית — זו סגירת פער אמיתי, כדי שללוגים ולדוחות יהיה על מה לדווח.

## 3. מבנה חבילות חדשות

```
src/main/java/logging/
  SystemLogger.java        (Singleton שני — ר' סעיף 4)

src/main/java/server/       (תוספות)
  ProductCatalog.java        (נתוני seed: מוצרים + מלאי לכל סניף)
  SaleRecord.java             (רשומת מכירה שנשמרת בזיכרון)
  RecordSaleRequest.java      (Serializable)
  RecordSaleResponse.java     (Serializable)
  SaleService.java

src/main/java/reports/
  SalesReportLine.java
  SalesReport.java
  ReportGenerator.java        (קיבוץ לפי סניף/מוצר/קטגוריה)
  JsonReportExporter.java     (Gson)
  WordReportExporter.java     (Apache POI — .docx אמיתי)
  Main.java                    (דמו הרצה — מייצר קבצים אמיתיים)
```

## 4. `logging.SystemLogger` — Singleton שני, ולמה זה בסדר

Singleton כבר שימש כתשובה לדרישת ה-Design Pattern בשלב 4 (`ChatDispatcher`, לניהול תור השיחות). כאן זו לא "עוד תבנית בשביל לצאת ידי חובה" — לוגר הוא **הדוגמה הכי קלאסית** לשימוש ב-Singleton בעולם האמיתי (`java.util.logging.Logger`, Log4j וכו' עובדים בדיוק ככה): יש קובץ לוג אחד, וכל חלק במערכת צריך גישה אליו בלי להעביר אותו כפרמטר דרך כל שרשרת הבנאים.

```java
public class SystemLogger {
    private static final SystemLogger INSTANCE = new SystemLogger(Path.of("logs", "system.log"));
    public static SystemLogger getInstance() { return INSTANCE; }

    // קונסטרוקטור ציבורי בנוסף ל-Singleton — קיים אך ורק לבדיקות (כדי לכתוב
    // לקובץ זמני מבודד לכל בדיקה), בדיוק כמו resetForTests() ב-ChatDispatcher.
    public SystemLogger(Path logFilePath) { ... }

    public synchronized void log(String category, String message) { ... }
}
```

פורמט שורה: `[yyyy-MM-dd HH:mm:ss] [CATEGORY] message`. נכתב בקובץ (`logs/system.log`), מצורף (append) לא נדרס.

**חיווט:** `AuthService.login/logout`, `AccountService.createAccount`, ו-`ChatDispatcher` (התאמת שיחה / הצטרפות מנהל / הודעה) מוסיפים שורה אחת של `SystemLogger.getInstance().log(...)` בכל אירוע רלוונטי — **בלי לשנות אף קונסטרוקטור קיים**, כי הכל דרך ה-Singleton.

### 4.1 "אופציה לשמירת תוכן השיחה"

`ChatDispatcher` מחזיק קבוע `LOG_CHAT_CONTENT` (כרגע `false`, כברירת מחדל שמכבדת פרטיות). כשהוא `true`, שורת הלוג של הודעת צ'אט כוללת את הטקסט המלא; כש-`false`, רק מטא-דאטה (מי, מתי, לאיזה session). זה בדיוק ה"אופציה" שהדרישה המקורית ביקשה.

## 5. `server.ProductCatalog` + `server.SaleService`

`ProductCatalog` זורע 3 מוצרים קבועים (חלב/לחם/גבינה) ו-`Inventory` לכל אחד משני הסניפים הקיימים (משתמש ב-`employeeDirectory.getBranches()` משלב 3), עם כמות התחלתית לכל מוצר.

`SaleService.recordSale(Employee employee, RecordSaleRequest request)`:
1. מוצא את המוצר ב-`ProductCatalog`; לא נמצא → כישלון.
2. בודק שיש מספיק מלאי בסניף של העובד המחובר; אין → כישלון.
3. בונה את מחלקת ה-`Customer` הנכונה (`NewCustomer`/`ReturningCustomer`/`VipCustomer`) לפי `CustomerType` שנשלח בבקשה — **כאן הפולימורפיזם משלב 1 סוף-סוף רץ בזרימה אמיתית**, לא רק ביוניט-טסט.
4. מחשב `finalAmount = customer.purchase(price * quantity)`.
5. מפחית מלאי (`inventory.reduceStock`).
6. שומר `SaleRecord` ברשימה בזיכרון (`salesLedger`) — זהו מקור הנתונים לדוחות בסעיף 6.
7. רושם ללוג (`"SALE"`).

זו לא "מערכת קופה" מלאה — אין עגלת קניות, אין תשלום אמיתי. זה בדיוק ברמת "פעולת רכישה מינימלית" שסוכם עם המשתמש: מספיק כדי שדוחות ולוגים יהיו על נתונים אמיתיים.

## 6. `reports` — סטטיסטיקה, JSON, Word

### 6.1 `ReportGenerator`

מקבל `List<SaleRecord>` ומחזיר `SalesReport` (קיבוץ + `key`, `totalQuantity`, `totalRevenue` לכל קבוצה) לפי אחד משלושה מפתחות: סניף (`branchId`), מוצר (`productName`), קטגוריה (`category`). קיבוץ באמצעות `Map.merge` — תבנית אקומולציה סטנדרטית וקלה להסבר.

### 6.2 `JsonReportExporter` (Gson)

`export(SalesReport, Path)` — `new GsonBuilder().setPrettyPrinting().create().toJson(report)`, נכתב לקובץ. תלות חדשה ב-`pom.xml`: `com.google.code.gson:gson`.

### 6.3 `WordReportExporter` (Apache POI, `.docx` אמיתי)

`export(SalesReport, Path)` — בונה `XWPFDocument` אמיתי: פסקת כותרת + טבלה (עמודות: מפתח הקיבוץ, כמות כוללת, הכנסה כוללת), שורה אחת לכל `SalesReportLine`, ונכתב לקובץ `.docx`. תלות חדשה ב-`pom.xml`: `org.apache.poi:poi-ooxml`. **זו הספרייה הכי "כבדה" בפרויקט עד כה** — זה מתועד כאן במפורש כי היא מוצדקת: המרצה דורש הצגת דוחות במסמך Word בפועל (סעיף 7ד), לא רק "פורמט שדומה ל-Word".

הבדיקות (`WordReportExporterTest`) לא רק מוודאות "לא קרסה" — הן **קוראות את הקובץ בחזרה** דרך POI (`XWPFDocument` מ-`FileInputStream`) ומוודאות שהכותרת והנתונים בטבלה נכונים. זו ההוכחה שהקובץ אמיתי וקריא, לא רק שנוצר.

### 6.4 `reports.Main` — דמו הרצה

נקודת כניסה שרצה עצמאית (בלי שרת רץ): בונה `EmployeeDirectory` + `ProductCatalog` + `SaleService`, רושם כמה מכירות דמו, מייצר את שלושת סוגי הדוח (סניף/מוצר/קטגוריה) ומייצא כל אחד גם ל-JSON וגם ל-Word לתיקייה `reports/`. **זו הדרך המעשית להראות למרצה קובץ `.docx` אמיתי** — מריצים את זה ומקבלים קבצים אמיתיים על הדיסק.

## 7. מה בכוונה נשאר מחוץ לשלב 5 — ולמה

**דוחות/רכישה לא מחוברים לפרוטוקול הרשת (אין `GenerateReportRequest` שהלקוח שולח).** באותה רוח שבה שלב 4 השאיר את הצ'אט ברמת שרת/פרוטוקול בלי מסך GUI (כי זה דרש תשתית Push א-סינכרונית שלא הייתה קיימת), כאן הפקת הדוחות נשארת שירות עצמאי שניתן לבדיקה מלאה ולהרצה ידנית (`reports.Main`), לא פעולה שהלקוח מבקש דרך ה-Socket. חשוב: **פעולת הרכישה עצמה (`RecordSaleRequest`) כן מחוברת לפרוטוקול** (סעיף 5) — רק ייצור/ייצוא הדוחות עצמם נשאר צד-שרת בלבד.

- מסך "רכישה" או "דוחות" ב-Swing — לא בשלב זה (יכול להיות הרחבה עתידית, כמו שצ'אט GUI נשאר הרחבה עתידית לשלב 4).
- לוגים לכישלונות (login שגוי, מלאי חסר וכו') — רק אירועים מוצלחים נרשמים ללוג, כדי לשמור על היקף מינימלי; מגבלה ידועה ומתועדת.
- אין persistence אמיתי (DB/קובץ) לרשימת המכירות — `salesLedger` נשאר בזיכרון, כמו `EmployeeDirectory` מאז שלב 2. זהו סוף שרשרת ה-persistence הזמני שהוזכר בכל שלב קודם — אין עוד "שלב הבא" שיתקן את זה.

## 8. יציאה (Definition of Done לשלב 5)

- כל המחלקות בסעיף 3 קיימות, מתקמפלות.
- `SystemLoggerTest`, `ProductCatalogTest`, `SaleServiceTest`, `SaleIntegrationTest` (Socket אמיתי), `ReportGeneratorTest`, `JsonReportExporterTest`, `WordReportExporterTest` עוברים, כולל כל הבדיקות הקיימות משלבים 1-4.
- הרצה ידנית: `reports.Main` רץ בהצלחה ומייצר קבצי `.json` ו-`.docx` אמיתיים בתיקיית `reports/` — נפתח בפועל ב-Word/Excel כדי לוודא שהוא קריא.
