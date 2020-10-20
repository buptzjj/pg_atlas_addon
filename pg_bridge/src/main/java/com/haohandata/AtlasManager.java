package com.haohandata;

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

    public AtlasManager() {
        this.atlasUrl = MessageFormat.format("http://{0}:{1}/api/atlas/v2", Constant.ATLAS_IP,
                String.valueOf(Constant.ATLAS_PORT));
        this.entitySet = new HashSet<>();
        this.entityMap = new HashMap<>();
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
                String pgDBGuid = checkDB(ra,pgDBName);
                String tableName = event.getObjectIdentity().substring(event.getObjectIdentity().indexOf(".")+1);
                createTable(ra, tableName, pgDBGuid);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

    public String createTable(RemoteAccess ra, String tableName, String guid){
        String tableGuid = null;
        System.out.println("crate table =>" + tableName);
        return tableGuid;
    }

    public String checkDB(RemoteAccess ra, String pgDBName) {
        String pgDBGuid = null;
        if (this.entityMap.containsKey(pgDBName)) {
            pgDBGuid = entityMap.get(pgDBGuid);
        } else {
            searchByType("postgresql_db");
            if (!this.entityMap.containsKey(pgDBName)) {
                pgDBGuid = createDB(ra, pgDBName);
                this.entityMap.put(pgDBName, pgDBGuid);
            }
        }
        return pgDBGuid;
    }

    public String createDB(RemoteAccess ra, String dbName){
        System.out.println("create database =>" + dbName);
        return null;
    }

    public String checkInstance(RemoteAccess ra, String pgInstanceName) {
        String pgInstanceGuid = null;
        if (this.entityMap.containsKey(pgInstanceName)) {
            pgInstanceGuid = entityMap.get(pgInstanceName);
        } else {
            searchByType("postgresql_instance");
            if (!this.entityMap.containsKey(pgInstanceName)) {
                pgInstanceGuid = createInstance(ra, pgInstanceName);
                this.entityMap.put(pgInstanceName, pgInstanceGuid);
            }
        }
        return pgInstanceGuid;
    }

    public String createInstance(RemoteAccess ra, String instanceName) {
        System.out.println("create instance =>" + instanceName);
        return null;
    }

    /**
     * 根据type名称进行检索，返回所有符合条件的entity名称列表
     * 
     * @param typeName
     * @return
     */
    public List<EntityModel> searchByType(String typeName) {
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
                Gson gson = new Gson();
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
