package twoson.cars.hello.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("hello")
public class HelloController {

	@RequestMapping("")
	public String helloPage(){
		return "hello/hello";
	}
	@RequestMapping("/thyme")
	public String helloThyme(){
		return "hello/helloThyme";
	}
	@RequestMapping("/login")
	public String helloLogin(){
		return "login";
	}
}
