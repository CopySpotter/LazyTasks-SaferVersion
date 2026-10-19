// BusyTasks Safe 1.1 – desktop dashboard for Linux Mint Cinnamon / X11.
// Original BusyTasks by Bernhard Mayr (GPL-3.0-or-later); dashboard added in 2026.
package BusyTasks;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public final class BusyTasksDashboard {
    private static final Color BG=new Color(17,24,32);
    private static final Color PANEL=new Color(27,37,48);
    private static final Color PANEL_ALT=new Color(35,47,60);
    private static final Color FORE=new Color(235,243,249);
    private static final Color MUTED=new Color(167,186,201);
    private static final Color ACCENT=new Color(67,205,169);
    private static final Color CPU=new Color(255,126,119);
    private static final Color MEMORY=new Color(106,198,140);
    private static final Color DISK=new Color(248,184,101);
    private static final Color NETWORK=new Color(109,179,250);
    private static final Font BODY=new Font(Font.SANS_SERIF,Font.PLAIN,14);
    private final JFrame frame=new JFrame("BusyTasks Safe 1.1 – Systemmonitor");
    private final MonitorService sampler = new MonitorService();
    private final StatCard cpuCard=new StatCard("PROZESSOR",CPU,"Gesamte CPU-Auslastung",100);
    private final StatCard memoryCard=new StatCard("ARBEITSSPEICHER",MEMORY,"Belegter Arbeitsspeicher",100);
    private final StatCard diskCard=new StatCard("DATENTRÄGER",DISK,"Summe Lese- und Schreibzugriffe / s",0);
    private final StatCard netCard=new StatCard("NETZWERK",NETWORK,"Empfang und Versand / s",0);
    private final JLabel status=new JLabel("Messwerte werden geladen …");
    private final JLabel processCount=new JLabel("Prozesse: –");
    private final JButton pauseButton=new JButton("Pausieren");
    private final JButton refreshButton=new JButton("Jetzt aktualisieren");
    private final JComboBox<String> intervalSelect=new JComboBox<>(new String[]{"1 Sekunde","2 Sekunden","5 Sekunden"});
    private final JTextField searchField=new JTextField(18);
    private final ProcessModel model=new ProcessModel();
    private final JTable table=new JTable(model);
    private final TableRowSorter<ProcessModel> sorter=new TableRowSorter<>(model);
    private final javax.swing.Timer timer;
    private volatile boolean busy=false;
    private boolean paused=false;
    private int refreshSeconds=2;

    public static void main(String[] args) {
        // The upstream compact widget remains available, but does not start implicitly.
        if (args.length>0 && "--classic".equals(args[0])) {
            SwingUtilities.invokeLater(BusyTasks::new);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* Swing fallback is fine */ }
            new BusyTasksDashboard().show();
        });
    }

    public BusyTasksDashboard() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG);
        frame.setMinimumSize(new Dimension(820,630));
        frame.setSize(1100,780);
        frame.setLocationRelativeTo(null);
        applySavedSettings();
        // Keep the whole monitor visible on smaller laptop screens (e.g. 1366 × 768).
        Rectangle usable=GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        frame.setSize(Math.min(frame.getWidth(),Math.max(820,usable.width-24)),
                      Math.min(frame.getHeight(),Math.max(630,usable.height-24)));
        frame.setLocationRelativeTo(null);
        frame.setJMenuBar(createMenu());
        frame.add(createHeader(),BorderLayout.NORTH);
        frame.add(createCenter(),BorderLayout.CENTER);
        frame.add(createFooter(),BorderLayout.SOUTH);
        configureTable();
        configureEvents();
        timer=new javax.swing.Timer(refreshSeconds*1000,evt->requestSample());
        timer.setCoalesce(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                timer.stop();
                saveSettings();
            }
        });
    }
    private void show() {
        frame.setVisible(true);
        requestSample();
        timer.start();
    }
    private void applySavedSettings() {
        try {
            Properties p=SafeConfig.load();
            int width=SafeConfig.integer(p,"dashboard.width",1100,820,2400);
            int height=SafeConfig.integer(p,"dashboard.height",780,630,1800);
            frame.setSize(width,height);
            refreshSeconds=SafeConfig.integer(p,"dashboard.interval",2,1,5);
        } catch(IOException ignored) { /* first start, or inaccessible preference file */ }
        intervalSelect.setSelectedIndex(refreshSeconds==1?0:refreshSeconds==5?2:1);
    }
    private void saveSettings() {
        try {
            Properties p;
            try { p=SafeConfig.load(); } catch(IOException ignored) { p=new Properties(); }
            p.setProperty("dashboard.width", String.valueOf(frame.getWidth()));
            p.setProperty("dashboard.height",String.valueOf(frame.getHeight()));
            p.setProperty("dashboard.interval",String.valueOf(refreshSeconds));
            SafeConfig.save(p);
        } catch(IOException ex) {
            System.err.println("Einstellungen konnten nicht gesichert werden: "+ex.getMessage());
        }
    }
    private JMenuBar createMenu() {
        JMenuBar bar=new JMenuBar();
        JMenu datei=new JMenu("Datei");
        JMenuItem classic=new JMenuItem("Klassisches Mini-Widget öffnen");
        classic.addActionListener(evt -> {
            int choice=JOptionPane.showConfirmDialog(frame,
                    "Das ursprüngliche Mini-Widget öffnet eigene Fenster.\n"+
                    "Bereits aktivierte Diagnoseberechtigungen könnten erneut abgefragt werden.\n\n"+
                    "Mini-Widget öffnen?","Klassische Oberfläche",JOptionPane.YES_NO_OPTION);
            if(choice==JOptionPane.YES_OPTION) SwingUtilities.invokeLater(BusyTasks::new);
        });
        JMenuItem exit=new JMenuItem("Beenden");
        exit.addActionListener(evt -> {
            saveSettings();
            timer.stop();
            frame.dispose();
            System.exit(0);
        });
        datei.add(classic); datei.addSeparator(); datei.add(exit);
        JMenu hilfe=new JMenu("Hilfe");
        JMenuItem explain=new JMenuItem("Bedienung und Sicherheit");
        explain.addActionListener(evt->showHelp());
        hilfe.add(explain);
        bar.add(datei); bar.add(hilfe);
        return bar;
    }
    private JPanel createHeader() {
        JPanel outer=new JPanel(new BorderLayout(12,8));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(18,22,12,22));
        JPanel titles=new JPanel(new GridLayout(2,1,0,3));
        titles.setOpaque(false);
        titles.add(text("BusyTasks Safe",28,Font.BOLD,FORE));
        titles.add(text("Systemmonitor 1.1  ·  Linux Mint / Cinnamon  ·  keine Administratorrechte",13,Font.PLAIN,MUTED));
        outer.add(titles,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,5));
        actions.setOpaque(false);
        styleButton(pauseButton,false);
        styleButton(refreshButton,true);
        actions.add(pauseButton);
        actions.add(refreshButton);
        outer.add(actions,BorderLayout.EAST);
        return outer;
    }
    private JComponent createCenter() {
        JPanel cards=new JPanel(new GridLayout(2,2,14,14));
        cards.setBackground(BG);
        cards.add(cpuCard); cards.add(memoryCard); cards.add(diskCard); cards.add(netCard);
        cards.setBorder(new EmptyBorder(0,22,0,22));
        cards.setMinimumSize(new Dimension(740,300));
        JPanel below=createProcesses();
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,cards,below);
        split.setResizeWeight(.53);
        split.setBorder(null);
        split.setDividerSize(9);
        split.setOpaque(true);
        split.setBackground(BG);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(.53));
        return split;
    }
    private JPanel createProcesses() {
        JPanel panel=new JPanel(new BorderLayout(0,10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(8,22,6,22));
        JPanel heading=new JPanel(new BorderLayout(10,4));
        heading.setOpaque(false);
        JPanel labelBox=new JPanel(new GridLayout(2,1,0,4));
        labelBox.setOpaque(false);
        labelBox.add(text("Laufende Prozesse",19,Font.BOLD,FORE));
        labelBox.add(text("Nach CPU-Auslastung sortiert · Doppelklick zeigt Details",12,Font.PLAIN,MUTED));
        heading.add(labelBox,BorderLayout.WEST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT,9,7));
        controls.setOpaque(false);
        JLabel searchLabel=text("Suche:",13,Font.PLAIN,MUTED);
        controls.add(searchLabel);
        searchField.setFont(BODY);
        searchField.setToolTipText("Nach Name oder Prozess-ID filtern");
        controls.add(searchField);
        controls.add(text("Intervall:",13,Font.PLAIN,MUTED));
        intervalSelect.setFont(BODY);
        controls.add(intervalSelect);
        heading.add(controls,BorderLayout.EAST);
        panel.add(heading,BorderLayout.NORTH);
        table.setBackground(PANEL);
        table.setForeground(FORE);
        table.setGridColor(PANEL_ALT);
        table.setSelectionBackground(new Color(50,92,105));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setFont(BODY);
        table.setIntercellSpacing(new Dimension(0,1));
        JTableHeader header=table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
        header.setBackground(PANEL_ALT);
        header.setForeground(FORE);
        header.setPreferredSize(new Dimension(0,35));
        JScrollPane scroll=new JScrollPane(table);
        scroll.getViewport().setBackground(PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(53,67,80),1));
        panel.add(scroll,BorderLayout.CENTER);
        return panel;
    }
    private JPanel createFooter() {
        JPanel bottom=new JPanel(new BorderLayout(12,0));
        bottom.setBackground(BG);
        bottom.setBorder(new EmptyBorder(8,24,15,24));
        status.setForeground(MUTED);
        status.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        processCount.setForeground(ACCENT);
        processCount.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));
        bottom.add(status,BorderLayout.CENTER);
        bottom.add(processCount,BorderLayout.EAST);
        return bottom;
    }
    private void configureTable() {
        table.setRowSorter(sorter);
        sorter.setComparator(0,Comparator.comparingInt(a -> ((Number)a).intValue()));
        sorter.setComparator(2,Comparator.comparingDouble(a -> ((Number)a).doubleValue()));
        sorter.setComparator(3,Comparator.comparingDouble(a -> ((Number)a).doubleValue()));
        sorter.setSortKeys(List.of(new RowSorter.SortKey(2,SortOrder.DESCENDING)));
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(340);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(125);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        DefaultTableCellRenderer cellRenderer=new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int row,int col) {
                Object shown=v;
                if(col==2 && v instanceof Number) shown=String.format(Locale.GERMAN,"%.1f %%",((Number)v).doubleValue());
                if(col==3 && v instanceof Number) shown=MonitorService.size(((Number)v).doubleValue());
                Component c=super.getTableCellRendererComponent(t,shown,selected,focus,row,col);
                if(!selected) {
                    c.setBackground(row%2==0?PANEL:PANEL_ALT);
                    c.setForeground(FORE);
                }
                setBorder(new EmptyBorder(0,12,0,12));
                setHorizontalAlignment((col==0||col==2||col==3)?SwingConstants.RIGHT:SwingConstants.LEFT);
                return c;
            }
        };
        for(int i=0;i<table.getColumnCount();i++) table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
    }
    private void configureEvents() {
        pauseButton.addActionListener(evt -> {
            paused=!paused;
            pauseButton.setText(paused?"Fortsetzen":"Pausieren");
            status.setText(paused?"Pausiert · Werte werden nicht aktualisiert":"Überwachung läuft");
            if(!paused) requestSample();
        });
        refreshButton.addActionListener(evt -> requestSample());
        intervalSelect.addActionListener(evt -> {
            int choice=intervalSelect.getSelectedIndex();
            refreshSeconds=choice==0?1:choice==2?5:2;
            if(timer!=null) timer.setDelay(refreshSeconds*1000);
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                if(evt.getClickCount()==2 && table.getSelectedRow()>=0) showSelectedProcess();
            }
        });
    }
    private void filter() {
        String raw=searchField.getText().trim().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(raw.isBlank()?null:new RowFilter<>() {
            @Override public boolean include(Entry<? extends ProcessModel,? extends Integer> entry) {
                return String.valueOf(entry.getValue(0)).contains(raw) ||
                       String.valueOf(entry.getValue(1)).toLowerCase(Locale.ROOT).contains(raw);
            }
        });
    }
    private void showSelectedProcess() {
        int view=table.getSelectedRow();
        if(view<0) return;
        int row=table.convertRowIndexToModel(view);
        MonitorService.ProcessEntry p=model.entry(row);
        if(p==null) return;
        String content="<html><div style='font-family:sans-serif;width:310px'>"+
             "<h3>"+html(p.name)+"</h3>"+
             "<b>Prozess-ID:</b> "+p.pid+"<br>"+
             "<b>CPU-Anteil:</b> "+String.format(Locale.GERMAN,"%.1f %%",p.cpuPercent)+"<br>"+
             "<b>Arbeitsspeicher:</b> "+MonitorService.size(p.residentBytes)+"<br>"+
             "<b>Status:</b> "+html(p.state)+"<br><br>"+
             "Die Werte stammen von OSHI/Linux und benötigen keine Administratorrechte. " +
             "Prozessbefehle oder Passwörter werden nicht angezeigt.</div></html>";
        JOptionPane.showMessageDialog(frame,content,"Prozessdetails",JOptionPane.INFORMATION_MESSAGE);
    }
    private static String html(String s) {
        return (s==null?"":s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    private void showHelp() {
        JOptionPane.showMessageDialog(frame,
            "BusyTasks Safe 1.1 – Bedienung\n\n"+
            "• Oben: vier Messkarten mit Verlaufsgrafiken.\n"+
            "• Unten: Prozessliste, nach CPU oder RAM sortierbar (Spaltenkopf anklicken).\n"+
            "• Suche: Prozessname oder PID eingeben.\n"+
            "• Doppelklick: Details zum gewählten Prozess.\n"+
            "• Pausieren/Fortsetzen: letzte Messung festhalten.\n"+
            "• Intervall: 1, 2 oder 5 Sekunden.\n"+
            "• Größe: Fenster an einer Ecke ziehen; sie bleibt gespeichert.\n\n"+
            "Sicherheit: Keine sudo-Aufrufe und keine Passwörter in dieser Oberfläche.\n"+
            "Datenträger- und Netzwerkwerte zeigen GESAMTRATEN, keine Prozesse.\n"+
            "Die ersten Geschwindigkeitswerte können 0 sein, bis zwei Messungen vorliegen.\n"+
            "Klassisches Widget: Datei > Klassisches Mini-Widget öffnen.",
            "Hilfe zu BusyTasks Safe",JOptionPane.INFORMATION_MESSAGE);
    }
    private void requestSample() {
        if(paused || busy) return;
        busy=true;
        new SwingWorker<MonitorService.Snapshot,Void>() {
            @Override protected MonitorService.Snapshot doInBackground() { return sampler.sample(); }
            @Override protected void done() {
                busy=false;
                if(paused) return;
                try { updateView(get()); }
                catch (Exception ex) {
                    status.setText("Messung fehlgeschlagen: "+ex.getClass().getSimpleName()+
                            " – später erneut versuchen");
                    System.err.println("Messfehler: "+ex);
                }
            }
        }.execute();
    }
    private void updateView(MonitorService.Snapshot s) {
        cpuCard.update(String.format(Locale.GERMAN,"%.1f %%",s.cpuPercent),
                "Auslastung des gesamten Prozessors",s.cpuPercent);
        memoryCard.update(String.format(Locale.GERMAN,"%.1f %%",s.memoryPercent),
                MonitorService.size(s.usedMemory)+" von "+MonitorService.size(s.totalMemory),s.memoryPercent);
        diskCard.update(MonitorService.size(s.diskReadRate+s.diskWriteRate)+"/s",
                "Lesen "+MonitorService.size(s.diskReadRate)+"/s  ·  Schreiben "+MonitorService.size(s.diskWriteRate)+"/s",
                s.diskReadRate+s.diskWriteRate);
        netCard.update(MonitorService.size(s.netReadRate+s.netWriteRate)+"/s",
                "Empfang "+MonitorService.size(s.netReadRate)+"/s  ·  Senden "+MonitorService.size(s.netWriteRate)+"/s",
                s.netReadRate+s.netWriteRate);
        model.setEntries(s.processes);
        processCount.setText("Prozesse: "+s.processCount);
        status.setText("Aktualisiert: "+new java.text.SimpleDateFormat("HH:mm:ss",Locale.GERMAN).format(new Date())+
                       "  ·  Systemdaten ohne Administratorrechte  ·  IO/NET = Gesamtverkehr");
    }
    private static JLabel text(String text,int size,int weight,Color color) {
        JLabel label=new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font(Font.SANS_SERIF,weight,size));
        return label;
    }
    private static void styleButton(JButton button,boolean primary) {
        button.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
        button.setForeground(primary?BG:FORE);
        button.setBackground(primary?ACCENT:PANEL_ALT);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(BorderFactory.createLineBorder(primary?ACCENT:new Color(69,91,103),1),
            new EmptyBorder(8,12,8,12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    private static final class ProcessModel extends AbstractTableModel {
        private final String[] columns={"PID","Prozess","CPU","RAM","Zustand"};
        private List<MonitorService.ProcessEntry> rows=Collections.emptyList();
        void setEntries(List<MonitorService.ProcessEntry> entries) {
            rows=new ArrayList<>(entries);
            fireTableDataChanged();
        }
        MonitorService.ProcessEntry entry(int row) { return row>=0 && row<rows.size()?rows.get(row):null; }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int i) { return columns[i]; }
        @Override public Class<?> getColumnClass(int col) { return (col==0)?Integer.class:(col==2)?Double.class:(col==3)?Long.class:String.class; }
        @Override public Object getValueAt(int row,int column) {
            MonitorService.ProcessEntry p=rows.get(row);
            switch(column) {
                case 0: return p.pid;
                case 1: return p.name;
                case 2: return p.cpuPercent;
                case 3: return p.residentBytes;
                case 4: return p.state;
                default: return "";
            }
        }
    }
    private static final class StatCard extends JPanel {
        private final JLabel big=new JLabel("–");
        private final JLabel detail=new JLabel("Warte auf die erste Messung …");
        private final Sparkline sparkline;
        StatCard(String title,Color color,String caption,double max) {
            super(new BorderLayout(5,0));
            setBackground(PANEL);
            setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(52,67,81),1),
                    new EmptyBorder(14,18,12,18)));
            JPanel top=new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(text(title,13,Font.BOLD,color),BorderLayout.NORTH);
            big.setForeground(FORE);
            big.setFont(new Font(Font.SANS_SERIF,Font.BOLD,28));
            big.setBorder(new EmptyBorder(6,0,0,0));
            top.add(big,BorderLayout.CENTER);
            add(top,BorderLayout.NORTH);
            sparkline=new Sparkline(color,max);
            sparkline.setPreferredSize(new Dimension(100,65));
            add(sparkline,BorderLayout.CENTER);
            detail.setForeground(MUTED);
            detail.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
            detail.setText(caption);
            add(detail,BorderLayout.SOUTH);
        }
        void update(String headline,String desc,double value) {
            big.setText(headline); detail.setText(desc); sparkline.addValue(value);
        }
    }
    private static final class Sparkline extends JComponent {
        private static final int LIMIT=100;
        private final ArrayDeque<Double> values=new ArrayDeque<>();
        private final Color color;
        private final double fixedMax;
        Sparkline(Color color,double fixedMax) {this.color=color;this.fixedMax=fixedMax;setOpaque(false);}
        void addValue(double value) {
            values.addLast(Double.isFinite(value)?Math.max(0,value):0);
            if(values.size()>LIMIT) values.removeFirst();
            repaint();
        }
        @Override protected void paintComponent(Graphics original) {
            super.paintComponent(original);
            Graphics2D g=(Graphics2D)original.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight();
                if(w<3||h<3) return;
                g.setColor(new Color(62,76,89));
                for(int y=1;y<4;y++) {
                    int line=y*(h-5)/4;
                    g.drawLine(0,line,w-1,line);
                }
                if(values.size()<2) return;
                double highest=fixedMax>0?fixedMax:1;
                if(fixedMax<=0) for(double value:values) highest=Math.max(highest,value*1.12);
                int step=0, previousX=-1,previousY=0, n=values.size();
                g.setStroke(new BasicStroke(2.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(color);
                for(double value: values) {
                    int x=(int)Math.round(step*(w-1.0)/(n-1));
                    int y=h-3-(int)Math.round(Math.min(value/highest,1.0)*(h-8));
                    if(previousX>=0) g.drawLine(previousX,previousY,x,y);
                    previousX=x; previousY=y; step++;
                }
            } finally { g.dispose(); }
        }
    }
}
