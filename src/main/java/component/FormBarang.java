/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package component;

import java.sql.*;
import koneksi.koneksi;
/**
 *
 * @author ACER
 */
public class FormBarang extends javax.swing.JPanel {
    Connection conn;
    /**
     * Creates new form formBarang
     */
    public FormBarang() {
        initComponents();
        conn = koneksi.getConnection();
        loadKategori();
        loadSupplier();
        autoKodeBarang();
    }
// Load dropdown Kategori
    private void loadKategori() {
        try {
            jComboBox1.removeAllItems();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT ID_Kategori, Nama_Kategori FROM kategori ORDER BY Nama_Kategori");
            while (rs.next()) {
                jComboBox1.addItem(rs.getString("ID_Kategori") + " - " + rs.getString("Nama_Kategori"));
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal load kategori: " + e.getMessage());
        }
    }

    // Load dropdown Supplier
    private void loadSupplier() {
        try {
            jComboBox2.removeAllItems();
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT ID_Supplier, Nama_Supplier FROM supplier ORDER BY Nama_Supplier");
            while (rs.next()) {
                jComboBox2.addItem(rs.getString("ID_Supplier") + " - " + rs.getString("Nama_Supplier"));
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal load supplier: " + e.getMessage());
        }
    }

    // Auto-generate Kode Barang
    private void autoKodeBarang() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT MAX(CAST(SUBSTRING(ID_Barang, 4) AS UNSIGNED)) AS maxAngka FROM barang");
            int angka = 1;
            if (rs.next() && rs.getString("maxAngka") != null) {
                angka = rs.getInt("maxAngka") + 1;
            }
            jTextField1.setText(String.format("BRG%03d", angka));
            jTextField1.setEditable(false);
        } catch (Exception e) {
            jTextField1.setText("BRG001");
        }
    }
    
    private void clearForm() {
        // jTextField1 (Kode Barang) di-generate otomatis, bukan dikosongkan
        autoKodeBarang();
        jTextField2.setText(""); // Nama Barang
        jTextField3.setText(""); // Merk
        jTextField4.setText(""); // Satuan
        jTextField5.setText(""); // Harga Beli
        jTextField6.setText(""); // Harga Jual
        jTextField7.setText(""); // Stok
        jTextField8.setText(""); // Stok Minimum
        jComboBox1.setSelectedIndex(0); // Kategori
        jComboBox2.setSelectedIndex(0); // Supplier
    }

    // Simpan barang
    private boolean simpanBarang() {
        try {
            String nama     = jTextField2.getText().trim();
            String merk     = jTextField3.getText().trim();
            String satuan   = jTextField4.getText().trim();
            double hargaBeli = Double.parseDouble(jTextField5.getText().trim());
            double hargaJual = Double.parseDouble(jTextField6.getText().trim());
            int stok         = Integer.parseInt(jTextField7.getText().trim());
            int stokMin      = Integer.parseInt(jTextField8.getText().trim());

            String idKategori = jComboBox1.getSelectedItem().toString().split(" - ")[0];
            String idSupplier = jComboBox2.getSelectedItem().toString().split(" - ")[0];

            if (nama.isEmpty() || satuan.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "Nama dan Satuan wajib diisi!");
                return false;
            }

            // Generate ID dan Kode dari MAX yang sudah ada di DB
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT MAX(CAST(SUBSTRING(ID_Barang, 4) AS UNSIGNED)) AS maxAngka FROM barang");
            int angka = 1;
            if (rs.next() && rs.getString("maxAngka") != null) {
                angka = rs.getInt("maxAngka") + 1;
            }
            String idBarang  = String.format("BRG%03d", angka);
            String kodeBarang = idBarang; // ID dan Kode sama

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO barang (ID_Barang, Kode_Barang, Nama_Barang, Merk, Satuan, " +
                "Harga_Beli, Harga_Jual, Stok, Stok_Minimum, Kategori_ID_Kategori, Supplier_ID_Supplier) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, idBarang);
            ps.setString(2, kodeBarang);
            ps.setString(3, nama);
            ps.setString(4, merk);
            ps.setString(5, satuan);
            ps.setDouble(6, hargaBeli);
            ps.setDouble(7, hargaJual);
            ps.setInt(8, stok);
            ps.setInt(9, stokMin);
            ps.setString(10, idKategori);
            ps.setString(11, idSupplier);
            ps.executeUpdate();

            javax.swing.JOptionPane.showMessageDialog(this, "Barang berhasil disimpan!");
            return true;

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Harga, Stok, dan Stok Minimum harus berupa angka!");
            return false;
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
            return false;
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

        mainContain = new javax.swing.JPanel();
        nav14 = new javax.swing.JLabel();
        nav15 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        nav16 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        nav17 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        nav18 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        nav19 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField6 = new javax.swing.JTextField();
        nav20 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        nav21 = new javax.swing.JLabel();
        nav22 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        nav23 = new javax.swing.JLabel();
        nav24 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jComboBox2 = new javax.swing.JComboBox<>();
        jButton3 = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 102, 102));
        setMaximumSize(new java.awt.Dimension(780, 490));
        setMinimumSize(new java.awt.Dimension(780, 490));
        setPreferredSize(new java.awt.Dimension(780, 490));

        mainContain.setBackground(new java.awt.Color(204, 204, 255));
        mainContain.setMaximumSize(new java.awt.Dimension(695, 310));
        mainContain.setMinimumSize(new java.awt.Dimension(695, 310));

        nav14.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        nav14.setText("Tambah Barang Spare Parts");

        nav15.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav15.setText("Kode Barang");

        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        nav16.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav16.setText("Nama Barang");

        nav17.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav17.setText("Merk");

        nav18.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav18.setText("Satuan");

        nav19.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav19.setText("Harga Beli");

        jButton1.setText("Kembali");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setText("Simpan");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        nav20.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav20.setText("Harga Jual");

        nav21.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav21.setText("Stok");

        nav22.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav22.setText("Stok Minimum");

        nav23.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav23.setText("Kategori");

        nav24.setFont(new java.awt.Font("Baskerville Old Face", 0, 14)); // NOI18N
        nav24.setText("Supplier");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(this::jComboBox1ActionPerformed);

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButton3.setText("Reset");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        javax.swing.GroupLayout mainContainLayout = new javax.swing.GroupLayout(mainContain);
        mainContain.setLayout(mainContainLayout);
        mainContainLayout.setHorizontalGroup(
            mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainContainLayout.createSequentialGroup()
                .addGap(195, 195, 195)
                .addComponent(jButton1)
                .addGap(69, 69, 69)
                .addComponent(jButton3)
                .addGap(70, 70, 70)
                .addComponent(jButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(mainContainLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nav19)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainContainLayout.createSequentialGroup()
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav18))
                                .addGap(44, 44, 44)
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField8)
                                    .addComponent(jTextField6)
                                    .addGroup(mainContainLayout.createSequentialGroup()
                                        .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(nav22)
                                            .addComponent(nav20))
                                        .addGap(0, 0, Short.MAX_VALUE))))
                            .addGroup(mainContainLayout.createSequentialGroup()
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav17)
                                    .addComponent(jTextField3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(44, 44, 44)
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainContainLayout.createSequentialGroup()
                                        .addComponent(nav21)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(jTextField7)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainContainLayout.createSequentialGroup()
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nav16))
                                .addGap(44, 44, 44)
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav24)
                                    .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addComponent(nav14)
                            .addGroup(mainContainLayout.createSequentialGroup()
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(nav15)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(44, 44, 44)
                                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jComboBox1, 0, 321, Short.MAX_VALUE)
                                    .addGroup(mainContainLayout.createSequentialGroup()
                                        .addComponent(nav23)
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addGap(63, 63, 63))))
        );
        mainContainLayout.setVerticalGroup(
            mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainContainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nav14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainContainLayout.createSequentialGroup()
                        .addComponent(nav20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                .addGroup(mainContainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addGap(38, 38, 38))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainContain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainContain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        boolean berhasil = simpanBarang();
        if (berhasil) {
            autoKodeBarang();           // generate kode barang baru
            jTextField2.setText("");
            jTextField3.setText("");
            jTextField4.setText("");
            jTextField5.setText("");
            jTextField6.setText("");
            jTextField7.setText("");
            jTextField8.setText("");
        }      
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
 
//        javax.swing.JPanel parent = (javax.swing.JPanel) this.getParent(); // ini adalah layout
//        parent.removeAll();
//
//        // Kembalikan layout ke GroupLayout semula dengan menampilkan ulang Supplier
//        javax.swing.JPanel grandParent = (javax.swing.JPanel) parent.getParent(); // ini Supplier panel
//        grandParent.removeAll();
//        grandParent.add(new navbar.MasterBarang());
//        grandParent.revalidate();
//        grandParent.repaint();
        
        // Naik 3 level: FormBarang → mainContain → layout → NavMasterBarang (parent)
        javax.swing.JPanel mainContain  = (javax.swing.JPanel) this.getParent();
        javax.swing.JPanel layout       = (javax.swing.JPanel) mainContain.getParent();
        javax.swing.JPanel parentPanel  = (javax.swing.JPanel) layout.getParent();

        parentPanel.removeAll();
        parentPanel.add(new navbar.NavMasterBarang());
        parentPanel.revalidate();
        parentPanel.repaint();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        clearForm();
    }//GEN-LAST:event_jButton3ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JPanel mainContain;
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
    // End of variables declaration//GEN-END:variables
}
