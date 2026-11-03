# Opravy modů pro Forge 1.7.10

Tři malé samostatné mody, které mění chování cizích modů bez zásahu do jejich jarů. Originály zůstávají
beze změny, jary z téhle složky se jen přidají vedle nich do `mods`. Hráči nemusí nic instalovat.

| Jar | Co dělá | Config |
|---|---|---|
| [`dist/AetherVoid-1.0.0.jar`](dist/AetherVoid-1.0.0.jar) | Pád z Aetheru skončí ve voidu místo v Overworldu | `config/aethervoid.cfg` |
| [`dist/PlasticOreToggle-1.0.0.jar`](dist/PlasticOreToggle-1.0.0.jar) | Vypne generaci Plastic Ore z MedicineCraftu | `config/plasticoretoggle.cfg` |
| [`dist/DedicatedServerFix-1.0.0.jar`](dist/DedicatedServerFix-1.0.0.jar) | Opraví pády dedikovaného serveru kvůli Glacii a dalším modům | žádný |

Každý funguje sám. Pro singleplayer patří Aether Void a Plastic Ore Toggle do `mods` v klientovi, Dedicated Server Fix
patří jen na dedikovaný server.

## Aether Void

Pro The Aether `aether-1.7.10-v1.1.2.5.jar`. Originál přesouvá do Overworldu všechno, co spadne pod ostrovy.
S tímhle modem pod Aetherem žádný přesun není a platí vanilla pravidlo: pod `y=-64` zabíjí void.

- Hráč zemře („fell out of the world“) a respawnuje se jako po každé jiné smrti. Předměty propadnou do voidu.
- Totéž platí pro moby a pro mount i s jezdcem.
- Portály fungují dál oběma směry. ID dimenze Aetheru nehraje roli, bere se z configu Aetheru.

```
B:playersFallIntoVoid=true    # false = hráče to přenese do Overworldu jako v originále
B:mobsFallIntoVoid=true       # false = moby a mounty to přenese do Overworldu jako v originále
```

Změna configu se projeví po restartu.

Mod upravuje při načítání tři konkrétní podmínky v kódu Aetheru (`PlayerAether.onUpdate`, `EntityHook.onUpdate`,
`AetherEntityEvents.entityUpdateEvents`). Kdyby jiná verze Aetheru měla kód jinak, mod ji nechá být a zapíše
do logu `Expected one 'posY vs ...' check`.

## Plastic Ore Toggle

Pro MedicineCraft `MedicineCraft-V4.2-1.7.10.jar` (`MedicineCraft-Mod.jar` je bajtově stejný soubor).

```
B:generatePlasticOre=false    # true = ruda se generuje jako v originále
```

**Mod ve výchozím nastavení rudu vypíná**, protože kvůli tomu se instaluje. Platí jen pro nově vznikající chunky:
ruda v už vygenerovaném světě zůstane. Blok, předmět i recepty MedicineCraftu dál existují. V logu serveru musí
být `Plastic Ore generation is off`. Změna configu se projeví po restartu.

## Dedicated Server Fix

Vanilla Minecraft má některé metody jen v klientovi. Když je mod volá i z kódu, který běží na serveru, v singleplayeru
a na LAN to projde, protože tam klientský kód je. Teprve dedikovaný server spadne na `NoSuchMethodError`. Proto modpack
vyzkoušený na LAN může na skutečném serveru padat. Tenhle mod chybějící metody na dedikovaném serveru doplní:

| Metoda | Kdo ji volá |
|---|---|
| `Block.isNormalCube` (`func_149637_q`) | Glacia: krystaly, portál, dveře |
| `EntityPlayer.getItemInUseDuration` (`func_71057_bx`) | Betweenlands: `EntityVolatileSoul` (duše Wighta) |
| `EntityLivingBase.getSwingProgress` (`func_70678_g`) | Betweenlands: elixír Swiftarm |
| `FoodStats.setFoodLevel` (`func_75114_a`) | Betweenlands: elixír Starvation |
| `WorldProvider.getCloudHeight` (`func_76571_f`) | Twilight Forest: Magic Beans |

Pád serveru je otestovaný u Glacie, kde přijde pár sekund po vstupu do dimenze. U ostatních je volání vyčtené z jarů
těch modů. Navíc mod zaregistruje dva tile entity Glacie, které Glacia registruje jen v klientovi. Bez toho server při každém ukládání
světa píše do logu tisíce řádků `is missing a mapping`. Na klientovi mod nedělá nic a klient ho mít nemusí.

## Otestováno

Na skutečném dedikovaném serveru Forge 1.7.10-10.13.4.1614 s originálními jary obou modů. Hráče představovali headless
klienti přes síťový protokol 1.7.10.

**Aether Void** ([`test/aether-void.js`](test/aether-void.js)):

| | Bez modu | S modem | S modem, config `false` |
|---|---|---|---|
| Hráč spadne pod `y=-12` | přesun do Overworldu živý | zůstane v Aetheru, pod `y=-64` zemře, respawn v Overworldu | jako bez modu |
| Prase spadne | přesun do Overworldu | zemře ve voidu | jako bez modu |
| Předmět spadne | zničí ho void | zničí ho void | jako bez modu |
| Jezdec na praseti | netestováno | oba padají, jezdec zemře, respawn v Overworldu | netestováno |
| Portál oběma směry | funguje | funguje | funguje |

**Plastic Ore Toggle** ([`test/plastic-ore.js`](test/plastic-ore.js)), stejný seed a 289 chunků:

| | Plastic Ore | Železná ruda |
|---|---|---|
| Bez modu | 11 819 | 27 547 |
| S modem, výchozí config | **0** | 27 551 |
| S modem, `generatePlasticOre=true` | 11 819 | 27 547 |

**Dedicated Server Fix** ([`test/server-fix.js`](test/server-fix.js)):

| | Výsledek |
|---|---|
| Bez modu | Metoda chybí, vstup do Glacie shodí server do 3 s |
| S modem, všech 5 metod zavolaných přímo | Vrací správné hodnoty, kámen je plná kostka a sklo ne |
| S modem, 60 s v Glacii a nové přihlášení přímo v ní | Server běží |
| S modem, uložení světa | V logu není `missing a mapping` |
| S modem, položení dveří Glacie a 30 000 vynucených ticků portálu na obtížnosti 2 | Server běží |

Všechny tři mody běžely společně s Dimension Control 1.2.0, HQM The Journey, BetterQuesting a ostatními mody na jednom
dedikovaném serveru při průchodu všemi 12 portály.

**Netestováno:** singleplayer a skutečný herní klient. Testy běžely proti serveru, takže chybí kontrola
na obrazovce hráče. Jezdec na mountu se v testu netýkal aetherských mountů (Moa, Phyg), ale prasete, které jde
stejnou cestou v kódu. U Betweenlands a Twilight Forest test volal doplněné metody přímo, nespouštěl samotné elixíry,
Wight ani fazole.

## Sestavení a testy

Každý mod je samostatný Gradle projekt. Potřeba je JDK 21 (spouští Gradle) a JDK 8 (kompilace, Gradle si ho
případně stáhne sám).

```bash
cd aether-void && JAVA_HOME=<cesta k JDK 21> ./gradlew build
```

Testy jsou ve složce `test/`, postup je v hlavičce každého skriptu. `testhelper/` je pomocný mod jen pro testy
a na ostrý server nepatří.
