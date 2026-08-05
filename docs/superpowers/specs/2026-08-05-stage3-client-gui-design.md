# Spec — שלב 3: Client GUI (Swing)

תאריך: 2026-08-05
תת-פרויקט מתוך: [2026-08-05-store-chain-system-overview-design.md](./2026-08-05-store-chain-system-overview-design.md)
תלוי ב-: שלב 1 (Entities), שלב 2 (Server) — שניהם ממוזגים ב-`master`.

## 1. מטרת השלב

לבנות אפליקציית לקוח ב-Swing עם שלושה מסכים: התחברות, לוח מידע לעובד (מותאם לתפקיד וסניף), ומסך אדמין ליצירת חשבונות חדשים (זמין רק למנהל משמרת). המסכים מתחברים לשרת האמיתי משלב 2 דרך Socket — לא Mock, לא נתונים מדומים בצד לקוח.

תזכורת אילוץ מחייב (מפורט במסמך-העל): קוד פשוט, ניתן להסבר מלא ע"י סטודנט. Swing סטנדרטי בלבד — ללא JavaFX, ללא ספריות UI חיצוניות.

## 2. היקף "לוח המידע לעובד" — החלטה מפורשת

המסך המותאם לתפקיד/סניף הוא **לוח מידע (Dashboard)**, לא מסך קנייה/מכירה מלא: מציג שם, תפקיד וסניף של העובד המחובר, ואם התפקיד הוא MANAGER — גם כפתור לפתיחת מסך האדמין. ממשק קנייה/מכירה מלא (עם היררכיית ה-Customer משלב 1) אינו חלק מהיקף שלב 3 — ייתכן שיתווסף בשלב עתידי, אך לא נדרש כאן.

## 3. מבנה חבילות (packages)

```
src/main/java/
  model/            (ללא שינוי משלבים 1-2)
  server/
    PasswordPolicy.java        (חדש)
    CreateAccountRequest.java  (חדש, Serializable)
    CreateAccountResponse.java (חדש, Serializable)
    AccountService.java        (חדש)
    EmployeeDirectory.java     (מתעדכן: getBranches, findBranchById, addEmployee הופך public)
    ClientHandler.java         (מתעדכן: לולאת דיווח אחרי login מטפלת עכשיו ב-CreateAccountRequest)
    Server.java                 (מתעדכן: מקבל גם AccountService)
    Main.java                   (מתעדכן: בונה AccountService ומעביר ל-Server)
  client/            (חדש)
    ServerConnection.java
    LoginPanel.java
    DashboardPanel.java
    AdminPanel.java
    MainFrame.java
    Main.java
```

## 4. שרת — הרחבות נדרשות

### 4.1 `server.PasswordPolicy`

`String validate(String password)` — מחזיר `null` אם תקין, אחרת הודעת שגיאה. כלל (ברירת מחדל, ללא מספר מדויק מהמרצה — כמו ההנחות בשלב 1): לפחות 6 תווים **וגם** לפחות ספרה אחת.

### 4.2 `server.CreateAccountRequest` / `CreateAccountResponse`

זהה בסגנון ל-`LoginRequest`/`LoginResponse` משלב 2 — `Serializable`, שני שדות פשוטים ב-Response (`success`, `errorMessage`).

`CreateAccountRequest` נושא את כל מה שצריך כדי לבנות `Employee` חדש: `idNumber, fullName, username, password, Role role, String branchId` (מזהה סניף קיים, לא אובייקט `Branch` מלא — הלקוח בוחר מתוך רשימת סניפים קיימת).

### 4.3 `server.EmployeeDirectory` — הרחבה

- `List<Branch> getBranches()` — מחזיר את שני הסניפים שנזרעו (B1/B2), כדי שגם השרת (בדיקת branchId) וגם הלקוח (תפריט בחירה) יעבדו מול אותם סניפים.
- `Branch findBranchById(String branchId)`.
- `addEmployee(Employee)` הופך מ-`private` ל-`public` — נקרא גם מהקונסטרקטור (זריעה) וגם מ-`AccountService` (יצירת חשבון בזמן ריצה).

### 4.4 `server.AccountService`

`CreateAccountResponse createAccount(Role requesterRole, CreateAccountRequest request)`:
1. אם `requesterRole != Role.MANAGER` → כישלון ("רק מנהל משמרת יכול ליצור חשבונות").
2. הרצת `PasswordPolicy.validate(request.getPassword())` — אם לא `null`, כישלון עם ההודעה.
3. אם `employeeDirectory.findByUsername(request.getUsername()) != null` → כישלון ("שם המשתמש כבר קיים").
4. אם `employeeDirectory.findBranchById(request.getBranchId()) == null` → כישלון ("סניף לא קיים").
5. אחרת: בונה `Employee` חדש, קורא ל-`employeeDirectory.addEmployee(...)`, מחזיר הצלחה.

**חשוב:** ה-`EmployeeDirectory` שמועבר ל-`AccountService` חייב להיות **אותו מופע** שמועבר ל-`AuthService` — אחרת חשבון חדש שנוצר לא יהיה נראה בבדיקת login. שניהם נבנים פעם אחת ב-`Main` ומועברים כפרמטרים (לא כל אחד בונה `EmployeeDirectory` משלו).

### 4.5 `server.ClientHandler` — הרחבת לולאת ההודעות

בשלב 2 הלולאה אחרי login הצליח רק "המתינה לניתוק" (`in.readObject()` בלולאה, בלי לעשות כלום עם התוצאה — עם הערה שזה יורחב בהמשך). עכשיו היא בפועל מפלגת (dispatch) לפי סוג ההודעה:

```java
Object message = in.readObject();
if (message instanceof CreateAccountRequest request) {
    CreateAccountResponse response = accountService.createAccount(loggedInEmployee.getRole(), request);
    out.writeObject(response);
    out.flush();
}
```

ה-`Employee` המחובר (לא רק שם המשתמש שלו) נשמר על ה-`ClientHandler` אחרי login מוצלח, כדי שיהיה אפשר לבדוק את ה-Role שלו בלי לפנות שוב ל-`EmployeeDirectory`.

## 5. לקוח — חבילת `client` חדשה

### 5.1 `client.ServerConnection`

עטיפה דקה סביב `Socket` + `ObjectOutputStream`/`ObjectInputStream` (אותו סדר יצירה כמו ב-`ClientHandler` — output לפני input, כדי למנוע deadlock). שתי מתודות בלבד: `login(username, password)` ו-`createAccount(CreateAccountRequest)`, שתיהן סינכרוניות (שולחות ומחכות לתשובה — בלי async/callbacks, זה מעבר לנדרש בקורס).

**הערה ארכיטקטונית:** הלקוח (`client`) והשרת (`server`) חיים באותו מודול Maven ומשתמשים **ישירות** באותן מחלקות הודעה (`LoginRequest`, `CreateAccountRequest` וכו') — אין כפילות DTOs. זו הפשטה סבירה לפרויקט קורס עם קודבייס אחד; במערכת אמיתית עם שני deployables נפרדים זה היה דורש מודול "common" משותף.

### 5.2 מסכי Swing (`MainFrame` + `CardLayout`)

- **`LoginPanel`** — שדות משתמש/סיסמה + כפתור. בהצלחה קורא ל-callback (`java.util.function.Consumer<Employee>`) שה-`MainFrame` מספק.
- **`DashboardPanel`** — מציג שם/תפקיד/סניף של העובד המחובר. אם `role == MANAGER`, מציג גם כפתור "ניהול חשבונות" שקורא ל-`Runnable` callback שעובר למסך האדמין.
- **`AdminPanel`** — טופס יצירת חשבון (ת"ז, שם מלא, שם משתמש, סיסמה, `JComboBox<Role>`, `JComboBox<Branch>` מבוסס `getBranches()`). בשליחה קורא ל-`connection.createAccount(...)`, מציג הצלחה/שגיאה.
- **`MainFrame`** — `JFrame` עם `CardLayout`, מחליף בין שלושת המסכים. הוא זה שמחזיק את ה-`ServerConnection` ומעביר אותה למסכים שצריכים אותה.
- **`Main`** — נקודת כניסה: `new ServerConnection("localhost", 5000)`, בונה ומציג `MainFrame` (על `SwingUtilities.invokeLater`, כמקובל ב-Swing).

**הערה ידועה:** כתובת השרת (`localhost:5000`) קבועה בקוד (hardcoded), לא מוגדרת דרך קובץ קונפיגורציה — סביר לפרויקט קורס שרץ על מכונה אחת; לא נדרש מעבר לזה.

## 6. גבול הבדיקות (JUnit) — החלטה מתועדת

כל **לוגיקה** מקבלת JUnit: `PasswordPolicyTest`, `AccountServiceTest` (הצלחה; דחייה — לא מנהל; דחייה — סיסמה חלשה; דחייה — שם משתמש תפוס), ובדיקת אינטגרציה אחת חדשה שמוכיחה זרימה מלאה מקצה-לקצה: מנהל מתחבר → יוצר חשבון קופאי חדש → הקופאי מתחבר בהצלחה עם הפרטים החדשים (וגם: קופאי שמנסה ליצור חשבון נדחה).

**מסכי Swing עצמם (`LoginPanel`, `DashboardPanel`, `AdminPanel`, `MainFrame`) לא מקבלים בדיקות JUnit.** זו החלטה מודעת, לא פער: בדיקת יוניט לרכיבי Swing (כפתורים, שדות טקסט) דורשת תשתית נוספת (כמו AssertJ-Swing) שהיא Over-engineering ברור לרמת הקורס, וכל הלוגיקה שהמסכים האלה קוראים לה (AccountService, PasswordPolicy, ServerConnection דרך בדיקת האינטגרציה) כבר מכוסה. שווה לדעת להסביר את זה בהגנה אם ישאלו "למה אין בדיקות למסכים?".

## 7. מה בכוונה נשאר מחוץ לשלב 3

- ממשק קנייה/מכירה מלא עם המלאי וה-Customer hierarchy (ר' סעיף 2) — לא נדרש כאן.
- עדכון מלאי בזמן אמת בין כמה עובדים (broadcast מהשרת) — עדיין לא קיים מנגנון "דחיפה" מהשרת ללקוחות; כרגע רק בקשה-תשובה סינכרונית. ייתכן שיתווסף עם הצ'אט בשלב 4 (אותו חיבור Socket כבר פתוח).
- הצ'אט עצמו ותור העובדים הפנויים → שלב 4.
- כל persistence אמיתי → שלב 5 (`EmployeeDirectory` עדיין בזיכרון).

## 8. יציאה (Definition of Done לשלב 3)

- כל המחלקות בסעיף 3 קיימות, מתקמפלות.
- `PasswordPolicyTest`, `AccountServiceTest`, ובדיקת האינטגרציה החדשה עוברים (`mvn test` ירוק), כולל כל הבדיקות הקיימות משלבים 1-2.
- הרצה ידנית: מפעילים `server.Main`, ואז שתי פעמים `client.Main` (שני חלונות) — מתחברים כ-`dana.l` (MANAGER) בחלון אחד ורואים כפתור "ניהול חשבונות"; יוצרים חשבון קופאי חדש דרכו; מתחברים איתו בחלון השני ורואים לוח מידע בלי כפתור ניהול חשבונות. נבדק ידנית פעם אחת, לא חלק מה-CI/JUnit.
