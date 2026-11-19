

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PharmacyManagement extends JFrame {

    private static final long serialVersionUID = 1L;

    // ================= INDUSTRIAL COLOR PALETTE =================
    private final Color SIDEBAR_BG = new Color(15, 23, 42);       // Deep Slate Navy
    private final Color SIDEBAR_HOVER = new Color(30, 41, 59);    // Slate Hover
    private final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);  // Industrial Blue Accent
    private final Color HEADER_BG = new Color(255, 255, 255);     // Crisp White Topbar
    private final Color APP_BG = new Color(241, 245, 249);        // Soft Cool Gray Canvas
    private final Color CARD_BG = new Color(255, 255, 255);       // Pure White Cards
    private final Color BORDER_GRAY = new Color(226, 232, 240);   // Subtle Divider / Border
    private final Color TEXT_PRIMARY = new Color(15, 23, 42);     // High-contrast Dark Text
    private final Color TEXT_MUTED = new Color(100, 116, 139);    // Secondary Label Text

    private final Color BLUE = new Color(37, 99, 235);
    private final Color GREEN = new Color(5, 150, 105);
    private final Color RED = new Color(220, 38, 38);
    private final Color AMBER = new Color(217, 119, 6);
    private final Color PURPLE = new Color(124, 58, 237);
    private final Color TEAL = new Color(13, 148, 136);
    private final Color DARK = new Color(30, 41, 59);

    private final Font FONT_TITLE = uiFont(Font.BOLD, 18);
    private final Font FONT_HEADING = uiFont(Font.BOLD, 22);
    private final Font FONT_SECTION = uiFont(Font.BOLD, 14);
    private final Font FONT_BODY = uiFont(Font.PLAIN, 13);
    private final Font FONT_BOLD = uiFont(Font.BOLD, 13);
    private final Font FONT_SMALL = uiFont(Font.PLAIN, 11);

    private final File folder = new File("PharmacyData");
    private final File medicineFile = new File(folder, "medicines.txt");
    private final File supplierFile = new File(folder, "suppliers.txt");
    private final File salesFile = new File(folder, "sales.txt");
    private final File userFile = new File(folder, "users.txt");
    private final File billLocationFile = new File(folder, "bill_location.txt");
    private File billDirectory = new File(folder, "Bills");

    private List<Medicine> medicines = new ArrayList<>();
    private List<Supplier> suppliers = new ArrayList<>();
    private List<Sale> sales = new ArrayList<>();
    private List<UserAccount> userAccounts = new ArrayList<>();
    private UserAccount currentUser;

    private JTable medicineTable, salesMedicineTable, supplierTable;
    private JTable reportTable, alertTable;

    private DefaultTableModel medicineModel, salesMedicineModel;
    private DefaultTableModel supplierModel, reportModel, alertModel;
    private DefaultTableModel userModel;
    private JTable userTable;

    private JLabel totalMedicineLabel, totalStockLabel, lowStockLabel;
    private JLabel todaySalesLabel, totalRevenueLabel, totalProfitLabel;

    private JTabbedPane tabs;
    private JLabel signedInLabel;
    private final Map<Integer, JButton> sidebarButtons = new LinkedHashMap<>();

    private static final int PASSWORD_ITERATIONS = 120000;
    private static final int PASSWORD_BITS = 256;

    private static Font uiFont(int style, int size) {
        Font f = new Font("Segoe UI", style, size);
        if (!"Segoe UI".equalsIgnoreCase(f.getFamily())) {
            return new Font(Font.SANS_SERIF, style, size);
        }
        return f;
    }

    public PharmacyManagement(UserAccount authenticatedUser) {
        this.currentUser = authenticatedUser;
        setTitle("MediCare ERP — Pharmacy Management System");
        setSize(1280, 780);
        setMinimumSize(new Dimension(1040, 660));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadAllData();
        loadUserAccounts();

        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);

        add(createSidebar(), BorderLayout.WEST);

        JPanel mainShell = new JPanel(new BorderLayout());
        mainShell.setBackground(APP_BG);
        mainShell.add(createHeader(), BorderLayout.NORTH);
        mainShell.add(createTabs(), BorderLayout.CENTER);
        add(mainShell, BorderLayout.CENTER);

        refreshAll();
        setActiveSidebar(0);
    }

    // ================= USER ACCOUNT DATA =================

    static class UserAccount {
        String userId;
        String role;
        String salt;
        String passwordHash;

        UserAccount(String userId, String role, String salt, String passwordHash) {
            this.userId = userId;
            this.role = role;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }
    }

    private boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.role);
    }

    // ================= DATA CLASSES =================

    static class Medicine {
        int id;
        String name, company, category, expiry, supplier;
        double buyPrice, sellPrice;
        int quantity;

        Medicine(int id, String name, String company,
                 String category, double buyPrice,
                 double sellPrice, int quantity,
                 String expiry, String supplier) {
            this.id = id;
            this.name = name;
            this.company = company;
            this.category = category;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.quantity = quantity;
            this.expiry = expiry;
            this.supplier = supplier;
        }
    }

    static class Supplier {
        int id;
        String name, phone, address;

        Supplier(int id, String name, String phone, String address) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.address = address;
        }
    }

    static class Sale {
        String date, customer, phone, medicineName, invoiceId;
        int medicineId, quantity;
        double total, cost;

        Sale(String invoiceId, String date, int medicineId, String medicineName,
             int quantity, double total, double cost,
             String customer, String phone) {
            this.invoiceId = invoiceId;
            this.date = date;
            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.quantity = quantity;
            this.total = total;
            this.cost = cost;
            this.customer = customer;
            this.phone = phone;
        }
    }

    // ================= HEADER (TOPBAR) =================

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(HEADER_BG);
        panel.setBorder(new CompoundBorder(
            new javax.swing.border.MatteBorder(0, 0, 1, 0, BORDER_GRAY),
            new EmptyBorder(12, 22, 12, 22)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel badge = new JLabel("LIVE POS");
        badge.setFont(uiFont(Font.BOLD, 10));
        badge.setForeground(GREEN);
        badge.setOpaque(true);
        badge.setBackground(new Color(209, 250, 229));
        badge.setBorder(new EmptyBorder(3, 7, 3, 7));

        signedInLabel = new JLabel("Operator: " + currentUser.userId + "  •  Role: " + currentUser.role);
        signedInLabel.setForeground(TEXT_PRIMARY);
        signedInLabel.setFont(FONT_BOLD);

        left.add(badge);
        left.add(signedInLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton changeUserId = ghostButton("Change ID");
        changeUserId.addActionListener(e -> changeOwnUserId());

        JButton changePassword = ghostButton("Password");
        changePassword.addActionListener(e -> changeOwnPassword());

        JButton logout = button("Logout", RED);
        logout.setBorder(new EmptyBorder(7, 12, 7, 12));
        logout.addActionListener(e -> logout());

        right.add(changeUserId);
        right.add(changePassword);
        right.add(logout);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(215, 0));
        panel.setBackground(SIDEBAR_BG);

        // Brand block at top of sidebar
        JPanel brandBox = new JPanel(new GridLayout(2, 1, 0, 2));
        brandBox.setBackground(SIDEBAR_BG);
        brandBox.setBorder(new CompoundBorder(
            new javax.swing.border.MatteBorder(0, 0, 1, 0, new Color(30, 41, 59)),
            new EmptyBorder(18, 18, 16, 18)
        ));

        JLabel brandName = new JLabel("MediCare");
        brandName.setFont(FONT_TITLE);
        brandName.setForeground(Color.WHITE);

        JLabel brandSub = new JLabel("PHARMACY SUITE v2.0");
        brandSub.setFont(uiFont(Font.BOLD, 10));
        brandSub.setForeground(new Color(148, 163, 184));

        brandBox.add(brandName);
        brandBox.add(brandSub);

        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBackground(SIDEBAR_BG);
        navList.setBorder(new EmptyBorder(12, 10, 12, 10));

        String[] mainNav = {"Dashboard", "Medicines", "Sales / POS", "Stock & Alerts", "Suppliers", "Sales Reports"};
        for (int i = 0; i < mainNav.length; i++) {
            final int tabIdx = i;
            JButton btn = createNavButton(mainNav[i], () -> showTab(tabIdx));
            sidebarButtons.put(tabIdx, btn);
            navList.add(btn);
            navList.add(Box.createVerticalStrut(4));
        }

        if (isAdmin()) {
            JButton userBtn = createNavButton("User Accounts", () -> showTab(6));
            sidebarButtons.put(6, userBtn);
            navList.add(userBtn);
            navList.add(Box.createVerticalStrut(4));
        }

        JPanel footerBox = new JPanel(new BorderLayout());
        footerBox.setBackground(SIDEBAR_BG);
        footerBox.setBorder(new CompoundBorder(
            new javax.swing.border.MatteBorder(1, 0, 0, 0, new Color(30, 41, 59)),
            new EmptyBorder(10, 10, 14, 10)
        ));

        JButton aboutBtn = createNavButton("System Info", () ->
            JOptionPane.showMessageDialog(this,
                "MediCare Pharmacy Management System\n" +
                "Architecture: Java Swing Desktop Suite\n" +
                "Signed in as: " + currentUser.userId + " (" + currentUser.role + ")\n" +
                "Storage Engine: Local Encrypted/Encoded Files",
                "System Information", JOptionPane.INFORMATION_MESSAGE)
        );
        footerBox.add(aboutBtn, BorderLayout.CENTER);

        panel.add(brandBox, BorderLayout.NORTH);
        panel.add(navList, BorderLayout.CENTER);
        panel.add(footerBox, BorderLayout.SOUTH);
        return panel;
    }

    private JButton createNavButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(190, 40));
        btn.setFont(FONT_BOLD);
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground(SIDEBAR_HOVER);
                    btn.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground(SIDEBAR_BG);
                    btn.setForeground(new Color(203, 213, 225));
                }
            }
        });

        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void setActiveSidebar(int activeTabIndex) {
        for (Map.Entry<Integer, JButton> entry : sidebarButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean active = entry.getKey() == activeTabIndex;
            btn.putClientProperty("active", active);
            if (active) {
                btn.setBackground(SIDEBAR_ACTIVE);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(SIDEBAR_BG);
                btn.setForeground(new Color(203, 213, 225));
            }
        }
    }

    private JTabbedPane createTabs() {
        tabs = new JTabbedPane();
        tabs.setBackground(APP_BG);
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override protected int calculateTabAreaHeight(int placement, int runs, int maxHeight) { return 0; }
            @Override protected int calculateTabAreaWidth(int placement, int runs, int maxWidth) { return 0; }
        });

        tabs.addTab("Dashboard", createDashboard());
        tabs.addTab("Medicines", createMedicinePanel());
        tabs.addTab("Sales / POS", createSalesPanel());
        tabs.addTab("Stock & Alerts", createAlertsPanel());
        tabs.addTab("Suppliers", createSupplierPanel());
        tabs.addTab("Sales Reports", createReportsPanel());
        if (isAdmin()) tabs.addTab("User Accounts", createUserManagementPanel());

        return tabs;
    }

    private void showTab(int index) {
        if (index >= 0 && index < tabs.getTabCount()) {
            tabs.setSelectedIndex(index);
            setActiveSidebar(index);
        }
    }

    // ================= USER ACCOUNT MANAGEMENT =================

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(APP_BG);
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel heading = new JLabel("User Account Management");
        heading.setFont(FONT_HEADING);
        heading.setForeground(TEXT_PRIMARY);

        userModel = model(new String[]{"User ID", "Role"});
        userTable = new JTable(userModel);
        styleTable(userTable);

        JButton addEmployee = button("Add Employee", GREEN);
        JButton addAdmin = button("Add Admin", PURPLE);
        JButton edit = button("Edit ID / Reset Password", BLUE);
        JButton delete = button("Delete Account", RED);
        JButton refresh = ghostButton("Refresh");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actions.setBackground(CARD_BG);
        actions.setBorder(new LineBorder(BORDER_GRAY, 1));
        actions.add(addEmployee);
        actions.add(addAdmin);
        actions.add(edit);
        actions.add(delete);
        actions.add(refresh);

        addEmployee.addActionListener(e -> addUserAccount("EMPLOYEE"));
        addAdmin.addActionListener(e -> addUserAccount("ADMIN"));
        edit.addActionListener(e -> editManagedAccount());
        delete.addActionListener(e -> deleteManagedAccount());
        refresh.addActionListener(e -> refreshUserTable());

        JTextArea note = new JTextArea(
            "Security & Access Policy: Administrators can provision Employee and Admin accounts. " +
            "New accounts start with a default password unless customized. Passwords are stored using salted PBKDF2 hashing."
        );
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setBackground(CARD_BG);
        note.setForeground(TEXT_MUTED);
        note.setFont(FONT_SMALL);
        note.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY, 1), new EmptyBorder(10, 14, 10, 14)));

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.setOpaque(false);
        south.add(actions, BorderLayout.NORTH);
        south.add(note, BorderLayout.SOUTH);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(wrapTable(userTable), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        refreshUserTable();
        return panel;
    }

    private void refreshUserTable() {
        if (userModel == null) return;
        userModel.setRowCount(0);
        for (UserAccount account : userAccounts) {
            userModel.addRow(new Object[]{account.userId, account.role});
        }
    }

    private UserAccount selectedManagedAccount() {
        int row = userTable == null ? -1 : userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an account first.");
            return null;
        }
        String userId = userTable.getValueAt(row, 0).toString();
        for (UserAccount account : userAccounts) {
            if (account.userId.equals(userId)) return account;
        }
        return null;
    }

    private void addUserAccount(String role) {
        JTextField userId = new JTextField();
        JPasswordField defaultPassword = new JPasswordField("welcome123");
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(form, role.equals("ADMIN") ? "Admin User ID:" : "Employee User ID:", userId);
        addField(form, "Default Password:", defaultPassword);
        int result = JOptionPane.showConfirmDialog(this, form,
            role.equals("ADMIN") ? "Create Admin Account" : "Create Employee Account",
            JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String id = userId.getText().trim();
        char[] password = defaultPassword.getPassword();
        try {
            validateNewUserId(id, null);
            validatePassword(password);
            userAccounts.add(createUserAccount(id, role, password));
            saveUserAccounts();
            refreshUserTable();
            JOptionPane.showMessageDialog(this,
                role + " account created successfully.\nUser ID: " + id +
                "\nDefault password: " + new String(password) +
                "\nThe user should change this password after login.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not create account: " + ex.getMessage(),
                "Account Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void editManagedAccount() {
        UserAccount account = selectedManagedAccount();
        if (account == null) return;

        JTextField userId = new JTextField(account.userId);
        JPasswordField resetPassword = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(form, "New User ID:", userId);
        addField(form, "New Password (blank = unchanged):", resetPassword);
        int result = JOptionPane.showConfirmDialog(this, form,
            "Edit " + account.role + " Account", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        char[] newPassword = resetPassword.getPassword();
        try {
            String updatedId = userId.getText().trim();
            validateNewUserId(updatedId, account);
            if (newPassword.length > 0) validatePassword(newPassword);
            account.userId = updatedId;
            if (newPassword.length > 0) applyNewPassword(account, newPassword);
            saveUserAccounts();
            if (account == currentUser) updateSignedInLabel();
            refreshUserTable();
            JOptionPane.showMessageDialog(this, "Account updated successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update account: " + ex.getMessage(),
                "Account Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(newPassword, '\0');
        }
    }

    private void deleteManagedAccount() {
        UserAccount account = selectedManagedAccount();
        if (account == null) return;
        if (account == currentUser || (account.userId.equalsIgnoreCase(currentUser.userId)
                && account.role.equalsIgnoreCase(currentUser.role))) {
            JOptionPane.showMessageDialog(this,
                "You cannot delete the account currently in use. Use another administrator account.");
            return;
        }
        if ("ADMIN".equalsIgnoreCase(account.role)) {
            int adminCount = 0;
            for (UserAccount item : userAccounts) if ("ADMIN".equalsIgnoreCase(item.role)) adminCount++;
            if (adminCount <= 1) {
                JOptionPane.showMessageDialog(this, "At least one administrator account must remain.");
                return;
            }
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + account.role + " account '" + account.userId + "'?",
            "Confirm Account Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            userAccounts.remove(account);
            saveUserAccounts();
            refreshUserTable();
        }
    }

    private void updateSignedInLabel() {
        if (signedInLabel != null) {
            signedInLabel.setText("Operator: " + currentUser.userId + "  •  Role: " + currentUser.role);
        }
    }

    private void changeOwnUserId() {
        JTextField newUserId = new JTextField(currentUser.userId);
        JPasswordField currentPassword = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(form, "New User ID:", newUserId);
        addField(form, "Current Password:", currentPassword);
        int result = JOptionPane.showConfirmDialog(this, form,
            "Change User ID", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        char[] password = currentPassword.getPassword();
        try {
            if (!passwordMatches(currentUser, password)) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            String updatedId = newUserId.getText().trim();
            validateNewUserId(updatedId, currentUser);
            currentUser.userId = updatedId;
            saveUserAccounts();
            updateSignedInLabel();
            refreshUserTable();
            JOptionPane.showMessageDialog(this, "Your User ID has been changed successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not change User ID: " + ex.getMessage(),
                "Account Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void logout() {
        if (!cart.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Your cart contains unsold items. Clear the cart and logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
            cart.clear();
        } else {
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        dispose();
        SwingUtilities.invokeLater(() -> launchLoginFlow());
    }

    private void changeOwnPassword() {
        JPasswordField currentPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(form, "Current Password:", currentPassword);
        addField(form, "New Password:", newPassword);
        addField(form, "Confirm New Password:", confirmPassword);
        int result = JOptionPane.showConfirmDialog(this, form,
            "Change Password - " + currentUser.userId, JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        char[] current = currentPassword.getPassword();
        char[] replacement = newPassword.getPassword();
        char[] confirmation = confirmPassword.getPassword();
        try {
            if (!passwordMatches(currentUser, current)) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            validatePassword(replacement);
            if (!Arrays.equals(replacement, confirmation)) {
                throw new IllegalArgumentException("New passwords do not match.");
            }
            applyNewPassword(currentUser, replacement);
            saveUserAccounts();
            JOptionPane.showMessageDialog(this, "Your password has been changed successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not change password: " + ex.getMessage(),
                "Password Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(current, '\0');
            Arrays.fill(replacement, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private void validateNewUserId(String id, UserAccount current) {
        if (id == null || id.trim().isEmpty() || id.contains("|")) {
            throw new IllegalArgumentException("User ID cannot be empty or contain '|'.");
        }
        for (UserAccount account : userAccounts) {
            if (account != current && account.userId.equalsIgnoreCase(id)) {
                throw new IllegalArgumentException("This User ID is already in use.");
            }
        }
    }

    private void validatePassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
    }

    // ================= DASHBOARD =================

    private JPanel createDashboard() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(APP_BG);
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Top section header
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel heading = new JLabel("Operations Overview");
        heading.setFont(FONT_HEADING);
        heading.setForeground(TEXT_PRIMARY);

        JLabel subHeading = new JLabel("Click any metric card for deep operational insights");
        subHeading.setFont(FONT_BODY);
        subHeading.setForeground(TEXT_MUTED);

        titleRow.add(heading, BorderLayout.WEST);
        titleRow.add(subHeading, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(2, 3, 14, 14));
        cards.setOpaque(false);

        totalMedicineLabel = new JLabel("0");
        totalStockLabel = new JLabel("0");
        lowStockLabel = new JLabel("0");
        todaySalesLabel = new JLabel("৳0.00");
        totalRevenueLabel = new JLabel("৳0.00");
        totalProfitLabel = new JLabel("৳0.00");

        cards.add(statCard("CATEGORY & DEMAND", totalMedicineLabel, BLUE, "Category demand & restock view"));
        cards.add(statCard("CAPITAL & DEAD STOCK", totalStockLabel, TEAL, "Blocked capital & slow/dead stock"));
        cards.add(statCard("LOW STOCK ITEMS", lowStockLabel, RED, "Stock <= 10 units alert"));
        cards.add(statCard("TODAY'S SALES", todaySalesLabel, PURPLE, "Daily POS transaction total"));
        cards.add(statCard("TOTAL REVENUE", totalRevenueLabel, AMBER, "Gross sales revenue"));
        cards.add(statCard("ESTIMATED PROFIT", totalProfitLabel, GREEN, "Gross margin over cost"));

        int detailIndex = 0;
        for (Component component : cards.getComponents()) {
            final int index = detailIndex++;
            if (component instanceof JButton) {
                JButton cardButton = (JButton) component;
                cardButton.addActionListener(e -> showDashboardDetail(index));
            }
        }

        // Bottom operational guidelines card
        JPanel guideCard = new JPanel(new BorderLayout(8, 8));
        guideCard.setBackground(CARD_BG);
        guideCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER_GRAY, 1),
            new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel guideTitle = new JLabel("Standard Operating Checklist");
        guideTitle.setFont(FONT_SECTION);
        guideTitle.setForeground(TEXT_PRIMARY);

        JTextArea note = new JTextArea(
            "• Inventory & CSV Sync: Import bulk medicine catalogs via CSV and verify buy/sell margins and batch expiry dates.\n" +
            "• Point of Sale (POS): Select single or multiple medicines, confirm stock availability, and generate automated PDF invoices.\n" +
            "• Expiry & Stock Governance: Monitor expiring batches (30-day window) and low-stock items to prevent stockouts and wastage."
        );
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(FONT_BODY);
        note.setForeground(TEXT_MUTED);
        note.setBackground(CARD_BG);

        guideCard.add(guideTitle, BorderLayout.NORTH);
        guideCard.add(note, BorderLayout.CENTER);

        JPanel topBlock = new JPanel(new BorderLayout(0, 14));
        topBlock.setOpaque(false);
        topBlock.add(titleRow, BorderLayout.NORTH);
        topBlock.add(cards, BorderLayout.CENTER);

        panel.add(topBlock, BorderLayout.NORTH);
        panel.add(guideCard, BorderLayout.CENTER);

        return panel;
    }

    private JButton statCard(String title, JLabel value, Color accent, String hint) {
        JButton card = new JButton();
        card.setLayout(new BorderLayout(4, 6));
        card.setBackground(CARD_BG);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setFocusPainted(false);
        card.setBorder(new CompoundBorder(
            new CompoundBorder(
                new LineBorder(BORDER_GRAY, 1),
                new javax.swing.border.MatteBorder(0, 4, 0, 0, accent)
            ),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT_MUTED);
        heading.setFont(uiFont(Font.BOLD, 11));

        value.setForeground(TEXT_PRIMARY);
        value.setFont(uiFont(Font.BOLD, 24));

        JLabel sub = new JLabel(hint);
        sub.setForeground(accent);
        sub.setFont(FONT_SMALL);

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(sub, BorderLayout.SOUTH);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(248, 250, 252));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(CARD_BG);
            }
        });

        return card;
    }

    private void showDashboardDetail(int index) {
        String[] columns;
        if (index == 0) {
            columns = new String[]{"Category", "Total Brands", "Stock Units", "Units Sold", "Demand Status"};
        } else if (index == 1) {
            columns = new String[]{"ID", "Medicine (Company)", "Stock Qty", "Blocked Capital", "Movement Status"};
        } else {
            columns = new String[]{"Date / ID", "Medicine / Details", "Quantity", "Customer", "Amount"};
        }

        DefaultTableModel dm = model(columns);

        if (index == 0) {
            Map<String, Integer> brandCount = new LinkedHashMap<>();
            Map<String, Integer> stockCount = new LinkedHashMap<>();
            Map<String, Integer> soldCount = new LinkedHashMap<>();

            for (Medicine m : medicines) {
                brandCount.put(m.category, brandCount.getOrDefault(m.category, 0) + 1);
                stockCount.put(m.category, stockCount.getOrDefault(m.category, 0) + m.quantity);
            }
            for (Sale s : sales) {
                Medicine m = findMedicine(s.medicineId);
                String cat = (m != null) ? m.category : "Other";
                soldCount.put(cat, soldCount.getOrDefault(cat, 0) + s.quantity);
            }
            for (String cat : brandCount.keySet()) {
                int stk = stockCount.getOrDefault(cat, 0);
                int sold = soldCount.getOrDefault(cat, 0);
                String status = (stk < sold) ? "High Demand (Restock Soon)" : (sold > 0 ? "Active" : "No Sales Yet");
                dm.addRow(new Object[]{cat, brandCount.get(cat), stk, sold, status});
            }
        } else if (index == 1) {
            Map<Integer, Integer> medSold = new LinkedHashMap<>();
            for (Sale s : sales) {
                medSold.put(s.medicineId, medSold.getOrDefault(s.medicineId, 0) + s.quantity);
            }
            for (Medicine m : medicines) {
                int sold = medSold.getOrDefault(m.id, 0);
                double blockedCapital = m.quantity * m.buyPrice;
                String movement = (sold == 0) ? "Dead Stock (0 Sold)" : (sold >= 20 ? "Fast Moving" : "Normal");
                dm.addRow(new Object[]{
                    m.id,
                    m.name + " (" + m.company + ")",
                    m.quantity,
                    "৳" + money(blockedCapital),
                    movement
                });
            }
        } else if (index == 2) {
            for (Medicine m : medicines) {
                if (m.quantity <= 10) {
                    dm.addRow(new Object[]{m.id, m.name, m.quantity, m.company, "Low stock"});
                }
            }
        } else if (index == 3) {
            String today = LocalDate.now().toString();
            for (Sale sale : sales) {
                if (sale.date.equals(today)) {
                    dm.addRow(new Object[]{sale.date, sale.medicineName, sale.quantity, sale.customer, "৳" + money(sale.total)});
                }
            }
        } else if (index == 4 || index == 5) {
            for (Sale sale : sales) {
                dm.addRow(new Object[]{sale.date, sale.medicineName, sale.quantity, sale.customer,
                    "৳" + money(index == 4 ? sale.total : sale.total - sale.cost)});
            }
        }

        JTable detail = new JTable(dm);
        styleTable(detail);
        JScrollPane scroll = new JScrollPane(detail);
        scroll.setPreferredSize(new Dimension(760, 400));

        String[] titles = {
            "Category & Demand Insights",
            "Stock Capital & Dead-Stock Analyzer",
            "Low Stock Items",
            "Today's Sales",
            "Total Revenue",
            "Estimated Profit"
        };
        JOptionPane.showMessageDialog(this, scroll, titles[index], JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= MEDICINE PANEL =================

    private JPanel createMedicinePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(APP_BG);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(CARD_BG);
        top.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY, 1), new EmptyBorder(10, 14, 10, 14)));

        JPanel searchLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchLeft.setOpaque(false);

        JTextField search = new JTextField(24);
        search.setFont(FONT_BODY);
        JButton searchButton = button("Search", BLUE);
        JButton reset = ghostButton("Reset");

        JLabel lblSearch = new JLabel("Filter Catalog:");
        lblSearch.setFont(FONT_BOLD);
        lblSearch.setForeground(TEXT_PRIMARY);

        searchLeft.add(lblSearch);
        searchLeft.add(search);
        searchLeft.add(searchButton);
        searchLeft.add(reset);

        JLabel panelTitle = new JLabel("Medicine Inventory");
        panelTitle.setFont(FONT_SECTION);
        panelTitle.setForeground(TEXT_PRIMARY);

        top.add(panelTitle, BorderLayout.WEST);
        top.add(searchLeft, BorderLayout.EAST);

        medicineModel = model(new String[]{
            "ID", "Medicine", "Company", "Category",
            "Buy Price", "Sell Price", "Stock",
            "Expiry Date", "Supplier"
        });

        medicineTable = new JTable(medicineModel);
        styleTable(medicineTable);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actions.setBackground(CARD_BG);
        actions.setBorder(new LineBorder(BORDER_GRAY, 1));

        JButton add = button("Add Medicine", GREEN);
        JButton edit = button("Edit Medicine", BLUE);
        JButton delete = button("Delete", RED);
        JButton restock = button("Restock", PURPLE);
        JButton importCsv = button("Import CSV", TEAL);
        JButton exportCsv = ghostButton("Export CSV");

        actions.add(add);
        actions.add(edit);
        actions.add(restock);
        actions.add(delete);
        actions.add(importCsv);
        actions.add(exportCsv);

        searchButton.addActionListener(e -> {
            String key = search.getText().trim().toLowerCase();
            medicineModel.setRowCount(0);

            for (Medicine m : medicines) {
                if (m.name.toLowerCase().contains(key)
                    || m.company.toLowerCase().contains(key)
                    || m.category.toLowerCase().contains(key)) {
                    medicineModel.addRow(medicineRow(m));
                }
            }
        });

        reset.addActionListener(e -> {
            search.setText("");
            refreshMedicineTable();
        });

        add.addActionListener(e -> addMedicine());
        edit.addActionListener(e -> editMedicine());
        delete.addActionListener(e -> deleteMedicine());
        restock.addActionListener(e -> restockMedicine());
        importCsv.addActionListener(e -> importMedicinesFromCSV());
        exportCsv.addActionListener(e -> exportMedicinesToCSV());

        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapTable(medicineTable), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    // ================= CSV IMPORT / EXPORT =================

    private void exportMedicinesToCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Medicines to CSV");
        chooser.setSelectedFile(new File("medicines_export.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.println("ID,Medicine,Company,Category,Buy Price,Sell Price,Stock,Expiry Date,Supplier");
            for (Medicine m : medicines) {
                out.println(csvRow(new String[]{String.valueOf(m.id), m.name, m.company,
                    m.category, String.valueOf(m.buyPrice), String.valueOf(m.sellPrice),
                    String.valueOf(m.quantity), m.expiry, m.supplier}));
            }
            JOptionPane.showMessageDialog(this, "Medicine data exported successfully.\nFile: "
                + file.getAbsolutePath() + "\nExported records: " + medicines.size());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "CSV export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importMedicinesFromCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Medicine CSV File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "CSV Files (*.csv)", "csv"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        int added = 0;
        int duplicates = 0;
        int invalid = 0;
        int rowNumber = 0;
        List<String> errors = new ArrayList<>();
        File selectedFile = chooser.getSelectedFile();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(selectedFile), StandardCharsets.UTF_8))) {
            String line;
            boolean firstNonEmptyRow = true;

            while ((line = br.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                List<String> columns;
                try {
                    columns = parseCSVLine(line);
                } catch (Exception parseError) {
                    invalid++;
                    if (errors.size() < 10) {
                        errors.add("Row " + rowNumber + ": " + parseError.getMessage());
                    }
                    continue;
                }

                if (columns.isEmpty()) continue;

                String first = columns.get(0).replace("\uFEFF", "")
                    .trim().toLowerCase(Locale.ROOT);
                String header = String.join(",", columns).toLowerCase(Locale.ROOT);

                if (firstNonEmptyRow) {
                    firstNonEmptyRow = false;
                    if (first.equals("id") || first.equals("medicine id")
                            || first.equals("medicine") || first.equals("medicine name")
                            || first.equals("name") || header.contains("buy price")
                            || header.contains("purchase price") || header.contains("expiry date")) {
                        continue;
                    }
                }

                try {
                    int offset;
                    if (columns.size() == 9) {
                        offset = 1;
                    } else if (columns.size() == 8) {
                        offset = 0;
                    } else {
                        throw new IllegalArgumentException(
                            "Expected 8 columns without ID or 9 columns with ID; found "
                            + columns.size() + ".");
                    }

                    String name = columns.get(offset).trim();
                    String company = columns.get(offset + 1).trim();
                    String category = columns.get(offset + 2).trim();
                    double buyPrice = parseCsvDouble(columns.get(offset + 3));
                    double sellPrice = parseCsvDouble(columns.get(offset + 4));
                    int quantity = Integer.parseInt(columns.get(offset + 5).trim()
                        .replace(",", ""));
                    String expiry = parseExpiryDate(columns.get(offset + 6).trim()).toString();
                    String supplier = columns.get(offset + 7).trim();

                    if (name.isEmpty() || company.isEmpty() || category.isEmpty()
                            || supplier.isEmpty() || !Double.isFinite(buyPrice)
                            || !Double.isFinite(sellPrice) || buyPrice < 0
                            || sellPrice <= 0 || sellPrice < buyPrice || quantity < 0) {
                        throw new IllegalArgumentException(
                            "Missing information or invalid price/stock value.");
                    }

                    boolean duplicate = false;
                    for (Medicine existing : medicines) {
                        if (existing.name.equalsIgnoreCase(name)
                                && existing.company.equalsIgnoreCase(company)) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (duplicate) {
                        duplicates++;
                        continue;
                    }

                    medicines.add(new Medicine(getNextMedicineId(), name, company,
                        category, buyPrice, sellPrice, quantity, expiry, supplier));
                    added++;

                } catch (Exception rowError) {
                    invalid++;
                    if (errors.size() < 10) {
                        errors.add("Row " + rowNumber + ": " + rowError.getMessage());
                    }
                }
            }

            if (added > 0) {
                saveMedicines();
                refreshAll();
            }

            StringBuilder report = new StringBuilder("CSV Import Completed!\n\n")
                .append("Successfully added: ").append(added).append('\n')
                .append("Skipped duplicates: ").append(duplicates).append('\n')
                .append("Invalid rows: ").append(invalid);

            if (!errors.isEmpty()) {
                report.append("\n\nFirst errors:\n");
                for (String error : errors) report.append("- ").append(error).append('\n');
                if (invalid > errors.size()) {
                    report.append("...and ").append(invalid - errors.size())
                        .append(" more invalid row(s).");
                }
            }

            JOptionPane.showMessageDialog(this, report.toString(),
                "CSV Import Report", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "CSV import failed: " + ex.getMessage(),
                "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseCsvDouble(String value) {
        String cleaned = value.trim().replace(",", "").replace("৳", "");
        return Double.parseDouble(cleaned);
    }

    private String csvRow(String[] values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append(',');
            String value = values[i] == null ? "" : values[i];
            row.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        return row.toString();
    }

    private List<String> parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                values.add(value.toString());
                value.setLength(0);
            } else value.append(ch);
        }
        if (inQuotes) throw new IllegalArgumentException("Unclosed quote in CSV row");
        values.add(value.toString());
        return values;
    }

    // ================= ADD MEDICINE =================

    private void addMedicine() {
        JTextField name = new JTextField();
        JTextField company = new JTextField();

        JComboBox<String> category = new JComboBox<>(new String[]{
            "Tablet", "Capsule", "Syrup", "Injection",
            "Cream", "Drops", "Inhaler", "Other"
        });

        JTextField buy = new JTextField();
        JTextField sell = new JTextField();
        JTextField quantity = new JTextField();
        JTextField expiry = new JTextField("2027-12-31");
        JTextField supplier = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        addField(form, "Medicine Name:", name);
        addField(form, "Company:", company);
        addField(form, "Category:", category);
        addField(form, "Purchase Price:", buy);
        addField(form, "Selling Price:", sell);
        addField(form, "Quantity:", quantity);
        addField(form, "Expiry (YYYY-MM-DD):", expiry);
        addField(form, "Supplier Name:", supplier);

        int result = JOptionPane.showConfirmDialog(
            this, form, "Add New Medicine",
            JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String n = name.getText().trim();
            String c = company.getText().trim();
            String ex = expiry.getText().trim();

            double bp = Double.parseDouble(buy.getText().trim());
            double sp = Double.parseDouble(sell.getText().trim());
            int qty = Integer.parseInt(quantity.getText().trim());

            ex = parseExpiryDate(ex).toString();

            if (n.isEmpty() || c.isEmpty()
                || supplier.getText().trim().isEmpty()
                || bp < 0 || sp <= 0 || qty < 0 || sp < bp) {
                throw new IllegalArgumentException(
                    "Check the medicine information and prices.");
            }

            Medicine m = new Medicine(
                getNextMedicineId(), n, c,
                category.getSelectedItem().toString(),
                bp, sp, qty, ex, supplier.getText().trim()
            );

            medicines.add(m);
            saveMedicines();
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Medicine added successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Invalid information!\n" +
                "Use numbers for prices and quantity.\n" +
                "Expiry format: YYYY-MM-DD\n\n" + ex.getMessage());
        }
    }

    // ================= EDIT MEDICINE =================

    private void editMedicine() {
        Medicine m = selectedMedicine(medicineTable);
        if (m == null) return;

        JTextField name = new JTextField(m.name);
        JTextField company = new JTextField(m.company);
        JTextField category = new JTextField(m.category);
        JTextField buy = new JTextField(String.valueOf(m.buyPrice));
        JTextField sell = new JTextField(String.valueOf(m.sellPrice));
        JTextField quantity = new JTextField(String.valueOf(m.quantity));
        JTextField expiry = new JTextField(m.expiry);
        JTextField supplier = new JTextField(m.supplier);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        addField(form, "Medicine Name:", name);
        addField(form, "Company:", company);
        addField(form, "Category:", category);
        addField(form, "Purchase Price:", buy);
        addField(form, "Selling Price:", sell);
        addField(form, "Quantity:", quantity);
        addField(form, "Expiry Date:", expiry);
        addField(form, "Supplier:", supplier);

        int result = JOptionPane.showConfirmDialog(
            this, form, "Edit Medicine",
            JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            double bp = Double.parseDouble(buy.getText().trim());
            double sp = Double.parseDouble(sell.getText().trim());
            int qty = Integer.parseInt(quantity.getText().trim());

            String normalizedExpiry = parseExpiryDate(expiry.getText().trim()).toString();

            if (name.getText().trim().isEmpty()
                || company.getText().trim().isEmpty()
                || supplier.getText().trim().isEmpty()
                || bp < 0 || sp <= 0 || sp < bp || qty < 0) {
                throw new IllegalArgumentException("Invalid values.");
            }

            m.name = name.getText().trim();
            m.company = company.getText().trim();
            m.category = category.getText().trim();
            m.buyPrice = bp;
            m.sellPrice = sp;
            m.quantity = qty;
            m.expiry = normalizedExpiry;
            m.supplier = supplier.getText().trim();

            saveMedicines();
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Medicine updated successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Invalid information: " + ex.getMessage());
        }
    }

    // ================= DELETE MEDICINE =================

    private void deleteMedicine() {
        Medicine m = selectedMedicine(medicineTable);
        if (m == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this, "Delete " + m.name + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            medicines.remove(m);
            saveMedicines();
            refreshAll();
        }
    }

    // ================= RESTOCK MEDICINE =================

    private void restockMedicine() {
        Medicine m = selectedMedicine(medicineTable);
        if (m == null) return;

        String input = JOptionPane.showInputDialog(
            this, "Enter additional stock quantity for " + m.name);

        if (input == null) return;

        try {
            int qty = Integer.parseInt(input.trim());

            if (qty <= 0 || m.quantity > Integer.MAX_VALUE - qty) {
                throw new NumberFormatException();
            }

            m.quantity += qty;
            saveMedicines();
            refreshAll();

            JOptionPane.showMessageDialog(this,
                "Stock updated successfully!\nNew Stock: " + m.quantity);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Enter a valid positive quantity.");
        }
    }

    // ================= SALES / POS =================

    private JTextField posSearchField;
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private final Map<Integer, Integer> cart = new LinkedHashMap<>();

    private JPanel createSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(APP_BG);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel heading = new JLabel("Point of Sale (POS) Terminal");
        heading.setFont(FONT_HEADING);
        heading.setForeground(TEXT_PRIMARY);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setBackground(CARD_BG);
        top.setBorder(new LineBorder(BORDER_GRAY, 1));

        posSearchField = new JTextField(22);
        posSearchField.setFont(FONT_BODY);
        JButton search = button("Search", BLUE);
        JButton reset = ghostButton("Reset");
        JButton addToCart = button("Add to Cart", GREEN);
        JButton billFolder = ghostButton("Bill Folder");

        JLabel lblPosSearch = new JLabel("Medicine:");
        lblPosSearch.setFont(FONT_BOLD);
        lblPosSearch.setForeground(TEXT_PRIMARY);

        top.add(lblPosSearch);
        top.add(posSearchField);
        top.add(search);
        top.add(reset);
        top.add(addToCart);
        top.add(billFolder);

        salesMedicineModel = model(new String[]{"ID", "Medicine", "Company", "Price", "Available Stock"});
        salesMedicineTable = new JTable(salesMedicineModel);
        salesMedicineTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        styleTable(salesMedicineTable);

        cartModel = model(new String[]{"ID", "Medicine", "Unit Price", "Quantity", "Line Total"});
        cartTable = new JTable(cartModel);
        styleTable(cartTable);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setOpaque(false);
        JLabel leftHdr = new JLabel("Available Stock (Ctrl/Shift to select multiple)");
        leftHdr.setFont(FONT_BOLD);
        leftHdr.setForeground(TEXT_MUTED);
        left.add(leftHdr, BorderLayout.NORTH);
        left.add(wrapTable(salesMedicineTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setOpaque(false);
        JLabel rightHdr = new JLabel("Active Cart");
        rightHdr.setFont(FONT_BOLD);
        rightHdr.setForeground(TEXT_MUTED);
        right.add(rightHdr, BorderLayout.NORTH);
        right.add(wrapTable(cartTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setBackground(APP_BG);

        JButton editQty = ghostButton("Edit Qty");
        JButton remove = button("Remove", RED);
        JButton clear = ghostButton("Clear Cart");
        JButton checkout = button("Checkout & Print PDF", GREEN);
        JButton refresh = ghostButton("Refresh");

        JLabel totalLabel = new JLabel("Cart Total: ৳0.00");
        totalLabel.setFont(uiFont(Font.BOLD, 16));
        totalLabel.setForeground(TEXT_PRIMARY);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(CARD_BG);
        bottom.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY, 1), new EmptyBorder(8, 14, 8, 14)));

        JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomLeft.setOpaque(false);
        bottomLeft.add(editQty);
        bottomLeft.add(remove);
        bottomLeft.add(clear);
        bottomLeft.add(refresh);

        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomRight.setOpaque(false);
        bottomRight.add(totalLabel);
        bottomRight.add(checkout);

        bottom.add(bottomLeft, BorderLayout.WEST);
        bottom.add(bottomRight, BorderLayout.EAST);

        Runnable updateCart = () -> {
            cartModel.setRowCount(0);
            double total = 0;
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                Medicine m = findMedicine(entry.getKey());
                if (m == null) continue;
                double line = m.sellPrice * entry.getValue();
                total += line;
                cartModel.addRow(new Object[]{m.id, m.name, "৳" + money(m.sellPrice), entry.getValue(), "৳" + money(line)});
            }
            totalLabel.setText("Cart Total: ৳" + money(total));
        };

        search.addActionListener(e -> filterSalesMedicines());
        posSearchField.addActionListener(e -> filterSalesMedicines());
        reset.addActionListener(e -> { posSearchField.setText(""); refreshSalesTable(); });
        billFolder.addActionListener(e -> chooseBillDirectory());
        addToCart.addActionListener(e -> {
            int[] rows = salesMedicineTable.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more medicines first.");
                return;
            }
            for (int row : rows) {
                int id = Integer.parseInt(salesMedicineTable.getValueAt(row, 0).toString());
                Medicine m = findMedicine(id);
                if (m == null || m.quantity <= 0 || isExpired(m)) {
                    JOptionPane.showMessageDialog(this, "Unavailable, out-of-stock or expired medicine skipped: " + (m == null ? id : m.name));
                    continue;
                }
                String input = JOptionPane.showInputDialog(this, "Enter quantity for " + m.name + " (Available: " + m.quantity + ")", String.valueOf(cart.getOrDefault(id, 1)));
                if (input == null) continue;
                try {
                    int qty = Integer.parseInt(input.trim());
                    if (qty <= 0 || qty > m.quantity) throw new IllegalArgumentException("Quantity must be 1 to " + m.quantity);
                    cart.put(id, qty);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity for " + m.name + ": " + ex.getMessage());
                }
            }
            updateCart.run();
        });
        editQty.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a cart item first.");
                return;
            }
            int id = Integer.parseInt(cartTable.getValueAt(row, 0).toString());
            Medicine m = findMedicine(id);
            if (m == null) return;
            String input = JOptionPane.showInputDialog(this, "Set quantity for " + m.name + " (Available: " + m.quantity + ")", String.valueOf(cart.get(id)));
            if (input == null) return;
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0 || qty > m.quantity) throw new IllegalArgumentException("Quantity must be 1 to " + m.quantity);
                cart.put(id, qty);
                updateCart.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity: " + ex.getMessage());
            }
        });
        remove.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a cart item to remove.");
                return;
            }
            cart.remove(Integer.parseInt(cartTable.getValueAt(row, 0).toString()));
            updateCart.run();
        });
        clear.addActionListener(e -> { cart.clear(); updateCart.run(); });
        checkout.addActionListener(e -> checkoutCart());
        refresh.addActionListener(e -> { refreshSalesTable(); filterSalesMedicines(); });

        panel.add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private Medicine findMedicine(int id) {
        for (Medicine m : medicines) if (m.id == id) return m;
        return null;
    }

    private void filterSalesMedicines() {
        if (salesMedicineModel == null) return;
        String key = posSearchField.getText().trim().toLowerCase(Locale.ROOT);
        salesMedicineModel.setRowCount(0);
        for (Medicine m : medicines) {
            if (m.name.toLowerCase(Locale.ROOT).contains(key)
                || m.company.toLowerCase(Locale.ROOT).contains(key)
                || m.category.toLowerCase(Locale.ROOT).contains(key)) {
                salesMedicineModel.addRow(new Object[]{m.id, m.name, m.company, "৳" + money(m.sellPrice), m.quantity});
            }
        }
    }

    private void checkoutCart() {
        if (cart.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }
        JTextField customer = new JTextField("Walk-in Customer");
        JTextField phone = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(form, "Customer Name:", customer);
        addField(form, "Phone:", phone);
        if (JOptionPane.showConfirmDialog(this, form, "Checkout Details", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (customer.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Customer name is required."); return; }
        double total = 0;
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Medicine m = findMedicine(e.getKey());
            if (m == null || e.getValue() <= 0 || e.getValue() > m.quantity || isExpired(m)) {
                JOptionPane.showMessageDialog(this, "Cart contains unavailable, expired or insufficient-stock medicine: " + (m == null ? e.getKey() : m.name));
                return;
            }
            total += m.sellPrice * e.getValue();
        }
        if (JOptionPane.showConfirmDialog(this, "Different medicines: " + cart.size() + "\nTotal: BDT " + money(total) + "\nBill folder: " + billDirectory.getAbsolutePath() + "\nConfirm sale?", "Confirm Checkout", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String invoice = "INV-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new java.util.Date());
        String date = LocalDate.now().toString();
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Medicine m = findMedicine(e.getKey());
            int qty = e.getValue();
            m.quantity -= qty;
            sales.add(new Sale(invoice, date, m.id, m.na