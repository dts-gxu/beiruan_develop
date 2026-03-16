package com.ruoyi.web.controller.ai;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件解析服务
 * 
 * 支持PDF、Word（doc/docx）、Excel（xls/xlsx）、图片（png/jpg/jpeg/gif/bmp）
 * 
 * @author ruoyi
 */
@Service
public class FileParseService
{
    private static final Logger log = LoggerFactory.getLogger(FileParseService.class);

    @Autowired
    private OcrService ocrService;

    /**
     * 解析上传的文件，提取文本内容
     *
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    public String parseFile(MultipartFile file) throws IOException
    {
        String fileName = file.getOriginalFilename();
        if (fileName == null)
        {
            throw new IOException("文件名为空");
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (ext)
        {
            case "pdf":
                return parsePdf(file.getInputStream());
            case "docx":
                return parseDocx(file.getInputStream());
            case "doc":
                return parseDoc(file.getInputStream());
            case "xlsx":
                return parseXlsx(file.getInputStream());
            case "xls":
                return parseXls(file.getInputStream());
            case "txt":
            case "md":
                return new String(file.getBytes(), "UTF-8");
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "bmp":
            case "webp":
                return parseImage(file);
            default:
                throw new IOException("不支持的文件格式：" + ext + "。支持的格式：PDF、Word、Excel、图片、TXT");
        }
    }

    /**
     * 解析PDF文件
     * 先尝试PDFBox提取文字，如果提取内容太少（扫描版PDF），则转图片走OCR识别
     */
    private String parsePdf(InputStream is) throws IOException
    {
        byte[] pdfBytes = is.readAllBytes();
        try (PDDocument doc = PDDocument.load(pdfBytes))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            // 判断是否为扫描版PDF：文字内容太少且有多页
            if (text.trim().length() < 50 && doc.getNumberOfPages() > 0)
            {
                log.info("PDF文字内容很少({}chars)，判断为扫描版PDF，转OCR识别", text.trim().length());
                return parsePdfByOcr(doc);
            }
            log.info("PDF解析完成（文字版），提取文本长度：{}", text.length());
            return text;
        }
    }

    /**
     * 扫描版PDF：将每页转成图片，用OCR识别
     */
    private String parsePdfByOcr(PDDocument doc) throws IOException
    {
        org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
        StringBuilder sb = new StringBuilder();
        for (int page = 0; page < doc.getNumberOfPages(); page++)
        {
            java.awt.image.BufferedImage image = renderer.renderImageWithDPI(page, 200);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] imgBytes = baos.toByteArray();
            log.info("PDF第{}页转图片完成，大小：{}KB", page + 1, imgBytes.length / 1024);
            String pageText = ocrService.recognizeImage(imgBytes, "image/png");
            if (pageText != null && !pageText.trim().isEmpty())
            {
                sb.append("--- 第").append(page + 1).append("页 ---\n");
                sb.append(pageText).append("\n\n");
            }
        }
        log.info("PDF OCR识别完成，总文本长度：{}", sb.length());
        return sb.toString();
    }

    /**
     * 解析Word docx文件
     */
    private String parseDocx(InputStream is) throws IOException
    {
        try (XWPFDocument doc = new XWPFDocument(is))
        {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs())
            {
                String text = para.getText();
                if (text != null && !text.trim().isEmpty())
                {
                    sb.append(text).append("\n");
                }
            }
            // 也解析表格
            doc.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        sb.append(cell.getText()).append("\t");
                    });
                    sb.append("\n");
                });
                sb.append("\n");
            });
            log.info("DOCX解析完成，提取文本长度：{}", sb.length());
            return sb.toString();
        }
    }

    /**
     * 解析Word doc文件（旧版格式）
     */
    private String parseDoc(InputStream is) throws IOException
    {
        try (HWPFDocument doc = new HWPFDocument(is))
        {
            WordExtractor extractor = new WordExtractor(doc);
            String text = extractor.getText();
            log.info("DOC解析完成，提取文本长度：{}", text.length());
            return text;
        }
    }

    /**
     * 解析Excel xlsx文件
     */
    private String parseXlsx(InputStream is) throws IOException
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is))
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                XSSFSheet sheet = workbook.getSheetAt(i);
                sb.append("## Sheet: ").append(sheet.getSheetName()).append("\n");
                for (int r = 0; r <= sheet.getLastRowNum(); r++)
                {
                    XSSFRow row = sheet.getRow(r);
                    if (row == null) continue;
                    for (int c = 0; c < row.getLastCellNum(); c++)
                    {
                        XSSFCell cell = row.getCell(c);
                        sb.append(getCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            log.info("XLSX解析完成，提取文本长度：{}", sb.length());
            return sb.toString();
        }
    }

    /**
     * 解析Excel xls文件（旧版格式）
     */
    private String parseXls(InputStream is) throws IOException
    {
        try (HSSFWorkbook workbook = new HSSFWorkbook(is))
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                HSSFSheet sheet = workbook.getSheetAt(i);
                sb.append("## Sheet: ").append(sheet.getSheetName()).append("\n");
                for (int r = 0; r <= sheet.getLastRowNum(); r++)
                {
                    HSSFRow row = sheet.getRow(r);
                    if (row == null) continue;
                    for (int c = 0; c < row.getLastCellNum(); c++)
                    {
                        HSSFCell cell = row.getCell(c);
                        sb.append(getCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            log.info("XLS解析完成，提取文本长度：{}", sb.length());
            return sb.toString();
        }
    }

    /**
     * 解析图片 — 优先PaddleOCR，降级AI视觉识别
     */
    private String parseImage(MultipartFile file) throws IOException
    {
        byte[] bytes = file.getBytes();
        String mimeType = file.getContentType();
        if (mimeType == null) mimeType = "image/png";
        log.info("图片解析开始，文件大小：{}KB，类型：{}", bytes.length / 1024, mimeType);
        String result = ocrService.recognizeImage(bytes, mimeType);
        if (result == null || result.trim().isEmpty())
        {
            throw new IOException("图片识别失败，未能提取到文字内容");
        }
        return result;
    }

    /**
     * 获取Excel单元格的值
     */
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell)
    {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        switch (type)
        {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num))
                {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { return cell.getStringCellValue(); }
            default:
                return "";
        }
    }
}
