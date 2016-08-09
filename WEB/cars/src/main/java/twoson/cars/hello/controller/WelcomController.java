package twoson.cars.hello.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class WelcomController {

	@RequestMapping("")
	public String welcome(){
		return "hello/layout_test";
//		return "welcome";
	}
}
