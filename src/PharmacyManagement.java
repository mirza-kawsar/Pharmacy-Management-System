package project;

import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Arrays;

public class PharmacyManagement extends JFrame {

    private final Color DARK = new Color(28, 43, 70);
    private final Color BLUE = new Color(48, 105, 190);
    private final Color GREEN = new Color(28, 150, 110);
    private final Color RED = new Color(200, 65, 65);
    private final Color BG = new Color(240, 244, 250);

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
    private JLabel todaySalesLabel, totalRevenueLabel, totalProfitLabel, expiredMedicineLabel;

    private JTabbedPane tabs;
    private JLabel signedInLabel;

    private static final int PASSWORD_ITERATIONS = 120000;
    private static final int PASSWORD_BITS = 256;

    public PharmacyManagement(UserAccount authenticatedUser) {
        this.currentUser = authenticatedUser;
        setTitle("MediCare - Pharmacy Management System");
        setSize(1250, 760);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadAllData();
        loadUserAccounts();

        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(createTabs(), BorderLayout.CENTER);

        refreshAll();
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

    // ================= HEADER =================

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK);
        panel.setBorder(new EmptyBorder(18, 25, 18, 25));

        JLabel title = new JLabel("MediCare Pharmacy");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        signedInLabel = new JLabel("Pharmacy Management System  |  Signed in: "
            + currentUser.userId + " (" + currentUser.role + ")");
        signedInLabel.setForeground(new Color(210, 225, 245));
        signedInLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        JButton changeUserId = button("Change User ID", new Color(75, 95, 135));
        changeUserId.addActionListener(e -> changeOwnUserId());
        JButton changePassword = button("Change Password", new Color(75, 95, 135));
        changePassword.addActionListener(e -> changeOwnPassword());
        JButton logout = button("Logout", new Color(175, 65, 65));
        logout.addActionListener(e -> logout());

        right.add(signedInLabel);
        right.add(changeUserId);
        right.add(changePassword);
        right.add(logout);

        panel.add(title, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 8));
        panel.setPreferredSize(new Dimension(185, 0));
        panel.setBackground(new Color(38, 55, 85));
        panel.setBorder(new EmptyBorder(15, 10, 15, 10));

        List<String> names = new ArrayList<>(Arrays.asList(
            "Dashboard", "Medicines", "Sales / POS", "Stock & Alerts",
            "Suppliers", "Sales Reports"));
        if (isAdmin()) names.add("User Accounts");
        names.add("About");

        for (String name : names) {
            JButton button = new JButton(name);
            button.setBackground(new Color(53, 72, 108));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Arial", Font.BOLD, 13));
            button.setFocusPainted(false);
            button.setBorderPainted(false);

            button.addActionListener(e -> {
                if (name.equals("Dashboard")) {
                    showTab(0);
                } else if (name.equals("Medicines")) {
                    showTab(1);
                } else if (name.equals("Sales / POS")) {
                    showTab(2);
                } else if (name.equals("Stock & Alerts")) {
                    showTab(3);
                } else if (name.equals("Suppliers")) {
                    showTab(4);
                } else if (name.equals("Sales Reports")) {
                    showTab(5);
                } else if (name.equals("User Accounts") && isAdmin()) {
                    showTab(6);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "MediCare Pharmacy Management System\n" +
                        "Developed using Java Swing\n" +
                        "Signed in as: " + currentUser.userId + "\n" +
                        "Data storage: TXT files");
                }
            });
            panel.add(button);
        }
        return panel;
    }

    private JTabbedPane createTabs() {
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 13));
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
        tabs.setSelectedIndex(index);
    }

    // ================= USER ACCOUNT MANAGEMENT =================

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel("User Account Management");
        heading.setFont(new Font("Arial", Font.BOLD, 21));
        heading.setForeground(DARK);

        userModel = model(new String[]{"User ID", "Role"});
        userTable = new JTable(userModel);
        styleTable(userTable);

        JButton addEmployee = button("Add Employee", GREEN);
        JButton addAdmin = button("Add Admin", new Color(130, 90, 175));
        JButton edit = button("Edit ID / Reset Password", BLUE);
        JButton delete = button("Delete Account", RED);
        JButton refresh = button("Refresh", DARK);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 9, 8));
        actions.setBackground(Color.WHITE);
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
            "The administrator can create separate Employee and Admin accounts.\n" +
            "New accounts use the default password welcome123 unless changed during setup.\n" +
            "Users can change their own User ID and password from the top bar.\n" +
            "The currently signed-in administrator cannot be deleted from this list.\n" +
            "Passwords are stored as salted hashes, not readable text.");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setBackground(Color.WHITE);
        note.setFont(new Font("Arial", Font.PLAIN, 13));
        note.setBorder(new EmptyBorder(10, 12, 10, 12));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.add(actions, BorderLayout.NORTH);
        south.add(note, BorderLayout.SOUTH);
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
            signedInLabel.setText("Pharmacy Management System  |  Signed in: "
                + currentUser.userId + " (" + currentUser.role + ")");
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
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Welcome to MediCare");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(DARK);

        JPanel cards = new JPanel(new GridLayout(3, 3, 15, 15));
        cards.setBackground(BG);

        totalMedicineLabel = new JLabel("0");
        totalStockLabel = new JLabel("0");
        lowStockLabel = new JLabel("0");
        todaySalesLabel = new JLabel("৳0.00");
        totalRevenueLabel = new JLabel("৳0.00");
        totalProfitLabel = new JLabel("৳0.00");
        expiredMedicineLabel = new JLabel("0");

        cards.add(statCard("Medicine Types", totalMedicineLabel, BLUE));
        cards.add(statCard("Total Stock", totalStockLabel, GREEN));
        cards.add(statCard("Low Stock Items", lowStockLabel, RED));
        cards.add(statCard("Today's Sales", todaySalesLabel,
            new Color(135, 90, 190)));
        cards.add(statCard("Total Revenue", totalRevenueLabel,
            new Color(210, 140, 40)));
        cards.add(statCard("Estimated Profit", totalProfitLabel,
            new Color(30, 145, 145)));
        cards.add(statCard("Expired Medicines", expiredMedicineLabel, new Color(160, 55, 55)));

        int detailIndex = 0;
        for (Component component : cards.getComponents()) {
            final int index = detailIndex++;
            if (component instanceof JButton) {
                JButton cardButton = (JButton) component;
                cardButton.addActionListener(e -> showDashboardDetail(index));
                for (Component child : cardButton.getComponents()) {
                    child.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent event) { cardButton.doClick(); }
                    });
                }
            }
        }

        JPanel center = new JPanel(new BorderLayout(10, 20));
        center.setBackground(BG);
        center.add(heading, BorderLayout.NORTH);
        center.add(cards, BorderLayout.CENTER);

        // Keep the information section compact so the dashboard cards get
        // most of the available height, especially on maximized screens.
        JTextArea note = new JTextArea(
            "PHARMACY MANAGEMENT SYSTEM\n" +
            "Manage your pharmacy from one place.\n" +
            "• Add, edit and remove medicines  |  Sell medicines and generate bills\n" +
            "• Update stock automatically  |  Track expired and low-stock medicines\n" +
            "• Manage suppliers  |  View sales history and profit estimates\n" +
            "Important: Always verify medicine expiry, prescription requirements and applicable pharmacy regulations."
        );

        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setRows(5);
        note.setFont(new Font("Arial", Font.PLAIN, 13));
        note.setBackground(Color.WHITE);
        note.setBorder(new EmptyBorder(12, 15, 12, 15));

        center.add(note, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private JButton statCard(String title, JLabel value, Color color) {
        JButton card = new JButton();
        card.setLayout(new GridLayout(2, 1, 4, 4));
        card.setBackground(color);
        card.setForeground(Color.WHITE);
        // Larger inner spacing makes each card look balanced at full screen.
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        card.setFocusPainted(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel heading = new JLabel(title);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("Arial", Font.BOLD, 17));
        value.setForeground(Color.WHITE);
        value.setFont(new Font("Arial", Font.BOLD, 30));
        card.add(heading);
        card.add(value);
        return card;
    }

    private void showDashboardDetail(int index) {
        String[] columns = {"Date / ID", "Medicine / Details", "Quantity", "Customer", "Amount"};
        DefaultTableModel dm = model(columns);
        if (index == 0) {
            for (Medicine m : medicines) dm.addRow(new Object[]{m.id, m.name + " (" + m.company + ")", m.quantity, m.category, "৳" + money(m.sellPrice)});
        } else if (index == 1) {
            for (Medicine m : medicines) dm.addRow(new Object[]{m.id, m.name, m.quantity, m.company, "৳" + money(m.sellPrice)});
        } else if (index == 2) {
            for (Medicine m : medicines) if (m.quantity <= 10) dm.addRow(new Object[]{m.id, m.name, m.quantity, m.company, "Low stock"});
        } else if (index == 3) {
            String today = LocalDate.now().toString();
            for (Sale sale : sales) if (sale.date.equals(today)) dm.addRow(new Object[]{sale.date, sale.medicineName, sale.quantity, sale.customer, "৳" + money(sale.total)});
        } else if (index == 4 || index == 5) {
            for (Sale sale : sales) dm.addRow(new Object[]{sale.date, sale.medicineName, sale.quantity, sale.customer,
                "৳" + money(index == 4 ? sale.total : sale.total - sale.cost)});
        } else if (index == 6) {
            for (Medicine m : medicines) if (isExpired(m)) {
                dm.addRow(new Object[]{m.id, m.name, m.quantity, m.company, "Expired: " + m.expiry});
            }
        }
        JTable detail = new JTable(dm);
        styleTable(detail);
        String[] titles = {"Medicine Types", "Total Stock", "Low Stock Items", "Today's Sales", "Total Revenue", "Estimated Profit", "Expired Medicines"};
        JOptionPane.showMessageDialog(this, new JScrollPane(detail), titles[index] + " Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= MEDICINE PANEL =================

    private JPanel createMedicinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);

        JTextField search = new JTextField(20);
        JButton searchButton = button("Search", BLUE);
        JButton reset = button("Reset", DARK);

        top.add(new JLabel("Medicine Name:"));
        top.add(search);
        top.add(searchButton);
        top.add(reset);

        medicineModel = model(new String[]{
            "ID", "Medicine", "Company", "Category",
            "Buy Price", "Sell Price", "Stock",
            "Expiry Date", "Supplier"
        });

        medicineTable = new JTable(medicineModel);
        styleTable(medicineTable);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        actions.setBackground(Color.WHITE);

        JButton add = button("Add Medicine", GREEN);
        JButton edit = button("Edit Medicine", BLUE);
        JButton delete = button("Delete Medicine", RED);
        JButton restock = button("Restock", new Color(130, 95, 180));
        JButton importCsv = button("Import CSV", new Color(35, 130, 150));
        JButton exportCsv = button("Export CSV", new Color(65, 115, 170));

        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(restock);
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
        panel.add(new JScrollPane(medicineTable), BorderLayout.CENTER);
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
                        offset = 1; // CSV has an ID column; a fresh ID is assigned on import.
                    } else if (columns.size() == 8) {
                        offset = 0; // CSV has no ID column.
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
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel heading = new JLabel("Medicine Sales / POS - Search and add multiple items");
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        heading.setForeground(DARK);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);
        posSearchField = new JTextField(22);
        JButton search = button("Search", BLUE);
        JButton reset = button("Reset", DARK);
        JButton addToCart = button("Add Selected to Cart", GREEN);
        JButton billFolder = button("Bill Save Location", new Color(105, 90, 160));
        top.add(new JLabel("Search medicine:")); top.add(posSearchField);
        top.add(search); top.add(reset); top.add(addToCart); top.add(billFolder);

        salesMedicineModel = model(new String[]{"ID", "Medicine", "Company", "Price", "Available Stock"});
        salesMedicineTable = new JTable(salesMedicineModel);
        salesMedicineTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        styleTable(salesMedicineTable);
        cartModel = model(new String[]{"ID", "Medicine", "Unit Price", "Quantity", "Line Total"});
        cartTable = new JTable(cartModel);
        styleTable(cartTable);

        JPanel left = new JPanel(new BorderLayout(5,5)); left.setBackground(BG);
        left.add(new JLabel("Available Medicines (Ctrl/Shift select multiple)"), BorderLayout.NORTH);
        left.add(new JScrollPane(salesMedicineTable), BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(5,5)); right.setBackground(BG);
        right.add(new JLabel("Shopping Cart (select an item and use Edit Qty)"), BorderLayout.NORTH);
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.55);

        JButton editQty = button("Edit Qty", GREEN);
        JButton remove = button("Remove Cart Item", RED);
        JButton clear = button("Clear Cart", DARK);
        JButton checkout = button("Checkout / Sell", GREEN);
        JButton refresh = button("Refresh", BLUE);
        JLabel totalLabel = new JLabel("Cart Total: BDT 0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setBackground(Color.WHITE);
        bottom.add(editQty); bottom.add(remove); bottom.add(clear); bottom.add(totalLabel); bottom.add(checkout); bottom.add(refresh);

        Runnable updateCart = () -> {
            cartModel.setRowCount(0); double total = 0;
            for (Map.Entry<Integer,Integer> entry : cart.entrySet()) {
                Medicine m = findMedicine(entry.getKey()); if (m == null) continue;
                double line = m.sellPrice * entry.getValue(); total += line;
                cartModel.addRow(new Object[]{m.id, m.name, "BDT "+money(m.sellPrice), entry.getValue(), "BDT "+money(line)});
            }
            totalLabel.setText("Cart Total: BDT " + money(total));
        };

        search.addActionListener(e -> filterSalesMedicines());
        posSearchField.addActionListener(e -> filterSalesMedicines());
        reset.addActionListener(e -> { posSearchField.setText(""); refreshSalesTable(); });
        billFolder.addActionListener(e -> chooseBillDirectory());
        addToCart.addActionListener(e -> {
            int[] rows = salesMedicineTable.getSelectedRows();
            if (rows.length == 0) { JOptionPane.showMessageDialog(this, "Select one or more medicines first."); return; }
            for (int row : rows) {
                int id = Integer.parseInt(salesMedicineTable.getValueAt(row,0).toString());
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
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid quantity for " + m.name + ": " + ex.getMessage()); }
            }
            updateCart.run();
        });
        editQty.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a cart item first."); return; }
            int id = Integer.parseInt(cartTable.getValueAt(row,0).toString());
            Medicine m = findMedicine(id); if (m == null) return;
            String input = JOptionPane.showInputDialog(this, "Set quantity for " + m.name + " (Available: " + m.quantity + ")", String.valueOf(cart.get(id)));
            if (input == null) return;
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0 || qty > m.quantity) throw new IllegalArgumentException("Quantity must be 1 to " + m.quantity);
                cart.put(id, qty); updateCart.run();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid quantity: " + ex.getMessage()); }
        });
        remove.addActionListener(e -> {
            int row = cartTable.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this,"Select a cart item to remove."); return; }
            cart.remove(Integer.parseInt(cartTable.getValueAt(row,0).toString())); updateCart.run();
        });
        clear.addActionListener(e -> { cart.clear(); updateCart.run(); });
        checkout.addActionListener(e -> checkoutCart());
        refresh.addActionListener(e -> { refreshSalesTable(); filterSalesMedicines(); });
        panel.add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(8,8)); center.add(top, BorderLayout.NORTH); center.add(split, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER); panel.add(bottom, BorderLayout.SOUTH);
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
        for (Medicine m : medicines) if (m.name.toLowerCase(Locale.ROOT).contains(key)
                || m.company.toLowerCase(Locale.ROOT).contains(key) || m.category.toLowerCase(Locale.ROOT).contains(key))
            salesMedicineModel.addRow(new Object[]{m.id,m.name,m.company,"৳"+money(m.sellPrice),m.quantity});
    }

    private void checkoutCart() {
        if (cart.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }
        JTextField customer = new JTextField("Walk-in Customer");
        JTextField phone = new JTextField();
        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        addField(form,"Customer Name:",customer); addField(form,"Phone:",phone);
        if (JOptionPane.showConfirmDialog(this,form,"Checkout Details",JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (customer.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this,"Customer name is required."); return; }
        double total = 0;
        for (Map.Entry<Integer,Integer> e : cart.entrySet()) {
            Medicine m = findMedicine(e.getKey());
            if (m == null || e.getValue() <= 0 || e.getValue() > m.quantity || isExpired(m)) {
                JOptionPane.showMessageDialog(this,"Cart contains unavailable, expired or insufficient-stock medicine: " + (m == null ? e.getKey() : m.name)); return;
            }
            total += m.sellPrice * e.getValue();
        }
        if (JOptionPane.showConfirmDialog(this,"Different medicines: "+cart.size()+"\nTotal: BDT "+money(total)+"\nBill folder: "+billDirectory.getAbsolutePath()+"\nConfirm sale?","Confirm Checkout",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String invoice = "INV-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new java.util.Date());
        String date = LocalDate.now().toString();
        for (Map.Entry<Integer,Integer> e : cart.entrySet()) {
            Medicine m = findMedicine(e.getKey()); int qty = e.getValue();
            m.quantity -= qty;
            sales.add(new Sale(invoice,date,m.id,m.name,qty,qty*m.sellPrice,qty*m.buyPrice,customer.getText().trim(),phone.getText().trim()));
        }
        saveMedicines(); saveSales();
        File pdfFile = createPdfBill(invoice,date,customer.getText().trim(),phone.getText().trim());
        cart.clear(); refreshAll();
        if (pdfFile != null) JOptionPane.showMessageDialog(this,"Sale completed successfully!\nInvoice: "+invoice+"\nPDF saved at:\n"+pdfFile.getAbsolutePath(),"Sale Complete",JOptionPane.INFORMATION_MESSAGE);
    }

    private void chooseBillDirectory() {
        JFileChooser chooser = new JFileChooser(billDirectory);
        chooser.setDialogTitle("Choose Default PDF Invoice Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            billDirectory = chooser.getSelectedFile();
            try {
                if (!billDirectory.exists() && !billDirectory.mkdirs()) throw new IOException("Could not create folder.");
                try (Writer w = new OutputStreamWriter(new FileOutputStream(billLocationFile), StandardCharsets.UTF_8)) {
                    w.write(encode(billDirectory.getAbsolutePath()));
                }
                JOptionPane.showMessageDialog(this,"Default invoice folder updated:\n"+billDirectory.getAbsolutePath());
            } catch (IOException ex) { JOptionPane.showMessageDialog(this,"Could not save folder setting: "+ex.getMessage()); }
        }
    }

    private void loadBillDirectory() {
        if (!billLocationFile.exists()) { billDirectory.mkdirs(); return; }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(billLocationFile), StandardCharsets.UTF_8))) {
            String saved = br.readLine();
            if (saved != null && !saved.trim().isEmpty()) billDirectory = new File(decode(saved.trim()));
        } catch (Exception ex) { billDirectory = new File(folder, "Bills"); }
        if (!billDirectory.exists()) billDirectory.mkdirs();
    }

    private File invoiceFile(String invoice) {
        return new File(billDirectory, "Bill_" + invoice + ".pdf");
    }

    private File createPdfBill(String invoice, String date, String customer, String phone) {
        if (!billDirectory.exists() && !billDirectory.mkdirs()) {
            JOptionPane.showMessageDialog(this,"Cannot access invoice folder: "+billDirectory.getAbsolutePath()); return null;
        }
        File target = invoiceFile(invoice);
        List<String> lines = new ArrayList<>();
        lines.add("MEDICARE PHARMACY"); lines.add("PHARMACY MANAGEMENT SYSTEM");
        lines.add("Invoice: "+invoice); lines.add("Date: "+date); lines.add("Customer: "+customer); lines.add("Phone: "+phone);
        lines.add("------------------------------------------------------------");
        lines.add(String.format(Locale.US,"%-25s %8s %12s %12s","Medicine","Qty","Unit Price","Total"));
        double grand = 0;
        for (Sale sale : sales) if (sale.invoiceId.equals(invoice)) {
            lines.add(String.format(Locale.US,"%-25s %8d %12s %12s",pdfSafe(sale.medicineName),sale.quantity,money(sale.total/sale.quantity),money(sale.total)));
            grand += sale.total;
        }
        lines.add("------------------------------------------------------------"); lines.add("GRAND TOTAL: BDT "+money(grand)); lines.add("Thank you for shopping with us.");
        try { writeSimplePdf(target,lines); return target; }
        catch (IOException ex) { JOptionPane.showMessageDialog(this,"Could not create PDF: "+ex.getMessage()); return null; }
    }

    private void openInvoice(String invoice) {
        File pdf = invoiceFile(invoice);
        if (!pdf.exists()) {
            Sale first = null;
            for (Sale sale : sales) if (sale.invoiceId.equals(invoice)) { first = sale; break; }
            if (first != null) pdf = createPdfBill(invoice, first.date, first.customer, first.phone);
        }
        if (pdf == null || !pdf.exists()) { JOptionPane.showMessageDialog(this,"Invoice PDF not found or could not be generated."); return; }
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf);
            else JOptionPane.showMessageDialog(this,"Invoice saved at:\n"+pdf.getAbsolutePath());
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Invoice saved at:\n"+pdf.getAbsolutePath()+"\nCould not open automatically: "+ex.getMessage()); }
    }

    private String pdfSafe(String value) { return value.replaceAll("[^ -~]", "?"); }

    private void writeSimplePdf(File file, List<String> lines) throws IOException {
        StringBuilder stream = new StringBuilder("BT\n/F1 12 Tf\n50 790 Td\n");
        for (String line : lines) { stream.append("(").append(line.replace("\\","\\\\").replace("(","\\(").replace(")","\\)")).append(") Tj\n0 -22 Td\n"); }
        stream.append("ET");
        byte[] content = stream.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = new ArrayList<>();
        objects.add("<</Type/Catalog/Pages 2 0 R>>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<</Type/Pages/Kids[3 0 R]/Count 1>>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<</Type/Font/Subtype/Type1/BaseFont/Courier>>".getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream obj5 = new ByteArrayOutputStream(); obj5.write(("<</Length "+content.length+">>\nstream\n").getBytes(StandardCharsets.US_ASCII)); obj5.write(content); obj5.write("\nendstream".getBytes(StandardCharsets.US_ASCII)); objects.add(obj5.toByteArray());
        ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i=0;i<objects.size();i++) { offsets.add(out.size()); out.write((i+1+" 0 obj\n").getBytes(StandardCharsets.US_ASCII)); out.write(objects.get(i)); out.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII)); }
        int xref=out.size(); out.write(("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for(int i=1;i<offsets.size();i++) out.write(String.format(Locale.US,"%010d 00000 n \n",offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        out.write(("trailer<</Size "+(objects.size()+1)+"/Root 1 0 R>>\nstartxref\n"+xref+"\n%%EOF").getBytes(StandardCharsets.US_ASCII));
        try(FileOutputStream fos=new FileOutputStream(file)){out.writeTo(fos);}
    }

    // ================= STOCK & ALERTS =================

    private JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel("Stock, Low Stock & Expiry Alerts");
        heading.setFont(new Font("Arial", Font.BOLD, 20));

        alertModel = model(new String[]{
            "ID", "Medicine", "Stock", "Expiry Date", "Alert"
        });

        alertTable = new JTable(alertModel);
        styleTable(alertTable);

        JButton refresh = button("Refresh Alerts", BLUE);
        refresh.addActionListener(e -> refreshAlertTable());

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(alertTable), BorderLayout.CENTER);
        panel.add(refresh, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshAlertTable() {
        if (alertModel == null) return;

        alertModel.setRowCount(0);

        for (Medicine m : medicines) {
            String alert = getAlert(m);

            if (!alert.equals("Normal")) {
                alertModel.addRow(new Object[]{
                    m.id, m.name, m.quantity, m.expiry, alert
                });
            }
        }
    }

    private String getAlert(Medicine m) {
        if (isExpired(m)) return "EXPIRED";

        try {
            LocalDate expiry = parseExpiryDate(m.expiry);

            if (!expiry.isAfter(LocalDate.now().plusDays(30))) {
                return "EXPIRING SOON";
            }
        } catch (Exception ignored) {
            return "INVALID EXPIRY DATE";
        }

        if (m.quantity <= 10) return "LOW STOCK";

        return "Normal";
    }

    private boolean isExpired(Medicine m) {
        try {
            return parseExpiryDate(m.expiry).isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    // ================= SUPPLIER MANAGEMENT =================

    private JPanel createSupplierPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        supplierModel = model(new String[]{
            "ID", "Supplier Name", "Phone", "Address"
        });

        supplierTable = new JTable(supplierModel);
        styleTable(supplierTable);

        JButton add = button("Add Supplier", GREEN);
        JButton edit = button("Edit Supplier", BLUE);
        JButton delete = button("Delete Supplier", RED);
        JButton importCsv = button("Import Suppliers CSV", new Color(35,130,150));

        JPanel buttons = new JPanel();
        buttons.setBackground(Color.WHITE);
        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(importCsv);

        add.addActionListener(e -> addSupplier());
        edit.addActionListener(e -> editSupplier());
        delete.addActionListener(e -> deleteSupplier());
        importCsv.addActionListener(e -> importSuppliersFromCSV());

        panel.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private void importSuppliersFromCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)","csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int added=0, skipped=0, rowNo=0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(chooser.getSelectedFile()),StandardCharsets.UTF_8))) {
            String line; boolean first=true;
            while ((line=br.readLine())!=null) {
                rowNo++; if(line.trim().isEmpty()) continue;
                List<String> c=parseCSVLine(line);
                if(first){ first=false; String h=c.get(0).replace("\uFEFF","").trim().toLowerCase(Locale.ROOT); if(h.equals("id")||h.contains("supplier")||h.equals("name")) continue; }
                int offset = c.size()==4 ? 1 : 0;
                if(c.size()!=3 && c.size()!=4){ skipped++; continue; }
                String name=c.get(offset).trim(), phone=c.get(offset+1).trim(), address=c.get(offset+2).trim();
                if(name.isEmpty()){ skipped++; continue; }
                boolean duplicate=false; for(Supplier old:suppliers) if(old.name.equalsIgnoreCase(name)&&old.phone.equalsIgnoreCase(phone)){duplicate=true;break;}
                if(duplicate){skipped++;continue;}
                suppliers.add(new Supplier(getNextSupplierId(),name,phone,address)); added++;
            }
            if(added>0){saveSuppliers();refreshAll();}
            JOptionPane.showMessageDialog(this,"Supplier CSV import completed.\nAdded: "+added+"\nSkipped/invalid: "+skipped+"\nFormat: Name, Phone, Address (optional ID as first column). ");
        } catch(Exception ex){JOptionPane.showMessageDialog(this,"Supplier CSV import failed at/near row "+rowNo+": "+ex.getMessage());}
    }

    private void addSupplier() {
        JTextField name = new JTextField();
        JTextField phone = new JTextField();
        JTextField address = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        addField(form, "Supplier Name:", name);
        addField(form, "Phone:", phone);
        addField(form, "Address:", address);

        int result = JOptionPane.showConfirmDialog(
            this, form, "Add Supplier",
            JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Supplier name is required!");
            return;
        }

        suppliers.add(new Supplier(
            getNextSupplierId(),
            name.getText().trim(),
            phone.getText().trim(),
            address.getText().trim()
        ));

        saveSuppliers();
        refreshAll();
    }

    private void editSupplier() {
        int row = supplierTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Select a supplier first!");
            return;
        }

        int id = Integer.parseInt(
            supplierTable.getValueAt(row, 0).toString());

        Supplier s = findSupplier(id);
        if (s == null) return;

        JTextField name = new JTextField(s.name);
        JTextField phone = new JTextField(s.phone);
        JTextField address = new JTextField(s.address);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        addField(form, "Name:", name);
        addField(form, "Phone:", phone);
        addField(form, "Address:", address);

        int result = JOptionPane.showConfirmDialog(
            this, form, "Edit Supplier",
            JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Supplier name is required!");
            return;
        }

        s.name = name.getText().trim();
        s.phone = phone.getText().trim();
        s.address = address.getText().trim();

        saveSuppliers();
        refreshAll();
    }

    private void deleteSupplier() {
        int row = supplierTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Select a supplier first!");
            return;
        }

        int id = Integer.parseInt(
            supplierTable.getValueAt(row, 0).toString());

        Supplier s = findSupplier(id);
        if (s == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this, "Delete supplier " + s.name + "?",
            "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            suppliers.remove(s);
            saveSuppliers();
            refreshAll();
        }
    }

    private Supplier findSupplier(int id) {
        for (Supplier s : suppliers) {
            if (s.id == id) return s;
        }
        return null;
    }

    // ================= SALES REPORT =================

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        reportModel = model(new String[]{
            "Invoice (click to open PDF)", "Date", "Items", "Total Qty",
            "Customer", "Phone", "Total", "Profit"
        });

        reportTable = new JTable(reportModel);
        styleTable(reportTable);
        reportTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = reportTable.rowAtPoint(e.getPoint());
                int col = reportTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 0) openInvoice(reportTable.getValueAt(row, 0).toString());
            }
        });
        reportTable.setToolTipText("Click an invoice number to open its PDF bill");

        JLabel heading = new JLabel("Sales History & Reports");
        heading.setFont(new Font("Arial", Font.BOLD, 20));

        JButton refresh = button("Refresh Report", BLUE);
        JButton summary = button("Sales Summary", GREEN);

        JPanel bottom = new JPanel();
        bottom.setBackground(Color.WHITE);
        bottom.add(refresh);
        bottom.add(summary);

        refresh.addActionListener(e -> refreshReportTable());
        summary.addActionListener(e -> showSalesSummary());

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshReportTable() {
        if (reportModel == null) return;
        reportModel.setRowCount(0);
        LinkedHashMap<String, List<Sale>> grouped = new LinkedHashMap<>();
        for (int i = sales.size() - 1; i >= 0; i--) {
            Sale sale = sales.get(i);
            grouped.computeIfAbsent(sale.invoiceId, k -> new ArrayList<>()).add(sale);
        }
        for (Map.Entry<String, List<Sale>> entry : grouped.entrySet()) {
            List<Sale> items = entry.getValue();
            Sale first = items.get(0);
            StringBuilder names = new StringBuilder();
            int quantity = 0; double total = 0, profit = 0;
            for (Sale item : items) {
                if (names.length() > 0) names.append(", ");
                names.append(item.medicineName);
                quantity += item.quantity; total += item.total; profit += item.total - item.cost;
            }
            reportModel.addRow(new Object[]{entry.getKey(), first.date, names.toString(), quantity,
                first.customer, first.phone, "BDT " + money(total), "BDT " + money(profit)});
        }
    }

    private void showSalesSummary() {
        double revenue = 0;
        double profit = 0;
        int totalItems = 0;

        for (Sale s : sales) {
            revenue += s.total;
            profit += s.total - s.cost;
            totalItems += s.quantity;
        }

        java.util.Set<String> invoiceIds = new java.util.HashSet<>();
        for (Sale sale : sales) invoiceIds.add(sale.invoiceId);
        String summary =
            "TOTAL SALES REPORT\n\n" +
            "Number of Transactions: " + invoiceIds.size() + "\n" +
            "Items Sold: " + totalItems + "\n" +
            "Total Revenue: ৳" + money(revenue) + "\n" +
            "Estimated Gross Profit: ৳" + money(profit);

        JOptionPane.showMessageDialog(this, summary,
            "Sales Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= REFRESH METHODS =================

    private void refreshAll() {
        refreshMedicineTable();
        refreshSalesTable();
        refreshSupplierTable();
        refreshReportTable();
        refreshAlertTable();
        refreshDashboard();
    }

    private void refreshMedicineTable() {
        if (medicineModel == null) return;

        medicineModel.setRowCount(0);

        for (Medicine m : medicines) {
            medicineModel.addRow(medicineRow(m));
        }
    }

    private void refreshSalesTable() {
        if (salesMedicineModel == null) return;

        salesMedicineModel.setRowCount(0);

        for (Medicine m : medicines) {
            salesMedicineModel.addRow(new Object[]{
                m.id, m.name, m.company,
                "৳" + money(m.sellPrice), m.quantity
            });
        }
        if (posSearchField != null && !posSearchField.getText().trim().isEmpty()) filterSalesMedicines();
    }

    private void refreshSupplierTable() {
        if (supplierModel == null) return;

        supplierModel.setRowCount(0);

        for (Supplier s : suppliers) {
            supplierModel.addRow(new Object[]{
                s.id, s.name, s.phone, s.address
            });
        }
    }

    private void refreshDashboard() {
        if (totalMedicineLabel == null) return;

        int stock = 0;
        int low = 0;
        int expired = 0;

        for (Medicine m : medicines) {
            stock += m.quantity;

            if (m.quantity <= 10) {
                low++;
            }
            if (isExpired(m)) {
                expired++;
            }
        }

        double revenue = 0;
        double profit = 0;
        double today = 0;

        String todayDate = LocalDate.now().toString();

        for (Sale s : sales) {
            revenue += s.total;
            profit += s.total - s.cost;

            if (s.date.equals(todayDate)) {
                today += s.total;
            }
        }

        totalMedicineLabel.setText(String.valueOf(medicines.size()));
        totalStockLabel.setText(String.valueOf(stock));
        lowStockLabel.setText(String.valueOf(low));
        todaySalesLabel.setText("৳" + money(today));
        totalRevenueLabel.setText("৳" + money(revenue));
        totalProfitLabel.setText("৳" + money(profit));
        expiredMedicineLabel.setText(String.valueOf(expired));
    }

    private Object[] medicineRow(Medicine m) {
        return new Object[]{
            m.id, m.name, m.company, m.category,
            money(m.buyPrice), money(m.sellPrice),
            m.quantity, m.expiry, m.supplier
        };
    }

    // ================= COMMON HELPERS =================

    private DefaultTableModel model(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(205, 220, 245));
        table.setGridColor(new Color(220, 225, 235));

        table.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13));

        table.getTableHeader().setBackground(DARK);
        table.getTableHeader().setForeground(Color.WHITE);
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 13, 10, 13));

        return b;
    }

    private void addField(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private Medicine selectedMedicine(JTable table) {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a medicine first!");
            return null;
        }

        int id = Integer.parseInt(
            table.getValueAt(row, 0).toString());

        for (Medicine m : medicines) {
            if (m.id == id) return m;
        }

        return null;
    }

    private int getNextMedicineId() {
        int max = 0;

        for (Medicine m : medicines) {
            max = Math.max(max, m.id);
        }

        return max + 1;
    }

    private int getNextSupplierId() {
        int max = 0;

        for (Supplier s : suppliers) {
            max = Math.max(max, s.id);
        }

        return max + 1;
    }

    private String money(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    // ================= FILE ENCODING =================

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
            value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8);
    }

    // ================= LOAD ALL DATA =================

    private void loadAllData() {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        loadMedicines();
        loadSuppliers();
        loadSales();
        loadBillDirectory();
    }

    private void loadMedicines() {
        if (!medicineFile.exists()) return;

        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(medicineFile),
                StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);

                if (p.length == 9) {
                    try {
                        medicines.add(new Medicine(
                            Integer.parseInt(p[0]),
                            decode(p[1]),
                            decode(p[2]),
                            decode(p[3]),
                            Double.parseDouble(p[4]),
                            Double.parseDouble(p[5]),
                            Integer.parseInt(p[6]),
                            decode(p[7]),
                            decode(p[8])
                        ));
                    } catch (Exception ex) {
                        System.out.println(
                            "Skipped invalid medicine record.");
                    }
                }
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Medicine file error: " + ex.getMessage());
        }
    }

    private void loadSuppliers() {
        if (!supplierFile.exists()) return;

        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(supplierFile),
                StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);

                if (p.length == 4) {
                    try {
                        suppliers.add(new Supplier(
                            Integer.parseInt(p[0]),
                            decode(p[1]),
                            decode(p[2]),
                            decode(p[3])
                        ));
                    } catch (Exception ex) {
                        System.out.println(
                            "Skipped invalid supplier record.");
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadSales() {
        if (!salesFile.exists()) return;

        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(salesFile),
                StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);

                if (p.length == 8 || p.length == 9) {
                    try {
                        int offset = p.length == 9 ? 1 : 0;
                        String invoice = p.length == 9 ? decode(p[0]) : "LEGACY-" + decode(p[0]) + "-" + p[1];
                        sales.add(new Sale(
                            invoice, decode(p[offset]),
                            Integer.parseInt(p[offset+1]), decode(p[offset+2]),
                            Integer.parseInt(p[offset+3]), Double.parseDouble(p[offset+4]),
                            Double.parseDouble(p[offset+5]), decode(p[offset+6]), decode(p[offset+7])
                        ));
                    } catch (Exception ex) {
                        System.out.println(
                            "Skipped invalid sale record.");
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ================= SAVE DATA =================

    private void saveMedicines() {
        try (PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(medicineFile),
                StandardCharsets.UTF_8))) {

            for (Medicine m : medicines) {
                out.println(
                    m.id + "|" +
                    encode(m.name) + "|" +
                    encode(m.company) + "|" +
                    encode(m.category) + "|" +
                    m.buyPrice + "|" +
                    m.sellPrice + "|" +
                    m.quantity + "|" +
                    encode(m.expiry) + "|" +
                    encode(m.supplier)
                );
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not save medicines: " + ex.getMessage());
        }
    }

    private void saveSuppliers() {
        try (PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(supplierFile),
                StandardCharsets.UTF_8))) {

            for (Supplier s : suppliers) {
                out.println(
                    s.id + "|" +
                    encode(s.name) + "|" +
                    encode(s.phone) + "|" +
                    encode(s.address)
                );
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not save suppliers: " + ex.getMessage());
        }
    }

    private void saveSales() {
        try (PrintWriter out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(salesFile),
                StandardCharsets.UTF_8))) {

            for (Sale s : sales) {
                out.println(
                    encode(s.invoiceId) + "|" +
                    encode(s.date) + "|" +
                    s.medicineId + "|" +
                    encode(s.medicineName) + "|" +
                    s.quantity + "|" +
                    s.total + "|" +
                    s.cost + "|" +
                    encode(s.customer) + "|" +
                    encode(s.phone)
                );
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not save sales: " + ex.getMessage());
        }
    }

    private LocalDate parseExpiryDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date is empty.");
        }

        date = date.trim();

        try {
            return LocalDate.parse(date); // yyyy-MM-dd
        } catch (DateTimeParseException ignored) {
            // Try other common date formats below.
        }

        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US));
        } catch (DateTimeParseException ignored) {
            // Try day/month/year.
        }

        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("d/M/uuuu", Locale.US));
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("Invalid expiry date: " + date
                + ". Use YYYY-MM-DD or a valid date such as 12/31/2027.");
        }
    }

    // ================= LOGIN AND PASSWORD SECURITY =================

    private void loadUserAccounts() {
        try {
            userAccounts = readUserAccounts(userFile);
            for (UserAccount account : userAccounts) {
                if (account.userId.equalsIgnoreCase(currentUser.userId)
                        && account.role.equalsIgnoreCase(currentUser.role)) {
                    currentUser = account;
                    break;
                }
            }
        } catch (Exception ex) {
            userAccounts = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Could not read user accounts: " + ex.getMessage(),
                "Account File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveUserAccounts() {
        try {
            writeUserAccounts(userFile, userAccounts);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save user accounts: " + ex.getMessage(),
                "Account File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean initializeDefaultAdmin(File accountFile) throws Exception {
        File parent = accountFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create PharmacyData folder.");
        }
        List<UserAccount> accounts = readUserAccounts(accountFile);
        if (!accounts.isEmpty()) return false;
        char[] defaultPassword = "admin123".toCharArray();
        accounts.add(createUserAccount("admin", "ADMIN", defaultPassword));
        Arrays.fill(defaultPassword, '\0');
        writeUserAccounts(accountFile, accounts);
        return true;
    }

    private static JFrame createLoginBackground() {
        JFrame frame = new JFrame("MediCare Pharmacy - Welcome");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(920, 620);
        frame.setMinimumSize(new Dimension(760, 520));
        frame.setLocationRelativeTo(null);
        frame.setContentPane(new LoginWatermarkPanel());
        return frame;
    }

    private static UserAccount showLoginDialog(File accountFile, JFrame owner, boolean firstSetup) {
        while (true) {
            JDialog dialog = new JDialog(owner, "MediCare Pharmacy - Login", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);

            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setBorder(new EmptyBorder(18, 20, 16, 20));
            content.setBackground(new Color(248, 250, 253));

            JLabel heading = new JLabel("Sign in to continue", SwingConstants.CENTER);
            heading.setFont(new Font("Arial", Font.BOLD, 19));
            heading.setForeground(new Color(28, 55, 85));

            JTextField userId = new JTextField(19);
            JPasswordField password = new JPasswordField(19);
            userId.setFont(new Font("Arial", Font.PLAIN, 14));
            password.setFont(new Font("Arial", Font.PLAIN, 14));

            JPanel form = new JPanel(new GridLayout(0, 2, 10, 12));
            form.setOpaque(false);
            addStaticField(form, "User ID:", userId);
            addStaticField(form, "Password:", password);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            buttons.setOpaque(false);
            JButton login = new JButton("Login");
            login.setBackground(new Color(38, 105, 180));
            login.setForeground(Color.WHITE);
            login.setFont(new Font("Arial", Font.BOLD, 13));
            login.setFocusPainted(false);
            JButton cancel = new JButton("Exit");
            buttons.add(login);
            buttons.add(cancel);

            content.add(heading, BorderLayout.NORTH);
            content.add(form, BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout(5, 8));
            south.setOpaque(false);
            JLabel hint = new JLabel(firstSetup
                ? "First setup default: User ID admin  |  Password admin123"
                : "Use the User ID and password assigned to you.", SwingConstants.CENTER);
            hint.setFont(new Font("Arial", Font.PLAIN, 11));
            hint.setForeground(new Color(95, 105, 120));
            south.add(hint, BorderLayout.NORTH);
            south.add(buttons, BorderLayout.SOUTH);
            content.add(south, BorderLayout.SOUTH);
            dialog.setContentPane(content);
            dialog.getRootPane().setDefaultButton(login);

            final UserAccount[] authenticated = new UserAccount[1];
            login.addActionListener(e -> {
                char[] enteredPassword = password.getPassword();
                try {
                    String enteredId = userId.getText().trim();
                    if (enteredId.isEmpty() || enteredPassword.length == 0) {
                        JOptionPane.showMessageDialog(dialog, "Enter both User ID and Password.",
                            "Login Required", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    List<UserAccount> accounts = readUserAccounts(accountFile);
                    for (UserAccount account : accounts) {
                        if (account.userId.equalsIgnoreCase(enteredId)
                                && passwordMatches(account, enteredPassword)) {
                            authenticated[0] = account;
                            dialog.dispose();
                            return;
                        }
                    }
                    JOptionPane.showMessageDialog(dialog,
                        "Invalid User ID or Password. Please try again.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                    password.setText("");
                    password.requestFocusInWindow();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                        "Login could not be completed: " + ex.getMessage(),
                        "Login Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    Arrays.fill(enteredPassword, '\0');
                }
            });
            cancel.addActionListener(e -> dialog.dispose());

            dialog.pack();
            dialog.setSize(430, 245);
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
            if (authenticated[0] != null) return authenticated[0];
            return null;
        }
    }

    private static class LoginWatermarkPanel extends JPanel {
        LoginWatermarkPanel() {
            setBackground(new Color(247, 250, 253));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // Very light, watermark-style MediCare emblem and wordmark.
            g.setColor(new Color(38, 112, 154, 24));
            g.fillOval(centerX - 78, centerY - 125, 156, 156);
            g.setColor(new Color(255, 255, 255, 115));
            g.fillRoundRect(centerX - 18, centerY - 103, 36, 112, 8, 8);
            g.fillRoundRect(centerX - 56, centerY - 65, 112, 36, 8, 8);

            g.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            String brand = "MediCare";
            g.setColor(new Color(28, 68, 104, 25));
            g.drawString(brand, centerX - fm.stringWidth(brand) / 2, centerY + 95);

            g.setFont(new Font("Arial", Font.PLAIN, 17));
            String subtitle = "PHARMACY MANAGEMENT SYSTEM";
            fm = g.getFontMetrics();
            g.drawString(subtitle, centerX - fm.stringWidth(subtitle) / 2, centerY + 130);
            g.dispose();
        }
    }

    private static void addStaticField(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private static List<UserAccount> readUserAccounts(File file) throws IOException {
        List<UserAccount> accounts = new ArrayList<>();
        if (!file.exists()) return accounts;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) continue;
                try {
                    String id = decodeStatic(parts[0]);
                    String role = parts[1].trim().toUpperCase(Locale.ROOT);
                    if (!id.isEmpty() && (role.equals("ADMIN") || role.equals("EMPLOYEE"))) {
                        accounts.add(new UserAccount(id, role, parts[2], parts[3]));
                    }
                } catch (Exception ignored) {
                    // Skip malformed account records without exposing stored password data.
                }
            }
        }
        return accounts;
    }

    private static void writeUserAccounts(File file, List<UserAccount> accounts) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create account data folder.");
        }
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (UserAccount account : accounts) {
                writer.println(encodeStatic(account.userId) + "|" + account.role + "|"
                    + account.salt + "|" + account.passwordHash);
            }
            if (writer.checkError()) throw new IOException("Could not write account file.");
        }
    }

    private static UserAccount createUserAccount(String userId, String role, char[] password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltText = Base64.getEncoder().encodeToString(salt);
        String hashText = hashPassword(password, salt);
        return new UserAccount(userId, role, saltText, hashText);
    }

    private static void applyNewPassword(UserAccount account, char[] password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        account.salt = Base64.getEncoder().encodeToString(salt);
        account.passwordHash = hashPassword(password, salt);
    }

    private static String hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, PASSWORD_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean passwordMatches(UserAccount account, char[] password) throws Exception {
        byte[] salt = Base64.getDecoder().decode(account.salt);
        byte[] expected = Base64.getDecoder().decode(account.passwordHash);
        byte[] actual = Base64.getDecoder().decode(hashPassword(password, salt));
        return MessageDigest.isEqual(expected, actual);
    }

    private static String encodeStatic(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeStatic(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    // ================= MAIN =================

    private static void launchLoginFlow() {
        File dataFolder = new File("PharmacyData");
        File accountsFile = new File(dataFolder, "users.txt");
        JFrame background = createLoginBackground();
        background.setVisible(true);
        try {
            boolean firstSetup = initializeDefaultAdmin(accountsFile);
            UserAccount loggedInUser = showLoginDialog(accountsFile, background, firstSetup);
            background.dispose();
            if (loggedInUser != null) {
                new PharmacyManagement(loggedInUser).setVisible(true);
            }
        } catch (Exception ex) {
            background.dispose();
            JOptionPane.showMessageDialog(null,
                "Application could not start: " + ex.getMessage(),
                "Startup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> launchLoginFlow());
    }
}
