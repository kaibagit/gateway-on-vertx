package me.luliru.gateway.core.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application
 * Created by luliru on 2019-07-04.
 */
@EnableScheduling
@ImportResource(locations = "classpath:/spring/application-*.xml")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SolrAutoConfiguration.class})
@ComponentScan(basePackages = {"me.luliru"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
