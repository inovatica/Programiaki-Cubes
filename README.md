# Budowanie

Wymagany jest Maven.<br />
Polecenie dla klocka głównego: mvn clean install<br />
Polecenie dla klocka z instrukcją: mvn clean install -Dcube.command=_instrukcja_ <br />
Dostępne instrukcje: f, r, l<br />

Informacja o instrukcji podanej przy budowaniu, znajduje się w manifeście tworzonego archiwum _jar_.<br />
Budowany plik uruchomieniowy ma nazwę: cubes-_wersja_-jar-with-dependencies.jar

# Uruchamianie

Wymagana jest java w wersji 8 z zainstalowaną biblioteką librxtxSerial.so dla ARM (w _/jre/lib/arm_) oraz plikiem RXTXcomm.jar (w _/jre/lib/ext_).<br />
W domyślnej konfiguracji, aplikacja uruchamia się automatycznie przy starcie systemu operacyjnego i znajduje w _/home/pi/java/_.<br />
Polecenie uruchamiające znajduje się w _/etc/rc.local_: "sudo java -jar _nazwa pliku_", pn.: "sudo java -jar /home/pi/java/cubes-1.0-jar-with-dependencies.jar"<br />
Jeśli aplikacja ma być uruchamiana na Raspberry bez bluetooth, na końcu polecenia należy dopisać "&".

# Komunikacja z robotem

Klocek główny musi zostać jednorazowo sparowany z robotem. Można to wykonać za pomocą narzędzia _bluetoothctl_.<br />
Klocek główny musi mieć zainstalowany _python_ w wersji 3.5 z modułem USB. <br />