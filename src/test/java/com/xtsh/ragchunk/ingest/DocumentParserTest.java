package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.web.BadRequestException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    void parsesXlsx() throws Exception {
        try (var wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("数据");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("列A");
            row.createCell(1).setCellValue("列B");
            var row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue(100);
            row2.createCell(1).setCellValue("值");
            wb.createSheet("备注");

            var file = excelFile("sample.xlsx", wb);
            String text = parser.parse(file);

            assertTrue(text.contains("## 数据"));
            assertTrue(text.contains("列A\t列B"));
            assertTrue(text.contains("100\t值"));
        }
    }

    @Test
    void parsesXls() throws Exception {
        try (var wb = new HSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("hello");
            row.createCell(1).setCellValue("xls");

            var file = excelFile("legacy.xls", wb);
            String text = parser.parse(file);

            assertTrue(text.contains("hello\txls"));
            assertFalse(text.contains("## "));
        }
    }

    @Test
    void parsesDocxWithParagraphAndTable() throws Exception {
        try (var doc = new XWPFDocument()) {
            var p = doc.createParagraph();
            p.createRun().setText("说明段落");
            var table = doc.createTable(2, 2);
            table.getRow(0).getCell(0).setText("列A");
            table.getRow(0).getCell(1).setText("列B");
            table.getRow(1).getCell(0).setText("100");
            table.getRow(1).getCell(1).setText("值");

            String text = parser.parse(docxFile("with-table.docx", doc));

            assertTrue(text.contains("说明段落"));
            assertTrue(text.contains("列A\t列B"));
            assertTrue(text.contains("100\t值"));
            assertTrue(text.indexOf("说明段落") < text.indexOf("列A"));
        }
    }

    @Test
    void parsesDocxWithTableOnly() throws Exception {
        try (var doc = new XWPFDocument()) {
            var table = doc.createTable(1, 2);
            table.getRow(0).getCell(0).setText("仅表格");
            table.getRow(0).getCell(1).setText("内容");

            String text = parser.parse(docxFile("table-only.docx", doc));

            assertEquals("仅表格\t内容", text);
        }
    }

    @Test
    void rejectsEmptyDocx() throws Exception {
        try (var doc = new XWPFDocument()) {
            doc.createParagraph();
            var file = docxFile("empty.docx", doc);
            assertThrows(BadRequestException.class, () -> parser.parse(file));
        }
    }

    @Test
    void rejectsUnsupportedExtension() {
        var file = new MockMultipartFile("file", "data.pdf", "application/pdf", new byte[]{1, 2});
        assertThrows(BadRequestException.class, () -> parser.parse(file));
    }

    @Test
    void rejectsEmptyExcel() throws Exception {
        try (var wb = new XSSFWorkbook()) {
            wb.createSheet("空表");
            var file = excelFile("empty.xlsx", wb);
            assertThrows(BadRequestException.class, () -> parser.parse(file));
        }
    }

    private static MockMultipartFile excelFile(String name, org.apache.poi.ss.usermodel.Workbook wb) throws Exception {
        var out = new ByteArrayOutputStream();
        wb.write(out);
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }

    private static MockMultipartFile docxFile(String name, XWPFDocument doc) throws Exception {
        var out = new ByteArrayOutputStream();
        doc.write(out);
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
    }
}
