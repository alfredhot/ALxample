package twoson.cars.comp.controller;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("comp")
public class CompController {
	
	@RequestMapping(value="", method=RequestMethod.GET)
	public String compMainPage(@RequestParam String param, Model model){

		JSONObject obj = new JSONObject();
		obj.put("param1", "1");
		obj.put("param2", "2");
		model.addAttribute("parameter",obj);
		return "comp/compPage";
	}
	
	@RequestMapping(value="/info", method=RequestMethod.POST)
	public @ResponseBody String getCompInfo(@RequestParam String type){
		return "";
	}
}
