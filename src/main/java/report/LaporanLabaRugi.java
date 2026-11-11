/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package report;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import koneksi.koneksi;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author ACER
 */
public class LaporanLabaRugi {

    public LaporanLabaRugi() {
        // Tampilkan dialog pilih bulan & tahun dulu
        int[] periode = tampilkanDialogPeriode();
        if (periode == null) return; // user klik Cancel

        int bulan = periode[0];
        int tahun = periode[1];

        cetakLaporan(bulan, tahun);
    }

    private int[] tampilkanDialogPeriode() {
        // ComboBox Bulan
        String[] namaBulan = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        JComboBox<String> comboBulan = new JComboBox<>(namaBulan);

        // Spinner Tahun — default tahun sekarang
        int tahunSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        JSpinner spinnerTahun = new JSpinner(new SpinnerNumberModel(tahunSekarang, 2000, 2100, 1));
        spinnerTahun.setEditor(new JSpinner.NumberEditor(spinnerTahun, "#"));

        // Set default bulan sekarang
        int bulanSekarang = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH);
        comboBulan.setSelectedIndex(bulanSekarang);

        // Panel dialog
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Bulan :"));
        panel.add(comboBulan);
        panel.add(new JLabel("Tahun :"));
        panel.add(spinnerTahun);

        int result = JOptionPane.showConfirmDialog(
            null, panel,
            "Pilih Periode Laporan",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return null;

        int bulan = comboBulan.getSelectedIndex() + 1;
        int tahun = (int) spinnerTahun.getValue();
        return new int[]{bulan, tahun};
    }

    private void cetakLaporan(int bulan, int tahun) {
        try {
            Connection conn = koneksi.getConnection();

            // ── Total Pendapatan ──
            double totalPendapatan = 0;
            ResultSet rs1 = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(Total), 0) FROM penjualan " +
                "WHERE MONTH(Tanggal_Penjualan) = " + bulan +
                "  AND YEAR(Tanggal_Penjualan)  = " + tahun);
            if (rs1.next()) totalPendapatan = rs1.getDouble(1);

            // ── Total HPP ──
            double totalHPP = 0;
            ResultSet rs2 = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(dp.Qty * b.Harga_Beli), 0) " +
                "FROM detail_penjualan dp " +
                "JOIN penjualan pj ON dp.ID_Penjualan = pj.ID_Penjualan " +
                "JOIN barang b     ON dp.ID_Barang    = b.ID_Barang " +
                "WHERE MONTH(pj.Tanggal_Penjualan) = " + bulan +
                "  AND YEAR(pj.Tanggal_Penjualan)  = " + tahun);
            if (rs2.next()) totalHPP = rs2.getDouble(1);

            // ── Total Pembelian ──
            double totalPembelian = 0;
            ResultSet rs3 = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(Total), 0) FROM pembelian " +
                "WHERE MONTH(Tanggal_Pembelian) = " + bulan +
                "  AND YEAR(Tanggal_Pembelian)  = " + tahun);
            if (rs3.next()) totalPembelian = rs3.getDouble(1);

            // ── Jumlah Transaksi Jual ──
            int jmlJual = 0;
            ResultSet rs4 = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM penjualan " +
                "WHERE MONTH(Tanggal_Penjualan) = " + bulan +
                "  AND YEAR(Tanggal_Penjualan)  = " + tahun);
            if (rs4.next()) jmlJual = rs4.getInt(1);

            // ── Jumlah Transaksi Beli ──
            int jmlBeli = 0;
            ResultSet rs5 = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM pembelian " +
                "WHERE MONTH(Tanggal_Pembelian) = " + bulan +
                "  AND YEAR(Tanggal_Pembelian)  = " + tahun);
            if (rs5.next()) jmlBeli = rs5.getInt(1);

            // ── Parameter ──
            Map<String, Object> param = new HashMap<>();
            param.put("BULAN",              bulan);
            param.put("TAHUN",              tahun);
            param.put("TOTAL_PENDAPATAN",   totalPendapatan);
            param.put("TOTAL_HPP",          totalHPP);
            param.put("TOTAL_PEMBELIAN",    totalPembelian);
            param.put("JML_TRANSAKSI_JUAL", jmlJual);
            param.put("JML_TRANSAKSI_BELI", jmlBeli);

            // ── Compile & Tampilkan ──
            String path = getClass().getResource("/report/LaporanLabaRugi.jrxml").getPath();
            JasperReport jr = JasperCompileManager.compileReport(path);
            JasperPrint  jp = JasperFillManager.fillReport(jr, param, conn);
            JasperViewer.viewReport(jp, false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal membuka laporan:\n" + e.getMessage());
        }
    }
}
