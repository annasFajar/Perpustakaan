/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package report;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
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
public class LaporanStokKritis {

    public LaporanStokKritis() {
        cetakLaporan();
    }

    private void cetakLaporan() {
        try {
            // Path ke file jrxml
            String path = getClass().getResource("/report/LaporanStokKritis.jrxml").getPath();

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
