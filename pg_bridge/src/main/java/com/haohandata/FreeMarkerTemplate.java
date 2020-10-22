package com.haohandata;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class FreeMarkerTemplate {
    private final static String VERSION = "2.3.0";
    private final static String TEMPLATE_PATH = "pg_bridge/template";
    private static FreeMarkerTemplate instance;
    private static Configuration cfg = null;

    public static FreeMarkerTemplate getInstance() throws IOException {
        if (instance == null) {
            synchronized (FreeMarkerTemplate.class) {
                if (instance == null) {
                    instance = new FreeMarkerTemplate();
                    cfg = new Configuration(new Version(VERSION));
                    cfg.setDirectoryForTemplateLoading(new File(TEMPLATE_PATH));
                    cfg.setEncoding(Locale.getDefault(), "UTF-8");
                    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
                }
            }
        }
        return instance;
    }

    public String transferTemplateToString(String templateFile, Map<String, String> paramMap) {
        Writer strWriter = new StringWriter();
        try {
            Template template = cfg.getTemplate(templateFile);
            template.process(paramMap, strWriter);
        } catch (IOException | TemplateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return strWriter.toString();
    }

    public static void main(String[] args) {
        try {
            FreeMarkerTemplate ft = FreeMarkerTemplate.getInstance();
            Map<String,String> model = new HashMap<>();
            model.put("qualifiedName","test");
            model.put("name","test");
            model.put("host","test");
            model.put("port","test");
            String result = ft.transferTemplateToString("pg_instance.json", model);
            System.out.println(result);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
