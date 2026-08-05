# Spec — שלב 1: Entities / מודלים

תאריך: 2026-08-05
תת-פרויקט מתוך: [2026-08-05-store-chain-system-overview-design.md](./2026-08-05-store-chain-system-overview-design.md)
תלוי ב-: —

## 1. מטרת השלב

לבנות את מחלקות הבסיס (Entities) של המערכת: עובד, סניף, מוצר, מלאי, והיררכיית הלקוחות (עם פולימורפיזם ברכישה). **ללא** שרת, GUI, קבצים או תקשורת — רק מודלים + לוגיקה + unit tests. שכבות אלו ישמשו את שלבים 2-5 בהמשך.

תזכורת אילוץ מחייב (מפורט במסמך-העל): קוד פשוט, ניתן להסבר מלא ע"י סטודנט, ללא תבניות/הפשטות שלא נדרשות.

## 2. מבנה חבילות (packages)

```
src/main/java/
  model/
    Employee.java
    Role.java              (enum: MANAGER, CASHIER, SELLER)
    Branch.java
    Product.java
    Inventory.java
    Customer.java           (abstract)
    NewCustomer.java
    ReturningCustomer.java
    VipCustomer.java
    CustomerType.java       (enum, לשימוש בתצוגה/דוחות — לא לוגיקה)
```

## 3. מחלקות — שדות ואחריות

### 3.1 `Branch`
שדות: `branchId` (int/String), `name`, `address`.
מייצג סניף בודד ברשת החנויות. `Employee` ו-`Inventory` מקושרים אליו.

### 3.2 `Employee`
שדות: `idNumber` (ת"ז), `fullName`, `username`, `role` (`Role`), `branch` (`Branch`).
עדיין **ללא** סיסמה/אימות — זה מגיע בשלב 2 (Server/Auth), כדי לא לערבב אחריות. שלב 1 מודל בלבד.

### 3.3 `Role` (enum)
`MANAGER, CASHIER, SELLER` — תואם "מנהל משמרת, קופאי, מוכר" מהדרישות.

### 3.4 `Product`
שדות: `productId`, `name`, `category` (String), `price` (double/BigDecimal).
מוצר קטלוגי, בלי קשר לכמות מלאי (הכמות היא באחריות `Inventory`).

### 3.5 `Inventory`
שדות: `branch` (`Branch`), `stock` (`Map<Product, Integer>` — כמות זמינה לכל מוצר בסניף הזה).
מתודות: `getQuantity(Product)`, `reduceStock(Product, int)`, `addStock(Product, int)`.
מייצג "מלאי חנות נפרד" לפי הדרישה — כל סניף = מופע `Inventory` אחד. עדכון בזמן-אמת בין עובדי הסניף יטופל בשלב 2 (Server ישדר עדכונים) — כאן רק המודל שמחזיק את הנתון.

### 3.6 היררכיית `Customer` — הליבה של שלב 1

```
        Customer (abstract)
       /       |          \
NewCustomer  ReturningCustomer  VipCustomer
```

**`Customer` (abstract class)**
שדות משותפים: `fullName`, `idNumber`, `phone`, `customerType` (`CustomerType`, לתצוגה/דוחות בלבד).
מתודה אבסטרקטית: `abstract double calculateDiscount(double totalAmount);`
מתודה קונקרטית משותפת: `double purchase(double totalAmount)` — מחשבת `totalAmount - calculateDiscount(...)`, כך שכל תת-מחלקה רק מגדירה את אחוז/סכום ההנחה, וההתנהגות המשותפת (חישוב הסכום הסופי) לא משוכפלת.

**כללי הנחה** (ברירת מחדל פשוטה, ניתנת להרחבה — אין דרישה מדויקת מהמרצה מעבר ל"מבצע ומסלול שונה", ולכן נשארים במינימום סביר):
- `NewCustomer.calculateDiscount` → **0%**.
- `ReturningCustomer.calculateDiscount` → **5%**.
- `VipCustomer.calculateDiscount` → **10%**.

זה ממלא את הדרישה המפורשת ("כל מחלקה תטפל במבצע ובמסלול רכישה שונה... פולימורפיזם") בלי לבנות מנגנון מבצעים מורכב שלא נתבקש. אם בהמשך יתברר שהמרצה דרש כללים ספציפיים יותר (למשל מבצע לפי קטגוריית מוצר, סף VIP וכו') — מעדכנים רק את שלוש המתודות האלו, המבנה לא משתנה.

**`CustomerType` (enum: NEW, RETURNING, VIP)** — משמש רק לשדה תיאורי/לוגים/דוחות (שלב 5), לא לקבלת החלטות (ההחלטה היא דרך הפולימורפיזם, לא דרך switch על ה-enum — זה בדיוק ה"דגש הארכיטקטוני חובה" מהדרישות).

## 4. למה לא Strategy Pattern / לא enum+switch

- **Strategy Pattern** (Customer מחזיק אובייקט `DiscountStrategy`) — נפסל: פתרון תקין אך "מהודק" ומופשט יותר משנדרש; המרצה ביקש במפורש "מחלקה נפרדת לכל סוג לקוח", כלומר הורשה רגילה, לא קומפוזיציה של אסטרטגיה. גם עלול להיראות כמו הכנסת תבנית עיצוב לא-מוסברת בשלב שלא מיועד לזה (Design Patterns מרוכזים בדרישה בשלב הצ'אט, שלב 4).
- **מחלקה יחידה + enum + switch** — נפסל: סותר במפורש את הדרישה "יש להגדיר מחלקה נפרדת לכל סוג לקוח".
- **נבחר: הורשה פשוטה + מתודה אבסטרקטית** — התאמה ישירה לניסוח הדרישה, הכי קל להסבר, תבנית OOP סטנדרטית מהקורס.

## 5. בדיקות (Unit Tests — JUnit)

לפי הדרישה המפורשת ("חובה לכלול קבצי בדיקה לבדיקת הקוד לפני בדיקה חיצונית"), לכל מחלקת לוגיקה:
- `CustomerDiscountTest` — בודק ש-`purchase()` מחזיר סכום נכון עבור כל אחד משלושת סוגי הלקוח (כולל מקרה גבול: totalAmount = 0).
- `InventoryTest` — הוספה/הפחתה של מלאי, כולל מקרה שמנסים להפחית יותר ממה שיש (מוגדר: יזרוק `IllegalStateException` פשוטה — אין overengineering של exception hierarchy custom).
- `EmployeeTest` / `ProductTest` — בדיקות פשוטות ל-getters/constructor validation (למשל שם לא ריק).

## 6. מה בכוונה נשאר מחוץ לשלב 1

- סיסמאות/אימות/מניעת login כפול → שלב 2.
- עדכון מלאי "בזמן אמת" בין כמה עובדים (broadcast) → שלב 2 (תלוי בשרת).
- GUI כלשהו → שלב 3.
- לוגים/JSON/Word → שלב 5.

## 7. יציאה (Definition of Done לשלב 1)

- כל המחלקות בסעיף 3 קיימות, מתקמפלות, עם Javadoc קצר (רמת קורס — לא חובה מסיבית) במקומות הלא-מובנים מאליהם.
- כל הבדיקות בסעיף 5 עוברות (`mvn test` ירוק).
- פרויקט Maven תקין (`pom.xml` עם `junit`), נבנה בהצלחה (`mvn compile`).
