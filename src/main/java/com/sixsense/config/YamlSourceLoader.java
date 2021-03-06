package com.sixsense.config;

import com.sixsense.utillity.Literals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

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
        List<FileSystemResource> yamlFiles = new ArrayList<>();
        try (DirectoryStream<Path> configRoot = Files.newDirectoryStream(Paths.get(Literals.ConfigFilesPath))){
            for (Path child : configRoot) {
                if (Files.isRegularFile(child) && child.toString().endsWith(".yaml")) {
                    yamlFiles.add(new FileSystemResource(child));
                }
            }
        }catch (IOException e){
            logger.fatal("Failed to load sixsense configuration files! Caused by: ", e);
        }

        /*setResources() can use any of the spring io resource types
        * the method signature accepts any number of resources, but subsequent calls will override any existing resources, so call this method once*/
        yamlFactory.setResources(new FileSystemResource(Literals.projectDirectory + "default.yaml"));
        yamlFactory.setResources(yamlFiles.toArray(new FileSystemResource[yamlFiles.size()]));

        /*calling yamlFactory.getObject() checks internally if there are any properties (yamlFactory.properties != null)
        * because we haven't set them, the method invokes yamlFactory.createProperties(), which parses the .yaml files into a java.util.Properties object*/
        propertySourcesPlaceholderConfigurer.setProperties(yamlFactory.getObject());
        return propertySourcesPlaceholderConfigurer;
    }
}
