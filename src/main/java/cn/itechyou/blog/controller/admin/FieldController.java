package cn.itechyou.blog.controller.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.github.pagehelper.PageInfo;

import cn.itechyou.blog.common.Constant;
import cn.itechyou.blog.common.SearchEntity;
import cn.itechyou.blog.entity.Field;
import cn.itechyou.blog.entity.Form;
import cn.itechyou.blog.exception.TransactionException;
import cn.itechyou.blog.security.token.TokenManager;
import cn.itechyou.blog.service.FieldService;
import cn.itechyou.blog.service.FormService;
import cn.itechyou.blog.utils.StringUtils;
import cn.itechyou.blog.utils.UUIDUtils;

@Controller
@RequestMapping("/admin/field")
public class FieldController {
	
	@Autowired
	private FormService formService;
	
	@Autowired
	private FieldService fieldService;
	
	@RequestMapping("/toAdd")
	public String toAdd(Model model, String formId) {
		model.addAttribute("formId", formId);
		return "/admin/field/add";
	}
	
	@RequestMapping("/add")
	public String add(Model model,Field field) {
		field.setId(UUIDUtils.getPrimaryKey());
		field.setCreateBy(TokenManager.getToken().getId());
		field.setCreateTime(new Date());
		if(StringUtils.isBlank(field.getMaxLength())) {
			field.setMaxLength(200);
		}
		try {
			fieldService.add(field);
		} catch (TransactionException e) {
			model.addAttribute("exception", e);
			return Constant.ERROR;
		}
		return "redirect:/admin/forms/list";
	}
	
	@RequestMapping(value = "/toEdit", method = RequestMethod.GET)
	public String toEdit(Model model, String id) {
		Field field = fieldService.queryFieldById(id);
		Form form = formService.queryFormById(field.getFormId());

		model.addAttribute("form", form);
		model.addAttribute("field", field);
		
		return "/admin/field/edit";
	}
	
	@RequestMapping("/edit")
	public String edit(Model model,Field newField) {
		Form form = formService.queryFormById(newField.getFormId());
		
		Field oldField = fieldService.queryFieldById(newField.getId());
		newField.setUpdateBy(TokenManager.getToken().getId());
		newField.setUpdateTime(new Date());
		if(StringUtils.isBlank(newField.getMaxLength())) {
			newField.setMaxLength(200);
		}
		try {
			fieldService.update(newField,form,oldField);
		} catch (TransactionException e) {
			model.addAttribute("exception", e);
			return Constant.ERROR;
		}
		return "redirect:/admin/forms/list";
	}
	
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public String delete(Model model, String id) {
		Field field = fieldService.queryFieldById(id);
		Form form = formService.queryFormById(field.getFormId());
		if(form.getType() == 0) {
			throw new RuntimeException("??????????????????????????????");
		}
		try {
			fieldService.delete(form,field);
		} catch (TransactionException e) {
			model.addAttribute("exception", e);
			return Constant.ERROR;
		}
		return "redirect:/admin/forms/list";
	}
}
