Chat TCP-server og konsolklient

Dette projekt indeholder en simpel ChatServer og ChatClient implementering, organiseret i små packages efter ansvar.

Kompilering

Fra projektroden (kræver JDK):

javac -d out $(Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })

Kørsel

Start serveren i én terminal:

java -cp out chat.server.ChatServer

Start op til tre klienter i hver deres terminal:

java -cp out chat.client.ChatClient

Brug

Når en klient opretter forbindelse, bliver brugeren bedt om at vælge et brugernavn. Brugernavnet sendes til serveren i formatet LOGIN||<brugernavn>.

Serveren registrerer navnet atomisk i et delt, trådsikkert register. Hvis navnet allerede er optaget, afvises forsøget uden at ændre den eksisterende registrering. Serveren sender derefter en fejlmeddelelse på serverformatet og klienten lader brugeren vælge et nyt navn på samme forbindelse.

Efter et accepteret login kan klienten skrive tekst i konsollen. Hver linje sendes i formatet TEXT||<tekst>. Serveren logger hver modtaget besked sammen med klientens IP:port og den registrerede brugernavn.

Beskedformater

- LOGIN||<brugernavn> – klienten sender et ønsket brugernavn til serveren.
- TIMESTAMP|LOGIN|server||Brugernavnet er accepteret: <brugernavn> – serveren bekræfter et gyldigt login.
- TIMESTAMP|ERROR|server||Brugernavnet er optaget – serveren afviser et allerede optaget brugernavn.
- TEXT||<tekst> – klienten sender en chatbesked til serveren.
- TIMESTAMP|TEXT|<brugernavn>||<tekst> – serverens format for videreformidlede beskeder (bruges i senere issues).

Manuel kontrol (issue #9 og #10)

1. Start serveren og én klient. Vælg brugernavnet "Bob". Kontroller, at serveren viser, at forbindelsen er registreret med brugernavnet "Bob" og at klienten får en loginbekræftelse.
2. Send "Hej" fra klienten. Kontroller, at serveren logger den modtagne tekst sammen med "Bob" som afsender.
3. Start en anden klient og vælg samme brugernavn "Bob". Kontroller, at serveren sender `TIMESTAMP|ERROR|server||Brugernavnet er optaget`, og at den anden klient kan vælge et nyt navn uden at lukke forbindelsen.
4. Vælg "Alice" på den anden klients eksisterende forbindelse. Kontroller, at navnet accepteres og at både klienter kan fortsætte med chatinput.
5. Start yderligere klienter med forskellige brugernavne og send beskeder fra hver. Kontroller, at serveren viser det registrerede brugernavn for hver klient i stedet for kun socket-adressen.
