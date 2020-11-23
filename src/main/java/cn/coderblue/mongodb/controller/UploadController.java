package cn.coderblue.mongodb.controller;

import cn.coderblue.mongodb.entity.MongoFile;
import cn.coderblue.mongodb.utils.MD5;
import cn.coderblue.mongodb.utils.Result;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * 文件上传
 * @author coderblue
 */
@RequestMapping("/upload")
@RestController
@Slf4j
public class UploadController {

    /**
     * 默认20MB
     */
    @Value("${project.upload.sizeLimit}")
    private Long maxPostSize;
    /**
     * 支持的文件类型
     */
    private List<String> fileTypes;
    /**
     * 获得SpringBoot提供的mongodb的GridFS对象
     */
    @Autowired
    private GridFsTemplate gridFsTemplate;
    /**
     * 版本太高就会提示方法是被弃用的了
     */
    @Autowired
    private MongoDbFactory mongoDbFactory;

    /**
     * 单文件上传统一方法
     *
     * @param request
     * @param response
     * @return
     */
    @ApiOperation("单文件上传统一方法")
    @PostMapping("/image")
    public Result upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        // 获取上传文件对象
        MultipartFile file = multipartRequest.getFile("file");
        //初始化mongoFile对象
        MongoFile mongoFile = new MongoFile(file.getOriginalFilename(), file.getContentType(), file.getSize());
        this.uploadCommon(file, mongoFile);
        return Result.success().data("uploadFile", mongoFile);
    }

    /**
     * 批量文件上传
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @ApiOperation("批量文件上传")
    @PostMapping(value = "/batch")
    @ResponseBody
    protected Result uploadBatch(HttpServletRequest request, HttpServletResponse response) {
        List<MongoFile> mongoFiles = new ArrayList<>();

        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest mr = (MultipartHttpServletRequest) request;
            List<MultipartFile> multipartFile = mr.getFiles("file");

            if (null != multipartFile && !multipartFile.isEmpty()) {
                for (MultipartFile file : multipartFile) {
                    // 文件类型和大小检验
                    String sourceFileSuffix = file.getContentType();// 源文件类型
                    if (file.getSize() > maxPostSize) {
                        return Result.error().data("message", "上传文件超出" + formatWithUnit(maxPostSize) + "限制");
                    } else if (null == sourceFileSuffix || !fileTypes.contains(sourceFileSuffix)) {
                        return Result.error().data("message", "不允许上传的文件类型!");
                    }
                    MongoFile mongoFile = null;
                    try {
                        mongoFile = new MongoFile(file.getOriginalFilename(), file.getContentType(), file.getSize());
                        this.uploadCommon(file, mongoFile);
                    } catch (Exception e) {
                        mongoFile.setOperateMsg("文件上传失败！");
                        e.printStackTrace();
                    } finally {
                        //无论文件单文件上传失败成功与否皆保存文件对象至集合
                        mongoFiles.add(mongoFile);
                    }
                }
            }
        }
        return Result.success().data("uploadFile", mongoFiles);
    }

    @GetMapping(value = "/download/{_id}")
    @ApiOperation("下载文件")
    public void download(@PathVariable("_id") String _id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String fileId = _id;
        //通过文件对象id查询 GridFS类型文件
        GridFSFile image = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //获取文件资源
        GridFsResource gridFsResource = new GridFsResource(image, GridFSBuckets.create(mongoDbFactory.getDb()).openDownloadStream(image.getObjectId()));

        //获取文件名字
        String fileName = image.getFilename();
        //处理中文文件名乱码
        if (request.getHeader("User-Agent").toUpperCase().contains("MSIE") ||
                request.getHeader("User-Agent").toUpperCase().contains("TRIDENT")
                || request.getHeader("User-Agent").toUpperCase().contains("EDGE")) {
            fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
        } else {
            //非IE浏览器的处理：
            fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
        }
        //设置浏览器传输格式
        response.setContentType("multipart/form-data");
        //设置返回文件名
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        log.info("gridFsResource：" + gridFsResource);
        // 获取文件输入流至浏览器输出流
        IOUtils.copy(gridFsResource.getInputStream(), response.getOutputStream());
    }

    /**
     * 文件上传的通用抽离方法
     * @param file
     * @param mongoFile
     * @throws Exception
     */
    private void uploadCommon(MultipartFile file, MongoFile mongoFile) throws Exception {
        // 获得文件输入流
        InputStream is = file.getInputStream();
        //获取文件流的md5值
        String md5 = MD5.getMd5(is);
        //通过文件MD5值去mongo数据库查询文件
        GridFSFile fileByMd5 = gridFsTemplate.findOne(query(Criteria.where("md5").is(md5)));
        //如果文件存在返回文件 对象id（_id）
        if (fileByMd5 != null) {
            mongoFile.set_id(fileByMd5.getObjectId().toString());
        } else {
            //文件不存在，保存文件，返回文件 对象id（_id）
            ObjectId store = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
            mongoFile.set_id(store.toString());
        }
        mongoFile.setOperateStatus(true);
    }

    /**
     * 数据转为带单位字符串
     * Byte -> KB -> MB -> GB
     *
     * @returns {string}
     */
    public static String formatWithUnit(Long value) {
        if (value / 1024 < 1024) {
            return value * 100 / 1024 / 100 + "KB";
        } else if (value / Math.pow(1024, 2) < 1024) {
            return value * 100 / Math.pow(1024, 2) / 100 + "MB";
        } else {
            return value * 100 / Math.pow(1024, 3) / 100 + "GB";
        }
    }


    @Value("${project.upload.fileType}")
    public void setFileTypes(String fileTypes) {
        this.fileTypes = new ArrayList<>();
        if (!StringUtils.isEmpty(fileTypes)) {
            String[] types = fileTypes.split(",");
            for (String type : types) {
                if (!StringUtils.isEmpty(type)) {
                    this.fileTypes.add(type);
                }
            }
        }
    }
}
