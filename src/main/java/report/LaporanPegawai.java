/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package report;

import koneksi.koneksi;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
/**
 *
 * @author ACER
 */
public class LaporanPegawai extends JPanel {

    public LaporanPegawai() {
        cetakLaporan();
    }

    private void cetakLaporan() {
        try {
            // Path ke file jrxml
            String path = getClass().getResource("/report/LaporanPegawai.jrxml").getPath();

            // Compile jrxml
            JasperReport jr = JasperCompileManager.compileReport(path);

            // Parameter (kosong dulu, bisa ditambah nanti)
            Map<String, Object> param = new HashMap<>();

            // Koneksi database
            Connection conn = koneksi.getConnection(); // sesuaikan

            // Fill report dengan data
            JasperPrint jp = JasperFillManager.fillReport(jr, param, conn);

            // Tampilkan viewer
            JasperViewer.viewReport(jp, false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal membuka laporan:\n" + e.getMessage());
        }
    }
}
