package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        //1.从redis查询列表商铺缓存
        String shopTypeListKey = RedisConstants.CACHE_SHOP_TYPE_KEY;
        String listJson = stringRedisTemplate.opsForValue().get(shopTypeListKey);
        //2.判断是否存在
        if (StrUtil.isNotBlank(listJson)){
            //3.存在，直接返回
            return Result.ok(JSONUtil.toList(listJson, ShopType.class));
        }
        //4.不存在，数据库查询
        List<ShopType> shopTypeList = this.query().orderByDesc("sort").list();
        //5.不存在，返回错误
        if (shopTypeList == null || shopTypeList.size() == 0){
            return Result.fail("商品类型查询失败！");
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(shopTypeListKey, JSONUtil.toJsonStr(shopTypeList));
        //7.返回
        return  Result.ok(shopTypeList);
    }
}
