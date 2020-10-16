
### 背景
项目需要利用atlas来做元数据管理，但是要把已有系统的库表分别导入atlas生成entity是个巨大工作量。而atlas自身目前只支持hive表的hook，本模块则用来实现potgresql数据库的hook。话不多说，上酸菜~~

### 1. 概述
"把大象装冰箱分三步"：
1. 定义atlas的type
2. PG侧针对DDL操作发出notify消息
3. 编写hook主程序，监听notify消息，解析并生成Entity写入atlas


### 2. 定义Atlas Type

atlas已经有预定义的rdbms相关的类型，为了区别，我们只需要在继承这些类型的基础上做一些差异化修改即可。

创建json文件：`2020-pgsql_model.json`

加载方式有两种：

- 方法1：将此文件放到atlas的文件夹下，我这边的路径地址是：`/usr/local/src/atlas/apache-atlas-2.1.0/models`，然后重启atlas服务即可，系统会自动加载创建这些类型。
- 方法2：通过API创建

```
curl -i -X POST -H 'Content-Type: application/json' -H 'Accept: application/json' -u admin 'http://<atlas服务器地址>:21000/api/atlas/v2/types/typedefs' -d @2020-pgsql_model.json
```

创建成功后，即可以在atlas的UI上看到已创建的type。

### 3. 数据库侧操作

我们需要利用pg提供的触发器及Listen/notify机制来实现将数据库的DDL操作消息实时的发出来。

具体的SQL如下：

```
-- DDL操作存储过程，notify消息
create or replace function et_ddl_command_end() returns event_trigger as $$ 
declare
   v_obj text;
begin
   select array_to_json(array_agg(row_to_json(t)))::text into v_obj from (select TG_EVENT, TG_TAG,      
      classid, objid, objsubid, command_tag, object_type, schema_name, object_identity, in_extension from pg_event_trigger_ddl_commands()) t; 
    perform pg_notify('ddl_event', format('%s',v_obj));

end;
$$ language plpgsql strict;

CREATE EVENT TRIGGER et_ddl_command_end on ddl_command_end EXECUTE PROCEDURE et_ddl_command_end();

create or replace function et_sql_drop() returns event_trigger as $$ 
declare
   v_obj text;
begin
   select array_to_json(array_agg(row_to_json(t)))::text into v_obj from (select TG_EVENT, TG_TAG,      
      classid, objid, objsubid, original, normal, is_temporary, object_type, schema_name, object_name, object_identity, address_names, address_args from pg_event_trigger_dropped_objects()) t; 
    perform pg_notify('ddl_event', format('%s',v_obj));

end;
$$ language plpgsql strict;

CREATE EVENT TRIGGER et_sql_drop on sql_drop EXECUTE PROCEDURE et_sql_drop(); 
```

### 4. pg_bridge模块

**功能**：监听数据库发出的notify消息，解析后调用atlas接口，写入atlas。

实现步骤：
1. 调用pg的listener接口，监听预先定义的`ddl_event`的通道
2. 接收到消息后解析json对象，根据操作类型及对象类型分别进行处理，对于建表操作，在解析完后需要根据reltype的字段做查询得到该表每个字段的信息，并生成postgresql_column的entity。
3. 分别进行映射，生成atlas对应的entity。对于建表语句，新生成entity；更新操作，则删除原有entity后重新生成（因为没有办法传递变更字段信息）；对于删除操作，则直接删除对应的entity。

具体代码见github： 
### 参考链接
- https://blog.csdn.net/wjzhang5514/article/details/98620720
- https://billtian.github.io/digoal.blog/2017/09/25/02.html
