Rezultati i rasprava
4.9.1. Korišteni skup podataka
Vrednovanje je provedeno nad povijesnim podatcima dionice tvrtke International Business Machines (oznaka IBM), uvezenima iz datoteke s vrijednostima odvojenima zarezom. Skup obuhvaća 16 273 trgovinska dana, odnosno cjelokupno razdoblje trgovanja tom dionicom od 1962. godine do 4. rujna 2026. Nakon izračuna tehničkih pokazatelja i normalizacije postupkom najmanje i najveće vrijednosti, pri čemu je zabilježena najniža cijena od 2,66729 i najviša cijena od 329,23 dolara, skup je kronološki podijeljen u omjeru 80 prema 20. Skup za učenje obuhvaća 13 018 zapisa, a skup za ispitivanje 3 255 zapisa, što vremenski odgovara razdoblju od 3. listopada 2013. do 4. rujna 2026.
4.9.2. Kvaliteta predikcija
Mjere kvalitete dobivene usporednim treniranjem svih triju modela nad opisanim skupom podataka prikazane su u Tablici 6. Vrijednosti mjera RMSE i MAE [16.1]izražene su u normaliziranoj ljestvici, dakle u rasponu od 0 do 1.
Tablica 6. Mjere kvalitete istreniranih modela

Model	RMSE	MAE	R²	Vrijeme treniranja (ms)
Linearna regresija	0,0095	0,0050	0,9964	937
Višeslojni perceptron	0,1029	0,0497	0,5769	16 796
Slučajna šuma	0,1416	0,0630	0,1989	2 468[17.1]
Izvor: autor

Najbolje rezultate po svim trima mjerama ostvarila je linearna regresija, koja je ujedno bila i najbrža za treniranje. Višeslojni perceptron zahtijevao je gotovo osamnaest puta više vremena, a postigao je osjetno lošije rezultate, dok je slučajna šuma bila najlošija od triju modela. Takav je poredak suprotan uobičajenom očekivanju prema kojemu složeniji modeli bolje opisuju podatke, pa ga je potrebno podrobnije protumačiti.
4.9.3. Rezultati simulacije trgovinske strategije
Simulacija trgovinske strategije provedena je nad ispitnim skupom podataka, koji obuhvaća razdoblje od 3. listopada 2013. do 4. rujna 2026., uz početni ulog od 10 000 dolara i transakcijski trošak od 0,1 % vrijednosti svake transakcije. Uz sva tri modela izračunat je i prinos pasivne strategije držanja pozicije, koja u istome razdoblju iznosi 79,96 %. Rezultati su prikazani u Tablici 7.
Tablica 7. Rezultati simulacije trgovinske strategije

Model	Dobit ($)	Prinos (%)	Udio dobitnih (%)	Najveći pad (%)	Prosj. pogreška
Višeslojni perceptron	30 901,22	309,01	40,7	29,44	16,2332
Linearna regresija	15 804,25	158,04	51,8	44,46	1,6286
Slučajna šuma	1 078,55	10,79	48,4	46,85	20,5638
Držanje pozicije	7 996,00	79,96	–	–	–
Izvor: autor

4.9.4. Rasprava
Usporedba Tablice 6 i Tablice 7 otkriva nalaz koji je za ovaj rad najzanimljiviji: poredak modela po točnosti predikcija gotovo je obrnut od poretka po ostvarenome prinosu. Linearna regresija, koja je po svim trima mjerama kvalitete bila uvjerljivo najbolja i imala prosječnu pogrešku od 1,6286, ostvarila je prinos od 158,04 %. Višeslojni perceptron, čija je prosječna pogreška gotovo deset puta veća i iznosi 16,2332, ostvario je pritom najviši prinos od 309,01 %. Model s najtočnijim predikcijama, dakle, nije ujedno i model koji donosi najveću dobit.
Objašnjenje leži u tome što primijenjena trgovinska strategija ne koristi predviđenu vrijednost cijene izravno, nego samo smjer predviđene promjene u odnosu na cijenu prethodnoga dana. Model može sustavno griješiti u razini cijene, a da pritom smjer promjene pogađa dovoljno često da strategija ostvari dobit. Vrijedi i obrnuto: model može vrlo točno pogađati razinu cijene, a da mu je smjer promjene malo koristan.
Visoka vrijednost koeficijenta determinacije od 0,9964 kod linearne regresije upravo je takav slučaj i ne treba je tumačiti kao dokaz uspješnoga predviđanja. Zaključna se cijena dionice od jednoga do drugoga dana mijenja razmjerno malo u odnosu na svoju apsolutnu razinu, pa model koji jednostavno preslikava jučerašnju cijenu na sutrašnju već postiže vrlo visok koeficijent determinacije. Linearna je regresija najprikladnija za pronalaženje takve gotovo istovjetne veze, zbog čega po mjerama točnosti nadmašuje složenije modele. Naučila je razinu cijene, ali ne i njezinu promjenu, a upravo je promjena ono što je za odlučivanje bitno.
Slabi rezultati slučajne šume potvrđuju to tumačenje s druge strane. Stabla odluke dijele prostor obilježja na područja i unutar svakoga područja predviđaju stalnu vrijednost, pa ne mogu predvidjeti cijenu višu od najviše cijene zabilježene u skupu za učenje. Budući da je skup podijeljen kronološki, a cijena je dionice u ispitnome razdoblju rasla iznad razina iz razdoblja učenja, slučajna šuma sustavno podcjenjuje vrijednosti. Posljedica je najniži koeficijent determinacije od 0,1989 i prinos od svega 10,79 %, znatno ispod pasivne strategije.
Usporedba s pasivnom strategijom držanja pozicije, koja je u istome razdoblju ostvarila 79,96 %, pokazuje da su dva od tri modela tu razinu nadmašila. Taj rezultat ipak traži nekoliko ograda. Višeslojni perceptron ostvario je najviši prinos uz najniži udio dobitnih transakcija od svega 40,7 %, što znači da dobit ne proizlazi iz dosljedno ispravnih odluka, nego iz manjega broja vrlo uspješnih pojedinačnih transakcija. Takav je ishod osjetljiv na slučajnost i nema jamstva da bi se ponovio u drugome razdoblju ili na drugoj dionici. Nadalje, najveći pad vrijednosti portfelja kreće se između 29,44 % i 46,85 %, što znači da je ostvareni prinos pratio vrlo visok rizik koji sam prinos ne prikazuje.
Iz navedenoga proizlazi i metodološka pouka. Sustav u ovome radu automatski odabire najuspješniji model prema vrijednosti korijena srednje kvadratne pogreške, pa je kao najbolji odabrana linearna regresija. Kada bi se kao mjerilo uzeo ostvareni prinos, odabran bi bio višeslojni perceptron. Odabir mjerila prema kojemu se model vrednuje stoga izravno određuje koji će model biti proglašen najboljim, a mjere točnosti predikcije nisu nužno primjereno mjerilo kada je krajnji cilj donošenje trgovinskih odluka.
Dobiveni su rezultati u konačnici u skladu s hipotezom o učinkovitosti tržišta, prema kojoj cijene odražavaju sve javno dostupne informacije, zbog čega se na temelju povijesnih podataka o cijenama ne mogu trajno ostvarivati prinosi viši od tržišnoga prosjeka (Fama, 1970: 383). Ostvareno nadmašivanje pasivne strategije u jednome razdoblju i na jednoj dionici ne predstavlja opovrgavanje te hipoteze, nego rezultat koji bi tek ponovljenim ispitivanjem na više dionica i više razdoblja mogao dobiti širu potvrdu.
Naposljetku, pri tumačenju svih navedenih vrijednosti potrebno je uzeti u obzir da je simulacija pojednostavljena te da ne obuhvaća proklizavanje[18.1] cijene, ograničenja likvidnosti ni porezna davanja. Uključivanje podataka iz šezdesetih i sedamdesetih godina prošloga stoljeća u skup za učenje dodatno je upitno jer se tržišni uvjeti iz toga razdoblja bitno razlikuju od današnjih. 
