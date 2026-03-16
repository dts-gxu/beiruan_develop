package com.ruoyi.web.controller.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OCR文字识别服务
 *
 * 支持两种识别引擎：
 * 1. PaddleOCR（飞桨OCR）— 本地部署的HTTP服务，适合图片和扫描版PDF
 * 2. AI大模型视觉识别 — 降级方案，通过DeepSeek等大模型的视觉能力识别
 *
 * PaddleOCR部署方式：
 *   pip install paddleocr paddlehub
 *   hub serving start -m ocr_system -p 8868
 * 或使用Docker：
 *   docker run -dp 8868:8868 paddlepaddle/paddle_ocr:latest
 *
 * @author ruoyi
 */
@Service
public class OcrService
{
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${ai.ocr.paddleUrl:http://localhost:8868/predict/ocr_system}")
    private String paddleOcrUrl;

    @Value("${ai.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Autowired
    private AiChatService aiChatService;

    /**
     * 识别图片中的文字
     *
     * 优先使用PaddleOCR，失败则降级到AI大模型视觉识别
     *
     * @param imageBytes 图片字节数据
     * @param mimeType 图片MIME类型
     * @return 识别出的文字内容
     */
    public String recognizeImage(byte[] imageBytes, String mimeType)
    {
        if (!ocrEnabled)
        {
            return recognizeByAiVision(imageBytes, mimeType);
        }

        // 优先尝试PaddleOCR
        try
        {
            String result = recognizeByPaddleOcr(imageBytes);
            if (result != null && !result.trim().isEmpty())
            {
                log.info("PaddleOCR识别成功，文本长度：{}", result.length());
                return result;
            }
        }
        catch (Exception e)
        {
            log.warn("PaddleOCR识别失败，降级到AI视觉识别：{}", e.getMessage());
        }

        // 降级到AI大模型视觉识别
        return recognizeByAiVision(imageBytes, mimeType);
    }

    /**
     * 使用PaddleOCR识别图片文字
     *
     * PaddleOCR Hub Serving API格式：
     * POST http://localhost:8868/predict/ocr_system
     * Body: {"images": ["base64编码的图片"]}
     * Response: {"results": [{"data": [{"text": "识别文字", "confidence": 0.99, "text_region": [...]}]}]}
     *
     * @param imageBytes 图片字节数据
     * @return 识别的文字内容
     */
    private String recognizeByPaddleOcr(byte[] imageBytes) throws Exception
    {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 构建请求JSON
        JSONObject requestBody = new JSONObject();
        JSONArray images = new JSONArray();
        images.add(base64Image);
        requestBody.put("images", images);

        // 发送HTTP请求
        URL url = new URL(paddleOcrUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream())
        {
            os.write(requestBody.toJSONString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200)
        {
            throw new Exception("PaddleOCR返回错误码：" + responseCode);
        }

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }
        }

        // 解析PaddleOCR响应，提取文字
        return parsePaddleOcrResponse(response.toString());
    }

    /**
     * 解析PaddleOCR的响应JSON，提取所有文字并按位置排序拼接
     */
    private String parsePaddleOcrResponse(String responseStr)
    {
        try
        {
            JSONObject response = JSONObject.parseObject(responseStr);
            JSONArray results = response.getJSONArray("results");
            if (results == null || results.isEmpty())
            {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            JSONArray data = results.getJSONObject(0).getJSONArray("data");
            if (data == null)
            {
                return "";
            }

            for (int i = 0; i < data.size(); i++)
            {
                JSONObject item = data.getJSONObject(i);
                String text = item.getString("text");
                if (text != null && !text.trim().isEmpty())
                {
                    sb.append(text).append("\n");
                }
            }

            return sb.toString().trim();
        }
        catch (Exception e)
        {
            log.error("解析PaddleOCR响应失败：{}", responseStr, e);
            return "";
        }
    }

    /**
     * 使用AI大模型视觉能力识别图片中的文字（降级方案）
     *
     * @param imageBytes 图片字节数据
     * @param mimeType 图片MIME类型
     * @return 识别的文字内容
     */
    private String recognizeByAiVision(byte[] imageBytes, String mimeType)
    {
        try
        {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            if (mimeType == null || mimeType.isEmpty())
            {
                mimeType = "image/png";
            }
            String result = aiChatService.chatWithImage(
                    "请识别并完整提取这张图片中的所有文字内容。保持原有格式和层次结构。如果是表格，请以Markdown表格格式输出。只输出提取的文字，不要添加额外说明。",
                    base64, mimeType);
            log.info("AI视觉识别完成，文本长度：{}", result.length());
            return result;
        }
        catch (Exception e)
        {
            log.error("AI视觉识别也失败了", e);
            return "【OCR识别失败】PaddleOCR服务未部署，AI视觉识别也不可用。请手动复制图片中的文字到输入框。\n"
                    + "如需启用PaddleOCR，请执行：pip install paddleocr paddlehub && hub serving start -m ocr_system -p 8868";
        }
    }
}
