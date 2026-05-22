package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.web.BadRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 上传文件解析：支持 txt / md / markdown / docx / xlsx / xls，按扩展名分支。 */
@Component
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);
    private static final DataFormatter CELL_FORMATTER = new DataFormatter();

    public String parse(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return parseStream(in, file.getOriginalFilename());
        }
    }

    /**
     * 从输入流解析（流式归档后入库、重训使用）。
     */
    public String parseStream(InputStream input, String originalFileName) {
        String name = originalFileName != null ? originalFileName.toLowerCase() : "";
        try {
            String text;
            String type;
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
                type = "text";
                text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } else if (name.endsWith(".docx")) {
                type = "docx";
                text = parseDocx(input);
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                type = name.endsWith(".xlsx") ? "xlsx" : "xls";
                text = parseExcel(input);
            } else {
                throw new BadRequestException("unsupported file type, use txt, md, docx, xlsx, or xls");
            }
            log.info("[文档上传] 文件解析 类型={}, 字符数={}", type, text.length());
            return text;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("failed to parse file: " + e.getMessage());
        }
    }

    /**
     * 从字节解析（测试或内存数据）。
     */
    public String parseBytes(byte[] data, String originalFileName) {
        String name = originalFileName != null ? originalFileName.toLowerCase() : "";
        try {
            String text;
            String type;
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
                type = "text";
                text = new String(data, StandardCharsets.UTF_8);
            } else if (name.endsWith(".docx")) {
                type = "docx";
                text = parseDocx(new java.io.ByteArrayInputStream(data));
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                type = name.endsWith(".xlsx") ? "xlsx" : "xls";
                text = parseExcel(new java.io.ByteArrayInputStream(data));
            } else {
                throw new BadRequestException("unsupported file type, use txt, md, docx, xlsx, or xls");
            }
            log.info("[文档上传] 文件解析 类型={}, 字符数={}", type, text.length());
            return text;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("failed to parse file: " + e.getMessage());
        }
    }

    /**
     * Word：按文档顺序遍历正文元素（段落 + 表格）；表格行按制表符拼接单元格，与 Excel 解析风格一致。
     */
    private String parseDocx(InputStream in) throws Exception {
        try (var doc = new XWPFDocument(in)) {
            var sb = new StringBuilder();
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraphBlock(sb, paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    appendTableBlock(sb, table);
                }
            }
            String text = sb.toString().trim();
            if (text.isBlank()) {
                throw new BadRequestException("docx file contains no readable text");
            }
            return text;
        }
    }

    private static void appendParagraphBlock(StringBuilder sb, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(raw.trim());
    }

    private static void appendTableBlock(StringBuilder sb, XWPFTable table) {
        var tableText = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            if (row == null) {
                continue;
            }
            String line = formatDocxTableRow(row);
            if (!line.isBlank()) {
                if (!tableText.isEmpty()) {
                    tableText.append('\n');
                }
                tableText.append(line);
            }
        }
        if (tableText.isEmpty()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(tableText);
    }

    /** 单行单元格按列顺序用制表符连接；单元格内换行压成空格。 */
    private static String formatDocxTableRow(XWPFTableRow row) {
        List<String> cells = new ArrayList<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            if (cell == null) {
                continue;
            }
            String value = normalizeDocxCellText(cell.getText());
            cells.add(value);
        }
        while (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
            cells.remove(cells.size() - 1);
        }
        if (cells.isEmpty() || cells.stream().allMatch(String::isEmpty)) {
            return "";
        }
        return String.join("\t", cells);
    }

    private static String normalizeDocxCellText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replace('\r', ' ').replace('\n', ' ').trim();
    }

    /**
     * Excel：遍历所有工作表，非空行按制表符拼接单元格；多 sheet 时用 Markdown 二级标题区分。
     */
    private String parseExcel(InputStream in) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            var sb = new StringBuilder();
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    continue;
                }
                if (!sb.isEmpty()) {
                    sb.append("\n\n");
                }
                if (sheetCount > 1) {
                    String sheetName = sheet.getSheetName();
                    if (sheetName != null && !sheetName.isBlank()) {
                        sb.append("## ").append(sheetName.trim()).append("\n\n");
                    }
                }
                for (Row row : sheet) {
                    if (row == null) {
                        continue;
                    }
                    String line = formatExcelRow(row);
                    if (!line.isBlank()) {
                        sb.append(line).append('\n');
                    }
                }
            }
            String text = sb.toString().trim();
            if (text.isBlank()) {
                throw new BadRequestException("excel file contains no readable cell data");
            }
            return text;
        }
    }

    private static String formatExcelRow(Row row) {
        List<String> cells = new ArrayList<>();
        for (Cell cell : row) {
            if (cell == null) {
                continue;
            }
            String value = CELL_FORMATTER.formatCellValue(cell).trim();
            if (!value.isEmpty()) {
                cells.add(value);
            }
        }
        return String.join("\t", cells);
    }
}
