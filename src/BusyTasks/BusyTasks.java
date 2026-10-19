// BusyTasks - system monitor
// Copyright (C) 2025  Bernhard Mayr
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see https://www.gnu.org/licenses/.

package BusyTasks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSProcess;
import oshi.hardware.CentralProcessor;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class BusyTasks implements ActionListener
{
   private JFrame Hintergrundfenster, MinimiertesFenster;
   private JDialog Konfigurationsfenster, Popupfenster, Plotterfenster, Fehlerfenster, Plotterinfofenster, Farbfenster, Prozessfenster, Detailsfenster, Filterfenster, Optionsfenster;
   private Bewegungspanel meinBewegungspanel;
   private Größenfenster meinGrößenfenster;
   private Graph meinGraph;
   private JPanel Kästchen[], Popuppanel, Konfigurationspanel, Plotterinfopanel, Farbpanel, Optionspanel, Prozesspanel, Detailspanel;
   private JScrollPane Fehlerpanel, Detailsscrollpane;
   private JButton Beendigungsknopf, Konfigurationsknopf, Bestätigungsknopf, Schließungsknopf, Popupknopf, Farbfensterknopf, Farbrücksetzungsknopf, Farbauswahlknöpfe[];
   private JLabel Textfeld[], Plotterinfofeld[], Farblabelfeld[], Optionsfelder[], Detailslabels[];
   private JTextPane Prozesslisten[][];
   private JTextArea Fehlertext, Detailstext;
   private JTextField Filterfeld, Prozessauswahlfeld, Prozessnamensfeld;
   private Integer Länge, Breite, Positionx, Positiony, Textgröße, Layoutwahl, CpuKernzahl, Ansicht;
   private Timer Knopftimer, Fehlertimer, FehlendeKonfigTimer, Sekundentimer, SMEMTimer;
   private GridBagConstraints Layoutregeln;
   private JSpinner Längenauswahl, Breitenauswahl, Textgrößenauswahl;
   private boolean Texthintergrund, ZeigeEinheiten, iotopgestartet, nethogsgestartet, farbigerText, Rand, fügeDatenein, ZeigeBilder, Pause, offeneFenster, transparent, verwendeSMEM;
   private boolean erweiterteRechte = false; // Opt-in: PolicyKit only, no password
   private long Ticks[], freierSpeicher, gesamterSpeicher;
   private JComboBox Layoutwähler;
   private JCheckBox Texthintergrundcb, Einheitencb, farbigerTextcb, Randcb, Bildercb, Transparenzcb, SMEMcb, Rechtecb;
   private Thread Threads[];
   private Graphdaten AktuelleGraphdaten[];
   private Color Einheitenfarben[];
   private List<OSProcess> Prozesse;
   private SystemInfo allgemeineBetriebssysteminfo;
   private OperatingSystem Betriebssystem;
   private CentralProcessor Prozessor;
   private Hashtable<Integer,OSProcess> PIDProzessMapping, PIDProzessMapping_alt;
   private Hashtable<Integer,NETProzess> PIDNETProzessMapping;
   private HashMap<Integer,Double> PIDCPUMapping;
   private int Detailsansicht;
   private Standardadapter Mausadapter;

   private static Color CPUColor = Color.RED;
   private static Color MEMColor = Color.GREEN;
   private static Color IOColor = new Color(220,140,0);
   private static Color NETColor = Color.CYAN;

   public class Standardadapter extends MouseAdapter
   {
      public void mouseEntered(MouseEvent Ereignis)
      {
         if(Knopftimer!=null)
            Knopftimer.cancel();
      }
      public void mouseExited(MouseEvent Ereignis)
      {
         if (Knopftimer!=null)
            Knopftimer.cancel();
         Knopftimer=new Timer();
         setzeTimer(Knopftimer, 2000);
      }
   }
   public class leeresJPanel extends JPanel
   {
      leeresJPanel()
      {
         setOpaque(false);
         setBackground(new Color(0,0,0,0));
      }
      leeresJPanel(LayoutManager Manager)
      {
         super(Manager);
         setOpaque(false);
         setBackground(new Color(0,0,0,0));
      }
      @Override
      protected void paintComponent(Graphics Grafik)
      {
         Grafik.clearRect(0,0,getWidth(),getHeight());                               // Durchsichtiger Hintergrund erfordert manuelles Löschen alter Komponenten
      }
   }

   public class Bewegungspanel extends leeresJPanel                              // Innere Klasse: nicht-anonym und nicht-lokal
   {
      private Point Ausgangsposition;
      private JFrame Elterframe;
      
      public Bewegungspanel(final JFrame ÜbergebenesFrame)
      {
         Elterframe=ÜbergebenesFrame;
         addMouseListener(new Standardadapter()                               // Anonymes Objekt mit individuellen Methoden
         {
            public void mousePressed(MouseEvent Ereignis)                  
            {
               Ausgangsposition = Ereignis.getPoint();
            }
            public void mouseEntered(MouseEvent Ereignis)
            {
               if (Knopftimer!=null)
                  Knopftimer.cancel();
               Optionsfenster.setLocation(Elterframe.getLocation().x-Optionsfenster.getWidth(), Elterframe.getLocation().y-Optionsfenster.getHeight());
               Optionsfenster.setVisible(true);
               Plotterfenster.setLocation(Elterframe.getLocation().x, Elterframe.getLocation().y-Plotterfenster.getHeight());
               Prozessfenster.setLocation(Elterframe.getLocation().x, Elterframe.getLocation().y-Prozessfenster.getHeight());
               Detailsfenster.setLocation(Elterframe.getLocation().x, Elterframe.getLocation().y-Prozessfenster.getHeight());
               if (Ansicht==0)
                  Plotterfenster.setVisible(true);
               else if(Ansicht==1)
                  Prozessfenster.setVisible(true);
               else if(Ansicht==2)
                  Detailsfenster.setVisible(true);
               meinGrößenfenster.setLocation(Elterframe.getLocation().x+getWidth(), Elterframe.getLocation().y+getHeight());
               meinGrößenfenster.setVisible(true);
            }
            public void mouseClicked(MouseEvent Ereignis)
            {
               if (Ereignis.getButton()==MouseEvent.BUTTON3)
               {
                  Popupfenster.setLocation(Elterframe.getLocation().x+Ereignis.getPoint().x, Elterframe.getLocation().y+Ereignis.getPoint().y);
                  Popupfenster.setVisible(true);
               }
            }
         });
         addMouseMotionListener(new MouseMotionAdapter()
         {
            @Override
            public void mouseDragged(MouseEvent Ereignis)
            {
               Integer bewegtePixelx = Ereignis.getX() - Ausgangsposition.x;
               Integer bewegtePixely = Ereignis.getY() - Ausgangsposition.y;
               Elterframe.setLocation(Elterframe.getLocation().x + bewegtePixelx, Elterframe.getLocation().y + bewegtePixely);
               speichern();
            }
         });
      }
   }
   public class Größenfenster extends JDialog
   {
      private JFrame meinFrame;
      private JButton Größenknopf;
      private MouseEvent Ausgangsereignis;
      
      public Größenfenster(JFrame zuVeränderndes)
      {
         meinFrame=zuVeränderndes;
         setUndecorated(true);
         setBackground(new Color(0,0,0,0));
         setSize(15,15);
         Größenknopf = new JButton();
         Größenknopf.addMouseListener(new Standardadapter()
         {
            public void mousePressed(MouseEvent Ereignis)
            {
               Ausgangsereignis=Ereignis;
            }
            public void mouseReleased(MouseEvent Ereignis)
            {
               int bewegtePixelX = Ereignis.getXOnScreen() - Ausgangsereignis.getXOnScreen();
               int bewegtePixelY = Ereignis.getYOnScreen() - Ausgangsereignis.getYOnScreen();
               meinFrame.setSize(meinFrame.getWidth()+bewegtePixelX, meinFrame.getHeight()+bewegtePixelY);
               Längenauswahl.setValue(meinFrame.getWidth());
               Breitenauswahl.setValue(meinFrame.getHeight());
               speichern();
            }
         });
         Größenknopf.addMouseMotionListener(new MouseMotionAdapter()
         {
            public void mouseDragged(MouseEvent Ereignis)
            {
               int bewegtePixelX = Ereignis.getX() - (int) Ausgangsereignis.getX();
               int bewegtePixelY = Ereignis.getY() - (int) Ausgangsereignis.getY();
               setLocation((int) getLocation().getX()+ bewegtePixelX, (int) getLocation().getY()+bewegtePixelY);
            }
         });
         add(Größenknopf);
      }
   }
   public class Graph extends JPanel implements MouseWheelListener
   {
      private int Zahlen[][];
      private String Texte[][];
      private Date Zeitstempel[];
      private int Linien_x=0;
      public int Größe = 450;
      public int Lupenteil = 75;
      public int Zoom = 3;                                     // Lupenteil muss Vielfaches von Zoom sein
      public Graph()
      {
         setSize(450,160);
         setOpaque(false);
         setBackground(new Color(0,0,0,0));
         Zahlen = new int[4][];
         Texte = new String[4][];
         Zeitstempel = new Date[Größe];
         Zeitstempel[Größe-1] = new Date();
         for (int i=0; i<Größe-1; ++i)
            Zeitstempel[i] = Zeitstempel[Größe-1];

         for (int i=0; i<4; ++i)
         {
            Zahlen[i] = new int[Größe];
            Texte[i] = new String[Größe];
            for (int j=0; j<Größe; ++j)
            {
               Zahlen[i][j]=0;
               Texte[i][j]=" ";
            }
         }
         addMouseListener(new Standardadapter()
         {
            public void mouseEntered(MouseEvent Ereignis)
            {
               Plotterinfofenster.setLocation(Plotterfenster.getLocationOnScreen().x, Plotterfenster.getLocationOnScreen().y-Plotterinfofenster.getHeight());
               Plotterinfofeld[0].setText(Zeitstempel[Ereignis.getX()/2].toString());
               for (int i=0; i<4; ++i)
                  Plotterinfofeld[i+1].setText(Texte[i][Ereignis.getX()/2]);
               Plotterinfofenster.setVisible(true);
               if (Knopftimer!=null)
                  Knopftimer.cancel();
            }
            public void mouseExited(MouseEvent Ereignis)
            {
               Linien_x=-Lupenteil;
               Plotterinfofenster.dispose();
               if (Knopftimer!=null)
                  Knopftimer.cancel();
               Knopftimer=new Timer();
               setzeTimer(Knopftimer, 2000);
            }
         });
         addMouseMotionListener(new MouseMotionAdapter()
         {
            @Override
            public void mouseMoved(MouseEvent Ereignis)
            {
               Linien_x=Ereignis.getX();
               Plotterinfofeld[0].setText(Zeitstempel[Ereignis.getX()].toString());
               for (int i=0; i<4; ++i)
                  Plotterinfofeld[i+1].setText(Texte[i][Ereignis.getX()]);
               if (!Pause)
                  repaint();
            }
         });
         addMouseWheelListener(this);
      }
      @Override
      public void mouseWheelMoved(MouseWheelEvent e)
      {
         Linien_x+=e.getWheelRotation();
         if (Linien_x>=0 && Linien_x<450)
         {
            Plotterinfofeld[0].setText(Zeitstempel[Linien_x].toString());
            for (int i=0; i<4; ++i)
               Plotterinfofeld[i+1].setText(Texte[i][Linien_x]);
            if(!Pause)
               repaint();
         }
         else
            Linien_x=-Lupenteil;
      }
      public void hinzufügen(int Typ, Graphdaten neueDaten)
      {
         for (int i=0; i<Größe-1; ++i)
         {
            Zahlen[Typ][i]=Zahlen[Typ][i+1];
            Texte[Typ][i]=Texte[Typ][i+1];
         }
         if(Typ>1)
            Zahlen[Typ][Größe-1]=formatiereDaten(neueDaten.Zahlenwert);
         else
            Zahlen[Typ][Größe-1]=neueDaten.Zahlenwert;
         Texte[Typ][Größe-1] = neueDaten.Textinfo;
         if(Typ==0)
         {
            if (Linien_x>0)
               --Linien_x;
            else
               Linien_x=-Lupenteil;
            for (int i=0; i<Größe-1; ++i)
               Zeitstempel[i]=Zeitstempel[i+1];
            Zeitstempel[Größe-1] = new Date();
         }
      }
      private int formatiereDaten(int Wert)
      {
         double FW = Wert;
         if(Wert<=10)                                    // 10 KB/s
            FW=FW/0.5;                                   // Nenner = Grenze/Feldgröße = Grenze/20
         else if(Wert<=1024)                                  // 1 MB/s
            FW=20+FW/51.2;
         else if (Wert<=10240)                           // 10 MB/s
            FW=40+FW/256;
         else if (Wert<=102400)                           // 100 MB/s
            FW=80+FW/2560;
         else                                            // Maximum: 1000 MB/s
            FW=120+FW/25600;
         return (int) FW;
      }
      @Override
      protected void paintComponent(Graphics Grafik)
      {
         Grafik.clearRect(0,0, getWidth(), getHeight());
         Grafik.setColor(Color.WHITE);
         if(Linien_x>0)
            Grafik.drawLine(Linien_x, 0, Linien_x, 160);
         for (int i=1; i<5; ++i)
            Grafik.drawLine(0,i*40,450,i*40);
         Font Schriftart = Grafik.getFont();
         Grafik.setFont(Schriftart.deriveFont(8f));
         Grafik.drawString("100 MB/s", 5,38);
         Grafik.drawString("10 MB/s", 5,78);
         Grafik.drawString("1 MB/s", 5,118);
         Grafik.setColor(Color.GRAY);
         Grafik.drawLine(0,140,450,140);
         if (Linien_x>=0)
         {
            Grafik.drawLine(Linien_x-Lupenteil, 0, Linien_x-Lupenteil, 160);
            Grafik.drawLine(Linien_x+Lupenteil, 0, Linien_x+Lupenteil, 160);
         }
         Grafik.drawString("10 KB/s", 5,138);
         Grafik.setFont(Schriftart);
         Grafik.setColor(MEMColor);
         Grafik.drawString("MEM", 80,15);
         for (int i=0, j=0; i<Größe-1; ++i, ++j)
            if (i<Linien_x-Lupenteil || i>Linien_x+Lupenteil)
               Grafik.drawLine(j,160-Zahlen[1][i], j+1, 160-Zahlen[1][i+1]);
         for (int i=Linien_x-Lupenteil/Zoom, j=Linien_x-Lupenteil; j<Linien_x+Lupenteil; ++i, j+=Zoom)
            if (i>0 && i<Größe-1)
               Grafik.drawLine(j,160-Zahlen[1][i], j+Zoom, 160-Zahlen[1][i+1]);
         Grafik.setColor(IOColor);
         Grafik.drawString("IO", 115,15);
         for (int i=0, j=0; i<Größe-1; ++i, ++j)
            if (i<Linien_x-Lupenteil || i>Linien_x+Lupenteil)
               Grafik.drawLine(j,160-Zahlen[2][i], j+1, 160-Zahlen[2][i+1]);
         for (int i=Linien_x-Lupenteil/Zoom, j=Linien_x-Lupenteil; j<Linien_x+Lupenteil; ++i, j+=Zoom)
            if (i>0 && i<Größe-1)
               Grafik.drawLine(j,160-Zahlen[2][i], j+Zoom, 160-Zahlen[2][i+1]);
         Grafik.setColor(NETColor);
         Grafik.drawString("NET", 130,15);
         for (int i=0, j=0; i<Größe-1; ++i, ++j)
            if (i<Linien_x-Lupenteil || i>Linien_x+Lupenteil)
               Grafik.drawLine(j,160-Zahlen[3][i], j+1, 160-Zahlen[3][i+1]);
         for (int i=Linien_x-Lupenteil/Zoom, j=Linien_x-Lupenteil; j<Linien_x+Lupenteil; ++i, j+=Zoom)
            if (i>0 && i<Größe-1)
               Grafik.drawLine(j,160-Zahlen[3][i], j+Zoom, 160-Zahlen[3][i+1]);
         Grafik.setColor(CPUColor);
         Grafik.drawString("CPU", 50,15);
         for (int i=0, j=0; i<Größe-1; ++i, ++j)
            if (i<Linien_x-Lupenteil || i>Linien_x+Lupenteil)
               Grafik.drawLine(j,160-Zahlen[0][i], j+1, 160-Zahlen[0][i+1]);
         for (int i=Linien_x-Lupenteil/Zoom, j=Linien_x-Lupenteil; j<Linien_x+Lupenteil; ++i, j+=Zoom)
            if (i>0 && i<Größe-1)
               Grafik.drawLine(j,160-Zahlen[0][i], j+Zoom, 160-Zahlen[0][i+1]);
      }
   }
   private class Datenvektor
   {
      public double Wert;
      public String Einheit;
      public Color Farbe;

      private Datenvektor(double neuerWert, String neueEinheit, Color neueFarbe)
      {
         Wert = neuerWert;
         Einheit = neueEinheit;
         Farbe = neueFarbe;
      }
   }
   private class Graphdaten
   {
      public int Zahlenwert;
      public String Textinfo;

      public Graphdaten(int neuerZahlenwert, String neuesTextinfo)
      {
         Zahlenwert=neuerZahlenwert;
         Textinfo=neuesTextinfo;
      }
   }
   private class NETProzess implements Comparable<NETProzess>
   {
      public String Name;
      public Integer PID;
      public double Gesendet, Empfangen, kumGesendet, kumEmpfangen;

      public NETProzess()
      {
         Name=new String("");
         PID = 0;
         Gesendet = 0.0;
         Empfangen = 0.0;
         kumGesendet = 0.0;
         kumEmpfangen = 0.0;
      }
      public Double liefereGesamt()
      {
         return Gesendet + Empfangen;
      }
      public int compareTo(NETProzess andererNETProzess)
      {
         return Double.compare(andererNETProzess.liefereGesamt(), liefereGesamt());
      }
      public String toString()
      {
         return Name +" "+ Gesendet +" "+ Empfangen;
      }
   }
   private void neustarten()
   {
      try
      {
         String Befehl[] = new BufferedReader(new FileReader("/proc/self/cmdline")).readLine().split("\u0000");
         ProcessBuilder neuesBusyTasks = new ProcessBuilder(Befehl);
         for(int i=0; i<Befehl.length; ++i)
            System.out.print(Befehl[i]+ " ");
         neuesBusyTasks.inheritIO().start();
         System.exit(0);
      }
      catch (IOException IOAusnahme)
      {
         zeigeAusnahme(IOAusnahme);
      }
   }
   public static void main(String args[])
   {
      BusyTasks myBusyTasks = new BusyTasks();
   }
   public BusyTasks()
   {
      Fehlerfenster = new JDialog();
      Fehlerfenster.setTitle("BusyTasks: Error");
      Fehlerfenster.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      Fehlerfenster.setModal(false);
      Fehlerfenster.setSize(300,150);
      Fehlertext = new JTextArea(3,25);
      Fehlertext.setLineWrap(true);
      Fehlertext.setWrapStyleWord(true);
      Fehlertext.append("INFO:\n");
      Fehlerpanel = new JScrollPane(Fehlertext);
      Fehlerfenster.setContentPane(Fehlerpanel);

      Länge=300;
      Breite=60;
      Positionx=300;
      Positiony=300;
      Textgröße=10;
      Layoutwahl=0;
      Ansicht=0;
      Detailsansicht=0;
      Texthintergrund=true;
      ZeigeEinheiten=true;
      farbigerText = false;
      Rand=false;
      ZeigeBilder=false;
      iotopgestartet=false;
      nethogsgestartet=false;
      Pause=false;
      offeneFenster=false;
      transparent = true;
      verwendeSMEM = false;
      fügeDatenein = true;
      allgemeineBetriebssysteminfo = new SystemInfo();
      Betriebssystem = allgemeineBetriebssysteminfo.getOperatingSystem();
      Prozessor = allgemeineBetriebssysteminfo.getHardware().getProcessor();
      CpuKernzahl = Prozessor.getLogicalProcessorCount();
      gesamterSpeicher = allgemeineBetriebssysteminfo.getHardware().getMemory().getTotal();
      PIDProzessMapping = new Hashtable<Integer,OSProcess>();
      PIDProzessMapping_alt = new Hashtable<Integer,OSProcess>();
      PIDNETProzessMapping = new Hashtable<Integer,NETProzess>();
      PIDCPUMapping = new HashMap<Integer, Double>();

      Mausadapter = new Standardadapter();
      AktuelleGraphdaten = new Graphdaten[4];
      for (int i=0; i<4; ++i)
         AktuelleGraphdaten[i]=new Graphdaten(0,"");
      meinGraph=new Graph();
      Einheitenfarben = new Color[3];
      Einheitenfarben[0] = new Color(230,230,0);
      Einheitenfarben[1] = new Color(255,100,255);
      Einheitenfarben[2] = new Color(100,175,255);
      laden();
      if (Files.exists(Path.of(System.getProperty("user.home"), "BusyTasks.config")))
         System.err.println("WARNING: Legacy ~/BusyTasks.config may contain a plaintext sudo password. It is NOT read by BusyTasks Safe. Review and remove it manually.");
      Hintergrundfenster = new JFrame("BusyTasks Safe");
      RepaintManager.currentManager(Hintergrundfenster).setDoubleBufferingEnabled(false);                      // Bei doppeltem Buffer tritt unter Linux übles Flimmern auf

      meinGrößenfenster = new Größenfenster(Hintergrundfenster);
      Plotterfenster = new JDialog(); Plotterinfofenster = new JDialog(); Popupfenster = new JDialog(); Optionsfenster = new JDialog(); Farbfenster = new JDialog(); Prozessfenster = new JDialog(); Detailsfenster = new JDialog(); Filterfenster = new JDialog();
      Filterfenster.addMouseListener(Mausadapter);
      Prozessfenster.addMouseListener(Mausadapter);
      Hintergrundfenster.setType(javax.swing.JFrame.Type.POPUP);                       // Popup verhindert einen Eintrag in der Fensterleiste
      Hintergrundfenster.setSize(Länge, Breite);
      Popupfenster.setSize(200,40);
      Plotterfenster.setSize(450,162);
      Plotterinfofenster.setSize(300,75);
      Prozessfenster.setSize(500,204);
      Detailsfenster.setSize(500,204);
      Farbfenster.setSize(170,170);
      Optionsfenster.setSize(75,162);
      Filterfenster.setSize(300,40);
      Hintergrundfenster.setLocation(Positionx,Positiony);
      Hintergrundfenster.setUndecorated(true); Popupfenster.setUndecorated(true); Optionsfenster.setUndecorated(true); Prozessfenster.setUndecorated(true); Detailsfenster.setUndecorated(true); Filterfenster.setUndecorated(true); Plotterfenster.setUndecorated(true); Plotterinfofenster.setUndecorated(true);    // Transparenz erfordert Entfernung der OS-Dekoration, Java-Dekoration erlaubt

      if (Rand)
         Hintergrundfenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
      Popupfenster.setBackground(new Color(0,0,0,0));   // Der vierte Farbwert bestimmt den Transparenzgrad
      if (transparent)
      {
         Hintergrundfenster.setBackground(new Color(0,0,0,180));
         Plotterinfofenster.setBackground(new Color(0,0,0,180));
         Plotterfenster.setBackground(new Color(0,0,0,180));
         Prozessfenster.setBackground(new Color(0,0,0,180));
         Detailsfenster.setBackground(new Color(0,0,0,180));
         Filterfenster.setBackground(new Color(0,0,0,180));
      }
      else
      {
         Hintergrundfenster.setBackground(new Color(0,0,0,255));
         Plotterinfofenster.setBackground(new Color(0,0,0,255));
         Plotterfenster.setBackground(new Color(0,0,0,255));
         Prozessfenster.setBackground(new Color(0,0,0,255));
         Detailsfenster.setBackground(new Color(0,0,0,255));
         Filterfenster.setBackground(new Color(0,0,0,255));
      }
      Optionsfenster.setBackground(new Color(0,0,0,180));
      Plotterfenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.WHITE)); Plotterinfofenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.WHITE)); Filterfenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.WHITE)); Prozessfenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.WHITE));
      Plotterfenster.add(meinGraph);
      Optionsfenster.setAlwaysOnTop(true); Plotterfenster.setAlwaysOnTop(true); Plotterinfofenster.setAlwaysOnTop(true); Prozessfenster.setAlwaysOnTop(true); Detailsfenster.setAlwaysOnTop(true);
      meinBewegungspanel = new Bewegungspanel(Hintergrundfenster);
      meinBewegungspanel.setBackground(new Color(0,0,0,0));
      GridLayout Gridlayout = new GridLayout();
      if (Layoutwahl==0)
      {
         Gridlayout.setRows(4); Gridlayout.setColumns(1);
      }
      else if (Layoutwahl==1)
      {
         Gridlayout.setRows(1); Gridlayout.setColumns(4);
      }
      else
      {
         Gridlayout.setRows(2); Gridlayout.setColumns(2);
      }
      meinBewegungspanel.setLayout(Gridlayout);
      Beendigungsknopf= new JButton("X"); Popupknopf = new JButton("Minimize"); Konfigurationsknopf = new JButton("Config"); Bestätigungsknopf = new JButton("Save"); Schließungsknopf = new JButton("Close"); Farbfensterknopf = new JButton("Edit"); Farbrücksetzungsknopf = new JButton("Set default values");
      Beendigungsknopf.addActionListener(this); Popupknopf.addActionListener(this); Konfigurationsknopf.addActionListener(this); Bestätigungsknopf.addActionListener(this); Schließungsknopf.addActionListener(this); Farbfensterknopf.addActionListener(this); Farbrücksetzungsknopf.addActionListener(this);
      Farbfensterknopf.setMargin(new Insets(0,0,0,0));
      Farbauswahlknöpfe = new JButton[3];
      for (int i=0; i<3; ++i)
      {
         Farbauswahlknöpfe[i] = new JButton("Choose");
         Farbauswahlknöpfe[i].addActionListener(this);
      }
      Popuppanel = new leeresJPanel();
      Popuppanel.add(Popupknopf);
      Popuppanel.add(Beendigungsknopf);
      Popupfenster.add(Popuppanel);
      Plotterinfopanel = new leeresJPanel(new GridBagLayout());
      String Layouts[] = {"Vertical", "Horizontal", "Circular"};
      Layoutwähler = new JComboBox<String>(Layouts);
      Layoutwähler.setSelectedIndex(Layoutwahl);
      Texthintergrundcb=new JCheckBox("Draw text background", Texthintergrund);
      Einheitencb = new JCheckBox("Show units", ZeigeEinheiten);
      farbigerTextcb = new JCheckBox("Coloured text", farbigerText);
      Randcb = new JCheckBox("Draw border", Rand);
      Transparenzcb = new JCheckBox("Activate transparency", transparent);
      Bildercb = new JCheckBox("Draw icons", ZeigeBilder);
      SMEMcb = new JCheckBox("Use SMEM", verwendeSMEM);
      Rechtecb = new JCheckBox("Enable I/O + network (PolicyKit)", erweiterteRechte);
      Rechtecb.setToolTipText("Optional: system authorization via pkexec; no password is stored.");

      Detailslabels = new JLabel[4];
      for (int i=0; i<4; ++i)
      {
         Detailslabels[i] = new JLabel();
         Detailslabels[i].addMouseListener(
         new Standardadapter()
         {
            public void mouseClicked(MouseEvent Ereignis)
            {
               int i=0;
               for (i=0; i<4 && Ereignis.getSource()!=Detailslabels[i]; ++i) ;
               Detailsansicht=i;
               if (Detailsansicht>0)
                  getinfo(Prozessauswahlfeld.getText(), Detailsansicht);
               else
                  zeigeProzessCPUInfo(Prozessauswahlfeld.getText());
            }
         });
      }
      Detailslabels[0].setText(" CPU ");
      Detailslabels[0].setForeground(CPUColor);
      Detailslabels[1].setText(" MEM ");
      Detailslabels[1].setForeground(MEMColor);
      Detailslabels[2].setText(" IO ");
      Detailslabels[2].setForeground(IOColor);
      Detailslabels[3].setText(" NET ");
      Detailslabels[3].setForeground(NETColor);
      JLabel PIDLabel = new JLabel("PID: "), NamensLabel = new JLabel("Name: ");
      PIDLabel.setForeground(Color.white);
      NamensLabel.setForeground(Color.white);
      Prozessauswahlfeld = new JTextField(5);
      Prozessauswahlfeld.addActionListener(
      new ActionListener()
      {
         public void actionPerformed(ActionEvent Ereignis)
         {
            if (Detailsansicht>0)
               getinfo(Prozessauswahlfeld.getText(), Detailsansicht);
            else
            {
               zeigeProzessCPUInfo(Prozessauswahlfeld.getText());
            }
         }
      });
      Prozessnamensfeld = new JTextField(10);
      Prozessnamensfeld.addActionListener(
      new ActionListener()
      {
         public void actionPerformed(ActionEvent Ereignis)
         {
            ArrayList<OSProcess> gefundeneProzesse = new ArrayList<OSProcess>();
            for (int i=0; i<Prozesse.size(); ++i)
               if(Prozesse.get(i).getName().contains(Prozessnamensfeld.getText()))
                  gefundeneProzesse.add(Prozesse.get(i));
            if(gefundeneProzesse.size()==1)
            {
               if (Detailsansicht>0)
                  getinfo(String.valueOf(gefundeneProzesse.get(0).getProcessID()), Detailsansicht);
               else
               {
                  zeigeProzessCPUInfo(String.valueOf(gefundeneProzesse.get(0).getProcessID()));
               }
            }
            else if (gefundeneProzesse.size()>1)
            {
               Detailstext.setText("");
               for (int i=0; i<gefundeneProzesse.size(); ++i)
                  Detailstext.append(String.valueOf(gefundeneProzesse.get(i).getProcessID()) + " " + gefundeneProzesse.get(i).getName() + "\n");
            }
         }
      });
      leeresJPanel Ansichtspanel = new leeresJPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
      for (int i=0; i<4; ++i)
         Ansichtspanel.add(Detailslabels[i]);
      Ansichtspanel.add(Box.createHorizontalStrut(50));
      Ansichtspanel.add(PIDLabel);
      Ansichtspanel.add(Prozessauswahlfeld);
      Ansichtspanel.add(NamensLabel);
      Ansichtspanel.add(Prozessnamensfeld);
      Prozessauswahlfeld.addMouseListener(Mausadapter);
      Prozessauswahlfeld.setBackground(Color.lightGray);
      Prozessauswahlfeld.setFont(Prozessauswahlfeld.getFont().deriveFont(Font.BOLD));
      Prozessnamensfeld.addMouseListener(Mausadapter);
      Prozessnamensfeld.setBackground(Color.lightGray);
      Prozessnamensfeld.setFont(Prozessnamensfeld.getFont().deriveFont(Font.BOLD));
      Detailstext = new JTextArea(10,10);
      Detailstext.addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            Detailsfenster.dispose();
            Prozessfenster.setVisible(true);
            Ansicht=1;
         }
      });
      Detailstext.setLineWrap(false);
      Detailstext.setEditable(false);
      Detailstext.setWrapStyleWord(false);
      Detailstext.setForeground(Color.white);
      Detailstext.setBackground(new Color(0,0,0,255));
//       Detailstext.setOpaque(false);
      Detailstext.setFont(Detailstext.getFont().deriveFont(11f));
      Detailsscrollpane = new JScrollPane(Detailstext);
      Detailsscrollpane.getVerticalScrollBar().setUI(
      new BasicScrollBarUI()
      {
         @Override
         protected void configureScrollBarColors()
         {
            this.thumbColor = Color.darkGray;
            this.trackColor = Color.black;
         }
      });
      Detailsscrollpane.getVerticalScrollBar().addMouseListener(Mausadapter);
      Detailsscrollpane.getHorizontalScrollBar().setUI(
      new BasicScrollBarUI()
      {
         @Override
         protected void configureScrollBarColors()
         {
            this.thumbColor = Color.darkGray;
            this.trackColor = Color.black;
         }
      });
      Detailsscrollpane.getHorizontalScrollBar().addMouseListener(Mausadapter);
      Detailsscrollpane.addMouseListener(Mausadapter);
//       Detailsscrollpane.getViewport().setOpaque(false);
//       Detailsscrollpane.setOpaque(false);
      Detailsscrollpane.setBackground(new Color(0,0,0,255));
      Detailsscrollpane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE), BorderFactory.createEmptyBorder(5,5,5,5)));

      Detailspanel = new leeresJPanel();
      Detailspanel.setLayout(new BoxLayout(Detailspanel, BoxLayout.Y_AXIS));
      Detailspanel.add(Ansichtspanel);
      Detailspanel.add(Detailsscrollpane);
      Detailspanel.setBorder(BorderFactory.createLineBorder(Color.white));
      Detailsfenster.add(Detailspanel);

      Prozesspanel = new leeresJPanel(new GridLayout(2,2));
      JPanel Prozessfensterteile[] = new leeresJPanel[4];
      for (int i=0; i<4; ++i)
      {
         Prozessfensterteile[i]=new leeresJPanel(new GridBagLayout());
         Prozessfensterteile[i].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE), BorderFactory.createEmptyBorder(5,5,5,5)));
         Prozesspanel.add(Prozessfensterteile[i]);
      }
      Prozessfensterteile[0].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if(Ereignis.getButton()==MouseEvent.BUTTON1)
            {
               int Index = Ereignis.getY()/17;
               String PIDs[] = Prozesslisten[0][1].getText().split("\n");
               if (!PIDs[0].equals("") && Index<PIDs.length)
               {
                  Prozessfenster.dispose();
                  Detailsfenster.setVisible(true);
                  Ansicht=2;
                  Detailsansicht=0;
                  zeigeProzessCPUInfo(PIDs[Index]);
               }
            }
         }
      });
      Prozessfensterteile[1].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if(Ereignis.getButton()==MouseEvent.BUTTON1)
            {
               int Index = Ereignis.getY()/17;
               String PIDs[] = Prozesslisten[1][1].getText().split("\n");
               if (!PIDs[0].equals("") && Index<PIDs.length)
               {
                  Prozessfenster.dispose();
                  Detailsfenster.setVisible(true);
                  Ansicht=2;
                  Detailsansicht=2;
                  getinfo(PIDs[Index], Detailsansicht);
//                   lsof("sudo -S lsof -wap " + PIDs[Index] + " -d ^mem /");
               }
            }
         }
      });
      Prozessfensterteile[2].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if(Ereignis.getButton()==MouseEvent.BUTTON1)
            {
               int Index = Ereignis.getY()/17;
               String PIDs[] = Prozesslisten[2][1].getText().split("\n");
               if (!PIDs[0].equals("") && Index<PIDs.length)
               {
                  Prozessfenster.dispose();
                  Detailsfenster.setVisible(true);
                  Ansicht=2;
                  Detailsansicht=1;
                  getinfo(PIDs[Index], Detailsansicht);
               }
            }
         }
      });
      Prozessfensterteile[3].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if(Ereignis.getButton()==MouseEvent.BUTTON1)
            {
               int Index = Ereignis.getY()/17;
               String PIDs[] = Prozesslisten[3][1].getText().split("\n");
               if (!PIDs[0].equals("") && Index<PIDs.length)
               {
                  Prozessfenster.dispose();
                  Detailsfenster.setVisible(true);
                  Ansicht=2;
                  Detailsansicht=3;
                  getinfo(PIDs[Index], Detailsansicht);
               }
            }
         }
      });

      GridBagConstraints Regeln = new GridBagConstraints();
      Regeln.anchor=GridBagConstraints.FIRST_LINE_START;
      Regeln.fill=GridBagConstraints.BOTH;
      JLabel Kennzeichnungen[] = new JLabel[4];
      for (int i=0; i<4; ++i)
         Kennzeichnungen[i]= new JLabel();
      Regeln.gridx=0;
      Kennzeichnungen[0].setForeground(CPUColor);
      Kennzeichnungen[0].setText("<html>C<br>P<br>U</html>");
      Prozessfensterteile[0].add(Kennzeichnungen[0], Regeln);
      Kennzeichnungen[1].setForeground(IOColor);
      Kennzeichnungen[1].setText("<html>&nbsp;I<br>O</html>");
      Prozessfensterteile[1].add(Kennzeichnungen[1], Regeln);
      Kennzeichnungen[2].setForeground(MEMColor);
      Kennzeichnungen[2].setText("<html>M<br>E<br>M</html>");
      Prozessfensterteile[2].add(Kennzeichnungen[2], Regeln);
      Kennzeichnungen[3].setForeground(NETColor);
      Kennzeichnungen[3].setText("<html>N<br>E<br>T</html>");
      Prozessfensterteile[3].add(Kennzeichnungen[3], Regeln);
      Prozesslisten = new JTextPane[4][];
      for (int i=0; i<4; ++i)
      {
         Prozesslisten[i] = new JTextPane[3];
         for (int j=0; j<3; ++j)
         {
            Prozesslisten[i][j] = new JTextPane();
            Prozesslisten[i][j].setEditable(false);
            Prozesslisten[i][j].setFocusable(false);
            Prozesslisten[i][j].setForeground(Color.white);
            Prozesslisten[i][j].setOpaque(false);
            Prozesslisten[i][j].setFont(Prozesslisten[i][j].getFont().deriveFont(11f));
            Prozesslisten[i][j].addMouseListener(
            new Standardadapter()
            {
               public void mouseClicked(MouseEvent Ereignis)
               {
                  Ereignis.getComponent().getParent().dispatchEvent(Ereignis);
               }
            });
            Regeln.gridx=j+1;

            int Zusatz=0;
            if(j<2)
            {
//                if (i==0)
//                   Zusatz-=10;
               if (i==0 || i==2)
               {
                  SimpleAttributeSet Attribute = new SimpleAttributeSet(Prozesslisten[i][j].getParagraphAttributes());
                  StyleConstants.setAlignment(Attribute, StyleConstants.ALIGN_RIGHT);
                  Prozesslisten[i][j].setParagraphAttributes(Attribute, true);
                  if(i==0 && j==0)
                     Zusatz-=10;
                  if (j==1)
                     Zusatz-=20;
               }
               else
               {
                  if (j==0)
                     Zusatz=15;
                  if (j==1)
                  {
                     SimpleAttributeSet Attribute = new SimpleAttributeSet(Prozesslisten[i][j].getParagraphAttributes());
                     StyleConstants.setAlignment(Attribute, StyleConstants.ALIGN_RIGHT);
                     Prozesslisten[i][j].setParagraphAttributes(Attribute, true);
                     Zusatz-=20;
                  }
                  else if (j==2)
                     Zusatz+=40;
               }
               Regeln.weightx = 0;
            }
            else
            {
               Regeln.weightx = 1;
            }
            Prozesslisten[i][j].setMinimumSize(new Dimension(60+Zusatz, 100));
            Prozesslisten[i][j].setPreferredSize(new Dimension(60+Zusatz, 100));
            Prozessfensterteile[i].add(Prozesslisten[i][j], Regeln);
         }
      }
      Prozessfenster.setContentPane(Prozesspanel);

      Textfeld = new JLabel[10];
      Kästchen = new leeresJPanel[4];
      GridBagConstraints Layout = new GridBagConstraints();
      Layout.anchor=GridBagConstraints.LINE_START;                               // Objektausrichtung findet nur bei Gewichtung Berücksichtigung
      Layout.weightx=1.0;
      for (int i=0; i<10; ++i)
      {
         Textfeld[i]= new JLabel();
         Textfeld[i].setOpaque(Texthintergrund);
         Textfeld[i].setFont(Textfeld[i].getFont().deriveFont((float) Textgröße));
         Textfeld[i].setForeground(Color.white);
         Textfeld[i].setBackground(Color.black);
         Textfeld[i].setText(".");
         if (i<4)
         {
            Kästchen[i] = new leeresJPanel(new GridBagLayout());
            meinBewegungspanel.add(Kästchen[i]);
         }
      }
      Textfeld[2].setText("IOTOP DISCONNECTED");
      Textfeld[7].setText("NETHOGS DISCONNECTED");
      if (verwendeSMEM)
         Textfeld[5].setText("SMEM DISCONNECTED");
      for (int i=0, j=0; i<10 && j<10; ++i,++j)                                       // Gewichtung: Jene Objekte mit 0 erhalten keinen freien Platz
      {
         if(i==j || i==1 || i==3 || i==2 && j==5)
            Layout.weightx=0.0;
         if (i==1 && j==1 || i==3 && j==6 || i==2 && j<5 || i==4)
         {
            --i;
            if (j>1 && j<4 || j>6 && j<9)
               Layout.weightx=0.0;
            else
               Layout.weightx=1.0;
         }
         Kästchen[i].add(Textfeld[j], Layout);
      }
      Hintergrundfenster.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
      Hintergrundfenster.setContentPane(meinBewegungspanel);
      Hintergrundfenster.setVisible(true);
//       JButton Knopf = new JButton(new ImageIcon(getClass().getResource("Bilder/cpu.png")));
//       Kästchen[0].add(Knopf);
//       Knopf.setBorder(null);
      GridBagConstraints Layoutregeln = new GridBagConstraints();
      Layoutregeln.weightx=1.0;
      Layoutregeln.insets = new Insets(0,2,0,0);
      Plotterinfofeld = new JLabel[5];
      for (int i=0; i<5; ++i)
      {
         Plotterinfofeld[i] = new JLabel();
         Plotterinfofeld[i].setOpaque(Texthintergrund);
         Plotterinfofeld[i].setFont(Plotterinfofeld[i].getFont().deriveFont((float) 10));
         Plotterinfofeld[i].setBackground(Color.black);
         Plotterinfofeld[i].setText(".");
         if (i==0)
            Layoutregeln.anchor=GridBagConstraints.LINE_END;
         else
            Layoutregeln.anchor=GridBagConstraints.LINE_START;
         Layoutregeln.gridy=i;
         Plotterinfopanel.add(Plotterinfofeld[i], Layoutregeln);
      }
      Plotterinfofenster.setContentPane(Plotterinfopanel);
      Plotterinfofeld[0].setForeground(Color.white);
      Plotterinfofeld[1].setForeground(CPUColor);
      Plotterinfofeld[2].setForeground(MEMColor);
      Plotterinfofeld[3].setForeground(IOColor);
      Plotterinfofeld[4].setForeground(NETColor);
      Konfigurationsfenster = new JDialog();
      Konfigurationsfenster.setModal(false);
      Konfigurationsfenster.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      Konfigurationsfenster.setSize(360,385);
      Konfigurationsfenster.setTitle("Configuration");
      Konfigurationsfenster.setLocation(Hintergrundfenster.getLocation().x+Länge, Hintergrundfenster.getLocation().y);
      Konfigurationspanel = new leeresJPanel(new GridBagLayout());
      Layoutregeln = new GridBagConstraints();
      Layoutregeln.gridx=0; Layoutregeln.gridy=0;
      Konfigurationspanel.add(new JLabel("Length"), Layoutregeln);
      Längenauswahl=new JSpinner(new SpinnerNumberModel((int)Länge, 10, 1500, 1)); Breitenauswahl = new JSpinner(new SpinnerNumberModel((int)Breite,10,1000,1)); Textgrößenauswahl = new JSpinner(new SpinnerNumberModel((int)Textgröße,1,20,1));
      Layoutregeln.gridx=1; Layoutregeln.gridy=0;
      Konfigurationspanel.add(Längenauswahl, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=1;
      Konfigurationspanel.add(new JLabel("Width"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=1;
      Konfigurationspanel.add(Breitenauswahl, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=2;
      Konfigurationspanel.add(new JLabel("Font size"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=2;
      Konfigurationspanel.add(Textgrößenauswahl,Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=3;
      Konfigurationspanel.add(new JLabel("Safe mode (no stored password)"), Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=4;
      Konfigurationspanel.add(new JLabel("Layout"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=4;
      Konfigurationspanel.add(Layoutwähler, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridwidth=2; Layoutregeln.gridy=5;
      Konfigurationspanel.add(Texthintergrundcb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=6; Layoutregeln.gridwidth=2; Layoutregeln.anchor=GridBagConstraints.LINE_START;
      Konfigurationspanel.add(farbigerTextcb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=6; Layoutregeln.gridwidth=2; Layoutregeln.anchor=GridBagConstraints.LINE_END;
      Konfigurationspanel.add(Farbfensterknopf, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=7; Layoutregeln.gridwidth=2; Layoutregeln.anchor=GridBagConstraints.CENTER;
      Konfigurationspanel.add(Einheitencb, Layoutregeln);
//       Layoutregeln.gridx=0; Layoutregeln.gridy=9;
//       Konfigurationspanel.add(Bildercb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=8;
      Konfigurationspanel.add(Randcb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=9;
      Konfigurationspanel.add(SMEMcb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=10;
      Konfigurationspanel.add(Transparenzcb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=11; Layoutregeln.gridwidth=2;
      Konfigurationspanel.add(Rechtecb, Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridwidth=1; Layoutregeln.gridy=12;
      Konfigurationspanel.add(Bestätigungsknopf, Layoutregeln); 
      Layoutregeln.gridx=1; Layoutregeln.gridy=12;
      Konfigurationspanel.add(Schließungsknopf, Layoutregeln);
      Konfigurationsfenster.setContentPane(Konfigurationspanel);

      Farblabelfeld = new JLabel[3];
      for (int i=0; i<3; ++i)
      {
         Farblabelfeld[i] = new JLabel("     ");
         Farblabelfeld[i].setOpaque(true);
      }
      Farbfenster.setTitle("Colour configuration");
      Farbpanel = new leeresJPanel(new GridBagLayout());
      Layoutregeln = new GridBagConstraints();
      Layoutregeln.insets = new Insets(2,2,2,2);
      Layoutregeln.gridx=0; Layoutregeln.gridy=0;
      Farbpanel.add(new JLabel("KB(/s):"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=0;
      Farblabelfeld[0].setBackground(Einheitenfarben[0]);
      Farbpanel.add(Farblabelfeld[0], Layoutregeln);
      Layoutregeln.gridx=2; Layoutregeln.gridy=0;
      Farbpanel.add(Farbauswahlknöpfe[0], Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=1;
      Farbpanel.add(new JLabel("MB(/s):"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=1;
      Farblabelfeld[1].setBackground(Einheitenfarben[1]);
      Farbpanel.add(Farblabelfeld[1], Layoutregeln);
      Layoutregeln.gridx=2; Layoutregeln.gridy=1;
      Farbpanel.add(Farbauswahlknöpfe[1], Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=2;
      Farbpanel.add(new JLabel("GB(/s):"), Layoutregeln);
      Layoutregeln.gridx=1; Layoutregeln.gridy=2;
      Farblabelfeld[2].setBackground(Einheitenfarben[2]);
      Farbpanel.add(Farblabelfeld[2], Layoutregeln);
      Layoutregeln.gridx=2; Layoutregeln.gridy=2;
      Farbpanel.add(Farbauswahlknöpfe[2], Layoutregeln);
      Layoutregeln.gridx=0; Layoutregeln.gridy=3; Layoutregeln.gridwidth=3;
      Farbpanel.add(Farbrücksetzungsknopf, Layoutregeln);
      Farbfenster.setContentPane(Farbpanel);
      JPanel Filterpanel = new leeresJPanel();
      Filterfeld = new JTextField(10);
      Filterfeld.addMouseListener(Mausadapter);
      JLabel Prozesslabel = new JLabel("Process name:"), Filterrücksetzungslabel = new JLabel("Clear"), Fensterschließlabel = new JLabel("Close");
      Prozesslabel.setForeground(Color.white); Filterrücksetzungslabel.setForeground(Color.white); Fensterschließlabel.setForeground(Color.white);
      Filterrücksetzungslabel.addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            Filterfeld.setText("");
         }
      });
      Fensterschließlabel.addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            Filterfenster.dispose();
         }
      });
      Filterpanel.add(Prozesslabel);
      Filterpanel.add(Filterfeld);
      Filterpanel.add(Filterrücksetzungslabel);
      Filterpanel.add(Fensterschließlabel);
      Filterfenster.setContentPane(Filterpanel);
      Filterfenster.addMouseMotionListener(new MouseMotionAdapter()
      {
         @Override
         public void mouseDragged(MouseEvent Ereignis)
         {
            Filterfenster.setLocation(Filterfenster.getLocation().x + Ereignis.getX(), Filterfenster.getLocation().y + Ereignis.getY());
         }
      });
      Optionspanel = new leeresJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      Optionsfelder = new JLabel[6];
      Optionsfelder[0] = new JLabel("Filter");
      Optionsfelder[0].setForeground(Color.white);
      Optionsfelder[0].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            Filterfenster.setLocation(Hintergrundfenster.getLocation().x, Hintergrundfenster.getLocation().y-300);
            if (Filterfenster.isVisible()==true)
               Filterfenster.dispose();
            else
               Filterfenster.setVisible(true);
         }
      });
      Optionsfelder[1] = new JLabel("Config");
      Optionsfelder[1].setOpaque(false);
      Optionsfelder[1].setForeground(Color.white);
      Optionsfelder[1].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            Konfigurationsfenster.setLocation(400, 200);
            Konfigurationsfenster.setVisible(true);
         }
      });
      Optionsfelder[2] = new JLabel("Pause");
      Optionsfelder[2].setOpaque(false);
      Optionsfelder[2].setForeground(Color.white);
      Optionsfelder[2].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if (!Pause)
            {
               Pause=true;
               Optionsfelder[2].setText("Unpause");
            }
            else
            {
               Pause=false;
               Optionsfelder[2].setText("Pause");
            }
         }
      });
      Optionsfelder[3] = new JLabel("Keep open");
      Optionsfelder[3].setOpaque(false);
      Optionsfelder[3].setForeground(Color.white);
      Optionsfelder[3].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if (offeneFenster)
            {
               offeneFenster=false;
               Optionsfelder[3].setText("Keep open");
            }
            else
            {
               offeneFenster=true;
               Optionsfelder[3].setText("Auto-hide");
            }
         }
      });
      Optionsfelder[4] = new JLabel("Details");
      Optionsfelder[4].setOpaque(false);
      Optionsfelder[4].setForeground(Color.white);
      Optionsfelder[4].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if (Plotterfenster.isVisible())
            {
               Plotterfenster.dispose();
               Detailsfenster.setVisible(true);
               Ansicht=2;
            }
            else if (Prozessfenster.isVisible())
            {
               Prozessfenster.dispose();
               Detailsfenster.setVisible(true);
               Ansicht=2;
            }
            else if(Detailsfenster.isVisible())
            {
               Detailsfenster.dispose();
               Plotterfenster.setVisible(true);
               Ansicht=0;
            }
         }
      });
      Optionsfelder[5] = new JLabel("< View >");
      Optionsfelder[5].setOpaque(false);
      Optionsfelder[5].setForeground(Color.white);
      Optionsfelder[5].addMouseListener(
      new Standardadapter()
      {
         public void mouseClicked(MouseEvent Ereignis)
         {
            if (Plotterfenster.isVisible())
            {
               Plotterfenster.dispose();
               Prozessfenster.setVisible(true);
               Ansicht=1;
            }
            else if (Prozessfenster.isVisible())
            {
               Prozessfenster.dispose();
               Plotterfenster.setVisible(true);
               Ansicht=0;
            }
            else if(Detailsfenster.isVisible())
            {
               Detailsfenster.dispose();
               Plotterfenster.setVisible(true);
               Ansicht=0;
            }
         }
      });
      for (int i=0; i<6; ++i)
      {
         JPanel Randpanel= new leeresJPanel();
         Randpanel.setBorder(BorderFactory.createLineBorder(Color.white));
         Randpanel.setPreferredSize(new Dimension(75,27));
         Randpanel.add(Optionsfelder[i]);
         Optionspanel.add(Randpanel);
      }
      Optionsfenster.add(Optionspanel);

      SystemTray Systemtray=null;
      TrayIcon Trayicon=null;
      if (SystemTray.isSupported())
      {
         Systemtray = SystemTray.getSystemTray();
         Trayicon = new TrayIcon(new ImageIcon (getClass().getResource("Pictures/Trayicon.png")).getImage());
         PopupMenu Popupmenu = new PopupMenu();
         MenuItem ConfigItem = new MenuItem("Config");
         MenuItem ExitItem = new MenuItem("Exit");
         
         Trayicon.setImageAutoSize(true);
         Trayicon.setToolTip("BusyTasks");
         Trayicon.addMouseListener(new Standardadapter()
         {
            public void mouseClicked(MouseEvent Ereignis)
            {
               if (Ereignis.getButton()==MouseEvent.BUTTON1)
               {
                  if (Hintergrundfenster.isVisible())
                     Hintergrundfenster.setVisible(false);
                  else
                     Hintergrundfenster.setVisible(true);
               }
            }
         });
         ConfigItem.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent Ereignis)
            {
               Konfigurationsknopf.doClick();
            }
         });
         ExitItem.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent Ereignis)
            {
               Beendigungsknopf.doClick();
            }
         });
         Popupmenu.add(ConfigItem);
         Popupmenu.addSeparator();
         Popupmenu.add(ExitItem);
         Trayicon.setPopupMenu(Popupmenu);
      }
      try
      {
         Threads = new Thread[3];
         if (erweiterteRechte)
         {
            Threads[0] = führeBefehlaus(1);
            Threads[1] = führeBefehlaus(2);
         }
         else
         {
            Textfeld[2].setText("IO: DISABLED");
            Textfeld[7].setText("NET: DISABLED");
         }
         if (verwendeSMEM)
         {
            SMEMTimer = new Timer();
            setzeTimer(SMEMTimer, 60000);
         }
         Sekundentimer = new Timer();
         setzeTimer(Sekundentimer,1000);
         if (erweiterteRechte)
         {
            Fehlertimer = new Timer();
            setzeTimer(Fehlertimer, 7000);
         }

         if (Systemtray.isSupported())
            Systemtray.add(Trayicon);
      }
      catch (Exception Ausnahme)
      {
         zeigeAusnahme(Ausnahme);
      }
   }
   public void erzeugeRahmen(int Knopfindex)
   {
      for (int i=0; i<4; ++i)
      {
         if (i==Knopfindex)
            Detailslabels[i].setBorder(BorderFactory.createLineBorder(Color.white));
         else
            Detailslabels[i].setBorder(BorderFactory.createEmptyBorder());
      }
   }
   public void actionPerformed(ActionEvent Ereignis)
   {
      if (Ereignis.getSource()==Farbfensterknopf)
      {
         Farbfenster.setLocation(Konfigurationsfenster.getLocation().x+Konfigurationsfenster.getSize().width, Konfigurationsfenster.getLocation().y);
         Farbfenster.setVisible(true);
      }
      else if (Ereignis.getSource()==Beendigungsknopf)
         System.exit(0);
      else if (Ereignis.getSource()==Popupknopf)
      {
         if (Hintergrundfenster.isVisible())
         {
            Hintergrundfenster.setVisible(false);
            MinimiertesFenster = new JFrame("BusyTasks");
            MinimiertesFenster.setSize(100,100);
            MinimiertesFenster.setVisible(true);
            MinimiertesFenster.setState(JFrame.ICONIFIED);
            MinimiertesFenster.addWindowStateListener(
            new WindowStateListener()
            {
               public void windowStateChanged(WindowEvent WE)
               {
                  if (WE.getNewState() == JFrame.NORMAL)
                  {
                     Hintergrundfenster.setVisible(true);
                     MinimiertesFenster.dispose();
                  }
               }
            });
         }
         else
            Hintergrundfenster.setVisible(true);
      }
      else if (Ereignis.getSource()==Konfigurationsknopf)
      {
         Konfigurationsfenster.setLocation(Hintergrundfenster.getLocation().x+Länge, Hintergrundfenster.getLocation().y);
         Konfigurationsfenster.setVisible(true);
      }
      else if (Ereignis.getSource()==Schließungsknopf)
         Konfigurationsfenster.dispose();
      else if (Ereignis.getSource()==Bestätigungsknopf)
      {
         transparent = Transparenzcb.isSelected();
         if (transparent)
         {
            Hintergrundfenster.setBackground(new Color(0,0,0,180));
            Plotterinfofenster.setBackground(new Color(0,0,0,180));
            Plotterfenster.setBackground(new Color(0,0,0,180));
            Prozessfenster.setBackground(new Color(0,0,0,180));
            Detailsfenster.setBackground(new Color(0,0,0,180));
            Filterfenster.setBackground(new Color(0,0,0,180));
         }
         else
         {
            Hintergrundfenster.setBackground(new Color(0,0,0,255));
            Plotterinfofenster.setBackground(new Color(0,0,0,255));
            Plotterfenster.setBackground(new Color(0,0,0,255));
            Prozessfenster.setBackground(new Color(0,0,0,255));
            Detailsfenster.setBackground(new Color(0,0,0,255));
            Filterfenster.setBackground(new Color(0,0,0,255));
         }
         Länge=(int)Längenauswahl.getValue();
         Breite=(int)Breitenauswahl.getValue();
         Hintergrundfenster.setSize(Länge,Breite);
         Textgröße=(int)Textgrößenauswahl.getValue();
         for(int i=0; i<10; ++i)
            Textfeld[i].setFont(Textfeld[i].getFont().deriveFont((float) Textgröße));
         boolean alteRechte = erweiterteRechte;
         erweiterteRechte = Rechtecb.isSelected();

         Hintergrundfenster.repaint();
         for (int i=0; i<10; ++i)
         {
            Textfeld[i].setForeground(Color.white);
            Textfeld[i].setBackground(Color.black);
         }
         Rand=Randcb.isSelected();
         if (Rand)
            Hintergrundfenster.getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
         Texthintergrund=Texthintergrundcb.isSelected();
         for (int i=0; i<10; ++i)
         {
            Textfeld[i].setOpaque(Texthintergrund);
            if (i<5)
               Plotterinfofeld[i].setOpaque(Texthintergrund);
         }
         ZeigeEinheiten=Einheitencb.isSelected();
         Layoutwahl=Layoutwähler.getSelectedIndex();
         GridLayout Gridlayout = (GridLayout) meinBewegungspanel.getLayout();
         if (Layoutwahl==0)
         {
            Gridlayout.setColumns(1); Gridlayout.setRows(4);
         }
         else if (Layoutwahl==1)
         {
            Gridlayout.setRows(1); Gridlayout.setColumns(4);
         }
         else
         {
            Gridlayout.setRows(2); Gridlayout.setColumns(2);
         }
         farbigerText = farbigerTextcb.isSelected();
         ZeigeBilder = Bildercb.isSelected();
         verwendeSMEM = SMEMcb.isSelected();
         speichern();
         if (verwendeSMEM)
         {
            if(SMEMTimer==null)
            {
               SMEMTimer = new Timer();
               setzeTimer(SMEMTimer, 60000);
            }
         }
         else
         {
            if (SMEMTimer!=null)
            {
               SMEMTimer.cancel();
               SMEMTimer = null;
            }
         }
         if (alteRechte != erweiterteRechte)
            neustarten();
      }
      else if (Ereignis.getSource()==Farbauswahlknöpfe[0])
      {
         Einheitenfarben[0]=JColorChooser.showDialog(Farbfenster, "Colour chooser", Einheitenfarben[0]);
         Farblabelfeld[0].setBackground(Einheitenfarben[0]);
         speichern();
      }
      else if (Ereignis.getSource()==Farbauswahlknöpfe[1])
      {
         Einheitenfarben[1]=JColorChooser.showDialog(Farbfenster, "Colour chooser", Einheitenfarben[1]);
         Farblabelfeld[1].setBackground(Einheitenfarben[1]);
         speichern();
      }
      else if (Ereignis.getSource()==Farbauswahlknöpfe[2])
      {
         Einheitenfarben[2]=JColorChooser.showDialog(Farbfenster, "Colour chooser", Einheitenfarben[2]);
         Farblabelfeld[2].setBackground(Einheitenfarben[2]);
         speichern();
      }
      else if (Ereignis.getSource()==Farbrücksetzungsknopf)
      {
         Einheitenfarben[0] = new Color(230,230,0);
         Einheitenfarben[1] = new Color(255,100,255);
         Einheitenfarben[2] = new Color(100,175,255);
         for (int i=0; i<3; ++i)
            Farblabelfeld[i].setBackground(Einheitenfarben[i]);
         speichern();
      }
   }
   // Stores only display/preferences settings using a bounded, non-executable text format.
   // Never reads the legacy Java-deserialization configuration containing a password.
   private void speichern()
   {
      Länge = Hintergrundfenster.getWidth();
      Breite = Hintergrundfenster.getHeight();
      Positionx = Hintergrundfenster.getLocation().x;
      Positiony = Hintergrundfenster.getLocation().y;
      java.util.Properties daten = new java.util.Properties();
      daten.setProperty("length", String.valueOf(Länge));
      daten.setProperty("width", String.valueOf(Breite));
      daten.setProperty("x", String.valueOf(Positionx));
      daten.setProperty("y", String.valueOf(Positiony));
      daten.setProperty("fontSize", String.valueOf(Textgröße));
      daten.setProperty("textBackground", String.valueOf(Texthintergrund));
      daten.setProperty("showUnits", String.valueOf(ZeigeEinheiten));
      daten.setProperty("layout", String.valueOf(Layoutwahl));
      daten.setProperty("coloredText", String.valueOf(farbigerText));
      daten.setProperty("border", String.valueOf(Rand));
      daten.setProperty("icons", String.valueOf(ZeigeBilder));
      daten.setProperty("transparent", String.valueOf(transparent));
      daten.setProperty("smem", String.valueOf(verwendeSMEM));
      daten.setProperty("enablePolicyKit", String.valueOf(erweiterteRechte));
      for (int i=0; i<3; ++i)
         daten.setProperty("color"+i, String.valueOf(Einheitenfarben[i].getRGB()));
      try { SafeConfig.save(daten); }
      catch (IOException ausnahme) { zeigeAusnahme(ausnahme); }
   }

   private boolean laden()
   {
      try
      {
         java.util.Properties p = SafeConfig.load();
         Länge = SafeConfig.integer(p,"length", Länge, 10,1500);
         Breite = SafeConfig.integer(p,"width", Breite, 10,1000);
         Positionx = SafeConfig.integer(p,"x", Positionx, -10000,10000);
         Positiony = SafeConfig.integer(p,"y", Positiony, -10000,10000);
         Textgröße = SafeConfig.integer(p,"fontSize", Textgröße, 1,20);
         Texthintergrund = Boolean.parseBoolean(p.getProperty("textBackground","true"));
         ZeigeEinheiten = Boolean.parseBoolean(p.getProperty("showUnits","true"));
         Layoutwahl = SafeConfig.integer(p,"layout", Layoutwahl, 0,2);
         farbigerText = Boolean.parseBoolean(p.getProperty("coloredText","false"));
         Rand = Boolean.parseBoolean(p.getProperty("border","false"));
         ZeigeBilder = Boolean.parseBoolean(p.getProperty("icons","false"));
         transparent = Boolean.parseBoolean(p.getProperty("transparent","true"));
         verwendeSMEM = Boolean.parseBoolean(p.getProperty("smem","false"));
         // User can enable once in settings; new installations ALWAYS default to false.
         erweiterteRechte = Boolean.parseBoolean(p.getProperty("enablePolicyKit","false"));
         for (int i=0; i<3; ++i)
            Einheitenfarben[i] = new Color(SafeConfig.integer(p,"color"+i, Einheitenfarben[i].getRGB(), Integer.MIN_VALUE,Integer.MAX_VALUE),true);
         return true;
      }
      catch (java.nio.file.NoSuchFileException ausnahme)
      {
         // No config yet: do not show an unsolicited config dialog.
         return false;
      }
      catch (IOException ausnahme)
      {
         Fehlertext.append("\nCannot load safe settings: " + ausnahme.getMessage());
         return false;
      }
   }
   private void setzeTimer(Timer meinTimer, Integer Intervall)
   {
      if (meinTimer==Knopftimer)
      {
         meinTimer.schedule(new TimerTask()
         {
            public void run()
            {
               if(!offeneFenster)
               {
                  Filterfenster.dispose();
                  Optionsfenster.dispose();
                  Popupfenster.dispose();
                  Plotterfenster.dispose();
                  Prozessfenster.dispose();
                  Detailsfenster.dispose();
                  meinGrößenfenster.dispose();
               }
            }
         }, Intervall);
      }
      else if (meinTimer==Fehlertimer)
      {
         meinTimer.schedule(new TimerTask()
         {
            public void run()
            {
               boolean Fehler=true;
               if (!iotopgestartet && nethogsgestartet)
                  Fehlertext.append("\nCould not start iotop (PolicyKit authorization or package missing).");
               else if (iotopgestartet && !nethogsgestartet)
                  Fehlertext.append("\nCould not start nethogs. This may be due to an incorrect version. Please download and install latest version! https://github.com/raboof/nethogs");
               else if (!iotopgestartet && !nethogsgestartet)
                  Fehlertext.append("\nCould not start iotop and nethogs (PolicyKit authorization or packages missing).");
               else
                  Fehler=false;
               if (Fehler)
               {
                  Fehlerfenster.setLocation(Hintergrundfenster.getLocation().x+Länge, Hintergrundfenster.getLocation().y);
                  Fehlerfenster.setVisible(true);
               }
            }
         }, Intervall);
      }
      else if (meinTimer==FehlendeKonfigTimer)
      {
         meinTimer.schedule(new TimerTask()
         {
            public void run()
            {
               Konfigurationsfenster.setVisible(true);
            }
         }, Intervall);
      }
      else if(meinTimer==Sekundentimer)
      {
         meinTimer.scheduleAtFixedRate(new TimerTask()
         {
            public void run()
            {
               try
               {
                  Prozesse = Betriebssystem.getProcesses(null, OperatingSystem.ProcessSorting.PID_ASC, 0);
                  if(!Filterfeld.getText().equals(""))
                     filtereProzesse();
                  zeigeCPUProzess();
                  if(!verwendeSMEM)
                     zeigeMEMProzess();
                  if (fügeDatenein)
                  {
//                      fügeDatenein=false;
                     for (int i=0; i<4; ++i)
                     {
                        meinGraph.hinzufügen(i, AktuelleGraphdaten[i]);
                        if (!verwendeSMEM || i!=1)
                           AktuelleGraphdaten[i] = new Graphdaten(0, "[––] 0.00 [––] 0.00 ");                          //Vermeidung von Duplikaten, falls Update zu spät kommt.
                     }
                     if (!Pause)
                        meinGraph.repaint();
                  }
                  else
                     fügeDatenein=true;
                  PIDProzessMapping_alt = PIDProzessMapping;
                  PIDProzessMapping = new Hashtable<Integer, OSProcess>();
               }
               catch (Exception Ausnahme)
               {
                  zeigeAusnahme(Ausnahme);
               }
            }
         },0,Intervall);
      }
      else if (meinTimer==SMEMTimer)
         meinTimer.scheduleAtFixedRate(new TimerTask()
         {
            public void run()
            {
               try
               {
                  Threads[2] = führeBefehlaus(3);
               }
               catch (Exception Ausnahme)
               {
                  zeigeAusnahme(Ausnahme);
               }
            }
         },0, Intervall);
   }
   private void filtereProzesse()
   {
      List<OSProcess> gefilterteProzesse = new ArrayList<OSProcess>();
      for (int i=0; i<Prozesse.size(); ++i)
         if(Prozesse.get(i).getName().contains(Filterfeld.getText()))
            gefilterteProzesse.add(Prozesse.get(i));
      Prozesse = gefilterteProzesse;
   }
   // Strict executable allowlist; no shell and no PATH-based program execution.
   private static String systemProgramm(String name) throws IOException
   {
      if (!java.util.Set.of("pkexec", "iotop", "nethogs", "smem", "ss", "cat", "ls").contains(name))
         throw new IOException("Executable not allowed: " + name);
      for (String dir : new String[]{"/usr/bin", "/usr/sbin", "/bin", "/sbin"})
      {
         Path path = Path.of(dir, name);
         if (Files.isRegularFile(path) && Files.isExecutable(path))
            return path.toRealPath().toString();
      }
      throw new IOException("Required tool not installed: " + name);
   }

   private boolean verifiziereIotop(int option)
   {
      try
      {
         Process p = new ProcessBuilder(systemProgramm("iotop"), "--version").start();
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())))
         {
            String version = reader.readLine();
            // iotop implementations have different version text; avoid fragile numeric parsing.
            return version != null && !version.isBlank();
         }
      }
      catch (IOException ex)
      {
         Fehlertext.append("\niotop unavailable: " + ex.getMessage());
         return false;
      }
   }

   private void zeigeProzessCPUInfo(String PID)
   {
      erzeugeRahmen(Detailsansicht);
      try
      {
         int PIDzahl = Integer.parseInt(PID);
         OSProcess OSProzess = Betriebssystem.getProcess(PIDzahl);
         if (OSProzess!=null)
         {
            Prozessauswahlfeld.setText(PID);
            Detailstext.setText("");
            Prozessnamensfeld.setText(OSProzess.getName());
            double CPUZeitGesamt = Long.valueOf(OSProzess.getKernelTime() + OSProzess.getUserTime()).doubleValue()/1000.0;
            int CPUZeitStunden = (int)(CPUZeitGesamt / 3600);
            int CPUZeitMinuten = (int)(CPUZeitGesamt / 60.0) % 60;
            double CPUZeitSekunden = CPUZeitGesamt % 60;
            DecimalFormat DF0 = new DecimalFormat("00");
            DecimalFormat DF1 = new DecimalFormat("00.00");
            Detailstext.append("Executed command:\t" + OSProzess.getCommandLine());
            Detailstext.append("\nCurrent CPU%:\t\t" + DF1.format(PIDCPUMapping.get(PIDzahl)) + "%");
            Detailstext.append("\nAverage CPU%:\t" + DF1.format(OSProzess.getProcessCpuLoadCumulative()*100/CpuKernzahl) + "%");
            Detailstext.append("\nCPU time:\t\t" + DF0.format(CPUZeitStunden) + ":" + DF0.format(CPUZeitMinuten) + ":" + DF1.format(CPUZeitSekunden));
            Detailstext.append("\nStart date:\t\t" + new Date(OSProzess.getStartTime()));
            Detailstext.append("\nNumber of threads:\t" + OSProzess.getThreadCount());
            int a=0;
            for (int i=0; i<meinGraph.Größe; ++i)
               if(meinGraph.Texte[0][i].contains(OSProzess.getName()))
                  ++a;
            Detailstext.append("\n% of occurrences on top:\t" + DF1.format((Double.valueOf(a)/Double.valueOf(meinGraph.Größe))*100) +"%");
         }
         else
            throw new NumberFormatException();
      }
      catch (NumberFormatException Ausnahme)
      {
         Prozessauswahlfeld.setText("");
         Prozessnamensfeld.setText("");
         Detailstext.setText("Invalid PID!");
      }
   }
   private static String naechsteDetailzeile(BufferedReader reader, int option, int pid) throws IOException
   {
      String zeile;
      while ((zeile = reader.readLine()) != null)
      {
         if (option==1 && !zeile.matches(".*\\[.*\\].*")) continue;
         if (option==10)
         {
            int slash=zeile.indexOf('/');
            if (slash<0) continue;
            return zeile.substring(slash);
         }
         if (option==2 && !(zeile.startsWith("read_bytes:") || zeile.startsWith("write_bytes:"))) continue;
         if (option==20 && !zeile.contains(" -> /")) continue;
         if (option==20) return zeile.substring(zeile.indexOf(" -> /")+4);
         if (option==3 && !zeile.contains("pid="+pid+",")) continue;
         if (option==3 && zeile.trim().split("\\s+").length<6) continue;
         return zeile;
      }
      return null;
   }

   private Thread getinfo(String PID, int option)
   {
      Thread meinThread = new Thread(new Runnable()
      {
         public void run()
         {
            erzeugeRahmen(Detailsansicht);
            try
            {
               int PIDzahl = Integer.parseInt(PID);
               OSProcess OSProzess = Betriebssystem.getProcess(PIDzahl);
               if (OSProzess!=null)
               {
                  Prozessnamensfeld.setText(OSProzess.getName());
                  ProcessBuilder pb;
                  if (option==1 || option==10)
                     pb = new ProcessBuilder(systemProgramm("cat"), "/proc/" + PIDzahl + "/maps");
                  else if (option==2)
                     pb = new ProcessBuilder(systemProgramm("cat"), "/proc/" + PIDzahl + "/io");
                  else if (option==20)
                     pb = new ProcessBuilder(systemProgramm("ls"), "-l", "/proc/" + PIDzahl + "/fd");
                  else if (option==3)
                     pb = new ProcessBuilder(systemProgramm("ss"), "-pratu");
                  else
                     throw new IOException("Invalid view option");
                  // Least privilege: own user's /proc entries, never sudo, shell or pkexec.
                  Process meinProzess = pb.start();
                  BufferedReader Input = new BufferedReader(new InputStreamReader(meinProzess.getInputStream()));
                  String letzteZeile = new String("");
                  Prozessauswahlfeld.setText(PID);
                  if (option!=10 && option!=20)
                     Detailstext.setText("");
                  if (option == 10 || option == 20)
                  {
                     if (option == 10)
                        Detailstext.append("\nFILES MAPPED IN MEMORY:\n");
                     Set<String> Zeilensatz = new LinkedHashSet<String>();
                     while((letzteZeile=naechsteDetailzeile(Input, option, PIDzahl))!=null)
                        Zeilensatz.add(letzteZeile);
                     for (String Zeile : Zeilensatz)
                        Detailstext.append(Zeile + "\n");
                  }
                  else if (option == 1)
                  {
                     Detailstext.append("Resident set size: " + spalteDatenauf(OSProzess.getResidentSetSize())+"\n\n");
                     while ((letzteZeile=naechsteDetailzeile(Input, option, PIDzahl))!=null)
                     {
                        String Zeilenteile[] = letzteZeile.trim().split(" ");
                        Detailstext.append(Zeilenteile[0] + "\t" + Zeilenteile[Zeilenteile.length-1] +"\n");
                     }
                     getinfo(PID, 10);
                  }
                  else if (option == 2)
                  {
                     int i=0;
                     while((letzteZeile=naechsteDetailzeile(Input, option, PIDzahl))!=null && i<2)
                     {
                        long Rohwert = Long.parseLong(letzteZeile.trim().split(" ")[1]);
                        if (i==0)
                           Detailstext.append("Total bytes read: \t" + spalteDatenauf(Rohwert) + "\n");
                        else
                           Detailstext.append("Total bytes written: \t" + spalteDatenauf(Rohwert) + "\n");
                        ++i;
                     }
                     Detailstext.append("\n");
                     getinfo(PID, 20);
                  }
                  else if (option == 3)
                  {
                     double empfangeneBytes = 0d, gesendeteBytes = 0d;
                     NETProzess NETAbbild = PIDNETProzessMapping.get(OSProzess.getProcessID());
                     if (NETAbbild!=null)
                     {
                        empfangeneBytes = NETAbbild.kumEmpfangen;
                        gesendeteBytes = NETAbbild.kumGesendet;
                     }
                     Detailstext.append("Total bytes received:\t" + spalteDatenauf((long)(empfangeneBytes*1024d)));
                     Detailstext.append("\nTotal bytes sent:\t"+ spalteDatenauf((long)(gesendeteBytes*1024d)) + "\n\n");

                     while((letzteZeile=naechsteDetailzeile(Input, option, PIDzahl))!=null)
                     {
                        String Zeilenteile[] = letzteZeile.trim().split("\\s+");
                        String neueZeile = Zeilenteile[0] + "\t" + Zeilenteile[1] + "\t" + Zeilenteile[4] + "\t" + Zeilenteile[5] + "\n";
                        Detailstext.append(neueZeile);
                     }
                  }
               }
               else
                  throw new NumberFormatException();
            }
            catch (NumberFormatException Ausnahme)
            {
               Prozessauswahlfeld.setText("");
               Prozessnamensfeld.setText("");
               Detailstext.setText("Invalid PID!");
            }
            catch (Exception Ausnahme)
            {
               zeigeAusnahme(Ausnahme);
            }
            Detailsscrollpane.revalidate();           // Scrollbarreadaption nach Textänderung
         }
      });
      meinThread.start();
      return meinThread;
   }
   private Thread führeBefehlaus(final Integer option)
   {
      Thread meinThread = new Thread(new Runnable()
      {
         public void run()
         {
            try
            {
               ProcessBuilder Prozessstarter;
               if (option==1 || option==2)
               {
                  if (!erweiterteRechte) return;
                  if (option==1 && !verifiziereIotop(0)) return;
                  String executable = systemProgramm(option==1 ? "iotop" : "nethogs");
                  // PolicyKit handles the prompt: no password enters the JVM.
                  Prozessstarter = option==1
                     ? new ProcessBuilder(systemProgramm("pkexec"), executable, "-Pkqqbo")
                     : new ProcessBuilder(systemProgramm("pkexec"), executable, "-Ct");
               }
               else
                  Prozessstarter = new ProcessBuilder(systemProgramm("smem"), "-Htrc", "pss pid name");
               Process meinProzess = Prozessstarter.start();
               BufferedReader Input = new BufferedReader(new InputStreamReader(meinProzess.getInputStream()));
               String letzteZeile=null, Texte[] = new String[3];
               if (option==1)
               {
                  ArrayList<String> Datenzeilen = new ArrayList<String>();
                  String Prozess[] =new String[10];
                  Double Datenrate = 0.0, Summe[][] = new Double[10][];
                  for (int i=0; i<10; ++i)
                     Summe[i]=new Double[3];
                  DecimalFormat DF = new DecimalFormat("0.00");
                  letzteZeile=Input.readLine();
                  while (letzteZeile!=null && !letzteZeile.contains("DISK"))
                  {
                     Fehlertext.append("\n" + letzteZeile);
                     letzteZeile=Input.readLine();
                  }
                  while ((letzteZeile=Input.readLine())!=null)
                  {
                     if(letzteZeile.contains("DISK"))
                     {
                        for (int i=0; i<3; ++i)
                           Texte[i]=new String("");
                        iotopgestartet=true;
                        if (Datenzeilen.size()>0)
                        {
                           if (!Filterfeld.getText().equals(""))
                           {
                              for (int i=Datenzeilen.size()-1; i>=0; --i)
                                 if(!Datenzeilen.get(i).contains(Filterfeld.getText()))
                                    Datenzeilen.remove(i);
                           }
                           for (int i=Datenzeilen.size()-1; i>=0; --i)
                              if (!Datenzeilen.get(i).contains("K/s"))
                                 Datenzeilen.remove(i);
                           String Datenfeld[][] = new String[Datenzeilen.size()][];
                           for(int i=0; i<Datenzeilen.size(); ++i)
                              Datenfeld[i]=Datenzeilen.get(i).split("\\s+");                                  //Auftrennung nach Leerzeichen
                           for (int i=0; i<Datenzeilen.size(); ++i)
                           {
                              Datenfeld[i][3] = Datenfeld[i][3].replace(',', '.');                 //iotop-c orientiert sich an lokalen Kommazeichen
                              Datenfeld[i][5] = Datenfeld[i][5].replace(',', '.');
                           }
                           Arrays.sort(Datenfeld, new Comparator<String[]>()
                           {
                              public int compare(String erstesFeld[], String zweitesFeld[])
                              {
                                 return Double.compare(Double.parseDouble(zweitesFeld[3])+Double.parseDouble(zweitesFeld[5]), Double.parseDouble(erstesFeld[3])+Double.parseDouble(erstesFeld[5]));
                              }
                           });
                           int NI = 0;
                           if (Datenzeilen.size()>0)
                           {
                              if (Datenzeilen.get(0).contains("?unavailable?"))
                                 NI = 8;
                              else
                                 NI = 11;
                           }
                           Datenzeilen.clear();

                           for (int k=0; k<Datenfeld.length && k<6; ++k)
                           {
                              Double Lesen = Double.parseDouble(Datenfeld[k][3]), Schreiben = Double.parseDouble(Datenfeld[k][5]), Gesamtwert = Lesen+Schreiben;
                              Double Verhältnis = Lesen*100/Gesamtwert;
                              if(Gesamtwert!=0)
                              {
                                 Texte[0] += formatiereVerhältnis(Verhältnis) + " " + DatenFormatieren(Gesamtwert, true) + "\n";
                                 Texte[1] += Datenfeld[k][0] + "\n";
                                 String name = new String("");
                                 for (int e=NI; e<Datenfeld[k].length; ++e)
                                    name += Datenfeld[k][e] + " ";
                                 if (name.length()>17)
                                    name=name.substring(0,17);
                                 Texte[2] += name + "\n";
                              }
                           }
                           Double Datenrate_Lesen=0.0, Datenrate_Schreiben=0.0;
                           if(Datenfeld.length>0)
                           {
                              String name = new String("");
                              for (int e=NI; e<Datenfeld[0].length; ++e)
                                 name += Datenfeld[0][e] + " ";
                              if (name.length()>17)
                                 name=name.substring(0,17);
                              Prozess[0]=name.trim();
                              Datenrate_Lesen=Double.parseDouble(Datenfeld[0][3]);
                              Datenrate_Schreiben = Double.parseDouble(Datenfeld[0][5]);
                           }
                           else
                              Prozess[0]="";
                           Datenrate = Datenrate_Lesen + Datenrate_Schreiben;
                           Double Datenrate_Verhältnis = (Datenrate_Lesen*100)/Datenrate;
                           Summe[0][0]=Datenrate; Summe[0][1]=Datenrate_Lesen; Summe[0][2]=Datenrate_Schreiben;

                           int LV=1;
                           for (LV=1; LV<10 && !Prozess[0].equals(Prozess[LV]); ++LV) ;
                           if (LV!=10)
                           {
                              for (int i=0; i<3; ++i)
                                 Summe[0][i] += Summe[LV][i];
                           }
                           Double Summe_Verhältnis = Summe[0][1]*100/Summe[0][0];
                           Datenvektor V_Summe = liefereDatenvektorfürWert(Summe[0][0]);
                           Datenvektor V_Rate = liefereDatenvektorfürWert(Datenrate);
                           V_Rate.Einheit+="/s";

                           String Einheiten[] = new String[4];
                           if (!ZeigeEinheiten)
                           {
                              Einheiten[0]=" ";
                              Einheiten[1]=" ";
                              Einheiten[2]=V_Summe.Einheit + " ";
                              Einheiten[3]=V_Rate.Einheit + " ";
                           }
                           else
                           {
                              Einheiten[0]=" " + V_Summe.Einheit + " ";
                              Einheiten[1]=" " + V_Rate.Einheit + " ";
                              Einheiten[2]="";
                              Einheiten[3]="";
                           }
                           if (!Pause)
                           {
                              if(Datenrate!=0)                                                              //iotop-Bug: Manchmal wird ein Prozess ohne Datenrate angezeigt
                              {
                                 if (farbigerText)
                                 {
                                    Textfeld[2].setForeground(V_Summe.Farbe);
                                    Textfeld[3].setForeground(V_Rate.Farbe);
                                 }
                                 Textfeld[2].setText(formatiereVerhältnis(Summe_Verhältnis)+ " " + DF.format(V_Summe.Wert)+ Einheiten[0]);
                                 Textfeld[3].setText(formatiereVerhältnis(Datenrate_Verhältnis) + " " +DF.format(V_Rate.Wert)+ Einheiten[1]);
                                 Textfeld[4].setText(Prozess[0]);
                              }
                              else
                              {
                                 Prozess[0]="";
                                 for (int i=0; i<3; ++i)
                                 {
                                    if (Summe[0][i]==null)
                                       Summe[0][i]=0.0;
                                 }
                                 setzeTextfelderNull(0);
                              }
                           }
                           AktuelleGraphdaten[2].Zahlenwert = Datenrate.intValue();
                           AktuelleGraphdaten[2].Textinfo = Textfeld[2].getText() + Einheiten[2] + Textfeld[3].getText() + Einheiten[3] + Textfeld[4].getText();
                        }
                        else
                        {
                           if(!Pause)
                           {
                              Prozess[0]="";
                              for (int i=0; i<3; ++i)
                              {
                                 if (Summe[0][i]==null)
                                    Summe[0][i]=0.0;
                              }
                              setzeTextfelderNull(0);
                           }
                           AktuelleGraphdaten[2].Zahlenwert = 0;
                           AktuelleGraphdaten[2].Textinfo = Textfeld[2].getText() + Textfeld[3].getText();
                        }
                        for (int i=9; i>0; --i)                      //Pipeinhalt verschieben
                        {
                           Prozess[i]=Prozess[i-1];
                           for (int j=0; j<3; ++j)
                              Summe[i][j] = Summe[i-1][j];
                        }
                        if(!Pause)
                        {
                           Prozesslisten[1][0].setText(Texte[0]);
                           Prozesslisten[1][1].setText(Texte[1]);
                           Prozesslisten[1][2].setText(Texte[2]);
                        }
                        letzteZeile=Input.readLine();
                     }
                     else
                        Datenzeilen.add(letzteZeile.trim());
                  }
               }
               else if (option==2)
               {
                  Double Datenrate=0.0, Datenrate_Verhältnis=0.0, Datenrate_Empfangen=0.0, Datenrate_Senden=0.0;
                  Double Summe[][] = new Double[10][], Summe_Verhältnis=0.0;
                  for (int i=0; i<10; ++i)
                     Summe[i] = new Double[3];
                  String Prozess[]= new String[10];
                  DecimalFormat DF = new DecimalFormat("0.00");
                  ArrayList<String> Zeilen = new ArrayList<String>();

                  while ((letzteZeile=Input.readLine())!=null)
                  {
                     if(letzteZeile.contains("Refreshing") && Zeilen.size()>0)
                     {
                        nethogsgestartet=true;
                        for (int i=0; i<3; ++i)
                           Texte[i]=new String("");

                        ArrayList<NETProzess> aktuelleNETProzesse= new ArrayList<NETProzess>();
                        for (int x=0; x<Zeilen.size(); ++x)
                        {
                           NETProzess dieserProzess = new NETProzess();
                           String Zeilenteile[] = Zeilen.get(x).split("\\s+");
                           String Namensteile[] = null;
                           if (Zeilen.get(x).charAt(0)=='u')
                           {
                              Namensteile = Zeilenteile[1].split("/");
                              dieserProzess.Name = Namensteile[0];
                           }
                           else
                           {
                              Namensteile = Zeilenteile[0].split("/");
                              dieserProzess.Name = Zeilenteile[0];
                           }
                           dieserProzess.PID = Integer.parseInt(Namensteile[Namensteile.length-2]);
                           if (dieserProzess.Name.equals("TCP"))
                              dieserProzess.PID = -1;                                                          // IP muss eindeutig sein, nethogs liefert aber für unbekannte Daten 0
                           dieserProzess.Gesendet = Double.parseDouble(Zeilenteile[Zeilenteile.length-2]);
                           dieserProzess.Empfangen = Double.parseDouble(Zeilenteile[Zeilenteile.length-1]);
                           dieserProzess.kumGesendet = dieserProzess.Gesendet;
                           dieserProzess.kumEmpfangen = dieserProzess.Empfangen;
                           NETProzess altesAbbild = PIDNETProzessMapping.get(dieserProzess.PID);
                           if (altesAbbild!=null)
                           {
                              dieserProzess.kumGesendet += altesAbbild.kumGesendet;
                              dieserProzess.kumEmpfangen += altesAbbild.kumEmpfangen;
                           }
                           aktuelleNETProzesse.add(dieserProzess);
                        }
                        Collections.sort(aktuelleNETProzesse);

                        for (int i=aktuelleNETProzesse.size()-1; i>-1; --i)
                        {
                           NETProzess altesAbbild = PIDNETProzessMapping.get(aktuelleNETProzesse.get(i).PID);
                           NETProzess aktuellerProzess = aktuelleNETProzesse.get(i);
                           if (aktuellerProzess.liefereGesamt()==0 || (altesAbbild!=null && aktuellerProzess.Empfangen==altesAbbild.Empfangen && aktuellerProzess.Gesendet==altesAbbild.Gesendet))
                              aktuelleNETProzesse.remove(i);
                        }
                        for (int i=0; i<aktuelleNETProzesse.size(); ++i)
                           PIDNETProzessMapping.put(aktuelleNETProzesse.get(i).PID, aktuelleNETProzesse.get(i));                // überschreiben
                        if (!Filterfeld.getText().equals(""))
                           for (int i=aktuelleNETProzesse.size()-1; i>-1; --i)
                              if (!aktuelleNETProzesse.get(i).Name.contains(Filterfeld.getText()))
                                 aktuelleNETProzesse.remove(i);
                        if (aktuelleNETProzesse.size()>0)
                        {
                           for (int i=0; i<aktuelleNETProzesse.size() && i<6; ++i)
                           {
                              Double Verhältnis = (aktuelleNETProzesse.get(i).Empfangen*100)/aktuelleNETProzesse.get(i).liefereGesamt();
                              Texte[0] += formatiereVerhältnis(Verhältnis) + " " + DatenFormatieren(aktuelleNETProzesse.get(i).liefereGesamt(), true) + "\n";
                              Texte[1] += aktuelleNETProzesse.get(i).PID + "\n";
                              Texte[2] += aktuelleNETProzesse.get(i).Name + "\n";
                           }
                           Datenrate_Empfangen=aktuelleNETProzesse.get(0).Empfangen; Datenrate_Senden=aktuelleNETProzesse.get(0).Gesendet;
                           Datenrate=Datenrate_Empfangen+Datenrate_Senden; Datenrate_Verhältnis=Datenrate_Empfangen*100/Datenrate;
                           Prozess[0]=aktuelleNETProzesse.get(0).Name;
                           Summe[0][0]=Datenrate; Summe[0][1]=Datenrate_Empfangen; Summe[0][2]=Datenrate_Senden;

                           int LV=0;
                           for (LV=1; LV<10 && !Prozess[0].equals(Prozess[LV]); ++LV) ;
                           if (LV!=10)                                                           //Frühere Daten übernehmen
                           {
                              for (int i=0; i<3; ++i)
                                 Summe[0][i] += Summe[LV][i];
                           }
                           Summe_Verhältnis=Summe[0][1]*100/Summe[0][0];
                           Datenvektor V_Summe = liefereDatenvektorfürWert(Summe[0][0]);
                           Datenvektor V_Rate = liefereDatenvektorfürWert(Datenrate);
                           V_Rate.Einheit+="/s";
                           String Einheiten[] = new String[4];
                           if (!ZeigeEinheiten)
                           {
                              Einheiten[0]=" ";
                              Einheiten[1]=" ";
                              Einheiten[2]=V_Summe.Einheit + " ";
                              Einheiten[3]=V_Rate.Einheit + " ";
                           }
                           else
                           {
                              Einheiten[0]=" " + V_Summe.Einheit + " ";
                              Einheiten[1]=" " + V_Rate.Einheit + " ";
                              Einheiten[2]="";
                              Einheiten[3]="";
                           }
                           if (!Pause)
                           {
                              if (farbigerText)
                              {
                                 Textfeld[7].setForeground(V_Summe.Farbe);
                                 Textfeld[8].setForeground(V_Rate.Farbe);
                              }
                              Textfeld[7].setText(formatiereVerhältnis(Summe_Verhältnis)+ " " + DF.format(V_Summe.Wert)+ Einheiten[0]);
                              Textfeld[8].setText(formatiereVerhältnis(Datenrate_Verhältnis) + " " +DF.format(V_Rate.Wert)+ Einheiten[1]);
                              Textfeld[9].setText(Prozess[0]);
                           }
                           AktuelleGraphdaten[3].Zahlenwert = Datenrate.intValue();
                           AktuelleGraphdaten[3].Textinfo = Textfeld[7].getText() + Einheiten[2] +  Textfeld[8].getText() + Einheiten[3] + Textfeld[9].getText();
                        }
                        else
                        {
                           if (!Pause)
                           {
                              Prozess[0]="";
                              for (int i=0; i<3; ++i)
                              {
                                 if (Summe[0][i]==null)
                                    Summe[0][i]=0.0;
                              }
                              setzeTextfelderNull(5);
                           }
                           AktuelleGraphdaten[3].Zahlenwert = 0;
                           AktuelleGraphdaten[3].Textinfo = Textfeld[7].getText() + Textfeld[8].getText();
                        }
                        for (int i=9; i>0; --i)                                              //Pipeinhalt verschieben
                        {
                           Prozess[i]=Prozess[i-1];
                           for (int j=0; j<3; ++j)
                              Summe[i][j]=Summe[i-1][j];
                        }
                        if(!Pause)
                        {
                           Prozesslisten[3][0].setText(Texte[0]);
                           Prozesslisten[3][1].setText(Texte[1]);
                           Prozesslisten[3][2].setText(Texte[2]);
                        }
                        Zeilen.clear();
                     }
                     else if (letzteZeile.startsWith("/") || letzteZeile.startsWith("u"))
                     {
                        Zeilen.add(letzteZeile);
                     }
                  }
               }
               else if (option==3)
               {
                  for (int i=0; i<3; ++i)
                     Texte[i]=new String("");

                  ArrayList<String> Zeilen = new ArrayList<String>();
                  while((letzteZeile=Input.readLine())!=null)
                     Zeilen.add(letzteZeile.trim());

                  String Zeilenteile[][] = new String[6][];
                  for (int i=0; i<6 && i<Zeilen.size() && i<6; ++i)
                  {
                     Zeilenteile[i]=Zeilen.get(i).split("\\s+");
                     String Prozessname = new String("");
                     for (int m=2; m<Zeilenteile[i].length; ++m)
                        Prozessname += Zeilenteile[i][m] + " ";
                     Texte[0]+=DatenFormatieren(Double.parseDouble(Zeilenteile[i][0]), true) + "\n";
                     Texte[1]+=Zeilenteile[i][1] + "\n";
                     Texte[2]+=Prozessname + "\n";
                  }
                  long genutzterSpeicher = Long.parseLong(Zeilen.get(Zeilen.size()-1).split("\\s+")[0]);
                  Double genutzterSpeicherProzent = Double.valueOf(genutzterSpeicher)*100*1024/Double.valueOf(gesamterSpeicher);
                  DecimalFormat DF = new DecimalFormat("0.00");
                  int Farbdifferenz= (int)(genutzterSpeicherProzent*2.5);
                  if (!farbigerText)
                     Farbdifferenz=0;
                  Double Speicher = 0.0;
                  Double Prozent = 0.0;
                  Speicher = Double.parseDouble(Zeilenteile[0][0]);
                  Prozent = (Speicher*100.0*1024)/(gesamterSpeicher);
                  if (!Pause)
                  {
                     Prozesslisten[2][0].setText(Texte[0]);
                     Prozesslisten[2][1].setText(Texte[1]);
                     Prozesslisten[2][2].setText(Texte[2]);
                     String Prozessname = new String("");
                     for (int m=2; m<Zeilenteile[0].length; ++m)
                        Prozessname += Zeilenteile[0][m] + " ";
                     Textfeld[5].setForeground(new Color(255-Farbdifferenz,255,255-Farbdifferenz));
                     Textfeld[5].setText(DF.format(genutzterSpeicherProzent) + "% / " + DF.format(Prozent) + "% " + DatenFormatieren(Speicher, true) + " ");
                     Textfeld[6].setText(Prozessname);
                  }
                  genutzterSpeicherProzent*=1.6;
                  AktuelleGraphdaten[1] = new Graphdaten(genutzterSpeicherProzent.intValue(), Textfeld[5].getText()+Textfeld[6].getText());
                  Zeilen.clear();
               }
            }
            catch (Exception Ausnahme)
            {
               zeigeAusnahme(Ausnahme);
            }
         }
      });
      meinThread.start();
      return meinThread;
   }
   private void zeigeCPUProzess()
   {
      HashMap<Integer,Double> neuesPIDCPUMapping = new HashMap<Integer, Double>();
      for (int i=0; i<Prozesse.size(); ++i)
      {
         OSProcess dieserProzess = Prozesse.get(i);
         PIDProzessMapping.put(dieserProzess.getProcessID(),dieserProzess);
         neuesPIDCPUMapping.put(dieserProzess.getProcessID(), dieserProzess.getProcessCpuLoadBetweenTicks(PIDProzessMapping_alt.get(dieserProzess.getProcessID()))*100/CpuKernzahl);
      }
      PIDCPUMapping = neuesPIDCPUMapping;
      Double gesamtCPU=0.0, ProzessCPU=0.0;
      String Text1 = "", Text2 = "", Text3 ="", ProzessName="";
      int Farbdifferenz= 0;
      DecimalFormat DF = new DecimalFormat("0.00");

      Set<Entry<Integer, Double>> Einträge = PIDCPUMapping.entrySet();
      List<Entry<Integer, Double>> SortierteListe = new ArrayList<Entry<Integer, Double>>(Einträge);
      Collections.sort(SortierteListe,
      new Comparator<Entry<Integer, Double>>()
      {
         public int compare(Entry<Integer, Double> Eintrag1, Entry<Integer, Double> Eintrag2)
         {
            return Eintrag2.getValue().compareTo(Eintrag1.getValue());
         }
      });
      if(Ticks!=null)
         gesamtCPU=100.0*Prozessor.getSystemCpuLoadBetweenTicks(Ticks);
      Ticks = Prozessor.getSystemCpuLoadTicks();
      if (gesamtCPU<0.0)
         gesamtCPU=-gesamtCPU;
      Farbdifferenz=(int)(gesamtCPU*2.5);
      if (Farbdifferenz>255)
         Farbdifferenz=255;
      if (!farbigerText)
         Farbdifferenz=0;
      if(SortierteListe.size()>0)
      {
         int Listenindex = 0;
         if (SortierteListe.get(Listenindex).getKey()==0 && SortierteListe.size()>1)
            ++Listenindex;
         double CPUmax = SortierteListe.get(Listenindex).getValue();
         if (CPUmax>gesamtCPU)
            gesamtCPU=CPUmax;

         for (int i=Listenindex; i<6+Listenindex && i<SortierteListe.size() && SortierteListe.get(i).getValue()>0.0; ++i)
         {
            Text1 += DF.format(SortierteListe.get(i).getValue()) + "%\n";
            Text2 += SortierteListe.get(i).getKey() + "\n";
            Text3 += PIDProzessMapping.get(SortierteListe.get(i).getKey()).getName() + "\n";
         }

         if (ZeigeBilder)
         {
            Textfeld[0].setHorizontalAlignment(JLabel.LEFT);
         }
         ProzessCPU = SortierteListe.get(Listenindex).getValue();
         ProzessName = PIDProzessMapping.get(SortierteListe.get(Listenindex).getKey()).getName();
      }
      if(!Pause)
      {
         Prozesslisten[0][0].setText(Text1);
         Prozesslisten[0][1].setText(Text2);
         Prozesslisten[0][2].setText(Text3);
         Textfeld[0].setForeground(new Color(255,255-Farbdifferenz,255-Farbdifferenz));
         Textfeld[0].setText(DF.format(gesamtCPU) + "% / " + DF.format(ProzessCPU) + "% ");       // 0 wäre der Leerlaufprozess
         Textfeld[1].setText(ProzessName);
      }
      gesamtCPU*=1.6;
      AktuelleGraphdaten[0]= new Graphdaten(gesamtCPU.intValue(), Textfeld[0].getText()+Textfeld[1].getText());
   }
   private void zeigeMEMProzess()
   {
      freierSpeicher = allgemeineBetriebssysteminfo.getHardware().getMemory().getAvailable();
      long genutzterSpeicher = gesamterSpeicher-freierSpeicher;
      Double genutzterSpeicherProzent = Double.valueOf(genutzterSpeicher)*100/Double.valueOf(gesamterSpeicher);
      Collections.sort(Prozesse, new Comparator<OSProcess>()
      {
         public int compare(OSProcess ersterProzess, OSProcess zweiterProzess)
         {
            return Long.compare(zweiterProzess.getResidentSetSize(), ersterProzess.getResidentSetSize());
         }
      }
      );

      String Text1 = "", Text2 = "", Text3="", ProzessName="";
      Double Speicher = 0.0;
      Double Prozent = 0.0;
      DecimalFormat DF = new DecimalFormat("0.00");
      int Farbdifferenz= (int)(genutzterSpeicherProzent*2.5);
      if (!farbigerText)
         Farbdifferenz=0;

      if (Prozesse.size()>0)
      {
         for (int i=0; i<6 && i<Prozesse.size(); ++i)
         {
            Text1 += DatenFormatieren(Prozesse.get(i).getResidentSetSize(), false) + "\n";
            Text2 += Prozesse.get(i).getProcessID() + "\n";
            Text3 += Prozesse.get(i).getName() + "\n";
         }
         Speicher = (double) Prozesse.get(0).getResidentSetSize();
         Prozent = (Speicher*100.0)/(gesamterSpeicher);
         ProzessName = Prozesse.get(0).getName();
      }
      if(!Pause)
      {
         Prozesslisten[2][0].setText(Text1);
         Prozesslisten[2][1].setText(Text2);
         Prozesslisten[2][2].setText(Text3);
         Textfeld[5].setForeground(new Color(255-Farbdifferenz,255,255-Farbdifferenz));
         Textfeld[5].setText(DF.format(genutzterSpeicherProzent) + "% / " + DF.format(Prozent) + "% " + DatenFormatieren(Speicher, false) + " ");
         Textfeld[6].setText(ProzessName);
      }
      genutzterSpeicherProzent*=1.6;
      AktuelleGraphdaten[1] = new Graphdaten(genutzterSpeicherProzent.intValue(), Textfeld[5].getText()+Textfeld[6].getText());
   }
   private void setzeTextfelderNull(int option)
   {
      DecimalFormat DF = new DecimalFormat("0.00");

      Textfeld[2+option].setForeground(Color.WHITE);
      Textfeld[3+option].setForeground(Color.WHITE);

      String Summe_E="", Rate_E="";
      if (ZeigeEinheiten)
      {
         Summe_E="B "; Rate_E="B/s";
      }
      Textfeld[2+option].setText("[––] " + DF.format(0.00) +" "+Summe_E);
      Textfeld[3+option].setText("[––] " + DF.format(0.00) +" "+Rate_E);
      Textfeld[4+option].setText("");
   }
   private String formatiereVerhältnis(Double Verhältnis)
   {
      if (Verhältnis>99)
      {
         Verhältnis=99.0;
         return "[" + String.valueOf(Verhältnis.intValue()) + "]";
      }
      else if (Verhältnis<10)
         return "[0" + String.valueOf(Verhältnis.intValue()) + "]";
      else if (Verhältnis.isNaN())
         return "[––]";
      else
         return "[" + String.valueOf(Verhältnis.intValue()) + "]";
   }
   private Datenvektor liefereDatenvektorfürWert(double Wert)
   {
      Color Farbe=Color.WHITE;
      String Einheit="B";

      if (Wert<1)
         Wert *=1024;
      else if (Wert<=1024)
      {
         Farbe=Einheitenfarben[0];
         Einheit="KB";
      }
      else if (Wert>1024)
      {
         Wert /=1024;
         Farbe=Einheitenfarben[1];
         Einheit="MB";
         if (Wert>1024)
         {
            Wert /=1024;
            Farbe=Einheitenfarben[2];
            Einheit="GB";
         }
      }
      return new Datenvektor(Wert, Einheit, Farbe);
   }
   private String DatenFormatieren(double Daten, boolean inKB)
   {
      int i=0;
      for (i=0; Daten > 1024; ++i)
         Daten/=1024;
      if (inKB)
      {
         ++i;
         if (Daten<1)
         {
            Daten*=1024;
            --i;
         }
      }
      String Einheit = "";
      if (i==1)
         Einheit="k";
      else if (i==2)
         Einheit="m";
      else if (i>2)
         Einheit="g";
      return String.format("%.2f%s", Daten, Einheit);
   }
   private String spalteDatenauf(long Daten)
   {
      long TB = Daten/(1024l*1024l*1024l*1024l);
      long GB = (Daten/(1024l*1024l*1024l)) % 1024;
      long MB = (Daten/(1024l*1024l)) % 1024;
      long KB = (Daten/1024l) % 1024;
      long B = Daten % 1024;
      String Ausgabe = new String("");
      if (TB!=0)
         Ausgabe += String.valueOf(TB) + " TB ";
      if (GB!=0)
         Ausgabe += String.valueOf(GB) + " GB ";
      if (MB!=0)
         Ausgabe += String.valueOf(MB) + " MB ";
      if (KB!=0)
         Ausgabe += String.valueOf(KB) + " KB ";
      if (B!=0)
         Ausgabe += String.valueOf(B) + " B ";
      if (Ausgabe.equals(""))
         Ausgabe += "0 B";
      return Ausgabe;
   }
   private void zeigeAusnahme(Exception Ausnahme)
   {
      StringWriter stackTraceWriter = new StringWriter();
      Ausnahme.printStackTrace(new PrintWriter(stackTraceWriter));
      Fehlertext.append(Ausnahme.toString() + "\n" + stackTraceWriter.toString());
      Ausnahme.printStackTrace();
      Fehlerfenster.setLocation(Hintergrundfenster.getLocation().x+Länge, Hintergrundfenster.getLocation().y);
      Fehlerfenster.setVisible(true);
   }
}
