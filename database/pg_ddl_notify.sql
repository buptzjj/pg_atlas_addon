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