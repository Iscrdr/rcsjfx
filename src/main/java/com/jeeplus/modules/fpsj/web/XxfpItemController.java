/**
 * Copyright &copy; 2015-2020 <a href="http://www.xiaostarstar.com/">XSS</a> All rights reserved.
 */
package com.jeeplus.modules.fpsj.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;

import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.Lists;
import com.jeeplus.common.utils.DateUtils;
import com.jeeplus.common.utils.MyBeanUtils;
import com.jeeplus.common.config.Global;
import com.jeeplus.common.persistence.Page;
import com.jeeplus.common.web.BaseController;
import com.jeeplus.common.utils.StringUtils;
import com.jeeplus.common.utils.excel.ExportExcel;
import com.jeeplus.common.utils.excel.ImportExcel;
import com.jeeplus.modules.fpsj.entity.XxfpItem;
import com.jeeplus.modules.fpsj.service.XxfpItemService;

/**
 * 发票数据Controller
 * @author admin
 * @version 2018-02-24
 */
@Controller
@RequestMapping(value = "${adminPath}/fpsj/xxfpItem")
public class XxfpItemController extends BaseController {

	@Autowired
	private XxfpItemService xxfpItemService;
	
	@ModelAttribute
	public XxfpItem get(@RequestParam(required=false) String id) {
		XxfpItem entity = null;
		if (StringUtils.isNotBlank(id)){
			entity = xxfpItemService.get(id);
		}
		if (entity == null){
			entity = new XxfpItem();
		}
		return entity;
	}
	
	/**
	 * 发票数据列表页面
	 */
	@RequiresPermissions("fpsj:xxfpItem:list")
	@RequestMapping(value = {"list", ""})
	public String list(XxfpItem xxfpItem, HttpServletRequest request, HttpServletResponse response, Model model) {
		Page<XxfpItem> page = xxfpItemService.findPage(new Page<XxfpItem>(request, response), xxfpItem); 
		model.addAttribute("page", page);
		return "modules/fpsj/xxfpItemList";
	}

	/**
	 * 查看，增加，编辑发票数据表单页面
	 */
	@RequiresPermissions(value={"fpsj:xxfpItem:view","fpsj:xxfpItem:add","fpsj:xxfpItem:edit"},logical=Logical.OR)
	@RequestMapping(value = "form")
	public String form(XxfpItem xxfpItem, Model model) {
		model.addAttribute("xxfpItem", xxfpItem);
		return "modules/fpsj/xxfpItemForm";
	}

	/**
	 * 保存发票数据
	 */
	@RequiresPermissions(value={"fpsj:xxfpItem:add","fpsj:xxfpItem:edit"},logical=Logical.OR)
	@RequestMapping(value = "save")
	public String save(XxfpItem xxfpItem, Model model, RedirectAttributes redirectAttributes) throws Exception{
		if (!beanValidator(model, xxfpItem)){
			return form(xxfpItem, model);
		}
		if(!xxfpItem.getIsNewRecord()){//编辑表单保存
			XxfpItem t = xxfpItemService.get(xxfpItem.getId());//从数据库取出记录的值
			MyBeanUtils.copyBeanNotNull2Bean(xxfpItem, t);//将编辑表单中的非NULL值覆盖数据库记录中的值
			xxfpItemService.save(t);//保存
		}else{//新增表单保存
			xxfpItemService.save(xxfpItem);//保存
		}
		addMessage(redirectAttributes, "保存发票数据成功");
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
	}
	
	/**
	 * 删除发票数据
	 */
	@RequiresPermissions("fpsj:xxfpItem:del")
	@RequestMapping(value = "delete")
	public String delete(XxfpItem xxfpItem, RedirectAttributes redirectAttributes) {
		xxfpItemService.delete(xxfpItem);
		addMessage(redirectAttributes, "删除发票数据成功");
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
	}
	
	/**
	 * 批量删除发票数据
	 */
	@RequiresPermissions("fpsj:xxfpItem:del")
	@RequestMapping(value = "deleteAll")
	public String deleteAll(String ids, RedirectAttributes redirectAttributes) {
		String idArray[] =ids.split(",");
		for(String id : idArray){
			xxfpItemService.delete(xxfpItemService.get(id));
		}
		addMessage(redirectAttributes, "删除发票数据成功");
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
	}
	
	/**
	 * 导出excel文件
	 */
	@RequiresPermissions("fpsj:xxfpItem:export")
    @RequestMapping(value = "export", method=RequestMethod.POST)
    public String exportFile(XxfpItem xxfpItem, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try {
            String fileName = "发票数据"+DateUtils.getDate("yyyyMMddHHmmss")+".xlsx";
            Page<XxfpItem> page = xxfpItemService.findPage(new Page<XxfpItem>(request, response, -1), xxfpItem);
    		new ExportExcel("发票数据", XxfpItem.class).setDataList(page.getList()).write(response, fileName).dispose();
    		return null;
		} catch (Exception e) {
			addMessage(redirectAttributes, "导出发票数据记录失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
    }

	/**
	 * 导入Excel数据

	 */
	@RequiresPermissions("fpsj:xxfpItem:import")
    @RequestMapping(value = "import", method=RequestMethod.POST)
    public String importFile(MultipartFile file, RedirectAttributes redirectAttributes) {
		try {
			int successNum = 0;
			int failureNum = 0;
			StringBuilder failureMsg = new StringBuilder();
			ImportExcel ei = new ImportExcel(file, 1, 0);
			List<XxfpItem> list = ei.getDataList(XxfpItem.class);
			for (XxfpItem xxfpItem : list){
				try{
					xxfpItemService.save(xxfpItem);
					successNum++;
				}catch(ConstraintViolationException ex){
					failureNum++;
				}catch (Exception ex) {
					failureNum++;
				}
			}
			if (failureNum>0){
				failureMsg.insert(0, "，失败 "+failureNum+" 条发票数据记录。");
			}
			addMessage(redirectAttributes, "已成功导入 "+successNum+" 条发票数据记录"+failureMsg);
		} catch (Exception e) {
			addMessage(redirectAttributes, "导入发票数据失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
    }
	
	/**
	 * 下载导入发票数据数据模板
	 */
	@RequiresPermissions("fpsj:xxfpItem:import")
    @RequestMapping(value = "import/template")
    public String importFileTemplate(HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try {
            String fileName = "发票数据数据导入模板.xlsx";
    		List<XxfpItem> list = Lists.newArrayList(); 
    		new ExportExcel("发票数据数据", XxfpItem.class, 1).setDataList(list).write(response, fileName).dispose();
    		return null;
		} catch (Exception e) {
			addMessage(redirectAttributes, "导入模板下载失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/fpsj/xxfpItem/?repage";
    }
	
	
	

}