package com.ccsurvey.modules.file.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileRepository extends BaseMapper<FileMetadata> {

    default FileMetadata findByUuid(String uuid) {
        return selectById(uuid);
    }

    @Select("SELECT * FROM sys_file WHERE file_hash = #{hash}")
    FileMetadata findByHash(@Param("hash") String hash);
}