Chat TCP-server og konsolklient

Dette projekt indeholder en simpel ChatServer og ChatClient implementering (issue #4), organiseret i små packages efter ansvar.

Kompilering

Fra projektroden (kræver JDK):

javac -d out $(Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })

Kørsel

Start serveren i én terminal:

java -cp out chat.server.ChatServer

Start op til tre klienter i hver deres terminal:

java -cp out chat.client.ChatClient

Brug

Skriv en besked i klientterminals konsol og tryk Enter. Beskeden sendes i formatet TEXT||<tekst>, og serveren logger hver modtaget linje sammen med klientens IP:port.

Manuel kontrol (som i issue #4)

1. Start serveren og én klient. Send "Hej". Kontroller, at serveren viser TEXT||Hej.
2. Start tre klienter, hold alle forbindelser åbne. Send en forskellig besked fra hver. Kontroller, at serveren viser alle tre med deres klient-porte.
3. Lad én klient være inaktiv. Send beskeder fra de andre to og kontroller, at serveren modtager dem mens den inaktive forbliver tilsluttet.
