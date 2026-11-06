# Sands of Time

Platform 2D in Java + libGDX del **Gruppo Dugongo**.
Un esploratore entra in una piramide e, grazie a un artefatto misterioso, può viaggiare tra **presente** e **passato**: ogni stanza esiste in due epoche, con piattaforme, trappole e uscite diverse.

## Comandi

| Tasto | Azione |
|---|---|
| FRECCE ← → oppure A / D | muoversi |
| SPAZIO, W o FRECCIA SU | saltare (tieni premuto per saltare più in alto) |
| E | interagire (porte, bara, tesoro) |
| T | viaggiare nel tempo (dopo aver trovato l'artefatto) |
| ESC o P | pausa |
| M | audio on/off |
| F11 | schermo intero |

Nei menu: FRECCE SU/GIÙ + INVIO, oppure il mouse.

## Come avviarlo

**IntelliJ (come in laboratorio):** apri la cartella del progetto. Le librerie libGDX vanno messe nella cartella `GdxLibs` *accanto* al progetto (`../GdxLibs`), come prima. Avvia la classe `Launcher` con la cartella del progetto come *working directory*.

**Gradle (qualsiasi PC, scarica libGDX da solo):**

```bash
./gradlew run          # Windows: gradlew.bat run
./gradlew dist         # crea build/libs/SandsOfTime.jar
```

Il jar va avviato dalla cartella del progetto (`java -jar build/libs/SandsOfTime.jar`), perché il gioco legge i file da `assets/`.

## Versione web (browser)

Il gioco si compila anche in JavaScript con [gdx-teavm](https://github.com/xpenatan/gdx-teavm), quindi si gioca nel browser senza installare niente.

Ad ogni push su `main`, **GitHub Actions** (`.github/workflows/build.yml`) fa tre cose:

1. compila il gioco desktop e lo mette tra gli *Artifacts* della run (`SandsOfTime-desktop`, pronto da scaricare);
2. compila la versione web e la pubblica sul ramo **`web`** del repository;
3. la pubblica su **GitHub Pages**, se le Pages sono attivate.

Per provarla sul tuo PC: `./gradlew :web:gdx_teavm_web_js_run` e poi apri http://localhost:8080

**Pubblicarla online**, due possibilità:

- **GitHub Pages:** *Settings → Pages → Build and deployment → Source: GitHub Actions*. Poi rilancia la workflow (*Actions → Build e pubblicazione → Run workflow*). L'indirizzo sarà `https://chrifilvpn.github.io/sands-of-time/`.
- **Render:** crea un **Static Site** (non un Web Service) collegato a questo repository con:
  - Branch: `web`
  - Build Command: `echo ok`
  - Publish Directory: `.`

Nel browser i bottoni "Chiudi gioco" non compaiono, e la musica parte al primo tasto o click (i browser non permettono l'audio prima che l'utente interagisca).

## Struttura

```
src/
  Launcher, GiocoPiramide, BaseGame, BaseScreen   avvio e gestione schermate
  MenuScreen, LevelScreen, FineScreen             menu, livelli, finale
  Esploratore                                     il personaggio
  Oggetto, TilemapActor, BaseActor                oggetti della mappa e attori
  Risorse                                         font, immagini, suoni e musica (caricati una volta)
  ListaBottoni, Particelle                        bottoni da tastiera/mouse, effetti grafici
web/                                              versione browser (TeaVM): WebLauncher + build.gradle
  Save, StatoPartita                              salvataggio automatico e statistiche
assets/
  mappaN_presente.tmx / mappaN_passato.tmx        i livelli (Tiled)
  suoni/                                          effetti sonori
extra/assets_non_usati/                           file non usati dal gioco (bozze, vecchie versioni)
```

## Creare o modificare un livello (Tiled)

Ogni livello è `mappaN_presente.tmx` più, facoltativo, `mappaN_passato.tmx`. Il gioco trova da solo quanti livelli ci sono.

Oggetti (il **nome** dell'oggetto è quello che conta):

- `solid`: muri e pavimenti
- `kill`: trappole
- `finish`: uscita
- `start`: punto di partenza (se manca nell'epoca di partenza, si usa quello dell'altra epoca)
- `artefatto`: oggetto da ispezionare con E per ottenere l'artefatto del tempo

Proprietà della mappa del presente (*Mappa → Proprietà mappa*):

| Proprietà | Valori |
|---|---|
| `dimensionePersonaggio` | grandezza del personaggio in pixel (es. 96 nelle stanze grandi, 57.6 nelle gallerie) |
| `uscita` | `tocco` (si esce toccando `finish`, predefinito), `porta` (si esce premendo E), `passato` (nel presente `finish` è un muro chiuso), `tesoro` (E apre il tesoro e il gioco finisce) |
| `titolo` | nome del livello mostrato all'inizio |

## Novità di questa versione

**Bug corretti**
- Finendo il livello 11 nel passato, il livello 12 partiva "nel passato" senza pavimento e il personaggio cadeva. Ora si parte nel passato solo se il livello ha una mappa del passato.
- "Nuova partita" manteneva l'artefatto e l'epoca della partita precedente.
- Livello 5: il personaggio compariva in un punto sbagliato (mancava lo `start` nel presente).
- Viaggiando nel tempo dentro un muro dell'altra epoca si restava incastrati: ora il viaggio viene bloccato, con un avviso.
- Si poteva saltare all'infinito dentro porte e trappole.
- Saltando contro un soffitto il personaggio restava "incollato".
- L'ingranaggio della pausa poteva non rispondere ai click, perché era coperto da un livello trasparente.
- Con la finestra ridimensionata, l'interfaccia finiva fuori posto.
- Lo sfondo del menu era deformato.

**Prestazioni**
- Prima texture, font, mappe e musica venivano ricreati a ogni livello e a ogni "Riprova" senza mai essere liberati, quindi la memoria cresceva. Ora le risorse comuni si caricano una volta sola e quelle del livello vengono liberate quando lo si lascia.
- Gli sprite del personaggio sono passati da 1024×1024 a 256×256: circa 16 volte meno memoria, nessuna differenza visibile.

**Gameplay e grafica**
- Salto più piacevole: altezza variabile, *coyote time* e *jump buffer*.
- Hitbox del personaggio corretta: i piedi toccano davvero il pavimento.
- Animazione di morte, tremolio dello schermo e lampi colorati nel viaggio nel tempo.
- Particelle: sabbia nell'aria (dorata nel presente, azzurra nel passato), polvere all'atterraggio, scintille sull'uscita e sull'artefatto.
- Telecamera morbida.
- Titolo all'inizio di ogni livello, HUD con livello ed epoca, suggerimenti sui comandi.
- Effetti sonori (salto, morte, viaggio nel tempo, artefatto, porta, menu, vittoria).
- Una sola colonna sonora (la musica del passato) per tutto il gioco: non cambia con il viaggio nel tempo, continua da un livello all'altro e si ferma in pausa.
- Menu e pausa usabili da tastiera; "Continua" disattivato se non c'è un salvataggio. Il menu di pausa contiene Riprendi, Ricomincia livello, Menu e Chiudi, più l'elenco dei comandi.
- Schermata finale con tempo di gioco, numero di morti e crediti.
- Il gioco va in pausa da solo se si riduce a icona la finestra.

**Codice**
- Le impostazioni dei livelli sono nelle mappe (proprietà Tiled), non più in tanti `if (levelNumber == ...)`.
- Il codice duplicato per leggere gli oggetti dalla mappa (copiato 6 volte) ora è un solo metodo: `Oggetto.daMappa`.
- Compatibile sia con le versioni vecchie sia con quelle nuove di libGDX.
- File rinominati (niente più "ChatGPT Image 12 mag 2026…"), tileset rotti rimossi e file inutilizzati spostati in `extra/`.
