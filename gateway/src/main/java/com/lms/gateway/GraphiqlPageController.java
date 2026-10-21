package com.lms.gateway;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GraphiqlPageController {
    @GetMapping("/graphiql")
    public String graphiql() {
        return "forward:/vendor/graphiql/index.html";
    }
}
