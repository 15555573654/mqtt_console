package com.ruoyi.system.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.ruoyi.system.service.ITestCaseExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试用例导出服务实现
 * 
 * @author ruoyi
 */
@Service
public class TestCaseExportServiceImpl implements ITestCaseExportService 
{
    private static final Logger log = LoggerFactory.getLogger(TestCaseExportServiceImpl.class);

    /**
     * 导出为CSV格式
     */
    @Override
    public String exportToCsv(String caseContent) 
    {
        if (caseContent == null || caseContent.isEmpty()) 
        {
            return "";
        }

        StringBuilder csv = new StringBuilder();
        // CSV表头
        csv.append("用例ID,功能模块,测试目标,前置条件,测试步骤,预期结果,优先级\n");

        // 解析Markdown内容
        String[] lines = caseContent.split("\n");
        String currentModule = "";
        String currentCaseId = "";
        String currentObjective = "";
        String currentPrecondition = "";
        StringBuilder currentSteps = new StringBuilder();
        String currentExpected = "";
        String currentPriority = "";

        for (String line : lines) 
        {
            line = line.trim();
            
            // 解析模块 (## 开头)
            if (line.startsWith("## ")) 
            {
                currentModule = line.substring(3).trim();
            }
            // 解析用例标题 (### 开头)
            else if (line.startsWith("### ")) 
            {
                // 保存上一个用例
                if (!currentCaseId.isEmpty()) 
                {
                    csv.append(formatCsvLine(currentCaseId, currentModule, currentObjective, 
                        currentPrecondition, currentSteps.toString(), currentExpected, currentPriority));
                }
                // 重置
                currentCaseId = "";
                currentObjective = "";
                currentPrecondition = "";
                currentSteps = new StringBuilder();
                currentExpected = "";
                currentPriority = "";
            }
            // 解析用例ID
            else if (line.startsWith("- **用例ID**:") || line.startsWith("**用例ID**:")) 
            {
                currentCaseId = extractValue(line);
            }
            // 解析测试目标
            else if (line.startsWith("- **测试目标**:") || line.startsWith("**测试目标**:")) 
            {
                currentObjective = extractValue(line);
            }
            // 解析前置条件
            else if (line.startsWith("- **前置条件**:") || line.startsWith("**前置条件**:")) 
            {
                currentPrecondition = extractValue(line);
            }
            // 解析测试步骤
            else if (line.startsWith("- **测试步骤**:") || line.startsWith("**测试步骤**:")) 
            {
                // 开始收集步骤
            }
            else if (line.matches("^\\d+\\..*")) 
            {
                if (currentSteps.length() > 0) 
                {
                    currentSteps.append("; ");
                }
                currentSteps.append(line);
            }
            // 解析预期结果
            else if (line.startsWith("- **预期结果**:") || line.startsWith("**预期结果**:")) 
            {
                currentExpected = extractValue(line);
            }
            // 解析优先级
            else if (line.startsWith("- **优先级**:") || line.startsWith("**优先级**:")) 
            {
                currentPriority = extractValue(line);
            }
        }

        // 保存最后一个用例
        if (!currentCaseId.isEmpty()) 
        {
            csv.append(formatCsvLine(currentCaseId, currentModule, currentObjective, 
                currentPrecondition, currentSteps.toString(), currentExpected, currentPriority));
        }

        return csv.toString();
    }

    /**
     * 导出为XMind格式
     */
    @Override
    public byte[] exportToXMind(String caseTitle, String caseContent) throws Exception 
    {
        // 创建XML文档
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // 创建根元素
        Element xmapContent = doc.createElement("xmap-content");
        xmapContent.setAttribute("xmlns", "urn:xmind:xmap:xmlns:content:2.0");
        xmapContent.setAttribute("xmlns:fo", "http://www.w3.org/1999/XSL/Format");
        xmapContent.setAttribute("xmlns:svg", "http://www.w3.org/2000/svg");
        xmapContent.setAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
        xmapContent.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        xmapContent.setAttribute("version", "2.0");
        doc.appendChild(xmapContent);

        // 创建sheet
        Element sheet = doc.createElement("sheet");
        sheet.setAttribute("id", "sheet1");
        sheet.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
        xmapContent.appendChild(sheet);

        // 创建根主题
        Element rootTopic = doc.createElement("topic");
        rootTopic.setAttribute("id", "root");
        rootTopic.setAttribute("structure-class", "org.xmind.ui.map.unbalanced");
        sheet.appendChild(rootTopic);

        Element rootTitle = doc.createElement("title");
        rootTitle.setTextContent(caseTitle);
        rootTopic.appendChild(rootTitle);

        // 解析Markdown并创建子主题
        Element children = doc.createElement("children");
        Element topics = doc.createElement("topics");
        topics.setAttribute("type", "attached");
        children.appendChild(topics);
        rootTopic.appendChild(children);

        parseMarkdownToXMind(doc, topics, caseContent);

        // 转换为字符串
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String xmlContent = writer.toString();

        // 创建XMind文件(ZIP格式)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // 添加content.xml
        ZipEntry contentEntry = new ZipEntry("content.xml");
        zos.putNextEntry(contentEntry);
        zos.write(xmlContent.getBytes("UTF-8"));
        zos.closeEntry();

        // 添加META-INF/manifest.xml
        ZipEntry manifestEntry = new ZipEntry("META-INF/manifest.xml");
        zos.putNextEntry(manifestEntry);
        String manifest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<manifest xmlns=\"urn:xmind:xmap:xmlns:manifest:1.0\">\n" +
                         "  <file-entry full-path=\"content.xml\" media-type=\"text/xml\"/>\n" +
                         "</manifest>";
        zos.write(manifest.getBytes("UTF-8"));
        zos.closeEntry();

        zos.close();
        return baos.toByteArray();
    }

    /**
     * 解析Markdown到XMind结构
     */
    private void parseMarkdownToXMind(Document doc, Element parent, String content) 
    {
        String[] lines = content.split("\n");
        Element currentLevel1 = null;
        Element currentLevel2 = null;
        Element currentLevel3 = null;
        int topicId = 1;

        for (String line : lines) 
        {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 一级标题 (##)
            if (line.startsWith("## ")) 
            {
                String title = line.substring(3).trim();
                currentLevel1 = createTopic(doc, "topic" + topicId++, title);
                parent.appendChild(currentLevel1);
                currentLevel2 = null;
                currentLevel3 = null;
            }
            // 二级标题 (###)
            else if (line.startsWith("### ")) 
            {
                String title = line.substring(4).trim();
                if (currentLevel1 != null) 
                {
                    Element children = getOrCreateChildren(doc, currentLevel1);
                    currentLevel2 = createTopic(doc, "topic" + topicId++, title);
                    children.appendChild(currentLevel2);
                    currentLevel3 = null;
                }
            }
            // 三级标题 (####)
            else if (line.startsWith("#### ")) 
            {
                String title = line.substring(5).trim();
                if (currentLevel2 != null) 
                {
                    Element children = getOrCreateChildren(doc, currentLevel2);
                    currentLevel3 = createTopic(doc, "topic" + topicId++, title);
                    children.appendChild(currentLevel3);
                }
            }
            // 列表项
            else if (line.startsWith("- ") || line.matches("^\\d+\\..*")) 
            {
                String content_text = line.replaceFirst("^[-\\d+\\.\\*]\\s*", "");
                Element targetParent = currentLevel3 != null ? currentLevel3 : 
                                      (currentLevel2 != null ? currentLevel2 : currentLevel1);
                if (targetParent != null) 
                {
                    Element children = getOrCreateChildren(doc, targetParent);
                    Element topic = createTopic(doc, "topic" + topicId++, content_text);
                    children.appendChild(topic);
                }
            }
        }
    }

    /**
     * 创建主题节点
     */
    private Element createTopic(Document doc, String id, String title) 
    {
        Element topic = doc.createElement("topic");
        topic.setAttribute("id", id);
        
        Element titleElement = doc.createElement("title");
        titleElement.setTextContent(title);
        topic.appendChild(titleElement);
        
        return topic;
    }

    /**
     * 获取或创建children节点
     */
    private Element getOrCreateChildren(Document doc, Element parent) 
    {
        Element children = (Element) parent.getElementsByTagName("children").item(0);
        if (children == null) 
        {
            children = doc.createElement("children");
            Element topics = doc.createElement("topics");
            topics.setAttribute("type", "attached");
            children.appendChild(topics);
            parent.appendChild(children);
            return topics;
        }
        return (Element) children.getElementsByTagName("topics").item(0);
    }

    /**
     * 格式化CSV行
     */
    private String formatCsvLine(String... values) 
    {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) 
        {
            if (i > 0) line.append(",");
            line.append("\"").append(escapeCsv(values[i])).append("\"");
        }
        line.append("\n");
        return line.toString();
    }

    /**
     * 转义CSV特殊字符
     */
    private String escapeCsv(String value) 
    {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }

    /**
     * 提取值
     */
    private String extractValue(String line) 
    {
        int colonIndex = line.indexOf(":");
        if (colonIndex > 0 && colonIndex < line.length() - 1) 
        {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }
}
