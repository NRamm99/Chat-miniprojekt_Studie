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

Efter login kan klienten skrive tekst i konsollen. Hver linje sendes i formatet TEXT||<tekst>. Serveren logger hver modtaget besked sammen med klientens IP:port og den registrerede brugernavn.

Beskedformater

- LOGIN||<brugernavn> – angiver den klientens valgte brugernavn på den aktuelle forbindelse.
- TEXT||<tekst> – sender en chatbesked til serveren.

Manuel kontrol (issue #9)

1. Start serveren og én klient. Vælg brugernavnet "Bob". Kontroller, at serveren viser, at forbindelsen er registreret med brugernavnet "Bob".
2. Send "Hej" fra klienten. Kontroller, at serveren logger den modtagne tekst sammen med "Bob" som afsender.
3. Start yderligere klienter med forskellige brugernavne og send beskeder fra hver. Kontroller, at serveren viser det registrerede brugernavn for hver klient i stedet for kun socket-adressen.
