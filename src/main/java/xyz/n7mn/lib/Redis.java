package xyz.n7mn.lib;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.IOException;

public class Redis {

    public static void LogRedisWrite(String AccessCode, String Category, String Value){
        new Thread(()->{
            // Redis 読み込み
            final File config = new File("./config-redis.yml");
            final YamlMapping ConfigYml;
            try {
                if (!config.exists()){
                    return;
                } else {
                    ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.gc();
                return;
            }

            // 書き出し
            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
            Jedis jedis = jedisPool.getResource();
            jedis.auth(ConfigYml.string("RedisPass"));
            jedis.set("nico-proxy:log:"+Category+":"+AccessCode, Value);
            jedis.close();
            jedisPool.close();
        }).start();
        System.gc();
    }

    public static String LogRedisRead(String key){

        // Redis 読み込み
        final File config = new File("./config-redis.yml");
        final YamlMapping ConfigYml;
        try {
            if (!config.exists()){
                return null;
            } else {
                ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.gc();
            return null;
        }

        // 書き出し
        JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
        Jedis jedis = jedisPool.getResource();
        jedis.auth(ConfigYml.string("RedisPass"));
        String s = jedis.get(key);
        jedis.close();
        jedisPool.close();

        System.gc();
        return s;
    }

    public static void LogRedisDelete(String key){
        new Thread(()->{
            // Redis 読み込み
            final File config = new File("./config-redis.yml");
            final YamlMapping ConfigYml;
            try {
                if (!config.exists()){
                    return;
                } else {
                    ConfigYml = Yaml.createYamlInput(config).readYamlMapping();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.gc();
                return;
            }

            // 書き出し
            JedisPool jedisPool = new JedisPool(ConfigYml.string("RedisServer"), ConfigYml.integer("RedisPort"));
            Jedis jedis = jedisPool.getResource();
            jedis.auth(ConfigYml.string("RedisPass"));
            jedis.del(key);
            jedis.close();
            jedisPool.close();
            System.gc();
        }).start();
    }

}
