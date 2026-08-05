# Spec — שלב 2: Server Side (Threads + Authentication)

תאריך: 2026-08-05
תת-פרויקט מתוך: [2026-08-05-store-chain-system-overview-design.md](./2026-08-05-store-chain-system-overview-design.md)
תלוי ב-: שלב 1 (Entities) — ממוזג ב-`master`.

## 1. מטרת השלב

לבנות את שרת ה-Socket הבסיסי: קבלת חיבורי לקוחות מרובים, כל חיבור מקבל Thread נפרד, ומימוש אימות משתמשים (Authentication) כולל מניעת התחברות כפולה של אותו משתמש בו-זמנית. **ללא** GUI (שלב 3) ו**ללא** מסך אדמין להגדרת חשבונות/מדיניות סיסמאות (זה מסך — שייך לשלב 3; כאן רק לוגיקת האימות בצד שרת).

תזכורת אילוץ מחייב (מפורט במסמך-העל): קוד פשוט, ניתן להסבר מלא ע"י סטודנט, ללא ספריות רשת/Concurrency חיצוניות — `java.net` + `java.util.concurrent` הסטנדרטיים של Java בלבד.

## 2. מבנה חבילות (packages)

```
src/main/java/
  model/
    Employee.java          (מתעדכן: שדה password נוסף)
    ... (שאר מחלקות שלב 1 — ללא שינוי)
  server/
    LoginRequest.java       (Serializable)
    LoginResponse.java      (Serializable)
    EmployeeDirectory.java  (נתוני seed בזיכרון)
    AuthService.java        (לוגיקת אימות + מניעת כפילות)
    ClientHandler.java      (Runnable — Thread per client)
    Server.java             (start/stop)
    Main.java                (נקודת כניסה בלבד — new Server(...).start())
```

## 3. שינוי ב-`model.Employee`

מוסיפים שדה `password` (String) לקונסטרקטור הקיים. **החלטה מכוונת:** הסיסמה נשמרת ונבדקת כמחרוזת רגילה (ללא hashing/הצפנה). זו הפשטה מודעת התואמת את רמת הקורס — לא תקלה. במערכת אמיתית זה היה נדרש; כאן זה מחוץ לתחום.

```java
public Employee(String idNumber, String fullName, String username, String password, Role role, Branch branch)
```

`EmployeeTest` הקיים (משלב 1) יתעדכן בהתאם (פרמטר נוסף).

## 4. `server.EmployeeDirectory` — נתוני Seed

מחלקה פשוטה שמחזיקה `Map<String, Employee>` (username → Employee) עם כמה עובדים קבועים מוגדרים מראש בקוד (לא קובץ, לא DB — זה מגיע בשלב 5). מספקת `findByUsername(String)`.

## 5. `server.AuthService` — לוגיקת האימות (הליבה הניתנת לבדיקה)

**ללא Socket כלל** — מחלקה רגילה שניתן לבדוק ב-JUnit ישירות.

- `LoginResponse login(String username, String password)`:
  1. מחפש עובד לפי username ב-`EmployeeDirectory`. לא נמצא → תשובת כישלון.
  2. משווה סיסמה (`.equals`). לא תואם → תשובת כישלון.
  3. בודק אם ה-username כבר ברשימת המחוברים כרגע (`activeSessions`). אם כן → תשובת כישלון ("המשתמש כבר מחובר ממקום אחר").
  4. אחרת: מוסיף ל-`activeSessions`, מחזיר תשובת הצלחה עם פרטי העובד (ללא הסיסמה).
- `void logout(String username)`: מסיר מ-`activeSessions`.

`activeSessions` הוא `ConcurrentHashMap.newKeySet()` — קבוצה בטוחה לגישה בו-זמנית ממספר Threads (כל חיבור לקוח רץ ב-Thread נפרד ויכול לקרוא ל-`login`/`logout` באותו הזמן).

**מגבלה ידועה, מתועדת בכוונה:** אם לקוח מתנתק בצורה לא תקינה (קריסה, ניתוק רשת) בלי לקרוא ל-`logout`, ה-username יישאר "תפוס" עד שהחיבור בפועל נסגר וה-`ClientHandler` מזהה זאת (ר' סעיף 7). אין מנגנון heartbeat/timeout בשלב זה — מעבר לנדרש.

## 6. פרוטוקול תקשורת — `LoginRequest` / `LoginResponse`

שני POJOs פשוטים, `Serializable`, נשלחים דרך `ObjectOutputStream`/`ObjectInputStream` על ה-Socket. `Employee` ו-`Branch` צריכים גם הם להיות `Serializable` כדי שיהיה אפשר להחזיר אותם בתוך `LoginResponse`.

```java
public class LoginRequest implements Serializable {
    private final String username;
    private final String password;
    // constructor + getters
}

public class LoginResponse implements Serializable {
    private final boolean success;
    private final String errorMessage;   // null אם success
    private final Employee employee;     // null אם !success
    // constructor + getters
}
```

זהו אותו פרוטוקול-בסיס (חיבור פתוח + אובייקטים מסודרים) שעליו ייבנה הצ'אט בשלב 4 — שם יתווספו סוגי הודעות נוספים על אותו חיבור, לא פרוטוקול חדש.

## 7. `server.ClientHandler` — Thread per Client

`implements Runnable`. מקבל `Socket` + `AuthService` בקונסטרקטור. בתוך `run()`:
1. פותח `ObjectOutputStream`/`ObjectInputStream` על ה-Socket.
2. קורא `LoginRequest`, מעביר ל-`authService.login(...)`, שולח בחזרה `LoginResponse`.
3. אם ההתחברות הצליחה: ממתין בלולאה לקריאת הודעות נוספות (מוכן להרחבה בשלב 4 — כרגע רק מזהה ניתוק).
4. בכל מקרה של ניתוק (`IOException`/EOF) או הודעת LOGOUT מפורשת: קורא ל-`authService.logout(username)` ב-`finally`, וסוגר את ה-Socket.

## 8. `server.Server` — קבלת חיבורים

**לא** `main()` גולמי — מחלקה עם `start()`/`stop()` כדי שאפשר יהיה להריץ אותה בבדיקה על Thread נפרד ולסגור אותה בסוף.

```java
public class Server {
    public Server(int port, AuthService authService) { ... }
    public void start() { ... }  // ServerSocket.accept() בלולאה, new Thread(new ClientHandler(...)).start() לכל חיבור
    public void stop() { ... }   // סוגר את ה-ServerSocket, עוצר את לולאת ה-accept
}
```

`Main.java` הוא רק: `new Server(5000, new AuthService(new EmployeeDirectory())).start();` — לא נבדק ב-JUnit (נקודת כניסה בלבד).

## 9. בדיקות (JUnit)

- **`AuthServiceTest`** (עיקרי, ללא Socket כלל):
  - התחברות מוצלחת עם משתמש/סיסמה נכונים.
  - כישלון: סיסמה שגויה.
  - כישלון: משתמש לא קיים.
  - כישלון: אותו משתמש מנסה להתחבר פעמיים בו-זמנית (לפני logout).
  - הצלחה: אחרי `logout`, אותו משתמש יכול להתחבר שוב.
- **`ServerIntegrationTest`** (בדיקה אחת, קצה-לקצה עם Socket אמיתי):
  - מריץ `Server` על Thread נפרד ב-port בדיקה, פותח `Socket` לקוח אמיתי, שולח `LoginRequest`, מוודא שמתקבל `LoginResponse` תקין, סוגר את השרת.

## 10. מה בכוונה נשאר מחוץ לשלב 2

- מסך אדמין להגדרת חשבונות/מדיניות סיסמאות (GUI) → שלב 3.
- תצוגת ממשק מותאמת תפקיד/סניף (GUI) → שלב 3.
- הודעות צ'אט בפועל, תור עובדים פנויים → שלב 4 (אך בונה על אותו חיבור Socket + ObjectStream מה-`ClientHandler`).
- עדכון מלאי בזמן אמת בין עובדים → ישולב בשלב 4/3 כשיהיה GUI לצרוך אותו.
- כל persistence אמיתי (קובץ/DB) לעובדים → שלב 5. `EmployeeDirectory` נשאר in-memory עד אז.

## 11. יציאה (Definition of Done לשלב 2)

- כל המחלקות בסעיף 2 קיימות, מתקמפלות.
- `AuthServiceTest` ו-`ServerIntegrationTest` עוברים (`mvn test` ירוק), כולל כל בדיקות שלב 1 שעדיין קיימות.
- אפשר להריץ `Main` ידנית ולראות שרת שמאזין ומקבל חיבור (נבדק ידנית פעם אחת, לא חלק מה-CI/JUnit).
