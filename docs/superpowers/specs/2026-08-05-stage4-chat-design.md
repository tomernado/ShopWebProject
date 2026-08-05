# Spec — שלב 4: מערכת תקשורת וצ'אט (Concurrency & Design Patterns)

תאריך: 2026-08-05
תת-פרויקט מתוך: [2026-08-05-store-chain-system-overview-design.md](./2026-08-05-store-chain-system-overview-design.md)
תלוי ב-: שלב 1 (Entities), שלב 2 (Server) — שניהם ממוזגים ב-`master`. **לא** תלוי בשלב 3 (GUI) — ראו סעיף 2.

## 1. מטרת השלב

לבנות את מנגנון הצ'אט בין עובדים: תור לחיפוש עובד פנוי מסניף אחר, שמירת בקשות שיחה בצד שרת כשאין עובד פנוי (עם התראה כשמישהו מתפנה), הצטרפות מנהל משמרת לשיחה קיימת, ושימוש מפורש ב-Design Pattern (Singleton) לניהול התור.

## 2. היקף — ללא מסך צ'אט ב-GUI

בהתאם להחלטה מפורשת: שלב זה נשאר **ברמת שרת/פרוטוקול בלבד**, בדיוק כמו ששלב 2 (Server) נבנה ונבדק לגמרי בלי GUI. הסיבה אינה רק נוחות: כדי לבנות מסך צ'אט אינטראקטיבי אמיתי, `client.ServerConnection` (שנבנה בשלב 3 כקריאה סינכרונית "שלח-וחכה-לתשובה") יצטרך Thread קורא ברקע + מנגנון תיאום בין תשובות סינכרוניות להודעות Push א-סינכרוניות — תשתית משמעותית שלא שייכת לשלב הזה. שלב 4 מוכיח את הלוגיקה והפרוטוקול במלואם דרך בדיקות (יחידה + אינטגרציה עם Socket גולמי), ומסך צ'אט אמיתי יכול להתווסף כהרחבה עתידית כשיהיה צורך אמיתי בו.

## 3. מבנה חבילות (packages) — `chat` חדש

```
src/main/java/chat/
  ChatParticipant.java      (ממשק — send() + גישה ל-Employee)
  ChatSession.java
  ChatRequestOutcome.java   (ממשק סמן — ChatQueued / ChatStarted)
  ChatRequest.java          (Serializable, בקשה — ללא שדות)
  ChatQueued.java           (Serializable, ack — ללא שדות)
  ChatStarted.java          (Serializable — sessionId + Employee peer)
  ChatDispatcher.java       (Singleton — הליבה)
  JoinChatRequest.java
  JoinChatResponse.java
  ParticipantJoined.java
  ChatMessage.java
  ListActiveChatsRequest.java
  ActiveChatsResponse.java
  ChatSummary.java
```

`server.ClientHandler` יתעדכן לממש `chat.ChatParticipant` ולפזר גם הודעות צ'אט (בנוסף ל-`CreateAccountRequest` משלב 3).

## 4. `chat.ChatParticipant` — הממשק שמאפשר בדיקה בלי Socket

```java
public interface ChatParticipant {
    Employee getEmployee();
    void send(Object message);
}
```

`ClientHandler` יממש אותו (`send` כותב ל-`ObjectOutputStream` שלו). בבדיקות (`ChatDispatcherTest`) נשתמש ב-`FakeChatParticipant` — מחלקת עזר פשוטה שרק אוספת הודעות לרשימה, בלי שום Socket. זה בדיוק אותו עיקרון שהפך את `AuthService`/`AccountService` לניתנים לבדיקה — הפרדה בין לוגיקה לתקשורת.

## 5. `chat.ChatDispatcher` — ה-Design Pattern הנדרש: Singleton

```java
public class ChatDispatcher {
    private static final ChatDispatcher INSTANCE = new ChatDispatcher();
    private ChatDispatcher() {}
    public static ChatDispatcher getInstance() { return INSTANCE; }
    ...
}
```

**למה Singleton, ולמה זו הבחירה הפשוטה והמדויקת כאן (לא "תבנית בשביל תבנית"):** יש בפועל **מופע אחד** רצוי של מנהל-התור בכל השרת — כל ה-Threads (`ClientHandler` אחד לכל לקוח מחובר) צריכים לגשת לאותו תור-המתנה ולאותה מפת שיחות פעילות. Singleton הוא הדרך הכי פשוטה והכי מוכרת בקורס לבטא "יש בדיוק מופע אחד, וכולם ניגשים אליו דרך נקודת גישה גלובלית" — בלי להעביר הפניה (reference) דרך כל בנאי בשרשרת.

**Thread-safety:** כל מתודה ציבורית ב-`ChatDispatcher` היא `synchronized` — כלל אחד פשוט: "כל פעולה שנוגעת בתור או במפת השיחות נעולה על ה-Singleton עצמו, כך שרק Thread אחד בכל רגע נתון יכול לגעת בהם." זה מספיק ופשוט לרמת קורס; אין צורך במבני concurrency מתקדמים יותר (כמו נעילות נפרדות לכל מבנה נתונים) בהיקף הזה.

### 5.1 `requestChat(ChatParticipant requester)` → `ChatRequestOutcome`

1. מחפש בתור המתנה עובד ראשון מ**סניף שונה** מזה של המבקש.
2. **נמצא:** מוציא אותו מהתור, יוצר `ChatSession` חדש, ושולח `ChatStarted` **לשניהם**: לעובד שכבר חיכה — כ-Push לא-מבוקש (זו בדיוק ה"הקפצת התראה כשהמשתמש מתפנה" מהדרישה!); למבקש הנוכחי — כתשובה הסינכרונית הישירה לבקשה שלו.
3. **לא נמצא:** מוסיף את המבקש לתור ההמתנה (זו ה"שמירת בקשות שיחה בצד שרת אם אין עובד פנוי"), מחזיר `ChatQueued`.

**נקודה חשובה להגנה:** התור משמש **פעמיים באותו מבנה נתונים** — גם כ"מי מחפש שיחה עכשיו" וגם כ"בקשות אופליין שממתינות". זה לא באג/ערבוב, זו הפשטה מכוונת: "בקשת שיחה שלא נענתה מיד" ו"עובד פנוי שממתין לשיחה" הן בפועל אותו מצב בדיוק במערכת הזו.

### 5.2 `joinChat(String sessionId, ChatParticipant participant)` → `JoinChatResponse`

בודק ש-`participant.getEmployee().getRole() == Role.MANAGER` (אחרת כישלון — אותו דפוס בדיוק כמו `AccountService.createAccount`), ושה-session קיים. אם תקין: מוסיף למשתתפי ה-session, שולח `ParticipantJoined` לשאר המשתתפים (Push).

### 5.3 `sendMessage(String sessionId, ChatParticipant sender, String text)` — ללא תשובה סינכרונית

בונה `ChatMessage` חדש עם השולח **האמיתי** (מה שהשרת יודע על החיבור, לא מה שהלקוח טוען) ומשדר לכל שאר משתתפי ה-session (לא לשולח עצמו). אם ה-sessionId לא קיים — מתעלם בשקט (לא נדרשת התרעת שגיאה חזרה; הפשטה מתועדת).

### 5.4 `listActiveChats()` → `List<ChatSummary>`

הקרנה (projection) פשוטה של השיחות הפעילות — מזהה session + שמות משתתפים בלבד, לא חושף אובייקטי `ChatParticipant`/`ChatSession` פנימיים על הרשת.

### 5.5 `resetForTests()`

מנקה את התור ומפת השיחות. **קיים אך ורק לבדיקות** — כיוון ש-`ChatDispatcher` הוא Singleton אמיתי, המצב שלו נשאר בין הרצות בדיקה בתוך אותה הרצת JUnit, אלא אם מנקים אותו במפורש. נקרא מ-`@BeforeEach` בבדיקות. זה טריק בדיקה מוכר ומקובל לבדיקת Singletons — לא סותר את התבנית עצמה.

## 6. `server.ClientHandler` — שינוי מבני

`out`, `in`, וה-`Employee` המחובר הופכים משדות מקומיים בתוך `run()` לשדות של המחלקה, כי `send(Object)` (מ-`ChatParticipant`) צריך להיקרא גם **מ-Threads אחרים** (כשמשתתף אחר בצ'אט שולח הודעה, ה-Thread שלו קורא ל-`send` על ה-`ClientHandler` הזה). `send` הופך ל-`synchronized` כדי שכתיבות מקבילות ל-Socket לא יתערבבו.

לולאת ההודעות המאומתת (אחרי login מוצלח) גדלה לזהות עוד סוגי הודעה: `ChatRequest`, `JoinChatRequest`, `ChatMessage`, `ListActiveChatsRequest` — באותו סגנון `instanceof` שכבר קיים מ-`CreateAccountRequest` (שלב 3). זו לא תבנית Command/Visitor — `if/else if` פשוט, בכוונה.

## 7. מה בכוונה נשאר מחוץ לשלב 4

- מסך צ'אט ב-Swing (ר' סעיף 2).
- הרחבת `client.ServerConnection` לתמיכה ב-Push א-סינכרוני — יידרש רק כשיהיה מסך צ'אט אמיתי.
- שמירת **תוכן** השיחות ללוגים/היסטוריה קבועה → שלב 5 (המסמך-על מציין זאת במפורש כאופציה בדוחות/לוגים).
- מנגנון למניעת בקשת-צ'אט כפולה מאותו עובד בזמן שהוא כבר בתור (edge case נדיר, לא נדרש במפורש — מוגבל בכוונה).

## 8. יציאה (Definition of Done לשלב 4)

- כל המחלקות בסעיף 3 קיימות, מתקמפלות.
- `ChatDispatcherTest` (לוגיקת ההתאמה, ההצטרפות וההודעות — ללא Socket) ו-`ChatIntegrationTest` (זרימה מלאה עם Socket אמיתי: תור → התאמה → Push, הצטרפות מנהל → Push, הודעה → Push) עוברים, כולל כל הבדיקות הקיימות משלבים 1-3.
