package cn.itechyou.cms.vo;

import cn.itechyou.cms.entity.Archives;
import lombok.Data;

/**
 * 文章扩展实体
 * @author 王俊南
 * Date: 2020-12-29
 */
@Data
public class ArchivesVo extends Archives {
	private String categoryCnName;
	private String categoryEnName;
}
