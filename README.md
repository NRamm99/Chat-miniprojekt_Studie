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

Serveren starter med to faste rum: `lobby` og `room67`. Alle nye klienter bliver placeret i `lobby`, når deres brugernavn accepteres. Et klientforbindelses rumtilhørsforhold håndteres af serveren trådsikkert.

Efter et accepteret login kan klienten skrive tekst i konsollen. Hver linje sendes i formatet TEXT|<aktuelt rum>|<tekst>. Serveren logger hver modtaget besked sammen med klientens IP:port, det registrerede brugernavn og rummets navn.

Brugeren kan skifte til et andet rum ved at skrive `/join <rum>` i konsollen. Kommandoen sender `JOIN_ROOM|<rum>|` til serveren, som validerer at rummet findes, fjerner brugeren fra det nuværende rum, og føjer brugeren til det nye rum.

Beskedformater

- LOGIN||<brugernavn> – klienten sender et ønsket brugernavn til serveren.
- TIMESTAMP|LOGIN|server||Brugernavnet er accepteret: <brugernavn> – serveren bekræfter et gyldigt login.
- TIMESTAMP|ERROR|server||Brugernavnet er optaget – serveren afviser et allerede optaget brugernavn.
- TEXT|<rum>|<tekst> – klienten sender en chatbesked til serveren med det rum, den er registreret i.
- TIMESTAMP|TEXT|<brugernavn>|<rum>|<tekst> – serverens format for videreformidlede beskeder til alle registrerede klienter i samme rum, inklusive afsenderen.
- TIMESTAMP|ERROR|server|<brugernavn>|Beskedens TARGET svarer ikke til dit registrerede rum – serveren afviser et meddelelsesforsøg, hvis klienten sender et andet TARGET end sit eget rum, og broadcaster ikke videre.
- JOIN_ROOM|<rum>| – klienten sender en forespørgsel om at skifte til et eksisterende rum.
- TIMESTAMP|JOIN_ROOM|server|<rum>|Du er nu i rum <rum> – serveren bekræfter et gyldigt rumskift.
- /msg <brugernavn> <tekst> – klientens konsolkommando til at sende en privat besked til en bestemt registreret bruger (brugernavnet kan ikke indeholde mellemrum).
- TIMESTAMP|PRIVATE|<afsender>|<modtager>|<tekst> – serverens format for private beskeder. Modtageren er kun den angivne bruger; beskeden må ikke broadcastes.
- Hvis en privat besked er rettet mod en ikke-eksisterende bruger, sender serveren en TIMESTAMP|ERROR|server|<afsender>|<forklarende tekst> tilbage til afsenderen.

Fejlbeskeder bruger formatet `TIMESTAMP|ERROR|server|<brugernavn>|<fejlbeskrivelse>`. Target er tomt, indtil forbindelsen har fået et brugernavn. Derefter indeholder Target afsenderens registrerede brugernavn. Serveren afviser tomme linjer, ukendte beskedtyper og beskeder med manglende felter, men holder forbindelsen åben, så klienten kan sende næste gyldige besked. `|` i tekstfelter bevares.

Kommandoen `/quit` sender `QUIT||`, flusher linjen og lukker derefter klientens forbindelse. Serveren håndterer både `QUIT||`, EOF og læse- eller skrivefejl ved at fjerne forbindelsen fra brugerregister og rum. En skrivefejl hos én klient stopper ikke broadcast til de øvrige klienter.

Manuel kontrol (issue #16)

1. Forbind Bob, Alice og Charlie. Alle starter i `lobby`.
2. Lad Charlie skrive `/join room67`.
3. Send "Hej fra Bob" fra Bob. Kontroller, at Bob og Alice modtager beskeden, men Charlie ikke gør.
4. Send "Hej fra Charlie" fra Charlie. Kontroller, at kun Charlie modtager beskeden.
5. Lad Alice skifte til `room67` med `/join room67`. Kontroller, at Alice og Charlie nu kan chatte sammen uden Bob.
6. Forsøg at skifte til et rum, der ikke findes, fx `/join nonexistent`. Kontroller fejlbeskeden, og at det nuværende rum bevares.

Manuel kontrol (issue #15)

1. Start serveren. Kontroller, at den opretter de to faste rum `lobby` og `room67`.
2. Start tre klienter med forskellige brugernavne. Kontroller, at alle tre automatisk placeres i `lobby`.
3. Send "Hej fra Alice" fra Alice, derefter "Hej fra Bob" og "Hej fra Charlie". Kontroller, at alle tre klienter modtager beskederne med `lobby` som TARGET, og at hver besked vises som `TIMESTAMP|TEXT|<brugernavn>|lobby|<tekst>`.
4. Test et ugyldigt TARGET ved at forsøge at sende med et forkert rum i teksten. Kontroller, at serveren svarer med `TIMESTAMP|ERROR|server|Bob|Beskedens TARGET svarer ikke til dit registrerede rum` uden at broadcastede beskeden.

Manuel kontrol (issue #11)

1. Start serveren. Start tre klienter med forskellige brugernavne, fx "Alice", "Bob" og "Charlie".
2. Send "Hej fra Alice" fra Alice, derefter "Hej fra Bob" og "Hej fra Charlie". Kontroller, at hver klient modtager hver besked præcis én gang, og at hver meddelelse viser korrekt afsender og tekst.
3. Lad én klient være stille uden konsolinput i et par sekunder. Kontroller, at den stadig modtager beskeder fra de andre klienter.
4. Kontroller, at hver leveret besked vises med `split("\\|", 5)`-parse og formatteres som `TIMESTAMP|TEXT|<brugernavn>|<rum>|<tekst>`.

Manuel kontrol (issue #9 og #10)

1. Start serveren og én klient. Vælg brugernavnet "Bob". Kontroller, at serveren viser, at forbindelsen er registreret med brugernavnet "Bob" og at klienten får en loginbekræftelse.
2. Send "Hej" fra klienten. Kontroller, at serveren logger den modtagne tekst sammen med "Bob" som afsender.
3. Start en anden klient og vælg samme brugernavn "Bob". Kontroller, at serveren sender `TIMESTAMP|ERROR|server||Brugernavnet er optaget`, og at den anden klient kan vælge et nyt navn uden at lukke forbindelsen.
4. Vælg "Alice" på den anden klients eksisterende forbindelse. Kontroller, at navnet accepteres og at begge klienter kan fortsætte med chatinput.
5. Start yderligere klienter med forskellige brugernavne og send beskeder fra hver. Kontroller, at serveren viser det registrerede brugernavn for hver klient i stedet for kun socket-adressen.

Manuel kontrol (arbejdsprocessens trin 5)

1. Forbind Bob, Alice og Charlie. Send fejlformaterede protokollinjer fra en testklient, fx `TEXT` og `UKENDT||Hej`. Kontroller, at serveren svarer med `ERROR`, og send derefter en gyldig besked på samme forbindelse.
2. Lad Bob afslutte med `/quit`. Kontroller, at Alice og Charlie stadig kan chatte, og at en ny klient kan vælge brugernavnet Bob.
3. Stop Alices klientproces uden `/quit`. Kontroller, at serveren fortsætter, og at Alice kan vælge brugernavnet igen, efter afbrydelsen er registreret.
4. Afbryd en klient, mens en anden sender beskeder. Kontroller, at de resterende klienter fortsat modtager beskeder.
5. Stop serveren. Kontroller, at klienterne viser forbindelsesafbrydelsen, og at modtagertrådene stopper uden en gentagen fejlløkke.

Resultat: Socket-kontrollen gennemførte login for Bob, Alice og Charlie, modtog en `ERROR` for `UKENDT||Hej`, leverede efterfølgende en tekst med `|` til alle tre klienter, lukkede Bob med `QUIT||` og accepterede derefter en ny Bob-forbindelse.
