package cn.coderblue.mongodb.entity;


import lombok.Data;

/**
 * Mongo相关操作返回消息
 * @author coderblue
 */
@Data
public class MongoFile {

    /**
     * mongo文件对象id
     */
    private String _id;

    /**
     * 文件名字
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 操作状态,默认失败
     */
    private boolean operateStatus = false;

    /**
     * 操作说明
     */
    private String operateMsg;

    public MongoFile() {}

    public MongoFile(String fileName, String fileType, long fileSize) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
}

