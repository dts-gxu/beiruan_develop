package com.ruoyi.web.controller.ai;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSE流式响应工具类 — 仿FastGPT responseWrite
 * 
 * 核心原理：直接用OutputStream写UTF-8字节流，完全绕过Tomcat/Spring的字符编码层
 * FastGPT源码参考: packages/service/common/response/index.ts
 *   responseWrite({ res, event, data }) {
 *     event && res.write(`event: ${event}\n`);
 *     res.write(`data: ${data}\n\n`);
 *   }
 */
public class SseHelper
{
    private static final Logger log = LoggerFactory.getLogger(SseHelper.class);

    /**
     * 初始化SSE响应头（仿FastGPT res.setHeader）
     */
    public static void initSseHeaders(HttpServletResponse response)
    {
        response.setStatus(200);
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
    }

    /**
     * 启动异步上下文（仿FastGPT长连接）
     */
    public static AsyncContext startAsync(HttpServletRequest request, long timeoutMs)
    {
        AsyncContext ctx = request.startAsync();
        ctx.setTimeout(timeoutMs);
        return ctx;
    }

    /**
     * 仿FastGPT responseWrite: 写SSE事件到OutputStream
     * 直接写UTF-8字节，不经过PrintWriter/StringHttpMessageConverter
     */
    public static void write(HttpServletResponse response, String event, String data)
    {
        try
        {
            OutputStream out = response.getOutputStream();
            if (event != null && !event.isEmpty())
            {
                out.write(("event: " + event + "\n").getBytes(StandardCharsets.UTF_8));
            }
            // SSE规范：data中的换行必须拆成多行 data: 前缀
            String safeData = data.replace("\n", "\ndata: ");
            out.write(("data: " + safeData + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
        catch (Exception e)
        {
            log.warn("SSE write failed: {}", e.getMessage());
        }
    }

    /**
     * 写SSE数据事件（无event名，默认message）
     */
    public static void writeData(HttpServletResponse response, String data)
    {
        write(response, null, data);
    }

    /**
     * 写SSE命名事件
     */
    public static void writeEvent(HttpServletResponse response, String event, String data)
    {
        write(response, event, data);
    }

    /**
     * 通过AsyncContext写SSE数据
     */
    public static void writeData(AsyncContext ctx, String data)
    {
        try
        {
            HttpServletResponse resp = (HttpServletResponse) ctx.getResponse();
            write(resp, null, data);
        }
        catch (Exception e)
        {
            log.warn("SSE async write failed: {}", e.getMessage());
        }
    }

    /**
     * 通过AsyncContext写SSE命名事件
     */
    public static void writeEvent(AsyncContext ctx, String event, String data)
    {
        try
        {
            HttpServletResponse resp = (HttpServletResponse) ctx.getResponse();
            write(resp, event, data);
        }
        catch (Exception e)
        {
            log.warn("SSE async event write failed: {}", e.getMessage());
        }
    }

    /**
     * 安全关闭AsyncContext
     */
    public static void complete(AsyncContext ctx)
    {
        try
        {
            ctx.complete();
        }
        catch (Exception e)
        {
            log.warn("SSE async complete failed: {}", e.getMessage());
        }
    }
}
