package com.SixSense.config;

import com.SixSense.util.MessageLiterals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.PathResource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
/*adapted from https://stackoverflow.com/a/28829727/1658288*/
public class YamlSourceLoader {
    private static final Logger logger = LogManager.getLogger(YamlSourceLoader.class);

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        //Declarations
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();

        //Iterate the project configuration directory, and add any yaml file found to the list of path resources
        List<PathResource> yamlFiles = new ArrayList<>();
        try {
            DirectoryStream<Path> configRoot = Files.newDirectoryStream(Paths.get(MessageLiterals.configFilesPath));
            for (Path child : configRoot) {
                if (Files.isRegularFile(child) && child.toString().endsWith(".yaml")) {
                    yamlFiles.add(new PathResource(child));
                }
            }
        }catch (IOException e){
            logger.fatal("Failed to load sixsense configuration files! Caused by: ", e);
        }

        /*setResources() can use any of the spring io resource types
        * the method signature accepts any number of resources, but subsequent calls will override any existing resources, so call this method once*/
        yamlFactory.setResources(new PathResource(MessageLiterals.projectDirectory + "default.yaml"));
        yamlFactory.setResources(yamlFiles.toArray(new PathResource[yamlFiles.size()]));

        /*calling yamlFactory.getObject() checks internally if there are any properties (yamlFactory.properties != null)
        * because we haven't set them, the method invokes yamlFactory.createProperties(), which parses the .yaml files into a java.util.Properties object*/
        propertySourcesPlaceholderConfigurer.setProperties(yamlFactory.getObject());
        return propertySourcesPlaceholderConfigurer;
    }
}
