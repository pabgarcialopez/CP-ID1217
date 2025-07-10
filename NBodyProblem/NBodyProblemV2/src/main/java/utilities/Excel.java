package utilities;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import space.Coordinate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class Excel {

    private static final String FILE_PATH = "/Users/pablogarcialopez/eclipse-workspace/NBodyProblemV2/results/results2.xlsx";
    private static final String PRECISION = "%.6g";
    private int writeMetadata(Sheet sheet, int row, String tag, Double data) {
        Row metadataRow = sheet.createRow(row++);
        metadataRow.createCell(0).setCellValue(tag);
        metadataRow.createCell(1).setCellValue(data);
        return row;
    }

    public void saveResults(Vector<Vector<Report>> report, int numBodies, int numSteps, int DT, int start, double executionTime, Vector<Double> masses, int numWorkers) {

        System.out.println("Saving results...");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Results");

            // Creating and formatting cell styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFillBackgroundColor(IndexedColors.AQUA.getIndex());
            headerStyle.setFont(headerFont);

            // Writing metadata
            int rowNum = 0;

            rowNum = writeMetadata(sheet, rowNum, "Body number", ((Integer) numBodies).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "Steps end", ((Integer) numSteps).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "DT", ((Integer) DT).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "Start", ((Integer) start).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "Total steps", ((Integer) report.get(0).size()).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "Number of workers", ((Integer) numWorkers).doubleValue());
            rowNum = writeMetadata(sheet, rowNum, "Execution time (s)", executionTime/10e9);

            rowNum++; // Skip a row for spacing

            // Display masses:
            Row columnLabelsRow = sheet.createRow(rowNum++);
            columnLabelsRow.createCell(0).setCellValue("Masses");
            for (int i = 0; i < report.size(); i++) {
                columnLabelsRow.createCell(i + 1).setCellValue("Body " + (i + 1));
                columnLabelsRow.getCell(i + 1).setCellStyle(headerStyle);
            }
            Row massesRow = sheet.createRow(rowNum);
            for (int i = 0; i < report.size(); i++)
                massesRow.createCell(i + 1).setCellValue(masses.get(i));

            // Creating tables
            rowNum++; // Skip a row for spacing
            rowNum = createTable(sheet, report, rowNum, "Positions", headerStyle);
            rowNum++;
            rowNum = createTable(sheet, report, rowNum, "Velocities", headerStyle);
            rowNum++;
            createTable(sheet, report, rowNum, "Forces", headerStyle);

            // Auto-sizing columns
            for (int i = 0; i < report.size() + 5; i++) {
                sheet.autoSizeColumn(i);
            }

            // Writing to file
            try (FileOutputStream fileOut = new FileOutputStream(FILE_PATH)) {
                workbook.write(fileOut);
                System.out.println("Done!");
            }

        } catch(IOException e) {
            System.err.println("ERROR saving results in excel: " + e.getMessage());
        }
    }

    private void writeCell (Coordinate coordinate, Cell cell) {
        double x = coordinate.getX();
        double y = coordinate.getY();
        x = Double.parseDouble(String.format(PRECISION, x));
        y = Double.parseDouble(String.format(PRECISION, y));
        cell.setCellValue((new Coordinate(x, y)).toString());
    }

    private int createTable(Sheet sheet, Vector<Vector<Report>> report, int rowNum, String tableName, CellStyle headerStyle) {
        Row tableHeaderRow = sheet.createRow(rowNum++);
        tableHeaderRow.createCell(0).setCellValue(tableName);
        tableHeaderRow.getCell(0).setCellStyle(headerStyle);

        // Column labels
        Row columnLabelsRow = sheet.createRow(rowNum++);
        for (int i = 0; i < report.size(); i++) {
            columnLabelsRow.createCell(i + 1).setCellValue("Body " + (i + 1));
            columnLabelsRow.getCell(i + 1).setCellStyle(headerStyle);
        }

        // Row labels and data
        for (int j = 0; j < report.get(0).size(); j++) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue("Time step " + (j + 1));
            for (int i = 0; i < report.size(); i++) {
                Report reportEntry = report.get(i).get(j);
                Cell cell = dataRow.createCell(i + 1);
                switch (tableName) {
                    case "Positions" -> writeCell(reportEntry.getPosition(), cell);
                    case "Velocities" -> writeCell(reportEntry.getVelocity(), cell);
                    case "Forces" -> writeCell(reportEntry.getForce(), cell);
                }
            }
        }

        return rowNum;
    }
}
