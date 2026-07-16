package org.example.primemobile;

import org.apache.poi.ss.usermodel.*;
import java.io.File;

public class CheckExcel {
    public static void main(String[] args) throws Exception {
        Workbook workbook = WorkbookFactory.create(new File("S24U_10_IMEI.xlsx"));
        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 0; i <= Math.min(3, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                System.out.print((cell == null ? "" : cell.toString()) + "\t|\t");
            }
            System.out.println();
        }
        workbook.close();
    }
}
