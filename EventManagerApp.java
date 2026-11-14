import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * PIET Jaipur — Campus Event & Resource Manager
 * Poornima Institute of Engineering & Technology, Jaipur
 *
 * Clean, light, modern Swing UI with PIET branding.
 * All data persisted to MySQL via DatabaseStorageManager.
 */
public class EventManagerApp {

    // ── PIET BRAND PALETTE ──────────────────────────────────────────────
    static final Color PIET_RED        = new Color(0xD3, 0x2F, 0x2F);
    static final Color PIET_RED_DARK   = new Color(0xB7, 0x1C, 0x1C);
    static final Color PIET_BLUE       = new Color(0x15, 0x65, 0xC0);
    static final Color PIET_BLUE_DEEP  = new Color(0x0D, 0x47, 0xA1);
    static final Color PIET_GOLD       = new Color(0xFF, 0xB3, 0x00);

    // ── UI PALETTE ──────────────────────────────────────────────────────
    static final Color BG              = new Color(0xF1, 0xF5, 0xF9);
    static final Color CARD            = Color.WHITE;
    static final Color TEXT_PRIMARY    = new Color(0x1E, 0x29, 0x3B);
    static final Color TEXT_SECONDARY  = new Color(0x64, 0x74, 0x8B);
    static final Color BORDER          = new Color(0xE2, 0xE8, 0xF0);
    static final Color ACCENT_GREEN    = new Color(0x16, 0xA3, 0x4A);
    static final Color ACCENT_AMBER    = new Color(0xF5, 0x9E, 0x0B);
    static final Color ACCENT_CYAN     = new Color(0x06, 0xB6, 0xD4);
    static final Color ACCENT_VIOLET   = new Color(0x7C, 0x3A, 0xED);
    static final Color DANGER          = new Color(0xDC, 0x26, 0x26);

    // ── SHADOW COLORS for painted card shadows ──────────────────────────
    static final Color SHADOW_LIGHT    = new Color(0, 0, 0, 18);
    static final Color SHADOW_MED      = new Color(0, 0, 0, 10);

    // ── LOGO ────────────────────────────────────────────────────────────
    private static BufferedImage logoImage;
    static {
        try {
            File f = new File("assets/piet_logo.png");
            if (!f.exists()) f = new File("piet_logo.png");
            if (f.exists()) logoImage = ImageIO.read(f);
        } catch (Exception ignored) {}
    }

    // ── WINDOW ──────────────────────────────────────────────────────────
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // ── DATA ────────────────────────────────────────────────────────────
    List<CollegeEvent> eventList;
    private static int idCounter = 1;

    // ── HOME WIDGETS ────────────────────────────────────────────────────
    private JPanel eventListPanel;
    private JLabel statTotal, statToday, statUpcoming, statVenues;
    private JTextField searchField;
    private JComboBox<String> categoryFilter, venueFilter;

    // ── CREATE WIDGETS ──────────────────────────────────────────────────
    private JTextField nameField, organizerField, dateField, participantsField, formUrlField;
    private JComboBox<Venue> venueCombo;
    private JComboBox<Category> categoryCombo;
    private JTextArea descArea;
    private JPanel slotGridPanel;
    private JLabel formError;
    private TimeSlot selectedSlot;

    // ═════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new EventManagerApp().initialize();
        });
    }

    public void initialize() {
        eventList = new ArrayList<>();
        try { eventList = DatabaseStorageManager.loadEvents(); }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,
                "Could not read events from the database.\n\n" + ex.getMessage()
                + "\n\nStarting with an empty list for this session.",
                "Load Error", JOptionPane.ERROR_MESSAGE);
        }
        if (eventList.isEmpty()) { seedDummyData(); persistEvents(); }
        for (CollegeEvent ev : eventList) if (ev.id >= idCounter) idCounter = ev.id + 1;

        frame = new JFrame("PIET Jaipur — Campus Event & Resource Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 760);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG);
        mainPanel.add(buildHomePage(), "HOME");
        mainPanel.add(buildCreateEventPage(), "CREATE");

        frame.add(mainPanel);
        refreshEventListUI();
        frame.setVisible(true);
    }

    private void seedDummyData() {
        eventList.add(new CollegeEvent(idCounter++, "Aarohan 2026 — Annual Techno-Fest",
            LocalDate.now().plusDays(3), TimeSlot.SLOT_2, Venue.MAIN_AUDITORIUM, Category.TECHNICAL,
            "PIET Technical Committee",
            "Flagship annual fest: AI Hackathon, Robo-Wars, Code-Craft, Keynote talks by industry leaders.",
            480, "https://forms.google.com/example-aarohan"));
        eventList.add(new CollegeEvent(idCounter++, "Poornima Alumni Grand Conclave",
            LocalDate.now().plusDays(7), TimeSlot.SLOT_3, Venue.SEMINAR_HALL_A, Category.CULTURAL,
            "Alumni Relations Cell",
            "Networking event celebrating PIET alumni achievements across global tech corporations.", 120, null));
        eventList.add(new CollegeEvent(idCounter++, "Lakshya Inter-College Cricket Cup",
            LocalDate.now().plusDays(1), TimeSlot.SLOT_1, Venue.SPORTS_GROUND, Category.SPORTS,
            "Sports Department",
            "Championship cricket tournament featuring university teams from all over Rajasthan.", 220, null));
        eventList.add(new CollegeEvent(idCounter++, "Generative AI & Agentic Systems Workshop",
            LocalDate.now(), TimeSlot.SLOT_2, Venue.CONFERENCE_ROOM, Category.WORKSHOP,
            "AI & Data Science Dept",
            "Hands-on masterclass building LLM Agents, RAG pipelines, and Agentic applications.",
            45, "https://forms.google.com/example-genai"));
        eventList.add(new CollegeEvent(idCounter++, "Udaan 2026 — Cultural Extravaganza",
            LocalDate.now().minusDays(1), TimeSlot.SLOT_4, Venue.MAIN_AUDITORIUM, Category.CULTURAL,
            "Student Activity Council",
            "Grand musical evening, folk dance, classical band showcase, celebrity guest.", 650, null));
    }

    private void persistEvents() {
        try { DatabaseStorageManager.saveEvents(eventList); }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                "Failed to save data to the database:\n" + ex.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HOME PAGE
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        page.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(20, 32, 10, 32));

        JPanel statsRow = buildStatsRow();
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(statsRow);
        body.add(Box.createVerticalStrut(18));

        JPanel filterRow = buildFilterRow();
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(filterRow);

        eventListPanel = new JPanel();
        eventListPanel.setLayout(new BoxLayout(eventListPanel, BoxLayout.Y_AXIS));
        eventListPanel.setBackground(BG);
        eventListPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 20, 0));

        JScrollPane scroll = new JScrollPane(eventListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(body, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    // ── HEADER (gradient banner with logo + live clock) ──────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, PIET_BLUE_DEEP, getWidth(), getHeight(), PIET_RED);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // subtle bottom glow line
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        // ── Left: Logo + Title ──
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        if (logoImage != null) {
            Image scaled = logoImage.getScaledInstance(52, 52, Image.SCALE_SMOOTH);
            JLabel logoLbl = new JLabel(new ImageIcon(scaled));
            logoLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
            left.add(logoLbl);
        }

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("PIET Jaipur — Event & Resource Manager");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel clockLabel = new JLabel();
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clockLabel.setForeground(new Color(255, 255, 255, 200));
        clockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy  ·  hh:mm a");
        clockLabel.setText(LocalDateTime.now().format(dtf));
        new javax.swing.Timer(30_000, e -> clockLabel.setText(LocalDateTime.now().format(dtf))).start();

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(clockLabel);
        left.add(titleBlock);

        // ── Right: Create Event button ──
        SmoothButton createBtn = new SmoothButton("＋  Create Event", Color.WHITE, new Color(255,255,255,220),
                PIET_BLUE_DEEP, 22);
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createBtn.addActionListener(e -> openCreatePage());

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(createBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── STAT CARDS ROW ──────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        row.setPreferredSize(new Dimension(10, 84));

        statTotal    = new JLabel("0");
        statToday    = new JLabel("0");
        statUpcoming = new JLabel("0");
        statVenues   = new JLabel("0");

        row.add(buildStatCard("Total Events",   statTotal,    PIET_BLUE));
        row.add(buildStatCard("Today",           statToday,    ACCENT_AMBER));
        row.add(buildStatCard("Upcoming",        statUpcoming, ACCENT_GREEN));
        row.add(buildStatCard("Venues In Use",   statVenues,   ACCENT_VIOLET));
        return row;
    }

    private JPanel buildStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new ShadowCard(14);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 16));

        // accent bar on the left
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 6, 4, getHeight() - 12, 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(16, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(valueLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(lbl);

        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ── FILTER ROW ──────────────────────────────────────────────────────
    private JPanel buildFilterRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        searchField = new JTextField(18);
        styleField(searchField);
        searchField.getDocument().addDocumentListener(new DocListener(this::refreshEventListUI));

        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All Categories");
        for (Category c : Category.values()) categoryFilter.addItem(c.label);
        styleCombo(categoryFilter);
        categoryFilter.addActionListener(e -> refreshEventListUI());

        venueFilter = new JComboBox<>();
        venueFilter.addItem("All Venues");
        for (Venue v : Venue.values()) venueFilter.addItem(v.label);
        styleCombo(venueFilter);
        venueFilter.addActionListener(e -> refreshEventListUI());

        JLabel searchIcon = new JLabel("🔍 ");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        row.add(searchIcon);
        row.add(searchField);
        row.add(categoryFilter);
        row.add(venueFilter);
        return row;
    }

    // ── REFRESH EVENT LIST ──────────────────────────────────────────────
    private void refreshEventListUI() {
        if (eventListPanel == null) return;
        eventListPanel.removeAll();

        String q = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String catSel = categoryFilter != null ? (String) categoryFilter.getSelectedItem() : "All Categories";
        String venSel = venueFilter != null ? (String) venueFilter.getSelectedItem() : "All Venues";

        List<CollegeEvent> filtered = new ArrayList<>();
        for (CollegeEvent ev : eventList) {
            if (!q.isEmpty() && !ev.name.toLowerCase().contains(q) && !ev.description.toLowerCase().contains(q)
                    && !ev.organizer.toLowerCase().contains(q)) continue;
            if (catSel != null && !catSel.equals("All Categories") && !ev.category.label.equals(catSel)) continue;
            if (venSel != null && !venSel.equals("All Venues") && !ev.venue.label.equals(venSel)) continue;
            filtered.add(ev);
        }

        LocalDate today = LocalDate.now();
        List<CollegeEvent> todayList = new ArrayList<>(), upList = new ArrayList<>(), pastList = new ArrayList<>();
        for (CollegeEvent ev : filtered) {
            if (ev.date.isEqual(today)) todayList.add(ev);
            else if (ev.date.isAfter(today)) upList.add(ev);
            else pastList.add(ev);
        }
        todayList.sort(Comparator.comparingInt(e -> e.slot.ordinal()));
        upList.sort(Comparator.comparing(e -> e.date));
        pastList.sort(Comparator.comparing((CollegeEvent e) -> e.date, Comparator.reverseOrder()));

        // stats
        long tc = 0, uc = 0; Set<Venue> vu = new HashSet<>();
        for (CollegeEvent ev : eventList) {
            if (ev.date.isEqual(today)) tc++;
            if (ev.date.isAfter(today)) uc++;
            vu.add(ev.venue);
        }
        statTotal.setText(String.valueOf(eventList.size()));
        statToday.setText(String.valueOf(tc));
        statUpcoming.setText(String.valueOf(uc));
        statVenues.setText(vu.size() + "/" + Venue.values().length);

        if (filtered.isEmpty()) {
            eventListPanel.add(buildEmptyState());
        } else {
            if (!todayList.isEmpty()) addSection("🔴  Happening Today", todayList);
            if (!upList.isEmpty())    addSection("📅  Upcoming", upList);
            if (!pastList.isEmpty())  addSection("⏳  Past", pastList);
        }
        eventListPanel.revalidate();
        eventListPanel.repaint();
    }

    private void addSection(String title, List<CollegeEvent> events) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(14, 4, 8, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventListPanel.add(lbl);

        for (CollegeEvent ev : events) {
            JPanel card = buildEventCard(ev);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventListPanel.add(card);
            eventListPanel.add(Box.createVerticalStrut(10));
        }
    }

    // ── EVENT CARD ──────────────────────────────────────────────────────
    private JPanel buildEventCard(CollegeEvent ev) {
        ShadowCard card = new ShadowCard(14);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        card.setPreferredSize(new Dimension(10, 98));

        // Hover glow
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, ev.category.color),
                    BorderFactory.createEmptyBorder(14, 10, 14, 16)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 16));
                card.repaint();
            }
        });

        // left color stripe
        JPanel strip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ev.category.color);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        strip.setOpaque(false);
        strip.setPreferredSize(new Dimension(18, 0));

        // center info
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(ev.name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLbl.setForeground(TEXT_PRIMARY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.setBorder(BorderFactory.createEmptyBorder(5, 0, 4, 0));
        badges.add(new Pill(ev.category.label, ev.category.color, Color.WHITE));
        badges.add(new Pill(formatDate(ev.date), new Color(0xEE, 0xF2, 0xFF), PIET_BLUE));
        badges.add(new Pill(ev.slot.label, new Color(0xF1, 0xF5, 0xF9), TEXT_SECONDARY));
        badges.add(new Pill(ev.venue.label, new Color(0xF1, 0xF5, 0xF9), TEXT_SECONDARY));
        if (ev.participants > 0)
            badges.add(new Pill(ev.participants + " participants", new Color(0xEC, 0xFD, 0xF5), ACCENT_GREEN));

        JLabel descLbl = new JLabel(truncate(ev.organizer + " · " + ev.description, 110));
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(TEXT_SECONDARY);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLbl);
        info.add(badges);
        info.add(descLbl);

        // right actions
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

        SmoothButton viewBtn = new SmoothButton("View", PIET_BLUE, PIET_BLUE_DEEP, Color.WHITE, 10);
        viewBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        viewBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewBtn.addActionListener(e -> showEventDetails(ev));

        SmoothButton delBtn = new SmoothButton("Delete", new Color(0xFE,0xE2,0xE2), new Color(0xFC,0xCA,0xCA), DANGER, 10);
        delBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        delBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        delBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        delBtn.addActionListener(e -> deleteEvent(ev));

        actions.add(viewBtn);
        actions.add(Box.createVerticalStrut(6));
        actions.add(delBtn);

        card.add(strip, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private JPanel buildEmptyState() {
        JPanel w = new JPanel();
        w.setOpaque(false);
        w.setLayout(new BoxLayout(w, BoxLayout.Y_AXIS));
        w.setBorder(BorderFactory.createEmptyBorder(60, 0, 60, 0));
        JLabel ic = new JLabel("📭"); ic.setFont(new Font("Segoe UI", Font.PLAIN, 42)); ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel m = new JLabel("No events found"); m.setFont(new Font("Segoe UI", Font.BOLD, 15)); m.setForeground(TEXT_SECONDARY); m.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel h = new JLabel("Try adjusting your search or filters"); h.setFont(new Font("Segoe UI", Font.PLAIN, 12)); h.setForeground(TEXT_SECONDARY); h.setAlignmentX(Component.CENTER_ALIGNMENT);
        w.add(ic); w.add(Box.createVerticalStrut(8)); w.add(m); w.add(Box.createVerticalStrut(4)); w.add(h);
        return w;
    }

    private void deleteEvent(CollegeEvent ev) {
        int c = JOptionPane.showConfirmDialog(frame, "Delete \"" + ev.name + "\"? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (c == JOptionPane.YES_OPTION) { eventList.remove(ev); persistEvents(); refreshEventListUI(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CREATE EVENT PAGE
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildCreateEventPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 32, 10, 32));

        JButton backBtn = new JButton("← Back to Dashboard");
        styleLink(backBtn);
        backBtn.addActionListener(e -> { resetCreateForm(); cardLayout.show(mainPanel, "HOME"); });

        JLabel title = new JLabel("Book Venue & Create Event");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);

        JPanel tw = new JPanel();
        tw.setOpaque(false);
        tw.setLayout(new BoxLayout(tw, BoxLayout.Y_AXIS));
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        tw.add(backBtn); tw.add(Box.createVerticalStrut(6)); tw.add(title);
        header.add(tw, BorderLayout.WEST);
        page.add(header, BorderLayout.NORTH);

        ShadowCard formCard = new ShadowCard(18);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(26, 32, 26, 32));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(7, 8, 7, 8);
        int r = 0;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Event Name *"), g);
        g.gridx=1; g.weightx=1; nameField = new JTextField(22); styleField(nameField); formCard.add(nameField, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Organizer / Dept"), g);
        g.gridx=1; g.weightx=1; organizerField = new JTextField(22); styleField(organizerField); formCard.add(organizerField, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Category"), g);
        g.gridx=1; g.weightx=1; categoryCombo = new JComboBox<>(Category.values()); styleCombo(categoryCombo); formCard.add(categoryCombo, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Date (YYYY-MM-DD) *"), g);
        g.gridx=1; g.weightx=1; dateField = new JTextField(22); styleField(dateField);
        dateField.getDocument().addDocumentListener(new DocListener(this::refreshSlotGrid));
        formCard.add(dateField, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Venue *"), g);
        g.gridx=1; g.weightx=1; venueCombo = new JComboBox<>(Venue.values()); styleCombo(venueCombo);
        venueCombo.addActionListener(e -> refreshSlotGrid());
        formCard.add(venueCombo, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Description"), g);
        g.gridx=1; g.weightx=1; descArea = new JTextArea(3, 22); descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descArea.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JScrollPane ds = new JScrollPane(descArea); ds.setBorder(BorderFactory.createLineBorder(BORDER));
        formCard.add(ds, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Participants"), g);
        g.gridx=1; g.weightx=1; participantsField = new JTextField(22); styleField(participantsField); formCard.add(participantsField, g); r++;

        g.gridx=0; g.gridy=r; g.weightx=0; formCard.add(fLabel("Form URL (optional)"), g);
        g.gridx=1; g.weightx=1; formUrlField = new JTextField(22); styleField(formUrlField); formCard.add(formUrlField, g); r++;

        g.gridx=0; g.gridy=r; g.gridwidth=2; g.weightx=1;
        JLabel slotLbl = fLabel("Select Time Slot");
        slotLbl.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        formCard.add(slotLbl, g); r++;

        g.gridx=0; g.gridy=r; g.gridwidth=2;
        slotGridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        slotGridPanel.setOpaque(false);
        formCard.add(slotGridPanel, g); r++;

        g.gridx=0; g.gridy=r; g.gridwidth=2;
        formError = new JLabel(" "); formError.setForeground(DANGER); formError.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formCard.add(formError, g); r++;

        g.gridx=0; g.gridy=r; g.gridwidth=2;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel"); styleLink(cancelBtn);
        cancelBtn.addActionListener(e -> { resetCreateForm(); cardLayout.show(mainPanel, "HOME"); });
        SmoothButton saveBtn = new SmoothButton("Save Event", PIET_BLUE, PIET_BLUE_DEEP, Color.WHITE, 12);
        saveBtn.addActionListener(e -> handleSaveEvent());
        btnRow.add(cancelBtn); btnRow.add(saveBtn);
        formCard.add(btnRow, g);

        JPanel host = new JPanel(new GridBagLayout());
        host.setBackground(BG);
        GridBagConstraints hg = new GridBagConstraints();
        hg.gridx=0; hg.gridy=0; hg.weightx=1; hg.insets = new Insets(0,32,32,32);
        hg.fill = GridBagConstraints.HORIZONTAL; hg.anchor = GridBagConstraints.NORTH;
        host.add(formCard, hg);

        JScrollPane sc = new JScrollPane(host);
        sc.setBorder(null); sc.getViewport().setBackground(BG); sc.getVerticalScrollBar().setUnitIncrement(16);
        page.add(sc, BorderLayout.CENTER);
        refreshSlotGrid();
        return page;
    }

    private void refreshSlotGrid() {
        if (slotGridPanel == null) return;
        slotGridPanel.removeAll();
        LocalDate d = tryParseDate(dateField.getText());
        Venue v = (Venue) venueCombo.getSelectedItem();
        if (d == null) {
            slotGridPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel h = new JLabel("Enter a valid date to check slot availability");
            h.setFont(new Font("Segoe UI", Font.PLAIN, 12)); h.setForeground(TEXT_SECONDARY);
            slotGridPanel.add(h); selectedSlot = null;
        } else {
            slotGridPanel.setLayout(new GridLayout(0, 3, 10, 10));
            for (TimeSlot s : TimeSlot.values()) {
                boolean taken = isSlotTaken(d, v, s, null);
                if (taken && selectedSlot == s) selectedSlot = null;
                boolean sel = !taken && selectedSlot == s;
                SlotBtn btn = new SlotBtn(s, taken, sel);
                btn.addActionListener(e -> { selectedSlot = s; refreshSlotGrid(); });
                slotGridPanel.add(btn);
            }
        }
        slotGridPanel.revalidate(); slotGridPanel.repaint();
    }

    private void handleSaveEvent() {
        String name = nameField.getText().trim();
        String org = organizerField.getText().trim();
        String desc = descArea.getText().trim();
        String pTxt = participantsField.getText().trim();
        String fUrl = formUrlField.getText().trim();
        Category cat = (Category) categoryCombo.getSelectedItem();
        Venue ven = (Venue) venueCombo.getSelectedItem();
        LocalDate date = tryParseDate(dateField.getText());

        if (name.isEmpty()) { formError.setText("Please enter an event name."); return; }
        if (date == null) { formError.setText("Please enter a valid date (YYYY-MM-DD)."); return; }
        if (ven == null) { formError.setText("Please select a venue."); return; }
        if (selectedSlot == null) { formError.setText("Please select a time slot."); return; }
        if (isSlotTaken(date, ven, selectedSlot, null)) {
            formError.setText("Slot conflict! Pick another."); selectedSlot = null; refreshSlotGrid(); return;
        }
        int p = 0;
        if (!pTxt.isEmpty()) {
            try { p = Integer.parseInt(pTxt); if (p < 0) { formError.setText("Participants can't be negative."); return; } }
            catch (NumberFormatException ex) { formError.setText("Participants must be a number."); return; }
        }
        if (p > ven.capacity) { formError.setText("Participants (" + p + ") exceed venue capacity (" + ven.capacity + ")."); return; }
        if (!fUrl.isEmpty() && !fUrl.matches("^https?://.+\\..+")) { formError.setText("URL must start with http:// or https://"); return; }

        eventList.add(new CollegeEvent(idCounter++, name, date, selectedSlot, ven, cat,
                org.isEmpty() ? "Not specified" : org, desc.isEmpty() ? "No description." : desc,
                p, fUrl.isEmpty() ? null : fUrl));
        persistEvents();
        resetCreateForm(); refreshEventListUI(); cardLayout.show(mainPanel, "HOME");
    }

    private void resetCreateForm() {
        if (nameField != null) nameField.setText("");
        if (organizerField != null) organizerField.setText("");
        if (dateField != null) dateField.setText("");
        if (descArea != null) descArea.setText("");
        if (participantsField != null) participantsField.setText("");
        if (formUrlField != null) formUrlField.setText("");
        if (categoryCombo != null) categoryCombo.setSelectedIndex(0);
        if (venueCombo != null) venueCombo.setSelectedIndex(0);
        selectedSlot = null;
        if (formError != null) formError.setText(" ");
        refreshSlotGrid();
    }

    private void openCreatePage() { resetCreateForm(); cardLayout.show(mainPanel, "CREATE"); }

    // ═════════════════════════════════════════════════════════════════════
    //  EVENT DETAILS PAGE
    // ═════════════════════════════════════════════════════════════════════
    private void showEventDetails(CollegeEvent ev) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);
        top.setBorder(BorderFactory.createEmptyBorder(20, 32, 10, 32));
        JButton back = new JButton("← Back to Dashboard");
        styleLink(back); back.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));
        top.add(back, BorderLayout.WEST);
        page.add(top, BorderLayout.NORTH);

        ShadowCard card = new ShadowCard(18);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        // badge
        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bRow.setOpaque(false); bRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bRow.add(new Pill(ev.category.label, ev.category.color, Color.WHITE));

        JLabel nm = new JLabel(ev.name);
        nm.setFont(new Font("Segoe UI", Font.BOLD, 26)); nm.setForeground(TEXT_PRIMARY);
        nm.setAlignmentX(Component.LEFT_ALIGNMENT);
        nm.setBorder(BorderFactory.createEmptyBorder(10, 0, 16, 0));

        JPanel grid = new JPanel(new GridLayout(0, 2, 20, 14));
        grid.setOpaque(false); grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.add(infoBlock("Date", formatDate(ev.date)));
        grid.add(infoBlock("Time Slot", ev.slot.label));
        grid.add(infoBlock("Venue", ev.venue.label + "  (cap. " + ev.venue.capacity + ")"));
        grid.add(infoBlock("Organizer", ev.organizer));
        grid.add(infoBlock("Participants", ev.participants + " / " + ev.venue.capacity));

        JLabel dTitle = new JLabel("Description");
        dTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); dTitle.setForeground(TEXT_PRIMARY);
        dTitle.setBorder(BorderFactory.createEmptyBorder(24, 0, 6, 0)); dTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea dArea = new JTextArea(ev.description);
        dArea.setEditable(false); dArea.setLineWrap(true); dArea.setWrapStyleWord(true);
        dArea.setFont(new Font("Segoe UI", Font.PLAIN, 14)); dArea.setForeground(TEXT_SECONDARY);
        dArea.setBackground(CARD); dArea.setBorder(null); dArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel actRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actRow.setOpaque(false); actRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actRow.setBorder(BorderFactory.createEmptyBorder(26, 0, 0, 0));

        if (ev.formUrl != null && !ev.formUrl.isEmpty()) {
            SmoothButton fb = new SmoothButton("Open Registration Form", ACCENT_GREEN, new Color(0x12,0x8A,0x3E), Color.WHITE, 12);
            fb.addActionListener(e -> openUrl(ev.formUrl));
            actRow.add(fb);
        } else {
            JLabel nf = new JLabel("No registration form attached");
            nf.setFont(new Font("Segoe UI", Font.PLAIN, 12)); nf.setForeground(TEXT_SECONDARY);
            actRow.add(nf);
        }
        SmoothButton db = new SmoothButton("Delete Event", new Color(0xFE,0xE2,0xE2), new Color(0xFC,0xCA,0xCA), DANGER, 12);
        db.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(frame, "Delete \"" + ev.name + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) { eventList.remove(ev); persistEvents(); refreshEventListUI(); cardLayout.show(mainPanel, "HOME"); }
        });
        actRow.add(db);

        card.add(bRow); card.add(nm); card.add(grid); card.add(dTitle); card.add(dArea); card.add(actRow);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx=0; gc.gridy=0; gc.weightx=1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0,32,32,32); gc.anchor = GridBagConstraints.NORTH;
        wrap.add(card, gc);

        JScrollPane sc = new JScrollPane(wrap);
        sc.setBorder(null); sc.getViewport().setBackground(BG); sc.getVerticalScrollBar().setUnitIncrement(16);
        page.add(sc, BorderLayout.CENTER);

        mainPanel.add(page, "DETAILS");
        cardLayout.show(mainPanel, "DETAILS");
    }

    private JPanel infoBlock(String label, String value) {
        JPanel p = new JPanel(); p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(TEXT_SECONDARY); l.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 15)); v.setForeground(TEXT_PRIMARY); v.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(4)); p.add(v);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════
    public boolean isSlotTaken(LocalDate d, Venue v, TimeSlot s, Integer exId) {
        if (d == null || v == null || s == null) return false;
        for (CollegeEvent ev : eventList)
            if (ev.date.equals(d) && ev.venue == v && ev.slot == s && (exId == null || ev.id != exId)) return true;
        return false;
    }
    private void openUrl(String url) {
        try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new java.net.URI(url));
              else JOptionPane.showMessageDialog(frame, "Copy: " + url); }
        catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage()); }
    }
    private LocalDate tryParseDate(String t) {
        if (t == null || t.trim().isEmpty()) return null;
        try { return LocalDate.parse(t.trim(), DateTimeFormatter.ISO_LOCAL_DATE); } catch (DateTimeParseException e) { return null; }
    }
    private String formatDate(LocalDate d) { return d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")); }
    private String truncate(String s, int m) { return s == null ? "" : s.length() <= m ? s : s.substring(0, m) + "…"; }
    private JLabel fLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 13)); l.setForeground(TEXT_PRIMARY); return l;
    }
    private void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13)); f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(BORDER, 10, 1), BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }
    private void styleCombo(JComboBox<?> c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13)); c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(BORDER, 10, 1), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
    }
    private void styleLink(JButton b) {
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setForeground(PIET_BLUE); b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setHorizontalAlignment(SwingConstants.LEFT);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DATA MODEL
    // ═════════════════════════════════════════════════════════════════════
    public enum Category {
        TECHNICAL("Technical", new Color(0x25, 0x63, 0xEB)),
        CULTURAL("Cultural",   new Color(0xEC, 0x48, 0x99)),
        SPORTS("Sports",       new Color(0x16, 0xA3, 0x4A)),
        WORKSHOP("Workshop",   new Color(0xF5, 0x9E, 0x0B)),
        SEMINAR("Seminar",     new Color(0x06, 0xB6, 0xD4)),
        OTHER("Other",         new Color(0x8B, 0x5C, 0xF6));
        public final String label; public final Color color;
        Category(String l, Color c) { label = l; color = c; }
        @Override public String toString() { return label; }
    }

    public enum Venue {
        MAIN_AUDITORIUM("Main Auditorium", 500),
        SEMINAR_HALL_A("Seminar Hall A", 120),
        SEMINAR_HALL_B("Seminar Hall B", 100),
        SPORTS_GROUND("Sports Ground", 1000),
        CONFERENCE_ROOM("Conference Room", 40);
        public final String label; public final int capacity;
        Venue(String l, int c) { label = l; capacity = c; }
        @Override public String toString() { return label; }
    }

    public enum TimeSlot {
        SLOT_1("9:00 AM – 11:00 AM"), SLOT_2("11:00 AM – 1:00 PM"), SLOT_3("2:00 PM – 4:00 PM"),
        SLOT_4("4:00 PM – 6:00 PM"), SLOT_5("6:00 PM – 8:00 PM");
        public final String label;
        TimeSlot(String l) { label = l; }
        @Override public String toString() { return label; }
    }

    public static class CollegeEvent {
        public final int id; public String name; public LocalDate date; public TimeSlot slot;
        public Venue venue; public Category category; public String organizer, description, formUrl;
        public int participants;
        public CollegeEvent(int id, String name, LocalDate date, TimeSlot slot, Venue venue,
                Category category, String organizer, String description, int participants, String formUrl) {
            this.id = id; this.name = name; this.date = date; this.slot = slot; this.venue = venue;
            this.category = category; this.organizer = organizer; this.description = description;
            this.participants = participants; this.formUrl = formUrl;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CUSTOM COMPONENTS
    // ═════════════════════════════════════════════════════════════════════

    /** White card with a realistic soft painted shadow. */
    static class ShadowCard extends JPanel {
        private final int radius;
        ShadowCard(int radius) { this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // outer shadow
            g2.setColor(SHADOW_MED);
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, radius, radius);
            // card body
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, radius, radius);
            // top highlight
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A button with rounded fill, smooth hover gradient. */
    static class SmoothButton extends JButton {
        private final Color c1, c2; private final int rad;
        SmoothButton(String text, Color bg, Color hover, Color fg, int radius) {
            super(text); c1 = bg; c2 = hover; rad = radius;
            setContentAreaFilled(false); setOpaque(false); setFocusPainted(false); setBorderPainted(false);
            setForeground(fg); setFont(new Font("Segoe UI", Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? c2 : c1);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Category/date pill badge. */
    static class Pill extends JLabel {
        private final Color bg;
        Pill(String text, Color bg, Color fg) {
            super(text); this.bg = bg; setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9)); setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose(); super.paintComponent(g);
        }
    }

    /** Slot picker button — Available / Selected / Booked. */
    static class SlotBtn extends JButton {
        private final boolean taken, selected;
        SlotBtn(TimeSlot s, boolean taken, boolean selected) {
            super("<html><div style='text-align:center'><b>" + s.label + "</b><br>"
                + (taken ? "Booked" : "Available") + "</div></html>");
            this.taken = taken; this.selected = selected;
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setPreferredSize(new Dimension(140, 54));
            setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false);
            if (taken) { setEnabled(false); setCursor(Cursor.getDefaultCursor()); }
            else setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg, fg, bdr;
            if (taken) { bg = new Color(0xFE,0xE2,0xE2); fg = new Color(0xB9,0x1C,0x1C); bdr = new Color(0xFC,0xA5,0xA5); }
            else if (selected) { bg = PIET_BLUE; fg = Color.WHITE; bdr = PIET_BLUE_DEEP; }
            else { bg = new Color(0xEC,0xFD,0xF3); fg = new Color(0x15,0x80,0x3D); bdr = new Color(0xA7,0xF3,0xD0); }
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(bdr); g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12);
            g2.dispose(); setForeground(fg); super.paintComponent(g);
        }
    }

    /** Rounded stroke border for text fields. */
    static class RoundBorder extends AbstractBorder {
        private final Color c; private final int r, t;
        RoundBorder(Color c, int r, int t) { this.c = c; this.r = r; this.t = t; }
        @Override public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c); g2.setStroke(new BasicStroke(t));
            g2.drawRoundRect(x, y, w - t, h - t, r, r); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(6, 10, 6, 10); }
    }

    /** Simple doc listener. */
    static class DocListener implements DocumentListener {
        private final Runnable a; DocListener(Runnable a) { this.a = a; }
        @Override public void insertUpdate(DocumentEvent e) { a.run(); }
        @Override public void removeUpdate(DocumentEvent e) { a.run(); }
        @Override public void changedUpdate(DocumentEvent e) { a.run(); }
    }
}