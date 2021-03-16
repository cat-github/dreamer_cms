package cn.itechyou.cms.controller.admin;

import java.util.Date;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageInfo;

import cn.itechyou.cms.common.ResponseResult;
import cn.itechyou.cms.common.SearchEntity;
import cn.itechyou.cms.common.StateCodeEnum;
import cn.itechyou.cms.entity.Scheduled;
import cn.itechyou.cms.security.token.TokenManager;
import cn.itechyou.cms.service.ScheduledService;
import cn.itechyou.cms.task.ScheduledOfTask;
import cn.itechyou.cms.utils.CronUtils;
import cn.itechyou.cms.utils.UUIDUtils;

/**
 * 管理定时任务(需要做权限控制)
 *
 * @author yudong
 * @date 2019/5/10
 */
@Controller
@RequestMapping("admin/task")
public class TaskController {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private ScheduledService scheduledService;

    /**
     * 查看任务列表
     */
    @RequestMapping({"","/list"})
    public String taskList(Model model, SearchEntity params) {
        PageInfo<Scheduled> page = scheduledService.queryListByPage(params);
		model.addAttribute("pageInfo", page);
		return "admin/task/list";
    }

    /**
	 * 添加跳转
	 */
	@RequestMapping("/toAdd")
	@RequiresPermissions("8h9r1tin")
	public String toAdd(Model model) {
		return "admin/task/add";
	}
	
	/**
     * 编辑任务cron表达式
     */
    @RequestMapping("/add")
    public String add(Scheduled scheduled) {
        if (!CronUtils.isValidExpression(scheduled.getCronExpression())) {
            throw new IllegalArgumentException("失败,非法表达式:" + scheduled.getCronExpression());
        }
        scheduled.setId(UUIDUtils.getPrimaryKey());
        scheduled.setCreateBy(TokenManager.getUserId());
        scheduled.setCreateTime(new Date());
        int i = scheduledService.add(scheduled);
        return "redirect:/admin/task/list";
    }
	
	/**
	 * 编辑跳转
	 */
	@RequestMapping("/toEdit")
	@RequiresPermissions("8h9r1tin")
	public String toEdit(Model model,String id) {
		Scheduled scheduled = scheduledService.queryScheduledById(id);
		model.addAttribute("scheduled", scheduled);
		return "admin/task/edit";
	}
    
    /**
     * 编辑任务cron表达式
     */
    @RequestMapping("/update")
    public String update(Scheduled scheduled) {
        if (!CronUtils.isValidExpression(scheduled.getCronExpression())) {
            throw new IllegalArgumentException("失败,非法表达式:" + scheduled.getCronExpression());
        }
        scheduled.setUpdateBy(TokenManager.getUserId());
        scheduled.setUpdateTime(new Date());
        int i = scheduledService.update(scheduled);
        return "redirect:/admin/task/list";
    }

    /**
	 * 删除
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	@RequiresPermissions("zr25t2e0")
	public String delete(Model model, String id) {
		scheduledService.delete(id);
		return "redirect:/admin/task/list";
	}
    
    /**
     * 执行定时任务
     */
    @RequestMapping("/run")
    @ResponseBody
    public ResponseResult runTaskCron(String id) throws Exception {
    	Scheduled scheduled = scheduledService.queryScheduledById(id);
        ((ScheduledOfTask) context.getBean(Class.forName(scheduled.getClazzName()))).execute();
        return ResponseResult.Factory.newInstance(Boolean.TRUE,
				StateCodeEnum.HTTP_SUCCESS.getCode(), null,
				StateCodeEnum.HTTP_SUCCESS.getDescription());
    }

    /**
     * 启用或禁用定时任务
     */
    @RequestMapping("/changeStatus")
    public String changeStatusTaskCron(String id, String status) {
    	Scheduled scheduled = new Scheduled();
    	scheduled.setId(id);
    	scheduled.setStatus(status);
    	int i = scheduledService.update(scheduled);
    	return "redirect:/admin/task/list";
    }

}
