Chat TCP-server og konsolklient

Dette projekt indeholder en simpel ChatServer og ChatClient implementering (issue #4).

Kompilering

Fra projektroden (kræver JDK):

javac -d out src\*.java

Kørsel

Start serveren i én terminal:

java -cp out ChatServer

Start op til tre klienter i hver deres terminal:

java -cp out ChatClient

Brug

Skriv en besked i klientterminals konsol og tryk Enter. Beskeden sendes i formatet TEXT||<tekst> og serveren logger hver modtaget linje sammen med klientens IP:port.

Manuel kontrol (som i issue #4)

1. Start serveren og én klient. Send "Hej". Kontroller, at serveren viser TEXT||Hej.
2. Start tre klienter, hold alle forbindelser åbne. Send en forskellig besked fra hver. Kontroller, at serveren viser alle tre med deres klient-porte.
3. Lad én klient være inaktiv. Send beskeder fra de andre to og kontroller, at serveren modtager dem mens den inaktive forbliver tilsluttet.
