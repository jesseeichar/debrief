/* ===============
 * JFreeChart Demo
 * ===============
 *
 * Project Info:  http://www.object-refinery.com/jfreechart/index.html
 * Project Lead:  David Gilbert (david.gilbert@object-refinery.com);
 *
 * (C) Copyright 2000-2002, by Simba Management Limited and Contributors.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 * ------------------
 * DemoResources.java
 * ------------------
 * (C) Copyright 2002, by Simba Management Limited.
 *
 * Original Author:  David Gilbert (for Simba Management Limited);
 * Contributor(s):   -;
 * Polish translation: Krzysztof Pa� (kpaz@samorzad.pw.edu.pl)
 *
 * $Id: DemoResources_pl.java,v 1.1.1.1 2003/07/17 10:06:39 Ian.Mayo Exp $
 *
 * Changes
 * -------
 * 15-Mar-2002 : Version 1 (DG);
 * 26-Mar-2002 : Changed name from JFreeChartDemoResources.java --> DemoResources.java (DG);
 *
 */
package com.jrefinery.legacy.chart.demo.resources;

import java.util.ListResourceBundle;

/**
 * A resource bundle that stores all the user interface items that might need localisation.
 */
public class DemoResources_pl extends ListResourceBundle {

    /**
     * Returns the array of strings in the resource bundle.
     */
    public Object[][] getContents() {
        return contents;
    }

    /** The resources to be localised. */
    static final Object[][] contents = {

        // about frame...
        { "about.title", "Informacja o..."},
        { "about.version.label", "Wersja"},

        // menu labels...
        { "menu.file", "Plik"},
        { "menu.file.mnemonic", new Character('P') },

        { "menu.file.exit", "Zako�cz"},
        { "menu.file.exit.mnemonic", new Character('K') },

        { "menu.help", "Pomoc"},
        { "menu.help.mnemonic", new Character('C')},

        { "menu.help.about", "About..."},
        { "menu.help.about.mnemonic", new Character('A')},

        // dialog messages...
        { "dialog.exit.title", "Potwierd� zamkni�cie..."},
        { "dialog.exit.message", "Czy jeste� pewien, �e chcesz zako�czy� program ?"},

        // labels for the tabs in the main window...
        { "tab.bar",      "Wykresy Kolumnowe i S�upkowe"},
        { "tab.pie",      "Wykresy Ko�owe"},
        {"tab.xy",       "Wykresy XY"},
        {"tab.time",     "Wykresy Liniowe"},
        {"tab.other",    "Wykresy Inne"},
        {"tab.test",     "Wykresy Testowe"},
        {"tab.combined", "Wykresy Niestandardowe"},

        // sample chart descriptions...
        {"chart1.title",       "S�upkowy grupowany: "},
        {"chart1.description", "Wy�wietla poziome s�upki, por�wnuje zgrupowane warto�ci  "
                              +"dla r�nych kategorii.  Uwaga: skala na osi poziomej jest odwr�cona."},

        {"chart2.title",       "S�upkowy skumulowany: "},
        {"chart2.description", "Wy�wietla poziome s�upki, por�wnuje wk�ad poszczeg�lnych warto�ci "
                              +"do sumy dla r�nych kategorii."},

        {"chart3.title",       "Kolumnowy grupowany: "},
        {"chart3.description", "Wy�wietla pionowe kolumny, por�wnuje zgrupowane warto�ci dla r�nych kategorii."},

        {"chart4.title",       "Kolumnowy grupowany z efektem 3-W: "},
        {"chart4.description", "Wy�wietla pionowe kolumny z efektem 3-W,  "
                              +"por�wnuje zgrupowane warto�ci dla r�nych kategorii"},

        {"chart5.title",       "Kolumnowy skumulowany: "},
        {"chart5.description", "Wy�wietla pionowe kolumny, "
                              +"por�wnuje skumulowane warto�ci dla r�nych kategorii."},

        {"chart6.title",       "Kolumnowy skumulowany z efektem 3-W: "},
        {"chart6.description", "Wy�wietla pionowe kolumny z efektem 3-W,  "
                              +"por�wnuje skumulowane warto�ci dla r�nych kategorii."},

        {"chart7.title",       "Ko�owy wysuni�ty: "},
        {"chart7.description", "Wy�wietla wk�ad poszczeg�lnych warto�ci do sumy ca�kowitej, podkre�laj�c jedn� z warto�ci poprzez wysuni�cie."},

        {"chart8.title",       "Ko�owy tradycyjny: "},
        {"chart8.description", "Wy�wietla procentowy wk�ad poszczeg�lnych warto�ci do sumy ca�kowitej, "
                              +"ponadto wykres ma przyk�adowy obrazek w tle."},

        {"chart9.title",       "XY Punktowy: "},
        {"chart9.description", "Wykres punktowy, z punktami danych po��czonymi "
                              +"wyg�adzonymi liniami bez znacznik�w danych."},

        {"chart10.title",       "Liniowy 1: "},
        {"chart10.description", "Wykres liniowy - wy�wietla trend w czasie lub dla r�nych kategorii danych XY. "
                               +"Ponadto demonstruje u�ycie wielu etykiet/nazw na jednym wykresie."},

        {"chart11.title",       "Liniowy 2: "},
        {"chart11.description", "Wykres liniowy - wy�wietla trend w czasie lub dla r�nych kategorii danych XY. "
                               +"O� pionowa jest wyskalowana logarytmicznie."},

        {"chart12.title",       "Liniowy 3: "},
        {"chart12.description", "Wykres liniowy - wy�wietla trend w czasie lub dla r�nych kategorii danych XY ze wskazaniem zmian warto�ci u�rednionej ."},

        {"chart13.title",       "Gie�dowy - Liniowy: Max/Min/Otwarcie/Zamkni�cie "},
        {"chart13.description", "Wykres gie�dowy typu Max/Min/Otwarcie/Zamkni�cie oparty o dane HighLowDataset(serie warto�ci podawane w odpowiedniej kolejno�ci)."},

        {"chart14.title",       "Gie�dowy - Candlestick: Max/Min/Otwarcie/Zamkni�cie: "},
        {"chart14.description", "Wykres gie�dowy typu Candlestick (Max/Min/Otwarcie/Zamkni�cie) oparty o dane HighLowDataset(serie warto�ci podawane w odpowiedniej kolejno�ci)."},

        {"chart15.title",       "Sygna�owy: "},
        {"chart15.description", "Wykres sygna�owy oparty o dane z SignalDataset."},

        {"chart16.title",       "Wiatrowy: "},
        {"chart16.description", "Ilustracja graficzna wiatru, przedstawiaj�ca jego kierunek i si�� "
                               +"(reprezentowan� w WindDataset)."},

        {"chart17.title",       "Rozproszony punktowy: "},
        {"chart17.description", "Wykres punktowy, rozproszony przedstawiaj�cy dane w uk�adzie XY z XYDataset."},

        {"chart18.title",       "Liniowy: "},
        {"chart18.description", "Wykres wy�wielta linie i/lub kszta�ty, przedstawiaj�ce dane z CategoryDataset. "
                               +"Ponadto ilustruje u�ycie obrazka w tle wykresu oraz "
                               +"przezroczysto�ci alpha "
                               +"na rysunku."},

        {"chart19.title",       "Pionowy XY kolumnowy: "},
        {"chart19.description", "Wykres prezentuje pionowe s�upki oparte na "
                               +"IntervalXYDataset."},

        {"chart20.title",       "Puste dane: "},
        {"chart20.description", "Wykres dla braku danych (null dataset)."},

        {"chart21.title",       "Dane zero: "},
        {"chart21.description", "Wykres dla serii zer w danych."},

        {"chart22.title",       "Liniowy z JScrollPane: "},
        {"chart22.description", "Wykres liniowy osadzony w komponencie JScrollPane pozwalaj�cym na przewijanie obszaru wykresu wewn�trz okna gdy jest ono za ma�e."},

        {"chart23.title",       "Kolumnowy dla jednej serii: "},
        {"chart23.description", "Wykres kolumnowy dla jednej serii danych. "
                               +"Demonstruje przy okazji �ycie ramki w ChartPanel."},

        {"chart24.title",       "Wykres dynamiczy: "},
        {"chart24.description", "Dynamiczny (rysowany na bie��co) wykres do testowania mechanizmu zdarze� (event notification mechanism)."},

        {"chart25.title",       "Nak�adany gie�dowy: Max/Min/Otwarcie/Zamkni�cie: "},
        {"chart25.description", "Wyswietla wykres nak�adany gie�dowy: Max/Min/Otwarcie/Zamkni�cie z "
                               +"ilustracj� przebiegu �redniej."},

        {"chart26.title",       "Poziomy - kombinowany: "},
        {"chart26.description", "Wy�wietla 3 r�ne poziome wykresy liniowe /czasowe i XY kolumnowy "
                               +"."},

        {"chart27.title",       "Pionowy - kombinowany: "},
        {"chart27.description", "Wy�wietla 4 r�ne wykresy umo�liwiaj�ce por�wnanie danych w pionie na jednym rysunku "
                               +"dla XY, liniowe /czasowe oraz kolumn pionowych XY."},

        {"chart28.title",       "Kombinowany i nak�adany: "},
        {"chart28.description", "Kombinowany wykres XY, nak�adany liniowy/TimeSeries i nak�adany "
                               +"Max/Min & liniowy."},

        {"chart29.title",       "Kombinowany i nak�adany dynamiczny: "},
        {"chart29.description", "Wy�wietla kombinowany i nak�adany wykres dynamiczny w celu "
                               +"testowania / ilustracji mechnizmu obs�ugi zdarze�."},

        {"charts.display", "Poka�"},

        // chart titles and labels...
        {"bar.horizontal.title",  "Poziomy wykres s�upkowy"},
        {"bar.horizontal.domain", "Kategorie"},
        {"bar.horizontal.range",  "Warto�ci"},

        {"bar.horizontal-stacked.title",  "Poziomy, skumulowany wykres s�upkowy"},
        {"bar.horizontal-stacked.domain", "Kategorie"},
        {"bar.horizontal-stacked.range",  "Warto�ci"},

        {"bar.vertical.title",  "Pionowy wykres kolumnowy"},
        {"bar.vertical.domain", "Kategorie"},
        {"bar.vertical.range",  "Warto�ci"},

        {"bar.vertical3D.title",  "Pionowy wykres kolumnowy z efektem 3-W"},
        {"bar.vertical3D.domain", "Kategorie"},
        {"bar.vertical3D.range",  "Warto�ci"},

        {"bar.vertical-stacked.title",  "Pionowy, skumulowany wykres kolumnowy"},
        {"bar.vertical-stacked.domain", "Kategorie"},
        {"bar.vertical-stacked.range",  "Warto�ci"},

        {"bar.vertical-stacked3D.title",  "Pionowy, skumulowany wykres kolumnowy z efektem 3-W"},
        {"bar.vertical-stacked3D.domain", "Kategorie"},
        {"bar.vertical-stacked3D.range",  "Warto�ci"},

        {"pie.pie1.title", "Wykres ko�owy 1 - wysuni�ty"},

        {"pie.pie2.title", "Wykres ko�owy 2 - tradycyjny"},

        {"xyplot.sample1.title",  "Wykres XY Punktowy"},
        {"xyplot.sample1.domain", "X Warto�ci"},
        {"xyplot.sample1.range",  "Y Warto�ci"},

        {"timeseries.sample1.title",     "Wykres liniowy przebiegu kursu w czasie - 1"},
        {"timeseries.sample1.subtitle",  "Warto�ci PLN in JPY"},
        {"timeseries.sample1.domain",    "Data"},
        {"timeseries.sample1.range",     "CCY na z�ot�wk�"},
        {"timeseries.sample1.copyright", "(C)opyright 2002, by Krzysztof Pa�, PW"},

        {"timeseries.sample2.title",    "Liniowy 2"},
        {"timeseries.sample2.domain",   "Millisekundy"},
        {"timeseries.sample2.range",    "O� logarytmiczna"},
        {"timeseries.sample2.subtitle", "Millisekundy"},

        {"timeseries.sample3.title",    "Liniowy z ruchomym trendem u�rednionym"},
        {"timeseries.sample3.domain",   "Data"},
        {"timeseries.sample3.range",    "CCY na PLN"},
        {"timeseries.sample3.subtitle", "30 dniowy �redni przebieg kursu PLN"},

        {"timeseries.highlow.title",    "Gie�dowy wykres Max/Min/Otwarcie/Zamkni�cie "},
        {"timeseries.highlow.domain",   "Data"},
        {"timeseries.highlow.range",    "Cena (PLN za udzia�)"},
        {"timeseries.highlow.subtitle", "Warto�� akcji TPSA"},

        {"timeseries.candlestick.title",    "Gie�dowy CandleStick"},
        {"timeseries.candlestick.domain",   "Data"},
        {"timeseries.candlestick.range",    "Cena (PLN za udzia�)"},
        {"timeseries.candlestick.subtitle", "Warto�� akcji JTT"},

        {"timeseries.signal.title",    "Wykres sygna�owy"},
        {"timeseries.signal.domain",   "Data"},
        {"timeseries.signal.range",    "Cena (PLN za udzia�)"},
        {"timeseries.signal.subtitle", "Warto�� akcji OPTIMUS S.A."},

        {"other.wind.title",  "Wykres wiatru"},
        {"other.wind.domain", "O� X"},
        {"other.wind.range",  "O� Y"},

        {"other.scatter.title",  "Rozrzucony punktowy"},
        {"other.scatter.domain", "O� X"},
        {"other.scatter.range",  "O� Y"},

        {"other.line.title",  "Liniowy"},
        {"other.line.domain", "Kategoria"},
        {"other.line.range",  "Warto��"},

        {"other.xybar.title",  "Liniowy kolumnowy"},
        {"other.xybar.domain", "Data"},
        {"other.xybar.range",  "Warto��"},

        {"test.null.title",  "Wykres XY (null data)"},
        {"test.null.domain", "X"},
        {"test.null.range",  "Y"},

        {"test.zero.title",  "Wykres XY (zero data)"},
        {"test.zero.domain", "O� X"},
        {"test.zero.range",  "O� Y"},

        {"test.scroll.title",    "Liniowy / Time Series"},
        {"test.scroll.subtitle", "Warto�� PLN"},
        {"test.scroll.domain",   "Data"},
        {"test.scroll.range",    "Warto��"},

        {"test.single.title",     "Pojedyncza seria"},
        {"test.single.subtitle1", "Podtytu� 1"},
        {"test.single.subtitle2", "Podtytu� 2"},
        {"test.single.domain",    "Data"},
        {"test.single.range",     "Warto��"},

        {"test.dynamic.title",  "Wykres Dynamiczny"},
        {"test.dynamic.domain", "Domena"},
        {"test.dynamic.range",  "Zasi�g"},

        {"combined.overlaid.title",     "Wykres Nak�adany"},
        {"combined.overlaid.subtitle",  "Max/Min/Otwarcie/Zamkni�cie z ilustracj� przebiegu �redniej."},
        {"combined.overlaid.domain",    "Data" },
        {"combined.overlaid.range",     "OPTIMUS S.A."},

        {"combined.horizontal.title",     "Wykres poziomo kombinowany"},
        {"combined.horizontal.subtitle",  "Linowy / Time Series s�upkowy XY "},
        {"combined.horizontal.domains",   new String[] {"Dane 1", "Dane 2", "Dane 3"} },
        {"combined.horizontal.range",     "CCY na PLN"},

        {"combined.vertical.title",     "Wykres pionowo kombinowany"},
        {"combined.vertical.subtitle",  "Cztery wykresy na jednym"},
        {"combined.vertical.domain",    "Data"},
        {"combined.vertical.ranges",    new String[] {"CCY na PLN", "Z�ot�wki", "KGHM", "S�upki"} },

        {"combined.combined-overlaid.title",     "Wykres kombinowany i nak�adany"},
        {"combined.combined-overlaid.subtitle",  "XY, mnak�adany (dwie TimeSeries) i nak�adany "
                                                +"(Max/Min i TimeSeries)"},
        {"combined.combined-overlaid.domain",    "Data"},
        {"combined.combined-overlaid.ranges",    new String[] {"CCY na PLN", "Z�ot�wki", "TPSA"} },

        {"combined.dynamic.title",     "Wykres poziomo kombinowany - dynamiczny"},
        {"combined.dynamic.subtitle",  "XY (seria 0), XY (seria 1), nak��dany (obie serie) "
                                      +"oraz XY (obie serie)"},
        {"combined.dynamic.domain",    "X" },
        {"combined.dynamic.ranges",    new String[] {"Y1", "Y2", "Y3", "Y4"} },

    };

}
