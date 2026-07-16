package org.example.primemobile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;

class GenerateExcel2 {
    public static void main(String[] args) throws Exception {
        String[] skus = {
            "IP18-HONG-128"
        };

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh Sách IMEI");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("SKU");
        header.createCell(1).setCellValue("IMEI1");
        header.createCell(2).setCellValue("IMEI2");

        int rowNum = 1;
        long baseImei = 359123456789000L;
        
        for (String sku : skus) {
            for (int i = 1; i <= 10; i++) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sku);
                
                String imeiStr1 = String.valueOf(baseImei + rowNum + 200);
                String imeiStr2 = String.valueOf(baseImei + rowNum + 300);
                row.createCell(1).setCellValue(imeiStr1);
                row.createCell(2).setCellValue(imeiStr2);
            }
        }

        try (FileOutputStream out = new FileOutputStream("c:\\\\Users\\\\XalatKoNgon\\\\OneDrive\\\\Documents\\\\GitHub\\\\PrimeMobile\\\\IMEI_IP18-HONG-128.xlsx")) {
            workbook.write(out);
        }
        workbook.close();
        System.out.println("GENERATE_SUCCESS");
    }
}
