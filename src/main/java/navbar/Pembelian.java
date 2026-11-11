/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package navbar;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
/**
 *
 * @author ACER
 */
public class Pembelian extends javax.swing.JPanel {

    Connection conn;
    String idBarangDipilih = "";
    ArrayList<Object[]> keranjang = new ArrayList<>();
    
    
    
    /**
     * Creates new form Penjualan
     */
    String keteranganFinal = "";
    
    public Pembelian() {
        initComponents();
        conn = koneksi.getConnection();
        autoNoPO(); // mengisi jTextField1 (ID_Pembelian), jTextField2 (No_PO), dan jTextField3 (tanggal)
        jTextField2.setEditable(false);
        jTextField1.setEditable(false);
        jTextField3.setEditable(false); // tanggal
        jTextField5.setEditable(false); // nama barang
        jTextField6.setEditable(false); // harga beli
        setupTable();
        loadSupplier();
        
        jTextField8.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { keteranganFinal = jTextField8.getText().trim(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { keteranganFinal = jTextField8.getText().trim(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { keteranganFinal = jTextField8.getText().trim(); }
    });
    }
    
    private void setupTable() {
        jTable1.setModel(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Nama Barang", "Qty", "Harga Beli", "Subtotal"}
        ) {
            Class[] types = {String.class, Integer.class, String.class, String.class};
            public Class getColumnClass(int i) { return types[i]; }
            public boolean isCellEditable(int r, int c) { return false; }
        });
    }
    
    private void loadSupplier() {
        try {
            jComboBox1.removeAllItems();
            jComboBox1.addItem("-- Pilih Supplier --");
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT ID_Supplier, Nama_Supplier FROM supplier ORDER BY Nama_Supplier");
            while (rs.next()) {
                jComboBox1.addItem(rs.getString("ID_Supplier") + " | " + rs.getString("Nama_Supplier"));
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal load supplier: " + e.getMessage());
        }
    }
    

    // Auto-generate No. PO dan ID Pembelian
    private void autoNoPO() {
        try {
            String tanggal = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
 
            // Auto ID_Pembelian
            ResultSet rsId = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM pembelian");
            rsId.next();
            String idPembelian = String.format("PMB%03d", rsId.getInt(1) + 1);
            jTextField1.setText(idPembelian);
 
            // Auto No_PO
            ResultSet rsPO = conn.createStatement().executeQuery(
                "SELECT No_PO FROM pembelian WHERE No_PO LIKE 'PO-" + tanggal + "%' " +
                "ORDER BY No_PO DESC LIMIT 1");
            int urut = 1;
            if (rsPO.next()) {
                String last = rsPO.getString("No_PO");
                urut = Integer.parseInt(last.substring(last.length() - 3)) + 1;
            }
            jTextField2.setText(String.format("PO-%s%03d", tanggal, urut));
 
            // Tanggal
            jTextField3.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
 
        } catch (Exception e) {
            jTextField1.setText("PMB001");
            jTextField2.setText("PO-001");
        }
    }
    
    private void pilihBarang(String[] data) {
        idBarangDipilih = data[0];
        jTextField5.setText(data[2]);         // Nama_Barang
        jTextField6.setText(data[3]);         // Harga_Beli
    }

    // Cari barang (semua barang, tidak perlu cek stok > 0 karena ini pembelian)
    private void cariBarang() {
        String keyword = jTextField4.getText().trim();
        if (keyword.isEmpty()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT ID_Barang, Kode_Barang, Nama_Barang, Harga_Beli " +
                "FROM barang " +
                "WHERE Kode_Barang LIKE ? OR Nama_Barang LIKE ?");
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
 
            java.util.List<String[]> hasil = new java.util.ArrayList<>();
            while (rs.next()) {
                hasil.add(new String[]{
                    rs.getString("ID_Barang"),
                    rs.getString("Kode_Barang"),
                    rs.getString("Nama_Barang"),
                    String.valueOf(rs.getDouble("Harga_Beli"))
                });
            }
 
            if (hasil.isEmpty()) {
                jTextField5.setText("Tidak ditemukan");
                jTextField6.setText("0");
                idBarangDipilih = "";
                return;
            }
 
            if (hasil.size() == 1) {
                pilihBarang(hasil.get(0));
            } else {
                String[] opsi = new String[hasil.size()];
                for (int i = 0; i < hasil.size(); i++) {
                    opsi[i] = hasil.get(i)[1] + " - " + hasil.get(i)[2] +
                               " (Rp " + String.format("%,.0f", Double.parseDouble(hasil.get(i)[3])) + ")";
                }
                String pilihan = (String) javax.swing.JOptionPane.showInputDialog(
                    this, "Pilih barang:", "Hasil Pencarian",
                    javax.swing.JOptionPane.PLAIN_MESSAGE, null, opsi, opsi[0]);
                if (pilihan != null) {
                    int idx = java.util.Arrays.asList(opsi).indexOf(pilihan);
                    pilihBarang(hasil.get(idx));
                }
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error cari barang: " + e.getMessage());
        }
    }

    // Tambah ke keranjang
    // PERUBAHAN: kalau barang yang sama sudah ada di keranjang, qty digabung
    // dan subtotal dihitung ulang, bukan menambah baris baru.
    private void tambahKeKeranjang() {
        if (idBarangDipilih.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Pilih barang terlebih dahulu!");
            return;
        }
        try {
            String namaBarang = jTextField5.getText();
            double hargaBeli  = Double.parseDouble(jTextField6.getText());
            int qtyCnt        = Integer.parseInt(jTextField7.getText().trim());
 
            if (qtyCnt <= 0) {
                javax.swing.JOptionPane.showMessageDialog(this, "Qty harus lebih dari 0!");
                return;
            }
 
            // Cek apakah barang sudah ada di keranjang -> gabung qty
            int existingIndex = -1;
            for (int i = 0; i < keranjang.size(); i++) {
                if (keranjang.get(i)[0].equals(idBarangDipilih)) {
                    existingIndex = i;
                    break;
                }
            }
 
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
 
            if (existingIndex >= 0) {
                Object[] item = keranjang.get(existingIndex);
                int qtyBaru = (int) item[3] + qtyCnt;
                double subBaru = hargaBeli * qtyBaru;
                item[3] = qtyBaru;
                item[4] = subBaru;
 
                model.setValueAt(qtyBaru, existingIndex, 1);
                model.setValueAt("Rp " + String.format("%,.0f", subBaru), existingIndex, 3);
            } else {
                double sub = hargaBeli * qtyCnt;
                // item: [idBarang, namaBarang, hargaBeli, qty, subtotal]
                keranjang.add(new Object[]{idBarangDipilih, namaBarang, hargaBeli, qtyCnt, sub});
 
                model.addRow(new Object[]{
                    namaBarang,
                    qtyCnt,
                    "Rp " + String.format("%,.0f", hargaBeli),
                    "Rp " + String.format("%,.0f", sub)
                });
            }
 
            updateTotal();
            resetInputBarang();
 
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Qty harus berupa angka!");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private double hitungTotal() {
        double total = 0;
        for (Object[] item : keranjang) total += (double) item[4];
        return total;
    }

    private void updateTotal() {
        labelTotal.setText("Rp " + String.format("%,.0f", hitungTotal()));
        hitungKembalian();
    }
    
        // BARU: parsing input bayar dengan aman.
    // inputBayar defaultnya berisi teks "-" (lihat initComponents),
    // jadi perlu ditangani supaya tidak melempar NumberFormatException
    // saat user belum mengetik apa pun di field ini.
    private double parseBayar() {
        String teks = inputBayar.getText().trim();
        if (teks.isEmpty() || teks.equals("-")) {
            return 0;
        }
        // Buang "Rp", titik, dan spasi kalau user sempat mengetik format rupiah
        teks = teks.replace("Rp", "").replace(".", "").replace(",", "").trim();
        if (teks.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(teks);
        } catch (NumberFormatException e) {
            return -1; // sentinel: nilai tidak valid
        }
    }
    
        // BARU: hitung & tampilkan kembalian setiap kali bayar atau total berubah.
    private void hitungKembalian() {
        double bayar = parseBayar();
        double total = hitungTotal();
        if (bayar < 0) {
            kembalian.setText("Rp -");
            return;
        }
        double sisa = bayar - total;
        kembalian.setText("Rp " + String.format("%,.0f", sisa));
    }

    private void resetSemua() {
        keranjang.clear();
        ((DefaultTableModel) jTable1.getModel()).setRowCount(0);
        resetInputBarang();
        labelTotal.setText("Rp -");
        kembalian.setText("Rp -");
        inputBayar.setText("-");
        jComboBox1.setSelectedIndex(0);
        keteranganFinal = "";       // ← reset field instance
        jTextField8.setText("");    // ← baru reset di sini, bukan di resetInputBarang
        autoNoPO();
    }

    private void resetInputBarang() {
        idBarangDipilih = "";
        jTextField4.setText("");
        jTextField5.setText("");
        jTextField6.setText("0");
        jTextField7.setText("1");
//        jTextField8.setText("");
    }

    // Reset semua (batal)
//    private void resetSemua() {
//        keranjang.clear();
//        ((DefaultTableModel) jTable1.getModel()).setRowCount(0);
//        resetInputBarang();
//        total.setText("Rp -");
//        bayar.setText("Rp -");
//        kembalian.setText("Rp -");
//        autoNoNota();
//    }

    private void simpanPembelian() {
        if (keranjang.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Keranjang masih kosong!");
            return;
        }
        if (jComboBox1.getSelectedIndex() <= 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Pilih supplier terlebih dahulu!");
            return;
        }
 
        String selectedSupplier = (String) jComboBox1.getSelectedItem();
        if (selectedSupplier == null || !selectedSupplier.contains(" | ")) {
            javax.swing.JOptionPane.showMessageDialog(this, "Supplier tidak valid, silakan pilih ulang!");
            return;
        }
        String idSupplier = selectedSupplier.split(" \\| ")[0].trim();
        if (idSupplier.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Supplier tidak valid, silakan pilih ulang!");
            return;
        }
 
        double totalVal = hitungTotal();
        double bayarVal = parseBayar();
 
        if (bayarVal < 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Uang bayar harus berupa angka yang valid!");
            return;
        }
        if (bayarVal < totalVal) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Uang bayar (Rp " + String.format("%,.0f", bayarVal) +
                ") kurang dari total (Rp " + String.format("%,.0f", totalVal) + ")!");
            return;
        }
 
        double kembalianVal = bayarVal - totalVal;
 
        String idPembelian = jTextField1.getText();
        String noPO        = jTextField2.getText();
        String tanggal      = LocalDate.now().toString();
        String keterangan = keteranganFinal;
 
        try {
            conn.setAutoCommit(false);
 
            // Insert pembelian (sekarang termasuk Bayar dan Kembalian)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pembelian (ID_Pembelian, No_PO, Tanggal_Pembelian, " +
                    "Total, Bayar, Kembalian, Keterangan, ID_Pegawai, ID_Supplier) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, idPembelian);
                ps.setString(2, noPO);
                ps.setString(3, tanggal);
                ps.setDouble(4, totalVal);
                ps.setDouble(5, bayarVal);
                ps.setDouble(6, kembalianVal);
                ps.setString(7, keterangan.isEmpty() ? "-" : keterangan);
                ps.setString(8, "PGW001"); // TODO: ganti dengan ID pegawai login
                ps.setString(9, idSupplier);
                ps.executeUpdate();
            }
 
            // Insert detail_pembelian & tambah stok
            for (Object[] item : keranjang) {
                String idBarang   = (String) item[0];
                double hargaBeli  = (double) item[2];
                int qtyCnt        = (int)    item[3];
                double sub        = (double) item[4];
 
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "INSERT INTO detail_pembelian (ID_Pembelian, ID_Barang, Qty, " +
                        "Harga_Beli, Subtotal) VALUES (?,?,?,?,?)")) {
                    ps2.setString(1, idPembelian);
                    ps2.setString(2, idBarang);
                    ps2.setInt(3, qtyCnt);
                    ps2.setDouble(4, hargaBeli);
                    ps2.setDouble(5, sub);
                    ps2.executeUpdate();
                }
 
                // TAMBAH stok (bukan kurangi, karena ini pembelian/restok)
                try (PreparedStatement psUpdate = conn.prepareStatement(
                        "UPDATE barang SET Stok = Stok + ? WHERE ID_Barang = ?")) {
                    psUpdate.setInt(1, qtyCnt);
                    psUpdate.setString(2, idBarang);
                    int affected = psUpdate.executeUpdate();
                    if (affected == 0) {
                        throw new SQLException("Barang dengan ID " + idBarang + " tidak ditemukan saat update stok.");
                    }
                }
            }
 
            conn.commit();
 
            javax.swing.JOptionPane.showMessageDialog(this, "Pembelian berhasil disimpan!");
            cetakPDF(idPembelian);
            resetSemua();
 
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal simpan, transaksi dibatalkan: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException autoCommitEx) {
                autoCommitEx.printStackTrace();
            }
        }
    }
    
    
    private void cetakPDF(String idPembelian) {
        try {
            String path = "C:\\Users\\marko\\OneDrive\\Documents\\NetBeansProjects\\appSparePart\\src\\main\\java\\navbar\\LaporanPembelian.jrxml";
 
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "File tidak ditemukan:\n" + file.getAbsolutePath());
                return;
            }
 
            JasperReport report = JasperCompileManager.compileReport(path);
 
            Map<String, Object> params = new HashMap<>();
            params.put("ID_Pembelian", idPembelian);
 
            JasperPrint print = JasperFillManager.fillReport(report, params, conn);
            JasperViewer.viewReport(print, false);
 
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                "Gagal cetak:\n" + e.getClass().getName() + "\n" + e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        navbar = new javax.swing.JPanel();
        divNavbar = new javax.swing.JPanel();
        nav9 = new javax.swing.JLabel();
        nav10 = new javax.swing.JLabel();
        nav11 = new javax.swing.JLabel();
        nav12 = new javax.swing.JLabel();
        nav13 = new javax.swing.JLabel();
        layout = new javax.swing.JPanel();
        mainContain = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        nav14 = new javax.swing.JLabel();
        nav15 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        nav16 = new javax.swing.JLabel();
        nav17 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        nav18 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        nav19 = new javax.swing.JLabel();
        nav20 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        nav21 = new javax.swing.JLabel();
        nav22 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        nav24 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        nav23 = new javax.swing.JLabel();
        nav27 = new javax.swing.JLabel();
        nav28 = new javax.swing.JLabel();
        nav29 = new javax.swing.JLabel();
        cetakTransaksi = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        labelTotal = new javax.swing.JLabel();
        kembalian = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        inputBayar = new javax.swing.JTextField();

        setMaximumSize(new java.awt.Dimension(780, 540));
        setMinimumSize(new java.awt.Dimension(780, 540));
        setLayout(new java.awt.BorderLayout());

        navbar.setBackground(new java.awt.Color(11, 11, 101));
        navbar.setMaximumSize(new java.awt.Dimension(780, 50));
        navbar.setMinimumSize(new java.awt.Dimension(780, 50));
        navbar.setPreferredSize(new java.awt.Dimension(780, 50));

        divNavbar.setOpaque(false);

        nav9.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        nav9.setForeground(new java.awt.Color(255, 255, 255));
        nav9.setText("Form Pembelian");

        nav10.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav10.setForeground(new java.awt.Color(255, 255, 255));
        nav10.setText(">");

        nav11.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav11.setForeground(new java.awt.Color(255, 255, 255));
        nav11.setText("Transaksi");

        nav12.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav12.setForeground(new java.awt.Color(255, 255, 255));
        nav12.setText(">");

        nav13.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav13.setForeground(new java.awt.Color(255, 255, 255));
        nav13.setText("Pembelian");

        javax.swing.GroupLayout divNavbarLayout = new javax.swing.GroupLayout(divNavbar);
        divNavbar.setLayout(divNavbarLayout);
        divNavbarLayout.setHorizontalGroup(
            divNavbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(divNavbarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nav9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nav10, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nav11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nav12, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nav13)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        divNavbarLayout.setVerticalGroup(
            divNavbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(divNavbarLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(divNavbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nav9, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(nav10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(nav11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(nav12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(nav13))
                .addContainerGap())
        );

        javax.swing.GroupLayout navbarLayout = new javax.swing.GroupLayout(navbar);
        navbar.setLayout(navbarLayout);
        navbarLayout.setHorizontalGroup(
            navbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(navbarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(divNavbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(501, Short.MAX_VALUE))
        );
        navbarLayout.setVerticalGroup(
            navbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(navbarLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(divNavbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(5, 5, 5))
        );

        add(navbar, java.awt.BorderLayout.PAGE_START);

        layout.setBackground(new java.awt.Color(204, 204, 255));
        layout.setMaximumSize(new java.awt.Dimension(780, 540));
        layout.setMinimumSize(new java.awt.Dimension(780, 540));

        mainContain.setBackground(new java.awt.Color(204, 204, 255));
        mainContain.setMaximumSize(new java.awt.Dimension(695, 310));
        mainContain.setMinimumSize(new java.awt.Dimension(695, 310));
        mainContain.setPreferredSize(new java.awt.Dimension(695, 310));

        jPanel1.setMaximumSize(new java.awt.Dimension(330, 0));
        jPanel1.setMinimumSize(new java.awt.Dimension(330, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(330, 0));

        nav14.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        nav14.setText("Input Barang");

        nav15.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav15.setText("Id. Pembelian");

        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        jTextField2.addActionListener(this::jTextField2ActionPerformed);

        nav16.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav16.setText("No. PO");

        nav17.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav17.setText("Tanggal Pembelian ");

        jTextField3.addActionListener(this::jTextField3ActionPerformed);

        nav18.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav18.setText("Cari kode / nama barang");

        jTextField4.addActionListener(this::jTextField4ActionPerformed);

        jTextField5.addActionListener(this::jTextField5ActionPerformed);

        nav19.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav19.setText("Nama barang ");

        nav20.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav20.setText("Harga Beli");

        jTextField6.addActionListener(this::jTextField6ActionPerformed);

        jTextField7.addActionListener(this::jTextField7ActionPerformed);

        nav21.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav21.setText("Qty");

        nav22.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav22.setText("Keterangan ");

        jTextField8.addActionListener(this::jTextField8ActionPerformed);

        jButton1.setBackground(new java.awt.Color(255, 51, 51));
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Reset");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setBackground(new java.awt.Color(51, 204, 0));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setText("+ Tambah Ke Keranjang");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        nav24.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav24.setText("Supplier");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(nav14)
                        .addGap(129, 129, 129))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nav18)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(nav22)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTextField8, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField4, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav15))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(nav16)
                                        .addGap(124, 124, 124))
                                    .addComponent(jTextField2)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav17))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(nav19)
                                        .addGap(83, 83, 83))
                                    .addComponent(jTextField5)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav20)
                                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(36, 36, 36)
                                        .addComponent(nav21))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(28, 28, 28)
                                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(26, 26, 26)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav24)
                                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(23, 23, 23))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jButton1)
                .addGap(35, 35, 35)
                .addComponent(jButton2)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(nav14)
                .addGap(34, 34, 34)
                .addComponent(nav18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(nav15)
                                    .addGap(0, 0, 0)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(nav16)
                                    .addGap(22, 22, 22)))
                            .addComponent(jTextField2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(nav19)
                                    .addGap(22, 22, 22))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(nav17)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jTextField5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(nav20)
                            .addComponent(nav24)
                            .addComponent(nav21))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jComboBox1)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(nav22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(36, 36, 36))
        );

        jPanel2.setMaximumSize(new java.awt.Dimension(330, 0));
        jPanel2.setMinimumSize(new java.awt.Dimension(330, 0));

        nav23.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        nav23.setText("Keranjang Belanja");

        nav27.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav27.setText("Total");

        nav28.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav28.setText("Uang Bayar");

        nav29.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav29.setText("Kembalian");

        cetakTransaksi.setBackground(new java.awt.Color(51, 204, 0));
        cetakTransaksi.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        cetakTransaksi.setForeground(new java.awt.Color(255, 255, 255));
        cetakTransaksi.setText("Cetak Transaksi");
        cetakTransaksi.addActionListener(this::cetakTransaksiActionPerformed);

        jButton4.setBackground(new java.awt.Color(255, 51, 51));
        jButton4.setForeground(new java.awt.Color(255, 255, 255));
        jButton4.setText("Batal");
        jButton4.addActionListener(this::jButton4ActionPerformed);

        labelTotal.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        labelTotal.setText("Rp -");

        kembalian.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        kembalian.setText("Rp -");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Nama Barang", "Qty", "Subtotal"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTable1);

        inputBayar.setText("-");
        inputBayar.addActionListener(this::inputBayarActionPerformed);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(107, 107, 107)
                        .addComponent(nav23))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav27)
                                    .addComponent(labelTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav29)
                                    .addComponent(kembalian, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(40, 40, 40)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav28)
                                    .addComponent(inputBayar)))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(43, 43, 43)
                                .addComponent(jButton4)
                                .addGap(40, 40, 40)
                                .addComponent(cetakTransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(nav23)
                .addGap(34, 34, 34)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nav27)
                    .addComponent(nav28))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(labelTotal)
                        .addGap(18, 18, 18)
                        .addComponent(nav29)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(kembalian))
                    .addComponent(inputBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cetakTransaksi)
                    .addComponent(jButton4))
                .addGap(27, 27, 27))
        );

        javax.swing.GroupLayout mainContainLayout = new javax.swing.GroupLayout(mainContain);
        mainContain.setLayout(mainContainLayout);
        mainContainLayout.setHorizontalGroup(
            mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainContainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        mainContainLayout.setVerticalGroup(
            mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainContainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layoutLayout = new javax.swing.GroupLayout(layout);
        layout.setLayout(layoutLayout);
        layoutLayout.setHorizontalGroup(
            layoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layoutLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(mainContain, javax.swing.GroupLayout.PREFERRED_SIZE, 750, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );
        layoutLayout.setVerticalGroup(
            layoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layoutLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(mainContain, javax.swing.GroupLayout.PREFERRED_SIZE, 460, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(65, Short.MAX_VALUE))
        );

        add(layout, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        cariBarang();
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField5ActionPerformed

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField7ActionPerformed

    private void jTextField8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField8ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField8ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        resetInputBarang();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        tambahKeKeranjang();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void cetakTransaksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cetakTransaksiActionPerformed
        simpanPembelian();
    }//GEN-LAST:event_cetakTransaksiActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed

        resetSemua();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void inputBayarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputBayarActionPerformed
        hitungKembalian();
    }//GEN-LAST:event_inputBayarActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cetakTransaksi;
    private javax.swing.JPanel divNavbar;
    private javax.swing.JTextField inputBayar;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JLabel kembalian;
    private javax.swing.JLabel labelTotal;
    private javax.swing.JPanel layout;
    private javax.swing.JPanel mainContain;
    private javax.swing.JLabel nav10;
    private javax.swing.JLabel nav11;
    private javax.swing.JLabel nav12;
    private javax.swing.JLabel nav13;
    private javax.swing.JLabel nav14;
    private javax.swing.JLabel nav15;
    private javax.swing.JLabel nav16;
    private javax.swing.JLabel nav17;
    private javax.swing.JLabel nav18;
    private javax.swing.JLabel nav19;
    private javax.swing.JLabel nav20;
    private javax.swing.JLabel nav21;
    private javax.swing.JLabel nav22;
    private javax.swing.JLabel nav23;
    private javax.swing.JLabel nav24;
    private javax.swing.JLabel nav27;
    private javax.swing.JLabel nav28;
    private javax.swing.JLabel nav29;
    private javax.swing.JLabel nav9;
    private javax.swing.JPanel navbar;
    // End of variables declaration//GEN-END:variables
}
