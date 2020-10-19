package com.haohandata;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import com.haohandata.httpclient.RemoteAccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责与元数据管理模块atlas的接口交互
 */
public class AtlasManager {
    private final static Logger logger = LoggerFactory.getLogger(AtlasManager.class);
    private String atlasUrl;
    private Set<Integer> entitySet; // 记录当前处理的数据库对象，避免重复操作

    public AtlasManager() {
        this.atlasUrl = MessageFormat.format("http://{0}:{1}/api/atlas/v2", Constant.ATLAS_IP, Constant.ATLAS_PORT);
        this.entitySet = new HashSet<>();
    }

    public void createEntity(DDLEvent event) {
        String api = this.atlasUrl + "/entity";
        if (!entitySet.contains(event.getObjId())) {
            logger.info("create entity {}", event.toString());
            RemoteAccess ra;
            try {
                ra = new RemoteAccess(api);
                ra.build();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

}
