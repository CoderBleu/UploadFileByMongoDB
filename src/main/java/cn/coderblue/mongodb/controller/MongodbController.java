package cn.coderblue.mongodb.controller;

import cn.coderblue.mongodb.entity.UserInfo;
import cn.coderblue.mongodb.utils.Result;
import com.mongodb.client.result.UpdateResult;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author coderblue
 */
@RestController
@RequestMapping("/mongo")
@Slf4j
public class MongodbController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping
    @Cacheable(value = "UserInfo", key = "'selectUserInfo'")
    @ApiOperation(value = "查询", notes = "mongo查询")
    public Result find() {
        List<UserInfo> all = mongoTemplate.findAll(UserInfo.class);
        log.info("用户信息：" + all);
        return Result.success().data("findAll", all);
    }

    @GetMapping("/findCondition/{page}/{size}")
    @ApiOperation(value = "分页查询", notes = "mongo分页查询")
    public Result findCondition(UserInfo userInfo, @PathVariable Integer page, @PathVariable Integer size) {
        // 条件：字段区分大小写
        Criteria criteria = Criteria.where("gender").is(userInfo.getGender());
        Query query = new Query();
        query.addCriteria(criteria);
        // 数量
        long total = mongoTemplate.count(query, UserInfo.class);
        // 分页
        query.skip((page - 1) * size).limit(size);
        List<UserInfo> condition = mongoTemplate.find(query, UserInfo.class);
        log.info("用户信息-分页：" + condition);
        return Result.success().data("findCondition", condition).data("total", total);
    }

    @PostMapping
    @ApiOperation(value = "保存", notes = "mongo保存")
    public Result save(UserInfo userInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("age", 18);
        map.put("gender", "M");
        map.put("user", userInfo);
        // userInfo.setId(System.currentTimeMillis()); 不设置id，默认会生成
        mongoTemplate.save(userInfo);
        return Result.success().data(map);
    }

    /**
     * 根据用户名查询对象
     *
     * @return
     */
    @ApiOperation("根据用户名查询对象")
    @GetMapping("/findByName")
    public UserInfo findByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, UserInfo.class);
    }

    /**
     * 更新对象：将修改后的返回值作为value
     * CachePut：先修改，然后更新缓存数据
     */
    @PutMapping("/update")
    @ApiOperation("更新对象")
    @CachePut(value = "UserInfo", key = "'selectUserInfo'")
    public Result update(UserInfo userInfo) {
        Query query = new Query(Criteria.where("id").is(userInfo.getId()));
        Update update = new Update().set("age", userInfo.getAge()).set("name", userInfo.getName());
        //更新查询返回结果集的第一条
        UpdateResult result = mongoTemplate.updateFirst(query, update, UserInfo.class);
        // 将修改后的返回值作为value
        UserInfo info = this.findByName(userInfo.getName());
        return Result.success().data("data", info);
        // 更新查询返回结果集的所有
        // mongoTemplate.updateMulti(query,update,UserInfo.class);
    }

    /**
     * 根据id删除对象
     *
     * @param id
     */
    @ApiOperation("根据id删除对象")
    @DeleteMapping("deleteById")
    public void deleteById(Integer id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, UserInfo.class);
    }

}
