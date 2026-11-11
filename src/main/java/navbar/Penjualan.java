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
public class Penjualan extends javax.swing.JPanel {

    Connection conn;
    String idBarangDipilih = "";
    ArrayList<Object[]> keranjang = new ArrayList<>();
    
    
    
    /**
     * Creates new form Penjualan
     */
    public Penjualan() {
        initComponents();
        conn = koneksi.getConnection();
        autoNoNota();
        jTextField2.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        jTextField2.setEditable(false);
        jTextField1.setEditable(false);
        jTextField5.setEditable(false);
        jTextField6.setEditable(false);
        setupJTable3();
    }
    
    private void setupJTable3() {
        jTable3.setModel(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Nama Barang", "Qty", "Subtotal"}
        ) {
            Class[] types = {String.class, Integer.class, String.class};
            public Class getColumnClass(int i) { return types[i]; }
            public boolean isCellEditable(int r, int c) { return false; }
        });
    }
    

    // Auto-generate No. Nota
    private void autoNoNota() {
        try {
            String tanggal = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT No_Nota FROM penjualan WHERE No_Nota LIKE 'NJ-" + tanggal + "%' " +
                "ORDER BY No_Nota DESC LIMIT 1");
            int urut = 1;
            if (rs.next()) {
                String last = rs.getString("No_Nota");
                urut = Integer.parseInt(last.substring(last.length() - 3)) + 1;
            }
            jTextField1.setText(String.format("NJ-%s%03d", tanggal, urut));
        } catch (Exception e) {
            jTextField1.setText("NJ-001");
        }
    }
    
    private void pilihBarang(String[] data) {
        idBarangDipilih = data[0];
        jTextField5.setText(data[2]);
        jTextField6.setText(data[3]);
    }

    // Cari barang saat ketik di jTextField4
    private void cariBarang() {
        String keyword = jTextField4.getText().trim();
        if (keyword.isEmpty()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT ID_Barang, Kode_Barang, Nama_Barang, Harga_Jual " +
                "FROM barang " +
                "WHERE (Kode_Barang LIKE ? OR Nama_Barang LIKE ?) AND Stok > 0");
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            java.util.List<String[]> hasil = new java.util.ArrayList<>();
            while (rs.next()) {
                hasil.add(new String[]{
                    rs.getString("ID_Barang"),
                    rs.getString("Kode_Barang"),
                    rs.getString("Nama_Barang"),
                    String.valueOf(rs.getDouble("Harga_Jual"))
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
                // Popup pilih barang jika hasil lebih dari 1
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
    private void tambahKeKeranjang() {
        if (idBarangDipilih.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Pilih barang terlebih dahulu!");
            return;
        }
        try {
            String namaBarang = jTextField5.getText();
            double hargaJual  = Double.parseDouble(jTextField6.getText());
            int qtyCnt        = Integer.parseInt(jTextField7.getText().trim());
            double diskon     = Double.parseDouble(jTextField8.getText().trim());
            double sub        = (hargaJual * qtyCnt) - diskon;

            // Cek stok
            PreparedStatement psStok = conn.prepareStatement(
                "SELECT Stok FROM barang WHERE ID_Barang = ?");
            psStok.setString(1, idBarangDipilih);
            ResultSet rs = psStok.executeQuery();
            rs.next();
            if (rs.getInt("Stok") < qtyCnt) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Stok tidak cukup! Stok tersisa: " + rs.getInt("Stok"));
                return;
            }

            // Simpan ke list keranjang
            keranjang.add(new Object[]{idBarangDipilih, namaBarang, hargaJual, qtyCnt, diskon, sub});

            // Tampilkan di jTable3
            DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
            model.addRow(new Object[]{
                namaBarang,
                qtyCnt,
                "Rp " + String.format("%,.0f", sub)
            });

            updateTotal();
            resetInputBarang();

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Qty dan Diskon harus berupa angka!");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // Hitung total keranjang
    private double hitungTotal() {
        double total = 0;
        for (Object[] item : keranjang) total += (double) item[5];
        return total;
    }

    private void updateTotal() {
        total.setText("Rp " + String.format("%,.0f", hitungTotal()));
    }

    private void hitungKembalian(double bayarVal) {
        double kem = bayarVal - hitungTotal();
        kembalian.setText("Rp " + String.format("%,.0f", kem));
    }

    // Reset input barang saja
    private void resetInputBarang() {
        idBarangDipilih = "";
        jTextField4.setText("");
        jTextField5.setText("");
        jTextField6.setText("0");
        jTextField7.setText("1");
        jTextField8.setText("0");
    }

    // Reset semua (batal)
    private void resetSemua() {
        keranjang.clear();
        ((DefaultTableModel) jTable3.getModel()).setRowCount(0);
        resetInputBarang();
        total.setText("Rp -");
        bayar.setText("Rp -");
        kembalian.setText("Rp -");
        autoNoNota();
    }

    // Simpan transaksi ke database
    private void simpanTransaksi() {
        if (keranjang.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Keranjang masih kosong!");
            return;
        }
        try {
            // Input uang bayar
            String inputBayar = javax.swing.JOptionPane.showInputDialog(
                this, "Masukkan uang bayar (Rp):", "Uang Bayar",
                javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (inputBayar == null || inputBayar.trim().isEmpty()) return;

            double bayarVal = Double.parseDouble(inputBayar.trim().replace(",", ""));
            double totalVal = hitungTotal();

            if (bayarVal < totalVal) {
                javax.swing.JOptionPane.showMessageDialog(this, "Uang bayar kurang!");
                return;
            }

            double kembalianVal = bayarVal - totalVal;

            // Update label
            bayar.setText("Rp " + String.format("%,.0f", bayarVal));
            hitungKembalian(bayarVal);

            // Auto ID_Penjualan
            ResultSet rsCount = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM penjualan");
            rsCount.next();
            String idPenjualan = String.format("PJL%03d", rsCount.getInt(1) + 1);

            String noNota  = jTextField1.getText();
            String tanggal = LocalDate.now().toString();
            String pembeli = jTextField3.getText().trim().isEmpty()
                ? "Umum" : jTextField3.getText().trim();

            // Insert penjualan
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO penjualan (ID_Penjualan, No_Nota, Tanggal_Penjualan, " +
                "Nama_Pembeli, Total, Bayar, Kembalian, ID_Pegawai) VALUES (?,?,?,?,?,?,?,?)");
            ps.setString(1, idPenjualan);
            ps.setString(2, noNota);
            ps.setString(3, tanggal);
            ps.setString(4, pembeli);
            ps.setDouble(5, totalVal);
            ps.setDouble(6, bayarVal);
            ps.setDouble(7, kembalianVal);
            ps.setString(8, "PGW001"); // ganti dengan ID pegawai login
            ps.executeUpdate();

            // Insert detail_penjualan & kurangi stok
            for (Object[] item : keranjang) {
                String idBarang  = (String) item[0];
                double hargaJual = (double) item[2];
                int qtyCnt       = (int)    item[3];
                double diskon    = (double) item[4];
                double sub       = (double) item[5];

                PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO detail_penjualan (ID_Penjualan, ID_Barang, Qty, " +
                    "Harga_Jual, Diskon, Subtotal) VALUES (?,?,?,?,?,?)");
                ps2.setString(1, idPenjualan);
                ps2.setString(2, idBarang);
                ps2.setInt(3, qtyCnt);
                ps2.setDouble(4, hargaJual);
                ps2.setDouble(5, diskon);
                ps2.setDouble(6, sub);
                ps2.executeUpdate();

                PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE barang SET Stok = Stok - ? WHERE ID_Barang = ?");
                psUpdate.setInt(1, qtyCnt);
                psUpdate.setString(2, idBarang);
                psUpdate.executeUpdate();
            }

            javax.swing.JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan!");
            cetakPDF(idPenjualan); // ← tambah ini
            resetSemua();

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Input uang bayar tidak valid!");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
        }
    }
    
    private void cetakPDF(String idPenjualan) {
        try {
            String path = "C:\\Users\\marko\\OneDrive\\Documents\\NetBeansProjects\\appSparePart\\src\\main\\java\\navbar\\LaporanTransaksi.jrxml";

            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "File tidak ditemukan:\n" + file.getAbsolutePath());
                return;
            }

            JasperReport report = JasperCompileManager.compileReport(path);

            Map<String, Object> params = new HashMap<>();
            params.put("ID_Penjualan", idPenjualan);

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
        jPanel2 = new javax.swing.JPanel();
        nav23 = new javax.swing.JLabel();
        nav27 = new javax.swing.JLabel();
        nav28 = new javax.swing.JLabel();
        nav29 = new javax.swing.JLabel();
        cetakTransaksi = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        total = new javax.swing.JLabel();
        kembalian = new javax.swing.JLabel();
        bayar = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();

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
        nav9.setText("Form Penjualan");

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
        nav13.setText("Penjualan");

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
                .addContainerGap(510, Short.MAX_VALUE))
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
        nav15.setText("No. Nota");

        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        jTextField2.addActionListener(this::jTextField2ActionPerformed);

        nav16.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav16.setText("Tanggal");

        nav17.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav17.setText("Pembeli (ketik bebas / pilih)");

        jTextField3.addActionListener(this::jTextField3ActionPerformed);

        nav18.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav18.setText("Cari kode / nama barang");

        jTextField4.addActionListener(this::jTextField4ActionPerformed);

        jTextField5.addActionListener(this::jTextField5ActionPerformed);

        nav19.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav19.setText("Nama barang ");

        nav20.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav20.setText("Harga Jual");

        jTextField6.addActionListener(this::jTextField6ActionPerformed);

        jTextField7.addActionListener(this::jTextField7ActionPerformed);

        nav21.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav21.setText("Qty");

        nav22.setFont(new java.awt.Font("Baskerville Old Face", 0, 12)); // NOI18N
        nav22.setText("Diskon (Rp) ");

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(35, 35, 35)
                .addComponent(jButton2)
                .addGap(21, 21, 21))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(nav14)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nav17))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nav19)))
                    .addComponent(jTextField4)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav20)
                                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav21)
                                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(53, 53, 53)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav22)
                                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav15))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav16))))
                        .addContainerGap())))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nav18)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(nav14)
                .addGap(61, 61, 61)
                .addComponent(nav18)
                .addGap(0, 0, 0)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nav15)
                        .addGap(0, 0, 0)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(nav16))
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(12, 12, 12)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(nav19))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nav17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(26, 26, 26)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nav20)
                        .addGap(0, 0, 0)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(nav21)
                        .addGap(0, 0, 0)
                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(nav22)
                        .addGap(0, 0, 0)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(57, 57, 57))
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

        total.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        total.setText("Rp -");

        kembalian.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        kembalian.setText("Rp -");

        bayar.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        bayar.setText("Rp -");

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane3.setViewportView(jTable3);

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
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nav28)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(bayar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jButton4, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav27, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(total, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
                                .addGap(40, 40, 40)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cetakTransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav29)
                                    .addComponent(kembalian, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(nav23)
                .addGap(34, 34, 34)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(nav28)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bayar)
                        .addGap(18, 18, 18)
                        .addComponent(nav27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(total))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(nav29)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(kembalian)))
                .addGap(53, 53, 53)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cetakTransaksi)
                    .addComponent(jButton4))
                .addGap(52, 52, 52))
        );

        javax.swing.GroupLayout mainContainLayout = new javax.swing.GroupLayout(mainContain);
        mainContain.setLayout(mainContainLayout);
        mainContainLayout.setHorizontalGroup(
            mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainContainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                .addGap(18, 18, 18)
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
        simpanTransaksi();
    }//GEN-LAST:event_cetakTransaksiActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed

        resetSemua();
    }//GEN-LAST:event_jButton4ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bayar;
    private javax.swing.JButton cetakTransaksi;
    private javax.swing.JPanel divNavbar;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JLabel kembalian;
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
    private javax.swing.JLabel nav27;
    private javax.swing.JLabel nav28;
    private javax.swing.JLabel nav29;
    private javax.swing.JLabel nav9;
    private javax.swing.JPanel navbar;
    private javax.swing.JLabel total;
    // End of variables declaration//GEN-END:variables
}
