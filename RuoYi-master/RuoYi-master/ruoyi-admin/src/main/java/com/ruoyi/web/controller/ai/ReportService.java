package com.ruoyi.web.controller.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 报告生成服务
 *
 * 将Markdown格式的报告内容转换为Word文档(.docx)
 *
 * @author ruoyi
 */
@Service
public class ReportService
{
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    /**
     * 将Markdown报告转换为Word文档字节数组
     *
     * @param markdownContent Markdown格式的报告内容
     * @param title 报告标题
     * @return Word文档的字节数组
     */
    public byte[] generateWordReport(String markdownContent, String title) throws IOException
    {
        try (XWPFDocument document = new XWPFDocument())
        {
            // 封面标题
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            titlePara.setSpacingBefore(3000);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("微软雅黑");

            // 副标题 - 日期
            XWPFParagraph datePara = document.createParagraph();
            datePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun dateRun = datePara.createRun();
            dateRun.setText("生成日期：" + java.time.LocalDate.now().toString());
            dateRun.setFontSize(12);
            dateRun.setFontFamily("微软雅黑");
            dateRun.setColor("666666");

            // 分隔
            document.createParagraph().createRun().addBreak();

            // 解析Markdown内容
            parseMarkdownToWord(document, markdownContent);

            // 输出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            log.info("Word报告生成完成，大小：{}KB", baos.size() / 1024);
            return baos.toByteArray();
        }
    }

    /**
     * 解析Markdown内容并写入Word文档
     */
    private void parseMarkdownToWord(XWPFDocument document, String markdown)
    {
        if (markdown == null || markdown.isEmpty()) return;

        String[] lines = markdown.split("\n");
        boolean inTable = false;
        java.util.List<String[]> tableRows = new java.util.ArrayList<>();

        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            String trimmed = line.trim();

            // 表格行
            if (trimmed.startsWith("|") && trimmed.endsWith("|"))
            {
                // 跳过分隔行
                if (trimmed.matches("^\\|[\\s\\-:|]+\\|$"))
                {
                    continue;
                }
                if (!inTable)
                {
                    inTable = true;
                    tableRows.clear();
                }
                String[] cells = trimmed.split("\\|");
                java.util.List<String> cellList = new java.util.ArrayList<>();
                for (String cell : cells)
                {
                    if (!cell.trim().isEmpty())
                    {
                        cellList.add(cell.trim());
                    }
                }
                tableRows.add(cellList.toArray(new String[0]));
                continue;
            }

            // 表格结束
            if (inTable)
            {
                createTable(document, tableRows);
                inTable = false;
                tableRows.clear();
            }

            // 空行
            if (trimmed.isEmpty())
            {
                continue;
            }

            // 一级标题
            if (trimmed.startsWith("# "))
            {
                addHeading(document, trimmed.substring(2), 1);
            }
            // 二级标题
            else if (trimmed.startsWith("## "))
            {
                addHeading(document, trimmed.substring(3), 2);
            }
            // 三级标题
            else if (trimmed.startsWith("### "))
            {
                addHeading(document, trimmed.substring(4), 3);
            }
            // 列表项
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* "))
            {
                addListItem(document, trimmed.substring(2));
            }
            // 有序列表
            else if (trimmed.matches("^\\d+\\.\\s.*"))
            {
                addParagraph(document, trimmed, false);
            }
            // 普通段落
            else
            {
                addParagraph(document, trimmed, false);
            }
        }

        // 处理末尾可能残留的表格
        if (inTable && !tableRows.isEmpty())
        {
            createTable(document, tableRows);
        }
    }

    /**
     * 添加标题
     */
    private void addHeading(XWPFDocument document, String text, int level)
    {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(level == 1 ? 400 : 200);
        para.setSpacingAfter(100);
        XWPFRun run = para.createRun();
        run.setText(cleanMarkdown(text));
        run.setBold(true);
        run.setFontFamily("微软雅黑");
        switch (level)
        {
            case 1:
                run.setFontSize(18);
                break;
            case 2:
                run.setFontSize(15);
                break;
            case 3:
                run.setFontSize(13);
                break;
        }
    }

    /**
     * 添加普通段落
     */
    private void addParagraph(XWPFDocument document, String text, boolean indent)
    {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(60);
        if (indent)
        {
            para.setIndentationFirstLine(480);
        }

        // 处理粗体
        parseInlineFormatting(para, text);
    }

    /**
     * 添加列表项
     */
    private void addListItem(XWPFDocument document, String text)
    {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(40);
        para.setIndentationLeft(480);

        XWPFRun bullet = para.createRun();
        bullet.setText("• ");
        bullet.setFontSize(11);
        bullet.setFontFamily("微软雅黑");

        parseInlineFormatting(para, text);
    }

    /**
     * 处理行内格式（粗体、斜体等）
     */
    private void parseInlineFormatting(XWPFParagraph para, String text)
    {
        text = cleanMarkdown(text);
        // 简单处理：**粗体**
        Pattern boldPattern = Pattern.compile("\\*\\*(.+?)\\*\\*");
        Matcher matcher = boldPattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find())
        {
            // 粗体前的普通文字
            if (matcher.start() > lastEnd)
            {
                XWPFRun run = para.createRun();
                run.setText(text.substring(lastEnd, matcher.start()));
                run.setFontSize(11);
                run.setFontFamily("微软雅黑");
            }
            // 粗体文字
            XWPFRun boldRun = para.createRun();
            boldRun.setText(matcher.group(1));
            boldRun.setBold(true);
            boldRun.setFontSize(11);
            boldRun.setFontFamily("微软雅黑");
            lastEnd = matcher.end();
        }

        // 剩余文字
        if (lastEnd < text.length())
        {
            XWPFRun run = para.createRun();
            run.setText(text.substring(lastEnd));
            run.setFontSize(11);
            run.setFontFamily("微软雅黑");
        }
    }

    /**
     * 创建表格
     */
    private void createTable(XWPFDocument document, java.util.List<String[]> rows)
    {
        if (rows.isEmpty()) return;

        int cols = rows.get(0).length;
        XWPFTable table = document.createTable(rows.size(), cols);

        // 设置表格宽度100%
        try
        {
            CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
            width.setType(STTblWidth.PCT);
            width.setW(BigInteger.valueOf(5000));
        }
        catch (Exception e)
        {
            // 忽略格式设置失败
        }

        for (int r = 0; r < rows.size(); r++)
        {
            XWPFTableRow tableRow = table.getRow(r);
            String[] cells = rows.get(r);
            for (int c = 0; c < cols && c < cells.length; c++)
            {
                XWPFTableCell cell = tableRow.getCell(c);
                cell.setText(cleanMarkdown(cells[c]));

                // 表头加粗
                if (r == 0)
                {
                    for (XWPFParagraph p : cell.getParagraphs())
                    {
                        for (XWPFRun run : p.getRuns())
                        {
                            run.setBold(true);
                            run.setFontSize(10);
                            run.setFontFamily("微软雅黑");
                        }
                    }
                    cell.setColor("F0F0F0");
                }
            }
        }

        // 表格后空行
        document.createParagraph();
    }

    /**
     * 清理Markdown标记符号
     */
    private String cleanMarkdown(String text)
    {
        if (text == null) return "";
        // 去除行内代码
        text = text.replaceAll("`([^`]+)`", "$1");
        // 去除emoji（保留中文和常规字符）
        text = text.replaceAll("[✅⚠️❌🚀🎉📋]", "");
        return text.trim();
    }
}
