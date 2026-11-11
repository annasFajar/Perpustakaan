/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package report;
 
import koneksi.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
/**
 *
 * @author ACER
 */
public class LaporanBulanan {
    
    public LaporanBulanan() {
        // Tampilkan dialog pilih bulan & tahun dulu
        int[] periode = tampilkanDialogPeriode();
        if (periode == null) return; // user klik Cancel
 
        int bulan = periode[0];
        int tahun = periode[1];
 
        cetakLaporan(bulan, tahun);
    }

    private int[] tampilkanDialogPeriode() {
        String[] namaBulan = {
            "Januari", "Februari", "Maret", "April",
            "Mei", "Juni", "Juli", "Agustus",
            "September", "Oktober", "November", "Desember"
        };
 
        // Isi tahun: 5 tahun ke belakang s.d. tahun ini
        int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String[] daftarTahun = new String[6];
        for (int i = 0; i < 6; i++) {
            daftarTahun[i] = String.valueOf(tahunSekarang - i);
        }
 
        JComboBox<String> cbBulan = new JComboBox<>(namaBulan);
        JComboBox<String> cbTahun = new JComboBox<>(daftarTahun);
 
        // Set default: bulan & tahun berjalan
        int bulanSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH); // 0-based
        cbBulan.setSelectedIndex(bulanSekarang);
        cbTahun.setSelectedIndex(0);
 
        JPanel panel = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Bulan  :"));
        panel.add(cbBulan);
        panel.add(new JLabel("Tahun  :"));
        panel.add(cbTahun);
 
        int hasil = JOptionPane.showConfirmDialog(
            null, panel,
            "Pilih Periode Laporan",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
 
        if (hasil != JOptionPane.OK_OPTION) return null;
 
        int bulan = cbBulan.getSelectedIndex() + 1; // 1-12
        int tahun = Integer.parseInt((String) cbTahun.getSelectedItem());
        return new int[]{bulan, tahun};
    }
    
    /**
     * Query total pembelian bulan tersebut dari DB,
     * lalu isi sebagai parameter ke JasperReports.
     *
     * Tanggal_Pembelian di DB bertipe DATETIME,
     * sehingga filter menggunakan MONTH() dan YEAR().
     */
    private void cetakLaporan(int bulan, int tahun) {
        try {
            Connection conn = koneksi.getConnection();
 
            // ── Hitung total pembelian bulan ini ──
            double totalPembelian = 0.0;
            String sqlPembelian =
                "SELECT COALESCE(SUM(Total), 0) AS TotalBeli " +
                "FROM pembelian " +
                "WHERE MONTH(Tanggal_Pembelian) = ? " +
                "  AND YEAR(Tanggal_Pembelian)  = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlPembelian)) {
                ps.setInt(1, bulan);
                ps.setInt(2, tahun);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalPembelian = rs.getDouble("TotalBeli");
                }
            }
 
            // ── Compile JRXML ──
            String path = getClass().getResource("/report/LaporanBulanan.jrxml").getPath();
            JasperReport jr = JasperCompileManager.compileReport(path);
 
            // ── Parameter ke laporan ──
            Map<String, Object> param = new HashMap<>();
            param.put("BULAN",           bulan);
            param.put("TAHUN",           tahun);
            param.put("TOTAL_PEMBELIAN", totalPembelian);
 
            // ── Fill & tampilkan ──
            JasperPrint jp = JasperFillManager.fillReport(jr, param, conn);
            JasperViewer.viewReport(jp, false);
 
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Gagal membuka laporan:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
