package com.haohandata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haohandata.httpclient.HttpParam;
import com.haohandata.httpclient.RemoteAccess;
import com.haohandata.httpclient.RemoteResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责与元数据管理模块atlas的接口交互
 */
public class AtlasManager {
    private final static Logger logger = LoggerFactory.getLogger(AtlasManager.class);
    private String atlasUrl;
    private Set<Integer> entitySet; // 记录当前处理的数据库对象，避免重复操作
    private Map<String, String> entityMap;
    private Gson gson;

    public AtlasManager() {
        this.atlasUrl = MessageFormat.format("http://{0}:{1}/api/atlas/v2", Constant.ATLAS_IP,
                String.valueOf(Constant.ATLAS_PORT));
        this.entitySet = new HashSet<>();
        this.entityMap = new HashMap<>();
        this.gson = new Gson();
    }

    public void createEntity(DDLEvent event) {
        String api = this.atlasUrl + "/entity";
        if (!entitySet.contains(event.getObjId())) {
            logger.info("create entity {}", event.toString());
            RemoteAccess ra;
            try {
                ra = new RemoteAccess(api);
                ra.build();
                // check if postgresql_instance exist
                String pgInstanceName = MessageFormat.format("{0}:{1}@postgresql", Constant.DB_HOST,
                        String.valueOf(Constant.DB_PORT));
                String pgInstanceGuid = checkInstance(ra, pgInstanceName);
                String pgDBName = MessageFormat.format("{0}@{1}:{2}@postgresql", Constant.DB_NAME, Constant.DB_HOST,
                        String.valueOf(Constant.DB_PORT));
                String pgDBGuid = checkDB(ra, pgDBName, pgInstanceGuid);
                String tableName = event.getObjectIdentity().substring(event.getObjectIdentity().indexOf(".") + 1);
                createTable(ra, pgDBName, tableName, pgDBGuid);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

    public void removeEntity(DDLEvent event) {
        String api = this.atlasUrl + "/entity/guid/";
        if (!entitySet.contains(event.getObjId())) {
            searchByType("postgresql_table");
        }
        String tableName = event.getObjectIdentity().substring(event.getObjectIdentity().indexOf(".") + 1);
        String key = MessageFormat.format("{0}@{1}@{2}:{3}@postgresql", tableName, Constant.DB_NAME, Constant.DB_HOST,
                String.valueOf(Constant.DB_PORT));
        String guid = entityMap.getOrDefault(key, null);
        if (null != guid) {
            api += guid;
            RemoteAccess ra;
            try {
                ra = new RemoteAccess(api);
                ra.build();
                ra.delete(new ArrayList<HttpParam>() {
                });
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // ra.delete()
        }
    }

    private String createTable(RemoteAccess ra, String dbName, String tableName, String dbGuid) {
        String tableGuid = null;
        System.out.println("crate table =>" + tableName);
        Map<String, String> model = new HashMap<>();
        model.put("qualifiedName", tableName + "@" + dbName);
        model.put("name", tableName);
        model.put("dbGuid", dbGuid);
        FreeMarkerTemplate ft;
        try {
            ft = FreeMarkerTemplate.getInstance();
            String jsonBody = ft.transferTemplateToString("pg_table.json", model);
            List<RemoteResponse> responseList = ra.post(new ArrayList<HttpParam>(), jsonBody);
            if (responseList.size() > 0) {
                RemoteResponse response = responseList.get(0);
                JsonElement jsonResult = gson.fromJson(response.getResponse(), JsonElement.class);
                tableGuid = jsonResult.getAsJsonObject().getAsJsonObject("mutatedEntities").getAsJsonArray("CREATE")
                        .get(0).getAsJsonObject().get("guid").getAsString();
                System.out.println(tableGuid);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tableGuid;
    }

    private String checkDB(RemoteAccess ra, String pgDBName, String instanceGuid) {
        String pgDBGuid = null;
        if (this.entityMap.containsKey(pgDBName)) {
            pgDBGuid = entityMap.get(pgDBName);
        } else {
            searchByType("postgresql_db");
            if (!this.entityMap.containsKey(pgDBName)) {
                pgDBGuid = createDB(ra, pgDBName, instanceGuid);
                this.entityMap.put(pgDBName, pgDBGuid);
            } else {
                pgDBGuid = entityMap.get(pgDBName);
            }
        }
        return pgDBGuid;
    }

    private String createDB(RemoteAccess ra, String dbName, String instanceGuid) {
        String guid = null;
        logger.info("create database =>" + dbName);
        Map<String, String> model = new HashMap<>();
        model.put("qualifiedName", dbName);
        model.put("name", dbName);
        model.put("instanceGuid", instanceGuid);
        FreeMarkerTemplate ft;
        try {
            ft = FreeMarkerTemplate.getInstance();
            String jsonBody = ft.transferTemplateToString("pg_database.json", model);
            List<RemoteResponse> responseList = ra.post(new ArrayList<HttpParam>(), jsonBody);
            if (responseList.size() > 0) {
                RemoteResponse response = responseList.get(0);
                JsonElement jsonResult = gson.fromJson(response.getResponse(), JsonElement.class);
                guid = jsonResult.getAsJsonObject().getAsJsonObject("mutatedEntities").getAsJsonArray("CREATE").get(0)
                        .getAsJsonObject().get("guid").getAsString();
                System.out.println(guid);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return guid;
    }

    private String checkInstance(RemoteAccess ra, String pgInstanceName) {
        String pgInstanceGuid = null;
        if (this.entityMap.containsKey(pgInstanceName)) {
            pgInstanceGuid = entityMap.get(pgInstanceName);
        } else {
            searchByType("postgresql_instance");
            if (!this.entityMap.containsKey(pgInstanceName)) {
                pgInstanceGuid = createInstance(ra, pgInstanceName);
                this.entityMap.put(pgInstanceName, pgInstanceGuid);
            } else {
                pgInstanceGuid = entityMap.get(pgInstanceName);
            }
        }
        return pgInstanceGuid;
    }

    private String createInstance(RemoteAccess ra, String instanceName) {
        String instanceGuid = null;
        logger.info("create instance =>" + instanceName);
        Map<String, String> model = new HashMap<>();
        model.put("qualifiedName", instanceName);
        model.put("name", instanceName);
        model.put("host", Constant.DB_HOST);
        model.put("port", String.valueOf(Constant.DB_PORT));
        FreeMarkerTemplate ft;
        try {
            ft = FreeMarkerTemplate.getInstance();
            String jsonBody = ft.transferTemplateToString("pg_instance.json", model);
            List<RemoteResponse> responseList = ra.post(new ArrayList<HttpParam>(), jsonBody);
            if (responseList.size() > 0) {
                RemoteResponse response = responseList.get(0);
                JsonElement jsonResult = gson.fromJson(response.getResponse(), JsonElement.class);
                instanceGuid = jsonResult.getAsJsonObject().getAsJsonObject("mutatedEntities").getAsJsonArray("CREATE")
                        .get(0).getAsJsonObject().get("guid").getAsString();
                System.out.println(instanceGuid);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return instanceGuid;
    }

    /**
     * 根据type名称进行检索，返回所有符合条件的entity名称列表
     * 
     * @param typeName
     * @return
     */
    private List<EntityModel> searchByType(String typeName) {
        List<EntityModel> qualifiedNameList = new ArrayList<>();
        String api = this.atlasUrl + "/search/basic";
        List<HttpParam> httpParams = new ArrayList<>();
        httpParams.add(new HttpParam<String>("typeName", typeName));
        RemoteAccess ra;
        try {
            ra = new RemoteAccess(api);
            ra.build();
            List<RemoteResponse> responseList = ra.get(httpParams);
            for (RemoteResponse response : responseList) {
                JsonElement jElement = gson.fromJson(response.getResponse(), JsonElement.class);
                JsonObject jObject = jElement.getAsJsonObject();
                JsonArray entityArray = jObject.get("entities").getAsJsonArray();
                for (int i = 0; i < entityArray.size(); i++) {
                    JsonObject entity = entityArray.get(i).getAsJsonObject();
                    String guid = entity.get("guid").getAsString();
                    String qualifiedName = entity.get("attributes").getAsJsonObject().get("qualifiedName")
                            .getAsString();
                    qualifiedNameList.add(EntityModel.builder().guid(guid).qualifiedName(qualifiedName).build());
                    this.entityMap.put(qualifiedName, guid);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return qualifiedNameList;
    }

    public static void main(String[] args) {
        AtlasManager atlasManager = new AtlasManager();
        List<EntityModel> modelList = atlasManager.searchByType("postgresql_instance");
        for (EntityModel model : modelList) {
            System.out.println(model.toString());
        }
    }

}
