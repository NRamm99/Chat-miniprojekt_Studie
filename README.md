# Chat-miniprojekt

Et konsolbaseret chatprogram i Java, hvor flere klienter kommunikerer gennem en fælles TCP-server.

Programmet understøtter unikke brugernavne, chatrum, broadcast og private beskeder. Serveren har to faste rum: `lobby` og `room67`.

## Start server og klient

### Forudsætninger

- Java JDK skal være installeret.
- Projektet skal have en JDK valgt som Project SDK i IntelliJ.

### Start gennem IntelliJ

1. Åbn projektet i IntelliJ.
2. Kør `main()` i `chat.server.ChatServer`.
3. Kør `main()` i `chat.client.ChatClient`.
4. Start yderligere klientinstanser for at forbinde flere brugere. Aktivér **Allow multiple instances** i klientens run configuration, hvis det er nødvendigt.
5. Vælg et forskelligt brugernavn i hver klient.

Serveren lytter på port `5001`. Klienten forbinder som standard til `localhost`, så server og klient kører på samme computer.

### Forbind fra en anden computer

Begge computere skal kunne nå hinanden på netværket, eksempelvis gennem samme router.

1. Start serveren på den ene computer.
2. Find servercomputerens lokale IPv4-adresse under computerens netværksindstillinger.
3. Ret `HOST` i `ChatClient` på den anden computer til serverens IP-adresse.
4. Behold port `5001`, og start klienten igen.

Servercomputerens firewall skal tillade indgående TCP-forbindelser på port `5001`.

## Brug af programmet

Når et brugernavn accepteres, bliver klienten automatisk medlem af `lobby`. Et optaget navn afvises, og brugeren kan vælge et andet på samme forbindelse.

Almindelig tekst sendes til alle brugere i det aktuelle rum, inklusive afsenderen.

| Input | Funktion |
|---|---|
| Almindelig tekst efterfulgt af Enter | Sender en besked til det aktuelle rum. |
| `/join room67` | Skifter til `room67`. |
| `/join lobby` | Skifter tilbage til `lobby`. |
| `/msg Alice Hej Alice` | Sender en privat besked til Alice, uanset hendes chatrum. |
| `/help` | Viser de tilgængelige kommandoer. |
| `/quit` | Lukker forbindelsen og afslutter klienten. |

Brugernavne sammenlignes med forskel på store og små bogstaver. `Bob` og `bob` er derfor forskellige navne. Brugernavne må ikke være tomme eller indeholde `|`.

Brug navne uden mellemrum, så de kan angives som modtagere i `/msg`-kommandoen.

## Beskedprotokol

Klient og server kommunikerer med tekstlinjer. Hver besked afsluttes med et linjeskift, og felterne adskilles med `|`.

### Fra klient til server

Format:

```text
TYPE|TARGET|PAYLOAD
```

- `TYPE` angiver handlingen.
- `TARGET` angiver et rum eller en modtager. Feltet er tomt, når handlingen ikke kræver et mål.
- `PAYLOAD` indeholder brugernavnet eller beskedteksten.

Eksempler:

```text
LOGIN||Bob
JOIN_ROOM|room67|
TEXT|room67|Hej alle
PRIVATE|Alice|Hej Alice
QUIT||
```

| Type | Betydning |
|---|---|
| `LOGIN` | Vælger et brugernavn til forbindelsen. |
| `JOIN_ROOM` | Anmoder om at skifte til et eksisterende rum. |
| `TEXT` | Sender en besked til klientens aktuelle rum. |
| `PRIVATE` | Sender en besked til én bestemt bruger. |
| `QUIT` | Afslutter forbindelsen. |

`LOGIN` bruges kun til valg af brugernavn. Programmet har ikke brugerkonti eller adgangskoder.

### Fra server til klient

Format:

```text
TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD
```

Serveren fastsætter tidspunktet og henter afsendernavnet fra den registrerede forbindelse. Tidsformatet er `yyyy-MM-dd HH:mm:ss`.

Eksempler:

```text
2026-09-17 12:00:00|LOGIN|server||Brugernavnet er accepteret: Bob
2026-09-17 12:00:05|JOIN_ROOM|server|room67|Du er nu i rum room67
2026-09-17 12:00:10|TEXT|Bob|room67|Hej alle
2026-09-17 12:00:15|PRIVATE|Bob|Alice|Hej Alice
2026-09-17 12:00:20|ERROR|server||Brugernavnet er optaget
```

| Type | Betydning |
|---|---|
| `LOGIN` | Bekræfter det accepterede brugernavn. |
| `JOIN_ROOM` | Bekræfter rumskiftet og angiver det nye rum i TARGET. |
| `TEXT` | Leverer en chatbesked til brugerne i rummet. |
| `PRIVATE` | Leverer en privat besked til den angivne modtager. |
| `ERROR` | Forklarer, hvorfor en besked eller handling blev afvist. |

Ved `ERROR` er TARGET brugerens registrerede navn eller tomt, hvis forbindelsen endnu ikke har fået et navn.

Tekstfelter må indeholde `|`. Serveren bruger højst tre felter ved parsing af `TEXT` og `PRIVATE`, mens klienten opdeler serverbeskeder i højst fem felter. Det bevarer resten som beskedtekst.

Fejlformaterede beskeder afvises med `ERROR`, og forbindelsen holdes åben, så klienten kan sende en ny besked.

## Diagrammer

### Simpelt klassediagram

<img width="8192" height="5616" alt="Chat Application Message-2026-09-17-202650" src="https://github.com/user-attachments/assets/cf02eb77-ba97-4174-a5c9-89d4165482a2" />

### Fuldt klassediagram

<img width="8192" height="5470" alt="Chat Application Message-2026-09-17-194844" src="https://github.com/user-attachments/assets/1cc770a0-dff6-4d77-89bf-9e6c91c085de" />

### Sekvensdiagram — optaget brugernavn

<img width="7252" height="5720" alt="Chat Application Message-2026-09-17-194943" src="https://github.com/user-attachments/assets/8d938d2a-3298-42e5-a40a-d8a7d47be114" />

## Trådmodel og delte ressourcer

**Server:** Hovedtråden accepterer forbindelser med `ServerSocket.accept()`. Hver klient får sin egen `Socket` og `ClientHandler`, som køres af en `ExecutorService` med 3 arbejdstråde.

**Klient:** Main-tråden læser tastaturinput og sender beskeder. En separat modtagertråd lytter på serveren og viser beskeder.

**Delte ressourcer:** Klienthandlerne deler brugerregisteret og rumsamlingen, som holder styr på brugernavne, forbindelser og rummenes medlemmer.

## Testresultater
Alle vores test bestod, der er et scenarie vi ikke har testet, fordi vi mangler en udvidelse af programmet. Så om udvidelsen af programmet fungerer som beskrevet er ikke testet!

<img width="777" height="204" alt="billede" src="https://github.com/user-attachments/assets/f321cda7-1d8a-4ec3-8a0e-c79c207769e4" />


## AI-dokumentation
Under refactoring afviste vi flere gange dens forslag, fordi den ikke overholdte vores Agent_Instructions
<img width="417" height="58" alt="AI1" src="https://github.com/user-attachments/assets/dace25af-1c09-4282-b53e-71a52b0a0e01" />
<img width="412" height="39" alt="AI2" src="https://github.com/user-attachments/assets/6aa59b64-727e-4090-abaa-cc5debfbaa54" />


## Valgt udvidelse

Udvidelsen er endnu ikke implementeret.
