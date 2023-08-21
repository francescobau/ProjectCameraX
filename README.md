# ProjectCameraX

----------
## Lint

L'applicazione "Project CameraX" è stata sviluppata per mostrare una gamma di problemi che Android Lint è in grado di rilevare.

Ci sono 3 branch da visualizzare:
- **develop_lint_KO**, in cui è presente la app "problematica", che presenta vari problemi ed errori, sia fatti di proposito che non, che vengono rilevati da Android Lint.
- **develop_lint_OK**, in cui è presente la app "corretta", in cui sono stati risolti i problemi rilevati da Android Lint, aggiunti i JavaDocs e la traduzione in italiano.
- **develop**, in cui si può vedere lo stato della app prima di essere "corretta" nel branch "develop_lint_OK".

Per generare un report in Lint tramite linea di comando, si può usare il Terminale in Android Studio e digitare il comando: **.\gradlew lint**.
Non è possibile effettuarlo nel branch **develop_lint_KO**, perché presenta errori che impediscono la build della app, quindi è raccomandato usare lo strumento in Android Studio: Code -> Inspect Code.

----------
## Descrizione del progetto

Project CameraX offre all'utente la possibilità di scattare foto e catturare video, sia da fotocamera anteriore sia da fotocamera posteriore, salvando in memoria non volatile, e dà anche la possibilità di vedere la lista dei media salvati, gestita tramite RecyclerView, con possibilità di cancellare ciascun media: bisogna tenere premuto l'elemento in lista per far comparire la "X" che, quando cliccata, cancella l'elemento dalla lista e dalla memoria.

Grazie all'aiuto di Android Lint, nel branch **develop_lint_OK** si ha potuto realizzare, senza problemi, la traduzione in italiano dell'interfaccia grafica.

----------
## Note sul progetto

- L'app è stata sviluppata utilizzando l'emulatore Pixel XL, API 30 (Android 11), e anche sullo smartphone del sottoscritto, un Redmi Note 8 Pro, anch'esso Android 11.
- Come ambiente di sviluppo si ha utilizzato prevalentemente Android Studio Flamingo 2022.2.1 Patch 2, ma si ha ridotto la versione di Android Gradle Plugin e di Gradle affinché sia compatibile con l'ultima versione di Android Studio Electric Eel.
- Per la corretta esecuzione di Android Lint su terminale, bisogna avere a disposizione il JDK di Java 11.

----------
## Autore
Il progetto è stato sviluppato da Francesco Bau'.
