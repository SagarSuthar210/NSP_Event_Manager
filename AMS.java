import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

// ===================== RoundedPanel =====================
/** A JPanel with rounded corners and an optional border color, used as a "card". */
class RoundedPanel extends JPanel {
    private final int radius;
    private Color borderColor = null;

    public RoundedPanel(int radius) {
        this.radius = radius;
        setOpaque(false);
    }

    public void setBorderColor(Color c) { this.borderColor = c; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

// ===================== StatCard =====================
/** Small stat card: label, big value, and colored sub-label (used for the top row of KPIs). */
class StatCard extends RoundedPanel {

    public StatCard(String label, String value, String subLabel, Color accent) {
        super(16);
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JLabel iconDot = new JLabel();
        iconDot.setOpaque(true);
        iconDot.setBackground(accent);
        iconDot.setPreferredSize(new Dimension(10, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(new Color(110, 110, 130));
        JPanel dotWrap = new JPanel();
        dotWrap.setOpaque(false);
        dotWrap.add(iconDot);
        top.add(lbl, BorderLayout.WEST);
        top.add(dotWrap, BorderLayout.EAST);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 30));
        val.setForeground(new Color(30, 30, 40));

        JLabel sub = new JLabel(subLabel);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(accent);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(Box.createVerticalStrut(6));
        center.add(val);
        center.add(Box.createVerticalStrut(4));
        center.add(sub);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }
}

// ===================== DonutChart =====================
/** Simple donut chart drawn with Graphics2D arcs, with a centered percentage label. */
class DonutChart extends JPanel {

    public static class Slice {
        String label; double value; Color color;
        public Slice(String label, double value, Color color) {
            this.label = label; this.value = value; this.color = color;
        }
    }

    private final List<Slice> slices;
    private final String centerBig;
    private final String centerSmall;

    public DonutChart(List<Slice> slices, String centerBig, String centerSmall) {
        this.slices = slices;
        this.centerBig = centerBig;
        this.centerSmall = centerSmall;
        setOpaque(false);
        setPreferredSize(new Dimension(180, 180));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 10;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        int thickness = size / 5;

        double total = 0;
        for (Slice s : slices) total += s.value;

        double start = 90;
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        for (Slice s : slices) {
            double angle = (s.value / total) * 360.0;
            g2.setColor(s.color);
            g2.draw(new java.awt.geom.Arc2D.Double(
                    x + thickness / 2.0, y + thickness / 2.0,
                    size - thickness, size - thickness,
                    start, -angle, java.awt.geom.Arc2D.OPEN));
            start -= angle;
        }
        g2.dispose();

        // center labels
        Graphics2D g3 = (Graphics2D) g.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(new Color(30, 30, 40));
        g3.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g3.getFontMetrics();
        int bw = fm.stringWidth(centerBig);
        g3.drawString(centerBig, getWidth() / 2 - bw / 2, getHeight() / 2 - 2);

        g3.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g3.setColor(new Color(120, 120, 135));
        fm = g3.getFontMetrics();
        int sw = fm.stringWidth(centerSmall);
        g3.drawString(centerSmall, getWidth() / 2 - sw / 2, getHeight() / 2 + 16);
        g3.dispose();
    }
}

// ===================== LineChart =====================
/** Simple filled line chart (area chart) drawn with Graphics2D, values are 0-100 percentages. */
class LineChart extends JPanel {
    private final int[] values;
    private final String[] xLabels;
    private final Color lineColor;

    public LineChart(int[] values, String[] xLabels, Color lineColor) {
        this.values = values;
        this.xLabels = xLabels;
        this.lineColor = lineColor;
        setOpaque(false);
        setPreferredSize(new Dimension(400, 200));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padL = 40, padR = 15, padT = 15, padB = 25;
        int w = getWidth() - padL - padR;
        int h = getHeight() - padT - padB;

        // gridlines + y labels
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(new Color(230, 230, 235));
        int[] yTicks = {0, 25, 50, 75, 100};
        for (int t : yTicks) {
            int y = padT + h - (int) (h * (t / 100.0));
            g2.drawLine(padL, y, padL + w, y);
            g2.setColor(new Color(140, 140, 150));
            g2.drawString(t + "%", 5, y + 4);
            g2.setColor(new Color(230, 230, 235));
        }

        if (values.length < 2) { g2.dispose(); return; }

        Path2D.Double area = new Path2D.Double();
        Path2D.Double line = new Path2D.Double();
        for (int i = 0; i < values.length; i++) {
            double px = padL + (w * i / (double) (values.length - 1));
            double py = padT + h - (h * (values[i] / 100.0));
            if (i == 0) { area.moveTo(px, padT + h); area.lineTo(px, py); line.moveTo(px, py); }
            else { area.lineTo(px, py); line.lineTo(px, py); }
        }
        area.lineTo(padL + w, padT + h);
        area.closePath();

        g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 40));
        g2.fill(area);
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(line);

        // dots
        g2.setColor(lineColor);
        for (int i = 0; i < values.length; i++) {
            double px = padL + (w * i / (double) (values.length - 1));
            double py = padT + h - (h * (values[i] / 100.0));
            g2.fillOval((int) px - 3, (int) py - 3, 6, 6);
        }

        // x labels
        g2.setColor(new Color(140, 140, 150));
        for (int i = 0; i < xLabels.length; i++) {
            double px = padL + (w * i / (double) (xLabels.length - 1));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(xLabels[i], (int) px - fm.stringWidth(xLabels[i]) / 2, getHeight() - 6);
        }
        g2.dispose();
    }
}

// ===================== BarChart =====================
/** Simple bar chart drawn with Graphics2D. */
class BarChart extends JPanel {
    private final String[] labels;
    private final int[] values;
    private final Color[] colors;
    private final int maxValue;

    public BarChart(String[] labels, int[] values, Color[] colors, int maxValue) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        this.maxValue = maxValue;
        setOpaque(false);
        setPreferredSize(new Dimension(400, 220));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padL = 35, padR = 15, padT = 10, padB = 25;
        int w = getWidth() - padL - padR;
        int h = getHeight() - padT - padB;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        int[] yTicks = {0, 25, 50, 75, 100};
        for (int t : yTicks) {
            if (t > maxValue) continue;
            int y = padT + h - (int) (h * (t / (double) maxValue));
            g2.setColor(new Color(235, 235, 240));
            g2.drawLine(padL, y, padL + w, y);
            g2.setColor(new Color(140, 140, 150));
            g2.drawString(String.valueOf(t), 5, y + 4);
        }

        int n = values.length;
        int slot = w / n;
        int barWidth = (int) (slot * 0.45);
        for (int i = 0; i < n; i++) {
            int barH = (int) (h * (values[i] / (double) maxValue));
            int x = padL + i * slot + (slot - barWidth) / 2;
            int y = padT + h - barH;
            g2.setColor(colors[i % colors.length]);
            g2.fillRoundRect(x, y, barWidth, barH, 6, 6);

            g2.setColor(new Color(90, 90, 105));
            FontMetrics fm = g2.getFontMetrics();
            String lbl = labels[i];
            g2.drawString(lbl, padL + i * slot + slot / 2 - fm.stringWidth(lbl) / 2, getHeight() - 6);
        }
        g2.dispose();
    }
}

// ===================== Sidebar =====================
/** Left navigation sidebar: logo header, list of nav items (one highlighted as active), and a footer panel. */
class Sidebar extends JPanel {

    public Sidebar(Color bgColor, Color activeColor, String appName, String appSub,
                    String[] items, int activeIndex, JComponent footer, Runnable[] onClick) {
        setLayout(new BorderLayout());
        setBackground(bgColor);
        setPreferredSize(new Dimension(215, 10));

        // Logo header
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 20));
        JLabel logoIcon = new JLabel(" ");
        logoIcon.setOpaque(true);
        logoIcon.setBackground(activeColor);
        logoIcon.setPreferredSize(new Dimension(34, 34));
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(appName);
        name.setForeground(Color.WHITE);
        name.setFont(new Font("SansSerif", Font.BOLD, 15));
        JLabel sub = new JLabel(appSub);
        sub.setForeground(new Color(180, 180, 200));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 9));
        textPanel.add(name);
        textPanel.add(sub);
        logoPanel.add(logoIcon);
        logoPanel.add(textPanel);

        // Nav items
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < items.length; i++) {
            JPanel item = buildNavItem(items[i], i == activeIndex, activeColor);
            if (onClick != null && onClick[i] != null) {
                final Runnable r = onClick[i];
                item.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { r.run(); }
                });
            }
            navPanel.add(item);
            navPanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(navPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(logoPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        if (footer != null) add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildNavItem(String text, boolean active, Color activeColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setMaximumSize(new Dimension(500, 38));
        p.setPreferredSize(new Dimension(195, 38));
        p.setBackground(active ? activeColor : new Color(0, 0, 0, 0));
        p.setOpaque(active);
        p.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 12));
        lbl.setForeground(active ? Color.WHITE : new Color(200, 200, 215));
        p.add(lbl, BorderLayout.CENTER);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return p;
    }
}

// ===================== TopHeader =====================
/** Top header bar: hamburger + welcome text on the left, notification bell + avatar on the right. */
class TopHeader extends JPanel {

    public TopHeader(String welcomeText, String subText, int notifCount) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JLabel menu = new JLabel("\u2630");
        menu.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        JLabel welcome = new JLabel(welcomeText);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel sub = new JLabel(subText);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(new Color(130, 130, 145));
        textWrap.add(welcome);
        if (subText != null && !subText.isEmpty()) textWrap.add(sub);
        left.add(menu);
        left.add(textWrap);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        JLabel bell = new JLabel("\uD83D\uDD14");
        bell.setFont(new Font("SansSerif", Font.PLAIN, 16));
        if (notifCount > 0) {
            JLabel badge = new JLabel(String.valueOf(notifCount));
            badge.setOpaque(true);
            badge.setBackground(new Color(230, 60, 70));
            badge.setForeground(Color.WHITE);
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setPreferredSize(new Dimension(15, 15));
            JLayeredPane bellWrap = new JLayeredPane();
            bellWrap.setPreferredSize(new Dimension(24, 20));
            bell.setBounds(0, 0, 20, 20);
            badge.setBounds(12, -2, 15, 15);
            bellWrap.add(bell, Integer.valueOf(0));
            bellWrap.add(badge, Integer.valueOf(1));
            right.add(bellWrap);
        } else {
            right.add(bell);
        }
        JLabel avatar = new JLabel("\uD83D\uDC64");
        avatar.setOpaque(true);
        avatar.setBackground(new Color(230, 230, 240));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(34, 34));
        right.add(avatar);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }
}

// ===================== StatusCellRenderer =====================
/** Renders "Present"/"Absent" (or similar) cells with green/red colored text. */
class StatusCellRenderer extends DefaultTableCellRenderer {
    private final int statusColumn;

    public StatusCellRenderer(int statusColumn) {
        this.statusColumn = statusColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setForeground(new Color(60, 60, 70));
        setFont(getFont().deriveFont(Font.PLAIN));
        if (column == statusColumn && value != null) {
            String v = value.toString();
            if (v.equalsIgnoreCase("Present")) setForeground(new Color(40, 170, 120));
            else if (v.equalsIgnoreCase("Absent")) setForeground(new Color(220, 70, 80));
            setFont(getFont().deriveFont(Font.BOLD));
        }
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        return c;
    }
}

// ===================== LoginPanel =====================
/** Login screen. On successful "login" it calls onLogin.accept(role) with "STUDENT"/"FACULTY"/"ADMIN". */
class LoginPanel extends JPanel {

    private String selectedRole = "STUDENT";

    public LoginPanel(Consumer<String> onLogin) {
        setLayout(new GridBagLayout());
        setBackground(new Color(244, 245, 250));

        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(400, 520));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));

        JLabel logo = new JLabel("AMS");
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        logo.setForeground(new Color(76, 46, 158));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Attendance Management System");
        title.setFont(new Font("SansSerif", Font.PLAIN, 12));
        title.setForeground(new Color(120, 120, 135));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Welcome Back");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

        // Role selector
        JPanel rolePanel = new JPanel(new GridLayout(1, 3, 8, 0));
        rolePanel.setOpaque(false);
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rolePanel.setMaximumSize(new Dimension(400, 40));
        JToggleButton studentBtn = new JToggleButton("Student", true);
        JToggleButton facultyBtn = new JToggleButton("Faculty");
        JToggleButton adminBtn = new JToggleButton("Admin");
        ButtonGroup group = new ButtonGroup();
        for (JToggleButton b : new JToggleButton[]{studentBtn, facultyBtn, adminBtn}) {
            group.add(b);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            rolePanel.add(b);
        }
        studentBtn.addActionListener(e -> selectedRole = "STUDENT");
        facultyBtn.addActionListener(e -> selectedRole = "FACULTY");
        adminBtn.addActionListener(e -> selectedRole = "ADMIN");

        JLabel userLbl = fieldLabel("Username / Email");
        JTextField userField = new JTextField("rahul.kumar");
        styleField(userField);

        JLabel passLbl = fieldLabel("Password");
        JPasswordField passField = new JPasswordField("password123");
        styleField(passField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(400, 42));
        loginBtn.setBackground(new Color(76, 46, 158));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        loginBtn.addActionListener(e -> onLogin.accept(selectedRole));

        JLabel hint = new JLabel("Demo: any username/password works. Pick a role above.");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hint.setForeground(new Color(150, 150, 165));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        card.add(logo);
        card.add(title);
        card.add(heading);
        card.add(rolePanel);
        card.add(Box.createVerticalStrut(20));
        card.add(userLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(userField);
        card.add(Box.createVerticalStrut(14));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(passField);
        card.add(Box.createVerticalStrut(24));
        card.add(loginBtn);
        card.add(hint);

        add(card);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(new Color(90, 90, 105));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleField(JTextField f) {
        f.setMaximumSize(new Dimension(400, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 230), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
    }
}

// ===================== StudentDashboardPanel =====================
class StudentDashboardPanel extends JPanel {

    private static final Color BG = new Color(30, 27, 75);
    private static final Color ACCENT = new Color(91, 60, 196);
    private static final Color PAGE_BG = new Color(244, 245, 250);

    public StudentDashboardPanel(Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        String[] items = {"Dashboard", "My Attendance", "Attendance History", "My Profile",
                "Notifications", "Change Password", "Logout"};
        Runnable[] actions = new Runnable[items.length];
        actions[items.length - 1] = onLogout;

        JPanel footer = buildHelpFooter();
        Sidebar sidebar = new Sidebar(BG, ACCENT, "AMS", "Attendance Management", items, 0, footer, actions);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(PAGE_BG);
        main.add(new TopHeader("Welcome, Rahul Kumar \uD83D\uDC4B", null, 1), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildMiddleRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildBottomRow());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 100));
        row.add(new StatCard("Overall Attendance", "82%", "Good", new Color(76, 46, 158)));
        row.add(new StatCard("Classes Attended", "41", "of 50", new Color(40, 170, 120)));
        row.add(new StatCard("Classes Absent", "9", "9 of 50", new Color(220, 70, 80)));
        row.add(new StatCard("Classes Remaining", "0", "Today", new Color(230, 150, 40)));
        return row;
    }

    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 300));

        // Attendance by subject (donut + legend)
        RoundedPanel donutCard = card();
        donutCard.setLayout(new BorderLayout());
        donutCard.add(sectionTitle("Attendance by Subject"), BorderLayout.NORTH);
        JPanel donutBody = new JPanel(new BorderLayout());
        donutBody.setOpaque(false);
        List<DonutChart.Slice> slices = Arrays.asList(
                new DonutChart.Slice("Data Structures", 90, new Color(76, 110, 230)),
                new DonutChart.Slice("Java Programming", 75, new Color(230, 150, 60)),
                new DonutChart.Slice("DBMS", 87, new Color(60, 190, 160)),
                new DonutChart.Slice("Operating Systems", 80, new Color(230, 90, 100)),
                new DonutChart.Slice("Digital Logic", 95, new Color(150, 90, 220))
        );
        donutBody.add(new DonutChart(slices, "82%", "Overall"), BorderLayout.WEST);
        JPanel legend = new JPanel();
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 0));
        String[] subjPct = {"90%", "75%", "87%", "80%", "95%"};
        for (int i = 0; i < slices.size(); i++) {
            legend.add(legendRow(slices.get(i).color, slices.get(i).label, subjPct[i]));
            legend.add(Box.createVerticalStrut(6));
        }
        donutBody.add(legend, BorderLayout.CENTER);
        donutCard.add(donutBody, BorderLayout.CENTER);

        // Recent attendance table
        RoundedPanel tableCard = card();
        tableCard.setLayout(new BorderLayout());
        JPanel header = sectionTitle("Recent Attendance");
        tableCard.add(header, BorderLayout.NORTH);
        String[] cols = {"Date", "Subject", "Status", "Faculty"};
        Object[][] data = {
                {"26 May 2025", "Data Structures", "Present", "Amit Sharma"},
                {"26 May 2025", "Java Programming", "Absent", "Priya Verma"},
                {"25 May 2025", "DBMS", "Present", "Rohit Singh"},
                {"25 May 2025", "Operating Systems", "Present", "Sneha Patel"},
                {"24 May 2025", "Digital Logic", "Present", "Amit Sharma"}
        };
        JTable table = buildStatusTable(cols, data, 2);
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(table.getTableHeader(), BorderLayout.NORTH);
        tableWrap.add(table, BorderLayout.CENTER);
        tableCard.add(tableWrap, BorderLayout.CENTER);
        JButton viewAll = ghostButton("View All");
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnWrap.setOpaque(false);
        btnWrap.add(viewAll);
        tableCard.add(btnWrap, BorderLayout.SOUTH);

        row.add(donutCard);
        row.add(tableCard);
        return row;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 260));

        RoundedPanel lineCard = card();
        lineCard.setLayout(new BorderLayout());
        lineCard.add(sectionTitle("Attendance Overview (Last 30 Days)"), BorderLayout.NORTH);
        int[] vals = {80, 70, 85, 60, 75, 90, 65, 78, 82, 88};
        String[] xl = {"28 Apr", "3 May", "8 May", "13 May", "18 May", "23 May", "28 May"};
        lineCard.add(new LineChart(vals, xl, ACCENT), BorderLayout.CENTER);

        RoundedPanel noticeCard = card();
        noticeCard.setBackground(new Color(255, 246, 225));
        noticeCard.setLayout(new BoxLayout(noticeCard, BoxLayout.Y_AXIS));
        noticeCard.setPreferredSize(new Dimension(260, 240));
        JLabel bell = new JLabel("\u26A0 Important Notice");
        bell.setFont(new Font("SansSerif", Font.BOLD, 13));
        bell.setForeground(new Color(180, 130, 20));
        JTextArea msg = new JTextArea("Your attendance in Java Programming is 75%. Maintain at least 75% to be eligible for exams.");
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setOpaque(false);
        msg.setEditable(false);
        msg.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msg.setForeground(new Color(120, 90, 20));
        msg.setBorder(BorderFactory.createEmptyBorder(10, 0, 14, 0));
        JButton details = new JButton("View Details");
        details.setBackground(ACCENT);
        details.setForeground(Color.WHITE);
        details.setFocusPainted(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        bell.setAlignmentX(Component.LEFT_ALIGNMENT);
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        noticeCard.add(bell);
        noticeCard.add(msg);
        noticeCard.add(details);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(260, 240));
        right.add(noticeCard, BorderLayout.CENTER);

        row.add(lineCard, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ---------- helpers ----------

    private RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(16);
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    private JPanel sectionTitle(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel legendRow(Color c, String label, String pct) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(220, 20));
        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(c);
        JLabel text = new JLabel(" " + label);
        text.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(text);
        JLabel pctLbl = new JLabel(pct);
        pctLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        row.add(left, BorderLayout.WEST);
        row.add(pctLbl, BorderLayout.EAST);
        return row;
    }

    private JTable buildStatusTable(String[] cols, Object[][] data, int statusCol) {
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setShowGrid(false);
        table.setDefaultRenderer(Object.class, new StatusCellRenderer(statusCol));
        return table;
    }

    private JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return b;
    }

    private JPanel buildHelpFooter() {
        RoundedPanel p = new RoundedPanel(12);
        p.setBackground(new Color(45, 40, 100));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.setMaximumSize(new Dimension(190, 110));
        JLabel t = new JLabel("Need Help?");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 12));
        JTextArea msg = new JTextArea("If you have any queries, contact your faculty or admin.");
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setOpaque(false);
        msg.setForeground(new Color(190, 190, 210));
        msg.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JButton contact = new JButton("Contact Support");
        contact.setBackground(ACCENT);
        contact.setForeground(Color.WHITE);
        contact.setFocusPainted(false);
        contact.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        p.add(msg);
        p.add(Box.createVerticalStrut(8));
        p.add(contact);
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(10, 10, 16, 10));
        wrap.add(p);
        return wrap;
    }
}

// ===================== FacultyDashboardPanel =====================
class FacultyDashboardPanel extends JPanel {

    private static final Color BG = new Color(15, 23, 64);
    private static final Color ACCENT = new Color(45, 110, 220);
    private static final Color PAGE_BG = new Color(244, 245, 250);

    public FacultyDashboardPanel(Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        String[] items = {"Dashboard", "Mark Attendance", "My Classes", "Attendance History",
                "Reports", "Students", "Notifications", "Profile", "Logout"};
        Runnable[] actions = new Runnable[items.length];
        actions[items.length - 1] = onLogout;

        Sidebar sidebar = new Sidebar(BG, ACCENT, "AMS", "Management System", items, 0, null, actions);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(PAGE_BG);
        main.add(new TopHeader("Welcome, Amit Sharma", "Computer Science Department", 2), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildScheduleRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildRecentClassesRow());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 100));
        row.add(new StatCard("Total Classes", "6", "", ACCENT));
        row.add(new StatCard("Classes Today", "2", "", new Color(230, 150, 40)));
        row.add(new StatCard("Present Today", "42", "87%", new Color(40, 170, 120)));
        row.add(new StatCard("Absent Today", "6", "13%", new Color(220, 70, 80)));
        return row;
    }

    private JPanel buildScheduleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 260));

        RoundedPanel scheduleCard = card();
        scheduleCard.setLayout(new BorderLayout());
        scheduleCard.add(sectionTitle("Today's Schedule"), BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(scheduleItem("Data Structures", "CSE 3rd Year - Section A", "10:00 AM - 11:00 AM"));
        list.add(Box.createVerticalStrut(10));
        list.add(scheduleItem("Operating Systems", "CSE 3rd Year - Section B", "12:00 PM - 01:00 PM"));
        scheduleCard.add(list, BorderLayout.CENTER);

        RoundedPanel donutCard = card();
        donutCard.setLayout(new BorderLayout());
        donutCard.add(sectionTitle("Attendance Summary (Today)"), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        java.util.List<DonutChart.Slice> slices = java.util.Arrays.asList(
                new DonutChart.Slice("Present", 42, new Color(40, 170, 120)),
                new DonutChart.Slice("Absent", 6, new Color(220, 70, 80))
        );
        body.add(new DonutChart(slices, "87%", "Present"), BorderLayout.WEST);
        JPanel legend = new JPanel();
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 0));
        legend.add(legendRow(new Color(40, 170, 120), "Present", "42 (87%)"));
        legend.add(Box.createVerticalStrut(8));
        legend.add(legendRow(new Color(220, 70, 80), "Absent", "6 (13%)"));
        legend.add(Box.createVerticalStrut(8));
        legend.add(legendRow(new Color(190, 190, 200), "Total Students", "48"));
        body.add(legend, BorderLayout.CENTER);
        donutCard.add(body, BorderLayout.CENTER);

        row.add(scheduleCard);
        row.add(donutCard);
        return row;
    }

    private JPanel buildRecentClassesRow() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 260));

        RoundedPanel tableCard = card();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(sectionTitle("Recent Classes"), BorderLayout.NORTH);
        String[] cols = {"Date", "Class", "Subject", "Present", "Absent", "Action"};
        Object[][] data = {
                {"26 May 2025", "CSE 3rd Year A", "Data Structures", "42", "6", "View"},
                {"26 May 2025", "CSE 2nd Year B", "Java Programming", "38", "10", "View"},
                {"25 May 2025", "CSE 3rd Year B", "Operating Systems", "40", "13", "View"},
                {"25 May 2025", "CSE 2nd Year A", "DBMS", "35", "13", "View"}
        };
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setShowGrid(false);
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(table.getTableHeader(), BorderLayout.NORTH);
        tableWrap.add(table, BorderLayout.CENTER);
        tableCard.add(tableWrap, BorderLayout.CENTER);

        RoundedPanel actionsCard = card();
        actionsCard.setPreferredSize(new Dimension(230, 240));
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));
        actionsCard.add(sectionTitle("Quick Actions"));
        actionsCard.add(actionButton("Mark Attendance", new Color(45, 110, 220)));
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(actionButton("View Reports", new Color(45, 110, 220)));
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(actionButton("Export Attendance", new Color(230, 150, 40)));

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(230, 240));
        right.add(actionsCard, BorderLayout.CENTER);

        row.add(tableCard, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ---------- helpers ----------

    private RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(16);
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    private JPanel sectionTitle(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel scheduleItem(String subject, String detail, String time) {
        RoundedPanel p = new RoundedPanel(10);
        p.setBackground(new Color(245, 247, 252));
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.setMaximumSize(new Dimension(600, 60));

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        JLabel subj = new JLabel(subject);
        subj.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel det = new JLabel(detail);
        det.setFont(new Font("SansSerif", Font.PLAIN, 11));
        det.setForeground(new Color(120, 120, 135));
        JLabel t = new JLabel(time);
        t.setFont(new Font("SansSerif", Font.PLAIN, 11));
        t.setForeground(new Color(120, 120, 135));
        textWrap.add(subj);
        textWrap.add(det);
        textWrap.add(t);

        JButton mark = new JButton("Mark Attendance");
        mark.setBackground(ACCENT);
        mark.setForeground(Color.WHITE);
        mark.setFocusPainted(false);
        mark.setFont(new Font("SansSerif", Font.PLAIN, 11));

        p.add(textWrap, BorderLayout.WEST);
        p.add(mark, BorderLayout.EAST);
        return p;
    }

    private JPanel legendRow(Color c, String label, String pct) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(220, 20));
        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(c);
        JLabel text = new JLabel(" " + label);
        text.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(text);
        JLabel pctLbl = new JLabel(pct);
        pctLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        row.add(left, BorderLayout.WEST);
        row.add(pctLbl, BorderLayout.EAST);
        return row;
    }

    private JButton actionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(500, 36));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return b;
    }
}

// ===================== AdminDashboardPanel =====================
class AdminDashboardPanel extends JPanel {

    private static final Color BG = new Color(6, 60, 49);
    private static final Color ACCENT = new Color(30, 130, 100);
    private static final Color PAGE_BG = new Color(244, 245, 250);

    public AdminDashboardPanel(Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        String[] items = {"Dashboard", "Manage Students", "Manage Faculty", "Manage Subjects",
                "Manage Classes", "User Credentials", "Attendance Overview", "Reports",
                "Notifications", "Settings", "Logout"};
        Runnable[] actions = new Runnable[items.length];
        actions[items.length - 1] = onLogout;

        Sidebar sidebar = new Sidebar(BG, ACCENT, "AMS", "Management System", items, 0, null, actions);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(PAGE_BG);
        main.add(new TopHeader("Welcome, Admin", "Super Administrator", 2), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(16));
        content.add(buildMainRow());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 100));
        row.add(new StatCard("Total Students", "248", "+ 12 this month", ACCENT));
        row.add(new StatCard("Total Faculty", "28", "+ 2 this month", new Color(45, 110, 220)));
        row.add(new StatCard("Total Subjects", "18", "+ 1 this month", new Color(230, 150, 40)));
        row.add(new StatCard("Total Classes", "42", "", new Color(120, 90, 220)));
        row.add(new StatCard("Today's Attendance", "87%", "Overall", new Color(40, 170, 120)));
        row.add(new StatCard("Low Attendance Students", "15", "Below 75%", new Color(220, 70, 80)));
        return row;
    }

    private JPanel buildMainRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(3000, 420));

        RoundedPanel chartCard = card();
        chartCard.setLayout(new BorderLayout());
        chartCard.add(sectionTitle("System Overview"), BorderLayout.NORTH);
        String[] labels = {"Students", "Faculty", "Subjects", "Classes", "Attendance (%)"};
        int[] vals = {75, 40, 25, 50, 87};
        Color[] colors = {new Color(60, 190, 160), new Color(60, 190, 160), new Color(60, 190, 160),
                new Color(60, 190, 160), new Color(70, 130, 230)};
        chartCard.add(new BarChart(labels, vals, colors, 100), BorderLayout.CENTER);

        RoundedPanel activityCard = card();
        activityCard.setLayout(new BorderLayout());
        activityCard.add(sectionTitle("Recent Activities"), BorderLayout.NORTH);
        JPanel activityList = new JPanel();
        activityList.setOpaque(false);
        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.add(activityRow("New student Rohit Verma added", "26 May 2025, 10:30 AM", new Color(45, 110, 220)));
        activityList.add(Box.createVerticalStrut(10));
        activityList.add(activityRow("Faculty Priya Singh added", "26 May 2025, 09:45 AM", new Color(120, 90, 220)));
        activityList.add(Box.createVerticalStrut(10));
        activityList.add(activityRow("Attendance marked for Data Structures (3rd Year A)", "26 May 2025, 10:00 AM", new Color(220, 70, 80)));
        activityList.add(Box.createVerticalStrut(10));
        activityList.add(activityRow("New subject Artificial Intelligence added", "25 May 2025, 04:20 PM", new Color(230, 150, 40)));
        activityCard.add(activityList, BorderLayout.CENTER);
        JButton viewAll = linkButton("View All Activities");
        JPanel vaWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        vaWrap.setOpaque(false);
        vaWrap.add(viewAll);
        activityCard.add(vaWrap, BorderLayout.SOUTH);

        RoundedPanel lowAttCard = card();
        lowAttCard.setLayout(new BorderLayout());
        lowAttCard.add(sectionTitle("Low Attendance Students"), BorderLayout.NORTH);
        String[] cols = {"Student", "Class", "Attendance"};
        Object[][] data = {
                {"Rahul Kumar", "CSE 3rd Year A", "62%"},
                {"Sneha Gupta", "CSE 2nd Year B", "65%"},
                {"Aman Verma", "CSE 1st Year A", "68%"},
                {"Pooja Sharma", "CSE 3rd Year B", "70%"},
                {"Vikash Singh", "CSE 2nd Year A", "72%"}
        };
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.setShowGrid(false);
        table.setDefaultRenderer(Object.class, new StatusCellRenderer(-1));
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(table.getTableHeader(), BorderLayout.NORTH);
        tableWrap.add(table, BorderLayout.CENTER);
        lowAttCard.add(tableWrap, BorderLayout.CENTER);
        JButton viewAll2 = linkButton("View All");
        JPanel va2Wrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        va2Wrap.setOpaque(false);
        va2Wrap.add(viewAll2);
        lowAttCard.add(va2Wrap, BorderLayout.SOUTH);

        RoundedPanel actionsCard = card();
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));
        actionsCard.add(sectionTitle("Quick Actions"));
        actionsCard.add(actionButton("Add Student", new Color(30, 130, 100)));
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(actionButton("Add Faculty", new Color(45, 110, 220)));
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(actionButton("Add Subject", new Color(230, 150, 40)));
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(actionButton("Generate Report", new Color(120, 90, 220)));

        row.add(chartCard);
        row.add(activityCard);
        row.add(lowAttCard);
        row.add(actionsCard);
        return row;
    }

    // ---------- helpers ----------

    private RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(16);
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return p;
    }

    private JPanel sectionTitle(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel activityRow(String text, String time, Color dotColor) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(600, 46));
        JLabel dot = new JLabel(" ");
        dot.setOpaque(true);
        dot.setBackground(dotColor);
        dot.setPreferredSize(new Dimension(8, 8));
        JPanel dotWrap = new JPanel();
        dotWrap.setOpaque(false);
        dotWrap.add(dot);

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        JLabel txt = new JLabel("<html><body style='width:220px'>" + text + "</body></html>");
        txt.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel t = new JLabel(time);
        t.setFont(new Font("SansSerif", Font.PLAIN, 10));
        t.setForeground(new Color(150, 150, 165));
        textWrap.add(txt);
        textWrap.add(t);

        p.add(dotWrap, BorderLayout.WEST);
        p.add(textWrap, BorderLayout.CENTER);
        return p;
    }

    private JButton actionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(500, 36));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return b;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(ACCENT);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return b;
    }
}

// ===================== Main =====================
/**
 * Attendance Management System (AMS) - Java Swing front-end.
 * Login page routes to one of three dashboards: Student, Faculty, Admin.
 */
public class AMS {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AMS::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        JFrame frame = new JFrame("AMS - Attendance Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 860);
        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setLocationRelativeTo(null);

        CardLayout cardLayout = new CardLayout();
        JPanel root = new JPanel(cardLayout);

        Runnable goToLogin = () -> cardLayout.show(root, "LOGIN");

        LoginPanel loginPanel = new LoginPanel(role -> {
            switch (role) {
                case "FACULTY": cardLayout.show(root, "FACULTY"); break;
                case "ADMIN": cardLayout.show(root, "ADMIN"); break;
                default: cardLayout.show(root, "STUDENT");
            }
        });

        StudentDashboardPanel studentPanel = new StudentDashboardPanel(goToLogin);
        FacultyDashboardPanel facultyPanel = new FacultyDashboardPanel(goToLogin);
        AdminDashboardPanel adminPanel = new AdminDashboardPanel(goToLogin);

        root.add(loginPanel, "LOGIN");
        root.add(studentPanel, "STUDENT");
        root.add(facultyPanel, "FACULTY");
        root.add(adminPanel, "ADMIN");

        frame.setContentPane(root);
        frame.setVisible(true);
    }
}
