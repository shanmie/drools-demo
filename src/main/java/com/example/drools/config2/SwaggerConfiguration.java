package com.example.drools.config2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2) //
                .apiInfo(apiInfo()) //
                .pathMapping("/")// base，最终调用接口后会和paths拼接在一起
                .select() //
                .apis(RequestHandlerSelectors.basePackage("com.example.drools")) //
                .paths(PathSelectors.any()) //
                .build(); //
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder() //
                .title("drools-demo") //
                .version("1.0") //
                .build();
    }
}
