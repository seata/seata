package com.alibaba.fescar.rm.datasource.plugin;

import com.alibaba.fescar.rm.datasource.plugin.context.LockKeyBuildAfterContext;
import com.alibaba.fescar.rm.datasource.plugin.context.SqlBuildAfterContext;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

import java.util.*;

/**
 * 插件管理类
 */
public class PluginManager {
    /**
     * plugins
     */
    private Map<String, List<Plugin>> pluginMap = new HashMap<>();

    public Map<String, List<Plugin>> getPluginMap() {
        return pluginMap;
    }

    /**
     * 添加插件
     *
     * @param plugin 插件实例对象
     */
    public void addPlugins(Plugin plugin) {
        for (String action : plugin.supportedActions()) {
            List<Plugin> plugins = pluginMap.get(action);
            if (plugins == null) {
                plugins = new ArrayList<>();
                pluginMap.put(action, plugins);
            }
            plugins.add(plugin);
        }
    }

    /**
     * 获取特定action相关的插件列表
     *
     * @param action 插件相关的action
     * @return
     */
    public List<Plugin> getPlugins(String action) {
        return pluginMap.getOrDefault(action, Collections.emptyList());
    }


    /**
     * sql语句构建后处理
     *
     * @param sqlHints  SqlHint列表
     * @param originSql 已经构建的sql
     * @return
     */
    public String execSqlBuildAfter(List<String> sqlHints, String originSql) {
        SqlBuildAfterContext context = new SqlBuildAfterContext(sqlHints, originSql);
        Object result = execPlugin(context, PluginConstants.ACTION_SQL_BUILD_AFTER);
        return (String) result;
    }

    /**
     * lockKey构建后处理
     *
     * @param sqlHints     SqlHint列表
     * @param tableRecords TableRecords对象实例
     * @param lockKey      已经构建的lockKey
     * @return
     */
    public String execLockKeyBuildAfter(List<String> sqlHints, TableRecords tableRecords, String lockKey) {
        LockKeyBuildAfterContext context = new LockKeyBuildAfterContext(sqlHints, tableRecords, lockKey);
        Object result = execPlugin(context, PluginConstants.ACTION_LOCK_KEY_BUILD_AFTER);
        return (String) result;
    }

    /**
     * 执行特定action相关的处理
     *
     * @param context plugin上下文对象
     * @param action  插件相关的action
     * @return
     */
    public Object execPlugin(PluginContext context, String action) {
        List<Plugin> pluginList = getPlugins(action);
        for (Plugin plugin : pluginList) {
            Object result = plugin.proc(context);
            //本次执行结果传递到后续处理
            context.setResult(result);
        }
        return context.getResult();
    }

}
