📌 Dinner Planner – Domain Model & JPA Backend

Dieses Repository enthält das JPA-basierte Domain-Model des DinnerPlanner-Projekts.
Es stellt alle grundlegenden Entity-Klassen, Validierungen und Strukturen bereit, die für die Planung von Menüs benötigt werden — darunter Gänge, Kategorien, regionale Einordnung, Food-Restrictions und zentrale Angaben wie Personenanzahl oder Anlass. Damit bildet dieses Modul die fachliche Grundlage. Die weiterführenden Systeme (KI-Generierung, externe APIs, automatische Vorschläge) werden im REST-Server umgesetzt.

Das Projekt entstand parallel zur Umschulung zum Fachinformatiker Anwendungsentwicklung und wurde auf eigene Initiative begonnen. 
Es wird gemeinschaftlich im Team weitergeführt.

---

👥 Team & Zusammenarbeit

Das Projekt entsteht gemeinsam mit:

Angela Schlieben

Ali Abukel

Andreas Scherer

Der Fokus liegt auf teamorientierter Softwareentwicklung, sauberer Architektur und praxisnaher Anwendung moderner Java-Technologien.

---

🛠️ Erweiterte Systeme & geplante Architektur

*Die folgenden Erweiterungen sind Teil der Gesamtarchitektur des DinnerPlanner-Systems und werden im REST-Server implementiert. Das Domain-Modell dient dabei als fachliche Basis.*

Das DinnerPlanner-Projekt entwickelt sich perspektivisch zu einem modularen Assistenzsystem, das interne Daten, KI-Modelle und externe APIs kombiniert.
Die folgenden Komponenten gehören zum fest geplanten Funktionsumfang:

1. KI-basierte Rezepterstellung und -optimierung

Rezepte werden mithilfe externer KI-Modelle (z. B. ChatGPT, Gemini) erstellt, überarbeitet oder ergänzt.
Die generierten Inhalte werden anschließend standardisiert, intern verarbeitet und im System gespeichert.

2. Integration externer APIs

Einbindung von Diensten wie Spotify, Wetterdiensten oder weiteren Quellen, um Kontextinformationen (z. B. Stimmung, Wetterlage, Veranstaltungsart) in die Menü- und Eventplanung einzubeziehen.

3. KI-gestützte Auswertung und Vorschläge

Analyse der gespeicherten Daten — inklusive KI-Rezepte, Nutzerpräferenzen und externer Informationen — zur automatischen Generierung von Menüvorschlägen, Pairing-Empfehlungen und Ablaufplänen.

4. Zentrale Datenspeicherung & interne Kommunikation

Alle eingehenden Daten (externe APIs, KI-Antworten, Nutzereingaben) werden serverseitig vereinheitlicht, verarbeitet und persistent abgelegt.

5. Erweiterbares Modul-System

Architektur, die es erlaubt, später weitere Komponenten anzuschließen:
z. B. Kalender-Integration, Musiksteuerung, automatisierte Einkaufslisten, Eventautomatisierung, Getränke-Pairing u. a.

---

🔧 Architektur

Dieses Modul umfasst:

JPA Entities

Validierungslogik (Jakarta Validation)

Hilfsklassen und Utility-Komponenten

vollständiges Datenmodell für die DinnerPlanner-Architektur

Das Modul ist eigenständig, wird aber in der Praxis zusammen mit dem REST-Server betrieben.

---

🔗 Verbindung zum REST-Server

Für den vollständigen Betrieb wird zusätzlich der REST-Server benötigt:

👉 Dinner Planner Server
https://github.com/AndreasScherer508/dinner_planner_server
 (Repository wird separat gepflegt)

Der Server nutzt dieses Model-Modul und stellt HTTP/JSON-Schnittstellen bereit.

---

🧰 Technologien

Java 17

Jakarta Persistence (JPA)

Jakarta Validation

Eclipse-basierte Projektstruktur

---

🎯 Ziel & Kontext

Das Projekt dient der:

Vertiefung objektorientierter Modellierung

Umsetzung professioneller Backend-Architektur

Teamentwicklung und GitHub-basierter Zusammenarbeit

Vorbereitung auf weiterführende Softwareentwicklungsprojekte

Es wird privat und unabhängig vom Unterricht kontinuierlich weiterentwickelt.

---

👤 Autor

Andreas Scherer (2025)
Fachinformatiker für Anwendungsentwicklung (in Ausbildung)
