package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
        // Shop shop2 = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
        //         this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null){
            return Result.fail("店铺不存在！");
        }
        //7. 返回
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    // public Shop queryWithLogicalExpire(Long id){
    //     //1. 从redis查询商铺缓存
    //     String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
    //     String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
    //     //2. 判断是否存在
    //     if (StrUtil.isBlank(shopJson)) { //2.1这里空值和NULL不一样
    //         //3. 不存在，直接返回
    //         return null;
    //     }
    //     //4. 命中需要先把json反序列化为对象
    //     RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
    //     Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
    //     LocalDateTime expireTime = redisData.getExpireTime();
    //     //5. 判断是否过期
    //     if (expireTime.isAfter(LocalDateTime.now())){
    //         //5.1 未过期，直接返回店铺信息
    //         return shop;
    //     }
    //     //5.2 已过期，需要缓存重建
    //     //6. 缓存重建
    //     //6.1 获取互斥锁
    //     String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    //     //6.2 判断是否获取锁成功
    //     boolean isLock = tryLock(lockKey);
    //     if (isLock){
    //         //6.3 成功，开启独立线程，实现缓存重建
    //         CACHE_REBUILD_EXECUTOR.submit( () -> {
    //             //重建缓存
    //             try {
    //                 this.saveShop2Redis(id, RedisConstants.CACHE_SHOP_TTL);
    //             } catch (Exception e) {
    //                 throw new RuntimeException(e);
    //             } finally {
    //                 unlock(lockKey);
    //             }
    //         });
    //     }
    //     //6.4 返回过期的商铺信息
    //     return shop;
    // }


    // public Shop queryWithMutex(Long id){
    //     //1. 从redis查询商铺缓存
    //     String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
    //     String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
    //     //2. 判断是否存在
    //     if (StrUtil.isNotBlank(shopJson)) { //2.1这里空值和NULL不一样
    //         //3. 存在，直接返回
    //         return JSONUtil.toBean(shopJson, Shop.class);
    //     }
    //     //2.2判断命中的是否是空值
    //     if (shopJson != null){// shopJson == ""
    //         // 返回一个错误信息
    //         return null;
    //     }
    //     //4.实现缓存重建
    //     //4.1 获取互斥锁
    //     String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
    //     Shop shop = null;
    //     try {
    //         boolean isLock = tryLock(lockKey);
    //         //4.2 判断是否获取成功
    //         if (!isLock) {
    //             //4.3 失败 则休眠并重试
    //             Thread.sleep(50);
    //             queryWithMutex(id);
    //         }
    //         //4.4 成功，根据ID查询数据库
    //         shop = getById(id);
    //         //模拟重建的延时
    //         Thread.sleep(200);
    //         //5. 不存在，返回错误
    //         if (shop == null) {
    //             // 将空值写入redis  缓存穿透
    //             stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
    //             // 返回错误信息
    //             return null;
    //         }
    //         //6. 存在，写入redis
    //         stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //
    //     } catch (InterruptedException e){
    //         throw new RuntimeException(e);
    //     }finally {
    //         //7.释放互斥锁
    //         unlock(lockKey);
    //     }
    //     //8. 返回
    //     return shop;
    // }


    // public Shop queryWithPassThrough(Long id){
    //     //1. 从redis查询商铺缓存
    //     String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
    //     String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
    //     //2. 判断是否存在
    //     if (StrUtil.isNotBlank(shopJson)) { //2.1这里空值和NULL不一样
    //         //3. 存在，直接返回
    //         return JSONUtil.toBean(shopJson, Shop.class);
    //     }
    //     //2.2判断命中的是否是空值
    //     if (shopJson != null){// shopJson == ""
    //         // 返回一个错误信息
    //         return null;
    //     }
    //     //4. 不存在，根据ID查询数据库
    //     Shop shop = getById(id);
    //     //5. 不存在，返回错误
    //     if (shop == null){
    //         // 将空值写入redis  缓存穿透
    //         stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
    //         // 返回错误信息
    //         return null;
    //     }
    //     //6. 存在，写入redis
    //     stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //     //7. 返回
    //     return shop;
    // }


    // private boolean tryLock(String key){
    //     Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
    //     return BooleanUtil.isTrue(flag);//自动拆箱可能会null和flase, 防止是null
    // }
    //
    // private void unlock(String key){
    //     stringRedisTemplate.delete(key);
    // }
    //
    // public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
    //     //1.查询店铺数据
    //     Shop shop = getById(id);
    //     Thread.sleep(200);
    //     //2.封装逻辑过期时间
    //     RedisData redisData = new RedisData();
    //     redisData.setData(shop);
    //     redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
    //     //3.写入redis
    //     stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    // }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺ID不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return null;
    }
}
